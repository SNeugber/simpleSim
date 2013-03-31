import java.util.ArrayList;


public class DistLearnCar extends Car {
	
	public static final int numActions = 8;
	
	private DQTCarState state;
	private DistributedQTable q;
	private ArrayList<ActionHistEntry> actionHist = new ArrayList<ActionHistEntry>();
	private double reward;
	protected int action;
	private double velMean = 0;
	private double velM2 = 0;
	private double velVariance = 0;
	private ActionHistEntry crashAction;
	private boolean canDriveToken = false;

	private float approachingDesiredVel = desiredVelocity / 2f;
	private float criticialDesiredVel = 5;
	private float oldDesiredVel = desiredVelocity;
	
	private boolean actionTaken = false;
	
	private final double maxSteps;
	private final double maxCars;
	
	private int[] lastState;

	public DistLearnCar(Road road, float length, float width, int velocity, DistributedQTable qTable, int maxSteps, int maxCars) {
		super(road,length,width,velocity);
		q = qTable;
		state = new DQTCarState();
		maxLookAhead = 20;
		this.maxSteps = maxSteps;
		this.maxCars = maxCars;
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
	
	public int[] getLastState() {
		return lastState;
	}
	
	protected void updateState() {
		
		// get car which is nearest
		//road.getRoadToRight()
		lastState = state.getState();
		state.updateState(
				velocity,
				lastVel,
				position,
				road.getCarInFront(this),
				this.nearestIntersectingCar,
				predictCollision(),
				(road.getIntersectionPosAsRoadFract() - this.positionInRoadFraction)* road.getLength());
		calcReward();
	}

	public void canDrive() {
		canDriveToken = true;
	}
	
	public boolean atInter() {
		if(state.getState()[1] == 2) return true;
		return false;
	}
	
	protected boolean checkForOtherCars() {
				
		if(road.interBlocked()) return false;
		else if(canDriveToken) return true;

		Car blocking = road.getRoadToRight().getCarAtIntersection();
		if(blocking == null) return true;
		else return false;
	}
	
	private boolean safetyBehaviour() {
		if((state.getState()[1] == 3 && !checkForOtherCars()) || (state.getState()[1] == 4 && nearestIntersectingCar != null)) {
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
	
	public void crash() {
		inCrash = true;
		crashAction = actionHist.get(actionHist.size()-1);
	}
	
	public ActionHistEntry getCrashAction() {
		return crashAction;
	}
	
	public boolean hasToken() {
		return canDriveToken;
	}
	
	public boolean blockingInter() {
		if(state.getState()[1] == 2) return true;
		return false;
	}

	public void adjustVelocity() {
		
		action = 0;
		actionTaken = false;
		
		if(Runner.safeBehaviour && safetyBehaviour()) return;

		Car c = road.getCarInFront(this);
		if(c == null || Utils.dist(predictPosition(1),c.getPosition()) > c.getCarLength() + 0.5f) {	
			int[] s = state.getState();
			if (s[1] > 1) { // s[1] < 6 &&	This part of the state means that we're close to the intersection
				action = q.getAction(s);
				actionHist.add(new ActionHistEntry(s, action));
				actionTaken = true;
			}
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
	
	public boolean usedQLearning() {
		return actionTaken;
	}
	
	public int[] getState () {
		return state.getState().clone();
	}
	
	public ArrayList<ActionHistEntry> getActionHistory() {
		return actionHist;
	}
}
