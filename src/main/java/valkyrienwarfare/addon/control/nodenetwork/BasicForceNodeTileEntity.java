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

package valkyrienwarfare.addon.control.nodenetwork;

import net.minecraft.nbt.NBTTagCompound;
import valkyrienwarfare.api.TransformType;
import valkyrienwarfare.math.Vector;
import valkyrienwarfare.physics.PhysicsCalculations;
import valkyrienwarfare.physics.management.PhysicsObject;
import valkyrienwarfare.physics.management.PhysicsWrapperEntity;
import valkyrienwarfare.util.NBTUtils;

public abstract class BasicForceNodeTileEntity extends BasicNodeTileEntity implements IForceTile {

    protected double maxThrust;
    protected double currentThrust;
    private Vector forceOutputVector;
    private Vector normalVelocityUnoriented;
    private int ticksSinceLastControlSignal;
    // Tells if the tile is in Ship Space, if it isn't then it doesn't try to find a
    // parent Ship object
    private boolean hasAlreadyCheckedForParent;

    /**
     * Only used for the NBT creation, other <init> calls should go through the
     * other constructors first
     */
    public BasicForceNodeTileEntity() {
        this.maxThrust = 5000D;
        this.currentThrust = 0D;
        this.forceOutputVector = new Vector();
        this.ticksSinceLastControlSignal = 0;
        this.hasAlreadyCheckedForParent = false;
    }

    public BasicForceNodeTileEntity(Vector normalVeclocityUnoriented, boolean isForceOutputOriented, double maxThrust) {
        this();
        this.normalVelocityUnoriented = normalVeclocityUnoriented;
        this.maxThrust = maxThrust;
    }

    /**
     * True for all engines except for Ether Compressors
     *
     * @return
     */
    public boolean isForceOutputOriented() {
        return true;
    }

    @Override
    public Vector getForceOutputNormal() {
        // TODO Auto-generated method stub
        return this.normalVelocityUnoriented;
    }

    @Override
    public Vector getForceOutputUnoriented(double secondsToApply, PhysicsObject physicsObject) {
        return this.normalVelocityUnoriented.getProduct(this.currentThrust * secondsToApply);
    }

    @Override
    public Vector getForceOutputOriented(double secondsToApply, PhysicsObject physicsObject) {
        Vector outputForce = this.getForceOutputUnoriented(secondsToApply, physicsObject);
        if (this.isForceOutputOriented()) {
            if (this.updateParentShip()) {
                this.getNode().getPhysicsObject().getShipTransformationManager().getCurrentTickTransform().rotate(outputForce, TransformType.SUBSPACE_TO_GLOBAL);
//                RotationMatrices.doRotationOnly(getNode().getPhysicsObject().coordTransform.lToWTransform, outputForce);
            }
        }
        return outputForce;
    }

    @Override
    public double getMaxThrust() {
        return this.maxThrust;
    }

    @Override
    public double getThrustActual() {
        return this.currentThrust;
    }

    @Override
    public double getThrustGoal() {
        return this.currentThrust;
    }

    @Override
    public void setThrustGoal(double newMagnitude) {
        this.currentThrust = newMagnitude;
    }

    @Override
    public Vector getPositionInLocalSpaceWithOrientation() {
        if (this.updateParentShip()) {
            return null;
        }
        PhysicsWrapperEntity parentShip = this.getNode().getPhysicsObject().getWrapperEntity();
        Vector engineCenter = new Vector(this.getPos().getX() + .5D, this.getPos().getY() + .5D, this.getPos().getZ() + .5D);
//        RotationMatrices.applyTransform(parentShip.wrapping.coordTransform.lToWTransform, engineCenter);
        parentShip.getPhysicsObject().getShipTransformationManager().getCurrentTickTransform().transform(engineCenter, TransformType.SUBSPACE_TO_GLOBAL);
        engineCenter.subtract(parentShip.posX, parentShip.posY, parentShip.posZ);
        return engineCenter;
    }

    @Override
    public Vector getVelocityAtEngineCenter() {
        if (this.updateParentShip()) {
            return null;
        }
        PhysicsCalculations calculations = this.getNode().getPhysicsObject().getPhysicsProcessor();
        return calculations.getVelocityAtPoint(this.getPositionInLocalSpaceWithOrientation());
    }

    @Override
    public Vector getLinearVelocityAtEngineCenter() {
        if (this.updateParentShip()) {
            return null;
        }
        PhysicsCalculations calculations = this.getNode().getPhysicsObject().getPhysicsProcessor();
        return calculations.linearMomentum;
    }

    @Override
    public Vector getAngularVelocityAtEngineCenter() {
        if (this.updateParentShip()) {
            return null;
        }
        PhysicsCalculations calculations = this.getNode().getPhysicsObject().getPhysicsProcessor();
        return calculations.angularVelocity.cross(this.getPositionInLocalSpaceWithOrientation());
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        this.maxThrust = compound.getDouble("maxThrust");
        this.currentThrust = compound.getDouble("currentThrust");
        this.normalVelocityUnoriented = NBTUtils.readVectorFromNBT("normalVelocityUnoriented", compound);
        this.ticksSinceLastControlSignal = compound.getInteger("ticksSinceLastControlSignal");
        super.readFromNBT(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setDouble("maxThrust", this.maxThrust);
        compound.setDouble("currentThrust", this.currentThrust);
        NBTUtils.writeVectorToNBT("normalVelocityUnoriented", this.normalVelocityUnoriented, compound);
        compound.setInteger("ticksSinceLastControlSignal", this.ticksSinceLastControlSignal);
        return super.writeToNBT(compound);
    }

    /**
     * Returns false if a parent Ship exists, and true if otherwise
     *
     * @return
     */
    public boolean updateParentShip() {
        return true;
    }

    public void updateTicksSinceLastRecievedSignal() {
        this.ticksSinceLastControlSignal = 0;
    }

    @Override
    public void update() {
        super.update();
        this.ticksSinceLastControlSignal++;
        if (this.ticksSinceLastControlSignal > 5) {
            this.setThrustGoal(this.getThrustActual() * .9D);
        }
    }

}
