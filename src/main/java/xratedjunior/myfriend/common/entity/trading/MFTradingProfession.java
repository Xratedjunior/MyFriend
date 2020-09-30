package xratedjunior.myfriend.common.entity.trading;

import com.google.common.collect.ImmutableSet;

import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.util.SoundEvent;
import net.minecraft.village.PointOfInterestType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import xratedjunior.myfriend.core.MyFriend;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class MFTradingProfession {
	public static final VillagerProfession FRIEND = new VillagerProfession("friend", PointOfInterestType.NITWIT, ImmutableSet.of(), ImmutableSet.of(), (SoundEvent)null);
	
    @SubscribeEvent
    public static void registerProfessions(RegistryEvent.Register<VillagerProfession> event)
	{
		MyFriend.logger.info("Registering Friend Professions");
    	registerProfession(FRIEND, "friend");
	}
    
    public static VillagerProfession registerProfession(VillagerProfession profession, String name)
    {
    	profession.setRegistryName(name);
        ForgeRegistries.PROFESSIONS.register(profession);
        return profession;
    }
}
