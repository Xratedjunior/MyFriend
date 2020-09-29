package xratedjunior.myfriend.client.renderer.entity;

import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xratedjunior.myfriend.client.models.entity.defaultmodel.DefaultModel;
import xratedjunior.myfriend.common.entity.RomeoEntity;
import xratedjunior.myfriend.core.MyFriend;

@OnlyIn(Dist.CLIENT)
public class RomeoRenderer extends DefaultRendererAbstract<RomeoEntity, DefaultModel<RomeoEntity>>
{	
	public RomeoRenderer(EntityRendererManager renderManagerIn) {
		super(renderManagerIn, new DefaultModel<>(0.0F, false), 0.5F, new DefaultModel<>(0.5F, true), new DefaultModel<>(1.0F, true));
	}
	
	private static final ResourceLocation ROMEO_TEXTURES = MyFriend.resourceLocation("textures/entity/romeo.png");
	
	/**
	* Returns the location of an entity's texture.
	*/
	public ResourceLocation getEntityTexture(RomeoEntity entity) {
		return ROMEO_TEXTURES;
	}
}