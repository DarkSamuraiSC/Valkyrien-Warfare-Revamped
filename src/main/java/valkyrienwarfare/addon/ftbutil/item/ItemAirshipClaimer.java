package valkyrienwarfare.addon.ftbutil.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import valkyrienwarfare.ValkyrienWarfareMod;
import valkyrienwarfare.physics.management.PhysicsWrapperEntity;

/**
 * @author DaPorkchop_
 */
public class ItemAirshipClaimer extends Item {
    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return EnumActionResult.PASS;
        } else {
            PhysicsWrapperEntity entity = ValkyrienWarfareMod.VW_PHYSICS_MANAGER.getObjectManagingPos(world, pos);
            if (entity == null) {
                return EnumActionResult.FAIL;
            }
            //TODO: set airship owner, and handle automatic claiming and unclaiming of chunks
            return null;
        }
    }
}
