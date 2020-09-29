package xratedjunior.myfriend.init;

import org.apache.logging.log4j.Logger;

import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import xratedjunior.myfriend.core.MyFriend;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class MFItemsInit 
{
    public static Logger logger = MyFriend.logger;
	
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event)
	{	
    	    	
    	logger.info("Items registered");
	}
    
    public static Item registerItem(Item item, String name)
    {
    	item.setRegistryName(name);
        ForgeRegistries.ITEMS.register(item);
        return item;
    }
}
