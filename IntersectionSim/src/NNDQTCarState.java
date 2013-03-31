
public class NNDQTCarState {
		private int zoneOfNearestCar;
		private int relativeSpeedOfNearestCar;
		private double[] state;
		
		public static final int NUMSTATES = 18;
		
		/* STATE SPACE:
		 * Acceleration
		 * Velocity as % of desired velocity
		 * dist to intersection
		 * expected time to intersection
		 * relative velocity of car in front
		 * acceleration of car in front
		 * collision detected
		 * relative velocity of other car
		 * acceleration of other car
		 * dist to intersection of colliding car
		 * expected time to intersection of colliding car
		 * 
		 * 
		 */
		
		public NNDQTCarState () {
			state = new double[NUMSTATES];
		}
		
		public void updateState(double v, double oldV, double desiredV, float[] position, Car inFront, Car nearestIntersecting, int timeToCollision, float distToInter) {
			state[0] = Math.abs(v-oldV);
			state[1] = (v - oldV) >= 0 ? 0 : 1;
			state[2] = v/desiredV;
			state[3] = Math.abs(distToInter);
			state[4] = distToInter >= 0 ? 0 : 1;
			state[5] = v > 0 ? Math.abs(distToInter)/v : Math.abs(distToInter);
			if(inFront != null) {
				state[6] = Math.abs(inFront.getVelocity() - v);
				state[7] = (inFront.getVelocity() - v) >= 0 ? 0 : 1;
				state[8] = Math.abs(inFront.getVelocity() - inFront.getLastVel());
				state[9] = (inFront.getVelocity() - inFront.getLastVel()) >= 0 ? 0 : 1;
			} else {
				state[6] = state[7] = state[8] = state[9] = 0;
			}
			state[10] = timeToCollision == -1 ? NNLearnCar.maxLookAhead + 1 : timeToCollision;
			if(nearestIntersecting != null) {
				state[11] = Math.abs(nearestIntersecting.getVelocity() - v);
				state[12] = (nearestIntersecting.getVelocity() - v) >= 0 ? 0 : 1;
				state[13] = Math.abs(nearestIntersecting.getVelocity() - nearestIntersecting.getLastVel());
				state[14] = (nearestIntersecting.getVelocity() - nearestIntersecting.getLastVel()) >= 0 ? 0 : 1;
				double dist = Utils.dist(nearestIntersecting.getPosition(),nearestIntersecting.getIntersectionPos());
				state[15] = dist;
				state[16] = nearestIntersecting.getVelocity() > 0 ? dist/nearestIntersecting.getVelocity() : dist;
			} else {
				state[11] = state[12] = state[13] = state[14] = state[15] = state[16] = 0;
			}
			state[17] = 1; // bias unit for NN
		}
		
		public double[] getState() {
			return state;
		}
}
