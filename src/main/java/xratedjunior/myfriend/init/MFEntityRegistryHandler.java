package xratedjunior.myfriend.init;

import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import xratedjunior.myfriend.api.entity.MFEntityTypes;
import xratedjunior.myfriend.common.entity.RomeoEntity;
import xratedjunior.myfriend.core.MyFriend;

@EventBusSubscriber(modid = MyFriend.MOD_ID, bus = Bus.MOD)
public class MFEntityRegistryHandler 
{	
	@SubscribeEvent
	public static void onRegisterItems(Register<Item> event)
	{
		register(event.getRegistry(), "romeo_spawn_egg", new SpawnEggItem(MFEntityTypes.ROMEO, 0xfbc79d, 0xb50000, new Item.Properties().group(ItemGroup.MISC)));
	}

	@SubscribeEvent
	public static void onRegisterEntityTypes(Register<EntityType<?>> event)
	{
		MFEntityTypes.init(event);
		EntitySpawnPlacementRegistry.register(MFEntityTypes.ROMEO, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, RomeoEntity::checkRomeoSpawnRules);
	}
	
	public static <T extends IForgeRegistryEntry<T>> void register(IForgeRegistry<T> registry, String name, T object)
	{
		object.setRegistryName(MyFriend.resourceLocation(name));
		registry.register(object);
	}
}
