package xratedjunior.myfriend.configuration;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig;
import xratedjunior.myfriend.core.MyFriend;

@Mod.EventBusSubscriber(modid = MyFriend.MOD_ID, bus = Bus.MOD)
public class MFConfig 
{
	private static Logger logger = MyFriend.logger;
	
	public static class Common
	{
		
		public Common(ForgeConfigSpec.Builder builder)
		{	
			//HunterConfig.init(builder);
			//DebugConfig.init(builder);
			logger.info("Built Config");
		}
	}
	
	public static final ForgeConfigSpec COMMON_SPEC;
	public static final Common COMMON;
	static {
		final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
		COMMON_SPEC = specPair.getRight();
		COMMON = specPair.getLeft();
	}
	
    @SubscribeEvent
    public static void onLoad(final ModConfig.Loading configEvent) {
    	//logger.info("Registering Hunter World Spawns");
    	//HunterModSpawns.registerEntityWorldSpawn();
		logger.info("Loaded Config");
    }

    @SubscribeEvent
    public static void onFileChange(final ModConfig.Reloading configEvent) {
    	//logger.info("Huntermod Config Changed");
    }

}
