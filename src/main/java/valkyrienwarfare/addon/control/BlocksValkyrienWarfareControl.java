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

        basicEngine = (BlockNormalEngine) new BlockNormalEngine(Material.WOOD, 4000.0d).setHardness(5f).setUnlocalizedName("basicengine").setRegistryName(getModID(), "basicengine").setCreativeTab(ValkyrienWarfareMod.vwTab);
        advancedEngine = (BlockNormalEngine) new BlockNormalEngine(Material.ROCK, 6000.0d).setHardness(6f).setUnlocalizedName("advancedengine").setRegistryName(getModID(), "advancedengine").setCreativeTab(ValkyrienWarfareMod.vwTab);
        eliteEngine = (BlockNormalEngine) new BlockNormalEngine(Material.IRON, 8000.0d).setHardness(8f).setUnlocalizedName("eliteengine").setRegistryName(getModID(), "eliteengine").setCreativeTab(ValkyrienWarfareMod.vwTab);
        ultimateEngine = (BlockNormalEngine) new BlockNormalEngine(Material.GROUND, 16000.0d).setHardness(10f).setUnlocalizedName("ultimateengine").setRegistryName(getModID(), "ultimateengine").setCreativeTab(ValkyrienWarfareMod.vwTab);
        redstoneEngine = (BlockRedstoneEngine) new BlockRedstoneEngine(Material.REDSTONE_LIGHT, 500.0d).setHardness(7.0f).setUnlocalizedName("redstoneengine").setRegistryName(getModID(), "redstoneengine").setCreativeTab(ValkyrienWarfareMod.vwTab);

        antigravityEngine = (BlockNormalEtherCompressor) new BlockNormalEtherCompressor(Material.WOOD, 25000.0d).setHardness(8f).setUnlocalizedName("antigravengine").setRegistryName(getModID(), "antigravengine").setCreativeTab(ValkyrienWarfareMod.vwTab);
        advancedEtherCompressor = (BlockNormalEtherCompressor) new BlockNormalEtherCompressor(Material.ROCK, 45000.0d).setHardness(8f).setUnlocalizedName("advancedethercompressor").setRegistryName(getModID(), "advancedethercompressor").setCreativeTab(ValkyrienWarfareMod.vwTab);
        eliteEtherCompressor = (BlockNormalEtherCompressor) new BlockNormalEtherCompressor(Material.IRON, 80000.0d).setHardness(8f).setUnlocalizedName("eliteethercompressor").setRegistryName(getModID(), "eliteethercompressor").setCreativeTab(ValkyrienWarfareMod.vwTab);
        ultimateEtherCompressor = (BlockNormalEtherCompressor) new BlockNormalEtherCompressor(Material.GROUND, 100000.0d).setHardness(8f).setUnlocalizedName("ultimateethercompressor").setRegistryName(getModID(), "ultimateethercompressor").setCreativeTab(ValkyrienWarfareMod.vwTab);
        creativeEtherCompressor = (BlockCreativeEtherCompressor) new BlockCreativeEtherCompressor(Material.BARRIER, Double.MAX_VALUE / 4).setHardness(0.0f).setUnlocalizedName("creativeethercompressor").setRegistryName(getModID(), "creativeethercompressor").setCreativeTab(ValkyrienWarfareMod.vwTab);

        dopedEtherium = new BlockDopedEtherium(Material.GLASS).setHardness(4f).setUnlocalizedName("dopedetherium").setRegistryName(getModID(), "dopedetherium").setCreativeTab(ValkyrienWarfareMod.vwTab);
        pilotsChair = new BlockShipPilotsChair(Material.IRON).setHardness(4f).setUnlocalizedName("shippilotschair").setRegistryName(getModID(), "shippilotschair").setCreativeTab(ValkyrienWarfareMod.vwTab);

        passengerChair = new BlockShipPassengerChair(Material.IRON).setHardness(4f).setUnlocalizedName("shippassengerchair").setRegistryName(getModID(), "shippassengerchair").setCreativeTab(ValkyrienWarfareMod.vwTab);
        shipHelm = new BlockShipHelm(Material.WOOD).setHardness(4f).setUnlocalizedName("shiphelm").setRegistryName(getModID(), "shiphelm").setCreativeTab(ValkyrienWarfareMod.vwTab);
        shipWheel = new BlockShipWheel(Material.WOOD).setHardness(5f).setUnlocalizedName("shiphelmwheel").setRegistryName(getModID(), "shiphelmwheel");
        shipTelegraph = new BlockShipTelegraph(Material.WOOD).setHardness(5f).setUnlocalizedName("shiptelegraph").setRegistryName(getModID(), "shiptelegraph").setCreativeTab(ValkyrienWarfareMod.vwTab);

        thrustRelay = new BlockThrustRelay(Material.IRON).setHardness(5f).setUnlocalizedName("thrustrelay").setRegistryName(getModID(), "thrustrelay").setCreativeTab(ValkyrienWarfareMod.vwTab);
        thrustModulator = new BlockThrustModulator(Material.IRON).setHardness(8f).setUnlocalizedName("thrustmodulator").setRegistryName(getModID(), "thrustmodulator").setCreativeTab(ValkyrienWarfareMod.vwTab);

        gyroscope = new BlockGyroscope(Material.IRON).setHardness(5f).setUnlocalizedName("vw_gyroscope").setRegistryName(getModID(), "vw_gyroscope").setCreativeTab(ValkyrienWarfareMod.vwTab);

        liftValve = new BlockLiftValve(Material.IRON).setHardness(7f).setUnlocalizedName("vw_liftvalve").setRegistryName(getModID(), "vw_liftvalve").setCreativeTab(ValkyrienWarfareMod.vwTab);
        networkDisplay = new BlockNetworkDisplay(Material.IRON).setHardness(5f).setUnlocalizedName("vw_networkdisplay").setRegistryName(getModID(), "vw_networkdisplay").setCreativeTab(ValkyrienWarfareMod.vwTab);
        liftControl = new BlockLiftControl(Material.IRON).setHardness(5f).setUnlocalizedName("vw_liftcontrol").setRegistryName(getModID(), "vw_liftcontrol").setCreativeTab(ValkyrienWarfareMod.vwTab);
        etherGasCompressor = new BlockEtherGasCompressor(Material.IRON).setHardness(5f).setUnlocalizedName("vw_ethergascompressor").setRegistryName(getModID(), "vw_ethergascompressor").setCreativeTab(ValkyrienWarfareMod.vwTab);
    }

    protected void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(basicEngine);
        event.getRegistry().register(advancedEngine);
        event.getRegistry().register(eliteEngine);
        event.getRegistry().register(ultimateEngine);
        event.getRegistry().register(redstoneEngine);

        event.getRegistry().register(antigravityEngine);
        event.getRegistry().register(advancedEtherCompressor);
        event.getRegistry().register(eliteEtherCompressor);
        event.getRegistry().register(ultimateEtherCompressor);
        event.getRegistry().register(creativeEtherCompressor);

        event.getRegistry().register(dopedEtherium);
        event.getRegistry().register(pilotsChair);
        event.getRegistry().register(passengerChair);

        event.getRegistry().register(shipHelm);
        event.getRegistry().register(shipWheel);
        event.getRegistry().register(shipTelegraph);
        event.getRegistry().register(thrustRelay);
        event.getRegistry().register(thrustModulator);

        event.getRegistry().register(gyroscope);
        event.getRegistry().register(liftValve);
        event.getRegistry().register(networkDisplay);
        event.getRegistry().register(liftControl);
        event.getRegistry().register(etherGasCompressor);
    }

    protected void registerBlockItems(RegistryEvent.Register<Item> event) {
        registerItemBlock(event, basicEngine);
        registerItemBlock(event, advancedEngine);
        registerItemBlock(event, eliteEngine);
        registerItemBlock(event, ultimateEngine);
        registerItemBlock(event, redstoneEngine);

        registerItemBlock(event, antigravityEngine);
        registerItemBlock(event, advancedEtherCompressor);
        registerItemBlock(event, eliteEtherCompressor);
        registerItemBlock(event, ultimateEtherCompressor);
        registerItemBlock(event, creativeEtherCompressor);

        registerItemBlock(event, dopedEtherium);
        registerItemBlock(event, pilotsChair);
        registerItemBlock(event, passengerChair);

        registerItemBlock(event, shipHelm);
        registerItemBlock(event, shipWheel);
        registerItemBlock(event, shipTelegraph);
        registerItemBlock(event, thrustRelay);
        registerItemBlock(event, thrustModulator);

        registerItemBlock(event, gyroscope);
        registerItemBlock(event, liftValve);
        registerItemBlock(event, networkDisplay);
        registerItemBlock(event, liftControl);
        registerItemBlock(event, etherGasCompressor);
    }

    private void registerItemBlock(RegistryEvent.Register<Item> event, Block block) {
        Module.registerItemBlock(event, block);
    }

    private String getModID() {
        return mod_vwcontrol.getModID();
    }
}
