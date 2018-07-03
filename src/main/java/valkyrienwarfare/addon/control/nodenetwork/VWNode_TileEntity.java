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

package valkyrienwarfare.addon.control.nodenetwork;

import gigaherz.graph.api.Graph;
import gigaherz.graph.api.GraphObject;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.server.management.PlayerList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import valkyrienwarfare.physics.management.PhysicsObject;

import javax.annotation.Nullable;
import java.util.*;

public class VWNode_TileEntity implements IVWNode {

    private final TileEntity parentTile;
    // No duplicate connections, use Set<Node> to guarantee this
    private final Set<BlockPos> linkedNodesPos;
    // A wrapper unmodifiable Set that allows external classes to see an immutable
    // version of linkedNodesPos.
    private final Set<BlockPos> unmodifiableLinkedNodesPos;
    private final int maximumConnections;
    private boolean isValid;
    private PhysicsObject parentPhysicsObject;
    private Graph nodeGraph;

    public VWNode_TileEntity(TileEntity parent, int maximumConnections) {
        this.parentTile = parent;
        this.linkedNodesPos = new HashSet<>();
        this.unmodifiableLinkedNodesPos = Collections.unmodifiableSet(this.linkedNodesPos);
        this.isValid = false;
        this.parentPhysicsObject = null;
        this.nodeGraph = null;
        this.maximumConnections = maximumConnections;
    }

    @Nullable
    @Deprecated
    public static IVWNode getVWNode_TileEntity(World world, BlockPos pos) {
        if (world == null || pos == null) {
            throw new IllegalArgumentException("Null arguments");
        }
        boolean isChunkLoaded = world.isBlockLoaded(pos);
        if (!isChunkLoaded) {
            return null;
            // throw new IllegalStateException("VWNode_TileEntity wasn't loaded in the
            // world!");
        }
        TileEntity entity = world.getTileEntity(pos);
        if (entity == null) {
            return null;
            // throw new IllegalStateException("VWNode_TileEntity was null");
        }
        if (entity instanceof IVWNodeProvider) {
            IVWNode vwNode = ((IVWNodeProvider) entity).getNode();
            if (!vwNode.isValid()) {
                return null;
                // throw new IllegalStateException("IVWNode was not valid!");
            } else {
                return vwNode;
            }
        } else {
            return null;
            // throw new IllegalStateException("VWNode_TileEntity of different class");
        }
    }

    @Override
    public Iterable<IVWNode> getDirectlyConnectedNodes() {
        // assertValidity();
        List<IVWNode> nodesList = new ArrayList<>();
        for (BlockPos pos : this.linkedNodesPos) {
            IVWNode node = getVWNode_TileEntity(this.getNodeWorld(), pos);
            if (node != null) {
                nodesList.add(node);
            }
        }
        return nodesList;
    }

    @Override
    public void makeConnection(IVWNode other) {
        this.assertValidity();
        boolean contains = this.linkedNodesPos.contains(other.getNodePos());
        if (!contains) {
            this.linkedNodesPos.add(other.getNodePos());
            this.parentTile.markDirty();
            other.makeConnection(this);
            this.sendNodeUpdates();
            List stupid = Collections.singletonList(other);
            this.getGraph().addNeighours(this, stupid);
            // System.out.println("Connections: " + getGraph().getObjects().size());
            // getNodeGraph().addNode(other);
        }
    }

    @Override
    public void breakConnection(IVWNode other) {
        this.assertValidity();
        boolean contains = this.linkedNodesPos.contains(other.getNodePos());
        if (contains) {
            this.linkedNodesPos.remove(other.getNodePos());
            this.parentTile.markDirty();
            other.breakConnection(this);
            this.sendNodeUpdates();
            this.getGraph().removeNeighbour(this, other);
            // System.out.println(getGraph().getObjects().size());
            // getNodeGraph().removeNode(other);
        }
    }

    @Override
    public BlockPos getNodePos() {
        this.assertValidity();
        return this.parentTile.getPos();
    }

    @Override
    public void validate() {
        this.isValid = true;
    }

    @Override
    public void invalidate() {
        this.isValid = false;
    }

    @Override
    public boolean isValid() {
        return this.isValid;
    }

    @Override
    public World getNodeWorld() {
        return this.parentTile.getWorld();
    }

    @Override
    public Set<BlockPos> getLinkedNodesPos() {
        return this.unmodifiableLinkedNodesPos;
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        int[] positions = new int[this.getLinkedNodesPos().size() * 3];
        int cont = 0;
        for (BlockPos pos : this.getLinkedNodesPos()) {
            positions[cont] = pos.getX();
            positions[cont + 1] = pos.getY();
            positions[cont + 2] = pos.getZ();
            cont += 3;
        }
        compound.setIntArray(NBT_DATA_KEY, positions);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        int[] positions = compound.getIntArray(NBT_DATA_KEY);
        for (int i = 0; i < positions.length; i += 3) {
            this.linkedNodesPos.add(new BlockPos(positions[i], positions[i + 1], positions[i + 2]));
        }
    }

    @Override
    public PhysicsObject getPhysicsObject() {
        return this.parentPhysicsObject;
    }

    @Override
    public void sendNodeUpdates() {
        if (!this.getNodeWorld().isRemote) {
            Packet toSend = this.parentTile.getUpdatePacket();

            double xPos = this.parentTile.getPos().getX();
            double yPos = this.parentTile.getPos().getY();
            double zPos = this.parentTile.getPos().getZ();

            WorldServer serverWorld = (WorldServer) this.getNodeWorld();
            PlayerList list = serverWorld.mcServer.getPlayerList();
            // System.out.println("help");
            if (!this.parentTile.isInvalid()) {
                list.sendToAllNearExcept(null, xPos, yPos, zPos, 128D, serverWorld.provider.getDimension(), toSend);
            }
        }
    }

    private void assertValidity() {
        if (!this.isValid()) {
            throw new IllegalStateException("This node is not valid / initialized!");
        }
    }

    @Override
    public void shiftConnections(BlockPos offset) {
        if (this.isValid()) {
            throw new IllegalStateException("Cannot shift the connections of a Node while it is valid and in use!");
        }
        List<BlockPos> shiftedNodesPos = new ArrayList<>(this.linkedNodesPos.size());
        for (BlockPos originalPos : this.linkedNodesPos) {
            shiftedNodesPos.add(originalPos.add(offset));
        }
        this.linkedNodesPos.clear();
        this.linkedNodesPos.addAll(shiftedNodesPos);
    }

    @Override
    public void setParentPhysicsObject(PhysicsObject parent) {
        if (this.isValid()) {
            throw new IllegalStateException(
                    "Cannot change the parent physics object of a Node while it is valid and in use!");
        }
        this.parentPhysicsObject = parent;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other instanceof VWNode_TileEntity) {
            VWNode_TileEntity otherNode = (VWNode_TileEntity) other;
            return otherNode.getNodePos().equals(this.getNodePos());
        } else {
            return false;
        }
    }

    @Override
    public Graph getGraph() {
        return this.nodeGraph;
    }

    @Override
    public void setGraph(Graph graph) {
        this.nodeGraph = graph;
    }

    private List<GraphObject> getNeighbors() {
        List<GraphObject> neighbors = new ArrayList<>();
        for (BlockPos pos : this.getLinkedNodesPos()) {
            IVWNode node = getVWNode_TileEntity(this.getNodeWorld(), pos);
            if (node == null) {
                throw new IllegalStateException();
            }
            neighbors.add(node);
        }
        return neighbors;
    }

    @Override
    public List<GraphObject> getNeighbours() {
        List<GraphObject> nodesList = new ArrayList<>();
        for (BlockPos pos : this.linkedNodesPos) {
            IVWNode node = getVWNode_TileEntity(this.getNodeWorld(), pos);
            if (node != null) {
                nodesList.add(node);
            }
        }
        return nodesList;
    }

    @Override
    public TileEntity getParentTile() {
        return this.parentTile;
    }

    @Override
    public int getMaximumConnections() {
        return this.maximumConnections;
    }
}
