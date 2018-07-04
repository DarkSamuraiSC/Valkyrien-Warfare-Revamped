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

package valkyrienwarfare.physics.management;

import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.network.play.server.SPacketUnloadChunk;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkProviderServer;
import valkyrienwarfare.ValkyrienWarfareMod;
import valkyrienwarfare.addon.control.ValkyrienWarfareControl;
import valkyrienwarfare.addon.control.network.EntityFixMessage;
import valkyrienwarfare.addon.control.nodenetwork.INodeController;
import valkyrienwarfare.addon.control.nodenetwork.IVWNodeProvider;
import valkyrienwarfare.addon.ftbutil.ValkyrienWarfareFTBUtil;
import valkyrienwarfare.api.TransformType;
import valkyrienwarfare.math.Quaternion;
import valkyrienwarfare.math.Vector;
import valkyrienwarfare.mod.BlockPhysicsRegistration;
import valkyrienwarfare.mod.client.render.PhysObjectRenderManager;
import valkyrienwarfare.mod.coordinates.ISubspace;
import valkyrienwarfare.mod.coordinates.ISubspaceProvider;
import valkyrienwarfare.mod.coordinates.ImplSubspace;
import valkyrienwarfare.mod.coordinates.ShipTransform;
import valkyrienwarfare.mod.coordinates.ShipTransformationPacketHolder;
import valkyrienwarfare.mod.network.PhysWrapperPositionMessage;
import valkyrienwarfare.mod.physmanagement.chunk.PhysicsChunkManager;
import valkyrienwarfare.mod.physmanagement.chunk.VWChunkCache;
import valkyrienwarfare.mod.physmanagement.chunk.VWChunkClaim;
import valkyrienwarfare.mod.physmanagement.relocation.DetectorManager;
import valkyrienwarfare.mod.physmanagement.relocation.SpatialDetector;
import valkyrienwarfare.mod.schematics.SchematicReader.Schematic;
import valkyrienwarfare.physics.BlockForce;
import valkyrienwarfare.physics.PhysicsCalculations;
import valkyrienwarfare.util.NBTUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The heart and soul of this mod. The physics object does everything from
 * custom collision, block interactions, physics, networking, rendering, and
 * more!
 *
 * @author thebest108
 */
@Getter
public class PhysicsObject implements ISubspaceProvider {

    public static final int MIN_TICKS_EXISTED_BEFORE_PHYSICS = 5;
    private final PhysicsWrapperEntity wrapper;
    private final List<EntityPlayerMP> watchingPlayers;
    private PhysObjectRenderManager shipRenderer;
    // This is used to delay mountEntity() operations by 1 tick
    private final List<Entity> queuedEntitiesToMount;
    // Used when rendering to avoid horrible floating point errors, just a random
    // blockpos inside the ship space.
    @Setter
    private BlockPos refrenceBlockPos;
    @Setter
    private Vector centerCoord;
    @Setter(AccessLevel.PRIVATE)
    private ShipTransformationManager shipTransformationManager;
    @Setter
    private PhysicsCalculations physicsProcessor;
    private final Set<INodeController> physicsControllers;
    private final Set<INodeController> physicsControllersImmutable;
    @Setter
    private int detectorID;
    // The closest Chunks to the Ship cached in here
    @Setter
    private ChunkCache cachedSurroundingChunks;
    // TODO: Make for re-organizing these to make Ship sizes Dynamic
    @Setter
    private VWChunkClaim ownedChunks;
    // Used for faster memory access to the Chunks this object 'owns'
    private Chunk[][] claimedChunks;
    @Setter
    private VWChunkCache chunkCache;
    // Some badly written mods use these Maps to determine who to send packets to,
    // so we need to manually fill them with nearby players
    private PlayerChunkMapEntry[][] claimedChunksEntries;
    // Compatibility for ships made before the update
    private boolean claimedChunksInMap;
    private boolean isNameCustom;
    @Setter
    private AxisAlignedBB shipBoundingBox;
    private TIntObjectMap<Vector> entityLocalPositions;
    @Setter
    private ShipType shipType;
    private volatile int gameConsecutiveTicks;
    private volatile int physicsConsecutiveTicks;
    private final ISubspace shipSubspace;
    // Has to be concurrent, only exists properly on the server. Do not use this for
    // anything client side!
    @Setter
    private Set<BlockPos> blockPositions;
    private boolean isPhysicsEnabled;
    @Setter
    private GameProfile owner;

    public PhysicsObject(@NonNull PhysicsWrapperEntity host) {
        this.wrapper = host;
        if (host.world.isRemote) {
            this.shipRenderer = new PhysObjectRenderManager(this);
        }
        this.isNameCustom = false;
        this.claimedChunksInMap = false;
        this.queuedEntitiesToMount = new ArrayList<>();
        this.entityLocalPositions = new TIntObjectHashMap<>();
        this.isPhysicsEnabled = false;
        // We need safe access to this across multiple threads.
        this.setBlockPositions(ConcurrentHashMap.newKeySet());
        this.shipBoundingBox = Entity.ZERO_AABB;
        this.watchingPlayers = new ArrayList<>();
        this.isPhysicsEnabled = false;
        this.gameConsecutiveTicks = 0;
        this.physicsConsecutiveTicks = 0;
        this.shipSubspace = new ImplSubspace(this);
        this.physicsControllers = Sets.newConcurrentHashSet();
        this.physicsControllersImmutable = Collections.unmodifiableSet(this.physicsControllers);
    }

    public void onSetBlockState(IBlockState oldState, IBlockState newState, BlockPos posAt) {
        if (this.getWorldObj().isRemote) {
            return;
        }

        if (!this.ownedChunks.isChunkEnclosedInSet(posAt.getX() >> 4, posAt.getZ() >> 4)) {
            return;
        }

        // If the block here is not to be made with physics, just treat it like you'd
        // treat AIR blocks.
        if (oldState != null && BlockPhysicsRegistration.blocksToNotPhysicise.contains(oldState.getBlock())) {
            oldState = Blocks.AIR.getDefaultState();
        }
        if (newState != null && BlockPhysicsRegistration.blocksToNotPhysicise.contains(newState.getBlock())) {
            newState = Blocks.AIR.getDefaultState();
        }

        boolean isOldAir = oldState == null || oldState.getBlock().equals(Blocks.AIR);
        boolean isNewAir = newState == null || newState.getBlock().equals(Blocks.AIR);

        if (isNewAir) {
            this.blockPositions.remove(posAt);
        }

        if ((isOldAir && !isNewAir)) {
            this.blockPositions.add(posAt);
            int chunkX = (posAt.getX() >> 4) - this.claimedChunks[0][0].x;
            int chunkZ = (posAt.getZ() >> 4) - this.claimedChunks[0][0].z;
            this.ownedChunks.markChunkOccupied(chunkX, chunkZ, this);
        }

        if (this.blockPositions.isEmpty()) {
            this.destroy();
        }

        if (this.physicsProcessor != null) {
            this.physicsProcessor.onSetBlockState(oldState, newState, posAt);
        }
    }

    public void destroy() {
        this.wrapper.setDead();
        List<EntityPlayerMP> watchersCopy = new ArrayList<>(this.watchingPlayers);
        for (int x = this.ownedChunks.getMinX(); x <= this.ownedChunks.getMaxX(); x++) {
            for (int z = this.ownedChunks.getMinZ(); z <= this.ownedChunks.getMaxZ(); z++) {
                SPacketUnloadChunk unloadPacket = new SPacketUnloadChunk(x, z);
                for (EntityPlayerMP wachingPlayer : watchersCopy) {
                    wachingPlayer.connection.sendPacket(unloadPacket);
                }
            }
            // NOTICE: This method isnt being called to avoid the
            // watchingPlayers.remove(player) call, which is a waste of CPU time
            // onPlayerUntracking(wachingPlayer);
        }
        this.watchingPlayers.clear();

        ValkyrienWarfareFTBUtil.handleUnclaim(this);
        ValkyrienWarfareMod.VW_CHUNK_MANAGER.getManagerForWorld(this.getWorldObj()).markChunksAvailable(this.ownedChunks);
        ValkyrienWarfareMod.VW_CHUNK_MANAGER.removeRegisteredChunksForShip(this.wrapper);
        ValkyrienWarfareMod.VW_CHUNK_MANAGER.removeShipPosition(this.wrapper);
        ValkyrienWarfareMod.VW_CHUNK_MANAGER.removeShipNameRegistry(this.wrapper);
        ValkyrienWarfareMod.VW_PHYSICS_MANAGER.onShipUnload(this.wrapper);
    }

    public void claimNewChunks(int radius) {
        this.setOwnedChunks(ValkyrienWarfareMod.VW_CHUNK_MANAGER.getManagerForWorld(this.wrapper.world)
                .getNextAvailableChunkSet(radius));
        ValkyrienWarfareMod.VW_CHUNK_MANAGER.registerChunksForShip(this.wrapper);
        this.claimedChunksInMap = true;
    }

    /**
     * Generates the new chunks
     */
    public void processChunkClaims(EntityPlayer player) {
        BlockPos centerInWorld = new BlockPos(this.wrapper.posX, this.wrapper.posY, this.wrapper.posZ);
        SpatialDetector detector = DetectorManager.getDetectorFor(this.detectorID, centerInWorld, this.getWorldObj(),
                ValkyrienWarfareMod.maxShipSize + 1, true);
        if (detector.foundSet.size() > ValkyrienWarfareMod.maxShipSize || detector.cleanHouse) {
            if (player != null) {
                player.sendMessage(new TextComponentString(
                        "Ship construction canceled because its exceeding the ship size limit; or because it's attatched to bedrock. Raise it with /physsettings maxshipsize [number]"));
            }
            this.wrapper.setDead();
            return;
        }
        this.assembleShip(player, detector, centerInWorld);
    }

    public void processChunkClaims(Schematic toFollow) {
        BlockPos centerInWorld = new BlockPos(-(toFollow.width / 2), 128 - (toFollow.height / 2),
                -(toFollow.length / 2));

        int radiusNeeded = (Math.max(toFollow.length, toFollow.width) / 16) + 2;

        // System.out.println(radiusNeeded);

        this.claimNewChunks(radiusNeeded);

        ValkyrienWarfareMod.VW_PHYSICS_MANAGER.onShipPreload(this.wrapper);

        this.claimedChunks = new Chunk[(this.ownedChunks.getRadius() * 2) + 1][(this.ownedChunks.getRadius() * 2) + 1];
        this.claimedChunksEntries = new PlayerChunkMapEntry[(this.ownedChunks.getRadius() * 2) + 1][(this.ownedChunks.getRadius() * 2) + 1];
        for (int x = this.ownedChunks.getMinX(); x <= this.ownedChunks.getMaxX(); x++) {
            for (int z = this.ownedChunks.getMinZ(); z <= this.ownedChunks.getMaxZ(); z++) {
                Chunk chunk = new Chunk(this.getWorldObj(), x, z);
                this.injectChunkIntoWorld(chunk, x, z, true);
                this.claimedChunks[x - this.ownedChunks.getMinX()][z - this.ownedChunks.getMinZ()] = chunk;
            }
        }

        this.replaceOuterChunksWithAir();

        this.setChunkCache(new VWChunkCache(this.getWorldObj(), this.claimedChunks));

        this.setRefrenceBlockPos(this.getRegionCenter());
        this.setCenterCoord(new Vector(this.refrenceBlockPos.getX(), this.refrenceBlockPos.getY(), this.refrenceBlockPos.getZ()));

        this.createPhysicsCalculations();
        BlockPos centerDifference = this.refrenceBlockPos.subtract(centerInWorld);

        toFollow.placeBlockAndTilesInWorld(this.getWorldObj(), centerDifference);

        this.detectBlockPositions();

        // TODO: This fixes the lighting, but it adds lag; maybe remove this
        for (int x = this.ownedChunks.getMinX(); x <= this.ownedChunks.getMaxX(); x++) {
            for (int z = this.ownedChunks.getMinZ(); z <= this.ownedChunks.getMaxZ(); z++) {
                // claimedChunks[x - ownedChunks.minX][z - ownedChunks.minZ].isTerrainPopulated
                // = true;
                // claimedChunks[x - ownedChunks.minX][z -
                // ownedChunks.minZ].generateSkylightMap();
                // claimedChunks[x - ownedChunks.minX][z - ownedChunks.minZ].checkLight();
            }
        }

        this.setShipTransformationManager(new ShipTransformationManager(this));
        this.physicsProcessor.processInitialPhysicsData();
        this.physicsProcessor.updateParentCenterOfMass();

        this.shipTransformationManager.updateAllTransforms(false, false);
    }

    /**
     * Creates the PhysicsProcessor object before any data gets loaded into it; can
     * be overridden to change the class of the Object
     */
    private void createPhysicsCalculations() {
        if (this.physicsProcessor == null) {
            this.setPhysicsProcessor(new PhysicsCalculations(this));
        }
    }

    private void assembleShip(EntityPlayer player, SpatialDetector detector, BlockPos centerInWorld) {
        this.isPhysicsEnabled = true;
        MutableBlockPos pos = new MutableBlockPos();
        TIntIterator iter = detector.foundSet.iterator();
        /*int radiusNeeded = 1;

        while (iter.hasNext()) {
            int i = iter.next();
            SpatialDetector.setPosWithRespectTo(i, BlockPos.ORIGIN, pos);
            int xRad = Math.abs(pos.getX() >> 4);
            int zRad = Math.abs(pos.getZ() >> 4);
            radiusNeeded = Math.max(Math.max(zRad, xRad), radiusNeeded + 1);
        }

        radiusNeeded = Math.min(radiusNeeded,
                ValkyrienWarfareMod.VW_CHUNK_MANAGER.getManagerForWorld(getWrapperEntity().world).maxChunkRadius);*/
        this.claimNewChunks(PhysicsChunkManager.getMaxChunkRadius());
        ValkyrienWarfareMod.VW_PHYSICS_MANAGER.onShipPreload(this.wrapper);

        this.claimedChunks = new Chunk[(this.ownedChunks.getRadius() * 2) + 1][(this.ownedChunks.getRadius() * 2) + 1];
        this.claimedChunksEntries = new PlayerChunkMapEntry[(this.ownedChunks.getRadius() * 2) + 1][(this.ownedChunks.getRadius() * 2) + 1];
        for (int x = this.ownedChunks.getMinX(); x <= this.ownedChunks.getMaxX(); x++) {
            for (int z = this.ownedChunks.getMinZ(); z <= this.ownedChunks.getMaxZ(); z++) {
                Chunk chunk = new Chunk(this.getWorldObj(), x, z);
                this.injectChunkIntoWorld(chunk, x, z, true);
                this.claimedChunks[x - this.ownedChunks.getMinX()][z - this.ownedChunks.getMinZ()] = chunk;
            }
        }

        // Prevents weird shit from spawning at the edges of a ship
        this.replaceOuterChunksWithAir();

        this.setChunkCache(new VWChunkCache(this.getWorldObj(), this.claimedChunks));
        int minChunkX = this.claimedChunks[0][0].x;
        int minChunkZ = this.claimedChunks[0][0].z;

        this.setRefrenceBlockPos(this.getRegionCenter());
        this.setCenterCoord(new Vector(this.refrenceBlockPos.getX(), this.refrenceBlockPos.getY(), this.refrenceBlockPos.getZ()));

        this.createPhysicsCalculations();

        iter = detector.foundSet.iterator();
        BlockPos centerDifference = this.refrenceBlockPos.subtract(centerInWorld);
        while (iter.hasNext()) {
            int i = iter.next();
            SpatialDetector.setPosWithRespectTo(i, centerInWorld, pos);

            IBlockState state = detector.cache.getBlockState(pos);

            TileEntity worldTile = detector.cache.getTileEntity(pos);

            pos.setPos(pos.getX() + centerDifference.getX(), pos.getY() + centerDifference.getY(),
                    pos.getZ() + centerDifference.getZ());
            this.ownedChunks.markChunkOccupied((pos.getX() >> 4) - minChunkX, (pos.getZ() >> 4) - minChunkZ, this);

            Chunk chunkToSet = this.claimedChunks[(pos.getX() >> 4) - minChunkX][(pos.getZ() >> 4) - minChunkZ];
            int storageIndex = pos.getY() >> 4;

            if (chunkToSet.storageArrays[storageIndex] == Chunk.NULL_BLOCK_STORAGE) {
                chunkToSet.storageArrays[storageIndex] = new ExtendedBlockStorage(storageIndex << 4, true);
            }
            chunkToSet.storageArrays[storageIndex].set(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, state);

            // All this code just tries to transform the TileEntity properly without deleting its data.
            // TODO: Replace this with the system vanilla uses
            if (worldTile != null) {
                NBTTagCompound tileEntNBT = new NBTTagCompound();
                tileEntNBT = worldTile.writeToNBT(tileEntNBT);
                // Change the block position to be inside of the Ship
                tileEntNBT.setInteger("x", pos.getX());
                tileEntNBT.setInteger("y", pos.getY());
                tileEntNBT.setInteger("z", pos.getZ());

                TileEntity newInstance = TileEntity.create(this.getWorldObj(), tileEntNBT);
                // Order the IVWNodeProvider to move by the given offset.
                if (newInstance != null && newInstance instanceof IVWNodeProvider) {
                    IVWNodeProvider.class.cast(newInstance).shiftInternalData(centerDifference);
                    IVWNodeProvider.class.cast(newInstance).getNode().setParentPhysicsObject(this);
                }
                newInstance.validate();

                this.getWorldObj().setTileEntity(pos, newInstance);
                this.onSetTileEntity(pos, newInstance);
                newInstance.markDirty();
            }
        }
        iter = detector.foundSet.iterator();
        while (iter.hasNext()) {
            int i = iter.next();
            // BlockPos respectTo = detector.getPosWithRespectTo(i, centerInWorld);
            SpatialDetector.setPosWithRespectTo(i, centerInWorld, pos);
            // detector.cache.setBlockState(pos, Blocks.air.getDefaultState());
            // TODO: Get this to update on clientside as well, you bastard!
            TileEntity tile = this.getWorldObj().getTileEntity(pos);
            if (tile != null) {
                tile.invalidate();
            }
            this.getWorldObj().setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
        }

        for (int x = this.ownedChunks.getMinX(); x <= this.ownedChunks.getMaxX(); x++) {
            for (int z = this.ownedChunks.getMinZ(); z <= this.ownedChunks.getMaxZ(); z++) {
                this.claimedChunks[x - this.ownedChunks.getMinX()][z - this.ownedChunks.getMinZ()].isTerrainPopulated = true;
                this.claimedChunks[x - this.ownedChunks.getMinX()][z - this.ownedChunks.getMinZ()].generateSkylightMap();
                this.claimedChunks[x - this.ownedChunks.getMinX()][z - this.ownedChunks.getMinZ()].checkLight();
            }
        }

        this.detectBlockPositions();

        // TODO: This fixes the lighting, but it adds lag; maybe remove this
        for (int x = this.ownedChunks.getMinX(); x <= this.ownedChunks.getMaxX(); x++) {
            for (int z = this.ownedChunks.getMinZ(); z <= this.ownedChunks.getMaxZ(); z++) {
                // claimedChunks[x - ownedChunks.minX][z - ownedChunks.minZ].isTerrainPopulated
                // = true;
                // claimedChunks[x - ownedChunks.minX][z -
                // ownedChunks.minZ].generateSkylightMap();
                // claimedChunks[x - ownedChunks.minX][z - ownedChunks.minZ].checkLight();
            }
        }

        this.setShipTransformationManager(new ShipTransformationManager(this));
        this.physicsProcessor.processInitialPhysicsData();
        this.physicsProcessor.updateParentCenterOfMass();
    }

    public void injectChunkIntoWorld(Chunk chunk, int x, int z, boolean putInId2ChunkMap) {
        ChunkProviderServer provider = (ChunkProviderServer) this.getWorldObj().getChunkProvider();
        chunk.dirty = true;
        this.claimedChunks[x - this.ownedChunks.getMinX()][z - this.ownedChunks.getMinZ()] = chunk;

        if (putInId2ChunkMap) {
            provider.id2ChunkMap.put(ChunkPos.asLong(x, z), chunk);
        }

        chunk.onLoad();

        PlayerChunkMap map = ((WorldServer) this.getWorldObj()).getPlayerChunkMap();

        PlayerChunkMapEntry entry = new PlayerChunkMapEntry(map, x, z);

        // TODO: This is causing concurrency crashes
        long i = PlayerChunkMap.getIndex(x, z);

        map.entryMap.put(i, entry);
        map.entries.add(entry);

        entry.sentToPlayers = true;
        entry.players = this.watchingPlayers;

        this.claimedChunksEntries[x - this.ownedChunks.getMinX()][z - this.ownedChunks.getMinZ()] = entry;
    }

    // Experimental, could fix issues with random shit generating inside of Ships
    private void replaceOuterChunksWithAir() {
        for (int x = this.ownedChunks.getMinX() - 1; x <= this.ownedChunks.getMaxX() + 1; x++) {
            for (int z = this.ownedChunks.getMinZ() - 1; z <= this.ownedChunks.getMaxZ() + 1; z++) {
                if (x == this.ownedChunks.getMinX() - 1 || x == this.ownedChunks.getMaxX() + 1 || z == this.ownedChunks.getMinZ() - 1
                        || z == this.ownedChunks.getMaxZ() + 1) {
                    // This is satisfied for the chunks surrounding a Ship, do fill it with empty
                    // space
                    Chunk chunk = new Chunk(this.getWorldObj(), x, z);
                    ChunkProviderServer provider = (ChunkProviderServer) this.getWorldObj().getChunkProvider();
                    chunk.dirty = true;
                    provider.id2ChunkMap.put(ChunkPos.asLong(x, z), chunk);
                }
            }
        }
    }

    /**
     * TODO: Add the methods that send the tileEntities in each given chunk
     */
    public void preloadNewPlayers() {
        Set<EntityPlayerMP> newWatchers = this.getPlayersThatJustWatched();
        for (Chunk[] chunkArray : this.claimedChunks) {
            for (Chunk chunk : chunkArray) {
                SPacketChunkData data = new SPacketChunkData(chunk, 65535);
                for (EntityPlayerMP player : newWatchers) {
                    player.connection.sendPacket(data);
                    ((WorldServer) this.getWorldObj()).getEntityTracker().sendLeashedEntitiesInChunk(player, chunk);
                }
            }
        }
    }

    public BlockPos getRegionCenter() {
        return this.ownedChunks.getRegionCenter();
    }

    /**
     * TODO: Make this further get the player to stop all further tracking of those
     * physObject
     *
     * @param untracking EntityPlayer that stopped tracking
     */
    public void onPlayerUntracking(EntityPlayer untracking) {
        this.watchingPlayers.remove(untracking);
        for (int x = this.ownedChunks.getMinX(); x <= this.ownedChunks.getMaxX(); x++) {
            for (int z = this.ownedChunks.getMinZ(); z <= this.ownedChunks.getMaxZ(); z++) {
                SPacketUnloadChunk unloadPacket = new SPacketUnloadChunk(x, z);
                ((EntityPlayerMP) untracking).connection.sendPacket(unloadPacket);
            }
        }
    }

    /**
     * Called when this entity has been unloaded from the world
     */
    public void onThisUnload() {
        if (!this.getWorldObj().isRemote) {
            this.unloadShipChunksFromWorld();
        } else {
            this.shipRenderer.killRenderers();
        }
    }

    public void unloadShipChunksFromWorld() {
        ChunkProviderServer provider = (ChunkProviderServer) this.getWorldObj().getChunkProvider();
        for (int x = this.ownedChunks.getMinX(); x <= this.ownedChunks.getMaxX(); x++) {
            for (int z = this.ownedChunks.getMinZ(); z <= this.ownedChunks.getMaxZ(); z++) {
                provider.queueUnload(this.claimedChunks[x - this.ownedChunks.getMinX()][z - this.ownedChunks.getMinZ()]);

                // Ticket ticket =
                // ValkyrienWarfareMod.physicsManager.getManagerForWorld(this.worldObj).chunkLoadingTicket;
                // So fucking laggy!
                // ForgeChunkManager.unforceChunk(manager.chunkLoadingTicket, new ChunkPos(x,
                // z));
                // MinecraftForge.EVENT_BUS.post(new UnforceChunkEvent(ticket, new ChunkPos(x,
                // z)));
            }
        }
    }

    private Set getPlayersThatJustWatched() {
        HashSet newPlayers = new HashSet();
        for (Object o : ((WorldServer) this.getWorldObj()).getEntityTracker().getTrackingPlayers(this.wrapper)) {
            EntityPlayerMP player = (EntityPlayerMP) o;
            if (!this.watchingPlayers.contains(player)) {
                newPlayers.add(player);
                this.watchingPlayers.add(player);
            }
        }
        return newPlayers;
    }

    public void onTick() {
        if (!this.getWorldObj().isRemote) {
            for (Entity e : this.queuedEntitiesToMount) {
                if (e != null) {
                    e.startRiding(this.wrapper, true);
                }
            }
            this.queuedEntitiesToMount.clear();
        }
        this.gameConsecutiveTicks++;
    }

    public void onPostTick() {
        if (!this.wrapper.isDead && !this.wrapper.world.isRemote) {
            ValkyrienWarfareMod.VW_CHUNK_MANAGER.updateShipPosition(this.wrapper);
            if (!this.claimedChunksInMap) {
                // Old ships not in the map will add themselves in once loaded
                ValkyrienWarfareMod.VW_CHUNK_MANAGER.registerChunksForShip(this.wrapper);
                System.out.println("Old ship detected, adding to the registered Chunks map");
                this.claimedChunksInMap = true;
            }
        }
    }

    /**
     * Updates the position and orientation of the client according to the data sent
     * from the server.
     */
    public void onPostTickClient() {
        ShipTransformationPacketHolder toUse = this.shipTransformationManager.serverBuffer.pollForClientTransform();
        if (toUse != null) {
            toUse.applySmoothLerp(this, .6D);
        }

        this.shipTransformationManager.updatePrevTickTransform();
        this.shipTransformationManager.updateAllTransforms(false, true);
    }

    public void updateChunkCache() {
        AxisAlignedBB cacheBB = this.shipBoundingBox;
        // Check if all those surrounding chunks are loaded
        BlockPos min = new BlockPos(cacheBB.minX, Math.max(cacheBB.minY, 0), cacheBB.minZ);
        BlockPos max = new BlockPos(cacheBB.maxX, Math.min(cacheBB.maxY, 255), cacheBB.maxZ);
        if (!this.getWorldObj().isRemote) {
            ChunkProviderServer serverChunkProvider = (ChunkProviderServer) this.getWorldObj().getChunkProvider();
            int chunkMinX = min.getX() >> 4;
            int chunkMaxX = max.getX() >> 4;
            int chunkMinZ = min.getZ() >> 4;
            int chunkMaxZ = max.getZ() >> 4;
            boolean areSurroundingChunksLoaded = true;
            for (int chunkX = chunkMinX; chunkX <= chunkMaxX; chunkX++) {
                for (int chunkZ = chunkMinZ; chunkZ <= chunkMaxZ; chunkZ++) {
                    boolean isChunkLoaded = serverChunkProvider.chunkExists(chunkX, chunkZ);
                    areSurroundingChunksLoaded &= isChunkLoaded;
                }
            }
            if (areSurroundingChunksLoaded) {
                this.setCachedSurroundingChunks(new ChunkCache(this.getWorldObj(), min, max, 0));
            } else {
                this.resetConsecutiveProperTicks();
            }
        } else {
            this.setCachedSurroundingChunks(new ChunkCache(this.getWorldObj(), min, max, 0));
        }
    }

    public void loadClaimedChunks() {
        ValkyrienWarfareMod.VW_PHYSICS_MANAGER.onShipPreload(this.wrapper);

        this.claimedChunks = new Chunk[(this.ownedChunks.getRadius() * 2) + 1][(this.ownedChunks.getRadius() * 2) + 1];
        this.claimedChunksEntries = new PlayerChunkMapEntry[(this.ownedChunks.getRadius() * 2) + 1][(this.ownedChunks.getRadius() * 2) + 1];
        for (int x = this.ownedChunks.getMinX(); x <= this.ownedChunks.getMaxX(); x++) {
            for (int z = this.ownedChunks.getMinZ(); z <= this.ownedChunks.getMaxZ(); z++) {
                Chunk chunk = this.getWorldObj().getChunkFromChunkCoords(x, z);
                if (chunk == null) {
                    System.out.println("Just a loaded a null chunk");
                    chunk = new Chunk(this.getWorldObj(), x, z);
                }
                // Do this to get it re-integrated into the world
                if (!this.getWorldObj().isRemote) {
                    this.injectChunkIntoWorld(chunk, x, z, false);
                }
                for (Entry<BlockPos, TileEntity> entry : chunk.tileEntities.entrySet()) {
                    this.onSetTileEntity(entry.getKey(), entry.getValue());
                }
                this.claimedChunks[x - this.ownedChunks.getMinX()][z - this.ownedChunks.getMinZ()] = chunk;
            }
        }
        this.setChunkCache(new VWChunkCache(this.getWorldObj(), this.claimedChunks));
        this.setRefrenceBlockPos(this.getRegionCenter());
        this.setShipTransformationManager(new ShipTransformationManager(this));
        if (!this.getWorldObj().isRemote) {
            this.createPhysicsCalculations();
            // The client doesn't need to keep track of this.
            this.detectBlockPositions();
        }

        this.shipTransformationManager.updateAllTransforms(false, false);
    }

    // Generates the blockPos array; must be loaded DIRECTLY after the chunks are
    // setup
    public void detectBlockPositions() {
        // int minChunkX = claimedChunks[0][0].x;
        // int minChunkZ = claimedChunks[0][0].z;
        int chunkX, chunkZ, index, x, y, z;
        Chunk chunk;
        ExtendedBlockStorage storage;
        for (chunkX = this.claimedChunks.length - 1; chunkX > -1; chunkX--) {
            for (chunkZ = this.claimedChunks[0].length - 1; chunkZ > -1; chunkZ--) {
                chunk = this.claimedChunks[chunkX][chunkZ];
                if (chunk != null && this.ownedChunks.isChunkOccupied(chunkX, chunkZ)) {
                    for (index = 0; index < 16; index++) {
                        storage = chunk.getBlockStorageArray()[index];
                        if (storage != null) {
                            for (y = 0; y < 16; y++) {
                                for (x = 0; x < 16; x++) {
                                    for (z = 0; z < 16; z++) {
                                        if (storage.data.storage
                                                .getAt(y << 8 | z << 4 | x) != ValkyrienWarfareMod.airStateIndex) {
                                            BlockPos pos = new BlockPos(chunk.x * 16 + x, index * 16 + y,
                                                    chunk.z * 16 + z);
                                            this.blockPositions.add(pos);
                                            if (BlockForce.basicForces.isBlockProvidingForce(
                                                    this.getWorldObj().getBlockState(pos), pos, this.getWorldObj())) {
                                                this.physicsProcessor.addPotentialActiveForcePos(pos);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean ownsChunk(int chunkX, int chunkZ) {
        return this.ownedChunks.isChunkEnclosedInSet(chunkX, chunkZ);
    }

    public void queueEntityForMounting(Entity toMount) {
        this.queuedEntitiesToMount.add(toMount);
    }

    /**
     * ONLY USE THESE 2 METHODS TO EVER ADD/REMOVE ENTITIES, OTHERWISE YOU'LL RUIN
     * EVERYTHING!
     *
     * @param toFix
     * @param posInLocal
     */
    public void fixEntity(Entity toFix, Vector posInLocal) {
        EntityFixMessage entityFixingMessage = new EntityFixMessage(this.wrapper, toFix, true, posInLocal);
        for (EntityPlayerMP watcher : this.watchingPlayers) {
            ValkyrienWarfareControl.controlNetwork.sendTo(entityFixingMessage, watcher);
        }
        this.entityLocalPositions.put(toFix.getPersistentID().hashCode(), posInLocal);
    }

    /**
     * ONLY USE THESE 2 METHODS TO EVER ADD/REMOVE ENTITIES
     */
    public void unFixEntity(Entity toUnfix) {
        EntityFixMessage entityUnfixingMessage = new EntityFixMessage(this.wrapper, toUnfix, false, null);
        for (EntityPlayerMP watcher : this.watchingPlayers) {
            ValkyrienWarfareControl.controlNetwork.sendTo(entityUnfixingMessage, watcher);
        }
        this.entityLocalPositions.remove(toUnfix.getPersistentID().hashCode());
    }

    public void fixEntityUUID(int uuidHash, Vector localPos) {
        this.entityLocalPositions.put(uuidHash, localPos);
    }

    public void removeEntityUUID(int uuidHash) {
        this.entityLocalPositions.remove(uuidHash);
    }

    public boolean isEntityFixed(Entity toCheck) {
        return this.entityLocalPositions.containsKey(toCheck.getPersistentID().hashCode());
    }

    public Vector getLocalPositionForEntity(Entity getPositionFor) {
        int uuidHash = getPositionFor.getPersistentID().hashCode();
        return this.entityLocalPositions.get(uuidHash);
    }

    public void writeToNBTTag(NBTTagCompound compound) {
        this.ownedChunks.writeToNBT(compound);
        NBTUtils.writeVectorToNBT("c", this.centerCoord, compound);
        NBTUtils.writeShipTransformToNBT("currentTickTransform",
                this.shipTransformationManager.getCurrentTickTransform(), compound);
        compound.setBoolean("doPhysics", this.isPhysicsEnabled/* isPhysicsEnabled() */);
        for (int row = 0; row < this.ownedChunks.getChunkOccupiedInLocal().length; row++) {
            boolean[] curArray = this.ownedChunks.getChunkOccupiedInLocal()[row];
            for (int column = 0; column < curArray.length; column++) {
                compound.setBoolean("CC:" + row + ':' + column, curArray[column]);
            }
        }
        NBTUtils.writeEntityPositionMapToNBT("entityPosHashMap", this.entityLocalPositions, compound);
        this.physicsProcessor.writeToNBTTag(compound);

        compound.setBoolean("claimedChunksInMap", this.claimedChunksInMap);
        compound.setBoolean("isNameCustom", this.isNameCustom);
        compound.setString("shipType", this.shipType.name());
        // Write and read AABB from NBT to speed things up.
        NBTUtils.writeAABBToNBT("collision_aabb", this.shipBoundingBox, compound);

        if (this.owner != null) {
            compound.setString("ownerName", this.owner.getName());
            compound.setUniqueId("ownerUuid", this.owner.getId());
        }
    }

    public void readFromNBTTag(NBTTagCompound compound) {
        this.setOwnedChunks(new VWChunkClaim(compound));
        this.setCenterCoord(NBTUtils.readVectorFromNBT("c", compound));
        ShipTransform savedTransform = NBTUtils.readShipTransformFromNBT("currentTickTransform", compound);
        if (savedTransform != null) {
            Vector centerOfMassInGlobal = new Vector(this.centerCoord);
            savedTransform.transform(centerOfMassInGlobal, TransformType.SUBSPACE_TO_GLOBAL);

            this.wrapper.posX = centerOfMassInGlobal.X;
            this.wrapper.posY = centerOfMassInGlobal.Y;
            this.wrapper.posZ = centerOfMassInGlobal.Z;

            Quaternion rotationQuaternion = savedTransform.createRotationQuaternion(TransformType.SUBSPACE_TO_GLOBAL);
            double[] angles = rotationQuaternion.toRadians();
            this.wrapper.setPhysicsEntityRotation(Math.toDegrees(angles[0]), Math.toDegrees(angles[1]), Math.toDegrees(angles[2]));
        } else {
            // Old code here for compatibility reasons. Should be removed by MC 1.13
            this.wrapper.setPhysicsEntityRotation(compound.getDouble("pitch"), compound.getDouble("yaw"), compound.getDouble("roll"));
        }

        for (int row = 0; row < this.ownedChunks.getChunkOccupiedInLocal().length; row++) {
            boolean[] curArray = this.ownedChunks.getChunkOccupiedInLocal()[row];
            for (int column = 0; column < curArray.length; column++) {
                curArray[column] = compound.getBoolean("CC:" + row + ':' + column);
            }
        }

        String shipTypeName = compound.getString("shipType");
        if (!shipTypeName.isEmpty()) {
            this.shipType = ShipType.valueOf(ShipType.class, shipTypeName);
        } else {
            // Assume its an older Ship, and that its fully unlocked
            this.shipType = ShipType.Full_Unlocked;
        }

        this.loadClaimedChunks();
        this.entityLocalPositions = NBTUtils.readEntityPositionMap("entityPosHashMap", compound);
        this.physicsProcessor.readFromNBTTag(compound);

        this.claimedChunksInMap = compound.getBoolean("claimedChunksInMap");
        this.isNameCustom = compound.getBoolean("isNameCustom");
        this.wrapper.dataManager.set(PhysicsWrapperEntity.IS_NAME_CUSTOM, this.isNameCustom);

        this.setShipBoundingBox(NBTUtils.readAABBFromNBT("collision_aabb", compound));

        this.isPhysicsEnabled = compound.getBoolean("doPhysics");

        if (compound.hasKey("ownerName")) {
            UUID uuid = compound.getUniqueId("ownerUuid");
            String name = compound.getString("ownerName");
            this.owner = new GameProfile(uuid, name);
        } else {
            this.owner = null;
        }
    }

    public void readSpawnData(ByteBuf additionalData) {
        PacketBuffer modifiedBuffer = new PacketBuffer(additionalData);

        this.setOwnedChunks(new VWChunkClaim(modifiedBuffer.readInt(), modifiedBuffer.readInt(), modifiedBuffer.readInt()));

        double posX = modifiedBuffer.readDouble();
        double posY = modifiedBuffer.readDouble();
        double posZ = modifiedBuffer.readDouble();
        double pitch = modifiedBuffer.readDouble();
        double yaw = modifiedBuffer.readDouble();
        double roll = modifiedBuffer.readDouble();

        this.wrapper.setPhysicsEntityPositionAndRotation(posX, posY, posZ, pitch, yaw, roll);
        this.wrapper.physicsUpdateLastTickPositions();

        this.setCenterCoord(new Vector(modifiedBuffer));
        for (boolean[] array : this.ownedChunks.getChunkOccupiedInLocal()) {
            for (int i = 0; i < array.length; i++) {
                array[i] = modifiedBuffer.readBoolean();
            }
        }
        this.loadClaimedChunks();
        this.shipRenderer.updateOffsetPos(this.refrenceBlockPos);

        this.shipTransformationManager.serverBuffer.pushMessage(new PhysWrapperPositionMessage(this));

        try {
            NBTTagCompound entityFixedPositionNBT = modifiedBuffer.readCompoundTag();
            this.entityLocalPositions = NBTUtils.readEntityPositionMap("entityFixedPosMap", entityFixedPositionNBT);
        } catch (IOException e) {
            System.err.println("Couldn't load the entityFixedPosNBT; this is really bad.");
            e.printStackTrace();
        }

        this.isNameCustom = modifiedBuffer.readBoolean();
        this.shipType = modifiedBuffer.readEnumValue(ShipType.class);
    }

    public void writeSpawnData(ByteBuf buffer) {
        PacketBuffer modifiedBuffer = new PacketBuffer(buffer);

        modifiedBuffer.writeInt(this.ownedChunks.getCenterX());
        modifiedBuffer.writeInt(this.ownedChunks.getCenterZ());
        modifiedBuffer.writeInt(this.ownedChunks.getRadius());

        modifiedBuffer.writeDouble(this.wrapper.posX);
        modifiedBuffer.writeDouble(this.wrapper.posY);
        modifiedBuffer.writeDouble(this.wrapper.posZ);

        modifiedBuffer.writeDouble(this.wrapper.getPitch());
        modifiedBuffer.writeDouble(this.wrapper.getYaw());
        modifiedBuffer.writeDouble(this.wrapper.getRoll());

        this.centerCoord.writeToByteBuf(modifiedBuffer);
        for (boolean[] array : this.ownedChunks.getChunkOccupiedInLocal()) {
            for (boolean b : array) {
                modifiedBuffer.writeBoolean(b);
            }
        }

        NBTTagCompound entityFixedPositionNBT = new NBTTagCompound();
        NBTUtils.writeEntityPositionMapToNBT("entityFixedPosMap", this.entityLocalPositions, entityFixedPositionNBT);
        modifiedBuffer.writeCompoundTag(entityFixedPositionNBT);

        modifiedBuffer.writeBoolean(this.isNameCustom);
        modifiedBuffer.writeEnumValue(this.shipType);
    }

    /**
     * @return The World this PhysicsObject exists in.
     */
    public World getWorldObj() {
        return this.wrapper.getEntityWorld();
    }

    /**
     * @return The Entity that wraps around this PhysicsObject.
     */
    public PhysicsWrapperEntity getWrapperEntity() {
        return this.wrapper;
    }

    public boolean areShipChunksFullyLoaded() {
        return this.chunkCache != null;
    }

    /**
     * @return true if physics are enabled
     */
    // TODO: This still breaks when the server is lagging, because it will skip
    // ticks and therefore the counter will go higher than it really should be.
    public boolean isPhysicsEnabled() {
        return this.isPhysicsEnabled && this.gameConsecutiveTicks >= MIN_TICKS_EXISTED_BEFORE_PHYSICS && this.physicsConsecutiveTicks >= MIN_TICKS_EXISTED_BEFORE_PHYSICS * 5;
    }

    /**
     * @param physicsEnabled If true enables physics processing for this physics object, if
     *                       false disables physics processing.
     */
    public void setPhysicsEnabled(boolean physicsEnabled) {
        this.isPhysicsEnabled = physicsEnabled;
    }

    /**
     * Sets the consecutive tick counter to 0.
     */
    public void resetConsecutiveProperTicks() {
        this.gameConsecutiveTicks = 0;
        this.physicsConsecutiveTicks = 0;
    }

    public void advanceConsecutivePhysicsTicksCounter() {
        this.physicsConsecutiveTicks++;
    }

    /**
     * @return true if this PhysicsObject needs to update the collision cache immediately.
     */
    public boolean needsImmediateCollisionCacheUpdate() {
        return this.gameConsecutiveTicks == MIN_TICKS_EXISTED_BEFORE_PHYSICS;
    }

    /**
     * @return the isNameCustom
     */
    public boolean isNameCustom() {
        return this.isNameCustom;
    }

    /**
     * @param isNameCustom the isNameCustom to set
     */
    public void setNameCustom(boolean isNameCustom) {
        this.isNameCustom = isNameCustom;
    }

    @Override
    public ISubspace getSubspace() {
        return this.shipSubspace;
    }

    // ===== Keep track of all Node Processors in a concurrent Set =====
    public void onSetTileEntity(BlockPos pos, TileEntity tileentity) {
        if (tileentity instanceof INodeController) {
            this.physicsControllers.add((INodeController) tileentity);
        }
        // System.out.println(physicsControllers.size());
    }

    public void onRemoveTileEntity(BlockPos pos) {
        this.physicsControllers.removeIf(next -> next.getNodePos().equals(pos));
        // System.out.println(physicsControllers.size());
    }

    // Do not allow anything external to modify the physics controllers Set.
    public Set<INodeController> getPhysicsControllersInShip() {
        return this.physicsControllersImmutable;
    }

}
