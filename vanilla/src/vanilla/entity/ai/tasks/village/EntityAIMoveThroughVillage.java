package vanilla.entity.ai.tasks.village;

import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3d;
import vanilla.entity.EntityCreature;
import vanilla.entity.ai.RandomPositionGenerator;
import vanilla.entity.ai.pathfinding.PathEntity;
import vanilla.entity.ai.pathfinding.PathNavigateGround;
import vanilla.entity.ai.tasks.EntityAIBase;
import vanilla.world.gen.feature.village.Village;
import vanilla.world.gen.feature.village.VillageCollection;
import vanilla.world.gen.feature.village.VillageDoorInfo;

import java.util.ArrayList;
import java.util.List;

public class EntityAIMoveThroughVillage extends EntityAIBase {

	private EntityCreature theEntity;
	private double movementSpeed;

	/**
	 * The PathNavigate of our entity.
	 */
	private PathEntity entityPathNavigate;
	private VillageDoorInfo doorInfo;
	private boolean isNocturnal;
	private List<VillageDoorInfo> doorList = new ArrayList<>();

	public EntityAIMoveThroughVillage(EntityCreature theEntityIn, double movementSpeedIn, boolean isNocturnalIn) {
		this.theEntity = theEntityIn;
		this.movementSpeed = movementSpeedIn;
		this.isNocturnal = isNocturnalIn;
		this.setMutexBits(1);

		if (!(theEntityIn.getNavigator() instanceof PathNavigateGround)) {
			throw new IllegalArgumentException("Unsupported mob for MoveThroughVillageGoal");
		}
	}

	/**
	 * Returns whether the EntityAIBase should begin execution.
	 */
	public boolean shouldExecute() {
		this.resizeDoorList();

		if (this.isNocturnal && this.theEntity.worldObj.isDaytime()) return false;
		Village village = VillageCollection.get(theEntity.worldObj).getNearestVillage(new BlockPos(this.theEntity), 0);

		if (village == null) return false;
		this.doorInfo = this.findNearestDoor(village);

		if (this.doorInfo == null) {
			return false;
		}
		PathNavigateGround pathnavigateground = (PathNavigateGround) this.theEntity.getNavigator();
		boolean flag = pathnavigateground.getEnterDoors();
		pathnavigateground.setBreakDoors(false);
		this.entityPathNavigate = pathnavigateground.getPathToPos(this.doorInfo.getDoorBlockPos());
		pathnavigateground.setBreakDoors(flag);

		if (this.entityPathNavigate != null) {
			return true;
		}
		Vec3d vec3D = RandomPositionGenerator.findRandomTargetBlockTowards(this.theEntity, 10, 7,
				new Vec3d((double) this.doorInfo.getDoorBlockPos().getX(), (double) this.doorInfo.getDoorBlockPos().getY(), (double) this.doorInfo.getDoorBlockPos().getZ()));

		if (vec3D == null) {
			return false;
		}
		pathnavigateground.setBreakDoors(false);
		this.entityPathNavigate = this.theEntity.getNavigator().getPathToXYZ(vec3D.xCoord, vec3D.yCoord, vec3D.zCoord);
		pathnavigateground.setBreakDoors(flag);
		return this.entityPathNavigate != null;
	}

	/**
	 * Returns whether an in-progress EntityAIBase should continue executing
	 */
	public boolean continueExecuting() {
		if (this.theEntity.getNavigator().noPath()) {
			return false;
		}
		float f = this.theEntity.width + 4.0F;
		return this.theEntity.getDistanceSq(this.doorInfo.getDoorBlockPos()) > (double) (f * f);
	}

	/**
	 * Execute a one shot task or start executing a continuous task
	 */
	public void startExecuting() {
		this.theEntity.getNavigator().setPath(this.entityPathNavigate, this.movementSpeed);
	}

	/**
	 * Resets the task
	 */
	public void resetTask() {
		if (this.theEntity.getNavigator().noPath() || this.theEntity.getDistanceSq(this.doorInfo.getDoorBlockPos()) < 16.0D) {
			this.doorList.add(this.doorInfo);
		}
	}

	private VillageDoorInfo findNearestDoor(Village villageIn) {
		VillageDoorInfo villagedoorinfo = null;
		int i = Integer.MAX_VALUE;

		for (VillageDoorInfo villagedoorinfo1 : villageIn.getVillageDoorInfoList()) {
			int j = villagedoorinfo1.getDistanceSquared(MathHelper.floor_double(this.theEntity.posX), MathHelper.floor_double(this.theEntity.posY), MathHelper.floor_double(this.theEntity.posZ));

			if (j < i && !this.doesDoorListContain(villagedoorinfo1)) {
				villagedoorinfo = villagedoorinfo1;
				i = j;
			}
		}

		return villagedoorinfo;
	}

	private boolean doesDoorListContain(VillageDoorInfo doorInfoIn) {
		for (VillageDoorInfo villagedoorinfo : this.doorList) {
			if (doorInfoIn.getDoorBlockPos().equals(villagedoorinfo.getDoorBlockPos())) {
				return true;
			}
		}

		return false;
	}

	private void resizeDoorList() {
		if (this.doorList.size() > 15) {
			this.doorList.remove(0);
		}
	}

}
