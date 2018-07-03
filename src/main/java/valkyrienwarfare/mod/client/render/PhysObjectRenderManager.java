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

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;
import valkyrienwarfare.api.TransformType;
import valkyrienwarfare.math.Quaternion;
import valkyrienwarfare.math.Vector;
import valkyrienwarfare.mod.proxy.ClientProxy;
import valkyrienwarfare.physics.management.PhysicsObject;
import valkyrienwarfare.physics.management.PhysicsWrapperEntity;

/**
 * Object owned by each physObject responsible for handling all rendering
 * operations
 *
 * @author thebest108
 */
public class PhysObjectRenderManager {

    // This pos is used to prevent Z-Buffer Errors D:
    // It's actual value is completely irrelevant as long as it's close to the
    // Ship's centerBlockPos
    public BlockPos offsetPos;
    public double curPartialTick;
    private int glCallListSolid;
    private int glCallListTranslucent;
    private int glCallListCutout;
    private int glCallListCutoutMipped;
    private PhysicsObject parent;
    private PhysRenderChunk[][] renderChunks;

    public PhysObjectRenderManager(PhysicsObject toRender) {
        this.parent = toRender;
        this.glCallListSolid = -1;
        this.glCallListTranslucent = -1;
        this.glCallListCutout = -1;
        this.glCallListCutoutMipped = -1;
        this.offsetPos = null;
        this.curPartialTick = 0;
        this.renderChunks = null;
    }

    public void updateOffsetPos(BlockPos newPos) {
        this.offsetPos = newPos;
    }

    public void renderBlockLayer(BlockRenderLayer layerToRender, double partialTicks, int pass) {
        if (this.renderChunks == null) {
            if (!this.parent.areShipChunksFullyLoaded()) {
                return;
            }
            this.renderChunks = new PhysRenderChunk[this.parent.getOwnedChunks().getChunkLengthX()][this.parent.getOwnedChunks()
                    .getChunkLengthZ()];
            for (int xChunk = 0; xChunk < this.parent.getOwnedChunks().getChunkLengthX(); xChunk++) {
                for (int zChunk = 0; zChunk < this.parent.getOwnedChunks().getChunkLengthZ(); zChunk++) {
                    this.renderChunks[xChunk][zChunk] = new PhysRenderChunk(this.parent, this.parent.getChunkCache()
                            .getChunkAt(xChunk + this.parent.getOwnedChunks().getMinX(), zChunk + this.parent.getOwnedChunks().getMinZ()));
                }
            }
        }

        GL11.glPushMatrix();
        Minecraft.getMinecraft().entityRenderer.enableLightmap();
        // int i = parent.wrapper.getBrightnessForRender((float) partialTicks);

        // int j = i % 65536;
        // int k = i / 65536;
        // OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)
        // j, (float) k);
        // GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        this.setupTranslation(partialTicks);
        for (PhysRenderChunk[] chunkArray : this.renderChunks) {
            for (PhysRenderChunk renderChunk : chunkArray) {
                renderChunk.renderBlockLayer(layerToRender, partialTicks, pass);
            }
        }

        Minecraft.getMinecraft().entityRenderer.disableLightmap();
        GL11.glPopMatrix();
    }

    public void killRenderers() {
        if (this.renderChunks != null) {
            for (PhysRenderChunk[] chunks : this.renderChunks) {
                for (PhysRenderChunk chunk : chunks) {
                    chunk.killRenderChunk();
                }
            }
        }
    }

    public void updateRange(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean updateImmediately) {
        if (this.renderChunks == null || this.parent == null || this.parent.getOwnedChunks() == null) {
            return;
        }

        int size = (maxX + 1 - minX) * (maxZ + 1 - minZ) * (maxY + 1 - minY);

        if (size > 65535) {
            return;
        }

        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        int minBlockArrayY = Math.max(0, minY >> 4);
        int maxBlockArrayY = Math.min(15, maxY >> 4);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                // TODO: Fix this render bug
                try {
                    if (chunkX >= this.parent.getOwnedChunks().getMinX() && chunkZ >= this.parent.getOwnedChunks().getMinZ()
                            && chunkX - this.parent.getOwnedChunks().getMinX() < this.renderChunks.length
                            && chunkZ - this.parent.getOwnedChunks().getMinZ() < this.renderChunks[0].length) {
                        PhysRenderChunk renderChunk = this.renderChunks[chunkX - this.parent.getOwnedChunks().getMinX()][chunkZ
                                - this.parent.getOwnedChunks().getMinZ()];
                        if (renderChunk != null) {
                            renderChunk.updateLayers(minBlockArrayY, maxBlockArrayY);
                        } else {
                            System.err.println("SHIP RENDER CHUNK CAME OUT NULL! THIS IS VERY WRONG!!");
                        }
                    } else {
                        // ValkyrienWarfareMod.VWLogger.info("updateRange Just attempted to update
                        // blocks outside of a Ship's block Range. ANY ERRORS PAST THIS ARE LIKELY
                        // RELATED!");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean shouldRender() {
        ICamera camera = ClientProxy.lastCamera;
        return camera == null || camera.isBoundingBoxInFrustum(this.parent.getShipBoundingBox());
    }

    public void setupTranslation(double partialTicks) {
        this.updateTranslation(partialTicks);
        // if(curPartialTick!=partialTicks){
        // updateTranslation(partialTicks);
        // }else{
        // updateTranslation(partialTicks);
        // curPartialTick = partialTicks;
        // }
    }

    public void updateTranslation(double partialTicks) {
        PhysicsWrapperEntity entity = this.parent.getWrapperEntity();
        Vector centerOfRotation = entity.getPhysicsObject().getCenterCoord();
        this.curPartialTick = partialTicks;

        double moddedX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double moddedY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double moddedZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;

        // System.out.println(entity.roll - entity.prevRoll);

        double p0 = Minecraft.getMinecraft().player.lastTickPosX
                + (Minecraft.getMinecraft().player.posX - Minecraft.getMinecraft().player.lastTickPosX) * partialTicks;
        double p1 = Minecraft.getMinecraft().player.lastTickPosY
                + (Minecraft.getMinecraft().player.posY - Minecraft.getMinecraft().player.lastTickPosY) * partialTicks;
        double p2 = Minecraft.getMinecraft().player.lastTickPosZ
                + (Minecraft.getMinecraft().player.posZ - Minecraft.getMinecraft().player.lastTickPosZ) * partialTicks;

        Quaternion smoothRotation = this.getSmoothRotationQuat(partialTicks);
        double[] radians = smoothRotation.toRadians();

        double moddedPitch = Math.toDegrees(radians[0]);
        double moddedYaw = Math.toDegrees(radians[1]);
        double moddedRoll = Math.toDegrees(radians[2]);

        this.parent.getShipTransformationManager().updateRenderTransform(moddedX, moddedY, moddedZ, moddedPitch, moddedYaw, moddedRoll);

        if (this.offsetPos != null) {
            double offsetX = this.offsetPos.getX() - centerOfRotation.X;
            double offsetY = this.offsetPos.getY() - centerOfRotation.Y;
            double offsetZ = this.offsetPos.getZ() - centerOfRotation.Z;

            GlStateManager.translate(-p0 + moddedX, -p1 + moddedY, -p2 + moddedZ);
            GL11.glRotated(moddedPitch, 1D, 0, 0);
            GL11.glRotated(moddedYaw, 0, 1D, 0);
            GL11.glRotated(moddedRoll, 0, 0, 1D);
            GL11.glTranslated(offsetX, offsetY, offsetZ);
            // transformBuffer = BufferUtils.createFloatBuffer(16);
        }
    }

    public Quaternion getSmoothRotationQuat(double partialTick) {
        Quaternion oneTickBefore = this.parent.getShipTransformationManager().getPrevTickTransform()
                .createRotationQuaternion(TransformType.SUBSPACE_TO_GLOBAL);
        Quaternion nextQuat = this.parent.getShipTransformationManager().getCurrentTickTransform()
                .createRotationQuaternion(TransformType.SUBSPACE_TO_GLOBAL);
        return Quaternion.slerpInterpolate(oneTickBefore, nextQuat, partialTick);
    }

    public void inverseTransform(double partialTicks) {
        PhysicsWrapperEntity entity = this.parent.getWrapperEntity();
        Vector centerOfRotation = entity.getPhysicsObject().getCenterCoord();
        this.curPartialTick = partialTicks;

        double moddedX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double moddedY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double moddedZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        double p0 = Minecraft.getMinecraft().player.lastTickPosX
                + (Minecraft.getMinecraft().player.posX - Minecraft.getMinecraft().player.lastTickPosX) * partialTicks;
        double p1 = Minecraft.getMinecraft().player.lastTickPosY
                + (Minecraft.getMinecraft().player.posY - Minecraft.getMinecraft().player.lastTickPosY) * partialTicks;
        double p2 = Minecraft.getMinecraft().player.lastTickPosZ
                + (Minecraft.getMinecraft().player.posZ - Minecraft.getMinecraft().player.lastTickPosZ) * partialTicks;

        Quaternion smoothRotation = this.getSmoothRotationQuat(partialTicks);
        double[] radians = smoothRotation.toRadians();

        double moddedPitch = Math.toDegrees(radians[0]);
        double moddedYaw = Math.toDegrees(radians[1]);
        double moddedRoll = Math.toDegrees(radians[2]);

        // parent.coordTransform.updateRenderMatrices(moddedX, moddedY, moddedZ,
        // moddedPitch, moddedYaw, moddedRoll);

        if (this.offsetPos != null) {
            double offsetX = this.offsetPos.getX() - centerOfRotation.X;
            double offsetY = this.offsetPos.getY() - centerOfRotation.Y;
            double offsetZ = this.offsetPos.getZ() - centerOfRotation.Z;

            GL11.glTranslated(-offsetX, -offsetY, -offsetZ);
            GL11.glRotated(-moddedRoll, 0, 0, 1D);
            GL11.glRotated(-moddedYaw, 0, 1D, 0);
            GL11.glRotated(-moddedPitch, 1D, 0, 0);
            GlStateManager.translate(p0 - moddedX, p1 - moddedY, p2 - moddedZ);
            // transformBuffer = BufferUtils.createFloatBuffer(16);
        }
    }

}
