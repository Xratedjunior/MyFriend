package xratedjunior.myfriend.common.entity.ai.goal;

import java.util.EnumSet;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import xratedjunior.myfriend.common.entity.FriendEntityAbstract;

public class WaitForFriendGoal extends Goal {
   private final FriendEntityAbstract friendEntity;

   public WaitForFriendGoal(FriendEntityAbstract friendEntity) {
      this.friendEntity = friendEntity;
      this.setMutexFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
   }

   /**
    * Returns whether an in-progress EntityAIBase should continue executing
    */
   public boolean shouldContinueExecuting() {
      return this.friendEntity.func_233685_eM_();
   }

   /**
    * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
    * method as well.
    */
   public boolean shouldExecute() {
      if (!this.friendEntity.isTamed()) {
         return false;
      } else if (this.friendEntity.isInWaterOrBubbleColumn()) {
         return false;
      } else if (!this.friendEntity.func_233570_aj_()) {
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
   public void startExecuting() {
      this.friendEntity.getNavigator().clearPath();
      this.friendEntity.func_233686_v_(true);
      if(!this.friendEntity.world.isRemote && this.friendEntity.getOwner() instanceof ServerPlayerEntity) {
          this.friendEntity.getOwner().sendMessage(new StringTextComponent(this.friendEntity.getName().getString() + ": I will wait here."), Util.field_240973_b_);
      }
   }

   /**
    * Reset the task's internal state. Called when this task is interrupted by another one
    */
   public void resetTask() {
      this.friendEntity.func_233686_v_(false);
      if(!this.friendEntity.world.isRemote && this.friendEntity.getOwner() instanceof ServerPlayerEntity) {
    	  this.friendEntity.getOwner().sendMessage(new StringTextComponent(this.friendEntity.getName().getString() + ": Okay let's go!"), Util.field_240973_b_);
      }
   }
}