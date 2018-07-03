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

import javax.annotation.concurrent.Immutable;

/**
 * An immutable record for an Entity within a subspace. Holds information which
 * can be used to restore the state of the given entity back to a safe value;
 * including position, rotation, and velocity data.
 *
 * @author thebest108
 */
@Immutable
public interface ISubspacedEntityRecord {

    ISubspacedEntity getParentEntity();

    ISubspace getParentSubspace();

    VectorImmutable getPosition();

    VectorImmutable getPositionLastTick();

    VectorImmutable getLookDirection();

    VectorImmutable getVelocity();

    default VectorImmutable getPositionInGlobalCoordinates() {
        if (getParentSubspace().getSubspaceCoordinatesType() == CoordinateSpaceType.GLOBAL_COORDINATES) {
            return getPosition();
        } else {
            return getParentSubspace().getSubspaceTransform().transform(getPosition(), TransformType.SUBSPACE_TO_GLOBAL);
        }
    }

    default VectorImmutable getPositionLastTickInGlobalCoordinates() {
        if (getParentSubspace().getSubspaceCoordinatesType() == CoordinateSpaceType.GLOBAL_COORDINATES) {
            return getPositionLastTick();
        } else {
            return getParentSubspace().getSubspaceTransform().transform(getPositionLastTick(), TransformType.SUBSPACE_TO_GLOBAL);
        }
    }

    default VectorImmutable getLookDirectionInGlobalCoordinates() {
        if (getParentSubspace().getSubspaceCoordinatesType() == CoordinateSpaceType.GLOBAL_COORDINATES) {
            return getLookDirection();
        } else {
            return getParentSubspace().getSubspaceTransform().rotate(getLookDirection(), TransformType.SUBSPACE_TO_GLOBAL);
        }
    }

    default VectorImmutable getVelocityInGlobalCoordinates() {
        if (getParentSubspace().getSubspaceCoordinatesType() == CoordinateSpaceType.GLOBAL_COORDINATES) {
            return getVelocity();
        } else {
            return getParentSubspace().getSubspaceTransform().rotate(getVelocity(), TransformType.SUBSPACE_TO_GLOBAL);
        }
    }

}
