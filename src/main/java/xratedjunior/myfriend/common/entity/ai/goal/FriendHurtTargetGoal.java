package xratedjunior.myfriend.common.entity.ai.goal;

import java.util.EnumSet;

import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.TargetGoal;
import xratedjunior.myfriend.common.entity.FriendEntity;

public class FriendHurtTargetGoal extends TargetGoal {
   private final FriendEntity tameable;
   private LivingEntity attacker;
   private int timestamp;

   public FriendHurtTargetGoal(FriendEntity theEntityTameableIn) {
      super(theEntityTameableIn, false);
      this.tameable = theEntityTameableIn;
      this.setMutexFlags(EnumSet.of(Goal.Flag.TARGET));
   }

   /**
    * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
    * method as well.
    */
   public boolean shouldExecute() {
      if (this.tameable.isTamed() && !this.tameable.isSitting()) {
         LivingEntity livingentity = this.tameable.getOwner();
         if (livingentity == null) {
            return false;
         } else {
            this.attacker = livingentity.getLastAttackedEntity();
            int i = livingentity.getLastAttackedEntityTime();
            return i != this.timestamp && this.isSuitableTarget(this.attacker, EntityPredicate.DEFAULT) && this.tameable.shouldAttackEntity(this.attacker, livingentity);
         }
      } else {
         return false;
      }
   }

   /**
    * Execute a one shot task or start executing a continuous task
    */
   public void startExecuting() {
      this.goalOwner.setAttackTarget(this.attacker);
      LivingEntity livingentity = this.tameable.getOwner();
      if (livingentity != null) {
         this.timestamp = livingentity.getLastAttackedEntityTime();
      }

      super.startExecuting();
   }
}