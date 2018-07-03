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

package valkyrienwarfare.addon.control;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import valkyrienwarfare.ValkyrienWarfareMod;
import valkyrienwarfare.addon.control.block.*;
import valkyrienwarfare.addon.control.block.engine.BlockNormalEngine;
import valkyrienwarfare.addon.control.block.engine.BlockRedstoneEngine;
import valkyrienwarfare.addon.control.block.ethercompressor.BlockCreativeEtherCompressor;
import valkyrienwarfare.addon.control.block.ethercompressor.BlockNormalEtherCompressor;
import valkyrienwarfare.api.addons.Module;

public class BlocksValkyrienWarfareControl {

    private final ValkyrienWarfareControl mod_vwcontrol;

    public final BlockNormalEngine basicEngine;
    public final BlockNormalEngine advancedEngine;
    public final BlockNormalEngine eliteEngine;
    public final BlockNormalEngine ultimateEngine;
    public final BlockRedstoneEngine redstoneEngine;

    public final BlockNormalEtherCompressor antigravityEngine; // leaving it with the old name to prevent blocks disappearing
    public final BlockNormalEtherCompressor advancedEtherCompressor;
    public final BlockNormalEtherCompressor eliteEtherCompressor;
    public final BlockNormalEtherCompressor ultimateEtherCompressor;
    public final BlockCreativeEtherCompressor creativeEtherCompressor;

    public final Block dopedEtherium;
    public final Block pilotsChair;
    public final Block passengerChair;
    public final Block shipHelm;
    public final Block shipWheel;
    public final Block shipTelegraph;
    public final Block thrustRelay;
    public final Block thrustModulator;
    public final Block gyroscope;
    public final Block liftValve;
    public final Block networkDisplay;
    public final Block liftControl;
    public final Block etherGasCompressor;

    public BlocksValkyrienWarfareControl(ValkyrienWarfareControl mod_vwcontrol) {
        this.mod_vwcontrol = mod_vwcontrol;

        this.basicEngine = (BlockNormalEngine) new BlockNormalEngine(Material.WOOD, 4000.0d).setHardness(5f).setUnlocalizedName("basicengine").setRegistryName(this.getModID(), "basicengine").setCreativeTab(ValkyrienWarfareMod.vwTab);
        this.advancedEngine = (BlockNormalEngine) new BlockNormalEngine(Material.ROCK, 6000.0d).setHardness(6f).setUnlocalizedName("advancedengine").setRegistryName(this.getModID(), "advancedengine").setCreativeTab(ValkyrienWarfareMod.vwTab);
        this.eliteEngine = (BlockNormalEngine) new BlockNormalEngine(Material.IRON, 8000.0d).setHardness(8f).setUnlocalizedName("eliteengine").setRegistryName(this.getModID(), "eliteengine").setCreativeTab(ValkyrienWarfareMod.vwTab);
        this.ultimateEngine = (BlockNormalEngine) new BlockNormalEngine(Material.GROUND, 16000.0d).setHardness(10f).setUnlocalizedName("ultimateengine").setRegistryName(this.getModID(), "ultimateengine").setCreativeTab(ValkyrienWarfareMod.vwTab);
        this.redstoneEngine = (BlockRedstoneEngine) new BlockRedstoneEngine(Material.REDSTONE_LIGHT, 500.0d).setHardness(7.0f).setUnlocalizedName("redstoneengine").setRegistryName(this.getModID(), "redstoneengine").setCreativeTab(ValkyrienWarfareMod.vwTab);

        this.antigravityEngine = (BlockNormalEtherCompressor) new BlockNormalEtherCompressor(Material.WOOD, 25000.0d).setHardness(8f).setUnlocalizedName("antigravengine").setRegistryName(this.getModID(), "antigravengine").setCreativeTab(ValkyrienWarfareMod.vwTab);
        this.advancedEtherCompressor = (BlockNormalEtherCompressor) new BlockNormalEtherCompressor(Material.ROCK, 45000.0d).setHardness(8f).setUnlocalizedName("advancedethercompressor").setRegistryName(this.getModID(), "advancedethercompressor").setCreativeTab(ValkyrienWarfareMod.vwTab);
        this.eliteEtherCompressor = (BlockNormalEtherCompressor) new BlockNormalEtherCompressor(Material.IRON, 80000.0d).setHardness(8f).setUnlocalizedName("eliteethercompressor").setRegistryName(this.getModID(), "eliteethercompressor").setCreativeTab(ValkyrienWarfareMod.vwTab);
        this.ultimateEtherCompressor = (BlockNormalEtherCompressor) new BlockNormalEtherCompressor(Material.GROUND, 100000.0d).setHardness(8f).setUnlocalizedName("ultimateethercompressor").setRegistryName(this.getModID(), "ultimateethercompressor").setCreativeTab(ValkyrienWarfareMod.vwTab);
        this.creativeEtherCompressor = (BlockCreativeEtherCompressor) new BlockCreativeEtherCompressor(Material.BARRIER, Double.MAX_VALUE / 4).setHardness(0.0f).setUnlocalizedName("creativeethercompressor").setRegistryName(this.getModID(), "creativeethercompressor").setCreativeTab(ValkyrienWarfareMod.vwTab);

        this.dopedEtherium = new BlockDopedEtherium(Material.GLASS).setHardness(4f).setUnlocalizedName("dopedetherium").setRegistryName(this.getModID(), "dopedetherium").setCreativeTab(ValkyrienWarfareMod.vwTab);
        this.pilotsChair = new BlockShipPilotsChair(Material.IRON).setHardness(4f).setUnlocalizedName("shippilotschair").setRegistryName(this.getModID(), "shippilotschair").setCreativeTab(ValkyrienWarfareMod.vwTab);

        this.passengerChair = new BlockShipPassengerChair(Material.IRON).setHardness(4f).setUnlocalizedName("shippassengerchair").setRegistryName(this.getModID(), "shippassengerchair").setCreativeTab(ValkyrienWarfareMod.vwTab);
        this.shipHelm = new BlockShipHelm(Material.WOOD).setHardness(4f).setUnlocalizedName("shiphelm").setRegistryName(this.getModID(), "shiphelm").setCreativeTab(ValkyrienWarfareMod.vwTab);
        this.shipWheel = new BlockShipWheel(Material.WOOD).setHardness(5f).setUnlocalizedName("shiphelmwheel").setRegistryName(this.getModID(), "shiphelmwheel");
        this.shipTelegraph = new BlockShipTelegraph(Material.WOOD).setHardness(5f).setUnlocalizedName("shiptelegraph").setRegistryName(this.getModID(), "shiptelegraph").setCreativeTab(ValkyrienWarfareMod.vwTab);

        this.thrustRelay = new BlockThrustRelay(Material.IRON).setHardness(5f).setUnlocalizedName("thrustrelay").setRegistryName(this.getModID(), "thrustrelay").setCreativeTab(ValkyrienWarfareMod.vwTab);
        this.thrustModulator = new BlockThrustModulator(Material.IRON).setHardness(8f).setUnlocalizedName("thrustmodulator").setRegistryName(this.getModID(), "thrustmodulator").setCreativeTab(ValkyrienWarfareMod.vwTab);

        this.gyroscope = new BlockGyroscope(Material.IRON).setHardness(5f).setUnlocalizedName("vw_gyroscope").setRegistryName(this.getModID(), "vw_gyroscope").setCreativeTab(ValkyrienWarfareMod.vwTab);

        this.liftValve = new BlockLiftValve(Material.IRON).setHardness(7f).setUnlocalizedName("vw_liftvalve").setRegistryName(this.getModID(), "vw_liftvalve").setCreativeTab(ValkyrienWarfareMod.vwTab);
        this.networkDisplay = new BlockNetworkDisplay(Material.IRON).setHardness(5f).setUnlocalizedName("vw_networkdisplay").setRegistryName(this.getModID(), "vw_networkdisplay").setCreativeTab(ValkyrienWarfareMod.vwTab);
        this.liftControl = new BlockLiftControl(Material.IRON).setHardness(5f).setUnlocalizedName("vw_liftcontrol").setRegistryName(this.getModID(), "vw_liftcontrol").setCreativeTab(ValkyrienWarfareMod.vwTab);
        this.etherGasCompressor = new BlockEtherGasCompressor(Material.IRON).setHardness(5f).setUnlocalizedName("vw_ethergascompressor").setRegistryName(this.getModID(), "vw_ethergascompressor").setCreativeTab(ValkyrienWarfareMod.vwTab);
    }

    protected void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(this.basicEngine);
        event.getRegistry().register(this.advancedEngine);
        event.getRegistry().register(this.eliteEngine);
        event.getRegistry().register(this.ultimateEngine);
        event.getRegistry().register(this.redstoneEngine);

        event.getRegistry().register(this.antigravityEngine);
        event.getRegistry().register(this.advancedEtherCompressor);
        event.getRegistry().register(this.eliteEtherCompressor);
        event.getRegistry().register(this.ultimateEtherCompressor);
        event.getRegistry().register(this.creativeEtherCompressor);

        event.getRegistry().register(this.dopedEtherium);
        event.getRegistry().register(this.pilotsChair);
        event.getRegistry().register(this.passengerChair);

        event.getRegistry().register(this.shipHelm);
        event.getRegistry().register(this.shipWheel);
        event.getRegistry().register(this.shipTelegraph);
        event.getRegistry().register(this.thrustRelay);
        event.getRegistry().register(this.thrustModulator);

        event.getRegistry().register(this.gyroscope);
        event.getRegistry().register(this.liftValve);
        event.getRegistry().register(this.networkDisplay);
        event.getRegistry().register(this.liftControl);
        event.getRegistry().register(this.etherGasCompressor);
    }

    protected void registerBlockItems(RegistryEvent.Register<Item> event) {
        this.registerItemBlock(event, this.basicEngine);
        this.registerItemBlock(event, this.advancedEngine);
        this.registerItemBlock(event, this.eliteEngine);
        this.registerItemBlock(event, this.ultimateEngine);
        this.registerItemBlock(event, this.redstoneEngine);

        this.registerItemBlock(event, this.antigravityEngine);
        this.registerItemBlock(event, this.advancedEtherCompressor);
        this.registerItemBlock(event, this.eliteEtherCompressor);
        this.registerItemBlock(event, this.ultimateEtherCompressor);
        this.registerItemBlock(event, this.creativeEtherCompressor);

        this.registerItemBlock(event, this.dopedEtherium);
        this.registerItemBlock(event, this.pilotsChair);
        this.registerItemBlock(event, this.passengerChair);

        this.registerItemBlock(event, this.shipHelm);
        this.registerItemBlock(event, this.shipWheel);
        this.registerItemBlock(event, this.shipTelegraph);
        this.registerItemBlock(event, this.thrustRelay);
        this.registerItemBlock(event, this.thrustModulator);

        this.registerItemBlock(event, this.gyroscope);
        this.registerItemBlock(event, this.liftValve);
        this.registerItemBlock(event, this.networkDisplay);
        this.registerItemBlock(event, this.liftControl);
        this.registerItemBlock(event, this.etherGasCompressor);
    }

    private void registerItemBlock(RegistryEvent.Register<Item> event, Block block) {
        Module.registerItemBlock(event, block);
    }

    private String getModID() {
        return this.mod_vwcontrol.getModID();
    }
}
