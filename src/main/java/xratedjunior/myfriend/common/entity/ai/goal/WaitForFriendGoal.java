package xratedjunior.myfriend.common.entity.ai.goal;

import java.util.EnumSet;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import xratedjunior.myfriend.common.entity.FriendEntityAbstract;

public class WaitForFriendGoal extends Goal {
   private final FriendEntityAbstract tameable;

   public WaitForFriendGoal(FriendEntityAbstract entityIn) {
      this.tameable = entityIn;
      this.setMutexFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
   }

   /**
    * Returns whether an in-progress EntityAIBase should continue executing
    */
   public boolean shouldContinueExecuting() {
      return this.tameable.func_233685_eM_();
   }

   /**
    * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
    * method as well.
    */
   public boolean shouldExecute() {
      if (!this.tameable.isTamed()) {
         return false;
      } else if (this.tameable.isInWaterOrBubbleColumn()) {
         return false;
      } else if (!this.tameable.func_233570_aj_()) {
         return false;
      } else {
         LivingEntity livingentity = this.tameable.getOwner();
         if (livingentity == null) {
            return true;
         } else {
            return this.tameable.getDistanceSq(livingentity) < 144.0D && livingentity.getRevengeTarget() != null ? false : this.tameable.func_233685_eM_();
         }
      }
   }

   /**
    * Execute a one shot task or start executing a continuous task
    */
   public void startExecuting() {
      this.tameable.getNavigator().clearPath();
      this.tameable.func_233686_v_(true);
      this.tameable.getOwner().sendMessage(new StringTextComponent(this.tameable.getName().getString() + ": I'll wait here."), Util.field_240973_b_);
   }

   /**
    * Reset the task's internal state. Called when this task is interrupted by another one
    */
   public void resetTask() {
      this.tameable.func_233686_v_(false);
      this.tameable.getOwner().sendMessage(new StringTextComponent(this.tameable.getName().getString() + ": Okay let's go!"), Util.field_240973_b_);
   }
}