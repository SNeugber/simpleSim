import java.util.ArrayList;
import java.util.HashMap;

public class LearnCar extends Car {
	
	public static final int numActions = 8;

	private CarState state;
	private ActionSelector actor;
	private double velMean = 0;
	private double velM2 = 0;
	private double velVariance = 0;
	private ArrayList<ActionHistEntry> actionHist = new ArrayList<ActionHistEntry>();
	private int crashTime = 0;
	private int learnStartTime = 0;
	private int action;
	private boolean actionTaken = false;
	private double reward = 0;

	public LearnCar(Road road, float length, float width, float velocity) {
		super(road, length, width, velocity);
		state = new CarState();
		actor = new ActionSelector(6, 1, 1);
		// System.out.println("aaaa");
		velMean = velocity;
	}

	public LearnCar(Road road, float length, float width, float velocity,
			ActionSelector as) {
		super(road, length, width, velocity);
		actor = as;
		state = new CarState();
		velMean = velocity;
	}

	public LearnCar(Road road, float length, float width, int velocity,
			ActionSelector as) {
		super(road, length, width, velocity);
		actor = as;
		state = new CarState();
		velMean = this.velocity;
	}

	protected void updateState() {
		float distToInter = (road.getIntersectionPosAsRoadFract() - this.positionInRoadFraction) * road.getLength();
		state.updateState(
				velocity,
				lastVel,
				position,
				road.getCarInFront(this),
				predictCollision(),
				distToInter);
		if(actionTaken) actor.updateQVal(lastState, action, state.getState(), reward);
		
	}
	
	public boolean blockingInter() {
		float distToInter = (road.getIntersectionPosAsRoadFract() - this.positionInRoadFraction) * road.getLength();
		if(distToInter > -10 && distToInter < 0) return true;
		return false;
	}
	
	public boolean atInter() {
		if(state.getState()[1] == 2) return true;
		return false;
	}


	public void adjustVelocity() {

		int[] s = state.getState();
		lastState = s;
		action = 0;
		actionTaken = false;
		if (s[1] > 1) { // s[1] < 6 && This part of the state means that we're close to the intersection
			action = actor.getAction(s);
			actionHist.add(new ActionHistEntry(s, action));
			if (learnStartTime == 0)
				learnStartTime = travelTime;
			reward = calcReward();
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
		

		if (velocity < 0) {
			velocity = 0;
		} else if (velocity > desiredVelocity)
			velocity = desiredVelocity;
	}

	private double calcReward() {

		double reward = 0;
		// Low variation in speed?
		// percentage of max speed divided by variance?
		updateSpeedMeasure();
		double var = velVariance < 1 ? 1 : velVariance;
		reward += ((1 - (Math.abs(velocity - desiredVelocity) / desiredVelocity)) / var);
		return reward;
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

	public void crash() {
		inCrash = true;
		crashTime = travelTime;
	}

	public void adjustForCrashes() {
		actor.adjustForCrashes(actionHist, crashTime - learnStartTime);
	}
}
