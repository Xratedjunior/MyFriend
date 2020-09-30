package xratedjunior.myfriend.common.entity.trading.event;

import net.minecraft.item.Items;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import xratedjunior.myfriend.common.entity.trading.MFTradingProfession;
import xratedjunior.myfriend.common.entity.trading.VillagerTradeUtils;
import xratedjunior.myfriend.core.MyFriend;

@EventBusSubscriber(modid = MyFriend.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public class MFTrades {
	
	@SubscribeEvent
	public static void onVillagerTradesEvent(VillagerTradesEvent event)
	{
		if (event.getType() == MFTradingProfession.FRIEND)
		{
			event.getTrades().get(1).add(new VillagerTradeUtils.ItemsForEmeraldsTrade(Items.DIAMOND, 2, 16, 1));
			MyFriend.logger.info("Added Friend Trades");
		}
	}
}
