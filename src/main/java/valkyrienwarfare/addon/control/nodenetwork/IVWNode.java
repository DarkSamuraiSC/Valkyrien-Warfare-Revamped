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

import gigaherz.graph.api.GraphObject;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import valkyrienwarfare.physics.management.PhysicsObject;

import java.util.List;
import java.util.Set;

/**
 * The nodes that form the graphs of control elements.
 *
 * @author thebest108
 */
public interface IVWNode extends GraphObject {

    String NBT_DATA_KEY = "VWNode_Tile_Data";

    /**
     * This does not return the full graph of connected nodes, just the ones that
     * are directly connected to this node.
     *
     * @return
     */
    Iterable<IVWNode> getDirectlyConnectedNodes();

    void makeConnection(IVWNode other);

    void breakConnection(IVWNode other);

    /**
     * Mark this IVWNode as safe to use.
     */
    void validate();

    /**
     * Mark this IVWNode as unsafe to use.
     */
    void invalidate();

    /**
     * Returns true if the node is safe, false if it isn't.
     *
     * @return
     */
    boolean isValid();

    /**
     * Returns null if this node doesn't have a blockpos.
     *
     * @return
     */
    BlockPos getNodePos();

    World getNodeWorld();

    Set<BlockPos> getLinkedNodesPos();

    void writeToNBT(NBTTagCompound compound);

    void readFromNBT(NBTTagCompound compound);

    default void breakAllConnections() {
        for (IVWNode node : getDirectlyConnectedNodes()) {
            breakConnection(node);
        }
    }

    default boolean canLinkToOtherNode(IVWNode other) {
        return getLinkedNodesPos().size() < getMaximumConnections() && other.getLinkedNodesPos().size() < other.getMaximumConnections();
    }

    void sendNodeUpdates();

    /**
     * Can only be called while this node is invalid. Otherwise an
     * IllegalStateException is thrown.
     *
     * @param offset
     */
    void shiftConnections(BlockPos offset);

    /**
     * Should only be called when after shiftConnections()
     *
     * @param parent
     */
    void setParentPhysicsObject(PhysicsObject parent);

    PhysicsObject getPhysicsObject();

    List<GraphObject> getNeighbours();

    TileEntity getParentTile();

    int getMaximumConnections();

    /**
     * @param other
     * @return True if the nodes are linked.
     */
    default boolean isLinkedToNode(IVWNode other) {
        return getLinkedNodesPos().contains(other.getNodePos());
    }
}
