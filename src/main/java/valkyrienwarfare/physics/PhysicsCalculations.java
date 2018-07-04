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

package valkyrienwarfare.physics;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import valkyrienwarfare.ValkyrienWarfareMod;
import valkyrienwarfare.addon.control.nodenetwork.INodeController;
import valkyrienwarfare.api.TransformType;
import valkyrienwarfare.deprecated_api.IBlockForceProvider;
import valkyrienwarfare.math.Quaternion;
import valkyrienwarfare.math.RotationMatrices;
import valkyrienwarfare.math.VWMath;
import valkyrienwarfare.math.Vector;
import valkyrienwarfare.mod.coordinates.ShipTransform;
import valkyrienwarfare.mod.multithreaded.PhysicsShipTransform;
import valkyrienwarfare.physics.collision.WorldPhysicsCollider;
import valkyrienwarfare.physics.management.PhysicsObject;
import valkyrienwarfare.physics.management.ShipTransformationManager;
import valkyrienwarfare.util.NBTUtils;
import valkyrienwarfare.util.PhysicsSettings;

import javax.vecmath.Matrix3d;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PhysicsCalculations {

    // Without this the physics feels too slow
    public static final double PHYSICS_SPEEDUP_FACTOR = 1.8D;
    public static final double DRAG_CONSTANT = .99D;
    public static final double INERTIA_OFFSET = .4D;
    public static final double EPSILON = 0xE - 8;

    private final PhysicsObject parent;
    private final WorldPhysicsCollider worldCollision;
    // CopyOnWrite to provide concurrency between threads.
    private final Set<BlockPos> activeForcePositions;
    public Vector gameTickCenterOfMass;
    public Vector linearMomentum;
    public Vector angularVelocity;
    private final Vector torque;
    private Vector physCenterOfMass;
    public boolean actAsArchimedes;
    private double gameTickMass;
    // TODO: Get this in one day
    // private double physMass;
    // The time occurring on each PhysTick
    private double physTickTimeDelta;
    private double[] gameMoITensor;
    private double[] physMOITensor;
    private double[] physInvMOITensor;
    private double physRoll, physPitch, physYaw;
    private double physX, physY, physZ;

    public PhysicsCalculations(PhysicsObject toProcess) {
        this.parent = toProcess;
        this.worldCollision = new WorldPhysicsCollider(this);

        this.gameMoITensor = RotationMatrices.getZeroMatrix(3);
        this.physMOITensor = RotationMatrices.getZeroMatrix(3);
        this.physInvMOITensor = RotationMatrices.getZeroMatrix(3);

        this.gameTickCenterOfMass = new Vector(toProcess.getCenterCoord());
        this.linearMomentum = new Vector();
        this.physCenterOfMass = new Vector();
        this.angularVelocity = new Vector();
        this.torque = new Vector();
        // We need thread safe access to this.
        this.activeForcePositions = ConcurrentHashMap.newKeySet();
    }

    public PhysicsCalculations(PhysicsCalculations toCopy) {
        this.parent = toCopy.parent;
        this.worldCollision = toCopy.worldCollision;
        this.gameTickCenterOfMass = toCopy.gameTickCenterOfMass;
        this.linearMomentum = toCopy.linearMomentum;
        this.angularVelocity = toCopy.angularVelocity;
        this.torque = toCopy.torque;
        this.gameTickMass = toCopy.gameTickMass;
        this.physTickTimeDelta = toCopy.physTickTimeDelta;
        this.activeForcePositions = toCopy.activeForcePositions;
        this.gameMoITensor = toCopy.gameMoITensor;
        this.physMOITensor = toCopy.physMOITensor;
        this.physInvMOITensor = toCopy.getPhysInvMOITensor();
        this.actAsArchimedes = toCopy.actAsArchimedes;
    }

    public void onSetBlockState(IBlockState oldState, IBlockState newState, BlockPos pos) {
        World worldObj = this.parent.getWorldObj();
        if (!newState.equals(oldState)) {
            if (oldState.getBlock() == Blocks.AIR) {
                if (BlockForce.basicForces.isBlockProvidingForce(newState, pos, worldObj)) {
                    this.activeForcePositions.add(pos);
                }
            } else {
                if (this.activeForcePositions.contains(pos)) {
                    if (!BlockForce.basicForces.isBlockProvidingForce(newState, pos, worldObj)) {
                        this.activeForcePositions.remove(pos);
                    }
                } else {
                    if (BlockForce.basicForces.isBlockProvidingForce(newState, pos, worldObj)) {
                        this.activeForcePositions.add(pos);
                    }
                }
            }
            if (newState.getBlock() == Blocks.AIR) {
                this.activeForcePositions.remove(pos);
            }

            double oldMass = BlockMass.basicMass.getMassFromState(oldState, pos, worldObj);
            double newMass = BlockMass.basicMass.getMassFromState(newState, pos, worldObj);
            double deltaMass = newMass - oldMass;
            // Don't change anything if the mass is the same
            if (Math.abs(deltaMass) > EPSILON) {
                double x = pos.getX() + .5D;
                double y = pos.getY() + .5D;
                double z = pos.getZ() + .5D;

                deltaMass /= 9D;
                this.addMassAt(x, y, z, deltaMass);
                this.addMassAt(x + INERTIA_OFFSET, y + INERTIA_OFFSET, z + INERTIA_OFFSET, deltaMass);
                this.addMassAt(x + INERTIA_OFFSET, y + INERTIA_OFFSET, z - INERTIA_OFFSET, deltaMass);
                this.addMassAt(x + INERTIA_OFFSET, y - INERTIA_OFFSET, z + INERTIA_OFFSET, deltaMass);
                this.addMassAt(x + INERTIA_OFFSET, y - INERTIA_OFFSET, z - INERTIA_OFFSET, deltaMass);
                this.addMassAt(x - INERTIA_OFFSET, y + INERTIA_OFFSET, z + INERTIA_OFFSET, deltaMass);
                this.addMassAt(x - INERTIA_OFFSET, y + INERTIA_OFFSET, z - INERTIA_OFFSET, deltaMass);
                this.addMassAt(x - INERTIA_OFFSET, y - INERTIA_OFFSET, z + INERTIA_OFFSET, deltaMass);
                this.addMassAt(x - INERTIA_OFFSET, y - INERTIA_OFFSET, z - INERTIA_OFFSET, deltaMass);
            }
        }
    }

    private void addMassAt(double x, double y, double z, double addedMass) {
        Vector prevCenterOfMass = new Vector(this.gameTickCenterOfMass);
        if (this.gameTickMass > .0001D) {
            this.gameTickCenterOfMass.multiply(this.gameTickMass);
            this.gameTickCenterOfMass.add(new Vector(x, y, z).getProduct(addedMass));
            this.gameTickCenterOfMass.multiply(1.0D / (this.gameTickMass + addedMass));
        } else {
            this.gameTickCenterOfMass = new Vector(x, y, z);
            this.gameMoITensor = RotationMatrices.getZeroMatrix(3);
        }
        double cmShiftX = prevCenterOfMass.X - this.gameTickCenterOfMass.X;
        double cmShiftY = prevCenterOfMass.Y - this.gameTickCenterOfMass.Y;
        double cmShiftZ = prevCenterOfMass.Z - this.gameTickCenterOfMass.Z;
        double rx = x - this.gameTickCenterOfMass.X;
        double ry = y - this.gameTickCenterOfMass.Y;
        double rz = z - this.gameTickCenterOfMass.Z;

        this.gameMoITensor[0] = this.gameMoITensor[0] + (cmShiftY * cmShiftY + cmShiftZ * cmShiftZ) * this.gameTickMass
                + (ry * ry + rz * rz) * addedMass;
        this.gameMoITensor[1] = this.gameMoITensor[1] - cmShiftX * cmShiftY * this.gameTickMass - rx * ry * addedMass;
        this.gameMoITensor[2] = this.gameMoITensor[2] - cmShiftX * cmShiftZ * this.gameTickMass - rx * rz * addedMass;
        this.gameMoITensor[3] = this.gameMoITensor[1];
        this.gameMoITensor[4] = this.gameMoITensor[4] + (cmShiftX * cmShiftX + cmShiftZ * cmShiftZ) * this.gameTickMass
                + (rx * rx + rz * rz) * addedMass;
        this.gameMoITensor[5] = this.gameMoITensor[5] - cmShiftY * cmShiftZ * this.gameTickMass - ry * rz * addedMass;
        this.gameMoITensor[6] = this.gameMoITensor[2];
        this.gameMoITensor[7] = this.gameMoITensor[5];
        this.gameMoITensor[8] = this.gameMoITensor[8] + (cmShiftX * cmShiftX + cmShiftY * cmShiftY) * this.gameTickMass
                + (rx * rx + ry * ry) * addedMass;

        // Do this to avoid a mass of zero, which runs the risk of dividing by zero and
        // crashing the program.
        if (this.gameTickMass + addedMass < .0001D) {
            this.gameTickMass = .0001D;
            this.parent.setPhysicsEnabled(false);
        } else {
            this.gameTickMass += addedMass;
        }
    }

    public void rawPhysTickPreCol(double newPhysSpeed) {
        if (this.parent.getShipTransformationManager().getCurrentPhysicsTransform() == ShipTransformationManager.ZERO_TRANSFORM) {
            // Create a new physics transform.
            this.physRoll = this.parent.getWrapperEntity().getRoll();
            this.physPitch = this.parent.getWrapperEntity().getPitch();
            this.physYaw = this.parent.getWrapperEntity().getYaw();
            this.physX = this.parent.getWrapperEntity().posX;
            this.physY = this.parent.getWrapperEntity().posY;
            this.physZ = this.parent.getWrapperEntity().posZ;
            this.physCenterOfMass.setValue(this.gameTickCenterOfMass);
            ShipTransform physicsTransform = new PhysicsShipTransform(this.physX, this.physY, this.physZ, this.physPitch, this.physYaw, this.physRoll,
                    this.physCenterOfMass, this.parent.getShipBoundingBox(),
                    this.parent.getShipTransformationManager().getCurrentTickTransform());
            this.parent.getShipTransformationManager().setCurrentPhysicsTransform(physicsTransform);
            this.parent.getShipTransformationManager().updatePreviousPhysicsTransform();
        }
        if (this.parent.isPhysicsEnabled()) {
            this.updatePhysSpeedAndIters(newPhysSpeed);
            this.updateParentCenterOfMass();
            this.calculateFramedMOITensor();
            if (!this.actAsArchimedes) {
                this.calculateForces();
            } else {
                this.calculateForcesArchimedes();
            }
        }
    }

    public void rawPhysTickPostCol() {
        if (!this.isPhysicsBroken()) {
            if (this.parent.isPhysicsEnabled()) {
                this.enforceStaticFriction();
                if (PhysicsSettings.doAirshipRotation) {
                    this.applyAngularVelocity();
                }
                if (PhysicsSettings.doAirshipMovement) {
                    this.applyLinearVelocity();
                }
            }
        } else {
            this.parent.setPhysicsEnabled(false);
            this.linearMomentum.zero();
            this.angularVelocity.zero();
        }

        PhysicsShipTransform finalPhysTransform = new PhysicsShipTransform(this.physX, this.physY, this.physZ, this.physPitch, this.physYaw,
                this.physRoll, this.physCenterOfMass, this.parent.getShipBoundingBox(),
                this.parent.getShipTransformationManager().getCurrentTickTransform());

        this.parent.getShipTransformationManager().updatePreviousPhysicsTransform();
        this.parent.getShipTransformationManager().setCurrentPhysicsTransform(finalPhysTransform);

        this.updatePhysCenterOfMass();
        // Moved out to VW Thread. Code run in this class should have no direct effect
        // on the physics object.
        // parent.coordTransform.updateAllTransforms(true, true);
    }

    // If the ship is moving at these speeds, its likely something in the physics
    // broke. This method helps detect that.
    private boolean isPhysicsBroken() {
        if (this.angularVelocity.lengthSq() > 50000 || this.linearMomentum.lengthSq() * this.getInvMass() * this.getInvMass() > 50000) {
            System.out.println("Ship tried moving too fast; freezing it and reseting velocities");
            return true;
        }
        return false;
    }

    /**
     * This method will set the linear and angular velocities to zero if both are too small.
     */
    private void enforceStaticFriction() {
        if (this.angularVelocity.lengthSq() < .001) {
            double linearSpeedSq = this.linearMomentum.lengthSq() * this.getInvMass() * this.getInvMass();
            if (linearSpeedSq < .05) {
                this.angularVelocity.zero();
                if (linearSpeedSq < .0001) {
                    this.linearMomentum.zero();
                }
            }
        }
    }

    // The x/y/z variables need to be updated when the centerOfMass location
    // changes.
    public void updateParentCenterOfMass() {
        Vector parentCM = this.parent.getCenterCoord();
        if (!this.parent.getCenterCoord().equals(this.gameTickCenterOfMass)) {
            Vector CMDif = this.gameTickCenterOfMass.getSubtraction(parentCM);
            // RotationMatrices.doRotationOnly(parent.coordTransform.lToWTransform, CMDif);

            this.parent.getShipTransformationManager().getCurrentPhysicsTransform().rotate(CMDif, TransformType.SUBSPACE_TO_GLOBAL);
            this.parent.getWrapperEntity().posX -= CMDif.X;
            this.parent.getWrapperEntity().posY -= CMDif.Y;
            this.parent.getWrapperEntity().posZ -= CMDif.Z;

            this.parent.getCenterCoord().setValue(this.gameTickCenterOfMass);
            // parent.coordTransform.updateAllTransforms(false, false);
        }
    }

    /**
     * Updates the physics center of mass to the game center of mass; does not do
     * any transformation updates on its own.
     */
    private void updatePhysCenterOfMass() {
        if (!this.physCenterOfMass.equals(this.gameTickCenterOfMass)) {
            Vector CMDif = this.physCenterOfMass.getSubtraction(this.gameTickCenterOfMass);
            // RotationMatrices.doRotationOnly(parent.coordTransform.lToWTransform, CMDif);

            this.parent.getShipTransformationManager().getCurrentPhysicsTransform().rotate(CMDif, TransformType.SUBSPACE_TO_GLOBAL);
            this.physX += CMDif.X;
            this.physY += CMDif.Y;
            this.physZ += CMDif.Z;

            this.physCenterOfMass.setValue(this.gameTickCenterOfMass);
        }
    }

    /**
     * Generates the rotated moment of inertia tensor with the body; uses the
     * following formula: I' = R * I * R-transpose; where I' is the rotated inertia,
     * I is unrotated interim, and R is the rotation matrix.
     */
    private void calculateFramedMOITensor() {
        double[] framedMOI = RotationMatrices.getZeroMatrix(3);

        double[] internalRotationMatrix = this.parent.getShipTransformationManager().getCurrentPhysicsTransform()
                .getInternalMatrix(TransformType.SUBSPACE_TO_GLOBAL);

        // Copy the rotation matrix, ignore the translation and scaling parts.
        Matrix3d rotationMatrix = new Matrix3d(internalRotationMatrix[0], internalRotationMatrix[1],
                internalRotationMatrix[2], internalRotationMatrix[4], internalRotationMatrix[5],
                internalRotationMatrix[6], internalRotationMatrix[8], internalRotationMatrix[9],
                internalRotationMatrix[10]);

        Matrix3d inertiaBodyFrame = new Matrix3d(this.gameMoITensor);
        // The product of the overall rotation matrix with the inertia tensor.
        inertiaBodyFrame.mul(rotationMatrix);
        rotationMatrix.transpose();
        // The product of the inertia tensor multiplied with the transpose of the
        // rotation transpose.
        inertiaBodyFrame.mul(rotationMatrix);
        framedMOI[0] = inertiaBodyFrame.m00;
        framedMOI[1] = inertiaBodyFrame.m01;
        framedMOI[2] = inertiaBodyFrame.m02;
        framedMOI[3] = inertiaBodyFrame.m10;
        framedMOI[4] = inertiaBodyFrame.m11;
        framedMOI[5] = inertiaBodyFrame.m12;
        framedMOI[6] = inertiaBodyFrame.m20;
        framedMOI[7] = inertiaBodyFrame.m21;
        framedMOI[8] = inertiaBodyFrame.m22;

        this.physMOITensor = framedMOI;
        this.physInvMOITensor = RotationMatrices.inverse3by3(framedMOI);
    }

    protected void calculateForces() {
        this.applyAirDrag();
        this.applyGravity();

        // Collections.shuffle(activeForcePositions);

        Vector blockForce = new Vector();
        Vector inBodyWO = new Vector();
        Vector crossVector = new Vector();
        World worldObj = this.parent.getWorldObj();

        if (PhysicsSettings.doPhysicsBlocks && this.parent.areShipChunksFullyLoaded()) {
            // We want to loop through all the physics nodes in a sorted order. Priority Queue handles that.
            Queue<INodeController> nodesPriorityQueue = new PriorityQueue<>();
            nodesPriorityQueue.addAll(this.parent.getPhysicsControllersInShip());

            while (nodesPriorityQueue.size() > 0) {
                INodeController controller = nodesPriorityQueue.poll();
                controller.onPhysicsTick(this.parent, this, this.physTickTimeDelta);
            }

            for (BlockPos pos : this.activeForcePositions) {
                IBlockState state = this.parent.getChunkCache().getBlockState(pos);
                Block blockAt = state.getBlock();
                VWMath.getBodyPosWithOrientation(pos, this.physCenterOfMass, this.parent.getShipTransformationManager()
                        .getCurrentPhysicsTransform().getInternalMatrix(TransformType.SUBSPACE_TO_GLOBAL), inBodyWO);

                BlockForce.basicForces.getForceFromState(state, pos, worldObj, this.physTickTimeDelta, this.parent,
                        blockForce);

                if (blockForce != null) {
                    if (blockAt instanceof IBlockForceProvider) {
                        Vector otherPosition = ((IBlockForceProvider) blockAt).getCustomBlockForcePosition(worldObj,
                                pos, state, this.parent.getWrapperEntity(), this.physTickTimeDelta);
                        if (otherPosition != null) {
                            VWMath.getBodyPosWithOrientation(otherPosition, this.gameTickCenterOfMass, this.parent.getShipTransformationManager()
                                            .getCurrentPhysicsTransform().getInternalMatrix(TransformType.SUBSPACE_TO_GLOBAL),
                                    inBodyWO);
                        }
                    }
                    this.addForceAtPoint(inBodyWO, blockForce, crossVector);
                }
            }
        }

        this.convertTorqueToVelocity();
    }

    public void applyGravity() {
        if (PhysicsSettings.doGravity) {
            this.addForceAtPoint(new Vector(0, 0, 0),
                    ValkyrienWarfareMod.gravity.getProduct(this.gameTickMass * this.physTickTimeDelta));
        }
    }

    public void calculateForcesArchimedes() {
        this.applyAirDrag();
    }

    protected void applyAirDrag() {
        double drag = this.getDragForPhysTick();
        this.linearMomentum.multiply(drag);
        this.angularVelocity.multiply(drag);
    }

    public void convertTorqueToVelocity() {
        if (!this.torque.isZero()) {
            this.angularVelocity.add(RotationMatrices.get3by3TransformedVec(this.getPhysInvMOITensor(), this.torque));
            this.torque.zero();
        }
    }

    public void addForceAtPoint(Vector inBodyWO, Vector forceToApply) {
        forceToApply.multiply(PHYSICS_SPEEDUP_FACTOR);
        this.torque.add(inBodyWO.cross(forceToApply));
        this.linearMomentum.add(forceToApply);
    }

    public void addForceAtPoint(Vector inBodyWO, Vector forceToApply, Vector crossVector) {
        forceToApply.multiply(PHYSICS_SPEEDUP_FACTOR);
        crossVector.setCross(inBodyWO, forceToApply);
        this.torque.add(crossVector);
        this.linearMomentum.add(forceToApply);
    }

    public void updatePhysSpeedAndIters(double newPhysSpeed) {
        this.physTickTimeDelta = newPhysSpeed;
    }

    public void applyAngularVelocity() {
        ShipTransformationManager coordTrans = this.parent.getShipTransformationManager();

        double[] rotationChange = RotationMatrices.getRotationMatrix(this.angularVelocity.X, this.angularVelocity.Y,
                this.angularVelocity.Z, this.angularVelocity.length() * this.physTickTimeDelta);
        Quaternion finalTransform = Quaternion.QuaternionFromMatrix(RotationMatrices.getMatrixProduct(rotationChange,
                coordTrans.getCurrentPhysicsTransform().getInternalMatrix(TransformType.SUBSPACE_TO_GLOBAL)));

        double[] radians = finalTransform.toRadians();

        this.physPitch = Double.isNaN(radians[0]) ? 0.0f : (float) Math.toDegrees(radians[0]);
        this.physYaw = Double.isNaN(radians[1]) ? 0.0f : (float) Math.toDegrees(radians[1]);
        this.physRoll = Double.isNaN(radians[2]) ? 0.0f : (float) Math.toDegrees(radians[2]);
    }

    public void applyLinearVelocity() {
        double momentMod = this.physTickTimeDelta * this.getInvMass();

        this.physX += (this.linearMomentum.X * momentMod);
        this.physY += (this.linearMomentum.Y * momentMod);
        this.physZ += (this.linearMomentum.Z * momentMod);
        this.physY = Math.min(Math.max(this.physY, ValkyrienWarfareMod.shipLowerLimit), ValkyrienWarfareMod.shipUpperLimit);
    }

    public Vector getVelocityAtPoint(Vector inBodyWO) {
        Vector speed = this.angularVelocity.cross(inBodyWO);
        double invMass = this.getInvMass();
        speed.X += (this.linearMomentum.X * invMass);
        speed.Y += (this.linearMomentum.Y * invMass);
        speed.Z += (this.linearMomentum.Z * invMass);
        return speed;
    }

    public void setVectorToVelocityAtPoint(Vector inBodyWO, Vector toSet) {
        toSet.setCross(this.angularVelocity, inBodyWO);
        double invMass = this.getInvMass();
        toSet.X += (this.linearMomentum.X * invMass);
        toSet.Y += (this.linearMomentum.Y * invMass);
        toSet.Z += (this.linearMomentum.Z * invMass);
    }

    public void writeToNBTTag(NBTTagCompound compound) {
        compound.setDouble("mass", this.gameTickMass);

        NBTUtils.writeVectorToNBT("linear", this.linearMomentum, compound);
        NBTUtils.writeVectorToNBT("angularVelocity", this.angularVelocity, compound);
        NBTUtils.writeVectorToNBT("CM", this.gameTickCenterOfMass, compound);

        NBTUtils.write3x3MatrixToNBT("MOI", this.gameMoITensor, compound);
    }

    public void readFromNBTTag(NBTTagCompound compound) {
        this.gameTickMass = compound.getDouble("mass");

        this.linearMomentum = NBTUtils.readVectorFromNBT("linear", compound);
        this.angularVelocity = NBTUtils.readVectorFromNBT("angularVelocity", compound);
        this.gameTickCenterOfMass = NBTUtils.readVectorFromNBT("CM", compound);

        this.gameMoITensor = NBTUtils.read3x3MatrixFromNBT("MOI", compound);
    }

    // Called upon a Ship being created from the World, and generates the physics
    // data for it
    public void processInitialPhysicsData() {
        IBlockState air = Blocks.AIR.getDefaultState();
        for (BlockPos pos : this.parent.getBlockPositions()) {
            this.onSetBlockState(air, this.parent.getChunkCache().getBlockState(pos), pos);
        }
    }

    // These getter methods guarantee that only code within this class can modify
    // the mass, preventing outside code from breaking things
    public double getMass() {
        return this.gameTickMass;
    }

    public double getInvMass() {
        return 1D / this.gameTickMass;
    }

    public double getPhysicsTimeDeltaPerPhysTick() {
        return this.physTickTimeDelta;
    }

    public double getDragForPhysTick() {
        return Math.pow(DRAG_CONSTANT, this.physTickTimeDelta * 20D);
    }

    public void addPotentialActiveForcePos(BlockPos pos) {
        this.activeForcePositions.add(pos);
    }

    /**
     * @return The inverse moment of inertia tensor with local translation (0 vector
     * is at the center of mass), but rotated into world coordinates.
     */
    public double[] getPhysInvMOITensor() {
        return this.physInvMOITensor;
    }

    /**
     * @param physInvMOITensor the physInvMOITensor to set
     */
    private void setPhysInvMOITensor(double[] physInvMOITensor) {
        this.physInvMOITensor = physInvMOITensor;
    }

    /**
     * @return The moment of inertia tensor with local translation (0 vector is at
     * the center of mass), but rotated into world coordinates.
     */
    public double[] getPhysMOITensor() {
        return this.physMOITensor;
    }

    /**
     * @return the parent
     */
    public PhysicsObject getParent() {
        return this.parent;
    }

    /**
     * @return the worldCollision
     */
    public WorldPhysicsCollider getWorldCollision() {
        return this.worldCollision;
    }

    public double getInertiaAlongRotationAxis() {
        Vector rotationAxis = new Vector(this.angularVelocity);
        rotationAxis.normalize();
        RotationMatrices.applyTransform3by3(this.getPhysMOITensor(), rotationAxis);
        return rotationAxis.length();
    }

}
