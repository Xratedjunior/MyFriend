package xratedjunior.myfriend.common.entity.backups;

import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.CreatureAttribute;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.Pose;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShootableItem;
import net.minecraft.item.TieredItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.RangedInteger;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.TickRangeConverter;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xratedjunior.myfriend.common.entity.ai.attribute.MFCreatureAttribute;
import xratedjunior.myfriend.common.entity.ai.goal.FollowFriendGoal;
import xratedjunior.myfriend.common.entity.ai.goal.FriendHurtByTargetGoal;
import xratedjunior.myfriend.common.entity.ai.goal.FriendHurtTargetGoal;
import xratedjunior.myfriend.common.entity.ai.goal.PassiveRangedBowAttackGoal;
import xratedjunior.myfriend.common.entity.ai.goal.WaitForFriendGoal;
import xratedjunior.myfriend.common.entity.trading.MFTradingProfession;
import xratedjunior.myfriend.core.MyFriend;

public abstract class DefaultFriendEntity extends TradeableFriendEntity implements IRangedAttackMob {
	private static final DataParameter<Integer> field_234232_bz_ = EntityDataManager.createKey(DefaultFriendEntity.class, DataSerializers.VARINT);

	private boolean isAngry;
	private boolean isWet;
	private boolean isShaking;
	private float timeFriendIsShaking;
	private float prevTimeFriendIsShaking;
	private static final RangedInteger field_234230_bG_ = TickRangeConverter.convertRange(20, 39);
	private UUID field_234231_bH_;
	
	//private final NonNullList<ItemStack> friendInventory = NonNullList.withSize(8, ItemStack.EMPTY);
	//protected final float[] friendInventoryDropChances = new float84];
	
	//private final Inventory friendInventory = new Inventory(8);
	   
	private final PassiveRangedBowAttackGoal<DefaultFriendEntity> bowGoal = new PassiveRangedBowAttackGoal<>(this, 1.0D, 20, 15.0F);
	private final MeleeAttackGoal meleeGoal = new MeleeAttackGoal(this, 1.2D, false) {
		
		/**
		* Reset the task's internal state. Called when this task is interrupted by another one
		*/
		public void resetTask() {
			super.resetTask();
			DefaultFriendEntity.this.setAggroed(false);
		}
	
		/**
		* Execute a one shot task or start executing a continuous task
		*/
		public void startExecuting() {
			super.startExecuting();
			DefaultFriendEntity.this.setAggroed(true);
		}
	};

	public DefaultFriendEntity(EntityType<? extends DefaultFriendEntity> type, World worldIn) {
		super(type, worldIn);
		this.setTamed(false);
		this.setCombatTask();
		this.experienceValue = 5;
	}

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
		
	public static AttributeModifierMap.MutableAttribute abstractFriendAttributes() {
		return MobEntity.func_233666_p_().createMutableAttribute(Attributes.MOVEMENT_SPEED, (double)0.3F).createMutableAttribute(Attributes.MAX_HEALTH, 20.0D).createMutableAttribute(Attributes.ATTACK_DAMAGE, 4.0D);
	}
	
	@Override
	protected void registerData() {
		super.registerData();
		this.dataManager.register(field_234232_bz_, 0);
	}
	
	@Nullable
	@Override
	public ILivingEntityData onInitialSpawn(IServerWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag) {
		spawnDataIn = super.onInitialSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
		this.enablePersistence();
		this.isAngry = false;
		this.setCombatTask();
		this.setCanPickUpLoot(true);
		if (this.getItemStackFromSlot(EquipmentSlotType.HEAD).isEmpty()) {
			LocalDate localdate = LocalDate.now();
			int i = localdate.get(ChronoField.DAY_OF_MONTH);
			int j = localdate.get(ChronoField.MONTH_OF_YEAR);
			if (j == 10 && i == 31 && this.rand.nextFloat() < 0.25F) {
				this.setItemStackToSlot(EquipmentSlotType.HEAD, new ItemStack(this.rand.nextFloat() < 0.1F ? Blocks.JACK_O_LANTERN : Blocks.CARVED_PUMPKIN));
				this.inventoryArmorDropChances[EquipmentSlotType.HEAD.getIndex()] = 0.0F;
			}
		}
		return spawnDataIn;
	}
   
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
	
	/**
	* (abstract) Protected helper method to read subclass entity data from NBT.
	*/
	public void readAdditional(CompoundNBT compound) {
		super.readAdditional(compound);
		this.setCombatTask();
	}
	
	/*
	 * On the bottom
	 */
	/*
	public void setItemStackToSlot(EquipmentSlotType slotIn, ItemStack stack) {
		super.setItemStackToSlot(slotIn, stack);
		if (!this.world.isRemote) {
			this.setCombatTask();
		}
	}
	*/
	   
	protected float getStandingEyeHeight(Pose poseIn, EntitySize sizeIn) {
		return 1.74F;
	}

	/**
	* Returns the Y Offset of this entity.
	*/
	public double getRidingHeight() {
		return -0.6D;
	}
		
	public CreatureAttribute getCreatureAttribute() {
		return MFCreatureAttribute.FRIEND;
	}
		
	/**
	* Returns whether this Entity is on the same team as the given Entity.
	*/
	public boolean isOnSameTeam(Entity entityIn) {
		if (super.isOnSameTeam(entityIn)) {
			return true;
		} else if (entityIn instanceof LivingEntity && ((LivingEntity)entityIn).getCreatureAttribute() == MFCreatureAttribute.FRIEND) {
			return this.getTeam() == null && entityIn.getTeam() == null;
		} else {
			return false;
		}
	}

	/**
	* Called frequently so the entity can update its state every tick as required. For example, zombies and skeletons
	* use this to react to sunlight and start to burn.
	*/
	public void livingTick() {
	   this.updateArmSwingProgress();
	   super.livingTick();
      if (!this.world.isRemote && this.isWet && !this.isShaking && !this.hasPath() && this.onGround) {
         this.isShaking = true;
         this.timeFriendIsShaking = 0.0F;
         this.prevTimeFriendIsShaking = 0.0F;
         this.world.setEntityState(this, (byte)8);
      }
   }

   /**
    * Called to update the entity's position/logic.
    */
   public void tick() {
      super.tick();
      if (this.isAlive()) {
         if (this.isInWaterRainOrBubbleColumn()) {
            this.isWet = true;
            if (this.isShaking && !this.world.isRemote) {
               this.world.setEntityState(this, (byte)56);
               this.func_242326_eZ();
            }
         } else if ((this.isWet || this.isShaking) && this.isShaking) {
            if (this.timeFriendIsShaking == 0.0F) {
               this.playSound(SoundEvents.ENTITY_WOLF_SHAKE, this.getSoundVolume(), (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F);
            }

            this.prevTimeFriendIsShaking = this.timeFriendIsShaking;
            this.timeFriendIsShaking += 0.05F;
            if (this.prevTimeFriendIsShaking >= 2.0F) {
               this.isWet = false;
               this.isShaking = false;
               this.prevTimeFriendIsShaking = 0.0F;
               this.timeFriendIsShaking = 0.0F;
            }

            if (this.timeFriendIsShaking > 0.4F) {
               float f = (float)this.getPosY();
               int i = (int)(MathHelper.sin((this.timeFriendIsShaking - 0.4F) * (float)Math.PI) * 7.0F);
               Vector3d vector3d = this.getMotion();

               for(int j = 0; j < i; ++j) {
                  float f1 = (this.rand.nextFloat() * 2.0F - 1.0F) * this.getWidth() * 0.5F;
                  float f2 = (this.rand.nextFloat() * 2.0F - 1.0F) * this.getWidth() * 0.5F;
                  this.world.addParticle(ParticleTypes.SPLASH, this.getPosX() + (double)f1, (double)(f + 0.8F), this.getPosZ() + (double)f2, vector3d.x, vector3d.y, vector3d.z);
               }
            }
         }

      }
   }

   private void func_242326_eZ() {
      this.isShaking = false;
      this.timeFriendIsShaking = 0.0F;
      this.prevTimeFriendIsShaking = 0.0F;
   }

   /**
    * Called when the mob's health reaches 0.
    */
   public void onDeath(DamageSource cause) {
      this.isWet = false;
      this.isShaking = false;
      this.prevTimeFriendIsShaking = 0.0F;
      this.timeFriendIsShaking = 0.0F;
      super.onDeath(cause);
   }

   /**
    * True if the friend is wet
    */
   @OnlyIn(Dist.CLIENT)
   public boolean isFriendWet() {
      return this.isWet;
   }

   public void setTamed(boolean tamed) {
      super.setTamed(tamed);
      if (tamed) {
         this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0D);
         this.setHealth(20.0F);
         this.setVillagerData(this.getVillagerData().withProfession(MFTradingProfession.FRIEND).withLevel(1));

      } else {
         this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(8.0D);
      }

      this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4.0D);
   }

   /*
   //OnRightClick, I think??
   public ActionResultType func_230254_b_(PlayerEntity player, Hand hand) {
      ItemStack itemstack = player.getHeldItem(hand);
      Item item = itemstack.getItem();
      if (this.world.isRemote) {
         boolean flag = this.isOwner(player) || this.isTamed() || item == Items.COOKIE && !this.isTamed() && !this.isAngry;
         return flag ? ActionResultType.CONSUME : ActionResultType.PASS;
      } else {
         if (this.isTamed()) {
            if (this.isHealingItem(itemstack) && this.getHealth() < this.getMaxHealth()) {
               if (!player.abilities.isCreativeMode) {
                  itemstack.shrink(1);
               }
               this.heal((float)item.getFood().getHealing());
               return ActionResultType.SUCCESS;
            }
            
            if (!(item instanceof DyeItem)) {
               ActionResultType actionresulttype = super.func_230254_b_(player, hand);
               //WAIT
               if ((!actionresulttype.isSuccessOrConsume() || this.isChild()) && this.isOwner(player) && Screen.func_231173_s_()) {
                  this.func_233687_w_(!this.func_233685_eM_());
                  this.isJumping = false;
                  this.navigator.clearPath();
                  this.setAttackTarget((LivingEntity)null);
                  return ActionResultType.SUCCESS;
               }

               return actionresulttype;
            }

         } else if (item == Items.COOKIE && !this.isAngry) {
            if (!player.abilities.isCreativeMode) {
               itemstack.shrink(1);
            }

            if (this.rand.nextInt(3) == 0 /*&& !net.minecraftforge.event.ForgeEventFactory.onAnimalTame(this, player)*//*) {
               this.setTamedBy(player);
               this.navigator.clearPath();
               this.setAttackTarget((LivingEntity)null);
               this.func_233687_w_(true);
               this.world.setEntityState(this, (byte)7);
            } else {
               this.world.setEntityState(this, (byte)6);
            }

            return ActionResultType.SUCCESS;
         }

         return super.func_230254_b_(player, hand);
      }
   }
   */

   /**
    * Handler for {@link World#setEntityState}
    */
   @OnlyIn(Dist.CLIENT)
   public void handleStatusUpdate(byte id) {
      if (id == 8) {
         this.isShaking = true;
         this.timeFriendIsShaking = 0.0F;
         this.prevTimeFriendIsShaking = 0.0F;
      } else if (id == 56) {
         this.func_242326_eZ();
      } else {
         super.handleStatusUpdate(id);
      }

   }

   /**
    * Checks if the parameter is an item which this animal can be fed to heal it.
    */
   public boolean isHealingItem(ItemStack stack) {
      Item item = stack.getItem();
      return item.isFood() && item.getFood().isMeat();
   }

   public int func_230256_F__() {
      return this.dataManager.get(field_234232_bz_);
   }

   public void func_230260_a__(int p_230260_1_) {
      this.dataManager.set(field_234232_bz_, p_230260_1_);
   }

   public void func_230258_H__() {
      this.func_230260_a__(field_234230_bG_.getRandomWithinRange(this.rand));
   }

   @Nullable
   public UUID func_230257_G__() {
      return this.field_234231_bH_;
   }

   public void func_230259_a_(@Nullable UUID p_230259_1_) {
      this.field_234231_bH_ = p_230259_1_;
   }
   
	/*********************************************************** ArmorStandStuff ********************************************************/

   private int disabledSlots;
   public static final DataParameter<Byte> STATUS = EntityDataManager.createKey(ArmorStandEntity.class, DataSerializers.BYTE);
   private final NonNullList<ItemStack> handItems = NonNullList.withSize(2, ItemStack.EMPTY);
   private final NonNullList<ItemStack> armorItems = NonNullList.withSize(4, ItemStack.EMPTY);
   
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

   @Override
   public boolean replaceItemInInventory(int inventorySlot, ItemStack itemStackIn) {
      EquipmentSlotType equipmentslottype;
      if (inventorySlot == 98) {
         equipmentslottype = EquipmentSlotType.MAINHAND;
      } else if (inventorySlot == 99) {
         equipmentslottype = EquipmentSlotType.OFFHAND;
      } else if (inventorySlot == 100 + EquipmentSlotType.HEAD.getIndex()) {
         equipmentslottype = EquipmentSlotType.HEAD;
      } else if (inventorySlot == 100 + EquipmentSlotType.CHEST.getIndex()) {
         equipmentslottype = EquipmentSlotType.CHEST;
      } else if (inventorySlot == 100 + EquipmentSlotType.LEGS.getIndex()) {
         equipmentslottype = EquipmentSlotType.LEGS;
      } else {
         if (inventorySlot != 100 + EquipmentSlotType.FEET.getIndex()) {
            return false;
         }

         equipmentslottype = EquipmentSlotType.FEET;
      }

      if (!itemStackIn.isEmpty() && !MobEntity.isItemStackInSlot(equipmentslottype, itemStackIn) && equipmentslottype != EquipmentSlotType.HEAD) {
         return false;
      } else {
         this.setItemStackToSlot(equipmentslottype, itemStackIn);
         return true;
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
	         boolean flag = this.isOwner(player) || this.isTamed() || item == Items.COOKIE && !this.isTamed() && !this.isAngry;
	         return flag ? ActionResultType.CONSUME : ActionResultType.PASS;
	      } else {
		      EquipmentSlotType equipmentslottype = MobEntity.getSlotForItemStack(itemstack);
              EquipmentSlotType equipmentslottype1 = this.getClickedSlot(vec);

	         if (this.isTamed()) {
	            if (this.isHealingItem(itemstack) && this.getHealth() < this.getMaxHealth()) {
	               if (!player.abilities.isCreativeMode) {
	                  itemstack.shrink(1);
	               }
	               this.heal((float)item.getFood().getHealing());
	               return ActionResultType.SUCCESS;
	            }
	      
	            if (!(item instanceof DyeItem) && equipmentslottype.getSlotType() != EquipmentSlotType.Group.ARMOR && !(item instanceof TieredItem) && !(item instanceof ShootableItem) && (itemstack.isEmpty() && !this.hasItemInSlot(equipmentslottype1))) {
	               ActionResultType actionresulttype = super.func_230254_b_(player, hand);
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

	         } else if (item == Items.COOKIE && !this.isAngry) {
	            if (!player.abilities.isCreativeMode) {
	               itemstack.shrink(1);
	            }

	            if (this.rand.nextInt(3) == 0 /*&& !net.minecraftforge.event.ForgeEventFactory.onAnimalTame(this, player)*/) {
	               this.setTamedBy(player);
	               this.navigator.clearPath();
	               this.setAttackTarget((LivingEntity)null);
	               this.setIsSitting(true);
	               this.world.setEntityState(this, (byte)7);
	            } else {
	               this.world.setEntityState(this, (byte)6);
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
		               if (this.hasItemInSlot(equipmentslottype1) && this.func_226529_a_(player, equipmentslottype1, itemstack, hand)) {
		                  return ActionResultType.SUCCESS;
		               }
		            } else {
		               if (this.func_226529_a_(player, equipmentslottype, itemstack, hand) && itemstack.getItem() != Items.CROSSBOW) {
		                  return ActionResultType.SUCCESS;
		               }
		            }
		            return ActionResultType.PASS;
		         }
		      } else {
	               MyFriend.logger.info("TEST");

		    	  super.func_230254_b_(player, hand);
		    	  return ActionResultType.PASS;
		      }
	      }
   }

   private EquipmentSlotType getClickedSlot(Vector3d p_190772_1_) {
      EquipmentSlotType equipmentslottype = EquipmentSlotType.MAINHAND;
      boolean flag = this.isSmall();
      double d0 = flag ? p_190772_1_.y * 2.0D : p_190772_1_.y;
      EquipmentSlotType equipmentslottype1 = EquipmentSlotType.FEET;
      if (d0 >= 0.1D && d0 < 0.1D + (flag ? 0.8D : 0.45D) && this.hasItemInSlot(equipmentslottype1)) {
         equipmentslottype = EquipmentSlotType.FEET;
      } else if (d0 >= 0.9D + (flag ? 0.3D : 0.0D) && d0 < 0.9D + (flag ? 1.0D : 0.7D) && this.hasItemInSlot(EquipmentSlotType.CHEST)) {
         equipmentslottype = EquipmentSlotType.CHEST;
      } else if (d0 >= 0.4D && d0 < 0.4D + (flag ? 1.0D : 0.8D) && this.hasItemInSlot(EquipmentSlotType.LEGS)) {
         equipmentslottype = EquipmentSlotType.LEGS;
      } else if (d0 >= 1.6D && this.hasItemInSlot(EquipmentSlotType.HEAD)) {
         equipmentslottype = EquipmentSlotType.HEAD;
      } else if (!this.hasItemInSlot(EquipmentSlotType.MAINHAND) && this.hasItemInSlot(EquipmentSlotType.OFFHAND)) {
         equipmentslottype = EquipmentSlotType.OFFHAND;
      }

      return equipmentslottype;
   }
   
   private boolean func_226529_a_(PlayerEntity p_226529_1_, EquipmentSlotType p_226529_2_, ItemStack p_226529_3_, Hand p_226529_4_) {
      ItemStack itemstack = this.getItemStackFromSlot(p_226529_2_);
      if (!itemstack.isEmpty() && (this.disabledSlots & 1 << p_226529_2_.getSlotIndex() + 8) != 0) {
         return false;
      } else if (itemstack.isEmpty() && (this.disabledSlots & 1 << p_226529_2_.getSlotIndex() + 16) != 0) {
         return false;
      } else if (p_226529_1_.abilities.isCreativeMode && itemstack.isEmpty() && !p_226529_3_.isEmpty()) {
         ItemStack itemstack2 = p_226529_3_.copy();
         itemstack2.setCount(1);
         this.setItemStackToSlot(p_226529_2_, itemstack2);
         return true;
      } else if (!p_226529_3_.isEmpty() && p_226529_3_.getCount() > 1) {
         if (!itemstack.isEmpty()) {
            return false;
         } else {
            ItemStack itemstack1 = p_226529_3_.copy();
            itemstack1.setCount(1);
            this.setItemStackToSlot(p_226529_2_, itemstack1);
            p_226529_3_.shrink(1);
            return true;
         }
      } else {
         this.setItemStackToSlot(p_226529_2_, p_226529_3_);
         p_226529_1_.setHeldItem(p_226529_4_, itemstack);
         return true;
      }
   }
   
   public boolean isSmall() {
      return (this.dataManager.get(STATUS) & 1) != 0;
   }
}