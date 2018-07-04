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

package valkyrienwarfare.physics.collision.optimization;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import valkyrienwarfare.api.TransformType;
import valkyrienwarfare.math.Vector;
import valkyrienwarfare.mod.physmanagement.relocation.SpatialDetector;
import valkyrienwarfare.physics.collision.CollisionInformationHolder;
import valkyrienwarfare.physics.collision.WorldPhysicsCollider;
import valkyrienwarfare.physics.collision.polygons.PhysPolygonCollider;
import valkyrienwarfare.physics.collision.polygons.Polygon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

public class ShipCollisionTask implements Callable<Void> {

    public final static int MAX_TASKS_TO_CHECK = 45;
    private final WorldPhysicsCollider toTask;
    private final int taskStartIndex;
    private final int tasksToCheck;
    private final MutableBlockPos mutablePos;
    private final MutableBlockPos inLocalPos;
    private final Vector inWorld;
    private final List<CollisionInformationHolder> collisionInformationGenerated;
    private IBlockState inWorldState;
    // public TIntArrayList foundPairs = new TIntArrayList();

    public ShipCollisionTask(WorldPhysicsCollider toTask, int taskStartIndex) {
        this.taskStartIndex = taskStartIndex;
        this.toTask = toTask;
        this.mutablePos = new MutableBlockPos();
        this.inLocalPos = new MutableBlockPos();
        this.inWorld = new Vector();
        this.collisionInformationGenerated = new ArrayList<>();
        this.inWorldState = null;

        int size = toTask.getCachedPotentialHitSize();
        if (taskStartIndex + MAX_TASKS_TO_CHECK > size + 1) {
            this.tasksToCheck = size + 1 - taskStartIndex;
        } else {
            this.tasksToCheck = MAX_TASKS_TO_CHECK;
        }
    }

    @Override
    public Void call() {
        for (int index = this.taskStartIndex; index < this.tasksToCheck + 1; index++) {
            int integer = this.toTask.getCachedPotentialHit(index);
            this.processNumber(integer);
        }

        return null;
    }

    public List<CollisionInformationHolder> getCollisionInformationGenerated() {
        return this.collisionInformationGenerated;
    }

    /**
     * Returns an iterator that loops over the collision information in quasi-random
     * order. This is important to avoid biasing one side over another, because
     * otherwise one side would slowly sink into the ground.
     *
     * @return
     */
    public Iterator<CollisionInformationHolder> getCollisionInformationIterator() {
        // This is preventing the cpu from precaching the rest of the List, slowly things down considerably.
        if (false && this.collisionInformationGenerated.size() != 0) {
            return new QuasiRandomIterator(this.collisionInformationGenerated);
        } else {
            return this.collisionInformationGenerated.iterator();
        }
    }

    private void processNumber(int integer) {
        SpatialDetector.setPosWithRespectTo(integer, this.toTask.getCenterPotentialHit(), this.mutablePos);
        this.inWorldState = this.toTask.getParent().getCachedSurroundingChunks().getBlockState(this.mutablePos);

        this.inWorld.X = this.mutablePos.getX() + .5;
        this.inWorld.Y = this.mutablePos.getY() + .5;
        this.inWorld.Z = this.mutablePos.getZ() + .5;

//        toTask.getParent().coordTransform.fromGlobalToLocal(inWorld);
        this.toTask.getParent().getShipTransformationManager().getCurrentPhysicsTransform().transform(this.inWorld, TransformType.GLOBAL_TO_SUBSPACE);

        int midX = MathHelper.floor(this.inWorld.X + .5D);
        int midY = MathHelper.floor(this.inWorld.Y + .5D);
        int midZ = MathHelper.floor(this.inWorld.Z + .5D);

        // Check the 27 possible positions
        this.checkPosition(midX - 1, midY - 1, midZ - 1, integer);
        this.checkPosition(midX - 1, midY - 1, midZ, integer);
        this.checkPosition(midX - 1, midY - 1, midZ + 1, integer);
        this.checkPosition(midX - 1, midY, midZ - 1, integer);
        this.checkPosition(midX - 1, midY, midZ, integer);
        this.checkPosition(midX - 1, midY, midZ + 1, integer);
        this.checkPosition(midX - 1, midY + 1, midZ - 1, integer);
        this.checkPosition(midX - 1, midY + 1, midZ, integer);
        this.checkPosition(midX - 1, midY + 1, midZ + 1, integer);

        this.checkPosition(midX, midY - 1, midZ - 1, integer);
        this.checkPosition(midX, midY - 1, midZ, integer);
        this.checkPosition(midX, midY - 1, midZ + 1, integer);
        this.checkPosition(midX, midY, midZ - 1, integer);
        this.checkPosition(midX, midY, midZ, integer);
        this.checkPosition(midX, midY, midZ + 1, integer);
        this.checkPosition(midX, midY + 1, midZ - 1, integer);
        this.checkPosition(midX, midY + 1, midZ, integer);
        this.checkPosition(midX, midY + 1, midZ + 1, integer);

        this.checkPosition(midX + 1, midY - 1, midZ - 1, integer);
        this.checkPosition(midX + 1, midY - 1, midZ, integer);
        this.checkPosition(midX + 1, midY - 1, midZ + 1, integer);
        this.checkPosition(midX + 1, midY, midZ - 1, integer);
        this.checkPosition(midX + 1, midY, midZ, integer);
        this.checkPosition(midX + 1, midY, midZ + 1, integer);
        this.checkPosition(midX + 1, midY + 1, midZ - 1, integer);
        this.checkPosition(midX + 1, midY + 1, midZ, integer);
        this.checkPosition(midX + 1, midY + 1, midZ + 1, integer);

    }

    public void checkPosition(int x, int y, int z, int positionHash) {
        Chunk chunkIn = this.toTask.getParent().getChunkCache().getChunkAt(x >> 4, z >> 4);
        y = Math.max(0, Math.min(y, 255));

        ExtendedBlockStorage storage = chunkIn.storageArrays[y >> 4];
        if (storage != null) {
            IBitOctreeProvider provider = IBitOctreeProvider.class.cast(storage.data);
            IBitOctree octree = provider.getBitOctree();

            if (octree.get(x & 15, y & 15, z & 15)) {
                IBlockState inLocalState = chunkIn.getBlockState(x, y, z);
                // Only if you want to stop short
                // foundPairs.add(positionHash);
                // foundPairs.add(x);
                // foundPairs.add(y);
                // foundPairs.add(z);

                this.inLocalPos.setPos(x, y, z);

                AxisAlignedBB inLocalBB = new AxisAlignedBB(this.inLocalPos.getX(), this.inLocalPos.getY(), this.inLocalPos.getZ(),
                        this.inLocalPos.getX() + 1, this.inLocalPos.getY() + 1, this.inLocalPos.getZ() + 1);
                AxisAlignedBB inGlobalBB = new AxisAlignedBB(this.mutablePos.getX(), this.mutablePos.getY(), this.mutablePos.getZ(),
                        this.mutablePos.getX() + 1, this.mutablePos.getY() + 1, this.mutablePos.getZ() + 1);

                // This changes the box bounding box to the real bounding box, not sure if this
                // is better or worse for this mod
                // List<AxisAlignedBB> colBB = worldObj.getCollisionBoxes(inLocalBB);
                // inLocalBB = colBB.get(0);

                Polygon shipInWorld = new Polygon(inLocalBB,
                        this.toTask.getParent().getShipTransformationManager().getCurrentPhysicsTransform(), TransformType.SUBSPACE_TO_GLOBAL);
                Polygon worldPoly = new Polygon(inGlobalBB);

                // TODO: Remove the normals crap
                PhysPolygonCollider collider = new PhysPolygonCollider(shipInWorld, worldPoly,
                        this.toTask.getParent().getShipTransformationManager().normals);

                if (!collider.seperated) {
                    // return handleActualCollision(collider, mutablePos, inLocalPos, inWorldState,
                    // inLocalState);
                    CollisionInformationHolder holder = new CollisionInformationHolder(collider, this.mutablePos.getX(),
                            this.mutablePos.getY(), this.mutablePos.getZ(), this.inLocalPos.getX(), this.inLocalPos.getY(),
                            this.inLocalPos.getZ(), this.inWorldState, inLocalState);

                    this.collisionInformationGenerated.add(holder);
                }
            }
        }
    }

    public WorldPhysicsCollider getToTask() {
        return this.toTask;
    }

    /**
     * Quasi-Random Iterator that uses a linear congruential generator to generate
     * the order of iteration.
     *
     * @param <E>
     * @author thebest108
     */
    private class QuasiRandomIterator<E> implements Iterator<E> {

        // Any large prime number works here.
        private final static int c = 65537;
        private final List<E> internalList;
        private final int startIndex;
        private int index;
        private boolean isFinished;

        /**
         * Creates a new quasi random iterator for the given list, the list passed must
         * not be empty otherwise an IllegalArgumentException is thrown.
         *
         * @param list
         */
        QuasiRandomIterator(List<E> list) {
            if (list.size() == 0) {
                throw new IllegalArgumentException();
            }
            this.internalList = list;
            this.isFinished = false;
            // Start the index at a random value between 0 <= x < list.size()
            this.startIndex = (int) (Math.random() * list.size());
            this.index = this.startIndex;
        }

        @Override
        public boolean hasNext() {
            return !this.isFinished;
        }

        @Override
        public E next() {
            int oldIndex = this.index;
            this.advanceIndex();
            return this.internalList.get(oldIndex);
        }

        /**
         * Sets index to be the next value in the linear congruential generator. Also
         * marks the iterator as finished once a full period has occured.
         */
        private void advanceIndex() {
            this.index = (this.index + c) % this.internalList.size();
            // Stop the iterator after we've been over every element.
            if (this.index == this.startIndex) {
                this.isFinished = true;
            }
        }

    }

}
