import java.util.ArrayList;


public class NNLearnCar extends Car {
	
	public static final int numActions = 8;
	
	NNDQTCarState state = new NNDQTCarState();
	double [] lastState;
	private double reward;
	private double velMean = 0;
	private double velM2 = 0;
	private double velVariance = 0;
	private ArrayList<NNActionHistEntry> actionHist = new ArrayList<NNActionHistEntry>();
	private NNDQT q; 


	private int action = 0;
	private boolean actionTaken = false;
	private final double maxSteps;
	
	int crashAction = 0;
	private boolean canDriveToken = false;

	public NNLearnCar(Road road, float length, float width, int velocity, NNDQT qTable, int maxSteps) {
		super(road,length,width,velocity);
		this.maxSteps = maxSteps;
		maxLookAhead = 20;
		q = qTable;
	}
	
	public void canDrive() {
		canDriveToken = true;
	}
	
	public boolean hasToken() {
		return canDriveToken;
	}
	
	protected boolean checkForOtherCars() {
		
		if(road.interBlocked()) return false;
		else if(canDriveToken) return true;

		Car blocking = road.getRoadToRight().getCarAtIntersection();
		if(blocking == null) return true;
		else return false;
	}
	
	private boolean safetyBehaviour() {
		double dist = (road.getIntersectionPosAsRoadFract() - this.positionInRoadFraction)* road.getLength();
		if((dist > 0 && dist <= 10 && !checkForOtherCars()) || (dist > 10 && dist <= 50 && nearestIntersectingCar != null)) {
				decelerateToAvoidCollision();
				return true;
		}
		return false;
	}
	
	private void decelerateToAvoidCollision() {
		int deceleration = (int) Math
				.round((1f - ((timeToCollision - 1) / (float) maxLookAhead)) * 12f) + 1;
		velocity -= deceleration;
	}

	public boolean blockingInter() {
		double dist = (road.getIntersectionPosAsRoadFract() - this.positionInRoadFraction)* road.getLength();
		if(dist > -10 && dist < 0) return true;
		else return false;
	}
	
	public boolean atInter() {
		double dist = (road.getIntersectionPosAsRoadFract() - this.positionInRoadFraction)* road.getLength();
		if(dist >= 0 && dist < 10) return true;
		else return false;
	}
	
	protected void adjustVelocity() {
		action = 0;
		actionTaken = false;
	
		if(Runner.safeBehaviour && safetyBehaviour()) return;
	
		Car c = road.getCarInFront(this);
		if(c == null || Utils.dist(predictPosition(1),c.getPosition()) > c.getCarLength() + 0.5f) {	
			double[] s = state.getState();
			action = q.getAction(s);
			actionHist.add(new NNActionHistEntry(s, action));
			actionTaken = true;
		}
		
		lastVel = velocity;
		switch (action) {
		case 0: // Get velocity from IDM
			velocity += calcIDMVelDiff();
			break;
		case 1: // Accelerate a bit
			velocity += 1;
			break;
		case 2: // Accelerate more
			velocity += 2;
			break;
		case 3: // Accelerate hard
			velocity += 3;
			break;
		case 4: // Decelerate a bit
			velocity -= 1;
			break;
		case 5: // Decelerate more
			velocity -= 2;
			break;
		case 6: // Decelerate hard
			velocity -= 3;
			break;
		case 7: // Keep speed
			break;
		default:
			System.out.println("WWWWHAHAAHAHT?");
		}
	
		
	}

	@Override
	protected void updateState() {
		lastState = state.getState();
		state.updateState(
				velocity,
				lastVel,
				desiredVelocity,
				position,
				road.getCarInFront(this),
				this.nearestIntersectingCar,
				predictCollision(),
				(road.getIntersectionPosAsRoadFract() - this.positionInRoadFraction)* road.getLength());
		calcReward();
	}
	

	private void calcReward() {
		//if(beenInCrash()) reward = 0;
		if(arrived()) reward = (1.0d-(double)travelTime/maxSteps);// * maxCars;
		else {
			updateSpeedMeasure();
			double var = velVariance < 1 ? 1 : velVariance;
			reward = ((1 - (Math.abs(velocity - desiredVelocity) / desiredVelocity)) / var);// * maxCars;
		}
	}
	
	private void updateSpeedMeasure() {
		double delta = velocity - velMean;
		if (travelTime > 0)
			velMean += delta / (double) travelTime;
		velM2 += delta * (velocity - velMean);
		if (travelTime > 1)
			velVariance = velM2 / (double) (travelTime - 1);
		velVariance = Math.sqrt(velVariance);
	}
	
	public double getReward() {
		return reward;
	}
	
	public int getLastAction() {
		return action;
	}
	
	public boolean usedQLearning() {
		return actionTaken;
	}

	public double[] getState() {
		return state.getState();
	}
	
	public double[] getLastState() {
		return lastState;
	}
	
	public void crash() {
		super.crash();
		crashAction = actionHist.size()-1;
	}
	
	public int getCrashAction() {
		return crashAction;
	}
	
	public ArrayList<NNActionHistEntry> getActionHistory() {
		return actionHist;
	}

}
