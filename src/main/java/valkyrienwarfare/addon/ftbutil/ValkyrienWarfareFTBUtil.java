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

package valkyrienwarfare.addon.ftbutil;

import lombok.Getter;
import lombok.NonNull;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLStateEvent;
import valkyrienwarfare.ValkyrienWarfareMod;
import valkyrienwarfare.addon.ftbutil.item.ItemAirshipClaimer;
import valkyrienwarfare.api.addons.Module;
import valkyrienwarfare.physics.management.PhysicsObject;

/**
 * @author DaPorkchop_
 */
public class ValkyrienWarfareFTBUtil extends Module {
    private static boolean LOADED = false;

    @Getter
    private static ValkyrienWarfareFTBUtil instance;

    @Getter
    private ItemAirshipClaimer airshipClaimer;

    public ValkyrienWarfareFTBUtil() {
        super("valkyrienwarfareftb", null, null, null);
        if (instance != null) {
            throw new IllegalStateException("Instance already set!");
        }
        instance = this;
    }

    @Override
    public void applyConfig(Configuration config) {
    }

    @Override
    protected void preInit(FMLStateEvent event) {
    }

    @Override
    protected void init(FMLStateEvent event) {
    }

    @Override
    protected void postInit(FMLStateEvent event) {
    }

    public static void initialClaim(@NonNull PhysicsObject object) {
        if (!LOADED) {
            return;
        }
        instance.airshipClaimer.initialClaim(object);
    }

    public static void handleClaim(@NonNull PhysicsObject object, int relX, int relZ) {
        if (!LOADED || object.getOwner() == null) {
            return;
        }
        instance.airshipClaimer.handleClaim(object, relX, relZ);
    }

    public static void handleUnclaim(@NonNull PhysicsObject object) {
        if (!LOADED) {
            return;
        }
        instance.airshipClaimer.handleUnclaim(object);
    }

    @Override
    public void registerItems(RegistryEvent.Register<Item> event) {
        if (Loader.isModLoaded("ftbutilities")) {
            LOADED = true;
            event.getRegistry().register(this.airshipClaimer = (ItemAirshipClaimer) new ItemAirshipClaimer().setUnlocalizedName("airshipclaimer").setRegistryName(new ResourceLocation(this.getModID(), "airshipclaimer")).setCreativeTab(ValkyrienWarfareMod.vwTab).setMaxStackSize(16));
        } else {
            System.out.println("FTB Utilities not found, skipping integration!");
        }
    }
}
