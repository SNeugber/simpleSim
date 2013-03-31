import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public abstract class Car {

	protected Road road;
	protected float[] position;
	protected float positionInRoadFraction;
	protected float[] dimensions;
	protected float velocity;
	protected float desiredVelocity;
	protected static final float maxDecel = 5;
	protected float orientation;
	protected float lastVel;
	public static int maxLookAhead = 10;
	protected boolean reduceSpeed;
	protected boolean inCrash;
	protected int timeToCollision;
	protected int reduceSpeedOtherCarHash;
	protected int travelTime = 0;
	protected Queue<float[]> positionHist;
	protected Queue<Float> velocityHist;
	protected ArrayList<Integer> collisionCars;
	protected ArrayList<Float> vels = new ArrayList<Float>();
	protected ArrayList<Float> accels = new ArrayList<Float>();
	protected int stopTime;
	protected HashMap<Integer, Car> closeCars;
	protected float[] intersectionPos;
	protected Car inFront;
	protected Car nearestIntersectingCar;
	private boolean canCommunicate = false;
	protected int[] lastState;

	public Car(Road road, float length, float width, float velocity) {

		this.velocity = velocity;
		this.desiredVelocity = this.velocity;
		lastVel = this.velocity;
		dimensions = new float[] { length, width };
		this.road = road;
		position = this.road.getStart().clone();
		positionInRoadFraction = 0f;
		calcOrientation();
		reduceSpeed = false;
		inCrash = false;
		timeToCollision = 0;
		reduceSpeedOtherCarHash = 0;
		this.intersectionPos = road.getIntersectionPos();
		collisionCars = new ArrayList<Integer>();

		// Logging
		positionHist = new ArrayBlockingQueue<float[]>(10);
		velocityHist = new ArrayBlockingQueue<Float>(10);
		for (int i = 0; i < 10; i++) {
			positionHist.add(this.position.clone());
			velocityHist.add(this.velocity);
		}
	}

	public Car(Road road, float length, float width, int velocity) {
		this(road, length, width, velocity * 10f / 36f);
	}
	
	public boolean canCommunicate() {
		return canCommunicate;
	}
	
	public float getLastVel() {
		return lastVel;
	}
	
	public void definitelyDrive() {
		velocity++;
	}

	public boolean checkAndAdd(int carHash) {
		if (collisionCars.contains(carHash)) {
			return true;
		} else {
			collisionCars.add(carHash);
			return false;
		}
	}

	protected void calcOrientation() {
		float[] dirVec = Utils.minus(road.getEnd(), road.getStart());
		orientation = (float) (180d / Math.PI * Math
				.atan2(dirVec[1], dirVec[0]));
		if (orientation < 0)
			orientation += 360;
	}

	public float getOrientation() {
		return orientation;
	}

	public boolean beenInCrash() {
		return inCrash;
	}

	public void crash() {
		inCrash = true;
	}
	
	public abstract boolean blockingInter();
	public abstract boolean atInter();

	public void move(HashMap<Integer, Car> closeCars) {
		// Update velocity based on car in front
		this.closeCars = closeCars;
		inFront = road.getCarInFront(this);
		adjustVelocity();
		checkSafeVelocity();
		updatePosition();
		updateState();
		travelTime++;
		log();
	}

	protected abstract void adjustVelocity();

	protected abstract void updateState();

	protected void updatePosition() {
		positionInRoadFraction += Math.abs(velocity / road.getLength());
		if (positionInRoadFraction >= 1) {
			positionInRoadFraction = 1;
			position = road.getEnd().clone();
		} else {
			position[0] = road.getStart()[0]
					+ (positionInRoadFraction * (road.getEnd()[0] - road
							.getStart()[0]));
			position[1] = road.getStart()[1]
					+ (positionInRoadFraction * (road.getEnd()[1] - road
							.getStart()[1]));
		}
		positionHist.poll();
		positionHist.add(position.clone());
		velocityHist.poll();
		velocityHist.add(velocity);
	}

	public float[] getIntersectionPos() {
		return intersectionPos;
	}

	public int predictCollision() {
		
		int closestCollision = -1;
		int min = maxLookAhead + 1;

		for (Integer key : closeCars.keySet()) {
			Car c = closeCars.get(key);
			if (c.equals(this))
				continue;

			for (int i = 1; i <= maxLookAhead; i++) {
				float[] otherPredictPos = c.predictPosition(i);
				float[] thisPredictPos = this.predictPosition(i);

				if (Math.abs(orientation - c.getOrientation()) % 180 != 0 && Utils.dist(thisPredictPos, otherPredictPos) < 15 && i < min) {
					closestCollision = i;
					min = i;
					nearestIntersectingCar = c;
				}

			}
		}
		if (closestCollision >= 0) {
			if(Utils.dist(intersectionPos, position) > Utils.dist(nearestIntersectingCar.getIntersectionPos(), nearestIntersectingCar.getPosition()) && road.getIntersectionPosAsRoadFract() - this.positionInRoadFraction > 0) {
				reduceSpeed(closestCollision, 0);
			}
		} else {
			nearestIntersectingCar = null;
		}
		return closestCollision;
	}
	
	protected boolean checkForOtherCars() {
		
		// Check if there is car near intersection on the right
		// Check if there is car blocking the intersection on either road
		
		if(road.interBlocked()) return false;
		Car blocking = road.getRoadToRight().getCarAtIntersection();
		if(blocking == null) return true;
		else return false;
	}

	protected void log() {
		vels.add(velocity);
		accels.add(velocity - lastVel);
		if (velocity == 0)
			stopTime++;
	}

	public ArrayList<Float> getVelLog() {
		return vels;
	}

	public ArrayList<Float> getAcellLog() {
		return accels;
	}

	public int getStopTime() {
		return stopTime;
	}

	public int getTravelTime() {
		return travelTime;
	}

	public float getDesiredVelocity() {
		return desiredVelocity;
	}

	public void printShit() {
		System.out.print("Car " + this.hashCode() + " moved to: " + position[0]
				+ " " + position[1] + "\t");
	}

	public void printPositionHist() {
		System.out.print("Position history of car: " + this.hashCode() + "; ");
		for (float[] pos : positionHist) {
			System.out.print("[" + pos[0] + " " + pos[1] + "]; ");
		}
		System.out.println();
	}

	public void printVelocityHist() {
		System.out.print("Velocity history of car: " + this.hashCode() + "; ");
		for (float v : velocityHist) {
			System.out.print(v + "; ");
		}
		System.out.println();
	}

	public boolean arrived() {
		if (positionInRoadFraction >= 1) {
			// System.out.print("Car " + this.hashCode()+ " at position: " +
			// position[0] + " " + position[1] + " " + positionInRoadFraction +
			// "\t");
			return true;
		} else
			return false;
	}

	public float getPositionInPercent() {
		return positionInRoadFraction;
	}

	public float getCarLength() {
		return dimensions[0];
	}

	public float getVelocity() {
		return velocity;
	}

	public Road getRoad() {
		return road;
	}

	public float[] getPosition() {
		return position;
	}

	public void printDiffToCarInFront() {
		Car other = road.getCarInFront(this);
		if (other != null)
			System.out.println(Utils.dist(position, other.getPosition()));
	}

	public void printVelDiff() {
		System.out.println("Diff: " + (lastVel - velocity));
		System.out.print(" Other: " + road.getCarInFront(this).getVelocity()
				+ " self: " + this.velocity);
		System.out.println(" Dist: "
				+ Utils.dist(position, road.getCarInFront(this).getPosition()));
	}

	public void setVelocity(int velocity) {
		this.velocity = velocity * 10f / 36f;
		this.desiredVelocity = this.velocity;
	}

	public void setVelocity(float velocity) {
		this.velocity = velocity;
		this.desiredVelocity = this.velocity;
	}

	public void reduceSpeed(int timeToCollision, int otherCarHash) {
		reduceSpeed = true;
		this.timeToCollision = timeToCollision;
		reduceSpeedOtherCarHash = otherCarHash;
	}

	public int getReduceSpeedOtherCarHash() {
		return reduceSpeedOtherCarHash;
	}

	public float[] predictPosition(int secondsInFuture) {
		float roadFract = positionInRoadFraction + secondsInFuture
				* Math.abs(velocity / road.getLength());
		float[] pos = new float[2];

		if (roadFract >= 1) {
			roadFract = 1;
			pos = road.getEnd().clone();
		} else {
			pos[0] = road.getStart()[0]
					+ (roadFract * (road.getEnd()[0] - road.getStart()[0]));
			pos[1] = road.getStart()[1]
					+ (roadFract * (road.getEnd()[1] - road.getStart()[1]));
		}

		return pos;
	}

	public float predictPositionInPercent(int secondsInFuture) {
		return positionInRoadFraction + secondsInFuture
				* Math.abs(velocity / road.getLength());
	}

	protected float calcIDMVelDiff() {
		float comfBreakDecel = 3;
		float timeHeadway = 2;
		float exponent = 4;
		float accel = 1;
		float minSpacing = 5;

		Car inFront = road.getCarInFront(this);
		if (inFront != null) {
			float velDiff = accel
					* (1 - (float) Math.pow(velocity / desiredVelocity,
							exponent) - (float) Math
							.pow(((minSpacing + (velocity * timeHeadway) + (velocity
									* (velocity - inFront.getVelocity()) / (2f * (float) Math
									.sqrt(accel * comfBreakDecel)))) / (Utils
									.dist(inFront.position, this.position) - inFront
									.getCarLength())), 2));
			// if(velDiff < -1 || velDiff > 1) {
			// System.out.println("Car: " + this.hashCode() + ", Diff: " +
			// velDiff + ", Vel: " + this.velocity + ", Pos: " +
			// this.getPositionInPercent() + "; [" + this.position[0] + "," +
			// this.position[1] + "]");
			// System.out.println("Oth: " + this.hashCode() + " , Vel: " +
			// inFront.getVelocity() + ", Pos: " +
			// inFront.getPositionInPercent() + "; [" + inFront.getPosition()[0]
			// + "," + inFront.getPosition()[1] + "]");
			// }
			return velDiff;
		} else if (velocity < desiredVelocity)
			return 1;
		else
			return 0;
	}
	
	protected float calcIDMVelDiffToInter() {
		float comfBreakDecel = 3;
		float timeHeadway = 2;
		float exponent = 4;
		float accel = 1;
		float minSpacing = 5;

			float velDiff = accel
					* (1 - (float) Math.pow(velocity / desiredVelocity,
							exponent) - (float) Math
							.pow(((minSpacing + (velocity * timeHeadway) + (velocity
									* (velocity) / (2f * (float) Math
									.sqrt(accel * comfBreakDecel)))) / (Utils
									.dist(intersectionPos, this.position))), 2));
			// if(velDiff < -1 || velDiff > 1) {
			// System.out.println("Car: " + this.hashCode() + ", Diff: " +
			// velDiff + ", Vel: " + this.velocity + ", Pos: " +
			// this.getPositionInPercent() + "; [" + this.position[0] + "," +
			// this.position[1] + "]");
			// System.out.println("Oth: " + this.hashCode() + " , Vel: " +
			// inFront.getVelocity() + ", Pos: " +
			// inFront.getPositionInPercent() + "; [" + inFront.getPosition()[0]
			// + "," + inFront.getPosition()[1] + "]");
			// }
			return velDiff;
	}
	
	public boolean hasStopped() {
		if(velocity == 0) return true;
		else return false;
	}
	
	protected void checkSafeVelocity() {
		// Would the new velocity be within safety distance? If not, revert to old velocity
		// if that's not enough, use IDM model to find better deceleration
		if(inFront != null && (Utils.dist(predictPosition(1),inFront.getPosition()) < inFront.getCarLength() +0.5f || predictPositionInPercent(1) > inFront.getPositionInPercent()) && velocity > 0) {
			velocity = lastVel;
			if(this.getClass() == DistLearnCar.class) ((DistLearnCar)this).action = 7; // same as no action taken for the learning car
			if((Utils.dist(predictPosition(1),inFront.getPosition()) < inFront.getCarLength() +0.5f || predictPositionInPercent(1) > inFront.getPositionInPercent()) && velocity > 0) {
				velocity += calcIDMVelDiff();
				if(this.getClass() == DistLearnCar.class) ((DistLearnCar)this).action = 0; // same as using IDM for learning car
			}
		}

		if (velocity < 0) {
			velocity = 0;
		} else if (velocity > desiredVelocity) {
			velocity = desiredVelocity;
		}
	}

}