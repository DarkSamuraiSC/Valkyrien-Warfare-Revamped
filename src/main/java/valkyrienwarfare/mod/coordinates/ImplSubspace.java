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

package valkyrienwarfare.mod.coordinates;

import valkyrienwarfare.api.TransformType;
import valkyrienwarfare.math.Vector;
import valkyrienwarfare.physics.management.PhysicsObject;
import valkyrienwarfare.physics.management.PhysicsWrapperEntity;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * A basic implementation of the ISubspace interface.
 *
 * @author thebest108
 */
public class ImplSubspace implements ISubspace {

    // If null, then we are the World subspace.
    private final PhysicsObject parent;
    private final Map<ISubspacedEntity, ISubspacedEntityRecord> subspacedEntityRecords;

    public ImplSubspace(@Nullable PhysicsObject parent) {
        this.parent = parent;
        this.subspacedEntityRecords = new HashMap<ISubspacedEntity, ISubspacedEntityRecord>();
    }

    @Override
    public boolean hasRecordForSubspacedEntity(ISubspacedEntity subspaced) {
        return subspacedEntityRecords.containsKey(subspaced);
    }

    @Override
    public ISubspacedEntityRecord getRecordForSubspacedEntity(ISubspacedEntity subspaced) {
        return subspacedEntityRecords.get(subspaced);
    }

    @Override
    public void snapshotSubspacedEntity(ISubspacedEntity subspaced) {
        if (subspaced.currentSubspaceType() != CoordinateSpaceType.GLOBAL_COORDINATES) {
            throw new IllegalArgumentException("Subspace snapshots can only be taken for entities that in the global coordinates system!");
        }
        if (subspaced instanceof PhysicsWrapperEntity) {
            throw new IllegalArgumentException("Do not create subspace records for PhysicsWrapperEntities!!");
        }
        subspacedEntityRecords.put(subspaced, createRecordForSubspacedEntity(subspaced));
    }

    private ISubspacedEntityRecord createRecordForSubspacedEntity(ISubspacedEntity subspacedEntity) {
        Vector position = subspacedEntity.createCurrentPositionVector();
        Vector positionLastTick = subspacedEntity.createLastTickPositionVector();
        Vector look = subspacedEntity.createCurrentLookVector();
        Vector velocity = subspacedEntity.createCurrentVelocityVector();
        ShipTransform subspaceTransform = getSubspaceTransform();
        if (subspaceTransform != null) {
            subspaceTransform.transform(position, TransformType.GLOBAL_TO_SUBSPACE);
            subspaceTransform.rotate(look, TransformType.GLOBAL_TO_SUBSPACE);
            subspaceTransform.rotate(velocity, TransformType.GLOBAL_TO_SUBSPACE);
        }
        return new ImplSubspacedEntityRecord(subspacedEntity, this, position.toImmutable(),
                positionLastTick.toImmutable(), look.toImmutable(), velocity.toImmutable());
    }

    @Override
    public CoordinateSpaceType getSubspaceCoordinatesType() {
        if (parent == null) {
            return CoordinateSpaceType.GLOBAL_COORDINATES;
        } else {
            return CoordinateSpaceType.SUBSPACE_COORDINATES;
        }
    }

    @Override
    public ShipTransform getSubspaceTransform() {
        if (getSubspaceCoordinatesType() == CoordinateSpaceType.GLOBAL_COORDINATES) {
            return null;
        } else {
            ShipTransform transform = parent.getShipTransformationManager().getCurrentTickTransform();
            if (transform == null) {
                throw new IllegalStateException(
                        "A PhysicsObject got a request to use its subspace, but it had no transforms loaded. This is crash worthy.");
            }
            return transform;
        }
    }

    @Override
    public int getSubspaceParentEntityID() {
        if (getSubspaceCoordinatesType() == CoordinateSpaceType.GLOBAL_COORDINATES) {
            throw new IllegalStateException(
                    "The World coordinate subspace doesn't have an entity ID. Don't call this method unless you're sure that the subspace isn't the world.");
        }
        return parent.getWrapperEntity().getEntityId();
    }

    @Override
    public void forceSubspaceRecord(ISubspacedEntity entity, ISubspacedEntityRecord record) {
        subspacedEntityRecords.put(entity, record);
    }

}
