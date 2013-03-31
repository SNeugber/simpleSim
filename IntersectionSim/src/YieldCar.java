public class YieldCar extends Car {

	public YieldCar(Road road, float length, float width, float velocity) {
		super(road, length, width, velocity);
	}

	public YieldCar(Road road, float length, float width, int velocity) {
		super(road, length, width, velocity);
	}

	protected void updateState() {
		predictCollision();
	}
	
	public boolean blockingInter() {
		return false;
	}

	public void adjustVelocity() {

		lastVel = velocity;
		if (reduceSpeed) {
			decelerateToAvoidCollision();
		} else {
			float f = calcIDMVelDiff();
			velocity += f;
		}
		if (velocity < 0) {
			velocity = 0;
		}
	}

	private void decelerateToAvoidCollision() {
		int deceleration = (int) Math
				.round((1f - ((timeToCollision - 1) / (float) maxLookAhead)) * 6f) + 1;
		velocity -= deceleration;
		reduceSpeed = false;
		reduceSpeedOtherCarHash = -1;
	}

	public boolean atInter() {
		return false;
	}

}
