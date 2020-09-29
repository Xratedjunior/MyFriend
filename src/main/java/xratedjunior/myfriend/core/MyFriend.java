package xratedjunior.myfriend.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import xratedjunior.myfriend.client.MFRenderInit;
import xratedjunior.myfriend.configuration.MFConfig;

@Mod(value = MyFriend.MOD_ID)
public class MyFriend 
{
    public static MyFriend instance;
    public static final String MOD_ID = "myfriend";
    public static Logger logger = LogManager.getLogger(MOD_ID);

    public MyFriend()
    {
    	ModLoadingContext.get().registerConfig(Type.COMMON, MFConfig.COMMON_SPEC, "myFriend.toml");

		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(MFRenderInit::initialization);
    }
    
	//Will run at launch (preInit)
	private void commonSetup(final FMLCommonSetupEvent event)
	{

		logger.info("Setup method registered.");
	}
	
	public static ResourceLocation resourceLocation(String name)
	{
		return new ResourceLocation(MOD_ID, name);
	}
	
	public static String find(String key)
	{
		return new String(MOD_ID + ":" + key);
	}
}
