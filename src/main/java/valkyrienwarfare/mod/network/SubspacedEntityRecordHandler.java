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

import net.minecraft.entity.Entity;
import net.minecraft.util.IThreadListener;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import valkyrienwarfare.mod.coordinates.ISubspacedEntity;
import valkyrienwarfare.mod.coordinates.ISubspacedEntityRecord;
import valkyrienwarfare.mod.physmanagement.interaction.IDraggable;
import valkyrienwarfare.physics.management.PhysicsWrapperEntity;

public class SubspacedEntityRecordHandler implements IMessageHandler<SubspacedEntityRecordMessage, IMessage> {

    @Override
    public IMessage onMessage(final SubspacedEntityRecordMessage message, final MessageContext ctx) {
        IThreadListener threadScheduler = null;
        World world = null;
        if (ctx.side.isClient()) {
            // We are receiving this on the client
            threadScheduler = getClientThreadListener();
            world = getClientWorld();
        } else {
            // Otherwise we are receiving this on the server
            threadScheduler = ctx.getServerHandler().serverController;
            world = ctx.getServerHandler().player.world;
        }
        final World worldFinal = world;
        threadScheduler.addScheduledTask(() -> {
            Entity physicsEntity = worldFinal.getEntityByID(message.physicsObjectWrapperID);
            Entity subspacedEntity = worldFinal.getEntityByID(message.entitySubspacedID);
            if (physicsEntity != null && subspacedEntity != null) {
                PhysicsWrapperEntity wrapperEntity = (PhysicsWrapperEntity) physicsEntity;
                ISubspacedEntityRecord record = message.createRecordForThisMessage(
                        ISubspacedEntity.class.cast(subspacedEntity), wrapperEntity.getPhysicsObject().getSubspace());
                IDraggable draggable = IDraggable.class.cast(subspacedEntity);
                draggable.setForcedRelativeSubspace(wrapperEntity);
                wrapperEntity.getPhysicsObject().getSubspace().forceSubspaceRecord(record.getParentEntity(), record);
                // Now just synchronize the player to the data sent by the client.
            } else {
                System.err.println("An incorrect SubspacedEntityRecordMessage has been thrown out");
            }
        });
        return null;
    }

    @SideOnly(Side.CLIENT)
    private IThreadListener getClientThreadListener() {
        return net.minecraft.client.Minecraft.getMinecraft();
    }

    @SideOnly(Side.CLIENT)
    private World getClientWorld() {
        return net.minecraft.client.Minecraft.getMinecraft().world;
    }

}
