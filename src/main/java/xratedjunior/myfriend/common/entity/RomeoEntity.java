package xratedjunior.myfriend.common.entity;

import java.util.Random;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

public class RomeoEntity extends FriendEntity
{
	public RomeoEntity(EntityType<? extends RomeoEntity> type, World worldIn) {
		super(type, worldIn);
	}
	
	public static boolean checkRomeoSpawnRules(EntityType<? extends RomeoEntity> type, IWorld worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
		return canSpawnOn(type, worldIn, reason, pos, randomIn);
	}
}
