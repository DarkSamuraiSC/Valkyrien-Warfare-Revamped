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
import net.minecraft.world.World;

/**
 * This class is responsible for finding/allocating the Chunks for
 * PhysicsObjects; also ensures the custom chunk-loading system in place
 *
 * @author thebest108
 */
@Getter
public class PhysicsChunkManager {
    private static final int xChunkStartingPos = -1870000;
    private static final int zChunkStartingPos = -1869950;
    /**
     * the widest area that a ship is allowed to take up
     */
    @Getter
    private static final int maxChunkRadius = 12;
    /**
     * the + 1 is for padding, to reduce the risk of ships interacting with each other
     * inside of ship space
     */
    private static final int distanceBetweenSets = maxChunkRadius + 1;
    /**
     * the maximum number of airships that are allowed to be in a single row on the x axis
     * before starting a new row
     */
    private static final long maxSetsPerRow = (-xChunkStartingPos) * 2L / distanceBetweenSets; //287692
    private World worldObj;
    private ChunkClaimWorldData data;

    public PhysicsChunkManager(World worldFor) {
        worldObj = worldFor;
        loadDataFromWorld();
    }

    public static boolean isLikelyShipChunk(int chunkX, int chunkZ) {
        // The +50 is used to make sure chunks too close to ships dont interfere
        //return chunkZ < zChunkStartingPos + distanceBetweenSets + 50;
        return chunkZ < (27000000 >> 16) - 1024;
        //we prevent players from going beyond 27 million, plus 1024
        //to play it safe. in all honesty there'll probably never be this many ships in a world, but hey :P
    }

    /**
     * This finds the next empty chunkSet for use
     * <p>
     * This will increase the x position until it hits the maximum value, at which point it will
     * reset the x position and increment the z position by one, and then continue to increment x.
     * <p>
     * If free, previously occupied chunkSets are found, this will attempt to fill those before
     * allocating a new one.
     *
     * @return a new, empty chunkSet
     */
    public VWChunkClaim getNextAvailableChunkSet(int chunkRadius) {
        if (chunkRadius > maxChunkRadius) {
            return null;
        }
        //TODO: figure out why this was being loaded every time the method is called
        //loadDataFromWorld();

        long key;
        if (this.data.getAvailableChunkKeys().getSize() == 0) {
            key = this.data.getChunkKey().getAndIncrement();
        } else {
            //get the next free chunk key
            key = this.data.getAvailableChunkKeys().get(0);
            this.data.getAvailableChunkKeys().remove(0);
        }
        data.markDirty();

        int chunkX = (int) ((key % maxSetsPerRow) * distanceBetweenSets + xChunkStartingPos);
        int chunkZ = (int) ((key / maxSetsPerRow) * distanceBetweenSets + zChunkStartingPos);
        return new VWChunkClaim(chunkX, chunkZ, chunkRadius);
    }

    /**
     * This retrieves the ChunkSetKey data for the specific world
     */
    public void loadDataFromWorld() {
        data = ChunkClaimWorldData.get(worldObj);
    }

    public void markChunksAvailable(@NonNull VWChunkClaim claim) {
        long chunkKey = (claim.getCenterZ() - zChunkStartingPos) * maxSetsPerRow + (claim.getCenterX() - xChunkStartingPos);
        this.data.getAvailableChunkKeys().add(chunkKey);
        this.data.markDirty();
    }
}
