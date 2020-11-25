package xratedjunior.myfriend.common.entity.ai.goal;

import java.util.EnumSet;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import xratedjunior.myfriend.common.entity.TameableFriendEntity;

public class WaitForFriendGoal extends Goal {
   private final TameableFriendEntity friendEntity;

   public WaitForFriendGoal(TameableFriendEntity friendEntity) {
      this.friendEntity = friendEntity;
      this.setMutexFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
   }

   /**
    * Returns whether an in-progress EntityAIBase should continue executing
    */
   @Override
   public boolean shouldContinueExecuting() {
      return this.friendEntity.func_233685_eM_();
   }

   /**
    * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
    * method as well.
    */
   @Override
   public boolean shouldExecute() {
      if (!this.friendEntity.isTamed()) {
         return false;
      } else if (this.friendEntity.isInWaterOrBubbleColumn()) {
         return false;
      } else if (!this.friendEntity.isOnGround()) {
         return false;
      } else {
         LivingEntity livingentity = this.friendEntity.getOwner();
         if (livingentity == null) {
            return true;
         } else {
            return this.friendEntity.getDistanceSq(livingentity) < 144.0D && livingentity.getRevengeTarget() != null ? false : this.friendEntity.func_233685_eM_();
         }
      }
   }

   /**
    * Execute a one shot task or start executing a continuous task
    */
   @Override
   public void startExecuting() {
      this.friendEntity.getNavigator().clearPath();
      this.friendEntity.func_233686_v_(true);
      if(!this.friendEntity.world.isRemote && this.friendEntity.getOwner() instanceof ServerPlayerEntity) {
          this.friendEntity.getOwner().sendMessage(new StringTextComponent(this.friendEntity.getDisplayName().getString() + ": I will wait here!."), Util.DUMMY_UUID);
      }
   }

   /**
    * Reset the task's internal state. Called when this task is interrupted by another one
    */
   @Override
   public void resetTask() {
      this.friendEntity.func_233686_v_(false);
      if(!this.friendEntity.world.isRemote && this.friendEntity.getOwner() instanceof ServerPlayerEntity) {
    	  this.friendEntity.getOwner().sendMessage(new StringTextComponent(this.friendEntity.getDisplayName().getString() + ": Okay let's go!"), Util.DUMMY_UUID);
      }
   }
}