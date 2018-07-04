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

package valkyrienwarfare;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLFingerprintViolationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.event.FMLStateEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;
import valkyrienwarfare.api.IPhysicsEntityManager;
import valkyrienwarfare.api.RealPhysicsEntityManager;
import valkyrienwarfare.api.addons.Module;
import valkyrienwarfare.api.addons.VWAddon;
import valkyrienwarfare.deprecated_api.DataTag;
import valkyrienwarfare.deprecated_api.ValkyrienWarfareHooks;
import valkyrienwarfare.math.Vector;
import valkyrienwarfare.mixin.MixinLoaderForge;
import valkyrienwarfare.mod.BlockPhysicsRegistration;
import valkyrienwarfare.mod.block.BlockPhysicsInfuser;
import valkyrienwarfare.mod.block.BlockPhysicsInfuserCreative;
import valkyrienwarfare.mod.capability.IAirshipCounterCapability;
import valkyrienwarfare.mod.capability.ImplAirshipCounterCapability;
import valkyrienwarfare.mod.capability.StorageAirshipCounter;
import valkyrienwarfare.mod.command.VWModCommandRegistry;
import valkyrienwarfare.mod.gui.TabValkyrienWarfare;
import valkyrienwarfare.mod.network.PhysWrapperPositionHandler;
import valkyrienwarfare.mod.network.PhysWrapperPositionMessage;
import valkyrienwarfare.mod.network.SubspacedEntityRecordHandler;
import valkyrienwarfare.mod.network.SubspacedEntityRecordMessage;
import valkyrienwarfare.mod.physmanagement.chunk.DimensionPhysicsChunkManager;
import valkyrienwarfare.mod.proxy.CommonProxy;
import valkyrienwarfare.mod.proxy.ServerProxy;
import valkyrienwarfare.physics.management.DimensionPhysObjectManager;
import valkyrienwarfare.physics.management.PhysicsWrapperEntity;
import valkyrienwarfare.util.PhysicsSettings;
import valkyrienwarfare.util.RealMethods;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

@Mod(modid = ValkyrienWarfareMod.MODID,
        name = ValkyrienWarfareMod.MODNAME,
        version = ValkyrienWarfareMod.MODVER,
        guiFactory = "valkyrienwarfare.mod.gui.GuiFactoryValkyrienWarfare",
        updateJSON = "https://raw.githubusercontent.com/Valkyrien-Warfare/Valkyrien-Warfare-Revamped/update.json",
        certificateFingerprint = "8f639e7b2d1117d8f2c5d545e2231f0a0519f0ce")
public class ValkyrienWarfareMod {
    public static final List<Module> addons = new ArrayList<>();
    public static final String MODID = "valkyrienwarfare";
    public static final String MODNAME = "Valkyrien Warfare";
    public static final String MODVER = "0.9_prerelease_5";
    public static final int SHIP_ENTITY_PLAYER_LOAD_DISTANCE = 128;
    @CapabilityInject(IAirshipCounterCapability.class)
    public static final Capability<IAirshipCounterCapability> airshipCounter = null;
    // This service is directly responsible for running collision tasks.
    public static ExecutorService PHYSICS_THREADS_EXECUTOR;
    @SuppressWarnings("unused")
    @SidedProxy(clientSide = "valkyrienwarfare.mod.proxy.ClientProxy", serverSide = "valkyrienwarfare.mod.proxy.ServerProxy")
    public static CommonProxy proxy;
    public static File configFile;
    public static Configuration config;
    public static Vector gravity = new Vector(0, -9.8D, 0);
    public static double physSpeed = .01D;
    public static Block physicsInfuser;
    public static Block physicsInfuserCreative;
    public static SimpleNetworkWrapper physWrapperNetwork;
    public static final DimensionPhysicsChunkManager VW_CHUNK_MANAGER = new DimensionPhysicsChunkManager();
    public static final DimensionPhysObjectManager VW_PHYSICS_MANAGER = new DimensionPhysObjectManager();
    public static CreativeTabs vwTab = new TabValkyrienWarfare();
    @Instance(MODID)
    public static ValkyrienWarfareMod INSTANCE = new ValkyrienWarfareMod();
    public static int airStateIndex;
    public static double standingTolerance = .42D;
    public static int maxShipSize = 1500000;
    public static double shipUpperLimit = 1000D;
    public static double shipLowerLimit = -30D;
    public static int maxAirships = -1;
    public static boolean accurateRain;
    public static boolean runAirshipPermissions;
    public static int threadCount = -1;
    public static Logger VW_LOGGER;
    private static boolean hasAddonRegistrationEnded;
    public DataTag tag;

    /**
     * Called by the game when loading the configuration file, also called whenever
     * the player makes a change in the MOD OPTIONS menu, effectively reloading all
     * the configuration values
     *
     * @param conf
     */
    public static void applyConfig(Configuration conf) {
        shipUpperLimit = config.get(Configuration.CATEGORY_GENERAL, "Ship Y-Height Maximum", 1000D).getDouble();
        shipLowerLimit = config.get(Configuration.CATEGORY_GENERAL, "Ship Y-Height Minimum", -30D).getDouble();
        maxAirships = config.get(Configuration.CATEGORY_GENERAL, "Max airships per player", -1, "Players can't own more than this many airships at once. Set to -1 to disable.").getInt();
        accurateRain = config.get(Configuration.CATEGORY_GENERAL, "Enable accurate rain on ships", false, "Debug feature, takes a lot of processing power").getBoolean();
        runAirshipPermissions = config.get(Configuration.CATEGORY_GENERAL, "Enable airship permissions", false, "Enables the airship permissions system").getBoolean();
        threadCount = config.get(Configuration.CATEGORY_GENERAL, "Physics thread count", Runtime.getRuntime().availableProcessors() - 2,
                "The number of threads to use for physics, recommened to use your cpu's thread count minus 2.").getInt();
        if (PHYSICS_THREADS_EXECUTOR == null) {
            PHYSICS_THREADS_EXECUTOR = Executors.newFixedThreadPool(Math.max(2, threadCount));
        }
        addons.forEach(m -> m.applyConfig(config));
    }

    public static File getWorkingFolder() {
        File toBeReturned;
        try {
            if (FMLCommonHandler.instance().getSide().isClient()) {
                toBeReturned = Minecraft.getMinecraft().mcDataDir;
            } else {
                toBeReturned = FMLCommonHandler.instance().getMinecraftServerInstance().getFile("");
            }
            return toBeReturned;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Checks to see if a player's airship counter can be changed.
     *
     * @param isAdding Should be true if you are adding a player, false if removing the
     *                 player.
     * @param player   The player to check for
     * @return
     */
    public static boolean canChangeAirshipCounter(boolean isAdding, EntityPlayer player) {
        if (isAdding) {
            if (ValkyrienWarfareMod.maxAirships == -1) {
                return true;
            }

            return player.getCapability(ValkyrienWarfareMod.airshipCounter, null)
                    .getAirshipCount() < ValkyrienWarfareMod.maxAirships;
        } else {
            return player.getCapability(ValkyrienWarfareMod.airshipCounter, null).getAirshipCount() > 0;
        }
    }

    public static void registerAddon(Module module) {
        if (hasAddonRegistrationEnded) {
            throw new IllegalStateException("Attempting to register addon after FMLConstructionEvent");
        } else {
            System.out.println("[VW Addon System] Registering addon: " + module.getClass().getCanonicalName());
            for (Module registered : addons) {
                if (registered.getClass() == module.getClass()) {
                    System.out.println(
                            "Addon " + module.getClass().getCanonicalName() + " already registered, skipping...");
                    return;
                }
            }
            addons.add(module);
        }
    }

    @Mod.EventHandler
    public void onFingerprintViolation(FMLFingerprintViolationEvent event) {
        // Don't crash for signatures if we're in dev environment.
        if (MixinLoaderForge.isObfuscatedEnvironment) {
            FMLLog.bigWarning(
                    "Valkyrien Warfare JAR fingerprint corrupted, which means this copy of the mod may have come from unofficial sources. Download the mod from CurseForge: https://minecraft.curseforge.com/projects/valkyrien-warfare");
            FMLCommonHandler.instance().exitJava(123, true);
        }
    }

    @EventHandler
    public void fmlConstruct(FMLConstructionEvent event) {
        URLClassLoader classLoader = (URLClassLoader) this.getClass().getClassLoader();
        ArrayList<String> allAddons = new ArrayList<>();
        boolean isAddonBugFixed = true;

        if (!MixinLoaderForge.isObfuscatedEnvironment) { // if in dev, read default addons from gradle output folder
            File f = ValkyrienWarfareMod.getWorkingFolder();
            File defaultAddons;
            String[] list = f.list();
            boolean rootDir = false;
            for (String s : list) {
                if (s.endsWith("build.gradle")) {
                    rootDir = true;
                }
            }
            if (rootDir) { // assume root directory
                defaultAddons = new File(f.getPath() + File.separatorChar + "src" + File.separatorChar + "main"
                        + File.separatorChar + "resources" + File.separatorChar + "vwAddon_default");
            } else { // assume run/ directory or similar
                defaultAddons = new File(f.getAbsoluteFile().getParentFile().getParent() + File.separatorChar + "src"
                        + File.separatorChar + "main" + File.separatorChar + "resources" + File.separatorChar
                        + "vwAddon_default");
            }
            System.out.println(defaultAddons.getAbsolutePath());
            try {
                InputStream inputStream = new FileInputStream(defaultAddons);
                Scanner scanner = new Scanner(inputStream);
                while (scanner.hasNextLine()) {
                    String className = scanner.nextLine().trim();
                    allAddons.add(className);
                    System.out.println("Found addon " + className);
                }
                scanner.close();
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (URL url : classLoader.getURLs()) {
            try {
                // ZipFile file = new ZipFile(new File(url.toURI()));
                ZipInputStream zis = new ZipInputStream(
                        new BufferedInputStream(new FileInputStream(new File(url.getPath()))));
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().startsWith("vwAddon_")) {
                        try {
                            ZipFile file = new ZipFile(new File(url.getPath()));
                            InputStream inputStream = file.getInputStream(file.getEntry(entry.getName()));
                            Scanner scanner = new Scanner(inputStream);
                            while (scanner.hasNextLine()) {
                                String className = scanner.nextLine().trim();
                                allAddons.add(className);
                                System.out.println("Found addon " + className);
                            }
                            scanner.close();
                            inputStream.close();
                        } catch (IOException e) {
                            // wtf java
                        }
                        break;
                    }
                }
                zis.close();
            } catch (IOException e) {
                // wtf java
            }
        }

        ALL_ADDONS:
        for (String className : allAddons) {
            try {
                Class<?> abstractclass = Class.forName(className);
                if (abstractclass.isAnnotationPresent(VWAddon.class)) {
                    String depends = abstractclass.getAnnotation(VWAddon.class).dependsOn();
                    if (depends.isEmpty()) {
                        System.out.println(abstractclass.getCanonicalName() + " does not have mod dependency");
                    } else {
                        if (Loader.isModLoaded(depends)) {
                            System.out.println("Mod " + depends + " found (required by: " + abstractclass.getCanonicalName() + ')');
                        } else {
                            System.out.println("Mod " + depends + " not found (required by: " + abstractclass.getCanonicalName() + ')');
                            System.out.println(abstractclass.getCanonicalName() + " will not be loaded.");
                            continue;
                        }
                    }
                    for (Module registered : addons) {
                        if (registered.getClass().getCanonicalName().equals(abstractclass.getCanonicalName())) {
                            System.out.println(
                                    "Addon " + abstractclass.getCanonicalName() + " already registered, skipping...");
                            continue ALL_ADDONS;
                        }
                    }
                    Module module = (Module) abstractclass.newInstance();
                    registerAddon(module);
                } else {
                    System.out.println("Class " + className + " does not have @VWAddon annonation, not loading");
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
                System.out.println("Not loading addon: " + className);
            }
        }
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        hasAddonRegistrationEnded = true;

        proxy.preInit(event);
        this.registerNetworks(event);
        this.runConfiguration(event);
        this.registerCapibilities();
        ValkyrienWarfareHooks.methods = new RealMethods();
        ValkyrienWarfareHooks.isValkyrienWarfareInstalled = true;
        VW_LOGGER = Logger.getLogger("ValkyrienWarfare");

        addons.forEach(m -> m.doPreInit(event));

        // Now change the static final field of INSTANCE in the IPhysicsEntityManager by using reflection.
        try {
            // First get the INSTANCE field
            Field vw_api_physics_entity_manager_field = IPhysicsEntityManager.class.getDeclaredField("INSTANCE");
            vw_api_physics_entity_manager_field.setAccessible(true);
            // Then remove the final modifier
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(vw_api_physics_entity_manager_field, vw_api_physics_entity_manager_field.getModifiers() & ~Modifier.FINAL);
            // Then update the field
            vw_api_physics_entity_manager_field.set(null, new RealPhysicsEntityManager());
            if (IPhysicsEntityManager.INSTANCE instanceof RealPhysicsEntityManager) {
                System.out.println("VW API injection successful!");
            } else {
                System.err.println("VW API injection failed! The API will not interact with other mods correctly!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("VW API injection failed! The API will not interact with other mods correctly!");
        }
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        // Print out a message of core count, we want this to know what AnvilNode is giving us.
        System.out.println("Valyrien Warfare Initilization:");
        System.out.println("We are running on " + Runtime.getRuntime().availableProcessors() + " threads; 4 or more is recommended!");
        proxy.init(event);
        EntityRegistry.registerModEntity(new ResourceLocation(MODID, "PhysWrapper"), PhysicsWrapperEntity.class,
                "PhysWrapper", 70, this, SHIP_ENTITY_PLAYER_LOAD_DISTANCE, 5, false);

        addons.forEach(m -> m.doInit(event));
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
        airStateIndex = Block.getStateId(Blocks.AIR.getDefaultState());
        BlockPhysicsRegistration.registerCustomBlockMasses();
        BlockPhysicsRegistration.registerVanillaBlockForces();
        BlockPhysicsRegistration.registerBlocksToNotPhysicise();

        addons.forEach(m -> m.doPostInit(event));
    }

    @EventHandler
    public void serverStart(FMLServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        VWModCommandRegistry.registerCommands(server);
    }

    private void registerNetworks(FMLStateEvent event) {
        physWrapperNetwork = NetworkRegistry.INSTANCE.newSimpleChannel("physChannel");
        physWrapperNetwork.registerMessage(PhysWrapperPositionHandler.class, PhysWrapperPositionMessage.class, 0,
                Side.CLIENT);
        physWrapperNetwork.registerMessage(SubspacedEntityRecordHandler.class, SubspacedEntityRecordMessage.class, 1, Side.CLIENT);
        physWrapperNetwork.registerMessage(SubspacedEntityRecordHandler.class, SubspacedEntityRecordMessage.class, 2, Side.SERVER);
    }

    public void registerBlocks(RegistryEvent.Register<Block> event) {
        physicsInfuser = new BlockPhysicsInfuser(Material.ROCK).setHardness(8f).setUnlocalizedName("shipblock")
                .setRegistryName(MODID, "shipblock").setCreativeTab(vwTab);
        physicsInfuserCreative = new BlockPhysicsInfuserCreative(Material.ROCK).setHardness(12f)
                .setUnlocalizedName("shipblockcreative").setRegistryName(MODID, "shipblockcreative")
                .setCreativeTab(vwTab);

        event.getRegistry().register(physicsInfuser);
        event.getRegistry().register(physicsInfuserCreative);
    }

    public void registerItems(RegistryEvent.Register<Item> event) {
        Module.registerItemBlock(event, physicsInfuser);
        Module.registerItemBlock(event, physicsInfuserCreative);
    }

    public void registerRecipies(RegistryEvent.Register<IRecipe> event) {
        Module.registerRecipe(event, new ItemStack(physicsInfuser), "IEI", "ODO", "IEI", 'E', Items.ENDER_PEARL, 'D',
                Items.DIAMOND, 'O', Item.getItemFromBlock(Blocks.OBSIDIAN), 'I', Items.IRON_INGOT);
    }

    private void runConfiguration(FMLPreInitializationEvent event) {
        configFile = event.getSuggestedConfigurationFile();
        config = new Configuration(configFile);
        config.load();
        applyConfig(config);
        config.save();
    }

    @EventHandler
    public void onServerStarted(FMLServerStartedEvent event) {
        this.loadConfig();
    }

    @EventHandler
    public void onServerStopping(FMLServerStoppingEvent event) {
        this.saveConfig();
    }

    public void loadConfig() {
        File file = new File(ValkyrienWarfareMod.getWorkingFolder(), "/valkyrienwarfaresettings.dat");

        if (!file.exists()) {
            this.tag = new DataTag(file);
            this.tag.setBoolean("doGravity", true);
            this.tag.setBoolean("doPhysicsBlocks", true);
            this.tag.setBoolean("doBalloons", true);
            this.tag.setBoolean("doAirshipRotation", true);
            this.tag.setBoolean("doAirshipMovement", true);
            this.tag.setBoolean("doSplitting", false);
            this.tag.setInteger("maxShipSize", 15000);
            this.tag.setDouble("gravityVecX", 0);
            this.tag.setDouble("gravityVecY", -9.8);
            this.tag.setDouble("gravityVecZ", 0);
            this.tag.setInteger("physicsIterations", 10);
            this.tag.setDouble("physicsSpeed", 0.05);
            this.tag.setBoolean("doEtheriumLifting", true);
            this.tag.save();
        } else {
            this.tag = new DataTag(file);
        }

        PhysicsSettings.doGravity = this.tag.getBoolean("doGravity", true);
        PhysicsSettings.doPhysicsBlocks = this.tag.getBoolean("doPhysicsBlocks", true);
        PhysicsSettings.doAirshipRotation = this.tag.getBoolean("doAirshipRotation", true);
        PhysicsSettings.doAirshipMovement = this.tag.getBoolean("doAirshipMovement", true);
        ValkyrienWarfareMod.maxShipSize = this.tag.getInteger("maxShipSize", 15000);
        ValkyrienWarfareMod.physSpeed = this.tag.getDouble("physicsSpeed", 0.01D);
        // TODO: Remove me later; this is just to force players VW configs to update.
        if (ValkyrienWarfareMod.physSpeed == .05D) {
            ValkyrienWarfareMod.physSpeed = .01D;
        }
        ValkyrienWarfareMod.gravity = new Vector(this.tag.getDouble("gravityVecX", 0.0), this.tag.getDouble("gravityVecY", -9.8),
                this.tag.getDouble("gravityVecZ", 0.0));
        PhysicsSettings.doEtheriumLifting = this.tag.getBoolean("doEtheriumLifting", true);

        // save the tag in case new fields are added, this way they are saved right away
        this.tag.save();
    }

    public void saveConfig() {
        this.tag.setBoolean("doGravity", PhysicsSettings.doGravity);
        this.tag.setBoolean("doPhysicsBlocks", PhysicsSettings.doPhysicsBlocks);
        this.tag.setBoolean("doAirshipRotation", PhysicsSettings.doAirshipRotation);
        this.tag.setBoolean("doAirshipMovement", PhysicsSettings.doAirshipMovement);
        this.tag.setInteger("maxShipSize", ValkyrienWarfareMod.maxShipSize);
        this.tag.setDouble("gravityVecX", ValkyrienWarfareMod.gravity.X);
        this.tag.setDouble("gravityVecY", ValkyrienWarfareMod.gravity.Y);
        this.tag.setDouble("gravityVecZ", ValkyrienWarfareMod.gravity.Z);
        this.tag.setDouble("physicsSpeed", ValkyrienWarfareMod.physSpeed);
        this.tag.save();
    }

    public void registerCapibilities() {
        CapabilityManager.INSTANCE.register(IAirshipCounterCapability.class, new StorageAirshipCounter(),
                ImplAirshipCounterCapability.class);
    }

    /**
     * Checks instance of ServerProxy to avoid calling client code on server side
     *
     * @return
     */
    public boolean isRunningOnClient() {
        return !(proxy instanceof ServerProxy);
    }
}
