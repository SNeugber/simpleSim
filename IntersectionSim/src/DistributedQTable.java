import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class DistributedQTable {

	private double epsilon = 1; 		// The rate of exploration: 1 = full exploration, 0 = no exploration
	private double learningRate = 1;	// The rate of learning: 1 = full learning, 0 = no more learning
	private HashMap<Integer, double[]> actionMapping; 	// The actual Q-Table: maps states (integers) to a set of Q-Values (one for each action)
	private HashMap<Integer, double[][]> updateList;	// This is used to find the average Q-Value for a certain past state over all cars in each iteration
	private int numActions = 0;		// The number of actions available
	private double[] defaultQs;		// Set of default Q-Values
	private double currentReward;	// The reward to be calculated at each iteration
	private ArrayList<Car> cars;	// The set of cars which are to be used for the distributed learning algorithm
	private final float numEpochs;	// The maximum number of epochs, used to scale epsilon and learning rate

	public DistributedQTable(int numActions, double defaultQ, int numEpochs) {
		this.numActions = numActions;
		this.defaultQs = new double[numActions];
		Arrays.fill(this.defaultQs, defaultQ);
		actionMapping = new HashMap<Integer, double[]>();
		updateList = new HashMap<Integer, double[][]>();
		this.numEpochs = (float)numEpochs;
		
		// need to initialize the default state at the beginning
		initState(DQTCarState.getDefaultState());
	}
	
	public void setCarRef(ArrayList<Car> allCars) {
		cars = allCars;
	}
	
	public void initState(int state) {
		double[] qValues = new double[defaultQs.length];
		System.arraycopy(defaultQs, 0, qValues, 0, qValues.length);
		actionMapping.put(state, qValues);
	}

	public int getAction(int[] state) {

		int key = Utils.arrayToInt(state);
		// System.out.println(key);

		if (!actionMapping.containsKey(key)) {
			initState(key);
		}
		double[] qValues = actionMapping.get(key);


		double maxQ = -Double.MAX_VALUE;
		int[] doubleValues = new int[numActions];
		int maxDV = 0;
		int selectedAction = -1;

		if (Math.random() >= epsilon) {

			for (int action = 0; action < numActions; action++) {

				if (qValues[action] > maxQ) {
					selectedAction = action;
					maxQ = qValues[action];
					maxDV = 0;
					doubleValues[maxDV] = selectedAction;
				} else if (qValues[action] == maxQ) {
					maxDV++;
					doubleValues[maxDV] = action;
				}
			}

			if (maxDV > 0) {
				int randomIndex = (int) (Math.random() * (maxDV + 1));
				selectedAction = doubleValues[randomIndex];
			}
		}

		// Select random action if all qValues == 0 or exploring.
		if (selectedAction == -1) {
			//System.out.println( "Exploring ..." );
			//exploreNum++;
			selectedAction = (int) (Math.random() * qValues.length);
			//System.out.println(selectedAction);
		}
		return selectedAction;
	}

	public double getQValue(int state, int action) {
		return actionMapping.get(state)[action];
	}

	public void setQValue(int state, int action, double val) {
		actionMapping.get(state)[action] = val;
	}
	
	public void updateTable(int run) {
		
		//System.out.println(exploreNum);
		
		// Set epsilon based on how many epochs we have passed
		//epsilon = 0.90f - 0.89f*(run/numEpochs);
		float val = 1-(((float)run)/numEpochs);
		//System.out.println("val: " + val);
		epsilon = (float)Math.pow(val,2);
		learningRate = (float) Math.pow(val,3);
//		epsilon = 0.1;
//		learningRate = 0.4;

		// Calculate average reward from those cars which have used qlearning in this step
		currentReward = 0;
		double carCount = 0;
		for(Car c : cars) {
			if(c.getClass() != DistLearnCar.class) continue;
			//if(((DistLearnCar)c).usedQLearning()) {
				currentReward += ((DistLearnCar)c).getReward();
				carCount++;
			//}
		}
		currentReward /= carCount;
		//System.out.println("reward: " + currentReward);
		
		// Clear qTable updates from last time
		updateList = new HashMap<Integer, double[][]>();
		
		// Find which entries of the qtable have to be updated and update them
		for(Car c : cars) {
			
			if(c.getClass() != DistLearnCar.class) continue;
			
			// Only consider cars which have actually used the qtable this timestep
			if(!((DistLearnCar)c).usedQLearning()) continue;
			
			int action = ((DistLearnCar)c).getLastAction();

			int state = Utils.arrayToInt(((DistLearnCar)c).getState());
			int oldState = Utils.arrayToInt(((DistLearnCar)c).getLastState());
			
			
			if(updateList.containsKey(oldState)) { // state has been seen before
				if(updateList.get(oldState)[action][1] == 0) { // .. but action has not been seen yet -> add it, and update qtable
					//System.out.println("here");
					updateList.get(oldState)[action][0]++;
					updateList.get(oldState)[action][1] += calcQVal(oldState, action, state, currentReward);
					//updateQVal(state, action, currentReward);
				}
			} else { // state has not been seen yet, so create new list of actions and update qtable
				//ArrayList<Integer> a = new ArrayList<Integer>();
				//a.add(action);
				double[][] a = new double[numActions][2];
				for(int i = 0; i < numActions; i++) {
					a[i] = new double[2];
				}
				a[action][0] = 1;
				a[action][1] = calcQVal(oldState, action, state, currentReward);
				updateList.put(oldState, a);
				//updateQVal(state, action, currentReward);
			}
		}
		
		for(Integer k : updateList.keySet()) {
			for(int i = 0; i < updateList.get(k).length; i++) {
				if(updateList.get(k)[i][0] > 0) setQValue(k, i, updateList.get(k)[i][1] / updateList.get(k)[i][0]);
			}
		}		
	}
	

	public double getMaxQVal(int state) {

		double[] qVals;
		if (!actionMapping.containsKey(state)) {
			initState(state);
		}
		qVals = actionMapping.get(state);

		double max = Double.MIN_VALUE;
		for (double q : qVals) {
			if (q > max)
				max = q;
		}
		return max;
	}
	
	private double calcQVal(int state, int action, int newState, double reward) {
		double this_Q = getQValue(state, action);
		double max_Q = getMaxQVal(newState);

		return this_Q + learningRate * (reward + 0.8d * max_Q - this_Q);
	}
	
	public void adjustForCrashes() {
		for(Car c : cars) {
			ActionHistEntry crashEntry;
			if(c.beenInCrash() && c.getClass() == DistLearnCar.class && (crashEntry = ((DistLearnCar)c).getCrashAction()) != null) {
				ArrayList<ActionHistEntry> hist = ((DistLearnCar)c).getActionHistory();
				
				double count = 1;
				double size = hist.lastIndexOf(crashEntry);
				for(ActionHistEntry elem : hist) {
					if(count >= size) break;
					double factor = 0.3d * Math.pow(count/size,2) * epsilon;
					count++;
					int state = elem.getStateAsInt();
					setQValue(state, elem.getAction(), actionMapping.get(state)[elem.getAction()] * (1d - factor));
				}
			}
		}
	}
	
	private void setQTable(HashMap<Integer, double[]> table) {
		this.actionMapping = table;
	}
	
	public DistributedQTable clone() {
		DistributedQTable out = new DistributedQTable(numActions, defaultQs[0], (int)numEpochs);
		HashMap<Integer, double[]> cloneMapping = new HashMap<Integer, double[]>();
		
		for(Integer key : actionMapping.keySet()) {
			double[] temp = new double[actionMapping.get(key).length];
			System.arraycopy(actionMapping.get(key), 0, temp, 0, temp.length);
			cloneMapping.put(key, temp);
		}
		
		out.setQTable(cloneMapping);
		return out;
	}
}
