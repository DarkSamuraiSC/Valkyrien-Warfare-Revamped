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

package valkyrienwarfare.addon.control.tileentity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import valkyrienwarfare.addon.control.block.BlockShipTelegraph;
import valkyrienwarfare.addon.control.controlsystems.ShipTelegraphState;
import valkyrienwarfare.addon.control.piloting.ControllerInputType;
import valkyrienwarfare.addon.control.piloting.PilotControlsMessage;

public class TileEntityShipTelegraph extends ImplTileEntityPilotable implements ITickable {

    public ShipTelegraphState telegraphState = ShipTelegraphState.LANGSAM_1;
    public double oldHandleRotation;
    public double handleRotation;

    double nextHandleRotation;

    @Override
    void processControlMessage(PilotControlsMessage message, EntityPlayerMP sender) {
        int deltaOrdinal = 0;
        double deltaRotation = 0;
        if (message.airshipLeft_KeyPressed) {
            deltaOrdinal -= 1;
            deltaRotation -= 22.5;
        }
        if (message.airshipRight_KeyPressed) {
            deltaOrdinal += 1;
            deltaRotation += 22.5;
        }
        IBlockState blockState = this.getWorld().getBlockState(this.getPos());
        if (blockState.getBlock() instanceof BlockShipTelegraph) {
            EnumFacing facing = blockState.getValue(BlockShipTelegraph.FACING);
            if (this.isPlayerInFront(sender, facing)) {
                deltaOrdinal *= -1;
                deltaRotation *= -1;
            }
        }


        int ordinal = this.telegraphState.ordinal();
        if ((ordinal > 0 && ordinal < 12) || (ordinal == 0 && deltaOrdinal > 0) || (ordinal == 12 && deltaOrdinal < 0)) {
            this.handleRotation += deltaRotation;
            ordinal += deltaOrdinal;
        }
        this.telegraphState = ShipTelegraphState.values()[ordinal];
    }


    @Override
    public void onDataPacket(net.minecraft.network.NetworkManager net, net.minecraft.network.play.server.SPacketUpdateTileEntity pkt) {
//		lastWheelRotation = wheelRotation;

        this.nextHandleRotation = pkt.getNbtCompound().getDouble("handleRotation");
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound tagToSend = new NBTTagCompound();
        tagToSend.setDouble("handleRotation", this.handleRotation);
        return new SPacketUpdateTileEntity(this.getPos(), 0, tagToSend);
    }

    public double getHandleRenderRotation() {
        return this.handleRotation;
    }

    @Override
    public void update() {
        if (this.getWorld().isRemote) {
            this.oldHandleRotation = this.handleRotation;
            this.handleRotation += (this.nextHandleRotation - this.handleRotation) * .35D;
        } else {
            this.sendUpdatePacketToAllNearby();
        }
//		this.markDirty();
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound toReturn = super.getUpdateTag();
        toReturn.setDouble("handleRotation", this.handleRotation);
        return toReturn;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.handleRotation = compound.getDouble("handleRotation");
        this.telegraphState = ShipTelegraphState.values()[compound.getInteger("telegraphStateOrdinal")];
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagCompound toReturn = super.writeToNBT(compound);
        toReturn.setDouble("handleRotation", this.handleRotation);
        toReturn.setInteger("telegraphStateOrdinal", this.telegraphState.ordinal());
        return toReturn;
    }

    @Override
    ControllerInputType getControlInputType() {
        return ControllerInputType.Telegraph;
    }

}
