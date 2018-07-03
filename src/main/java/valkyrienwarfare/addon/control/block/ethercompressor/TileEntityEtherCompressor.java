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

package valkyrienwarfare.addon.control.block.ethercompressor;

import net.minecraft.nbt.NBTTagCompound;
import valkyrienwarfare.addon.control.fuel.IEtherGasEngine;
import valkyrienwarfare.addon.control.nodenetwork.BasicForceNodeTileEntity;
import valkyrienwarfare.math.Vector;

public abstract class TileEntityEtherCompressor extends BasicForceNodeTileEntity implements IEtherGasEngine {

    private int etherGas;
    private int etherGasCapacity;

    public TileEntityEtherCompressor(Vector normalForceVector, double power) {
        super(normalForceVector, false, power);
        validate();
        etherGas = 0;
        etherGasCapacity = 1000;
    }

    public TileEntityEtherCompressor() {
        this(null, 0);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        etherGas = compound.getInteger("etherGas");
        etherGasCapacity = compound.getInteger("etherGasCapacity");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagCompound toReturn = super.writeToNBT(compound);
        toReturn.setInteger("etherGas", etherGas);
        toReturn.setInteger("etherGasCapacity", etherGasCapacity);
        return toReturn;
    }

    @Override
    public boolean isForceOutputOriented() {
        return false;
    }

    @Override
    public int getCurrentEtherGas() {
        return etherGas;
    }

    @Override
    public int getEtherGasCapacity() {
        return etherGasCapacity;
    }

    // pre : Throws an IllegalArgumentExcepion if more gas is added than there is
    // capacity for this engine.
    @Override
    public void addEtherGas(int gas) {
        if (etherGas + gas > etherGasCapacity) {
            throw new IllegalArgumentException();
        }
        etherGas += gas;
    }

}
