package valkyrienwarfare.api;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import valkyrienwarfare.ValkyrienWarfareMod;

/**
 * Unlike the dummy version, this class actually returns something.
 * 
 * @author thebest108
 *
 */
public class RealPhysicsEntityManager implements IPhysicsEntityManager {

	@Override
	public IPhysicsEntity getPhysicsEntityFromShipSpace(World world, BlockPos pos) {
		return ValkyrienWarfareMod.VW_PHYSICS_MANAGER.getObjectManagingPos(world, pos);
	}

}
