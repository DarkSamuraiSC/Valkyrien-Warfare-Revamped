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

package valkyrienwarfare.addon.combat.entity;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import valkyrienwarfare.math.Vector;

public class EntityCannonBall extends Entity {

    public float explosionPower;
    private int lerpSteps;
    private double boatPitch, lerpY, lerpZ, lerpXRot, boatYaw;

    public EntityCannonBall(World worldIn) {
        super(worldIn);
        this.setSize(.4F, .4F);
        this.explosionPower = 2f;
    }

    public EntityCannonBall(World worldIn, Vector velocityVector, Entity parent) {
        this(worldIn);
        this.motionX = velocityVector.X;
        this.motionY = velocityVector.Y;
        this.motionZ = velocityVector.Z;
        this.prevRotationYaw = this.rotationYaw = parent.rotationYaw;
        this.prevRotationPitch = this.rotationPitch = parent.rotationPitch;
        this.prevPosX = this.lastTickPosX = this.posX = parent.posX;
        this.prevPosY = this.lastTickPosY = this.posY = parent.posY;
        this.prevPosZ = this.lastTickPosZ = this.posZ = parent.posZ;
    }

    @Override
    protected void entityInit() {

    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        this.tickLerp();
        // if(!worldObj.isRemote){
        this.processMovementForTick();
        // }

    }

    private void processMovementForTick() {
        Vec3d origin = new Vec3d(this.posX, this.posY, this.posZ);
        Vec3d traceEnd = origin.addVector(this.motionX, this.motionY, this.motionZ);

        RayTraceResult traceResult = this.world.rayTraceBlocks(origin, traceEnd, false, true, false);

        if (traceResult == null || traceResult.typeOfHit == Type.MISS) {
            this.posX += this.motionX;
            this.posY += this.motionY;
            this.posZ += this.motionZ;

            double drag = Math.pow(.995D, 1D / 20D);
            this.motionX *= drag;
            this.motionY *= drag;
            this.motionZ *= drag;
            this.motionY -= .05;
        } else {
            if (traceResult.hitVec != null && !this.world.isRemote) {
                this.processCollision(traceResult);
                this.setDead();
            }
        }
    }

    private void processCollision(RayTraceResult collisionTrace) {
        this.world.createExplosion(this, collisionTrace.hitVec.x, collisionTrace.hitVec.y, collisionTrace.hitVec.z, this.explosionPower, true);
    }

    private void tickLerp() {
        if (this.lerpSteps > 0 && !this.canPassengerSteer()) {
            double d0 = this.posX + (this.boatPitch - this.posX) / (double) this.lerpSteps;
            double d1 = this.posY + (this.lerpY - this.posY) / (double) this.lerpSteps;
            double d2 = this.posZ + (this.lerpZ - this.posZ) / (double) this.lerpSteps;
            double d3 = MathHelper.wrapDegrees(this.boatYaw - (double) this.rotationYaw);
            this.rotationYaw = (float) ((double) this.rotationYaw + d3 / (double) this.lerpSteps);
            this.rotationPitch = (float) ((double) this.rotationPitch + (this.lerpXRot - (double) this.rotationPitch) / (double) this.lerpSteps);
            --this.lerpSteps;
            this.setPosition(d0, d1, d2);
            this.setRotation(this.rotationYaw, this.rotationPitch);
        }
    }

    /**
     * Set the position and rotation values directly without any clamping.
     */
    @SideOnly(Side.CLIENT)
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        // this.boatPitch = x;
        // this.lerpY = y;
        // this.lerpZ = z;
        // this.boatYaw = (double)yaw;
        // this.lerpXRot = (double)pitch;
        // this.lerpSteps = 0;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        // TODO Auto-generated method stub

    }

}
