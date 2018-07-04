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

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;

/**
 * Gets around all the lag from Chunk checks
 *
 * @author thebest108
 */
public class VWChunkCache {

    private final Chunk[][] cachedChunks;
    private final boolean[][] isChunkLoaded;
    private final World worldFor;
    private final int minChunkX, minChunkZ, maxChunkX, maxChunkZ;

    public VWChunkCache(World world, int mnX, int mnZ, int mxX, int mxZ) {
        this.worldFor = world;
        this.minChunkX = mnX >> 4;
        this.minChunkZ = mnZ >> 4;
        this.maxChunkX = mxX >> 4;
        this.maxChunkZ = mxZ >> 4;
        this.cachedChunks = new Chunk[this.maxChunkX - this.minChunkX + 1][this.maxChunkZ - this.minChunkZ + 1];
        this.isChunkLoaded = new boolean[this.maxChunkX - this.minChunkX + 1][this.maxChunkZ - this.minChunkZ + 1];
        for (int x = this.minChunkX; x <= this.maxChunkX; x++) {
            for (int z = this.minChunkZ; z <= this.maxChunkZ; z++) {
                this.cachedChunks[x - this.minChunkX][z - this.minChunkZ] = world.getChunkFromChunkCoords(x, z);
                this.isChunkLoaded[x - this.minChunkX][z - this.minChunkZ] = !this.cachedChunks[x - this.minChunkX][z - this.minChunkZ].isEmpty();
                if (!this.isChunkLoaded[x - this.minChunkX][z - this.minChunkZ]) {
                    boolean allLoaded = false;
                }
            }
        }
    }

    public VWChunkCache(World world, Chunk[][] toCache) {
        this.worldFor = world;
        this.minChunkX = toCache[0][0].x;
        this.minChunkZ = toCache[0][0].z;
        this.maxChunkX = toCache[toCache.length - 1][toCache[0].length - 1].x;
        this.maxChunkZ = toCache[toCache.length - 1][toCache[0].length - 1].z;
        this.isChunkLoaded = new boolean[this.maxChunkX - this.minChunkX + 1][this.maxChunkZ - this.minChunkZ + 1];
        this.cachedChunks = toCache.clone();
    }

    @Nullable
    public TileEntity getTileEntity(BlockPos pos) {
        return this.getTileEntity(pos, Chunk.EnumCreateEntityType.QUEUED);
    }

    @Nullable
    public TileEntity getTileEntity(BlockPos pos, Chunk.EnumCreateEntityType type) {
        int i = (pos.getX() >> 4) - this.minChunkX;
        int j = (pos.getZ() >> 4) - this.minChunkZ;
        if (i < 0 || i >= this.cachedChunks.length || j < 0 || j >= this.cachedChunks[i].length)
            return null;
        if (this.cachedChunks[i][j] == null)
            return null;
        TileEntity tileEntity = this.cachedChunks[i][j].getTileEntity(pos, type);
        if (tileEntity == null) {
            // TODO: Re-enable this for debug testing
            // System.err.println("Physics Thread got a null TileEntity! Maybe it wasn't supposed to?");
        }
        return tileEntity;
    }

    private boolean hasChunkAt(int chunkX, int chunkZ) {
        int relX = chunkX - this.minChunkX;
        int relZ = chunkZ - this.minChunkZ;
        return relX >= 0 && relX < this.cachedChunks.length && relZ >= 0 && relZ < this.cachedChunks[0].length;
    }

    public Chunk getChunkAt(int x, int z) {
        return this.cachedChunks[x - this.minChunkX][z - this.minChunkZ];
    }

    public IBlockState getBlockState(BlockPos pos) {
        Chunk chunkForPos = this.cachedChunks[(pos.getX() >> 4) - this.minChunkX][(pos.getZ() >> 4) - this.minChunkZ];
        return chunkForPos.getBlockState(pos);
    }

    public IBlockState getBlockState(int x, int y, int z) {
        if (!this.hasChunkAt(x >> 4, z >> 4)) {
            return Blocks.AIR.getDefaultState();
        }
        Chunk chunkForPos = this.cachedChunks[(x >> 4) - this.minChunkX][(z >> 4) - this.minChunkZ];
        return chunkForPos.getBlockState(x, y, z);
    }

}