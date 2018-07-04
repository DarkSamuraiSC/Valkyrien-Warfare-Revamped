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

import net.daporkchop.lib.encoding.ToBytes;
import net.daporkchop.lib.primitive.list.LongList;
import net.daporkchop.lib.primitive.list.array.LongArrayList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import java.util.concurrent.atomic.AtomicLong;

public class ChunkClaimWorldData extends WorldSavedData {

    private static final String CHUNK_POS_DATA_KEY = "ChunkKeys";
    private final LongList availableChunkKeys = new LongArrayList();
    private final AtomicLong chunkKey = new AtomicLong(0L);

    public ChunkClaimWorldData()    {
        this(CHUNK_POS_DATA_KEY);
    }

    public ChunkClaimWorldData(String name) {
        super(name);
        this.markDirty();
    }

    public static ChunkClaimWorldData get(World world) {
        MapStorage storage = world.getPerWorldStorage();
        ChunkClaimWorldData data = (ChunkClaimWorldData) storage.getOrLoadData(ChunkClaimWorldData.class, CHUNK_POS_DATA_KEY);
        if (data == null) {
            System.err.println("Had to create a null ChunkKeysWorldData; could this be corruption?");
            data = new ChunkClaimWorldData();
            world.setData(CHUNK_POS_DATA_KEY, data);
        }
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.chunkKey.set(nbt.getLong("chunkKey"));
        if (nbt.hasKey("availableChunkKeys")) {
            //support for legacy format
            nbt.removeTag("availableChunkKeys");
        } else {
            this.availableChunkKeys.addAll(ToBytes.toLongs(nbt.getByteArray("availableChunkKeys_v2")));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setLong("chunkKey", this.chunkKey.get());
        //porktodo: toArray method for PorkLib primitive arrays (and maybe maps too?)
        long[] data = new long[this.availableChunkKeys.getSize()];
        for (int i = 0; i < data.length; i++) {
            data[i] = this.availableChunkKeys.get(i);
        }
        nbt.setByteArray("availableChunkKeys_v2", ToBytes.toBytes(data));
        return nbt;
    }

    public LongList getAvailableChunkKeys() {
        return this.availableChunkKeys;
    }

    public AtomicLong getChunkKey() {
        return this.chunkKey;
    }
}
