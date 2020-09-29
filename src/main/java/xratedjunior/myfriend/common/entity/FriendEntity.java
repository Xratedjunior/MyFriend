package xratedjunior.myfriend.common.entity;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.IAngerable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.ai.goal.ResetAngerGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.RangedInteger;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.TickRangeConverter;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xratedjunior.myfriend.common.entity.ai.goal.FollowFriendGoal;
import xratedjunior.myfriend.common.entity.ai.goal.FriendHurtByTargetGoal;
import xratedjunior.myfriend.common.entity.ai.goal.FriendHurtTargetGoal;
import xratedjunior.myfriend.common.entity.ai.goal.WaitForFriendGoal;

public abstract class FriendEntity extends FriendEntityAbstract implements IAngerable {
	   private static final DataParameter<Integer> field_234232_bz_ = EntityDataManager.createKey(FriendEntity.class, DataSerializers.VARINT);

	   private boolean isWet;
	   private boolean isShaking;
	   private float timeFriendIsShaking;
	   private float prevTimeFriendIsShaking;
	   private static final RangedInteger field_234230_bG_ = TickRangeConverter.func_233037_a_(20, 39);
	   private UUID field_234231_bH_;

	   public FriendEntity(EntityType<? extends FriendEntity> type, World worldIn) {
	      super(type, worldIn);
	      this.setTamed(false);
	   }

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
	      this.targetSelector.addGoal(3, (new HurtByTargetGoal(this)).setCallsForHelp());
	      this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::func_233680_b_));
	      this.targetSelector.addGoal(5, new ResetAngerGoal<>(this, true));
	   }

	   protected void registerData() {
	      super.registerData();
	      this.dataManager.register(field_234232_bz_, 0);
	   }

	   /**
	    * Called frequently so the entity can update its state every tick as required. For example, zombies and skeletons
	    * use this to react to sunlight and start to burn.
	    */
	   public void livingTick() {
	      super.livingTick();
	      if (!this.world.isRemote && this.isWet && !this.isShaking && !this.hasPath() && this.onGround) {
	         this.isShaking = true;
	         this.timeFriendIsShaking = 0.0F;
	         this.prevTimeFriendIsShaking = 0.0F;
	         this.world.setEntityState(this, (byte)8);
	      }

	      if (!this.world.isRemote) {
	         this.func_241359_a_((ServerWorld)this.world, true);
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

	   /**
	    * Used when calculating the amount of shading to apply while the friend is wet.
	    */
	   @OnlyIn(Dist.CLIENT)
	   public float getShadingWhileWet(float p_70915_1_) {
	      return Math.min(0.5F + MathHelper.lerp(p_70915_1_, this.prevTimeFriendIsShaking, this.timeFriendIsShaking) / 2.0F * 0.5F, 1.0F);
	   }


	   /**
	    * Called when the entity is attacked.
	    */
	   public boolean attackEntityFrom(DamageSource source, float amount) {
	      if (this.isInvulnerableTo(source)) {
	         return false;
	      } else {
	         Entity entity = source.getTrueSource();
	         this.func_233687_w_(false);
	         if (entity != null && !(entity instanceof PlayerEntity) && !(entity instanceof AbstractArrowEntity)) {
	            amount = (amount + 1.0F) / 2.0F;
	         }

	         return super.attackEntityFrom(source, amount);
	      }
	   }

	   public boolean attackEntityAsMob(Entity entityIn) {
	      boolean flag = entityIn.attackEntityFrom(DamageSource.causeMobDamage(this), (float)((int)this.func_233637_b_(Attributes.field_233823_f_)));
	      if (flag) {
	         this.applyEnchantments(this, entityIn);
	      }

	      return flag;
	   }

	   public void setTamed(boolean tamed) {
	      super.setTamed(tamed);
	      if (tamed) {
	         this.getAttribute(Attributes.field_233818_a_).setBaseValue(20.0D);
	         this.setHealth(20.0F);
	      } else {
	         this.getAttribute(Attributes.field_233818_a_).setBaseValue(8.0D);
	      }

	      this.getAttribute(Attributes.field_233823_f_).setBaseValue(4.0D);
	   }

	   public ActionResultType func_230254_b_(PlayerEntity player, Hand hand) {
	      ItemStack itemstack = player.getHeldItem(hand);
	      Item item = itemstack.getItem();
	      if (this.world.isRemote) {
	         boolean flag = this.isOwner(player) || this.isTamed() || item == Items.COOKIE && !this.isTamed() && !this.func_233678_J__();
	         return flag ? ActionResultType.CONSUME : ActionResultType.PASS;
	      } else {
	         if (this.isTamed()) {
	            if (this.isBreedingItem(itemstack) && this.getHealth() < this.getMaxHealth()) {
	               if (!player.abilities.isCreativeMode) {
	                  itemstack.shrink(1);
	               }

	               this.heal((float)item.getFood().getHealing());
	               return ActionResultType.SUCCESS;
	            }

	            if (!(item instanceof DyeItem)) {
	               ActionResultType actionresulttype = super.func_230254_b_(player, hand);
	               if ((!actionresulttype.isSuccessOrConsume() || this.isChild()) && this.isOwner(player)) {
	                  this.func_233687_w_(!this.func_233685_eM_());
	                  this.isJumping = false;
	                  this.navigator.clearPath();
	                  this.setAttackTarget((LivingEntity)null);
	                  return ActionResultType.SUCCESS;
	               }

	               return actionresulttype;
	            }

	         } else if (item == Items.COOKIE && !this.func_233678_J__()) {
	            if (!player.abilities.isCreativeMode) {
	               itemstack.shrink(1);
	            }

	            if (this.rand.nextInt(3) == 0 /*&& !net.minecraftforge.event.ForgeEventFactory.onAnimalTame(this, player)*/) {
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
	    * Checks if the parameter is an item which this animal can be fed to breed it (wheat, carrots or seeds depending on
	    * the animal type)
	    */
	   public boolean isBreedingItem(ItemStack stack) {
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
	      this.func_230260_a__(field_234230_bG_.func_233018_a_(this.rand));
	   }

	   @Nullable
	   public UUID func_230257_G__() {
	      return this.field_234231_bH_;
	   }

	   public void func_230259_a_(@Nullable UUID p_230259_1_) {
	      this.field_234231_bH_ = p_230259_1_;
	   }
}