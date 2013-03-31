public class CarState {

	protected int accel;
	protected int distToInter;		// 5 = very far, 1 = on intersection, 0 = driving away from intersection
	protected int distToCarInFront;
	protected int timeToCollision;
	protected int driving;

	public CarState() {
		accel = 1;
		distToInter = 6;
		distToCarInFront = 5;
		timeToCollision = 1;
		driving = 2;
		//zoneOfNearestCar = 5;
		//relativeSpeedOfNearestCar = 0;
	}

	public int[] getState() {
		return new int[] { accel, distToInter, distToCarInFront, timeToCollision, driving };
	}

	public void updateState(float v, float oldV, float[] position, Car inFront, int timeToCollision, float distToInter) {

		// Velocity state
		if (oldV < v)
			accel = 3; // accelerating
		else if (oldV > v)
			accel = 2; // decelerating
		else
			accel = 1; // no speed change

		// Closesness to other car state
		if(inFront == null) {
			distToCarInFront = 5;
		} else {
			double dist = Utils.dist(position, inFront.getPosition()) + inFront.getCarLength();
			
			if(dist < 2) {
				distToCarInFront = 1;
			} else if (dist < 5) {
				distToCarInFront = 2;
			} else if (dist < 10) {
				distToCarInFront = 3;
			} else if (dist < 50) {
				distToCarInFront = 4;
			} else {
				distToCarInFront = 5;
			}
		}
		// Could collide with a car?
		// time is -1 if not colliding, have to increment it to get a proper integer string out of it
		this.timeToCollision = timeToCollision + 2;

		// Distance to intersection
		if (distToInter > 200)
			this.distToInter = 6;
		else if (distToInter > 50)
			this.distToInter = 5;
		else if (distToInter > 10)
			this.distToInter = 4;
		else if (distToInter > 0)
			this.distToInter = 3;
		else if (distToInter > -10) {
			this.distToInter = 2;
		}
		else this.distToInter = 1;
		
		// State of nearest intersecting car
		//nearestIntersecting.get

		if (v > 0)
			driving = 2;
		else
			driving = 1;
	}
}
