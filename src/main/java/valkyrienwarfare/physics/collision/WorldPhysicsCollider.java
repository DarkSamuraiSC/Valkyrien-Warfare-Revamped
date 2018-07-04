/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2015-2018 the Valkyrien Warfare team
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income unless it is to be used as a part of a larger project (IE: "modpacks"), nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from the Valkyrien Warfare team.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: The Valkyrien Warfare team), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package valkyrienwarfare.physics.collision;

import gnu.trove.TCollections;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import valkyrienwarfare.api.TransformType;
import valkyrienwarfare.math.RotationMatrices;
import valkyrienwarfare.math.Vector;
import valkyrienwarfare.mod.multithreaded.PhysicsShipTransform;
import valkyrienwarfare.mod.physmanagement.relocation.SpatialDetector;
import valkyrienwarfare.physics.PhysicsCalculations;
import valkyrienwarfare.physics.collision.optimization.IBitOctree;
import valkyrienwarfare.physics.collision.optimization.IBitOctreeProvider;
import valkyrienwarfare.physics.collision.optimization.ShipCollisionTask;
import valkyrienwarfare.physics.collision.polygons.PhysCollisionObject;
import valkyrienwarfare.physics.collision.polygons.PhysPolygonCollider;
import valkyrienwarfare.physics.collision.polygons.Polygon;
import valkyrienwarfare.physics.collision.polygons.PolygonCollisionPointFinder;
import valkyrienwarfare.physics.management.PhysicsObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Handles the task of finding and processing collisions between a PhysicsObject
 * and the game world.
 *
 * @author thebest108
 */
public class WorldPhysicsCollider {

    // Used to expand the AABB used to check for potential collisions; helps prevent
    // ships ghosting through blocks
    public static final double AABB_EXPANSION = 2D;
    // The range we check for possible collisions with a block.
    public static final double RANGE_CHECK = 1.8D;
    // The minimum depth a collision projection must have, to not use the default
    // collision normal of <0, 1, 0>
    public static final double AXIS_TOLERANCE = .3D;
    // Time in seconds between collision cache updates. A value of .1D means we
    // update the collision cache every 1/10th of a second.
    public static final double CACHE_UPDATE_FREQUENCY = .075D;
    // Determines how 'bouncy' collisions are
    public static final double COEFFICIENT_OF_RESTITUTION = .52D;
    // The radius which the algorithm will search for a nearby block to collide with
    public static final double COLLISION_RANGE_CHECK = .65D;
    // If true, will use the octree assisted algorithm for finding collisions,
    // (Approx. O(log(n)^3)).
    // If false then this class uses the much slower iterative approach O(n^3).
    public static final boolean USE_OCTREE_COLLISION = true;
    // How likely it is for the collision tasks to shuffle every physics tick
    // ie. (.50D => 50% chance to shuffle, .30D => 30% chance, etc.)
    public static final double COLLISION_TASK_SHUFFLE_FREQUENCY = .50D;
    // Greater coefficients result in more friction
    public static final double KINETIC_FRICTION_COEFFICIENT = .15D;
    private final MutableBlockPos mutablePos;
    private final Random rand;
    private final Collection<ShipCollisionTask> tasks;
    private final PhysicsCalculations calculator;
    private final PhysicsObject parent;
    private final TIntList cachedPotentialHits;
    private final TIntArrayList cachedHitsToRemove;
    // Ensures this always updates the first tick after creation
    private double ticksSinceCacheUpdate;
    private boolean updateCollisionTasksCache;
    private BlockPos centerPotentialHit;

    public WorldPhysicsCollider(PhysicsCalculations calculations) {
        this.calculator = calculations;
        this.parent = calculations.getParent();
        World worldObj = this.parent.getWorldObj();
        this.cachedPotentialHits = TCollections.synchronizedList(new TIntArrayList());
        this.cachedHitsToRemove = new TIntArrayList();
        this.rand = new Random();
        this.mutablePos = new MutableBlockPos();
        this.tasks = new ArrayList<>();
        this.ticksSinceCacheUpdate = 25D;
        this.updateCollisionTasksCache = true;
        this.centerPotentialHit = null;
    }

    public void tickUpdatingTheCollisionCache() {
        // Multiply by 20 to convert seconds (physTickSpeed) into ticks
        this.ticksSinceCacheUpdate += this.calculator.getPhysicsTimeDeltaPerPhysTick();
        for (int i = 0; i < this.cachedHitsToRemove.size(); i++) {
            this.cachedPotentialHits.remove(this.cachedHitsToRemove.get(i));
        }
        this.cachedHitsToRemove.resetQuick();
        if (this.ticksSinceCacheUpdate > CACHE_UPDATE_FREQUENCY || this.parent.needsImmediateCollisionCacheUpdate()) {
            this.updatePotentialCollisionCache();
            this.updateCollisionTasksCache = true;
        }
        if (Math.random() < COLLISION_TASK_SHUFFLE_FREQUENCY) {
            this.cachedPotentialHits.shuffle(this.rand);
        }
    }

    public void splitIntoCollisionTasks(List<ShipCollisionTask> toAdd) {
        if (this.updateCollisionTasksCache) {
            this.tasks.clear();
            int index = 0;
            int size = this.cachedPotentialHits.size();
            while (index < size) {
                ShipCollisionTask task = new ShipCollisionTask(this, index);
                index += ShipCollisionTask.MAX_TASKS_TO_CHECK;
                this.tasks.add(task);
            }
            this.updateCollisionTasksCache = false;
        }
        toAdd.addAll(this.tasks);
    }

    public void processCollisionTask(ShipCollisionTask task) {
        MutableBlockPos inWorldPos = new MutableBlockPos();
        MutableBlockPos inLocalPos = new MutableBlockPos();

        Iterator<CollisionInformationHolder> collisionIterator = task.getCollisionInformationIterator();

        while (collisionIterator.hasNext()) {
            CollisionInformationHolder info = collisionIterator.next();
            inWorldPos.setPos(info.inWorldX, info.inWorldY, info.inWorldZ);
            inLocalPos.setPos(info.inLocalX, info.inLocalY, info.inLocalZ);
            this.handleActualCollision(info.collider, inWorldPos, inLocalPos, info.inWorldState, info.inLocalState);
        }

        /*
         * for (CollisionInformationHolder info :
         * task.getCollisionInformationGenerated()) { inWorldPos.setPos(info.inWorldX,
         * info.inWorldY, info.inWorldZ); inLocalPos.setPos(info.inLocalX,
         * info.inLocalY, info.inLocalZ); handleActualCollision(info.collider,
         * inWorldPos, inLocalPos, info.inWorldState, info.inLocalState); }
         */

        task.getCollisionInformationGenerated().clear();
    }

    // Runs through the cache ArrayList, checking each possible BlockPos for SOLID
    // blocks that can collide, if it finds any it will
    // move to the next method

    // TODO: Optimize from here, this is taking 10x the processing time of updating
    // collision cache!
    private void processPotentialCollisionsAccurately() {
        MutableBlockPos localCollisionPos = new MutableBlockPos();
        Vector inWorld = new Vector();

        TIntIterator cachedHitsIterator = this.cachedPotentialHits.iterator();
        while (cachedHitsIterator.hasNext()) {
            // Converts the int to a mutablePos
            SpatialDetector.setPosWithRespectTo(cachedHitsIterator.next(), this.centerPotentialHit, this.mutablePos);

            inWorld.X = this.mutablePos.getX() + .5;
            inWorld.Y = this.mutablePos.getY() + .5;
            inWorld.Z = this.mutablePos.getZ() + .5;

            this.parent.getShipTransformationManager().getCurrentPhysicsTransform().transform(inWorld,
                    TransformType.GLOBAL_TO_SUBSPACE);

            // parent.coordTransform.fromGlobalToLocal(inWorld);

            int minX = MathHelper.floor(inWorld.X - COLLISION_RANGE_CHECK);
            int minY = MathHelper.floor(inWorld.Y - COLLISION_RANGE_CHECK);
            int minZ = MathHelper.floor(inWorld.Z - COLLISION_RANGE_CHECK);

            int maxX = MathHelper.floor(inWorld.X + COLLISION_RANGE_CHECK);
            int maxY = MathHelper.floor(inWorld.Y + COLLISION_RANGE_CHECK);
            int maxZ = MathHelper.floor(inWorld.Z + COLLISION_RANGE_CHECK);

            /**
             * Something here is causing the game to freeze :/
             */

            int minChunkX = minX >> 4;
            int minChunkY = minY >> 4;
            int minChunkZ = minZ >> 4;

            int maxChunkX = maxX >> 4;
            int maxChunkY = maxY >> 4;
            int maxChunkZ = maxZ >> 4;

            entireLoop:
            if (!(minChunkY > 15 || maxChunkY < 0)) {
                for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                    for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                        if (this.parent.ownsChunk(chunkX, chunkZ)) {
                            Chunk chunkIn = this.parent.getChunkCache().getChunkAt(chunkX, chunkZ);

                            int minXToCheck = chunkX << 4;
                            int maxXToCheck = minXToCheck + 15;

                            int minZToCheck = chunkZ << 4;
                            int maxZToCheck = minZToCheck + 15;

                            minXToCheck = Math.max(minXToCheck, minX);
                            maxXToCheck = Math.min(maxXToCheck, maxX);

                            minZToCheck = Math.max(minZToCheck, minZ);
                            maxZToCheck = Math.min(maxZToCheck, maxZ);

                            for (int chunkY = minChunkY; chunkY <= maxChunkY; chunkY++) {
                                ExtendedBlockStorage storage = chunkIn.storageArrays[chunkY];
                                if (storage != null) {
                                    int minYToCheck = chunkY << 4;
                                    int maxYToCheck = minYToCheck + 15;

                                    minYToCheck = Math.max(minYToCheck, minY);
                                    maxYToCheck = Math.min(maxYToCheck, maxY);

                                    for (int x = minXToCheck; x <= maxXToCheck; x++) {
                                        for (int z = minZToCheck; z <= maxZToCheck; z++) {
                                            for (int y = minYToCheck; y <= maxYToCheck; y++) {
                                                IBlockState state = storage.get(x & 15, y & 15, z & 15);
                                                if (state.getMaterial().isSolid()) {

                                                    // Inject the multithreaded code here

                                                    localCollisionPos.setPos(x, y, z);

                                                    boolean brokeAWorldBlock = this.handleLikelyCollision(this.mutablePos,
                                                            localCollisionPos, this.parent.getCachedSurroundingChunks()
                                                                    .getBlockState(this.mutablePos),
                                                            state);

                                                    if (brokeAWorldBlock) {
                                                        int positionRemoved = SpatialDetector.getHashWithRespectTo(
                                                                this.mutablePos.getX(), this.mutablePos.getY(), this.mutablePos.getZ(),
                                                                this.centerPotentialHit);
                                                        this.cachedHitsToRemove.add(positionRemoved);
                                                        break entireLoop;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Tests two block positions directly against each other, and figures out
    // whether a collision is occuring or not
    private boolean handleLikelyCollision(BlockPos inWorldPos, BlockPos inLocalPos, IBlockState inWorldState,
                                          IBlockState inLocalState) {
        // System.out.println("Handling a likely collision");
        AxisAlignedBB inLocalBB = new AxisAlignedBB(inLocalPos.getX(), inLocalPos.getY(), inLocalPos.getZ(),
                inLocalPos.getX() + 1, inLocalPos.getY() + 1, inLocalPos.getZ() + 1);
        AxisAlignedBB inGlobalBB = new AxisAlignedBB(inWorldPos.getX(), inWorldPos.getY(), inWorldPos.getZ(),
                inWorldPos.getX() + 1, inWorldPos.getY() + 1, inWorldPos.getZ() + 1);

        // This changes the box bounding box to the real bounding box, not sure if this
        // is better or worse for this mod
        // List<AxisAlignedBB> colBB = worldObj.getCollisionBoxes(inLocalBB);
        // inLocalBB = colBB.get(0);

        Polygon shipInWorld = new Polygon(inLocalBB, this.parent.getShipTransformationManager().getCurrentPhysicsTransform(),
                TransformType.SUBSPACE_TO_GLOBAL);
        Polygon worldPoly = new Polygon(inGlobalBB);
        PhysPolygonCollider collider = new PhysPolygonCollider(shipInWorld, worldPoly,
                this.parent.getShipTransformationManager().normals);
        if (!collider.seperated) {
            return this.handleActualCollision(collider, inWorldPos, inLocalPos, inWorldState, inLocalState);
        }

        return false;
    }

    // Takes the collision data along all axes generated prior, and creates the
    // ideal value that is to be followed
    private boolean handleActualCollision(PhysPolygonCollider collider, BlockPos inWorldPos, BlockPos inLocalPos,
                                          IBlockState inWorldState, IBlockState inLocalState) {
        PhysCollisionObject toCollideWith = collider.collisions[1];

        if (toCollideWith.penetrationDistance > AXIS_TOLERANCE || toCollideWith.penetrationDistance < -AXIS_TOLERANCE) {
            toCollideWith = collider.collisions[collider.minDistanceIndex];
        }

        // NestedBoolean didBlockBreakInShip = new NestedBoolean(false);
        // NestedBoolean didBlockBreakInWorld = new NestedBoolean(false);

        Vector positionInBody = collider.entity.getCenter();
        positionInBody.subtract(this.parent.getWrapperEntity().posX, this.parent.getWrapperEntity().posY,
                this.parent.getWrapperEntity().posZ);

        Vector velocityAtPoint = this.calculator.getVelocityAtPoint(positionInBody);

        double impulseApplied = 1D;
        // BlockRammingManager.processBlockRamming(parent.wrapper, collisionSpeed,
        // inLocalState, inWorldState, inLocalPos, inWorldPos, didBlockBreakInShip,
        // didBlockBreakInWorld);

        Vector[] collisionPoints = PolygonCollisionPointFinder.getPointsOfCollisionForPolygons(toCollideWith);

        impulseApplied /= collisionPoints.length;

        for (Vector collisionPos : collisionPoints) {
            Vector inBody = collisionPos.getSubtraction(new Vector(this.parent.getWrapperEntity().posX,
                    this.parent.getWrapperEntity().posY, this.parent.getWrapperEntity().posZ));
            inBody.multiply(-1D);
            Vector momentumAtPoint = this.calculator.getVelocityAtPoint(inBody);
            Vector axis = toCollideWith.collision_normal;
            Vector offsetVector = toCollideWith.getResponse();
            this.calculateCollisionImpulseForce(inBody, momentumAtPoint, axis, offsetVector, false, false, impulseApplied);
        }

        return false;
    }

    // Finally, the end of all this spaghetti code! This step takes all of the math
    // generated before, and it directly adds the result to Ship velocities
    private void calculateCollisionImpulseForce(Vector inBody, Vector velocityAtPointOfCollision, Vector axis,
                                                Vector offsetVector, boolean didBlockBreakInShip, boolean didBlockBreakInWorld, double impulseApplied) {
        Vector firstCross = inBody.cross(axis);
        RotationMatrices.applyTransform3by3(this.calculator.getPhysInvMOITensor(), firstCross);

        Vector secondCross = firstCross.cross(inBody);

        double impulseMagnitude = -velocityAtPointOfCollision.dot(axis)
                / (this.calculator.getInvMass() + secondCross.dot(axis));

        // Below this speed our collision coefficient of restitution is zero.
        double slopR = .5D;
        double collisionSpeed = Math.abs(velocityAtPointOfCollision.dot(axis));
        if (collisionSpeed > slopR) {
            impulseMagnitude *= (1 + COEFFICIENT_OF_RESTITUTION);
        } else {
            // TODO: Need to reduce this value by some factor
            // impulseMagnitude *= .5D;
        }

        Vector collisionImpulseForce = new Vector(axis, impulseMagnitude);

        // This is just an optimized way to add this force as quickly as possible.
        // Added collisionImpulseForce.dot(inBody) > 0 to force all collision to move in
        // the direction towards the in body vector.
        if (collisionImpulseForce.dot(offsetVector) < 0 && collisionImpulseForce.dot(inBody) < 0) {
            // collisionImpulseForce.multiply(1.8D);
            double collisionVelocity = velocityAtPointOfCollision.dot(axis);

            this.addFrictionToNormalForce(velocityAtPointOfCollision, collisionImpulseForce, inBody);
            this.calculator.linearMomentum.add(collisionImpulseForce);
            Vector thirdCross = inBody.cross(collisionImpulseForce);

            RotationMatrices.applyTransform3by3(this.calculator.getPhysInvMOITensor(), thirdCross);
            this.calculator.angularVelocity.add(thirdCross);
        }
    }

    // Applies the friction force generated by the collision.
    // The magnitude of this vector must be adjusted to minimize energy
    private void addFrictionToNormalForce(Vector momentumAtPoint, Vector impulseVector, Vector inBody) {
        Vector contactNormal = new Vector(impulseVector);
        contactNormal.normalize();

        Vector frictionVector = new Vector(momentumAtPoint);
        frictionVector.normalize();
        frictionVector.multiply(impulseVector.length() * KINETIC_FRICTION_COEFFICIENT);

        if (frictionVector.dot(momentumAtPoint) > 0) {
            frictionVector.multiply(-1D);
        }

        // Remove all friction components along the impulse vector
        double frictionImpulseDot = frictionVector.dot(contactNormal);
        Vector toRemove = contactNormal.getProduct(frictionImpulseDot);
        frictionVector.subtract(toRemove);


        double inertiaScalarAlongAxis = this.parent.getPhysicsProcessor().getInertiaAlongRotationAxis();
        // The change in velocity vector
        Vector initialVelocity = new Vector(this.parent.getPhysicsProcessor().linearMomentum,
                this.parent.getPhysicsProcessor().getInvMass());
        // Don't forget to multiply by delta t
        Vector deltaVelocity = new Vector(frictionVector,
                this.parent.getPhysicsProcessor().getInvMass() * this.parent.getPhysicsProcessor().getDragForPhysTick());

        double A = initialVelocity.lengthSq();
        double B = 2 * initialVelocity.dot(deltaVelocity);
        double C = deltaVelocity.lengthSq();

        Vector initialAngularVelocity = this.parent.getPhysicsProcessor().angularVelocity;
        Vector deltaAngularVelocity = inBody.cross(frictionVector);
        // This might need to be 1 / inertiaScalarAlongAxis
        deltaAngularVelocity.multiply(this.parent.getPhysicsProcessor().getDragForPhysTick() / inertiaScalarAlongAxis);

        double D = initialAngularVelocity.lengthSq();
        double E = 2 * deltaAngularVelocity.dot(initialAngularVelocity);
        double F = deltaAngularVelocity.lengthSq();

        // This is tied to PhysicsCalculations line 430
        if (initialAngularVelocity.lengthSq() < .05 && initialVelocity.lengthSq() < .05) {
            // Remove rotational friction if we are rotating slow enough
            D = E = F = 0;
        }

        // The coefficients of energy as a function of energyScaleFactor in the form (A
        // + B * k + c * k^2)
        double firstCoefficient = A * this.parent.getPhysicsProcessor().getMass() + D * inertiaScalarAlongAxis;
        double secondCoefficient = B * this.parent.getPhysicsProcessor().getMass() + E * inertiaScalarAlongAxis;
        double thirdCoefficient = C * this.parent.getPhysicsProcessor().getMass() + F * inertiaScalarAlongAxis;

        double scaleFactor = -secondCoefficient / (thirdCoefficient * 2);

        if (new Double(scaleFactor).isNaN()) {
            scaleFactor = 0;
        } else {
            scaleFactor = Math.max(0, Math.min(scaleFactor, 1));
            frictionVector.multiply(scaleFactor);
        }

        // System.out.println(scaleFactor);
        // ===== Friction Scaling Code End =====

        impulseVector.add(frictionVector);
    }

    // TODO: The greatest physics lag starts here.
    private void updatePotentialCollisionCache() {
        PhysicsShipTransform currentPhysicsTransform = (PhysicsShipTransform) this.parent.getShipTransformationManager()
                .getCurrentPhysicsTransform();
        // final AxisAlignedBB collisionBB = parent.getCollisionBoundingBox()
        // .expand(calculator.linearMomentum.X * calculator.getInvMass(),
        // calculator.linearMomentum.Y * calculator.getInvMass(),
        // calculator.linearMomentum.Z * calculator.getInvMass())
        // .grow(AABB_EXPANSION);
        // Use the physics tick collision box instead of the game tick collision box.
        AxisAlignedBB collisionBB = currentPhysicsTransform.getShipBoundingBox().grow(AABB_EXPANSION).expand(
                this.calculator.linearMomentum.X * this.calculator.getInvMass() * this.calculator.getPhysicsTimeDeltaPerPhysTick() * 5,
                this.calculator.linearMomentum.Y * this.calculator.getInvMass() * this.calculator.getPhysicsTimeDeltaPerPhysTick() * 5,
                this.calculator.linearMomentum.Z * this.calculator.getInvMass() * this.calculator.getPhysicsTimeDeltaPerPhysTick()
                        * 5);
        AxisAlignedBB shipBB = currentPhysicsTransform.getShipBoundingBox();
        this.ticksSinceCacheUpdate = 0D;
        // This is being used to occasionally offset the collision cache update, in the
        // hopes this will prevent multiple ships from all updating
        // in the same tick
        if (Math.random() > .5) {
            this.ticksSinceCacheUpdate -= .05D;
        }
        int oldSize = this.cachedPotentialHits.size();
        // Resets the potential hits array in O(1) time! Isn't that something.
        // cachedPotentialHits.resetQuick();
        this.cachedPotentialHits.clear();
        // Ship is outside of world blockSpace, just skip this all together
        if (collisionBB.maxY < 0 || collisionBB.minY > 255) {
            return;
        }

        // Has a -1 on the minY value, I hope this helps with preventing things from
        // falling through the floor
        BlockPos min = new BlockPos(collisionBB.minX, Math.max(collisionBB.minY - 1, 0), collisionBB.minZ);
        BlockPos max = new BlockPos(collisionBB.maxX, Math.min(collisionBB.maxY, 255), collisionBB.maxZ);
        this.centerPotentialHit = new BlockPos((min.getX() + max.getX()) / 2D, (min.getY() + max.getY()) / 2D,
                (min.getZ() + max.getZ()) / 2D);

        ChunkCache cache = this.parent.getCachedSurroundingChunks();

        if (cache == null) {
            System.err.println(
                    "VW Cached Surrounding Chunks was null! This is going to cause catastophric terrible events!!");
            return;
        }

        int chunkMinX = min.getX() >> 4;
        int chunkMaxX = (max.getX() >> 4) + 1;
        int chunkMinZ = min.getZ() >> 4;
        int chunkMaxZ = (max.getZ() >> 4) + 1;
        // long startTime = System.nanoTime();

        int minX = min.getX();
        int minY = min.getY();
        int minZ = min.getZ();
        int maxX = max.getX();
        int maxY = max.getY();
        int maxZ = max.getZ();

        // More multithreading!
        if (this.parent.getBlockPositions().size() > 100) {
            List<Tuple<Integer, Integer>> tasks = new ArrayList<>();

            for (int chunkX = chunkMinX; chunkX < chunkMaxX; chunkX++) {
                for (int chunkZ = chunkMinZ; chunkZ < chunkMaxZ; chunkZ++) {
                    tasks.add(new Tuple<>(chunkX, chunkZ));
                }
            }

            Consumer<Tuple<Integer, Integer>> consumer = i -> { // i is a Tuple<Integer, Integer>
                // updateCollisionCacheParrallel(cache, cachedPotentialHits, i.getFirst(),
                // i.getSecond(), minX, minY, minZ, maxX, maxY, maxZ);
                this.updateCollisionCacheSequential(cache, i.getFirst(), i.getSecond(), minX, minY, minZ, maxX, maxY, maxZ,
                        shipBB);
            };
            try {
                tasks.parallelStream().forEach(consumer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            for (int chunkX = chunkMinX; chunkX < chunkMaxX; chunkX++) {
                for (int chunkZ = chunkMinZ; chunkZ < chunkMaxZ; chunkZ++) {
                    this.updateCollisionCacheSequential(cache, chunkX, chunkZ, minX, minY, minZ, maxX, maxY, maxZ, shipBB);
                }
            }
        }
    }

    private void updateCollisionCacheParrallel(ChunkCache cache, Queue<Integer> dataQueue, int chunkX, int chunkZ,
                                               int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int arrayChunkX = chunkX - cache.chunkX;
        int arrayChunkZ = chunkZ - cache.chunkZ;

        if (cache.chunkArray[arrayChunkX][arrayChunkZ] != null && !(arrayChunkX < 0 || arrayChunkZ < 0
                || arrayChunkX > cache.chunkArray.length - 1 || arrayChunkZ > cache.chunkArray[0].length - 1)) {

            Vector temp1 = new Vector();
            Vector temp2 = new Vector();
            Vector temp3 = new Vector();

            Chunk chunk = cache.chunkArray[arrayChunkX][arrayChunkZ];
            for (int storageY = minY >> 4; storageY <= maxY >> 4; storageY++) {
                ExtendedBlockStorage extendedblockstorage = chunk.storageArrays[storageY];
                if (extendedblockstorage != null) {
                    int minStorageX = chunkX << 4;
                    int minStorageY = storageY << 4;
                    int minStorageZ = chunkZ << 4;

                    int maxStorageX = minStorageX + 16;
                    int maxStorageY = minStorageY + 16;
                    int maxStorageZ = minStorageZ + 16;

                    IBitOctreeProvider provider = IBitOctreeProvider.class.cast(extendedblockstorage.data);
                    IBitOctree octree = provider.getBitOctree();

                    if (USE_OCTREE_COLLISION) {
                        for (int levelThree = 0; levelThree < 8; levelThree++) {
                            int levelThreeIndex = octree.getOctreeLevelThreeIndex(levelThree);
                            if (octree.getAtIndex(levelThreeIndex)) {
                                for (int levelTwo = 0; levelTwo < 8; levelTwo++) {
                                    int levelTwoIndex = octree.getOctreeLevelTwoIndex(levelThreeIndex, levelTwo);
                                    if (octree.getAtIndex(levelTwoIndex)) {
                                        for (int levelOne = 0; levelOne < 8; levelOne++) {
                                            int levelOneIndex = octree.getOctreeLevelOneIndex(levelTwoIndex, levelOne);
                                            if (octree.getAtIndex(levelOneIndex)) {

                                                int baseX = ((levelThree % 2) * 8) + ((levelTwo % 2) * 4)
                                                        + ((levelOne % 2) * 2);
                                                int baseY = (((levelThree >> 1) % 2) * 8) + (((levelTwo >> 1) % 2) * 4)
                                                        + (((levelOne >> 1) % 2) * 2);
                                                int baseZ = (((levelThree >> 2) % 2) * 8) + (((levelTwo >> 2) % 2) * 4)
                                                        + (((levelOne >> 2) % 2) * 2);

                                                int x = baseX + minStorageX;
                                                int y = baseY + minStorageY;
                                                int z = baseZ + minStorageZ;

                                                dataQueue.add(0);

                                                if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ
                                                        && z <= maxZ && false) {
                                                    this.checkForCollision(x, y, z, extendedblockstorage, octree, temp1,
                                                            temp2, temp3, dataQueue);
                                                    this.checkForCollision(x, y, z + 1, extendedblockstorage, octree, temp1,
                                                            temp2, temp3, dataQueue);
                                                    this.checkForCollision(x, y + 1, z, extendedblockstorage, octree, temp1,
                                                            temp2, temp3, dataQueue);
                                                    this.checkForCollision(x, y + 1, z + 1, extendedblockstorage, octree,
                                                            temp1, temp2, temp3, dataQueue);
                                                    this.checkForCollision(x + 1, y, z, extendedblockstorage, octree, temp1,
                                                            temp2, temp3, dataQueue);
                                                    this.checkForCollision(x + 1, y, z + 1, extendedblockstorage, octree,
                                                            temp1, temp2, temp3, dataQueue);
                                                    this.checkForCollision(x + 1, y + 1, z, extendedblockstorage, octree,
                                                            temp1, temp2, temp3, dataQueue);
                                                    this.checkForCollision(x + 1, y + 1, z + 1, extendedblockstorage, octree,
                                                            temp1, temp2, temp3, dataQueue);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        for (int x = minStorageX; x < maxStorageX; x++) {
                            for (int y = minStorageY; y < maxStorageY; y++) {
                                for (int z = minStorageZ; z < maxStorageZ; z++) {
                                    this.checkForCollision(x, y, z, extendedblockstorage, octree, temp1, temp2, temp3,
                                            dataQueue);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateCollisionCacheSequential(ChunkCache cache, int chunkX, int chunkZ, int minX, int minY, int minZ,
                                                int maxX, int maxY, int maxZ, AxisAlignedBB shipBB) {
        int arrayChunkX = chunkX - cache.chunkX;
        int arrayChunkZ = chunkZ - cache.chunkZ;

        if (!(arrayChunkX < 0 || arrayChunkZ < 0 || arrayChunkX > cache.chunkArray.length - 1
                || arrayChunkZ > cache.chunkArray[0].length - 1)
                && cache.chunkArray[arrayChunkX][arrayChunkZ] != null) {

            Vector temp1 = new Vector();
            Vector temp2 = new Vector();
            Vector temp3 = new Vector();

            Chunk chunk = cache.chunkArray[arrayChunkX][arrayChunkZ];
            for (int storageY = minY >> 4; storageY <= maxY >> 4; storageY++) {
                ExtendedBlockStorage extendedblockstorage = chunk.storageArrays[storageY];
                if (extendedblockstorage != null) {
                    int minStorageX = chunkX << 4;
                    int minStorageY = storageY << 4;
                    int minStorageZ = chunkZ << 4;

                    int maxStorageX = minStorageX + 16;
                    int maxStorageY = minStorageY + 16;
                    int maxStorageZ = minStorageZ + 16;

                    IBitOctreeProvider provider = IBitOctreeProvider.class.cast(extendedblockstorage.data);
                    IBitOctree octree = provider.getBitOctree();

                    if (USE_OCTREE_COLLISION) {
                        for (int levelThree = 0; levelThree < 8; levelThree++) {
                            int levelThreeIndex = octree.getOctreeLevelThreeIndex(levelThree);
                            if (octree.getAtIndex(levelThreeIndex)) {
                                for (int levelTwo = 0; levelTwo < 8; levelTwo++) {
                                    int levelTwoIndex = octree.getOctreeLevelTwoIndex(levelThreeIndex, levelTwo);
                                    if (octree.getAtIndex(levelTwoIndex)) {
                                        for (int levelOne = 0; levelOne < 8; levelOne++) {
                                            int levelOneIndex = octree.getOctreeLevelOneIndex(levelTwoIndex, levelOne);
                                            if (octree.getAtIndex(levelOneIndex)) {

                                                int baseX = ((levelThree % 2) * 8) + ((levelTwo % 2) * 4)
                                                        + ((levelOne % 2) * 2);
                                                int baseY = (((levelThree >> 1) % 2) * 8) + (((levelTwo >> 1) % 2) * 4)
                                                        + (((levelOne >> 1) % 2) * 2);
                                                int baseZ = (((levelThree >> 2) % 2) * 8) + (((levelTwo >> 2) % 2) * 4)
                                                        + (((levelOne >> 2) % 2) * 2);

                                                int x = baseX + minStorageX;
                                                int y = baseY + minStorageY;
                                                int z = baseZ + minStorageZ;

                                                if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ
                                                        && z <= maxZ) {
                                                    this.checkForCollision(x, y, z, extendedblockstorage, octree, temp1,
                                                            temp2, temp3, shipBB);
                                                    this.checkForCollision(x, y, z + 1, extendedblockstorage, octree, temp1,
                                                            temp2, temp3, shipBB);
                                                    this.checkForCollision(x, y + 1, z, extendedblockstorage, octree, temp1,
                                                            temp2, temp3, shipBB);
                                                    this.checkForCollision(x, y + 1, z + 1, extendedblockstorage, octree,
                                                            temp1, temp2, temp3, shipBB);
                                                    this.checkForCollision(x + 1, y, z, extendedblockstorage, octree, temp1,
                                                            temp2, temp3, shipBB);
                                                    this.checkForCollision(x + 1, y, z + 1, extendedblockstorage, octree,
                                                            temp1, temp2, temp3, shipBB);
                                                    this.checkForCollision(x + 1, y + 1, z, extendedblockstorage, octree,
                                                            temp1, temp2, temp3, shipBB);
                                                    this.checkForCollision(x + 1, y + 1, z + 1, extendedblockstorage, octree,
                                                            temp1, temp2, temp3, shipBB);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        for (int x = minStorageX; x < maxStorageX; x++) {
                            for (int y = minStorageY; y < maxStorageY; y++) {
                                for (int z = minStorageZ; z < maxStorageZ; z++) {
                                    this.checkForCollision(x, y, z, extendedblockstorage, octree, temp1, temp2, temp3,
                                            shipBB);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkForCollision(int x, int y, int z, ExtendedBlockStorage storage, IBitOctree octree, Vector inLocal,
                                   Vector inBody, Vector speedInBody, AxisAlignedBB shipBB) {
        if (octree.get(x & 15, y & 15, z & 15)) {
            inLocal.X = x + .5D;
            inLocal.Y = y + .5D;
            inLocal.Z = z + .5D;
            // TODO: Something
            // parent.coordTransform.fromGlobalToLocal(inLocal);
            if (inLocal.X > shipBB.minX && inLocal.X < shipBB.maxX && inLocal.Y > shipBB.minY && inLocal.Y < shipBB.maxY
                    && inLocal.Z > shipBB.minZ && inLocal.Z < shipBB.maxZ) {
                this.parent.getShipTransformationManager().getCurrentPhysicsTransform().transform(inLocal,
                        TransformType.GLOBAL_TO_SUBSPACE);

                inBody.setSubtraction(inLocal, this.parent.getCenterCoord());
                // parent.physicsProcessor.setVectorToVelocityAtPoint(inBody, speedInBody);
                // speedInBody.multiply(-parent.physicsProcessor.getPhysicsTimeDeltaPerGameTick());

                // TODO: This isnt ideal, but we do gain a lot of performance.
                speedInBody.zero();

                // double RANGE_CHECK = 1;

                int minX, minY, minZ, maxX, maxY, maxZ;
                if (speedInBody.X > 0) {
                    minX = MathHelper.floor(inLocal.X - RANGE_CHECK);
                    maxX = MathHelper.floor(inLocal.X + RANGE_CHECK + speedInBody.X);
                } else {
                    minX = MathHelper.floor(inLocal.X - RANGE_CHECK + speedInBody.X);
                    maxX = MathHelper.floor(inLocal.X + RANGE_CHECK);
                }

                if (speedInBody.Y > 0) {
                    minY = MathHelper.floor(inLocal.Y - RANGE_CHECK);
                    maxY = MathHelper.floor(inLocal.Y + RANGE_CHECK + speedInBody.Y);
                } else {
                    minY = MathHelper.floor(inLocal.Y - RANGE_CHECK + speedInBody.Y);
                    maxY = MathHelper.floor(inLocal.Y + RANGE_CHECK);
                }

                if (speedInBody.Z > 0) {
                    minZ = MathHelper.floor(inLocal.Z - RANGE_CHECK);
                    maxZ = MathHelper.floor(inLocal.Z + RANGE_CHECK + speedInBody.Z);
                } else {
                    minZ = MathHelper.floor(inLocal.Z - RANGE_CHECK + speedInBody.Z);
                    maxZ = MathHelper.floor(inLocal.Z + RANGE_CHECK);
                }

                minY = Math.min(255, Math.max(minY, 0));
                maxY = Math.min(255, Math.max(maxY, 0));

                // int localX = MathHelper.floor(inLocal.X);
                // int localY = MathHelper.floor(inLocal.Y);
                // int localZ = MathHelper.floor(inLocal.Z);

                // tooTiredToName(localX, localY, localZ, x, y, z);
                // if (false)
                // maxX = Math.min(maxX, minX << 4);
                // maxZ = Math.min(maxZ, minZ << 4);

                if (this.parent.ownsChunk(minX >> 4, minZ >> 4) && this.parent.ownsChunk(maxX >> 4, maxZ >> 4)) {

                    Chunk chunkIn00 = this.parent.getChunkCache().getChunkAt(minX >> 4, minZ >> 4);
                    Chunk chunkIn01 = this.parent.getChunkCache().getChunkAt(minX >> 4, maxZ >> 4);
                    Chunk chunkIn10 = this.parent.getChunkCache().getChunkAt(maxX >> 4, minZ >> 4);
                    Chunk chunkIn11 = this.parent.getChunkCache().getChunkAt(maxX >> 4, maxZ >> 4);

                    breakThisLoop:
                    for (int localX = minX; localX < maxX; localX++) {
                        for (int localZ = minZ; localZ < maxZ; localZ++) {
                            Chunk theChunk = null;
                            if (localX >> 4 == minX >> 4) {
                                if (localZ >> 4 == minZ >> 4) {
                                    theChunk = chunkIn00;
                                } else {
                                    theChunk = chunkIn01;
                                }
                            } else {
                                if (localZ >> 4 == minZ >> 4) {
                                    theChunk = chunkIn10;
                                } else {
                                    theChunk = chunkIn11;
                                }
                            }
                            for (int localY = minY; localY < maxY; localY++) {
                                boolean result = this.checkForCollisionFast(theChunk, localX, localY, localZ, x, y, z);
                                if (result) {
                                    break breakThisLoop;
                                }

                                /*
                                 * if (false) // TODO: This code isn't thread safe. try { boolean result =
                                 * tooTiredToName(localX, localY, localZ, x, y, z); if (result) { break
                                 * breakThisLoop; } } catch (Exception e) { e.printStackTrace(); }
                                 */
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean checkForCollisionFast(Chunk chunk, int localX, int localY, int localZ,
                                          int x, int y, int z) {
        if (chunk.storageArrays[localY >> 4] != null) {
            IBitOctreeProvider provider = (IBitOctreeProvider) chunk.storageArrays[localY >> 4].getData();
            IBitOctree octreeInLocal = provider.getBitOctree();
            if (octreeInLocal.get(localX & 15, localY & 15, localZ & 15)) {
                int hash = SpatialDetector.getHashWithRespectTo(x, y, z, this.centerPotentialHit);
                // Sometimes we end up adding to the hits array in multiple threads at once,
                // crashing the physics.
                try {
                    this.cachedPotentialHits.add(hash);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
                // break outermostloop;
            }
            // }
        }
        return false;
    }

    private void checkForCollision(int x, int y, int z, ExtendedBlockStorage storage, IBitOctree octree, Vector inLocal,
                                   Vector inBody, Vector speedInBody, Collection<Integer> collection) {
        if (octree.get(x & 15, y & 15, z & 15)) {
            inLocal.X = x + .5D;
            inLocal.Y = y + .5D;
            inLocal.Z = z + .5D;
            // TODO: Something
            // parent.coordTransform.fromGlobalToLocal(inLocal);
            this.parent.getShipTransformationManager().getCurrentPhysicsTransform().transform(inLocal,
                    TransformType.GLOBAL_TO_SUBSPACE);

            inBody.setSubtraction(inLocal, this.parent.getCenterCoord());
            // parent.physicsProcessor.setVectorToVelocityAtPoint(inBody, speedInBody);
            // speedInBody.multiply(-parent.physicsProcessor.getPhysicsTimeDeltaPerGameTick());

            // TODO: This isnt ideal, but we do gain a lot of performance.
            speedInBody.zero();

            double RANGE_CHECK = 1.8;

            int minX, minY, minZ, maxX, maxY, maxZ;
            if (speedInBody.X > 0) {
                minX = MathHelper.floor(inLocal.X - RANGE_CHECK);
                maxX = MathHelper.floor(inLocal.X + RANGE_CHECK + speedInBody.X);
            } else {
                minX = MathHelper.floor(inLocal.X - RANGE_CHECK + speedInBody.X);
                maxX = MathHelper.floor(inLocal.X + RANGE_CHECK);
            }

            if (speedInBody.Y > 0) {
                minY = MathHelper.floor(inLocal.Y - RANGE_CHECK);
                maxY = MathHelper.floor(inLocal.Y + RANGE_CHECK + speedInBody.Y);
            } else {
                minY = MathHelper.floor(inLocal.Y - RANGE_CHECK + speedInBody.Y);
                maxY = MathHelper.floor(inLocal.Y + RANGE_CHECK);
            }

            if (speedInBody.Z > 0) {
                minZ = MathHelper.floor(inLocal.Z - RANGE_CHECK);
                maxZ = MathHelper.floor(inLocal.Z + RANGE_CHECK + speedInBody.Z);
            } else {
                minZ = MathHelper.floor(inLocal.Z - RANGE_CHECK + speedInBody.Z);
                maxZ = MathHelper.floor(inLocal.Z + RANGE_CHECK);
            }

            minY = Math.min(255, Math.max(minY, 0));

            outermostloop:
            for (int localX = minX; localX < maxX; localX++) {
                for (int localZ = minZ; localZ < maxZ; localZ++) {
                    for (int localY = minY; localY < maxY; localY++) {
                        if (this.parent.ownsChunk(localX >> 4, localZ >> 4)) {
                            Chunk chunkIn = this.parent.getChunkCache().getChunkAt(localX >> 4, localZ >> 4);
                            if (localY >> 4 < 16 && chunkIn.storageArrays[localY >> 4] != null) {
                                IBitOctreeProvider provider = IBitOctreeProvider.class
                                        .cast(chunkIn.storageArrays[localY >> 4].getData());
                                IBitOctree octreeInLocal = provider.getBitOctree();
                                if (octreeInLocal.get(localX & 15, localY & 15, localZ & 15)) {
                                    int hash = SpatialDetector.getHashWithRespectTo(x, y, z, this.centerPotentialHit);
                                    collection.add(hash);
                                    break outermostloop;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public BlockPos getCenterPotentialHit() {
        return this.centerPotentialHit;
    }

    public int getCachedPotentialHit(int offset) {
        return this.cachedPotentialHits.get(offset);
    }

    public int getCachedPotentialHitSize() {
        return this.cachedPotentialHits.size();
    }

    public PhysicsObject getParent() {
        return this.parent;
    }

}
