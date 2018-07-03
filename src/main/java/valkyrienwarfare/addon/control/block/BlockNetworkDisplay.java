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

package valkyrienwarfare.addon.control.block;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import valkyrienwarfare.addon.control.nodenetwork.IVWNode;
import valkyrienwarfare.addon.control.tileentity.TileEntityNetworkDisplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockNetworkDisplay extends Block implements ITileEntityProvider {

    public BlockNetworkDisplay(Material materialIn) {
        super(materialIn);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
                                    EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity tile = worldIn.getTileEntity(pos);
            if (tile instanceof TileEntityNetworkDisplay) {
                TileEntityNetworkDisplay displayTile = (TileEntityNetworkDisplay) tile;
                Iterable<IVWNode> networkedObjects = displayTile.getNetworkedConnections();
                List<IVWNode> connectedNodes = new ArrayList<IVWNode>();
                Map<String, Integer> networkedClassTypeCounts = new HashMap<String, Integer>();
                for (IVWNode node : networkedObjects) {
                    connectedNodes.add(node);
                    Class nodeClass = node.getParentTile().getClass();
                    String tileClassName = nodeClass.getSimpleName();
                    if (!networkedClassTypeCounts.containsKey(tileClassName)) {
                        networkedClassTypeCounts.put(tileClassName, 0);
                    }
                    networkedClassTypeCounts.put(tileClassName, networkedClassTypeCounts.get(tileClassName) + 1);
                }
                playerIn.sendMessage(new TextComponentString("Networked objects connected: " + connectedNodes.size()));
                playerIn.sendMessage(new TextComponentString("Types of objects connected: " + networkedClassTypeCounts.toString()));
            }
        }
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityNetworkDisplay();
    }

}
