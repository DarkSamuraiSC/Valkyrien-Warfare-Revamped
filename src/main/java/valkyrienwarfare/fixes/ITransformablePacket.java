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

package valkyrienwarfare.fixes;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.INetHandlerPlayServer;
import valkyrienwarfare.MixinLoadManager;
import valkyrienwarfare.api.TransformType;
import valkyrienwarfare.math.RotationMatrices;
import valkyrienwarfare.mod.coordinates.*;
import valkyrienwarfare.physics.management.PhysicsWrapperEntity;

/**
 * Used to indicate when a packet must be transformed into ship space to work
 * properly (Digging packets for example). Also comes with functionality to
 * store and retrieve a player data backup to prevent the player from getting
 * teleported somewhere else, but this is not necessarily required.
 *
 * @author thebest108
 */
public interface ITransformablePacket {

    default boolean isPacketOnMainThread(INetHandlerPlayServer server, boolean callingFromSponge) {
        if (!MixinLoadManager.isSpongeEnabled() || callingFromSponge) {
            NetHandlerPlayServer serverHandler = (NetHandlerPlayServer) server;
            EntityPlayerMP player = serverHandler.player;
            return player.getServerWorld().isCallingFromMinecraftThread();
        } else {
            return false;
        }
    }

    /**
     * Puts the player into local coordinates and makes a record of where they used
     * to be.
     *
     * @param server
     * @param callingFromSponge
     */
    default void doPreProcessing(INetHandlerPlayServer server, boolean callingFromSponge) {
        if (this.isPacketOnMainThread(server, callingFromSponge)) {
            // System.out.println("Pre packet process");
            NetHandlerPlayServer serverHandler = (NetHandlerPlayServer) server;
            EntityPlayerMP player = serverHandler.player;
            PhysicsWrapperEntity wrapper = this.getPacketParent(serverHandler);
            if (wrapper != null && wrapper.getPhysicsObject().getShipTransformationManager() != null) {
                ISubspaceProvider worldProvider = ISubspaceProvider.class.cast(player.getServerWorld());
                ISubspace worldSubspace = worldProvider.getSubspace();
                worldSubspace.snapshotSubspacedEntity(ISubspacedEntity.class.cast(player));
                RotationMatrices.applyTransform(
                        wrapper.getPhysicsObject().getShipTransformationManager().getCurrentTickTransform(), player,
                        TransformType.GLOBAL_TO_SUBSPACE);
            }

        }
    }

    /**
     * Restores the player from local coordinates to where they used to be.
     *
     * @param server
     * @param callingFromSponge
     */
    default void doPostProcessing(INetHandlerPlayServer server, boolean callingFromSponge) {
        if (this.isPacketOnMainThread(server, callingFromSponge)) {
            NetHandlerPlayServer serverHandler = (NetHandlerPlayServer) server;
            EntityPlayerMP player = serverHandler.player;
            PhysicsWrapperEntity wrapper = this.getPacketParent(serverHandler);
            // I don't care what happened to that ship in the time between, we must restore
            // the player to their proper coordinates.
            ISubspaceProvider worldProvider = ISubspaceProvider.class.cast(player.getServerWorld());
            ISubspace worldSubspace = worldProvider.getSubspace();
            ISubspacedEntity subspacedEntity = ISubspacedEntity.class.cast(player);
            ISubspacedEntityRecord record = worldSubspace.getRecordForSubspacedEntity(subspacedEntity);
            // System.out.println(player.getPosition());
            if (subspacedEntity.currentSubspaceType() == CoordinateSpaceType.SUBSPACE_COORDINATES) {
                subspacedEntity.restoreSubspacedEntityStateToRecord(record);
                player.setPosition(player.posX, player.posY, player.posZ);
            }
            // System.out.println(player.getPosition());
            // We need this because Sponge Mixins prevent this from properly working. This
            // won't be necessary on client however.
        }
    }

    PhysicsWrapperEntity getPacketParent(NetHandlerPlayServer server);
}
