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

package valkyrienwarfare.mod.physmanagement.chunk;

import lombok.Getter;
import lombok.NonNull;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import valkyrienwarfare.addon.ftbutil.ValkyrienWarfareFTBUtil;
import valkyrienwarfare.physics.management.PhysicsObject;

/**
 * This stores the chunk claims for a PhysicsObject; not the chunks themselves
 *
 * @author thebest108
 */
@Getter
public class VWChunkClaim {

    private final int centerX;
    private final int centerZ;
    private final int radius;
    private final boolean[][] chunkOccupiedInLocal;

    public VWChunkClaim(int x, int z, int size) {
        this.centerX = x;
        this.centerZ = z;
        this.radius = size;
        this.chunkOccupiedInLocal = new boolean[(this.radius << 1) + 1][(this.radius << 1) + 1];
    }

    public VWChunkClaim(NBTTagCompound readFrom) {
        this(readFrom.getInteger("centerX"), readFrom.getInteger("centerZ"), readFrom.getInteger("radius"));
    }

    public void writeToNBT(NBTTagCompound toSave) {
        toSave.setInteger("centerX", getCenterX());
        toSave.setInteger("centerZ", getCenterZ());
        toSave.setInteger("radius", getRadius());
    }

    public boolean isChunkEnclosedInMaxSet(int chunkX, int chunkZ) {
        boolean inX = (chunkX >= getCenterX() - 12) && (chunkX <= getCenterX() + 12);
        boolean inZ = (chunkZ >= getCenterZ() - 12) && (chunkZ <= getCenterZ() + 12);
        return inX && inZ;
    }

    public boolean isChunkEnclosedInSet(int chunkX, int chunkZ) {
        boolean inX = (chunkX >= getMinX()) && (chunkX <= getMaxX());
        boolean inZ = (chunkZ >= getMinZ()) && (chunkZ <= getMaxZ());
        return inX && inZ;
    }

    public void markChunkOccupied(int x, int z, @NonNull PhysicsObject object) {
        if (!this.isChunkOccupied(x, z)) {
            ValkyrienWarfareFTBUtil.handleClaim(object, x, z);
            this.chunkOccupiedInLocal[x][z] = true;
        }
    }

    public boolean isChunkOccupied(int x, int z) {
        return this.chunkOccupiedInLocal[x][z];
    }

    @Override
    public String toString() {
        return getCenterX() + ":" + getCenterZ() + ":" + getRadius();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof VWChunkClaim) {
            VWChunkClaim other = (VWChunkClaim) o;
            return other.getCenterX() == getCenterX() && other.getCenterZ() == getCenterZ() && other.getRadius() == getRadius();
        }
        return false;
    }

    /**
     * @return the maxX
     */
    public int getMaxX() {
        return getCenterX() + getRadius();
    }

    /**
     * @return the maxZ
     */
    public int getMaxZ() {
        return getCenterZ() + getRadius();
    }

    /**
     * @return the minZ
     */
    public int getMinZ() {
        return getCenterZ() - getRadius();
    }

    /**
     * @return the minX
     */
    public int getMinX() {
        return getCenterX() - getRadius();
    }

    public BlockPos getRegionCenter() {
        return new BlockPos(this.getCenterX() * 16, 128, this.getCenterZ() * 16);
    }

    public int getChunkLengthX() {
        return getMaxX() - getMinX() + 1;
    }

    public int getChunkLengthZ() {
        return getMaxZ() - getMinZ() + 1;
    }
}
