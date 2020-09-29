package xratedjunior.myfriend.client;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class MFRenderInit 
{
	public static void initialization(FMLClientSetupEvent event)
	{
		MFEntityRendering.init();
	}
}
