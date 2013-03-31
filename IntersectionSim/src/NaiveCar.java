import java.util.ArrayList;

public class NaiveCar extends Car {

	private boolean atInter = false;
	private boolean atInterAndStopped = false;
	private boolean atInterCanGo = false;
	private float interMaxVel = -1;
	private float interDist = -1;
	private boolean interToken = false;
	private Runner runRef;
	private boolean interAsGoal = false;
	
	/*
	 *  Need states:
	 *  	- approaching intersection (like within 20 m)
	 * 		- waiting IN THE TOP POSITION at the intersection
	 * 		- driving on the intersection
	 * 		- off the intersection and driving away from it 
	 *  
	 *  
	 */
	
	private int onRoadSection = 6; // 6 = somewhere before inter, 5 = approaching intersection (200m), 4 = approaching intersection (20m), 3 = in waiting position, 2 = driving on intersection, 1 off the intersection, driving away
	

	public NaiveCar(Road road, float length, float width, int velocity) {
		super(road, length, width, velocity);
	}

	public NaiveCar(Road road, float length, float width, float velocity) {
		super(road, length, width, velocity);
	}

	public NaiveCar(Road road, float length, float width, float velocity,
			float desiredVelocity) {
		super(road, length, width, velocity);
		this.desiredVelocity = desiredVelocity;
	}

	public void setRunner(Runner ref) {
		runRef = ref;
	}

//	private boolean checkForOtherCars() {
////		ArrayList<Car> others = runRef.getNaiveInterCars();
////		if (others.size() == 4) {
////			for (Car c : others) {
////				if(c.equals(this)) continue;
////				if (((NaiveCar) c).hasToken())
////					return false;
////			}
////			interToken = true;
////			System.out.println("got token");
////			return true;
////		}
////
////		for (Car c : others) {
////			if (c == this)
////				continue;
////			float conflictOrientation = orientation + 90;
////			if (conflictOrientation >= 360)
////				conflictOrientation -= 360;
////			if (conflictOrientation == c.getOrientation())
////				return false;
////		}
////		return true;
//		NaiveCar blocking = road.getRoadToRight().getCarAtIntersection();
//		if(blocking == null) return true;
//		else return false;
//	}
	

	public boolean hasToken() {
		return interToken;
	}

//	public void adjustVelocity() {
//		// If moving towards intersection, slow down, otherwise calculate speed
//		// based on IDM
//
//		if(driveTowardsInter && !atInterAndStopped) {
//			velocity += calcIDMVelDiffToInter();
//		} else if (atInterAndStopped && !atInterCanGo) {
//			if (checkForOtherCars())
//				atInterCanGo = true;
//		} else {
//			velocity += calcIDMVelDiff();
//		}
//		
////		if (atInter && !atInterCanGo) {
////			if (checkForOtherCars())
////				atInterCanGo = true;
////		} else if(driveTowardsInter && !atInterCanGo) {
////			velocity += calcIDMVelDiffToInter();
//////			System.out.println("here");
////		} else {
////			velocity += calcIDMVelDiff();
////		}
//		if (velocity < 0.01)
//			velocity = 0;
//	}
	
	private boolean interBlocked(){
		return road.interBlocked();
	}
	
	protected void adjustVelocity() {
		if(onRoadSection == 6) {
			velocity += calcIDMVelDiff();
		} else if(onRoadSection == 5 || onRoadSection == 4) {
			if(interAsGoal) velocity += calcIDMVelDiffToInter();
			else velocity += calcIDMVelDiff();
		} else if(onRoadSection == 3) {
			// check for other cars
			if(!atInterCanGo && checkForOtherCars()) {
				velocity += calcIDMVelDiff();
				atInterCanGo = true;
			} else if(atInterCanGo && !interBlocked()) {
				velocity += calcIDMVelDiff();
			}
		} else { //if(onRoadSection == 2 || onRoadSection == 1)
			velocity += calcIDMVelDiff();
		}
	}
	
	protected void updateState() {
		interDist = (road.getIntersectionPosAsRoadFract() - this.positionInRoadFraction) * road.getLength();

		if(interDist < -15) {
			onRoadSection = 1;
		} else if (interDist < 0) {
			onRoadSection = 2;
		} else if (interDist < 5 && velocity == 0) {
			onRoadSection = 3;
		} else if (interDist < 30) {
			onRoadSection = 4;
		} else if(interDist < 200) {
			onRoadSection = 5;
		}
		
		// Approaching the intersection and the other car is further away than the intersection itself: set the intersection as goal for the IDM velocity calculation
		if((onRoadSection == 4 || onRoadSection == 5) && (road.getCarInFront(this) == null || interDist < Utils.dist(road.getCarInFront(this).getPosition(),this.position))) interAsGoal = true;
		
		//if()
		
	}

	public boolean atInter() {
		//return atInter;
		if(onRoadSection > 2 && onRoadSection < 5) return true;
		else return false;
	}
	
	public boolean blockingInter() {
		if(onRoadSection == 2 || (atInterCanGo && onRoadSection == 3)) return true;
		else return false;
	}

	public void driveAtInter() {
		if(!road.interBlocked()) atInterCanGo = true;
	}
	
	public boolean isDrivingAtInter() {
		return atInterCanGo;
	}

//	protected void updateState() {
//		interDist = (road.getIntersectionPosAsRoadFract() - this.positionInRoadFraction)
//				* road.getLength();
//		
//		if(interDist < 200) {
//			if(road.getCarInFront(this) == null || interDist < Utils.dist(road.getCarInFront(this).getPosition(),this.position)) driveTowardsInter = true;
//		}
//
//		if (!atInter && interDist < (dimensions[0] / 2f) + 15
//				&& interDist > 0) { // && velocity == 0
//			atInter = true;
//		} else if (atInter && interDist < (dimensions[0] / 2f) +5 && velocity == 0) {
//			atInterAndStopped = true;
//		} else if (atInter && interDist < -15) {
//			atInter = false;
//			atInterCanGo = false;
//		}
//	}
}
