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

package valkyrienwarfare.mod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import valkyrienwarfare.math.Vector;
import valkyrienwarfare.mod.multithreaded.PhysicsShipTransform;
import valkyrienwarfare.physics.management.PhysicsObject;
import valkyrienwarfare.physics.management.PhysicsWrapperEntity;

/**
 * This IMessage sends all the position rotation data of a PhysicsObject from
 * the server to the client. Usually the data sent from one of these packets is
 * coming from the physics tick and isn't exactly the same as the game tick;
 * this is done so that the client can see ship movement smoothly even when the
 * server game tick is lagging.
 *
 * @author thebest108
 */
public class PhysWrapperPositionMessage implements IMessage {

    private int entityID;
    private double posX, posY, posZ;
    private double pitch, yaw, roll;
    private Vector centerOfMass;
    private int relativeTick;
    private AxisAlignedBB shipBB;

    public PhysWrapperPositionMessage() {
    }

    public PhysWrapperPositionMessage(PhysicsShipTransform transformData, int entityID, int relativeTick) {
        this.entityID = entityID;
        this.relativeTick = relativeTick;
        this.shipBB = transformData.getShipBoundingBox();
        this.posX = transformData.getPosX();
        this.posY = transformData.getPosY();
        this.posZ = transformData.getPosZ();
        this.pitch = transformData.getPitch();
        this.yaw = transformData.getYaw();
        this.roll = transformData.getRoll();
        this.centerOfMass = transformData.getCenterOfMass();
    }

    public PhysWrapperPositionMessage(PhysicsWrapperEntity toSend, int relativeTick) {
        this.entityID = toSend.getEntityId();
        this.relativeTick = relativeTick;
        this.shipBB = toSend.getPhysicsObject().getShipBoundingBox();
        this.posX = toSend.posX;
        this.posY = toSend.posY;
        this.posZ = toSend.posZ;
        this.pitch = toSend.getPitch();
        this.yaw = toSend.getYaw();
        this.roll = toSend.getRoll();
        this.centerOfMass = toSend.getPhysicsObject().getCenterCoord();
    }

    public PhysWrapperPositionMessage(PhysicsObject toRunLocally) {
        this.posX = toRunLocally.getWrapperEntity().posX;
        this.posY = toRunLocally.getWrapperEntity().posY;
        this.posZ = toRunLocally.getWrapperEntity().posZ;

        this.pitch = toRunLocally.getWrapperEntity().getPitch();
        this.yaw = toRunLocally.getWrapperEntity().getYaw();
        this.roll = toRunLocally.getWrapperEntity().getRoll();

        this.centerOfMass = toRunLocally.getCenterCoord();
        this.shipBB = toRunLocally.getShipBoundingBox();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityID = buf.readInt();
        this.relativeTick = buf.readInt();

        this.posX = buf.readDouble();
        this.posY = buf.readDouble();
        this.posZ = buf.readDouble();

        this.pitch = buf.readDouble();
        this.yaw = buf.readDouble();
        this.roll = buf.readDouble();

        this.centerOfMass = new Vector(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.shipBB = new AxisAlignedBB(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityID);
        buf.writeInt(this.relativeTick);

        buf.writeDouble(this.posX);
        buf.writeDouble(this.posY);
        buf.writeDouble(this.posZ);

        buf.writeDouble(this.pitch);
        buf.writeDouble(this.yaw);
        buf.writeDouble(this.roll);

        buf.writeDouble(this.centerOfMass.X);
        buf.writeDouble(this.centerOfMass.Y);
        buf.writeDouble(this.centerOfMass.Z);

        buf.writeDouble(this.shipBB.minX);
        buf.writeDouble(this.shipBB.minY);
        buf.writeDouble(this.shipBB.minZ);
        buf.writeDouble(this.shipBB.maxX);
        buf.writeDouble(this.shipBB.maxY);
        buf.writeDouble(this.shipBB.maxZ);
    }

    /**
     * @return the posX
     */
    public double getPosX() {
        return this.posX;
    }

    /**
     * @param posX the posX to set
     */
    public void setPosX(double posX) {
        this.posX = posX;
    }

    /**
     * @return the posY
     */
    public double getPosY() {
        return this.posY;
    }

    /**
     * @param posY the posY to set
     */
    public void setPosY(double posY) {
        this.posY = posY;
    }

    /**
     * @return the posZ
     */
    public double getPosZ() {
        return this.posZ;
    }

    /**
     * @param posZ the posZ to set
     */
    public void setPosZ(double posZ) {
        this.posZ = posZ;
    }

    /**
     * @return the pitch
     */
    public double getPitch() {
        return this.pitch;
    }

    /**
     * @param pitch the pitch to set
     */
    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    /**
     * @return the yaw
     */
    public double getYaw() {
        return this.yaw;
    }

    /**
     * @param yaw the yaw to set
     */
    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    /**
     * @return the roll
     */
    public double getRoll() {
        return this.roll;
    }

    /**
     * @param roll the roll to set
     */
    public void setRoll(double roll) {
        this.roll = roll;
    }

    /**
     * @return the centerOfMass
     */
    public Vector getCenterOfMass() {
        return this.centerOfMass;
    }

    /**
     * @param centerOfMass the centerOfMass to set
     */
    public void setCenterOfMass(Vector centerOfMass) {
        this.centerOfMass = centerOfMass;
    }

    /**
     * @return the relativeTick
     */
    public int getRelativeTick() {
        return this.relativeTick;
    }

    /**
     * @param relativeTick the relativeTick to set
     */
    public void setRelativeTick(int relativeTick) {
        this.relativeTick = relativeTick;
    }

    /**
     * @return the shipBB
     */
    public AxisAlignedBB getShipBB() {
        return this.shipBB;
    }

    /**
     * @param shipBB the shipBB to set
     */
    public void setShipBB(AxisAlignedBB shipBB) {
        this.shipBB = shipBB;
    }

    /**
     * @return the entityID
     */
    public int getEntityID() {
        return this.entityID;
    }

    /**
     * @param entityID the entityID to set
     */
    public void setEntityID(int entityID) {
        this.entityID = entityID;
    }

}