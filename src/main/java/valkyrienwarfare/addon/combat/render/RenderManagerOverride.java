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

package valkyrienwarfare.addon.combat.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import valkyrienwarfare.ValkyrienWarfareMod;

import javax.annotation.Nullable;
import java.util.Map;

public class RenderManagerOverride extends RenderManager {

    private final RenderManager def;

    public RenderManagerOverride(RenderManager def) {
        super(def.renderEngine, Minecraft.getMinecraft().getRenderItem());
        this.def = def;
    }

    /*
     * INTERCEPT
     */

    private boolean shouldRender(Entity entity) {
        return !ValkyrienWarfareMod.VW_PHYSICS_MANAGER.isEntityFixed(entity);
    }

    @Override
    public boolean shouldRender(Entity entityIn, ICamera camera, double camX, double camY, double camZ) {
        return this.shouldRender(entityIn) && this.def.shouldRender(entityIn, camera, camX, camY, camZ);
    }

    @Override
    public void renderEntityStatic(Entity p_188388_1_, float p_188388_2_, boolean p_188388_3_) {
        if (this.shouldRender(p_188388_1_))
            this.def.renderEntityStatic(p_188388_1_, p_188388_2_, p_188388_3_);
    }

    @Override
    public void renderEntity(Entity entityIn, double x, double y, double z, float yaw, float partialTicks, boolean p_188391_10_) {
        if (this.shouldRender(entityIn))
            this.def.renderEntity(entityIn, x, y, z, yaw, partialTicks, p_188391_10_);
    }

    /*
     * We don't care
     */

    @Override
    public Map<String, RenderPlayer> getSkinMap() {
        return this.def.getSkinMap();
    }

    @Override
    public void setRenderPosition(double renderPosXIn, double renderPosYIn, double renderPosZIn) {
        this.def.setRenderPosition(renderPosXIn, renderPosYIn, renderPosZIn);
    }

    @Override
    public <T extends Entity> Render<T> getEntityClassRenderObject(Class<? extends Entity> entityClass) {
        return this.def.getEntityClassRenderObject(entityClass);
    }

    @Override
    @Nullable
    public <T extends Entity> Render<T> getEntityRenderObject(Entity entityIn) {
        return this.def.getEntityRenderObject(entityIn);
    }

    @Override
    public void cacheActiveRenderInfo(World worldIn, FontRenderer textRendererIn, Entity livingPlayerIn, Entity pointedEntityIn, GameSettings optionsIn, float partialTicks) {
        this.def.cacheActiveRenderInfo(worldIn, textRendererIn, livingPlayerIn, pointedEntityIn, optionsIn, partialTicks);
    }

    @Override
    public void setPlayerViewY(float playerViewYIn) {
        this.def.setPlayerViewY(playerViewYIn);
    }

    @Override
    public boolean isRenderShadow() {
        return this.def.isRenderShadow();
    }

    @Override
    public void setRenderShadow(boolean renderShadowIn) {
        this.def.setRenderShadow(renderShadowIn);
    }

    @Override
    public boolean isDebugBoundingBox() {
        return this.def.isDebugBoundingBox();
    }

    @Override
    public void setDebugBoundingBox(boolean debugBoundingBoxIn) {
        this.def.setDebugBoundingBox(debugBoundingBoxIn);
    }

    @Override
    public boolean isRenderMultipass(Entity p_188390_1_) {
        return this.def.isRenderMultipass(p_188390_1_);
    }

    @Override
    public void renderMultipass(Entity p_188389_1_, float p_188389_2_) {
        this.def.renderMultipass(p_188389_1_, p_188389_2_);
    }

    /**
     * World sets this RenderManager's worldObj to the world provided
     */
//	@Override
//	public void set(@Nullable World worldIn){
//		def.setWorld(worldIn);
//	}
    @Override
    public double getDistanceToCamera(double x, double y, double z) {
        return this.def.getDistanceToCamera(x, y, z);
    }

    /**
     * Returns the font renderer
     */
    @Override
    public FontRenderer getFontRenderer() {
        return this.def.getFontRenderer();
    }

    @Override
    public void setRenderOutlines(boolean renderOutlinesIn) {
        this.def.setRenderOutlines(renderOutlinesIn);
    }

}
