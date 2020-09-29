package xratedjunior.myfriend.common.entity;

import java.time.LocalDate;
import java.time.temporal.ChronoField;

import javax.annotation.Nullable;

import net.minecraft.block.Blocks;
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
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import xratedjunior.myfriend.common.entity.ai.attribute.MFCreatureAttribute;
import xratedjunior.myfriend.common.entity.ai.goal.PassiveRangedBowAttackGoal;

public abstract class DefaultFriendEntity2 extends FriendEntity implements IRangedAttackMob
{	
	/*********************************************************** Default Entity ********************************************************/

	//@SuppressWarnings("unused")
	//Check MobEntity for more features
	//private final NonNullList<ItemStack> inventorySlots = NonNullList.withSize(4, ItemStack.EMPTY);
	//protected final float[] inventorySlotsDropChances = new float[4];

	private final PassiveRangedBowAttackGoal<DefaultFriendEntity2> bowGoal = new PassiveRangedBowAttackGoal<>(this, 1.0D, 20, 15.0F);
	private final MeleeAttackGoal meleeGoal = new MeleeAttackGoal(this, 1.2D, false) {
		
		/**
		* Reset the task's internal state. Called when this task is interrupted by another one
		*/
		public void resetTask() {
			super.resetTask();
			DefaultFriendEntity2.this.setAggroed(false);
		}
	
		/**
		* Execute a one shot task or start executing a continuous task
		*/
		public void startExecuting() {
			super.startExecuting();
			DefaultFriendEntity2.this.setAggroed(true);
		}
	};

	public DefaultFriendEntity2(EntityType<? extends DefaultFriendEntity2> type, World worldIn) {
		super(type, worldIn);
		this.setCombatTask();
		this.experienceValue = 5;
	}
	
	/**
	* Called frequently so the entity can update its state every tick as required. For example, zombies and skeletons
	* use this to react to sunlight and start to burn.
	*/
	public void livingTick() {
		this.updateArmSwingProgress();
		super.livingTick();
	}

	public static AttributeModifierMap.MutableAttribute abstractFriendAttributes() {
	   return MobEntity.func_233666_p_().func_233815_a_(Attributes.field_233821_d_, (double)0.3F).func_233815_a_(Attributes.field_233818_a_, 20.0D).func_233815_a_(Attributes.field_233823_f_, 4.0D);
	}
   
	@Override
	public boolean canBeLeashedTo(PlayerEntity player) {
	   return false;
	}
   
   @Nullable
   public ILivingEntityData onInitialSpawn(IWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag) {
      spawnDataIn = super.onInitialSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
      this.setEquipmentBasedOnDifficulty(difficultyIn);
      //this.setEnchantmentBow();
      //this.setItemStackToSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
      this.setEnchantmentBasedOnDifficulty(difficultyIn);
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
	

	public void setItemStackToSlot(EquipmentSlotType slotIn, ItemStack stack) {
		super.setItemStackToSlot(slotIn, stack);
		if (!this.world.isRemote) {
			this.setCombatTask();
		}
	}
   
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
		return MFCreatureAttribute.ROMEO;
	}
	
	/**
	* Returns whether this Entity is on the same team as the given Entity.
	*/
	public boolean isOnSameTeam(Entity entityIn) {
		
		//Default
		if (super.isOnSameTeam(entityIn)) {
			return true;
		} else if (entityIn instanceof LivingEntity && ((LivingEntity)entityIn).getCreatureAttribute() == MFCreatureAttribute.ROMEO) {
			return this.getTeam() == null && entityIn.getTeam() == null;
		} else {
			return false;
		}
		
	}
	
	/*********************************************************** Tame Entity ********************************************************/

}
