package xratedjunior.myfriend.common.entity.backups;

import javax.annotation.Nullable;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.merchant.IReputationTracking;
import net.minecraft.entity.merchant.IReputationType;
import net.minecraft.entity.merchant.villager.VillagerData;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.entity.merchant.villager.VillagerTrades;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.villager.IVillagerDataHolder;
import net.minecraft.entity.villager.VillagerType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MerchantOffer;
import net.minecraft.item.MerchantOffers;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.network.DebugPacketSender;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.village.GossipManager;
import net.minecraft.village.GossipType;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.raid.Raid;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xratedjunior.myfriend.init.MFEntityRegistryHandler;

public abstract class TradeableFriendEntity extends TradeableEntity implements IReputationTracking, IVillagerDataHolder {
   private static final DataParameter<VillagerData> VILLAGER_DATA = EntityDataManager.createKey(TradeableFriendEntity.class, DataSerializers.VILLAGER_DATA);
   private int timeUntilReset;
   private boolean leveledUp;
   @Nullable
   private PlayerEntity previousCustomer;
   private byte foodLevel;
   private final GossipManager gossip = new GossipManager();
   private long lastGossipTime;
   private long lastGossipDecay;
   private int xp;
   private long lastRestock;
   private int restocksToday;
   private long lastRestockDayTime;
   private boolean assignProfessionWhenSpawned;

   public TradeableFriendEntity(EntityType<? extends TradeableFriendEntity> type, World worldIn) {
      this(type, worldIn, VillagerType.PLAINS);
   }

   public TradeableFriendEntity(EntityType<? extends TradeableFriendEntity> type, World worldIn, VillagerType villagerType) {
      super(type, worldIn);
      ((GroundPathNavigator)this.getNavigator()).setBreakDoors(true);
      this.getNavigator().setCanSwim(true);
      this.setCanPickUpLoot(true);
      this.setVillagerData(this.getVillagerData().withType(villagerType).withProfession(VillagerProfession.NONE));
   }

   public boolean func_234552_eW_() {
      return this.assignProfessionWhenSpawned;
   }

   protected void updateAITasks() {
      this.world.getProfiler().startSection("villagerBrain");
      this.world.getProfiler().endSection();
      if (this.assignProfessionWhenSpawned) {
         this.assignProfessionWhenSpawned = false;
      }

      if (!this.hasCustomer() && this.timeUntilReset > 0) {
         --this.timeUntilReset;
         if (this.timeUntilReset <= 0) {
            if (this.leveledUp) {
               this.levelUp();
               this.leveledUp = false;
            }

            this.addPotionEffect(new EffectInstance(Effects.REGENERATION, 200, 0));
         }
      }

      if (this.previousCustomer != null && this.world instanceof ServerWorld) {
         ((ServerWorld)this.world).updateReputation(IReputationType.TRADE, this.previousCustomer, this);
         this.world.setEntityState(this, (byte)14);
         this.previousCustomer = null;
      }

      if (!this.isAIDisabled() && this.rand.nextInt(100) == 0) {
         Raid raid = ((ServerWorld)this.world).findRaid(this.getPosition());
         if (raid != null && raid.isActive() && !raid.isOver()) {
            this.world.setEntityState(this, (byte)42);
         }
      }

      if (this.getVillagerData().getProfession() == VillagerProfession.NONE && this.hasCustomer()) {
         this.resetCustomer();
      }

      super.updateAITasks();
   }

   /**
    * Called to update the entity's position/logic.
    */
   public void tick() {
      super.tick();
      if (this.getShakeHeadTicks() > 0) {
         this.setShakeHeadTicks(this.getShakeHeadTicks() - 1);
      }
      this.tickGossip();
   }

   public ActionResultType func_230254_b_(PlayerEntity player, Hand hand) {
      ItemStack itemstack = player.getHeldItem(hand);
      if (itemstack.getItem() != MFEntityRegistryHandler.ROMEO_SPAWN_EGG && this.isAlive() && !this.hasCustomer() && !this.isSleeping() && !player.isSecondaryUseActive()) {
         if (this.isChild()) {
            this.shakeHead();
            return ActionResultType.func_233537_a_(this.world.isRemote);
         } else {
            boolean flag = this.getOffers().isEmpty();
            if (hand == Hand.MAIN_HAND) {
               if (flag && !this.world.isRemote) {
                  this.shakeHead();
               }

               player.addStat(Stats.TALKED_TO_VILLAGER);
            }

            if (flag) {
               return ActionResultType.func_233537_a_(this.world.isRemote);
            } else {
               if (!this.world.isRemote && !this.offers.isEmpty()) {
                  this.displayMerchantGui(player);
               }

               return ActionResultType.func_233537_a_(this.world.isRemote);
            }
         }
      } else {
         return super.func_230254_b_(player, hand);
      }
   }

   private void shakeHead() {
      this.setShakeHeadTicks(40);
      if (!this.world.isRemote()) {
         this.playSound(SoundEvents.ENTITY_VILLAGER_NO, this.getSoundVolume(), this.getSoundPitch());
      }
   }

   private void displayMerchantGui(PlayerEntity player) {
      this.recalculateSpecialPricesFor(player);
      this.setCustomer(player);
      this.openMerchantContainer(player, this.getDisplayName(), this.getVillagerData().getLevel());
   }

   public void setCustomer(@Nullable PlayerEntity player) {
      boolean flag = this.getCustomer() != null && player == null;
      super.setCustomer(player);
      if (flag) {
         this.resetCustomer();
      }

   }

   protected void resetCustomer() {
      super.resetCustomer();
      this.resetAllSpecialPrices();
   }

   private void resetAllSpecialPrices() {
      for(MerchantOffer merchantoffer : this.getOffers()) {
         merchantoffer.resetSpecialPrice();
      }

   }

   public boolean canRestockTrades() {
      return true;
   }

   public void restock() {
      this.calculateDemandOfOffers();

      for(MerchantOffer merchantoffer : this.getOffers()) {
         merchantoffer.resetUses();
      }

      this.lastRestock = this.world.getGameTime();
      ++this.restocksToday;
   }

   private boolean hasUsedOffer() {
      for(MerchantOffer merchantoffer : this.getOffers()) {
         if (merchantoffer.hasBeenUsed()) {
            return true;
         }
      }

      return false;
   }

   private boolean func_223720_ew() {
      return this.restocksToday == 0 || this.restocksToday < 2 && this.world.getGameTime() > this.lastRestock + 2400L;
   }

   public boolean func_223721_ek() {
      long i = this.lastRestock + 12000L;
      long j = this.world.getGameTime();
      boolean flag = j > i;
      long k = this.world.getDayTime();
      if (this.lastRestockDayTime > 0L) {
         long l = this.lastRestockDayTime / 24000L;
         long i1 = k / 24000L;
         flag |= i1 > l;
      }

      this.lastRestockDayTime = k;
      if (flag) {
         this.lastRestock = j;
         this.func_223718_eH();
      }

      return this.func_223720_ew() && this.hasUsedOffer();
   }

   private void func_223719_ex() {
      int i = 2 - this.restocksToday;
      if (i > 0) {
         for(MerchantOffer merchantoffer : this.getOffers()) {
            merchantoffer.resetUses();
         }
      }

      for(int j = 0; j < i; ++j) {
         this.calculateDemandOfOffers();
      }

   }

   private void calculateDemandOfOffers() {
      for(MerchantOffer merchantoffer : this.getOffers()) {
         merchantoffer.calculateDemand();
      }

   }

   private void recalculateSpecialPricesFor(PlayerEntity playerIn) {
      int i = this.getPlayerReputation(playerIn);
      if (i != 0) {
         for(MerchantOffer merchantoffer : this.getOffers()) {
            merchantoffer.increaseSpecialPrice(-MathHelper.floor((float)i * merchantoffer.getPriceMultiplier()));
         }
      }

      if (playerIn.isPotionActive(Effects.HERO_OF_THE_VILLAGE)) {
         EffectInstance effectinstance = playerIn.getActivePotionEffect(Effects.HERO_OF_THE_VILLAGE);
         int k = effectinstance.getAmplifier();

         for(MerchantOffer merchantoffer1 : this.getOffers()) {
            double d0 = 0.3D + 0.0625D * (double)k;
            int j = (int)Math.floor(d0 * (double)merchantoffer1.getBuyingStackFirst().getCount());
            merchantoffer1.increaseSpecialPrice(-Math.max(j, 1));
         }
      }

   }

   protected void registerData() {
      super.registerData();
      this.dataManager.register(VILLAGER_DATA, new VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 1));
   }

   public void writeAdditional(CompoundNBT compound) {
      super.writeAdditional(compound);
      VillagerData.CODEC.encodeStart(NBTDynamicOps.INSTANCE, this.getVillagerData()).resultOrPartial(LOGGER::error).ifPresent((p_234547_1_) -> {
         compound.put("VillagerData", p_234547_1_);
      });
      compound.putByte("FoodLevel", this.foodLevel);
      compound.put("Gossips", this.gossip.write(NBTDynamicOps.INSTANCE).getValue());
      compound.putInt("Xp", this.xp);
      compound.putLong("LastRestock", this.lastRestock);
      compound.putLong("LastGossipDecay", this.lastGossipDecay);
      compound.putInt("RestocksToday", this.restocksToday);
      if (this.assignProfessionWhenSpawned) {
         compound.putBoolean("AssignProfessionWhenSpawned", true);
      }

   }

   /**
    * (abstract) Protected helper method to read subclass entity data from NBT.
    */
   public void readAdditional(CompoundNBT compound) {
      super.readAdditional(compound);
      if (compound.contains("VillagerData", 10)) {
         DataResult<VillagerData> dataresult = VillagerData.CODEC.parse(new Dynamic<>(NBTDynamicOps.INSTANCE, compound.get("VillagerData")));
         dataresult.resultOrPartial(LOGGER::error).ifPresent(this::setVillagerData);
      }

      if (compound.contains("Offers", 10)) {
         this.offers = new MerchantOffers(compound.getCompound("Offers"));
      }

      if (compound.contains("FoodLevel", 1)) {
         this.foodLevel = compound.getByte("FoodLevel");
      }

      ListNBT listnbt = compound.getList("Gossips", 10);
      this.gossip.read(new Dynamic<>(NBTDynamicOps.INSTANCE, listnbt));
      if (compound.contains("Xp", 3)) {
         this.xp = compound.getInt("Xp");
      }

      this.lastRestock = compound.getLong("LastRestock");
      this.lastGossipDecay = compound.getLong("LastGossipDecay");
      this.setCanPickUpLoot(true);
      this.restocksToday = compound.getInt("RestocksToday");
      if (compound.contains("AssignProfessionWhenSpawned")) {
         this.assignProfessionWhenSpawned = compound.getBoolean("AssignProfessionWhenSpawned");
      }

   }

   public boolean canDespawn(double distanceToClosestPlayer) {
      return false;
   }

   @Nullable
   protected SoundEvent getAmbientSound() {
      if (this.isSleeping()) {
         return null;
      } else {
         return this.hasCustomer() ? SoundEvents.ENTITY_VILLAGER_TRADE : null;
      }
   }

   public void setVillagerData(VillagerData p_213753_1_) {
      VillagerData villagerdata = this.getVillagerData();
      if (villagerdata.getProfession() != p_213753_1_.getProfession()) {
         this.offers = null;
      }

      this.dataManager.set(VILLAGER_DATA, p_213753_1_);
   }

   public VillagerData getVillagerData() {
      return this.dataManager.get(VILLAGER_DATA);
   }

   protected void onVillagerTrade(MerchantOffer offer) {
      int i = 3 + this.rand.nextInt(4);
      this.xp += offer.getGivenExp();
      this.previousCustomer = this.getCustomer();
      if (this.canLevelUp()) {
         this.timeUntilReset = 40;
         this.leveledUp = true;
         i += 5;
      }

      if (offer.getDoesRewardExp()) {
         this.world.addEntity(new ExperienceOrbEntity(this.world, this.getPosX(), this.getPosY() + 0.5D, this.getPosZ(), i));
      }

   }

   public int getPlayerReputation(PlayerEntity player) {
      return this.gossip.getReputation(player.getUniqueID(), (p_223103_0_) -> {
         return true;
      });
   }

   public void setOffers(MerchantOffers offersIn) {
      this.offers = offersIn;
   }

   private boolean canLevelUp() {
      int i = this.getVillagerData().getLevel();
      return VillagerData.canLevelUp(i) && this.xp >= VillagerData.getExperienceNext(i);
   }

   private void levelUp() {
      this.setVillagerData(this.getVillagerData().withLevel(this.getVillagerData().getLevel() + 1));
      this.populateTradeData();
   }

   protected ITextComponent getProfessionName() {
      net.minecraft.util.ResourceLocation profName = this.getVillagerData().getProfession().getRegistryName();
      return new TranslationTextComponent(this.getType().getTranslationKey() + '.' + (!"minecraft".equals(profName.getNamespace()) ? profName.getNamespace() + '.' : "") + profName.getPath());
   }

   /**
    * Handler for {@link World#setEntityState}
    */
   @OnlyIn(Dist.CLIENT)
   public void handleStatusUpdate(byte id) {
      if (id == 12) {
         this.spawnParticles(ParticleTypes.HEART);
      } else if (id == 13) {
         this.spawnParticles(ParticleTypes.ANGRY_VILLAGER);
      } else if (id == 14) {
         this.spawnParticles(ParticleTypes.HAPPY_VILLAGER);
      } else if (id == 42) {
         this.spawnParticles(ParticleTypes.SPLASH);
      } else {
         super.handleStatusUpdate(id);
      }

   }

   @Nullable
   public ILivingEntityData onInitialSpawn(IServerWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag) {
	   this.setVillagerData(this.getVillagerData().withType(VillagerType.func_242371_a(worldIn.func_242406_i(this.getPosition()))));
	   this.setVillagerData(this.getVillagerData().withProfession(VillagerProfession.NONE));
	   return super.onInitialSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
   }

   protected void populateTradeData() {
      VillagerData villagerdata = this.getVillagerData();
      Int2ObjectMap<VillagerTrades.ITrade[]> int2objectmap = VillagerTrades.VILLAGER_DEFAULT_TRADES.get(villagerdata.getProfession());
      if (int2objectmap != null && !int2objectmap.isEmpty()) {
         VillagerTrades.ITrade[] avillagertrades$itrade = int2objectmap.get(villagerdata.getLevel());
         if (avillagertrades$itrade != null) {
            MerchantOffers merchantoffers = this.getOffers();
            this.addTrades(merchantoffers, avillagertrades$itrade, 2);
         }
      }
   }

   public void func_213746_a(TradeableFriendEntity villager, long gameTime) {
      if ((gameTime < this.lastGossipTime || gameTime >= this.lastGossipTime + 1200L) && (gameTime < villager.lastGossipTime || gameTime >= villager.lastGossipTime + 1200L)) {
         this.gossip.transferFrom(villager.gossip, this.rand, 10);
         this.lastGossipTime = gameTime;
         villager.lastGossipTime = gameTime;
      }
   }

   private void tickGossip() {
      long i = this.world.getGameTime();
      if (this.lastGossipDecay == 0L) {
         this.lastGossipDecay = i;
      } else if (i >= this.lastGossipDecay + 24000L) {
         this.gossip.tick();
         this.lastGossipDecay = i;
      }
   }

   @SuppressWarnings("deprecation")
   @Nullable
   private BlockPos getValidGolemSpawnPosition(BlockPos p_241433_1_, double p_241433_2_, double p_241433_4_) {
      BlockPos blockpos = p_241433_1_.add(p_241433_2_, 6.0D, p_241433_4_);
      BlockState blockstate = this.world.getBlockState(blockpos);

      for(int j = 6; j >= -6; --j) {
         BlockPos blockpos1 = blockpos;
         BlockState blockstate1 = blockstate;
         blockpos = blockpos.down();
         blockstate = this.world.getBlockState(blockpos);
         if ((blockstate1.isAir() || blockstate1.getMaterial().isLiquid()) && blockstate.getMaterial().isOpaque()) {
            return blockpos1;
         }
      }

      return null;
   }

   @Override
   public void updateReputation(IReputationType type, Entity target) {
      if (type == IReputationType.ZOMBIE_VILLAGER_CURED) {
         this.gossip.add(target.getUniqueID(), GossipType.MAJOR_POSITIVE, 20);
         this.gossip.add(target.getUniqueID(), GossipType.MINOR_POSITIVE, 25);
      } else if (type == IReputationType.TRADE) {
         this.gossip.add(target.getUniqueID(), GossipType.TRADING, 2);
      } else if (type == IReputationType.VILLAGER_HURT) {
         this.gossip.add(target.getUniqueID(), GossipType.MINOR_NEGATIVE, 25);
      } else if (type == IReputationType.VILLAGER_KILLED) {
         this.gossip.add(target.getUniqueID(), GossipType.MAJOR_NEGATIVE, 25);
      }

   }

   public int getXp() {
      return this.xp;
   }

   public void setXp(int xpIn) {
      this.xp = xpIn;
   }

   private void func_223718_eH() {
      this.func_223719_ex();
      this.restocksToday = 0;
   }

   public GossipManager getGossip() {
      return this.gossip;
   }

   public void func_223716_a(INBT p_223716_1_) {
      this.gossip.read(new Dynamic<>(NBTDynamicOps.INSTANCE, p_223716_1_));
   }

   protected void sendDebugPackets() {
      super.sendDebugPackets();
      DebugPacketSender.sendLivingEntity(this);
   }

   public void startSleeping(BlockPos pos) {
      super.startSleeping(pos);
      this.brain.setMemory(MemoryModuleType.LAST_SLEPT, this.world.getGameTime());
      this.brain.removeMemory(MemoryModuleType.WALK_TARGET);
      this.brain.removeMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
   }

   public void wakeUp() {
      super.wakeUp();
      this.brain.setMemory(MemoryModuleType.LAST_WOKEN, this.world.getGameTime());
   }
}