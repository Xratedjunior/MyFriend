package xratedjunior.myfriend.client;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import xratedjunior.myfriend.api.entity.MFEntityTypes;
import xratedjunior.myfriend.client.renderer.entity.RomeoRenderer;

public class MFEntityRendering 
{
	public static void init()
	{
		register(MFEntityTypes.ROMEO, RomeoRenderer::new);
	}

	private static <T extends Entity> void register(EntityType<T> entityClass, IRenderFactory<? super T> renderFactory)
	{
		RenderingRegistry.registerEntityRenderingHandler(entityClass, renderFactory);
	}
}
