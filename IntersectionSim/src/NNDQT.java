import java.util.ArrayList;
import java.util.Arrays;


/**
 * NNDQT: Neural network distributed Q-Table
 * 
 * Uses a neural network to generate Q-values for a given state
 * The difference between the old Q(x,a) and the new Q(x,a)
 * to be used as the error for backpropagation.
 * 
 * @author Samuel
 *
 */
public class NNDQT {
	
	private Network qTableNN;
	private final double NN_LEARNRATE = 0.5;
	private final double NN_MOMENTUM = 0.7;
	private double Q_LEARNRATE;
	private final int numActions;
	private double epsilon;
	private ArrayList<Car> cars;	// The set of cars which are to be used for the distributed learning algorithm	
	private double numEpochs;
	
	private boolean shouldLearn = true;
	
	public NNDQT (int inputStates, int numActions, double numEpochs) {
		this.numActions = numActions;
		int hiddenCount = (int)((inputStates+numActions)*2.0/3.0);
		qTableNN = new Network(inputStates, hiddenCount, numActions, NN_LEARNRATE, NN_MOMENTUM);
		epsilon = 0.1;
		this.numEpochs = numEpochs;
	}
	
	public void setCarsRef(ArrayList<Car> cars) {
		this.cars = cars;		
	}

	public void updateTable(int run) {
		/*
		 * go through each car
		 * 	calculate global reward with each of them
		 * go through each car again
		 * 	feed error based on global reward into NN from the car's old & new states
		 */
		
		if(shouldLearn) {
			double val = 1-(((double)run)/numEpochs);
			epsilon = (float)Math.pow(val,1);
			Q_LEARNRATE = (float) Math.pow(val,2);
		}

		// Calculate average reward from those cars which have used qlearning in this step
		double currentReward = 0;
		double carCount = 0;
		for(Car c : cars) {
			if(c.getClass() != NNLearnCar.class) continue;
				currentReward += ((NNLearnCar)c).getReward();
				carCount++;
		}
		currentReward /= carCount;
		
		for(Car c : cars) {
			if(c.getClass() != NNLearnCar.class || !((NNLearnCar)c).usedQLearning()) continue;

			int action = ((NNLearnCar)c).getLastAction();

			double[] state = ((NNLearnCar)c).getState();
			double[] oldState = ((NNLearnCar)c).getLastState();
			
			double[] learnV = calcLearnVector(oldState,action,state,currentReward);
			qTableNN.computeOutputs(oldState);
			qTableNN.calcError(learnV);
			qTableNN.learn(epsilon);			
		}
		
	}
	
	public int getAction(double[] state) {
		
		double maxQ = -Double.MAX_VALUE;
		int[] doubleValues = new int[numActions];
		int maxDV = 0;
		int selectedAction = -1;
		double[] qValues = getQVals(state);

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
			selectedAction = (int) (Math.random() * qValues.length);
		}
		return selectedAction;
	}
	
	public void adjustForCrashes() {
		if(shouldLearn) {
			for(Car c : cars) {
				int crashEntry;
				if(c.beenInCrash() && c.getClass() == NNLearnCar.class && (crashEntry = ((NNLearnCar)c).getCrashAction()) > 0) {
					ArrayList<NNActionHistEntry> hist = ((NNLearnCar)c).getActionHistory();
					
					double count = 0;
					for(NNActionHistEntry elem : hist) {
						if(count >= crashEntry) break;
						double factor = 1.0d - (0.2d * Q_LEARNRATE * Math.pow(count/crashEntry,2));
						count++;
						
						double[] learnV = qTableNN.computeOutputs(elem.getState());
						learnV[elem.getAction()] *= factor;
						qTableNN.calcError(learnV);
						qTableNN.learn(epsilon);
					}
				}
			}
		}
	}
	
	private double[] getQVals(double[] state) {
		return qTableNN.computeOutputs(state);
	}
	
	private double getMaxQVal(double[] state) {
		double[] qVals = getQVals(state);
		double maxQ = -Double.MAX_VALUE;
		for(double q : qVals) {
			if(q > maxQ) maxQ = q;
		}
		return maxQ;
	}
	
	private double[] calcLearnVector(double[] state, int action, double[] newState, double reward) {
		// this needs to be changed into the integral version maybe...
		double[] oldQVals = getQVals(state);
		double this_Q = oldQVals[action];
		double max_Q = getMaxQVal(newState);

		oldQVals[action] = this_Q + Q_LEARNRATE * (reward + 0.8d * max_Q - this_Q);
		return oldQVals;
	}
	
	public NNDQT clone() {
		NNDQT n = new NNDQT(qTableNN.getInputNeuronNum(), numActions, numEpochs);
		n.qTableNN = this.qTableNN.clone();
		return n;
	}
	
	public void stopLearning() {
		shouldLearn = false;
		epsilon = 0;
		Q_LEARNRATE = 0;
	}

}
