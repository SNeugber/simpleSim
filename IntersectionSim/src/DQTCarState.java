
public class DQTCarState extends CarState {

	private int zoneOfNearestCar;
	private int relativeSpeedOfNearestCar;
	
	public void updateState(float v, float oldV, float[] position, Car inFront, Car nearestIntersecting, int timeToCollision, float distToInter) {
		super.updateState(v, oldV, position, inFront, timeToCollision, distToInter);
		
		if(nearestIntersecting != null) {
			if(nearestIntersecting.getClass() == DistLearnCar.class) {
				int[] otherState = ((DistLearnCar)nearestIntersecting).getState();
				zoneOfNearestCar = otherState[3];
				
				float speedDiff = v - nearestIntersecting.getVelocity();
				if(speedDiff > 0) relativeSpeedOfNearestCar = 3;
				else if (speedDiff == 0) relativeSpeedOfNearestCar = 2;
				else if (speedDiff < 0) relativeSpeedOfNearestCar = 1;
			} 
//			else {
//				zoneOfNearestCar = 6; // TODO: needs to be changed!!
//				float speedDiff = v - nearestIntersecting.getVelocity();
//				if(speedDiff > 0) relativeSpeedOfNearestCar = 3;
//				else if (speedDiff == 0) relativeSpeedOfNearestCar = 2;
//				else if (speedDiff < 0) relativeSpeedOfNearestCar = 1;
//			}
		} else {
			zoneOfNearestCar = 6;
			relativeSpeedOfNearestCar = 3;
		}
	}
	
	public int[] getState() {
		return new int[] { accel, distToInter, distToCarInFront, zoneOfNearestCar, relativeSpeedOfNearestCar, timeToCollision, driving };
	}
	
	public static int getDefaultState() {
		return 1656312;
	}
}
