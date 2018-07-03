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

package valkyrienwarfare.mod.client.render;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.client.ForgeHooksClient;
import org.lwjgl.opengl.GL11;
import valkyrienwarfare.physics.management.PhysicsObject;

import java.util.ArrayList;
import java.util.List;

public class PhysRenderChunk {

    public RenderLayer[] layers = new RenderLayer[16];
    public PhysicsObject toRender;
    public Chunk renderChunk;

    public PhysRenderChunk(PhysicsObject toRender, Chunk renderChunk) {
        this.toRender = toRender;
        this.renderChunk = renderChunk;
        for (int i = 0; i < 16; i++) {
            ExtendedBlockStorage storage = renderChunk.storageArrays[i];
            if (storage != null) {
                RenderLayer renderLayer = new RenderLayer(renderChunk, i * 16, i * 16 + 15, this);
                this.layers[i] = renderLayer;
            }
        }
    }

    public void renderBlockLayer(BlockRenderLayer layerToRender, double partialTicks, int pass) {
        for (int i = 0; i < 16; i++) {
            RenderLayer layer = this.layers[i];
            if (layer != null) {
                layer.renderBlockLayer(layerToRender, partialTicks, pass);
            }
        }
    }

    public void updateLayers(int minLayer, int maxLayer) {
        for (int layerY = minLayer; layerY <= maxLayer; layerY++) {
            RenderLayer layer = this.layers[layerY];
            if (layer != null) {
                layer.markDirtyRenderLists();
            } else {
                RenderLayer renderLayer = new RenderLayer(this.renderChunk, layerY * 16, layerY * 16 + 15, this);
                this.layers[layerY] = renderLayer;
            }
        }
    }

    public void killRenderChunk() {
        for (int i = 0; i < 16; i++) {
            RenderLayer layer = this.layers[i];
            if (layer != null) {
                layer.deleteRenderLayer();
            }
        }
    }

    public class RenderLayer {

        Chunk chunkToRender;
        int yMin, yMax;
        int glCallListCutout, glCallListCutoutMipped, glCallListSolid, glCallListTranslucent;
        PhysRenderChunk parent;
        boolean needsCutoutUpdate, needsCutoutMippedUpdate, needsSolidUpdate, needsTranslucentUpdate;
        List<TileEntity> renderTiles = new ArrayList<>();

        public RenderLayer(Chunk chunk, int yMin, int yMax, PhysRenderChunk parent) {
            this.chunkToRender = chunk;
            this.yMin = yMin;
            this.yMax = yMax;
            this.parent = parent;
            this.markDirtyRenderLists();
            this.glCallListCutout = GLAllocation.generateDisplayLists(4);
            this.glCallListCutoutMipped = this.glCallListCutout + 1;
            this.glCallListSolid = this.glCallListCutout + 2;
            this.glCallListTranslucent = this.glCallListCutout + 3;
        }

        public void markDirtyRenderLists() {
            this.needsCutoutUpdate = true;
            this.needsCutoutMippedUpdate = true;
            this.needsSolidUpdate = true;
            this.needsTranslucentUpdate = true;
            this.updateRenderTileEntities();
        }

        // TODO: There's probably a faster way of doing this.
        public void updateRenderTileEntities() {
            ITileEntitiesToRenderProvider provider = ITileEntitiesToRenderProvider.class.cast(this.chunkToRender);
            List<TileEntity> updatedRenderTiles = provider.getTileEntitiesToRender(this.yMin >> 4);
            if (updatedRenderTiles != null) {
                Minecraft.getMinecraft().renderGlobal.updateTileEntities(this.renderTiles, updatedRenderTiles);
                this.renderTiles = new ArrayList<>(updatedRenderTiles);
            }
        }

        public void deleteRenderLayer() {
            this.clearRenderLists();
            Minecraft.getMinecraft().renderGlobal.updateTileEntities(this.renderTiles, new ArrayList());
            this.renderTiles.clear();
        }

        private void clearRenderLists() {
            GLAllocation.deleteDisplayLists(this.glCallListCutout);
            GLAllocation.deleteDisplayLists(this.glCallListCutoutMipped);
            GLAllocation.deleteDisplayLists(this.glCallListSolid);
            GLAllocation.deleteDisplayLists(this.glCallListTranslucent);
        }

        public void renderBlockLayer(BlockRenderLayer layerToRender, double partialTicks, int pass) {
            switch (layerToRender) {
                case CUTOUT:
                    if (this.needsCutoutUpdate) {
                        this.updateList(layerToRender);
                    }
                    GL11.glCallList(this.glCallListCutout);
                    break;
                case CUTOUT_MIPPED:
                    if (this.needsCutoutMippedUpdate) {
                        this.updateList(layerToRender);
                    }
                    GL11.glCallList(this.glCallListCutoutMipped);
                    break;
                case SOLID:
                    if (this.needsSolidUpdate) {
                        this.updateList(layerToRender);
                    }
                    GL11.glCallList(this.glCallListSolid);
                    break;
                case TRANSLUCENT:
                    if (this.needsTranslucentUpdate) {
                        this.updateList(layerToRender);
                    }
                    GL11.glCallList(this.glCallListTranslucent);
                    break;
                default:
                    break;
            }
        }

        public void updateList(BlockRenderLayer layerToUpdate) {
            if (this.parent.toRender.getShipRenderer() == null) {
                return;
            }
            BlockPos offsetPos = this.parent.toRender.getShipRenderer().offsetPos;
            if (offsetPos == null) {
                return;
            }
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder worldrenderer = tessellator.getBuffer();
            worldrenderer.begin(7, DefaultVertexFormats.BLOCK);
            worldrenderer.setTranslation(-offsetPos.getX(), -offsetPos.getY(), -offsetPos.getZ());
            GL11.glPushMatrix();
            switch (layerToUpdate) {
                case CUTOUT:
                    GLAllocation.deleteDisplayLists(this.glCallListCutout);
                    GL11.glNewList(this.glCallListCutout, GL11.GL_COMPILE);
                    break;
                case CUTOUT_MIPPED:
                    GLAllocation.deleteDisplayLists(this.glCallListCutoutMipped);
                    GL11.glNewList(this.glCallListCutoutMipped, GL11.GL_COMPILE);
                    break;
                case SOLID:
                    GLAllocation.deleteDisplayLists(this.glCallListSolid);
                    GL11.glNewList(this.glCallListSolid, GL11.GL_COMPILE);
                    break;
                case TRANSLUCENT:
                    GLAllocation.deleteDisplayLists(this.glCallListTranslucent);
                    GL11.glNewList(this.glCallListTranslucent, GL11.GL_COMPILE);
                    break;
                default:
                    break;
            }

            GlStateManager.pushMatrix();
            // worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            IBlockState iblockstate;
            // if (Minecraft.isAmbientOcclusionEnabled()) {
            // GlStateManager.shadeModel(GL11.GL_SMOOTH);
            // } else {
            // GlStateManager.shadeModel(GL11.GL_FLAT);
            // }
            ForgeHooksClient.setRenderLayer(layerToUpdate);
            MutableBlockPos pos = new MutableBlockPos();
            for (int x = this.chunkToRender.x * 16; x < this.chunkToRender.x * 16 + 16; x++) {
                for (int z = this.chunkToRender.z * 16; z < this.chunkToRender.z * 16 + 16; z++) {
                    for (int y = this.yMin; y <= this.yMax; y++) {
                        pos.setPos(x, y, z);
                        iblockstate = this.chunkToRender.getBlockState(pos);
                        try {
                            if (iblockstate.getBlock().canRenderInLayer(iblockstate, layerToUpdate)) {
                                Minecraft.getMinecraft().getBlockRendererDispatcher().renderBlock(iblockstate, pos, this.chunkToRender.world, worldrenderer);
                            }
                        } catch (NullPointerException e) {
                            System.out.println("Something was null! LValkyrienWarfareBase/render/PhysRenderChunk#updateList");
                        }
                    }
                }
            }
            tessellator.draw();
            // worldrenderer.finishDrawing();
            ForgeHooksClient.setRenderLayer(null);
            GlStateManager.popMatrix();
            GL11.glEndList();
            GL11.glPopMatrix();
            worldrenderer.setTranslation(0, 0, 0);

            switch (layerToUpdate) {
                case CUTOUT:
                    this.needsCutoutUpdate = false;
                    break;
                case CUTOUT_MIPPED:
                    this.needsCutoutMippedUpdate = false;
                    break;
                case SOLID:
                    this.needsSolidUpdate = false;
                    break;
                case TRANSLUCENT:
                    this.needsTranslucentUpdate = false;
                    break;
                default:
                    break;
            }
        }
    }
}
