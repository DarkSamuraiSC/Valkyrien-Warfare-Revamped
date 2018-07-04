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
import gigaherz.graph.api.Mergeable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;

import java.util.Collections;
import java.util.Iterator;

public abstract class BasicNodeTileEntity extends TileEntity implements IVWNodeProvider, ITickable {

    private final VWNode_TileEntity tileNode;
    private boolean firstUpdate;

    public BasicNodeTileEntity() {
        this.tileNode = new VWNode_TileEntity(this, this.getMaximumConnections());
        this.firstUpdate = true;
        Graph.integrate(this.tileNode, Collections.EMPTY_LIST, (graph) -> new GraphData());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, this.writeToNBT(new NBTTagCompound()));
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.tileNode.readFromNBT(compound);
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        this.readFromNBT(tag);
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound toReturn = super.getUpdateTag();
        return this.writeToNBT(toReturn);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        this.tileNode.writeToNBT(compound);
        return super.writeToNBT(compound);
    }

    @Override
    public VWNode_TileEntity getNode() {
        return this.tileNode;
    }

    @Override
    public void invalidate() {
        // The Node just got destroyed
        this.tileEntityInvalid = true;
        VWNode_TileEntity toInvalidate = this.tileNode;
        toInvalidate.breakAllConnections();
        toInvalidate.invalidate();
        Graph graph = toInvalidate.getGraph();
        if (graph != null) {
            graph.remove(toInvalidate);
        }
    }

    /**
     * validates a tile entity
     */
    @Override
    public void validate() {
        this.tileEntityInvalid = false;
        this.tileNode.validate();
    }

    @Override
    public void update() {
        if (this.firstUpdate) {
            this.firstUpdate = false;
            this.init();
        }
    }

    /**
     * @return The maximum number of nodes this tile entity can be connected to.
     */
    protected int getMaximumConnections() {
        return 1;
    }

    private void init() {
        this.tileNode.getGraph().addNeighours(this.tileNode, this.tileNode.getNeighbours());
    }

    @Override
    public Iterable<IVWNode> getNetworkedConnections() {
        Iterator<GraphObject> objects = this.tileNode.getGraph().getObjects().iterator();
        Iterator<IVWNode> nodes = new IteratorCaster(objects);
        return () -> nodes;
    }

    public static class GraphData implements Mergeable<GraphData> {
        private static int sUid;

        private final int uid;

        public GraphData() {
            this.uid = ++sUid;
        }

        public GraphData(int uid) {
            this.uid = uid;
        }

        @Override
        public GraphData mergeWith(GraphData other) {
            return new GraphData(this.uid + other.uid);
        }

        @Override
        public GraphData copy() {
            return new GraphData();
        }

        public int getUid() {
            return this.uid;
        }
    }

    private class IteratorCaster implements Iterator<IVWNode> {
        private final Iterator toCast;

        private IteratorCaster(Iterator toCast) {
            this.toCast = toCast;
        }

        @Override
        public boolean hasNext() {
            return this.toCast.hasNext();
        }

        @Override
        public IVWNode next() {
            return (IVWNode) this.toCast.next();
        }
    }

}
