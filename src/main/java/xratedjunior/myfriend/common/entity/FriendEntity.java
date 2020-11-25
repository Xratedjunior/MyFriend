/*
 * Reference Classes: TameableEntity, AbstractVillagerEntity, VillagerEntity
 */
package xratedjunior.myfriend.common.entity;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.common.collect.Sets;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.CreatureAttribute;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.INPC;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.Pose;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.merchant.IMerchant;
import net.minecraft.entity.merchant.IReputationTracking;
import net.minecraft.entity.merchant.IReputationType;
import net.minecraft.entity.merchant.villager.VillagerData;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.entity.merchant.villager.VillagerTrades;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.entity.villager.IVillagerDataHolder;
import net.minecraft.entity.villager.VillagerType;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MerchantOffer;
import net.minecraft.item.MerchantOffers;
import net.minecraft.item.ShootableItem;
import net.minecraft.item.TieredItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.network.DebugPacketSender;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.management.PreYggdrasilConverter;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.village.GossipManager;
import net.minecraft.village.GossipType;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.GameRules;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xratedjunior.myfriend.common.entity.ai.attribute.MFCreatureAttribute;
import xratedjunior.myfriend.common.entity.ai.goal.FollowFriendGoal;
import xratedjunior.myfriend.common.entity.ai.goal.FriendHurtByTargetGoal;
import xratedjunior.myfriend.common.entity.ai.goal.FriendHurtTargetGoal;
import xratedjunior.myfriend.common.entity.ai.goal.PassiveRangedBowAttackGoal;
import xratedjunior.myfriend.common.entity.ai.goal.WaitForFriendGoal;
import xratedjunior.myfriend.common.entity.trading.MFTradingProfession;
import xratedjunior.myfriend.init.MFEntityRegistryHandler;

public abstract class FriendEntity extends AgeableEntity implements INPC, IMerchant, IReputationTracking, IVillagerDataHolder, IRangedAttackMob {
	private final PassiveRangedBowAttackGoal<FriendEntity> bowGoal = new PassiveRangedBowAttackGoal<>(this, 1.0D, 20, 15.0F);
	private final MeleeAttackGoal meleeGoal = new MeleeAttackGoal(this, 1.2D, false) {
		
		/**
		* Reset the task's internal state. Called when this task is interrupted by another one
		*/
		@Override
		public void resetTask() {
			super.resetTask();
			FriendEntity.this.setAggroed(false);
		}
	
		/**
		* Execute a one shot task or start executing a continuous task
		*/
		@Override
		public void startExecuting() {
			super.startExecuting();
			FriendEntity.this.setAggroed(true);
		}
	};
	protected static final DataParameter<Byte> FRIEND = EntityDataManager.createKey(FriendEntity.class, DataSerializers.BYTE);
	protected static final DataParameter<Optional<UUID>> FRIEND_UNIQUE_ID = EntityDataManager.createKey(FriendEntity.class, DataSerializers.OPTIONAL_UNIQUE_ID);
	private static final DataParameter<Integer> SHAKE_HEAD_TICKS = EntityDataManager.createKey(FriendEntity.class, DataSerializers.VARINT);
	private static final DataParameter<VillagerData> FRIEND_DATA = EntityDataManager.createKey(FriendEntity.class, DataSerializers.VILLAGER_DATA);
	private final NonNullList<ItemStack> handItems = NonNullList.withSize(2, ItemStack.EMPTY);
	private final NonNullList<ItemStack> armorItems = NonNullList.withSize(4, ItemStack.EMPTY);
	private final Inventory friendInventory = new Inventory(8);
	private final GossipManager gossip = new GossipManager();
	protected static final byte notTamed = 6;
	protected static final byte tamed = 7;
	private boolean isSitting;
	private boolean leveledUp;
	private int xp;
	private int timeUntilReset;
	private long lastRestockDayTime;
	private long lastRestock;
	private int restocksToday;

	@Nullable
	private PlayerEntity customer;
	@Nullable
	private PlayerEntity previousCustomer;
	@Nullable
	protected MerchantOffers offers;
	
	private final String ownerString = "Owner";
	private final String sittingString = "Sitting";
	private final String offersString = "Offers";
	private final String inventoryString = "Inventory";
	private final String villagerDataString = "VillagerData";
	private final String gossipsString = "Gossips";
	private final String xpString = "Xp";
	private final String lastRestockString = "LastRestock";
	private final String restocksTodayString = "RestocksToday";
	   
	protected FriendEntity(EntityType<? extends FriendEntity> type, World worldIn) {
		super(type, worldIn);
		this.setupTamedAI();
		this.setPathPriority(PathNodeType.DANGER_FIRE, 16.0F);
		this.setPathPriority(PathNodeType.DAMAGE_FIRE, -1.0F);
		this.setTamed(false);
		this.setCombatTask();
		this.experienceValue = 5;
	}
	
	/*********************************************************** NBT ********************************************************/

	@Override
	protected void sendDebugPackets() {
		super.sendDebugPackets();
		DebugPacketSender.sendLivingEntity(this);
	}
	
	@Override
	protected void registerData() {
		super.registerData();
		this.dataManager.register(FRIEND, (byte)0);
		this.dataManager.register(FRIEND_UNIQUE_ID, Optional.empty());
		this.dataManager.register(SHAKE_HEAD_TICKS, 0);
		this.dataManager.register(FRIEND_DATA, new VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 1));
	}
	
	/**
	 * (abstract) Protected helper method to read subclass entity data from NBT.
	 */
	@Override
	public void readAdditional(CompoundNBT compound) {
		super.readAdditional(compound);
		this.readTameableData(compound);
		this.readTradeableData(compound);
		this.readAdvancedTradingData(compound);
		this.setCombatTask();
	}
	
	private void readTameableData(CompoundNBT compound) {
		UUID uuid;
		if (compound.hasUniqueId(this.ownerString)) {
			uuid = compound.getUniqueId(this.ownerString);
		} else {
			String friendName = compound.getString(this.ownerString);
			uuid = PreYggdrasilConverter.convertMobOwnerIfNeeded(this.getServer(), friendName);
		}
	
		if (uuid != null) {
			try {
				this.setOwnerId(uuid);
				this.setTamed(true);
			} catch (Throwable throwable) {
				this.setTamed(false);
			}
		}

		this.isSitting = compound.getBoolean(this.sittingString);
		this.setSleeping(this.isSitting);
	}
	
	private void readTradeableData(CompoundNBT compound) {
		if (compound.contains(this.offersString, 10)) {
			this.offers = new MerchantOffers(compound.getCompound(this.offersString));
		}
		this.friendInventory.read(compound.getList(this.inventoryString, 10));
	}
	
	private void readAdvancedTradingData(CompoundNBT compound) {
		if (compound.contains(this.villagerDataString, 10)) {
			DataResult<VillagerData> dataresult = VillagerData.CODEC.parse(new Dynamic<>(NBTDynamicOps.INSTANCE, compound.get(this.villagerDataString)));
			dataresult.resultOrPartial(LOGGER::error).ifPresent(this::setVillagerData);
		}

		if (compound.contains(this.offersString, 10)) {
			this.offers = new MerchantOffers(compound.getCompound(this.offersString));
		}

		ListNBT listnbt = compound.getList(this.gossipsString, 10);
		this.gossip.read(new Dynamic<>(NBTDynamicOps.INSTANCE, listnbt));
		if (compound.contains(this.xpString, 3)) {
			this.xp = compound.getInt(this.xpString);
		}

		this.lastRestock = compound.getLong(this.lastRestockString);
		this.setCanPickUpLoot(true);
		this.restocksToday = compound.getInt(this.restocksTodayString);
	}
	
	public void setVillagerData(VillagerData data) {
		VillagerData villagerdata = this.getVillagerData();
		if (villagerdata.getProfession() != data.getProfession()) {
			this.offers = null;
		}

		this.dataManager.set(FRIEND_DATA, data);
	}

	@Override
	public VillagerData getVillagerData() {
		return this.dataManager.get(FRIEND_DATA);
	}
	
	@Override
	public void writeAdditional(CompoundNBT compound) {
		super.writeAdditional(compound);
		this.writeTameableData(compound);
		this.writeTradeableData(compound);
		this.writeAdvancedTradingData(compound);
	}
	
	private void writeTameableData(CompoundNBT compound) {
		if (this.getOwnerId() != null) {
			compound.putUniqueId(this.ownerString, this.getOwnerId());
		}
		compound.putBoolean(this.sittingString, this.isSitting);
	}
	
	private void writeTradeableData(CompoundNBT compound) {
		MerchantOffers merchantoffers = this.getOffers();
		if (!merchantoffers.isEmpty()) {
			compound.put(this.offersString, merchantoffers.write());
		}
		compound.put(this.inventoryString, this.friendInventory.write());
	}
	
	private void writeAdvancedTradingData(CompoundNBT compound) {
		VillagerData.CODEC.encodeStart(NBTDynamicOps.INSTANCE, this.getVillagerData()).resultOrPartial(LOGGER::error).ifPresent((data) -> {
			compound.put(this.villagerDataString, data);
		});
		compound.put(this.gossipsString, this.gossip.write(NBTDynamicOps.INSTANCE).getValue());
		compound.putInt(this.xpString, this.xp);
		compound.putLong(this.lastRestockString, this.lastRestock);
		compound.putInt(this.restocksTodayString, this.restocksToday);
	}
	
	/*********************************************************** Spawn ********************************************************/
	
	@Override
	public ILivingEntityData onInitialSpawn(IServerWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag) {
		if (spawnDataIn == null) {
			spawnDataIn = new AgeableEntity.AgeableData(false);
		}
		this.setVillagerData(this.getVillagerData().withType(VillagerType.func_242371_a(worldIn.func_242406_i(this.getPosition()))));
		this.setVillagerData(this.getVillagerData().withProfession(VillagerProfession.NONE));
		
//		this.enablePersistence();
		this.setCombatTask();
//		this.setCanPickUpLoot(true);
		
		return super.onInitialSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
	}
	
	@Override
	public boolean canDespawn(double distanceToClosestPlayer) {
		return false;
	}
	
	/*********************************************************** Attributes ********************************************************/

	public static AttributeModifierMap.MutableAttribute abstractFriendAttributes() {
		return MobEntity.func_233666_p_().createMutableAttribute(Attributes.MOVEMENT_SPEED, (double)0.3F).createMutableAttribute(Attributes.MAX_HEALTH, 20.0D).createMutableAttribute(Attributes.ATTACK_DAMAGE, 4.0D);
	}
	
	@Override
	public CreatureAttribute getCreatureAttribute() {
		return MFCreatureAttribute.FRIEND;
	}
	
	@Override
	protected float getStandingEyeHeight(Pose poseIn, EntitySize sizeIn) {
		return this.isChild() ? sizeIn.height * 0.7F : sizeIn.height * 0.95F;
	}
	
	/**
	 * Returns the Y Offset of this entity.
	 */
	@Override
	public double getYOffset() {
		return -0.6D;
	}
	
	@Override
	public boolean canBeLeashedTo(PlayerEntity player) {
		return false;
	}
	
//	@OnlyIn(Dist.CLIENT)
//	public Vector3d getLeashPosition(float partialTicks) {
//		float f = MathHelper.lerp(partialTicks, this.prevRenderYawOffset, this.renderYawOffset) * ((float)Math.PI / 180F);
//		Vector3d vector3d = new Vector3d(0.0D, this.getBoundingBox().getYSize() - 1.0D, 0.2D);
//		return this.func_242282_l(partialTicks).add(vector3d.rotateYaw(-f));
//	}
	
	/*********************************************************** Goals ********************************************************/

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new SwimGoal(this));
		this.goalSelector.addGoal(2, new WaitForFriendGoal(this));
		this.goalSelector.addGoal(3, new FollowFriendGoal(this, 1.2D, 8.0F, 1.5F, false));
		this.goalSelector.addGoal(4, new LookAtGoal(this, PlayerEntity.class, 4.0F));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomWalkingGoal(this, 1.0D));
		this.goalSelector.addGoal(6, new LookAtGoal(this, PlayerEntity.class, 8.0F));
		this.goalSelector.addGoal(6, new LookRandomlyGoal(this));
		this.targetSelector.addGoal(1, new FriendHurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new FriendHurtTargetGoal(this));
		this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
	}
	
	/*********************************************************** Baby ********************************************************/

	/*
	 * Breed this entity
	 */
	@Nullable
	@Override
	public FriendEntity func_241840_a(ServerWorld serverWorld, AgeableEntity parent) {
		FriendEntity babyEntity = this.createChild(serverWorld);
		return babyEntity;
	}
	
	protected FriendEntity createChild(ServerWorld serverWorld) {
		return null;
	}
	
	/*********************************************************** Movement ********************************************************/
	
	@Override
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
	
	/*********************************************************** Attack ********************************************************/

	/**
	 * sets this entity's combat AI.
	 */
	public void setCombatTask() {
		if (this.world != null && !this.world.isRemote) {
			this.goalSelector.removeGoal(this.meleeGoal);
			this.goalSelector.removeGoal(this.bowGoal);
			ItemStack itemstack = this.getHeldItem(ProjectileHelper.getHandWith(this, Items.BOW));
			if (itemstack.getItem() instanceof net.minecraft.item.BowItem) {
				int i = 20;
				if (this.world.getDifficulty() != Difficulty.HARD) {
					i = 40;
				}
				this.bowGoal.setAttackCooldown(i);
				this.goalSelector.addGoal(4, this.bowGoal);
			} else {
				this.goalSelector.addGoal(4, this.meleeGoal);
			}
		}
	}
	
	/**
	 * Attack the specified entity using a ranged attack.
	 */
	@Override
	public void attackEntityWithRangedAttack(LivingEntity target, float distanceFactor) {
		ItemStack itemstack = this.findAmmo(this.getHeldItem(ProjectileHelper.getHandWith(this, Items.BOW)));
		AbstractArrowEntity abstractarrowentity = this.fireArrow(itemstack, distanceFactor);
		if (this.getHeldItemMainhand().getItem() instanceof net.minecraft.item.BowItem)
			abstractarrowentity = ((net.minecraft.item.BowItem)this.getHeldItemMainhand().getItem()).customArrow(abstractarrowentity);
		double dx = target.getPosX() - this.getPosX();
		//double d1 = target.getBoundingBox().minY + (double)(target.getHeight() / 3.0F) - abstractarrowentity.getPosY();
		//double d1 = target.getPosYHeight(0.3333333333333333D) - abstractarrowentity.getPosY();
		double targety = target.getPosYEye();
		double dy = targety - this.getPosYEye();
		double dz = target.getPosZ() - this.getPosZ();
		float velocity = 3.0F; //Default: 1.6F
		abstractarrowentity.shoot(dx, dy, dz, velocity, (float)(14 - this.world.getDifficulty().getId() * 4));
		this.playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0F, 1.0F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
		this.world.addEntity(abstractarrowentity);
	}
   
	/**
	* Fires an arrow
	*/
	protected AbstractArrowEntity fireArrow(ItemStack arrowStack, float distanceFactor) {
		return ProjectileHelper.fireArrow(this, arrowStack, distanceFactor);
	}
	
	@Override
	public boolean canAttack(LivingEntity target) {
		return this.isOwner(target) ? false : super.canAttack(target);
	}
	
	public boolean shouldAttackEntity(LivingEntity target, LivingEntity owner) {
		return true;
	}
	
	@Override
	public Team getTeam() {
		if (this.isTamed()) {
			LivingEntity livingentity = this.getOwner();
			if (livingentity != null) {
				return livingentity.getTeam();
			}
		}
		return super.getTeam();
	}
	
	/**
	 * Returns whether this Entity is on the same team as the given Entity.
	 */
	@Override
	public boolean isOnSameTeam(Entity entityIn) {
		if (this.isTamed()) {
			LivingEntity livingentity = this.getOwner();
			if (entityIn == livingentity) {
				return true;
			}
	
			if (livingentity != null) {
				return livingentity.isOnSameTeam(entityIn);
			}
		}
		return super.isOnSameTeam(entityIn);
	}
	
	/*********************************************************** Tick ********************************************************/

	/**
	 * Called to update the entity's position/logic.
	 */
	public void tick() {
		super.tick();
		if (this.getShakeHeadTicks() > 0) {
			this.setShakeHeadTicks(this.getShakeHeadTicks() - 1);
		}
	}
	
	/*********************************************************** AI ********************************************************/

	@Override
	protected void updateAITasks() {
//		this.world.getProfiler().startSection("villagerBrain");
//		this.world.getProfiler().endSection();

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

//		if (!this.isAIDisabled() && this.rand.nextInt(100) == 0) {
//			Raid raid = ((ServerWorld)this.world).findRaid(this.getPosition());
//			if (raid != null && raid.isActive() && !raid.isOver()) {
//				this.world.setEntityState(this, (byte)42);
//			}
//		}

		if (this.getVillagerData().getProfession() == VillagerProfession.NONE && this.hasCustomer()) {
			this.resetCustomer();
		}

		super.updateAITasks();
	}
	
	/*********************************************************** Make Friend ********************************************************/
	
	public boolean isTamed() {
		return (this.dataManager.get(FRIEND) & 4) != 0;
	}
	
	public void setTamed(boolean tamed) {
	   byte b0 = this.dataManager.get(FRIEND);
	   if (tamed) {
	      this.dataManager.set(FRIEND, (byte)(b0 | 4));
	      this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0D);
	      this.setHealth(20.0F);
	      this.setVillagerData(this.getVillagerData().withProfession(MFTradingProfession.FRIEND).withLevel(1));
	   } else {
	      this.dataManager.set(FRIEND, (byte)(b0 & -5));
	      this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(8.0D);
	   }
	
	   this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4.0D);
	   this.setupTamedAI();
	}
	
	protected void setupTamedAI() {
	}
	
	public boolean isEntitySleeping() {
	   return (this.dataManager.get(FRIEND) & 1) != 0;
	}
	
	public void setSleeping(boolean p_233686_1_) {
	   byte b0 = this.dataManager.get(FRIEND);
	   if (p_233686_1_) {
	      this.dataManager.set(FRIEND, (byte)(b0 | 1));
	   } else {
	      this.dataManager.set(FRIEND, (byte)(b0 & -2));
	   }
	}
	
	public boolean isSitting() {
		return this.isSitting;
	}
	
	public void setIsSitting(boolean sit) {
		this.isSitting = sit;
	}
	
	@Nullable
	public UUID getOwnerId() {
		return this.dataManager.get(FRIEND_UNIQUE_ID).orElse((UUID)null);
	}
	
	public void setOwnerId(@Nullable UUID uuid) {
		this.dataManager.set(FRIEND_UNIQUE_ID, Optional.ofNullable(uuid));
	}
	
	public void setTamedBy(PlayerEntity player) {
		this.setTamed(true);
		this.setOwnerId(player.getUniqueID());
		if (player instanceof ServerPlayerEntity) {
			//CriteriaTriggers.TAME_ANIMAL.trigger((ServerPlayerEntity)player, this);
		}
	}
	
	@Nullable
	public LivingEntity getOwner() {
		try {
			UUID uuid = this.getOwnerId();
			return uuid == null ? null : this.world.getPlayerByUuid(uuid);
		} catch (IllegalArgumentException illegalargumentexception) {
			return null;
		}
	}
	
	public boolean isOwner(LivingEntity entityIn) {
		return entityIn == this.getOwner();
	}
	
	/*********************************************************** Inventory ********************************************************/

	public Inventory getFriendInventory() {
		return this.friendInventory;
	}
	
	@Override
	public boolean replaceItemInInventory(int inventorySlot, ItemStack itemStackIn) {
		if (super.replaceItemInInventory(inventorySlot, itemStackIn)) {
			return true;
		} else {
			int i = inventorySlot - 300;
			if (i >= 0 && i < this.friendInventory.getSizeInventory()) {
				this.friendInventory.setInventorySlotContents(i, itemStackIn);
				return true;
			} else {
				return false;
			}
		}
	}
	
	/*********************************************************** Interaction ********************************************************/

	/**
	 * Checks if the parameter is an item which this animal can be fed to heal it.
	 */
	private boolean isHealingItem(ItemStack stack) {
		Item item = stack.getItem();
		return item.isFood() && item.getFood().isMeat();
	}

	@Override
	public Iterable<ItemStack> getHeldEquipment() {
		return this.handItems;
	}

	@Override
	public Iterable<ItemStack> getArmorInventoryList() {
		return this.armorItems;
	}
		  
	@Override
	public ItemStack getItemStackFromSlot(EquipmentSlotType slotIn) {
		switch(slotIn.getSlotType()) {
		case HAND:
			return this.handItems.get(slotIn.getIndex());
		case ARMOR:
			return this.armorItems.get(slotIn.getIndex());
		default:
			return ItemStack.EMPTY;
		}
	}

	   @Override
	   public void setItemStackToSlot(EquipmentSlotType slotIn, ItemStack stack) {
	      switch(slotIn.getSlotType()) {
	      case HAND:
	         this.playEquipSound(stack);
	         this.handItems.set(slotIn.getIndex(), stack);
	         break;
	      case ARMOR:
	         this.playEquipSound(stack);
	         this.armorItems.set(slotIn.getIndex(), stack);
	      }
	      super.setItemStackToSlot(slotIn, stack);
	      if (!this.world.isRemote) {
			this.setCombatTask();
	      }
	   }
	   
		/**
		 * Applies the given player interaction to this Entity.
		 */
		@Override
		public ActionResultType applyPlayerInteraction(PlayerEntity player, Vector3d vec, Hand hand) {
			ItemStack itemstack = player.getHeldItem(hand);
			Item item = itemstack.getItem();
		      if (this.world.isRemote) {
		         boolean flag = this.isOwner(player) || this.isTamed() || item == Items.COOKIE && !this.isTamed();
		         return flag ? ActionResultType.CONSUME : ActionResultType.PASS;
		      } else {
			      EquipmentSlotType equipmentslottype = MobEntity.getSlotForItemStack(itemstack);
	              EquipmentSlotType equipmentslottype1 = this.getClickedSlot(vec);

//	              ActionResultType actionresulttype1 = this.processInitialInteract(player, hand);
//	              if(actionresulttype1.isSuccessOrConsume()) {
//	            	  return ActionResultType.func_233537_a_(this.world.isRemote);
//	              }

		         if (this.isTamed()) {
		            if (this.isHealingItem(itemstack) && this.getHealth() < this.getMaxHealth()) {
		               if (!player.abilities.isCreativeMode) {
		                  itemstack.shrink(1);
		               }
		               this.heal((float)item.getFood().getHealing());
		               return ActionResultType.SUCCESS;
		            }

		            if (!(item instanceof DyeItem) && equipmentslottype.getSlotType() != EquipmentSlotType.Group.ARMOR && !(item instanceof TieredItem) && !(item instanceof ShootableItem) && (itemstack.isEmpty() && !this.hasItemInSlot(equipmentslottype1))) {
		            	ActionResultType actionresulttype = this.func_230254_b_(player, hand);
		               //WAIT
		               if ((!actionresulttype.isSuccessOrConsume() || this.isChild()) && this.isOwner(player) && Screen.hasShiftDown()) {
		                  this.setIsSitting(!this.isSitting());
		                  this.isJumping = false;
		                  this.navigator.clearPath();
		                  this.setAttackTarget((LivingEntity)null);
		                  return ActionResultType.SUCCESS;
		               }
		               return actionresulttype;
		            }
		         } else if (item == Items.COOKIE) {
		            if (!player.abilities.isCreativeMode) {
		               itemstack.shrink(1);
		            }

		            if (this.rand.nextInt(3) == 0 /*&& !net.minecraftforge.event.ForgeEventFactory.onAnimalTame(this, player)*/) {
		               this.setTamedBy(player);
		               this.navigator.clearPath();
		               this.setAttackTarget((LivingEntity)null);
		               this.setIsSitting(true);
		               this.world.setEntityState(this, tamed);
		            } else {
		               this.world.setEntityState(this, notTamed);
		            }

		            return ActionResultType.SUCCESS;
		         }
				   
			      if ((itemstack.getItem() != Items.NAME_TAG && Screen.hasShiftDown())) {
			    	  super.func_230254_b_(player, hand);
			         if (player.isSpectator()) {
			            return ActionResultType.SUCCESS;
			         } else if (player.world.isRemote) {
			            return ActionResultType.CONSUME;
			         } else {
			            if (itemstack.isEmpty()) {
			               if (this.hasItemInSlot(equipmentslottype1) && this.exchangeItem(player, equipmentslottype1, itemstack, hand)) {
			                  return ActionResultType.SUCCESS;
			               }
			            } else {
			               if (this.exchangeItem(player, equipmentslottype, itemstack, hand) && itemstack.getItem() != Items.CROSSBOW) {
			                  return ActionResultType.SUCCESS;
			               }
			            }
			            return ActionResultType.PASS;
			         }
			      } else {
			    	  super.func_230254_b_(player, hand);
			    	  return ActionResultType.PASS;
			      }
		      }
	   }

	   private EquipmentSlotType getClickedSlot(Vector3d v3d) {
	      EquipmentSlotType equipmentslottype = EquipmentSlotType.MAINHAND;
	      double d0 = v3d.y;
	      EquipmentSlotType equipmentslottype1 = EquipmentSlotType.FEET;
	      if (d0 >= 0.1D && d0 < 0.1D + (0.45D) && this.hasItemInSlot(equipmentslottype1)) {
	         equipmentslottype = EquipmentSlotType.FEET;
	      } else if (d0 >= 0.9D + (0.0D) && d0 < 0.9D + (0.7D) && this.hasItemInSlot(EquipmentSlotType.CHEST)) {
	         equipmentslottype = EquipmentSlotType.CHEST;
	      } else if (d0 >= 0.4D && d0 < 0.4D + (0.8D) && this.hasItemInSlot(EquipmentSlotType.LEGS)) {
	         equipmentslottype = EquipmentSlotType.LEGS;
	      } else if (d0 >= 1.6D && this.hasItemInSlot(EquipmentSlotType.HEAD)) {
	         equipmentslottype = EquipmentSlotType.HEAD;
	      } else if (!this.hasItemInSlot(EquipmentSlotType.MAINHAND) && this.hasItemInSlot(EquipmentSlotType.OFFHAND)) {
	         equipmentslottype = EquipmentSlotType.OFFHAND;
	      }

	      return equipmentslottype;
	   }
	   
		private boolean exchangeItem(PlayerEntity player, EquipmentSlotType equiptmentSlotType, ItemStack itemStack, Hand hand) {
			ItemStack itemstack = this.getItemStackFromSlot(equiptmentSlotType);
			//Set iItem to slot from creative
			if (player.abilities.isCreativeMode && itemstack.isEmpty() && !itemStack.isEmpty()) {
				ItemStack itemstack2 = itemStack.copy();
				itemstack2.setCount(1);
				this.setItemStackToSlot(equiptmentSlotType, itemstack2);
				return true;
	      } 
	      //Set Item to slot from survival
	      else if (!itemStack.isEmpty() && itemStack.getCount() > 1) {
	    	  //Cancel if already an Item
	         if (!itemstack.isEmpty()) {
	            return false;
	         } 
	         //Set Item to slot
	         else {
	            ItemStack itemstack1 = itemStack.copy();
	            itemstack1.setCount(1);
	            this.setItemStackToSlot(equiptmentSlotType, itemstack1);
	            itemStack.shrink(1);
	            return true;
	         }
	      } 
	      //Get Item from friend
	      else {
	         this.setItemStackToSlot(equiptmentSlotType, itemStack);
	         player.setHeldItem(hand, itemstack);
	         return true;
	      }
	   }
	
	/*********************************************************** Trading Interaction ********************************************************/

	@Override
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

//					player.addStat(Stats.TALKED_TO_VILLAGER);
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
	
	private void displayMerchantGui(PlayerEntity player) {
		this.recalculateSpecialPricesFor(player);
		this.setCustomer(player);
		this.openMerchantContainer(player, this.getDisplayName(), this.getVillagerData().getLevel());
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
	
	public int getPlayerReputation(PlayerEntity player) {
		return this.gossip.getReputation(player.getUniqueID(), (p_223103_0_) -> {
			return true;
		});
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
	
	@Override
	public void onTrade(MerchantOffer offer) {
		offer.increaseUses();
		this.livingSoundTime = -this.getTalkInterval();
		this.onVillagerTrade(offer);
		if (this.customer instanceof ServerPlayerEntity) {
			//CriteriaTriggers.VILLAGER_TRADE.func_215114_a((ServerPlayerEntity)this.customer, this, offer.getSellingStack());
		}
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
	
	/*********************************************************** Trading Restock ********************************************************/

	@Override
	public boolean canRestockTrades() {
		return true;
	}
	
	/*
	 * TODO SpawnGolemTask
	 */
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

	private boolean canRestock() {
		return this.restocksToday == 0 || this.restocksToday < 2 && this.world.getGameTime() > this.lastRestock + 2400L;
	}

	/*
	 * TODO SpawnGolemTask
	 */
	public boolean canResetStock() {
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
		return this.canRestock() && this.hasUsedOffer();
	}
	
	private void func_223718_eH() {
		this.resetOffersAndAdjustForDemand();
		this.restocksToday = 0;
	}
	
	private void resetOffersAndAdjustForDemand() {
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
	
	/*********************************************************** Trading Framework ********************************************************/

	@Override
	protected ITextComponent getProfessionName() {
		net.minecraft.util.ResourceLocation profName = this.getVillagerData().getProfession().getRegistryName();
		return new TranslationTextComponent(this.getType().getTranslationKey() + '.' + (!"minecraft".equals(profName.getNamespace()) ? profName.getNamespace() + '.' : "") + profName.getPath());
	}
	
	@Override
	public World getWorld() {
		return this.world;
	}
	
	@Nullable
	@Override
	public Entity changeDimension(ServerWorld p_241206_1_, net.minecraftforge.common.util.ITeleporter teleporter) {
		this.resetCustomer();
		return super.changeDimension(p_241206_1_, teleporter);
	}
		
	@Override
	public void setCustomer(@Nullable PlayerEntity player) {
		boolean flag = this.getCustomer() != null && player == null;
		this.customer = player;
		if (flag) {
			this.resetCustomer();
		}
	}

	@Nullable
	@Override
	public PlayerEntity getCustomer() {
		return this.customer;
	}

	public boolean hasCustomer() {
		return this.customer != null;
	}
	
	protected void resetCustomer() {
		this.setCustomer((PlayerEntity)null);
		this.resetAllSpecialPrices();
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

	public void setOffers(MerchantOffers offersIn) {
		this.offers = offersIn;
	}
	
	@Override
	public MerchantOffers getOffers() {
		if (this.offers == null) {
			this.offers = new MerchantOffers();
			this.populateTradeData();
		}
		return this.offers;
	}
	
	private void resetAllSpecialPrices() {
		for(MerchantOffer merchantoffer : this.getOffers()) {
			merchantoffer.resetSpecialPrice();
		}
	}
		
	@OnlyIn(Dist.CLIENT)
	@Override
	public void setClientSideOffers(@Nullable MerchantOffers offers) {
	}
	
	@Override
	public int getXp() {
		return this.xp;
	}
	
	@Override
	public void setXP(int xpIn) {
		this.xp = xpIn;
	}
	
	@Override
	public boolean hasXPBar() {
		return true;
	}
	
	private boolean canLevelUp() {
		int level = this.getVillagerData().getLevel();
		return VillagerData.canLevelUp(level) && this.xp >= VillagerData.getExperienceNext(level);
	}

	private void levelUp() {
		this.setVillagerData(this.getVillagerData().withLevel(this.getVillagerData().getLevel() + 1));
		this.populateTradeData();
	}
	
	/**
	 * add limites numbers of trades to the given MerchantOffers
	 */
	protected void addTrades(MerchantOffers givenMerchantOffers, VillagerTrades.ITrade[] newTrades, int maxNumbers) {
		Set<Integer> set = Sets.newHashSet();
		if (newTrades.length > maxNumbers) {
			while(set.size() < maxNumbers) {
				set.add(this.rand.nextInt(newTrades.length));
			}
		} else {
			for(int trade = 0; trade < newTrades.length; ++trade) {
				set.add(trade);
			}
		}

		for(Integer integer : set) {
			VillagerTrades.ITrade villagertrades$itrade = newTrades[integer];
			MerchantOffer merchantoffer = villagertrades$itrade.getOffer(this, this.rand);
			if (merchantoffer != null) {
				givenMerchantOffers.add(merchantoffer);
			}
		}
	}
	
	/*********************************************************** Death ********************************************************/
	
	/**
	 * Called when the mob's health reaches 0.
	 */
	@Override
	public void onDeath(DamageSource cause) {
		LOGGER.info("Friend {} died, message: '{}'", this, cause.getDeathMessage(this).getString());
		
		if (!this.world.isRemote && this.world.getGameRules().getBoolean(GameRules.SHOW_DEATH_MESSAGES) && this.getOwner() instanceof ServerPlayerEntity) {
			this.getOwner().sendMessage(this.getCombatTracker().getDeathMessage(), Util.DUMMY_UUID);
		}
		
		Entity entity = cause.getTrueSource();
		if (entity != null) {
//			this.sawMurder(entity);
		}
		
		super.onDeath(cause);
		
		this.resetCustomer();
	}
	
//	private void sawMurder(Entity murderer) {
//		if (this.world instanceof ServerWorld) {
//			Optional<List<LivingEntity>> optional = this.brain.getMemory(MemoryModuleType.VISIBLE_MOBS);
//			if (optional.isPresent()) {
//				ServerWorld serverworld = (ServerWorld)this.world;
//				optional.get().stream().filter((gossipTarget) -> {
//					return gossipTarget instanceof IReputationTracking;
//				}).forEach((gossipTarget) -> {
//					serverworld.updateReputation(IReputationType.VILLAGER_KILLED, murderer, (IReputationTracking)gossipTarget);
//				});
//			}
//		}
//	}
	
	/*********************************************************** Shake Head ********************************************************/

	private void shakeHead() {
		this.setShakeHeadTicks(40);
		if (!this.world.isRemote()) {
			this.playSound(SoundEvents.ENTITY_VILLAGER_NO, this.getSoundVolume(), this.getSoundPitch());
		}
	}
	
	public int getShakeHeadTicks() {
		return this.dataManager.get(SHAKE_HEAD_TICKS);
	}

	public void setShakeHeadTicks(int ticks) {
		this.dataManager.set(SHAKE_HEAD_TICKS, ticks);
	}
	
	/*********************************************************** Client Update ********************************************************/
	
	/**
	 * Handler for {@link World#setEntityState}
	 */
	@Override
	@OnlyIn(Dist.CLIENT)
	public void handleStatusUpdate(byte id) {
		if (id == tamed) {
			this.playTameEffect(true);
		} else if (id == notTamed) {
			this.playTameEffect(false);
		} else if (id == 12) {
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
	
	/**
	 * Play the taming effect, will either be hearts or smoke depending on status
	 */
	@OnlyIn(Dist.CLIENT)
	protected void playTameEffect(boolean play) {
		IParticleData iparticledata = ParticleTypes.HEART;
		if (!play) {
			iparticledata = ParticleTypes.SMOKE;
		}
	   
		for(int i = 0; i < 7; ++i) {
			double d0 = this.rand.nextGaussian() * 0.02D;
			double d1 = this.rand.nextGaussian() * 0.02D;
			double d2 = this.rand.nextGaussian() * 0.02D;
			this.world.addParticle(iparticledata, this.getPosXRandom(1.0D), this.getPosYRandom() + 0.5D, this.getPosZRandom(1.0D), d0, d1, d2);
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	protected void spawnParticles(IParticleData particleData) {
		for(int i = 0; i < 5; ++i) {
			double d0 = this.rand.nextGaussian() * 0.02D;
			double d1 = this.rand.nextGaussian() * 0.02D;
			double d2 = this.rand.nextGaussian() * 0.02D;
			this.world.addParticle(particleData, this.getPosXRandom(1.0D), this.getPosYRandom() + 1.0D, this.getPosZRandom(1.0D), d0, d1, d2);
		}
	}
	
	/*********************************************************** Sounds ********************************************************/

	/**
	 * Notifies the merchant of a possible merchantrecipe being fulfilled or not. Usually, this is just a sound byte
	 * being played depending if the suggested itemstack is not null.
	 */
	@Override
	public void verifySellingItem(ItemStack stack) {
		if (!this.world.isRemote && this.livingSoundTime > -this.getTalkInterval() + 20) {
			this.livingSoundTime = -this.getTalkInterval();
			this.playSound(this.getVillagerYesNoSound(!stack.isEmpty()), this.getSoundVolume(), this.getSoundPitch());
		}
	}
	
	@Override
	public SoundEvent getYesSound() {
		return SoundEvents.ENTITY_VILLAGER_YES;
	}
	
	protected SoundEvent getVillagerYesNoSound(boolean getYesSound) {
		return getYesSound ? SoundEvents.ENTITY_VILLAGER_YES : SoundEvents.ENTITY_VILLAGER_NO;
	}
	
	public void playCelebrateSound() {
		this.playSound(SoundEvents.ENTITY_VILLAGER_CELEBRATE, this.getSoundVolume(), this.getSoundPitch());
	}
	
	@Nullable
	@Override
	protected SoundEvent getAmbientSound() {
		if (this.isSleeping()) {
			return null;
		} else {
			return this.hasCustomer() ? SoundEvents.ENTITY_VILLAGER_TRADE : null;
		}
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
		return super.getHurtSound(damageSourceIn);
	}

	@Override
	protected SoundEvent getDeathSound() {
		return super.getDeathSound();
	}
}