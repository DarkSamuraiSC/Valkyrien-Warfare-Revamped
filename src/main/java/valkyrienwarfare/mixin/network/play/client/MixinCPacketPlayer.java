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

package valkyrienwarfare.mixin.network.play.client;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.world.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import valkyrienwarfare.fixes.ITransformablePacket;
import valkyrienwarfare.math.VWMath;
import valkyrienwarfare.mod.coordinates.ISubspace;
import valkyrienwarfare.mod.coordinates.ISubspacedEntity;
import valkyrienwarfare.mod.coordinates.ISubspacedEntityRecord;
import valkyrienwarfare.mod.coordinates.VectorImmutable;
import valkyrienwarfare.mod.physmanagement.interaction.IDraggable;
import valkyrienwarfare.physics.management.PhysicsWrapperEntity;

@Mixin(CPacketPlayer.class)
public class MixinCPacketPlayer implements ITransformablePacket {

    private final CPacketPlayer thisPacket = CPacketPlayer.class.cast(this);
    private GameType cachedPlayerGameType = null;

    @Inject(method = "processPacket", at = @At(value = "HEAD"))
    public void preDiggingProcessPacket(INetHandlerPlayServer server, CallbackInfo info) {
        this.doPreProcessing(server, false);
    }

    @Inject(method = "processPacket", at = @At(value = "RETURN"))
    public void postDiggingProcessPacket(INetHandlerPlayServer server, CallbackInfo info) {
        this.doPostProcessing(server, false);
    }

    @Override
    public void doPreProcessing(INetHandlerPlayServer server, boolean callingFromSponge) {
        if (this.isPacketOnMainThread(server, callingFromSponge)) {
            PhysicsWrapperEntity parent = this.getPacketParent((NetHandlerPlayServer) server);
            if (parent != null) {
                ISubspace parentSubspace = parent.getPhysicsObject().getSubspace();
                ISubspacedEntityRecord entityRecord = parentSubspace
                        .getRecordForSubspacedEntity(ISubspacedEntity.class.cast(this.getPacketPlayer(server)));
                VectorImmutable positionGlobal = entityRecord.getPositionInGlobalCoordinates();
                VectorImmutable lookVectorGlobal = entityRecord.getLookDirectionInGlobalCoordinates();

                float pitch = (float) VWMath.getPitchFromVectorImmutable(lookVectorGlobal);
                float yaw = (float) VWMath.getYawFromVectorImmutable(lookVectorGlobal, pitch);

                // ===== Set the proper position values for the player packet ====
                this.thisPacket.moving = true;
                this.thisPacket.onGround = true;
                this.thisPacket.x = positionGlobal.getX();
                this.thisPacket.y = positionGlobal.getY();
                this.thisPacket.z = positionGlobal.getZ();

                // ===== Set the proper rotation values for the player packet =====
                this.thisPacket.rotating = true;
                this.thisPacket.yaw = yaw;
                this.thisPacket.pitch = pitch;

                // ===== Dangerous code here =====
                this.cachedPlayerGameType = this.getPacketPlayer(server).interactionManager.gameType;
                this.getPacketPlayer(server).interactionManager.gameType = GameType.CREATIVE;
            }
        }
    }

    @Override
    public void doPostProcessing(INetHandlerPlayServer server, boolean callingFromSponge) {
        if (this.isPacketOnMainThread(server, callingFromSponge)) {
            // ===== Dangerous code here =====
            if (this.cachedPlayerGameType != null) {
                this.getPacketPlayer(server).interactionManager.gameType = this.cachedPlayerGameType;
            }
            PhysicsWrapperEntity parent = this.getPacketParent((NetHandlerPlayServer) server);
            if (parent != null) {
                parent.getPhysicsObject().getSubspace()
                        .forceSubspaceRecord(ISubspacedEntity.class.cast(this.getPacketPlayer(server)), null);
            }
            IDraggable draggable = IDraggable.class.cast(this.getPacketPlayer(server));
            draggable.setForcedRelativeSubspace(null);
        }
    }

    @Override
    public PhysicsWrapperEntity getPacketParent(NetHandlerPlayServer server) {
        EntityPlayerMP player = server.player;
        IDraggable draggable = IDraggable.class.cast(player);
        return draggable.getForcedSubspaceBelowFeet();
    }

    private EntityPlayerMP getPacketPlayer(INetHandlerPlayServer server) {
        return ((NetHandlerPlayServer) server).player;
    }

}
