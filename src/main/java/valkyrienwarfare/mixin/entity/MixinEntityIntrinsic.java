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

package valkyrienwarfare.mixin.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import valkyrienwarfare.deprecated_api.MixinMethods;
import valkyrienwarfare.physics.collision.EntityCollisionInjector;
import valkyrienwarfare.physics.collision.EntityCollisionInjector.IntermediateMovementVariableStorage;

@Mixin(value = Entity.class, priority = 1)
public abstract class MixinEntityIntrinsic {

    @Shadow
    public double posX;
    @Shadow
    public double posY;
    @Shadow
    public double posZ;
    @Shadow
    public World world;
    public Entity thisClassAsAnEntity = Entity.class.cast(this);
    private IntermediateMovementVariableStorage alteredMovement = null;
    private boolean hasChanged = false;

    @Shadow
    public abstract void move(MoverType type, double x, double y, double z);

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    public void changeMoveArgs(MoverType type, double dx, double dy, double dz, CallbackInfo callbackInfo) {
        if (!this.hasChanged) {
            this.alteredMovement = MixinMethods.handleMove(type, dx, dy, dz, this.thisClassAsAnEntity);
            if (this.alteredMovement != null) {
                this.hasChanged = true;
                this.move(type, this.alteredMovement.dxyz.X, this.alteredMovement.dxyz.Y, this.alteredMovement.dxyz.Z);
                this.hasChanged = false;
                callbackInfo.cancel();
            }
        }
    }

    @Inject(method = "move", at = @At("RETURN"))
    public void postMove(CallbackInfo callbackInfo) {
        if (this.hasChanged) {
            EntityCollisionInjector.alterEntityMovementPost(this.thisClassAsAnEntity, this.alteredMovement);
        }
    }
}
