package oortcloud.hungryanimals.entities.ai;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathNavigateGround;
import oortcloud.hungryanimals.entities.capability.ICapabilityHungryAnimal;
import oortcloud.hungryanimals.entities.capability.ICapabilityTamableAnimal;
import oortcloud.hungryanimals.entities.capability.ProviderHungryAnimal;
import oortcloud.hungryanimals.entities.capability.ProviderTamableAnimal;
import oortcloud.hungryanimals.entities.food_preferences.FoodPreferenceManager;
import oortcloud.hungryanimals.entities.food_preferences.IFoodPreference;

public class EntityAITemptEdibleItem extends EntityAIBase {
	/** The entity using this AI that is tempted by the player. */
	private EntityCreature temptedEntity;
	private double speed;
	/** The player that is tempting the entity that is using this AI. */
	private EntityPlayer temptingPlayer;
	private IFoodPreference<ItemStack> pref;
	private ICapabilityHungryAnimal capHungry;
	private ICapabilityTamableAnimal capTamable;
	/**
	 * A counter that is decremented each time the shouldExecute method is
	 * called. The shouldExecute method will always return false if
	 * delayTemptCounter is greater than 0.
	 */
	private int delayTemptCounter;
	private static int delay = 100;
	
	/** True if this EntityAITempt task is running */
	private boolean isRunning;
	private boolean avoidWater;

	public EntityAITemptEdibleItem(EntityCreature animal, double speedIn) {
		this.delayTemptCounter = animal.getRNG().nextInt(delay);
		
		this.temptedEntity = animal;
		this.speed = speedIn;
		this.pref = FoodPreferenceManager.getInstance().REGISTRY_ITEM.get(this.temptedEntity.getClass());
		this.capHungry = animal.getCapability(ProviderHungryAnimal.CAP, null);
		this.capTamable = animal.getCapability(ProviderTamableAnimal.CAP, null);
		this.setMutexBits(3);

		if (!(animal.getNavigator() instanceof PathNavigateGround)) {
			throw new IllegalArgumentException("Unsupported mob type for TemptGoal");
		}
	}

	@Override
	public boolean shouldExecute() {
		if (this.delayTemptCounter > 0) {
			--this.delayTemptCounter;
			return false;
		} else {
			if (capTamable.getTaming() < 1)
				return false;

			this.temptingPlayer = this.temptedEntity.worldObj.getClosestPlayerToEntity(this.temptedEntity, 10.0D);

			if (this.temptingPlayer == null) {
				return false;
			} else {
				ItemStack itemstack = this.temptingPlayer.getHeldItemMainhand();
				return itemstack == null ? false : pref.canEat(capHungry, itemstack);
			}
		}
	}

	@Override
	public void startExecuting() {
		this.isRunning = true;
		this.avoidWater = ((PathNavigateGround) this.temptedEntity.getNavigator()).getAvoidsWater();
		((PathNavigateGround) this.temptedEntity.getNavigator()).setAvoidsWater(false);
	}
	
	@Override
	public boolean continueExecuting() {
		return this.shouldExecute();
	}

	@Override
	public void updateTask() {
		this.temptedEntity.getLookHelper().setLookPositionWithEntity(this.temptingPlayer, 30.0F, (float) this.temptedEntity.getVerticalFaceSpeed());

		if (this.temptedEntity.getDistanceSqToEntity(this.temptingPlayer) < 6.25D) {
			this.temptedEntity.getNavigator().clearPathEntity();
		} else {
			this.temptedEntity.getNavigator().tryMoveToEntityLiving(this.temptingPlayer, this.speed);
		}
	}
	
	@Override
	public void resetTask() {
		this.temptingPlayer = null;
		this.temptedEntity.getNavigator().clearPathEntity();
		this.delayTemptCounter = delay;
		this.isRunning = false;
		((PathNavigateGround) this.temptedEntity.getNavigator()).setAvoidsWater(this.avoidWater);
	}
}
