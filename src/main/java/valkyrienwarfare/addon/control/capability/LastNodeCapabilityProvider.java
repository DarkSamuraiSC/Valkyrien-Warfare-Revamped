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

package valkyrienwarfare.addon.control.capability;

import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import valkyrienwarfare.addon.control.ValkyrienWarfareControl;

public class LastNodeCapabilityProvider implements ICapabilitySerializable<NBTTagIntArray> {

    private ICapabilityLastRelay inst = ValkyrienWarfareControl.lastRelayCapability.getDefaultInstance();

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == ValkyrienWarfareControl.lastRelayCapability;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        return capability == ValkyrienWarfareControl.lastRelayCapability ? ValkyrienWarfareControl.lastRelayCapability.cast(this.inst) : null;
    }

    @Override
    public NBTTagIntArray serializeNBT() {
        return (NBTTagIntArray) ValkyrienWarfareControl.lastRelayCapability.getStorage().writeNBT(ValkyrienWarfareControl.lastRelayCapability, this.inst, null);
    }

    @Override
    public void deserializeNBT(NBTTagIntArray nbt) {
        ValkyrienWarfareControl.lastRelayCapability.getStorage().readNBT(ValkyrienWarfareControl.lastRelayCapability, this.inst, null, nbt);
    }

}
