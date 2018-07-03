package valkyrienwarfare.addon.ftbutil;

import lombok.Getter;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.event.FMLStateEvent;
import valkyrienwarfare.ValkyrienWarfareMod;
import valkyrienwarfare.addon.ftbutil.item.ItemAirshipClaimer;
import valkyrienwarfare.api.addons.Module;

/**
 * @author DaPorkchop_
 */
public class ValkyrienWarfareFTBUtil extends Module {
    @Getter
    private ItemAirshipClaimer airshipClaimer;

    public ValkyrienWarfareFTBUtil() {
        super("valkyrienwarfareftb", null, null, null);
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

    @Override
    public void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(this.airshipClaimer = (ItemAirshipClaimer) new ItemAirshipClaimer().setUnlocalizedName("airshipclaimer").setRegistryName(new ResourceLocation(getModID(), "airshipclaimer")).setCreativeTab(ValkyrienWarfareMod.vwTab).setMaxStackSize(16));
    }
}
