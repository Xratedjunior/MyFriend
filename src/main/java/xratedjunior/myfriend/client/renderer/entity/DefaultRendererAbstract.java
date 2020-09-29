package xratedjunior.myfriend.client.renderer.entity;

import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.layers.BipedArmorLayer;
import net.minecraft.entity.MobEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xratedjunior.myfriend.client.models.entity.defaultmodel.DefaultModel;

@OnlyIn(Dist.CLIENT)
public abstract class DefaultRendererAbstract <T extends MobEntity, M extends DefaultModel<T>> extends BipedRenderer<T, M> 
{
	protected DefaultRendererAbstract(EntityRendererManager renderManagerIn, M modelBipedIn, float shadowSize, M modelLeggings, M modelArmor) {
		super(renderManagerIn, modelBipedIn, shadowSize);
		this.addLayer(new BipedArmorLayer<>(this, modelLeggings, modelArmor));
	}

}
