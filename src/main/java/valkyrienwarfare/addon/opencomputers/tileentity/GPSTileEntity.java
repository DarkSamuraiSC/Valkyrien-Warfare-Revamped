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

package valkyrienwarfare.addon.opencomputers.tileentity;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.SimpleComponent;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Optional;
import valkyrienwarfare.deprecated_api.ValkyrienWarfareHooks;
import valkyrienwarfare.physics.management.PhysicsWrapperEntity;

@Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "opencomputers")
public class GPSTileEntity extends TileEntity implements SimpleComponent {
    public GPSTileEntity() {
        super();
    }

    // Used by OpenComputers
    @Override
    public String getComponentName() {
        return "ship_gps";
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] getPosition(Context context, Arguments args) {
        if (ValkyrienWarfareHooks.isBlockPartOfShip(this.world, this.pos)) {
            BlockPos pos = ValkyrienWarfareHooks.getShipEntityManagingPos(this.getWorld(), this.getPos()).getPosition();
            return new Object[]{pos.getX(), pos.getY(), pos.getZ()};
        }
        return null;
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] getRotation(Context context, Arguments args) {
        if (ValkyrienWarfareHooks.isBlockPartOfShip(this.world, this.pos)) {
            PhysicsWrapperEntity ship = ValkyrienWarfareHooks.getShipEntityManagingPos(this.getWorld(), this.getPos());
            return new Object[]{ship.getYaw(), ship.getPitch(), ship.getRoll()};
        }
        return null;
    }
}
