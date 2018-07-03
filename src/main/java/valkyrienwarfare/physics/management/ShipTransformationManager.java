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

package valkyrienwarfare.physics.management;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.border.WorldBorder;
import valkyrienwarfare.ValkyrienWarfareMod;
import valkyrienwarfare.api.TransformType;
import valkyrienwarfare.math.RotationMatrices;
import valkyrienwarfare.math.Vector;
import valkyrienwarfare.mod.coordinates.ShipTransform;
import valkyrienwarfare.mod.multithreaded.PhysicsShipTransform;
import valkyrienwarfare.mod.network.PhysWrapperPositionMessage;

import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Stores various coordinates and transforms for the ship.
 *
 * @author thebest108
 */
public class ShipTransformationManager {

    // A transformation that does no rotation, and does no translation.
    public static final ShipTransform ZERO_TRANSFORM = new ShipTransform();
    // A buffer to hold ship transform data sent from server to the client.
    public final ShipTransformationBuffer serverBuffer;
    private final PhysicsObject parent;
    public Vector[] normals;
    private ShipTransform currentTickTransform;
    private ShipTransform renderTransform;
    private ShipTransform prevTickTransform;
    // Used exclusively by the physics engine; should never even be used by the
    // client.
    private ShipTransform currentPhysicsTransform;
    private ShipTransform prevPhysicsTransform;

    public ShipTransformationManager(PhysicsObject parent) {
        this.parent = parent;
        this.currentTickTransform = ZERO_TRANSFORM;
        this.renderTransform = ZERO_TRANSFORM;
        this.prevTickTransform = ZERO_TRANSFORM;
        this.currentPhysicsTransform = ZERO_TRANSFORM;
        this.prevPhysicsTransform = ZERO_TRANSFORM;
        this.updateAllTransforms(true, true);
        this.normals = Vector.generateAxisAlignedNorms();
        this.serverBuffer = new ShipTransformationBuffer();
    }

    /**
     * Polls position and rotation data from the parent ship, and creates a new
     * current transform made from this data.
     */
    public void updateCurrentTickTransform() {
        double[] lToWTransform = RotationMatrices.getTranslationMatrix(this.parent.getWrapperEntity().posX, this.parent.getWrapperEntity().posY,
                this.parent.getWrapperEntity().posZ);
        lToWTransform = RotationMatrices.rotateAndTranslate(lToWTransform, this.parent.getWrapperEntity().getPitch(),
                this.parent.getWrapperEntity().getYaw(), this.parent.getWrapperEntity().getRoll(), this.parent.getCenterCoord());
        this.currentTickTransform = new ShipTransform(lToWTransform);
    }

    public void updateRenderTransform(double x, double y, double z, double pitch, double yaw, double roll) {
        double[] RlToWTransform = RotationMatrices.getTranslationMatrix(x, y, z);
        RlToWTransform = RotationMatrices.rotateAndTranslate(RlToWTransform, pitch, yaw, roll, this.parent.getCenterCoord());
        this.renderTransform = new ShipTransform(RlToWTransform);
    }

    /**
     * Sets the previous transform to the current transform.
     */
    public void updatePrevTickTransform() {
        // Transformation objects are immutable, so this is 100% safe!
        this.prevTickTransform = this.currentTickTransform;
    }

    /**
     * Updates all the transformations, only updates the AABB if passed true.
     *
     * @param updateParentAABB
     */
    @Deprecated
    public void updateAllTransforms(boolean updateParentAABB, boolean updatePassengers) {
        // The client should never be updating the AABB on its own.
        if (this.parent.getWorldObj().isRemote) {
            updateParentAABB = false;
        }
        this.forceShipIntoWorldBorder();
        this.updateCurrentTickTransform();
        if (updateParentAABB) {
            this.updateParentAABB();
        }
        this.updateParentNormals();
        if (updatePassengers) {
            this.updatePassengerPositions();
        }
    }

    /**
     * Keeps the Ship in the world border
     */
    private void forceShipIntoWorldBorder() {
        WorldBorder border = this.parent.getWorldObj().getWorldBorder();
        AxisAlignedBB shipBB = this.parent.getShipBoundingBox();

        if (shipBB.maxX > border.maxX()) {
            this.parent.getWrapperEntity().posX += border.maxX() - shipBB.maxX;
        }
        if (shipBB.minX < border.minX()) {
            this.parent.getWrapperEntity().posX += border.minX() - shipBB.minX;
        }
        if (shipBB.maxZ > border.maxZ()) {
            this.parent.getWrapperEntity().posZ += border.maxZ() - shipBB.maxZ;
        }
        if (shipBB.minZ < border.minZ()) {
            this.parent.getWrapperEntity().posZ += border.minZ() - shipBB.minZ;
        }
    }

    public void updatePassengerPositions() {
        for (Entity entity : this.parent.getWrapperEntity().riddenByEntities) {
            this.parent.getWrapperEntity().updatePassenger(entity);
        }
    }

    public void sendPositionToPlayers(int positionTickID) {
        PhysWrapperPositionMessage posMessage = null;
        if (this.currentPhysicsTransform != ZERO_TRANSFORM) {
            posMessage = new PhysWrapperPositionMessage((PhysicsShipTransform) this.currentPhysicsTransform,
                    this.parent.getWrapperEntity().getEntityId(), positionTickID);
        } else {
            posMessage = new PhysWrapperPositionMessage(this.parent.getWrapperEntity(), positionTickID);
        }

        // Do a standard loop here to avoid a concurrentModificationException. A standard for each loop could cause a crash.
        for (int i = 0; i < this.parent.getWatchingPlayers().size(); i++) {
            EntityPlayerMP player = this.parent.getWatchingPlayers().get(i);
            if (player != null) {
                ValkyrienWarfareMod.physWrapperNetwork.sendTo(posMessage, player);
            }
        }
    }

    public void updateParentNormals() {
        this.normals = new Vector[15];
        // Used to generate Normals for the Axis Aligned World
        Vector[] alignedNorms = Vector.generateAxisAlignedNorms();
        Vector[] rotatedNorms = this.generateRotationNormals();
        for (int i = 0; i < 6; i++) {
            Vector currentNorm;
            if (i < 3) {
                currentNorm = alignedNorms[i];
            } else {
                currentNorm = rotatedNorms[i - 3];
            }
            this.normals[i] = currentNorm;
        }
        int cont = 6;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Vector norm = this.normals[i].crossAndUnit(this.normals[j + 3]);
                this.normals[cont] = norm;
                cont++;
            }
        }
        for (int i = 0; i < this.normals.length; i++) {
            if (this.normals[i].isZero()) {
                this.normals[i] = new Vector(0.0D, 1.0D, 0.0D);
            }
        }
        this.normals[0] = new Vector(1.0D, 0.0D, 0.0D);
        this.normals[1] = new Vector(0.0D, 1.0D, 0.0D);
        this.normals[2] = new Vector(0.0D, 0.0D, 1.0D);
    }

    public Vector[] generateRotationNormals() {
        Vector[] norms = Vector.generateAxisAlignedNorms();
        for (int i = 0; i < 3; i++) {
            this.currentTickTransform.rotate(norms[i], TransformType.SUBSPACE_TO_GLOBAL);
        }
        return norms;
    }

    public Vector[] getSeperatingAxisWithShip(PhysicsObject other) {
        // Note: This Vector array still contains potential 0 vectors, those are removed
        // later
        Vector[] normals = new Vector[15];
        Vector[] otherNorms = other.getShipTransformationManager().normals;
        Vector[] rotatedNorms = normals;
        for (int i = 0; i < 6; i++) {
            if (i < 3) {
                normals[i] = otherNorms[i];
            } else {
                normals[i] = rotatedNorms[i - 3];
            }
        }
        int cont = 6;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Vector norm = normals[i].crossAndUnit(normals[j + 3]);
                if (!norm.isZero()) {
                    normals[cont] = norm;
                } else {
                    normals[cont] = normals[1];
                }
                cont++;
            }
        }
        return normals;
    }

    // TODO: Use Octrees to optimize this, or more preferably QuickHull3D.
    public void updateParentAABB() {
        CollisionBBConsumer convexHullConsumer = new CollisionBBConsumer();
        Stream<BlockPos> parentPositionsStream = null;
        if (this.parent.getBlockPositions().size() < 300) {
            // If its a small ship use a sequential stream.
            parentPositionsStream = this.parent.getBlockPositions().stream();
        } else {
            // If its a big ship then we destroy the cpu consumption and go fully
            // multithreaded!
            parentPositionsStream = this.parent.getBlockPositions().parallelStream();
        }
        parentPositionsStream.forEach(convexHullConsumer);
        this.parent.setShipBoundingBox(convexHullConsumer.createWrappingAABB());
    }

    /**
     * Transforms a vector from global coordinates to local coordinates, using the
     * getCurrentTickTransform()
     *
     * @param inGlobal
     */
    public void fromGlobalToLocal(Vector inGlobal) {
        this.currentTickTransform.transform(inGlobal, TransformType.GLOBAL_TO_SUBSPACE);
    }

    /**
     * Transforms a vector from local coordinates to global coordinates, using the
     * getCurrentTickTransform()
     *
     * @param inLocal
     */
    public void fromLocalToGlobal(Vector inLocal) {
        this.currentTickTransform.transform(inLocal, TransformType.SUBSPACE_TO_GLOBAL);
    }

    /**
     * @return the current transformation being used this tick.
     */
    public ShipTransform getCurrentTickTransform() {
        return this.currentTickTransform;
    }

    /**
     * @param currentTransform the currentTransform to set
     */
    @Deprecated
    private void setCurrentTickTransform(ShipTransform currentTransform) {
        this.currentTickTransform = currentTransform;
    }

    /**
     * @return the renderTransform
     */
    public ShipTransform getRenderTransform() {
        return this.renderTransform;
    }

    /**
     * @param renderTransform the renderTransform to set
     */
    @Deprecated
    private void setRenderTransform(ShipTransform renderTransform) {
        this.renderTransform = renderTransform;
    }

    /**
     * @return the prevTransform
     */
    public ShipTransform getPrevTickTransform() {
        return this.prevTickTransform;
    }

    /**
     * @param prevTransform the prevTransform to set
     */
    private void setPrevTickTransform(ShipTransform prevTransform) {
        this.prevTickTransform = prevTransform;
    }

    /**
     * Returns the transformation data used for physics processing.
     *
     * @return the physics transform
     */
    public ShipTransform getCurrentPhysicsTransform() {
        return this.currentPhysicsTransform;
    }

    /**
     * Sets the physics transform to the given input.
     *
     * @param physicsTransform
     */
    public void setCurrentPhysicsTransform(ShipTransform currentPhysicsTransform) {
        this.currentPhysicsTransform = currentPhysicsTransform;
    }

    public ShipTransform getPrevPhysicsTransform() {
        return this.prevPhysicsTransform;
    }

    public void updatePreviousPhysicsTransform() {
        this.prevPhysicsTransform = this.currentPhysicsTransform;
    }

    private class CollisionBBConsumer implements Consumer<BlockPos> {
        private static final double AABB_EXPANSION = 1.6D;
        private final double[] M = ShipTransformationManager.this.getCurrentTickTransform().getInternalMatrix(TransformType.SUBSPACE_TO_GLOBAL);
        double minX, minY, minZ, maxX, maxY, maxZ;

        CollisionBBConsumer() {
            this.minX = ShipTransformationManager.this.parent.getWrapperEntity().posX;
            this.minY = ShipTransformationManager.this.parent.getWrapperEntity().posY;
            this.minZ = ShipTransformationManager.this.parent.getWrapperEntity().posZ;
            this.maxX = ShipTransformationManager.this.parent.getWrapperEntity().posX;
            this.maxY = ShipTransformationManager.this.parent.getWrapperEntity().posY;
            this.maxZ = ShipTransformationManager.this.parent.getWrapperEntity().posZ;
        }

        @Override
        public void accept(BlockPos pos) {
            double x = pos.getX() + .5D;
            double y = pos.getY() + .5D;
            double z = pos.getZ() + .5D;

            double newX = x * this.M[0] + y * this.M[1] + z * this.M[2] + this.M[3];
            double newY = x * this.M[4] + y * this.M[5] + z * this.M[6] + this.M[7];
            double newZ = x * this.M[8] + y * this.M[9] + z * this.M[10] + this.M[11];

            this.minX = Math.min(newX, this.minX);
            this.maxX = Math.max(newX, this.maxX);
            this.minY = Math.min(newY, this.minY);
            this.maxY = Math.max(newY, this.maxY);
            this.minZ = Math.min(newZ, this.minZ);
            this.maxZ = Math.max(newZ, this.maxZ);
        }

        AxisAlignedBB createWrappingAABB() {
            return new AxisAlignedBB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ).grow(AABB_EXPANSION);
        }

    }
}
