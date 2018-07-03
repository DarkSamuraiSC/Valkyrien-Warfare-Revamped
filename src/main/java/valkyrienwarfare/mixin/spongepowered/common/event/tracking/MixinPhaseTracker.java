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

package valkyrienwarfare.mixin.spongepowered.common.event.tracking;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.interfaces.world.IMixinWorldServer;
import valkyrienwarfare.ValkyrienWarfareMod;
import valkyrienwarfare.physics.management.PhysicsWrapperEntity;

/**
 * Used to hook into the setBlockState() method when Sponge is loaded.
 *
 * @author thebest108
 */
@Mixin(targets = "org/spongepowered/common/event/tracking/PhaseTracker", remap = false)
public class MixinPhaseTracker {

    /**
     * This basically replaces the World.setBlockState() when Sponge is loaded, so
     * we'll have to inject our hooks here as well when Sponge is loaded. All
     * setBlockState() calls that occur in Sponge get sent through here.
     *
     * @param mixinWorld
     * @param pos
     * @param newState
     * @param flag
     * @param info
     */
    @Inject(method = "setBlockState(Lorg/spongepowered/common/interfaces/world/IMixinWorldServer;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Lorg/spongepowered/api/world/BlockChangeFlag;)Z", at = @At(value = "HEAD"))
    public void preSetBlockState2(IMixinWorldServer mixinWorld, BlockPos pos, IBlockState newState,
                                  BlockChangeFlag flag, CallbackInfoReturnable info) {
        World world = (World) mixinWorld;
        PhysicsWrapperEntity physEntity = ValkyrienWarfareMod.VW_PHYSICS_MANAGER.getObjectManagingPos(world, pos);
        if (physEntity != null) {
            IBlockState oldState = world.getBlockState(pos);
            physEntity.getPhysicsObject().onSetBlockState(oldState, newState, pos);
        }
    }
}
