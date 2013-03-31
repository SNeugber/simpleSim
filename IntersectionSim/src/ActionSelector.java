import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class ActionSelector {

	private double epsilon = 1;

	private HashMap<Integer, double[]> actionMapping;
	//private HashMap<Integer, HashMap<Integer, ArrayList<Double>>> updateQueue;
	int numActions = 0;
	double[] defaultQs;
	
	private double learningRate = 1;
	private double numEpochs = 1;
	

	public ActionSelector(int numActions, double defaultQ, int numEpochs) {
		// dims = dimensions;
		// int size = 1;
		// for(int d : dimensions) {
		// size *= d;
		// }
		// actionMapping = new int[size];
		this.numActions = numActions;
		this.defaultQs = new double[numActions];
		Arrays.fill(this.defaultQs, defaultQ);
		actionMapping = new HashMap<Integer, double[]>();
		this.numEpochs = (double)numEpochs;				
		
		//updateQueue = new HashMap<Integer, HashMap<Integer,ArrayList<Double>>>();
	}

	public int getAction(int[] state) {

		String s = "";
		for (int i : state)
			s += i;
		int key = Integer.parseInt(s);
		// System.out.println(key);

		double[] qValues;
		if (actionMapping.containsKey(key)) {
			qValues = actionMapping.get(key);
		} else {
			qValues = new double[defaultQs.length];
			System.arraycopy(defaultQs, 0, qValues, 0, qValues.length);
			actionMapping.put(key, qValues);
		}

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

			// System.out.println( "Exploring ..." );
			selectedAction = (int) (Math.random() * qValues.length);
		}
		return selectedAction;
	}

	public double getQValue(int[] state, int action) {
		String s = "";
		for (int i : state)
			s += i;
		int key = Integer.parseInt(s);

		return actionMapping.get(key)[action];
	}

	public void setQValue(int[] state, int action, double val) {
		String s = "";
		for (int i : state)
			s += i;
		int key = Integer.parseInt(s);

		actionMapping.get(key)[action] = val;
	}

	public double getMaxQVal(int[] state) {
		String s = "";
		for (int i : state)
			s += i;
		int key = Integer.parseInt(s);

		double[] qValues;
		if (actionMapping.containsKey(key)) {
			qValues = actionMapping.get(key);
		} else {
			qValues = new double[defaultQs.length];
			System.arraycopy(defaultQs, 0, qValues, 0, qValues.length);
			actionMapping.put(key, qValues);
		}

		double max = Double.MIN_VALUE;
		for (double q : qValues) {
			if (q > max)
				max = q;
		}
		return max;
	}

	public void updateQVal(int[] state, int action, int[] newState, double reward) {
		// this_Q = policy.getQValue( state, action );
		// max_Q = policy.getMaxQValue( newstate );
		//
		// // Calculate new Value for Q
		// new_Q = this_Q + alpha * ( reward + gamma * max_Q - this_Q );
		// policy.setQValue( state, action, new_Q );
		double this_Q = getQValue(state, action);
		double max_Q = getMaxQVal(newState);

		double new_Q = this_Q + learningRate * (reward + 0.8d * max_Q - this_Q);
		setQValue(state, action, new_Q);
	}

	public void adjustForCrashes(ArrayList<ActionHistEntry> history, int crashTime) {
		for (int i = 0; i < crashTime; i++) {
			int[] state = history.get(i).getState();
			String s = "";
			for (int j : state)
				s += j;
			int key = Integer.parseInt(s);
			double factor = 0.3d * Math.pow(i/(double)crashTime,2) * epsilon;
			actionMapping.get(key)[history.get(i).getAction()] *= (1d - factor);
		}
	}
	
	public void setLearnRates(int run) {
		double val = 1-(run/numEpochs);
		epsilon = (float)Math.pow(val,2);
		learningRate = (float) Math.pow(val,3);
	}

}
