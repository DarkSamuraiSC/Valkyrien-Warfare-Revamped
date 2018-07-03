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

package valkyrienwarfare.physics.collision.optimization;

public class SimpleBitOctree implements IBitOctree {

    private final IBitSet bitbuffer;

    public SimpleBitOctree() {
        this.bitbuffer = new SmallBitSet(BITS_TOTAL);
    }

    @Override
    public void set(int x, int y, int z, boolean bit) {
        int index = this.getBlockIndex(x, y, z);
        this.ensureCapacity(index);
        if (this.bitbuffer.get(index) != bit) {
            this.bitbuffer.set(index, bit);
            this.updateOctrees(x, y, z, bit);
        }
    }

    @Override
    public boolean get(int x, int y, int z) {
        return this.getAtIndex(this.getBlockIndex(x, y, z));
    }

    @Override
    public boolean getAtIndex(int index) {
        this.ensureCapacity(index);
        return this.bitbuffer.get(index);
    }

    @Override
    public int getOctreeLevelOneIndex(int levelTwoIndex, int offset) {
        return levelTwoIndex + offset + 1;
    }

    @Override
    public int getOctreeLevelTwoIndex(int levelThreeIndex, int offset) {
        return levelThreeIndex + (9 * offset) + 1;
    }

    @Override
    public int getOctreeLevelThreeIndex(int offset) {
        return BLOCKS_TOTAL + (73 * offset);
    }

    // If something tried calling code outside of the buffer size, throw an
    // IllegalArgumentException its way.
    private void ensureCapacity(int index) {
        if (index > BITS_TOTAL) {
            throw new IllegalArgumentException("Tried accessing an element out of bounds!");
        }
    }

    private void updateOctrees(int x, int y, int z, boolean bit) {
        int levelThreeIndex = this.getOctreeLevelThreeIndex(x, y, z);
        int levelTwoIndex = this.getOctreeLevelTwoIndex(x, y, z, levelThreeIndex);
        int levelOneIndex = this.getOctreeLevelOneIndex(x, y, z, levelTwoIndex);

        if (this.getAtIndex(levelOneIndex) != bit) {
            if (this.updateOctreeLevelOne(levelOneIndex, x, y, z)) {
                if (this.updateOctreeLevelTwo(levelTwoIndex)) {
                    this.updateOctreeLevelThree(levelThreeIndex);
                }
            }
        }
    }

    private void updateOctreeLevelThree(int levelThreeIndex) {
        if (this.bitbuffer.get(levelThreeIndex + 1) || this.bitbuffer.get(levelThreeIndex + 10)
                || this.bitbuffer.get(levelThreeIndex + 19) || this.bitbuffer.get(levelThreeIndex + 28)
                || this.bitbuffer.get(levelThreeIndex + 37) || this.bitbuffer.get(levelThreeIndex + 46)
                || this.bitbuffer.get(levelThreeIndex + 55) || this.bitbuffer.get(levelThreeIndex + 64)) {
            this.bitbuffer.set(levelThreeIndex);
        } else {
            this.bitbuffer.clear(levelThreeIndex);
        }
    }

    // Returns true if the next level of octree should be updated
    private boolean updateOctreeLevelTwo(int levelTwoIndex) {
        if (this.bitbuffer.get(levelTwoIndex + 1) || this.bitbuffer.get(levelTwoIndex + 2) || this.bitbuffer.get(levelTwoIndex + 3)
                || this.bitbuffer.get(levelTwoIndex + 4) || this.bitbuffer.get(levelTwoIndex + 5)
                || this.bitbuffer.get(levelTwoIndex + 6) || this.bitbuffer.get(levelTwoIndex + 7)
                || this.bitbuffer.get(levelTwoIndex + 8)) {
            if (!this.bitbuffer.get(levelTwoIndex)) {
                this.bitbuffer.set(levelTwoIndex);
                return true;
            }
        } else {
            if (this.bitbuffer.get(levelTwoIndex)) {
                this.bitbuffer.clear(levelTwoIndex);
                return true;
            }
        }
        return false;
    }

    // Returns true if the next level of octree should be updated
    private boolean updateOctreeLevelOne(int levelOneIndex, int x, int y, int z) {
        // Only keep the last 4 bits; 0x0E = 1110, also removes the last bit
        x &= 0x0E;
        y &= 0x0E;
        z &= 0x0E;
        if (this.get(x, y, z) || this.get(x, y, z + 1) || this.get(x, y + 1, z) || this.get(x, y + 1, z + 1) || this.get(x + 1, y, z)
                || this.get(x + 1, y, z + 1) || this.get(x + 1, y + 1, z) || this.get(x + 1, y + 1, z + 1)) {
            if (!this.bitbuffer.get(levelOneIndex)) {
                this.bitbuffer.set(levelOneIndex);
                return true;
            }
        } else {
            if (this.bitbuffer.get(levelOneIndex)) {
                this.bitbuffer.clear(levelOneIndex);
                return true;
            }
        }
        return false;
    }

    private int getOctreeLevelOneIndex(int x, int y, int z, int levelTwoIndex) {
        x = (x & 0x02) >> 1;
        y = (y & 0x02);
        z = (z & 0x02) << 1;
        return this.getOctreeLevelOneIndex(levelTwoIndex, x | y | z);
    }

    private int getOctreeLevelTwoIndex(int x, int y, int z, int levelThreeIndex) {
        x = (x & 0x04) >> 2;
        y = (y & 0x04) >> 1;
        z = (z & 0x04);
        return this.getOctreeLevelTwoIndex(levelThreeIndex, x | y | z);
    }

    private int getOctreeLevelThreeIndex(int x, int y, int z) {
        x = (x & 0x08) >> 3;
        y = (y & 0x08) >> 2;
        z = (z & 0x08) >> 1;
        return this.getOctreeLevelThreeIndex(x | y | z);
    }

    private int getBlockIndex(int x, int y, int z) {
        return x | (y << 4) | (z << 8);
    }

}
