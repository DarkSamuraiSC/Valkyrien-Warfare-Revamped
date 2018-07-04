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

package valkyrienwarfare.addon.control.piloting;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import valkyrienwarfare.ValkyrienWarfareMod;

public class PilotControlsMessageHandler implements IMessageHandler<PilotControlsMessage, IMessage> {

    @Override
    public IMessage onMessage(PilotControlsMessage message, MessageContext ctx) {
        IThreadListener mainThread = ctx.getServerHandler().serverController;
        mainThread.addScheduledTask(() -> {
            World worldObj = ctx.getServerHandler().player.world;
            if (ValkyrienWarfareMod.VW_PHYSICS_MANAGER.getManagerForWorld(worldObj) != null) {
//                	UUID shipId = message.shipFor;
                BlockPos posFor = message.controlBlockPos;
                TileEntity tile = worldObj.getTileEntity(posFor);

                if (tile instanceof ITileEntityPilotable) {
                    ((ITileEntityPilotable) tile).onPilotControlsMessage(message, ctx.getServerHandler().player);
                }
            }
        });

        return null;
    }

}
