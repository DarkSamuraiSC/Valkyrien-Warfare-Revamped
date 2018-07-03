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
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import valkyrienwarfare.mod.coordinates.*;

import javax.annotation.Nullable;

/**
 * A message that sends the SubspacedEntityRecord from server to client and from
 * client to server.s
 *
 * @author thebest108
 */
public class SubspacedEntityRecordMessage implements IMessage {

    // This data only exists on the side that received this packet.
    public int physicsObjectWrapperID;
    public int entitySubspacedID;
    @Nullable
    public VectorImmutable position;
    @Nullable
    public VectorImmutable positionLastTick;
    @Nullable
    public VectorImmutable lookDirection;
    @Nullable
    public VectorImmutable velocity;
    // This object only exists on the side that created this packet.
    @Nullable
    private ISubspacedEntityRecord subspacedEntityRecord;

    public SubspacedEntityRecordMessage(ISubspacedEntityRecord subspacedEntityRecord) {
        this.subspacedEntityRecord = subspacedEntityRecord;
    }

    public SubspacedEntityRecordMessage() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        physicsObjectWrapperID = buf.readInt();
        entitySubspacedID = buf.readInt();
        position = VectorImmutable.readFromByteBuf(buf);
        positionLastTick = VectorImmutable.readFromByteBuf(buf);
        lookDirection = VectorImmutable.readFromByteBuf(buf);
        velocity = VectorImmutable.readFromByteBuf(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        if (subspacedEntityRecord.getParentSubspace().getSubspaceCoordinatesType() == CoordinateSpaceType.GLOBAL_COORDINATES) {
            throw new IllegalStateException(
                    "Just tried sending SubspacedEntityRecordMessage for a record that was made by the world subspace. This isn't right so we crash right here.");
        }
        buf.writeInt(subspacedEntityRecord.getParentSubspace().getSubspaceParentEntityID());
        buf.writeInt(subspacedEntityRecord.getParentEntity().getSubspacedEntityID());
        subspacedEntityRecord.getPosition().writeToByteBuf(buf);
        subspacedEntityRecord.getPositionLastTick().writeToByteBuf(buf);
        subspacedEntityRecord.getLookDirection().writeToByteBuf(buf);
        subspacedEntityRecord.getVelocity().writeToByteBuf(buf);
    }

    public ISubspacedEntityRecord createRecordForThisMessage(ISubspacedEntity entity, ISubspace provider) {
        return new ImplSubspacedEntityRecord(entity, provider, position, positionLastTick, lookDirection, velocity);
    }

}
