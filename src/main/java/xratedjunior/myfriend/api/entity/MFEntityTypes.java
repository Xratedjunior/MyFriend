package xratedjunior.myfriend.api.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.attributes.GlobalEntityTypeAttributes;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.registries.ObjectHolder;
import xratedjunior.myfriend.common.entity.DefaultEntity;
import xratedjunior.myfriend.common.entity.RomeoEntity;
import xratedjunior.myfriend.core.MyFriend;
import xratedjunior.myfriend.init.MFEntityRegistryHandler;

@ObjectHolder(MyFriend.MOD_ID)
public class MFEntityTypes 
{
	public static final EntityType<RomeoEntity> ROMEO = buildEntity("romeo", EntityType.Builder.create(RomeoEntity::new, EntityClassification.MISC).size(0.6F, 1.95F));

	public static void init(Register<EntityType<?>> event)
	{
		MFEntityRegistryHandler.register(event.getRegistry(), "romeo", ROMEO);
		GlobalEntityTypeAttributes.put(ROMEO, DefaultEntity.abstractFriendAttributes().func_233813_a_());
	}
	
	private static <T extends Entity> EntityType<T> buildEntity(String key, EntityType.Builder<T> builder)
	{
		return builder.build(MyFriend.find(key));
	}
}
