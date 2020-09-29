package xratedjunior.myfriend.client.renderer.entity;

import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.layers.BipedArmorLayer;
import net.minecraft.client.renderer.entity.layers.HeldItemLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import xratedjunior.myfriend.client.models.entity.DefaultTestMobModel;
import xratedjunior.myfriend.common.entity.RomeoEntity;
import xratedjunior.myfriend.core.MyFriend;

@OnlyIn(Dist.CLIENT)
public class RomeoTestRenderer extends BipedRenderer<RomeoEntity, DefaultTestMobModel<RomeoEntity>>
{	
	private static final ResourceLocation ROMEO_TEXTURES = MyFriend.resourceLocation("textures/entity/hunter_entity.png");
	   
	public RomeoTestRenderer(EntityRendererManager manager) {
		super(manager, new DefaultTestMobModel<>(), 0.5F);
		this.addLayer(new HeldItemLayer<>(this));
		this.addLayer(new BipedArmorLayer<>(this, new DefaultTestMobModel<>(0.5F, true), new DefaultTestMobModel<>(1.0F, true)));
	}

	public ResourceLocation getEntityTexture(RomeoEntity entity) {
		return ROMEO_TEXTURES;
	}
	   
	   
	public static class RenderFactory implements IRenderFactory<RomeoEntity>
	{
		@Override
		public EntityRenderer<? super RomeoEntity> createRenderFor(EntityRendererManager manager) {
			return new RomeoTestRenderer(manager);
		}		
	}
}