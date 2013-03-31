import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.security.SecureRandom;

public class Runner extends Thread {

	private Road[] roads;
	private ArrayList<Car> cars;
	private ArrayList<Car> allCars;
	private SecureRandom rand = new SecureRandom();
	private int MAXCARS = 100;
	public static int MEANCARNUM;
	public static int VARCARNUM;
	public static final int MAXSTEPS = 2000;
	public static final int NUMCARTYPES = 5;
	private HashMap<Integer, Car> intersectionCars;
	private ArrayList<Car> naiveIntersectionCars;
	private static boolean quit = false;
	private int step;
	private int run;

	private static boolean mixCars = false;

	private static int minVel = 30;
	private static int velDev = 30;

	private static ActionSelector[] ass;
	private DistributedQTable dqt;
	private DistributedQTable bestDQT;
	private NNDQT bestnndqt;
	private NNDQT nndqt;

	public static AtomicInteger threadCount = new AtomicInteger(0);

	private int carType = 0;
	private static int numRuns = 0;
	private static boolean displayCars = false;

	private static Vis2 visualizer;
	private static int refreshRate = 0;

	private DataAggregator data;

	private int runMode = 0;

	private int setVelocity = -1;

	private int[] carNumbers;
	private int[] carTypesByPosition;
	private boolean randomizeAtEachRun = false;

	private static boolean makeCrash = false;
	
	public static boolean safeBehaviour = false;

	public static void main(String[] args) {

		/*
		 * args: [0] = one of: {naive, protocol, rl} [1] = number of runs [2] =
		 * millisecond delay between frame updates for refreshing: 0 == no
		 * display
		 * 
		 * new args: [0] = exp, [1] = number of iterations, [2]..[6] = numCars of type 0..4
		 * 
		 */

		// ### Check input arguments
		if (args.length != 3 && args.length != 1 && args.length != 10) {
			System.out.println("Wrong number of arguments");
			System.out
			.println("[0] = one of: {naive, protocol, rl, drl, mixed}, [1] = number of runs, [2] = millisecond delay between frame updates for refreshing: 0 == no display, [3]: number of cars, [4]: variation in cars");
			System.exit(0);
		}

		int carType = 0;
		if (args[0].equals("naive")) {
			carType = 0;
		} else if (args[0].equals("protocol")) {
			carType = 1;
		} else if (args[0].equals("rl")) {
			carType = 2;
		} else if (args[0].equals("drl")) { 
			carType = 3;
		} else if (args[0].equals("mixed")) { 
			carType = 3;
			mixCars = true;
		} else if (args[0].equals("exp2")) {
			experiment2();
			return;
		}  else if (args[0].equals("exp3")) {
			experiment3();
			return;
		} else if (args[0].equals("exp4")) {
			continuousRun();
			return;
		} else if (args[0].equals("exp5")) {
			experiment5();
			return;
		} else if (args[0].equals("exp")) {
			int[] carNums = new int[NUMCARTYPES];
			try {
				numRuns = Integer.parseInt(args[1]);
				refreshRate = Integer.parseInt(args[2]);
				if(args[3].equals("safeOn")) {safeBehaviour = true;}
				if(args[4].equals("crash")) {makeCrash = true;}
				for(int i = 0; i < carNums.length; i++) carNums[i] = Integer.parseInt(args[(5+i)]);
			} catch (Exception e) {
				System.err.println("Invalid arguments for running multiple experiments");
				System.exit(0);
			}
			experiments(carNums);
			return;
		} else {
			System.out.println("Wrong car type given: " + args[0]);
			System.exit(0);
		}

		String chartFileName = args[0];
		for(int i = 1; i < args.length; i++) {chartFileName += "_" + args[i];}
		DataAggregator.initChart(1,args[0],new String[] {args[1] + ", " + args[2]},chartFileName);

		//int meanCars, varCars;
		try {
			numRuns = Integer.parseInt(args[1]);
			refreshRate = Integer.parseInt(args[2]);
			//MEANCARNUM = Integer.parseInt(args[3]);
			//VARCARNUM = Integer.parseInt(args[4]);
			if (numRuns <= 0 || refreshRate < 0) {
				System.out
				.println("Number of runs given is too small, or refreshrate negative");
				System.exit(0);
			}
		} catch (Exception e) {
			System.out.println("Invalid arguments");
			System.exit(0);
		}

		if (refreshRate > 0) {
			displayCars = true;
			visualizer = new Vis2();
		}
		// ### End check input arguments

		// Start program
		Runner r = new Runner();
		r.setCarType(carType);
		if(carType == 2) r.initQT();
		else if(carType == 3) r.initDQT();
		r.start();
		do {
			Thread.yield();
		} while (r.isAlive());
		r.printData();

		// Clean up GUI
		if (displayCars) {
			visualizer.dispose();
		}
	}


	private static void experiment2() {

		/* Experiment 2:
		 * 3 cars, cars driving at same speed (so arrive at intersection at the same time)
		 * varying mix of cars:
		 * 	- 0/1/2/3 cars learning
		 * 	- for 1/2 cars learning: at different positions
		 * 
		 * 
		 */

		Runner[] rs = new Runner[8];
		numRuns = 10000;
		/*
		 * TODO: need to get ordering down somehow...
		 */
		rs[0] = new Runner(new int[] {0,0,0});
		rs[1] = new Runner(new int[] {3,0,0});
		rs[2] = new Runner(new int[] {0,3,0});
		rs[3] = new Runner(new int[] {0,0,3});
		rs[4] = new Runner(new int[] {3,3,0});
		rs[5] = new Runner(new int[] {3,0,3});
		rs[6] = new Runner(new int[] {0,3,3});
		rs[7] = new Runner(new int[] {3,3,3});

		for(Runner r : rs) {
			r.runMode = 8;
			r.setVelocity = 60;
			r.initDQT();
			r.start();
		}

		while(threadCount.get() != rs.length) Thread.yield();

		for(int i = 0; i < rs.length; i++) {
			System.out.println("case: " + (i+1));
			rs[i].printData();
		}
	}

	private static void experiment3() {

//				displayCars = true;
//				refreshRate = 100;
//				visualizer = new Vis2();

		/* Experiment 3: testing different behaviours when one learning car
		 * approaches an intersection with two normal cars crossing
		 * 
		 * 3.1: normal learning and everything
		 * 3.2: force the car to slow down at the beginning so to avoid the crossing cars altogether
		 * 3.3: make the car learn safe behaviour
		 */
		DataAggregator.initChart(2, "experiment 3", new String[] {"[3,0,0,3]"},"experiment3");

		numRuns = 100000;
		makeCrash = true;
		safeBehaviour = true;
		Runner r = new Runner(new int[] {4,0,0,4}, -1);
		r.runMode = 8;
		r.setVelocity = 60;
		r.initDQT();
		r.initNNDQT();
		r.start();

		//		numRuns = 50000;
		//		Runner r2 = new Runner(new int[] {1,1,1,1}, 1);
		//		r2.runMode = 8;
		//		r2.setVelocity = 60;
		//		r2.initDQT();
		//		r2.start();

		while(threadCount.get() != 1) Thread.yield();
		r.printData();
	}

	private static void experiment5() {


		//		displayCars = true;
		//		refreshRate = 100;
		//		visualizer = new Vis2();

		DataAggregator.initChart(2, "experiment 5", new String[] {"[4,4,4,4]"}, "experiment5");
		numRuns = 100000;
		Runner r = new Runner(new int[] {4,0,0,4}, -1); // The "-1" here makes the graph also display the number of collisions
		r.runMode = 10;
		r.setVelocity = 60;
		r.initNNDQT();
		r.start();

		while(threadCount.get() != 1) Thread.yield();
		r.printData();

	}

	private static void experiments(int[] carNums) {


//		displayCars = true;
//		refreshRate = 100;
//		visualizer = new Vis2();

		int numCars = 0;
		String carTypes = "";
		String ratios = "";
		String chartFileName = "exp_r" + numRuns + "_" + (safeBehaviour ? "safeOn" : "safeOff") + (makeCrash ? "_collide" : "");
		for(int i = 0; i < carNums.length; i++) {
			if(carNums[i] > 0) {
				if(carTypes.length() == 0) {carTypes += i;}
				else {carTypes += ", " + i;}
				numCars += carNums[i];
			}
			chartFileName += "_t" + i + "_" + carNums[i];
		}
		
		for(int i = 0; i < carNums.length; i++) {
			if(carNums[i] > 0) {
				if(ratios.length() == 0) {ratios += (carNums[i]/(double)numCars);}
				else {ratios += ", " + (carNums[i]/(double)numCars);}
			}
		}

		DataAggregator.initChart(1, "Cartypes: " + carTypes + "; Ratio: " + ratios + "; NumCars: " + numCars + "; " + (safeBehaviour ? "safeOn" : "safeOff") + (makeCrash ? "; collide" : ""), new String[] {"Average travel times"}, chartFileName);
		Runner r = new Runner(carNums, true);
		r.runMode = 10;
		r.setVelocity = 60;
		if(carNums[2] > 0) {r.initQT();}
		if(carNums[3] > 0) {r.initDQT();}
		if(carNums[4] > 0) {r.initNNDQT();}
		r.start();
		while(threadCount.get() != 1) Thread.yield();
		r.printData();
		if (displayCars) {
			visualizer.dispose();
		}

	}

	private static void continuousRun() {
		////
		//		displayCars = true;
		//		refreshRate = 50;
		//		visualizer = new Vis2();

		Runner r = new Runner();
		r.runMode = 9;
		r.setCarType(3);
		r.initDQT();
		r.start();
		while(threadCount.get() != 1) Thread.yield();
		r.printData();
	}
	
	private int[] randomizePositions(int[] carNums) {
		int numCars = 0;
		int type1 = -1;
		int type2 = -1;
		for(int i = 0; i < carNums.length; i++) {
			if(carNums[i] > 0 && type1 == -1) type1 = i;
			else if(carNums[i]  > 0 && type2 == -1) type2 = i;
			numCars += carNums[i];
		}
		
		int[] carTypePositions = new int[numCars];
		Arrays.fill(carTypePositions, -1);
		ArrayList<Integer> remaining = new ArrayList<Integer>(numCars);
		for(int i = 0; i < numCars; i++) {
			remaining.add(i);
		}
		for(int i = 0; i < carNums.length; i++) {
			if(carNums[i] > 0) {
				for(int k = 0; k < carNums[i]; k++) {
					int index = (int)(rand.nextDouble() * remaining.size());
					carTypePositions[remaining.get(index)] = i;
					remaining.remove(index);
				}
			}
		}
//		for(int p : carTypePositions) System.out.print(p);
//		System.out.println();
		return carTypePositions;
	}

	public Runner (int[] carTypes) {
		carTypesByPosition = carTypes;
		data = new DataAggregator(0);

		MAXCARS = carTypesByPosition.length;
	}
	
	public Runner (int[] carNums, boolean randomizePositions) {
		carTypesByPosition = randomizePositions(carNums);
		carNumbers = carNums;
		randomizeAtEachRun = randomizePositions;

		data = new DataAggregator(0);
		MAXCARS = carTypesByPosition.length;
	}

	public Runner (int[] carTypes, int dataSeriesNum) {
		carTypesByPosition = carTypes;
		data = new DataAggregator(dataSeriesNum);

		MAXCARS = carTypes.length;
	}

	public Runner() {}

	public void run() {

		if(runMode == 9) {
			init();
			continuousEpoch();
			return;
		}


		for (run = 0; run < numRuns; run++) {
			if(randomizeAtEachRun) carTypesByPosition = randomizePositions(carNumbers);
			init();
			runEpoch();
			if(data.pushData()) {
				if(dqt != null) {bestDQT = dqt.clone();}
				if(nndqt != null) {bestnndqt = nndqt.clone();}
			}
		}

		if(refreshRate > 0) {
			if(bestDQT != null) {
				dqt = bestDQT;
				displayCars = true;
				visualizer = new Vis2();
				init();
				runEpoch();
			}
			else if(bestnndqt != null) {
				nndqt = bestnndqt;
				nndqt.stopLearning();
				displayCars = true;
				visualizer = new Vis2();
				init();
				runEpoch();
			}
			else if(ass != null) {
				displayCars = true;
				visualizer = new Vis2();
				init();
				runEpoch();
			}
		}
		threadCount.incrementAndGet();
	}

	private void setCarType(int ct) {
		carType = ct;
	}


	private void initQT() {
		ass = new ActionSelector[MAXCARS];
		for (int i = 0; i < MAXCARS; i++) {
			ass[i] = new ActionSelector(LearnCar.numActions, 1, numRuns);
		}
	}

	private void initDQT() {
		dqt = new DistributedQTable(DistLearnCar.numActions, 1, numRuns);
	}

	private void initNNDQT() {
		nndqt = new NNDQT(NNDQTCarState.NUMSTATES, NNLearnCar.numActions, numRuns);
	}

	public void printData() {
		data.printEntireDataSet();
		System.out.println(data.aggregateAndPrint());
		data.saveImage();
		data.killChart();
	}

	private void continuousEpoch() {
		run = 0;
		int carNum = 0;
		numRuns = 200000; // 1 million seconds
		int[] lastSpawns = new int[roads.length];
		float[] spawnRates = new float[roads.length];
		for(int i = 0; i < spawnRates.length; i++) {
			spawnRates[i] = 0.001f + rand.nextFloat()*0.04f;
			lastSpawns[i] = -(int)Math.ceil(1/spawnRates[i]);
		}
		do {
			//System.out.println(run/(float)numRuns);
			// Go through roads one by one and add cars
			for (int i = 0; i < roads.length; i++) {
				if (1.0f/(float)(run - lastSpawns[i]) <= spawnRates[i]) {
					lastSpawns[i] = run;
					//carType = carTypesByPosition[carNum];
					Car c = createCar(roads[i], carNum, -1);
					if (roads[i].addCar(c, run)) {
						addCar(c);
						carNum++;
					}
				}
			}

			// Display everything if specified
			if (displayCars) {
				try {
					visualizer.repaint();
					Thread.sleep(refreshRate);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			updateIntersectionCars(); // Update the list of cars which are at
			// the intersection (this may be
			// deprecated!!)
			nextStep(); // Perform one step in the simulation
			run++;
			//data.pushData();
		} while (run < numRuns && !allArrived() && !Runner.shouldTerminate());
		//dqt.adjustForCrashes();

	}

	//	

	private void runEpoch() {
		step = 0; // Current step in the simulation
		int carNum = 0; // Current number of cars in the simulation
		do {
			// Go through roads one by one and add cars
			for (Road road : roads) {
				if (carNum < MAXCARS) {
					carType = carTypesByPosition[carNum];
					if(makeCrash) {
						if(carType == 4 || carType == 3 || carType == 2) setVelocity = 44;
						else setVelocity = 60;
					}
					Car c = createCar(road, carNum, setVelocity);
					if (road.addCar(c, step)) {
						addCar(c);
						carNum++;
					}
				}
			}

			// Display everything if specified
			if (displayCars) {
				try {
					visualizer.repaint();
					Thread.sleep(refreshRate);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			updateIntersectionCars(); // Update the list of cars which are at
			// the intersection (this may be
			// deprecated!!)
			nextStep(); // Perform one step in the simulation
			step++;
		} while (step < MAXSTEPS && !allArrived() && !Runner.shouldTerminate());
		if(dqt != null) dqt.adjustForCrashes();
		if(nndqt != null) nndqt.adjustForCrashes();
		if(ass != null) {
			for(Car c : cars) {
				if(c.getClass() == LearnCar.class) ((LearnCar)c).adjustForCrashes();
			}
		}
	}

	private Car createCar(Road r, int carNum, int velocity) {
		if(velocity == -1) velocity = (int)(minVel + rand.nextFloat() * (float)velDev);
		if(mixCars) {
			if(rand.nextFloat() > 0.5f) carType = 3;
			else carType = 0;
		}
		switch (carType) {
		case 0:
			Car c = new NaiveCar(r, 4, 2, velocity);
			((NaiveCar) c).setRunner(this);
			return c;
		case 1:
			return new YieldCar(r, 4, 2, velocity);
		case 2:
			return new LearnCar(r, 4, 2, velocity, ass[carNum]);
			//return new LearnCar(r, 4, 2, velocity, ass[0]);
		case 3:
			return new DistLearnCar(r, 4, 2, velocity, dqt, MAXSTEPS, MAXCARS);
		case 4:
			return new NNLearnCar(r, 4, 2, velocity, nndqt, MAXSTEPS);
		default:
			System.out.println("Car type changed unexpectedly");
			System.exit(0);
			return null;
		}
	}

	public void init() {

		if(runMode == 0) MAXCARS = 20 + rand.nextInt(20);

		roads = new Road[4];
		roads[0] = new Road(new float[] { -1000f, 5f }, new float[] { 1000f, 5f });
		roads[1] = new Road(new float[] { 1000f, -5f }, new float[] { -1000f, -5f });
		roads[2] = new Road(new float[] { 5f, 1000f }, new float[] { 5f, -1000f });
		roads[3] = new Road(new float[] { -5f, -1000f }, new float[] { -5f, 1000f });
		// for(Road r : roads) {
		// System.out.println("Road " + r.hashCode() + " : " + r.getStart()[0] +
		// ", " + r.getStart()[1]);
		// }
		roads[0].setIntersection(new float[] { -5, 5f });
		roads[0].setIntersectingRoads(roads[2], new Road[] {roads[2],roads[3]});
		roads[1].setIntersection(new float[] { 5, -5f });
		roads[1].setIntersectingRoads(roads[3], new Road[] {roads[2],roads[3]});
		roads[2].setIntersection(new float[] { 5, 5f });
		roads[2].setIntersectingRoads(roads[1], new Road[] {roads[0],roads[1]});
		roads[3].setIntersection(new float[] { -5, -5f });
		roads[3].setIntersectingRoads(roads[0], new Road[] {roads[0],roads[1]});

		cars = new ArrayList<Car>();
		allCars = new ArrayList<Car>();
		intersectionCars = new HashMap<Integer, Car>();

		naiveIntersectionCars = new ArrayList<Car>(roads.length);

		if (displayCars) {
			visualizer.addRoads(roads);
			visualizer.setCars(cars);
		}
		data.setRefs(roads, allCars, carType);

		if(dqt != null) dqt.setCarRef(allCars);
		if(nndqt != null) nndqt.setCarsRef(allCars);
	}

	public void addRoadsToVisualizer(Vis2 v) {
		v.addRoads(roads);
	}

	public static void quit() {
		quit = true;
	}

	public static boolean shouldTerminate() {
		return quit;
	}

	public Road[] getRoads() {
		return roads;
	}

	public void addCar(Car c) {
		cars.add(c);
		allCars.add(c);
	}

	public ArrayList<Car> getCars() {
		return cars;
	}

	public boolean allArrived() {
		for (Car c : cars) {
			if (!c.arrived())
				return false;
		}
		return true;
	}

	public void nextStep() {
		for (Car c : cars) {
			if (!c.arrived())
				c.move(intersectionCars);
		}
		ArrayList<Car> removals = new ArrayList<Car>();
		for (Car c : cars) {
			if (c.arrived()) {
				c.getRoad().removeArrived(c, step);
				removals.add(c);
			}
		}
		for (Car c : removals) {
			if(runMode == 9) {
				data.pushData(c);
			}
			cars.remove(c);
		}
		Utils.checkCollisions(cars);
		if(dqt != null) dqt.updateTable(run);
		if(nndqt != null) nndqt.updateTable(run);
		if(ass != null) for(ActionSelector a : ass) a.setLearnRates(run);
		//if(carType == 3) dqt.updateTable(step);
		//		if(carType == 2) {
		//			ass[0].updateQs();			
		//		}
	}

	public void updateIntersectionCars() {

		int intersectionDist = 0;
		switch(carType) {
		case 0: intersectionDist = 20; break;
		case 1: intersectionDist = 200; break;
		case 2: intersectionDist = 20; break;
		case 3: intersectionDist = 200; break;
		case 4: intersectionDist = 200; break;
		default: 
			System.out.println("Car type not defined");
			System.exit(0);
		}

		for (Car c : cars) {
			if (Utils.dist(c.getPosition(), c.getIntersectionPos()) < intersectionDist) {
				intersectionCars.put(c.hashCode(), c);
			}
		}

		ArrayList<Integer> keys = new ArrayList<Integer>();
		for (int key : intersectionCars.keySet()) {
			if (intersectionCars.containsKey(key)
					&& Utils.dist(intersectionCars.get(key).getPosition(),
							intersectionCars.get(key).getIntersectionPos()) > intersectionDist) {
				keys.add(key);
			}
		}

		for (Integer key : keys) {
			intersectionCars.remove(key);
		}

		if (carType == 0 || carType == 3 || carType == 4 || mixCars) {
			naiveIntersectionCars.clear();
			for (Road r : roads) {
				Car c = r.getClosesetToInterAndStopped();
				if(c != null) naiveIntersectionCars.add(c);
			}
			if(naiveIntersectionCars.size() == 4) {
				for(Road r : roads) {
					if(r.blockingInter()) {
						for(Car c : naiveIntersectionCars) {
							if (((c.getClass() == NaiveCar.class && ((NaiveCar)c).isDrivingAtInter())
								|| (c.getClass() == DistLearnCar.class && ((DistLearnCar)c).hasToken())
								|| (c.getClass() == NNLearnCar.class && ((NNLearnCar)c).hasToken())) && c.hasStopped()) c.definitelyDrive();
						}
						return;
					}
				}
				for(Car c : naiveIntersectionCars){
					if((c.getClass() == NaiveCar.class && ((NaiveCar)c).isDrivingAtInter())
						|| (c.getClass() == DistLearnCar.class && ((DistLearnCar)c).hasToken())
						|| (c.getClass() == NNLearnCar.class && ((NNLearnCar)c).hasToken())) {
						return;
					}
				}
				int choice = (int)(Math.random() * 4);
				Car c = naiveIntersectionCars.get(choice);
				if(c.getClass() == NaiveCar.class) {
					((NaiveCar)naiveIntersectionCars.get(choice)).driveAtInter();
				}
				else if(c.getClass() == DistLearnCar.class) {
					((DistLearnCar)c).canDrive();
				}
				else if(c.getClass() == NNLearnCar.class) {
					((NNLearnCar)c).canDrive();
				}
			}
		}
	}

	public ArrayList<Car> getNaiveInterCars() {
		return naiveIntersectionCars;
	}
}

// ############################### old code

//private static void experiment1() {
//	/*
//	 * - PREREQ: Cars driving at same speed; 1k runs each
//	 * - Case 1: Intersecting
//	 * 	- 1.1: 1 learning car, 1 naive car
//	 * 	- 1.2: 2 learning cars
//	 * 	- 1.3: 2 naive cars (base case)
//	 *  - 1.4: 1 learning, 1 naive, switched roads
//	 * - Case 2: Non-intersecting (to see if reward signal really works)
//	 * 	- 2.1: 1 learning car, 1 naive car
//	 * 	- 2.2: 2 learning cars
//	 * 	- 2.3: 2 naive cars (base case)
//	 * 
//	 */
//
//	//		Runner[] runners = new Runner[6];
//	//		
//	//		for(Runner r : runners) {
//	//			r = new Runner();
//	//		}
//	//
//	//		runners[0].runMode = 1;
//	//		runners[0].runMode = 2;
//	//		runners[0].runMode = 3;
//	//		runners[0].runMode = 4;
//	//		runners[0].runMode = 5;
//	//		runners[0].runMode = 6;
//	//		displayCars = true;
//	//		refreshRate = 100;
//	//		visualizer = new Vis2();
//
//	numRuns = 10000;
//	Runner r11 = new Runner(new int[] {0,1});
//	r11.setVelocity = 60;
//	r11.initDQT();
//	r11.start();
//
//	Runner r12 = new Runner(new int[] {1,1});
//	r12.setVelocity = 60;
//	r12.initDQT();
//	r12.start();
//	Runner r13 = new Runner(new int[] {0,0});
//	r13.runMode = 3;
//	r13.setVelocity = 60;
//	r13.initDQT();
//	r13.start();
//	Runner r14 = new Runner();
//	r14.runMode = 7;
//	r14.setVelocity = 60;
//	r14.initDQT();
//	r14.start();
//	Runner r21 = new Runner();
//	r21.runMode = 4;
//	r21.setVelocity = 60;
//	r21.initDQT();
//	r21.start();
//	Runner r22 = new Runner();
//	r22.runMode = 5;
//	r22.setVelocity = 60;
//	r22.initDQT();
//	r22.start();
//	Runner r23 = new Runner();
//	r23.runMode = 6;
//	r23.setVelocity = 60;
//	r23.initDQT();
//	r23.start();
//
//	while(threadCount.get() != 7) Thread.yield();
//	System.out.println("case 1.1");
//	r11.printData();
//	System.out.println("case 1.2");
//	r12.printData();
//	System.out.println("case 1.3");
//	r13.printData();
//	System.out.println("case 1.4");
//	r14.printData();
//	System.out.println("case 2.1");
//	r21.printData();
//	System.out.println("case 2.2");
//	r22.printData();
//	System.out.println("case 2.3");
//	r23.printData();		
//}



//private void mode8Epoch() {
//	step = 0; // Current step in the simulation
//	int carNum = 0; // Current number of cars in the simulation
//	do {
//		// Go through roads one by one and add cars
//		for (int i = 0; i < roads.length; i++) {
//			if (carNum < MAXCARS) {
//				carType = carTypesByPosition[carNum];
//				if(carType == 4 || carType == 3) setVelocity = 44;
//				else setVelocity = 60;
//				Car c = createCar(roads[i], carNum);
//				if (roads[i].addCar(c, step)) {
//					addCar(c);
//					carNum++;
//				}
//			}
//		}
//
//		// Display everything if specified
//		if (displayCars) {
//			try {
//				visualizer.repaint();
//				Thread.sleep(refreshRate);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		updateIntersectionCars(); // Update the list of cars which are at
//		// the intersection (this may be
//		// deprecated!!)
//		nextStep(); // Perform one step in the simulation
//		step++;
//	} while (step < MAXSTEPS && !allArrived() && !Runner.shouldTerminate());
//	if(dqt != null) dqt.adjustForCrashes();
//	if(nndqt != null) nndqt.adjustForCrashes();
//}	
//
//private void mode1Epoch() {
//	MAXCARS = 2;
//	step = 0; // Current step in the simulation
//	int carNum = 0; // Current number of cars in the simulation
//	do {
//		// Go through roads one by one and add cars
//		for (int i = 0; i < roads.length; i+=2) {
//			if (carNum < MAXCARS) {
//				if(carNum == 0) carType = 0;
//				else carType = 3;
//				Car c = createCar(roads[i], carNum);
//				if (roads[i].addCar(c, step)) {
//					addCar(c);
//					carNum++;
//				}
//			}
//		}
//
//		// Display everything if specified
//		if (displayCars) {
//			try {
//				visualizer.repaint();
//				Thread.sleep(refreshRate);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		updateIntersectionCars(); // Update the list of cars which are at
//		// the intersection (this may be
//		// deprecated!!)
//		nextStep(); // Perform one step in the simulation
//		step++;
//	} while (step < MAXSTEPS && !allArrived() && !Runner.shouldTerminate());
//	dqt.adjustForCrashes();
//}	
//
//private void mode2Epoch() {
//	MAXCARS = 2;
//	step = 0; // Current step in the simulation
//	int carNum = 0; // Current number of cars in the simulation
//	carType = 0;
//	do {
//		// Go through roads one by one and add cars
//		for (int i = 0; i < roads.length; i+=2) {
//			if (carNum < MAXCARS) {
//				Car c = createCar(roads[i], carNum);
//				if (roads[i].addCar(c, step)) {
//					addCar(c);
//					carNum++;
//				}
//			}
//		}
//
//		// Display everything if specified
//		if (displayCars) {
//			try {
//				visualizer.repaint();
//				Thread.sleep(refreshRate);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		updateIntersectionCars(); // Update the list of cars which are at
//		// the intersection (this may be
//		// deprecated!!)
//		nextStep(); // Perform one step in the simulation
//		step++;
//	} while (step < MAXSTEPS && !allArrived() && !Runner.shouldTerminate());
//	dqt.adjustForCrashes();		
//}
//
//private void mode3Epoch() {
//	MAXCARS = 2;
//	step = 0; // Current step in the simulation
//	int carNum = 0; // Current number of cars in the simulation
//	carType = 3;
//	do {
//		// Go through roads one by one and add cars
//		for (int i = 0; i < roads.length; i+=2) {
//			if (carNum < MAXCARS) {
//				Car c = createCar(roads[i], carNum);
//				if (roads[i].addCar(c, step)) {
//					addCar(c);
//					carNum++;
//				}
//			}
//		}
//
//		// Display everything if specified
//		if (displayCars) {
//			try {
//				visualizer.repaint();
//				Thread.sleep(refreshRate);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		updateIntersectionCars(); // Update the list of cars which are at
//		// the intersection (this may be
//		// deprecated!!)
//		nextStep(); // Perform one step in the simulation
//		step++;
//	} while (step < MAXSTEPS && !allArrived() && !Runner.shouldTerminate());
//	dqt.adjustForCrashes();
//}
//
//private void mode4Epoch() {
//	MAXCARS = 2;
//	step = 0; // Current step in the simulation
//	int carNum = 0; // Current number of cars in the simulation
//	do {
//		// Go through roads one by one and add cars
//		for (int i = 0; i < roads.length; i++) {
//			if (carNum < MAXCARS) {
//				if(carNum == 0) carType = 0;
//				else carType = 3;
//				Car c = createCar(roads[i], carNum);
//				if (roads[i].addCar(c, step)) {
//					addCar(c);
//					carNum++;
//				}
//			}
//		}
//
//		// Display everything if specified
//		if (displayCars) {
//			try {
//				visualizer.repaint();
//				Thread.sleep(refreshRate);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		updateIntersectionCars(); // Update the list of cars which are at
//		// the intersection (this may be
//		// deprecated!!)
//		nextStep(); // Perform one step in the simulation
//		step++;
//	} while (step < MAXSTEPS && !allArrived() && !Runner.shouldTerminate());
//	dqt.adjustForCrashes();
//}
//
//private void mode5Epoch() {
//	MAXCARS = 2;
//	step = 0; // Current step in the simulation
//	int carNum = 0; // Current number of cars in the simulation
//	carType = 0;
//	do {
//		// Go through roads one by one and add cars
//		for (int i = 0; i < roads.length; i++) {
//			if (carNum < MAXCARS) {
//				Car c = createCar(roads[i], carNum);
//				if (roads[i].addCar(c, step)) {
//					addCar(c);
//					carNum++;
//				}
//			}
//		}
//
//		// Display everything if specified
//		if (displayCars) {
//			try {
//				visualizer.repaint();
//				Thread.sleep(refreshRate);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		updateIntersectionCars(); // Update the list of cars which are at
//		// the intersection (this may be
//		// deprecated!!)
//		nextStep(); // Perform one step in the simulation
//		step++;
//	} while (step < MAXSTEPS && !allArrived() && !Runner.shouldTerminate());
//	dqt.adjustForCrashes();
//}
//
//private void mode6Epoch() {
//	MAXCARS = 2;
//	step = 0; // Current step in the simulation
//	int carNum = 0; // Current number of cars in the simulation
//	carType = 3;
//	do {
//		// Go through roads one by one and add cars
//		for (int i = 0; i < roads.length; i++) {
//			if (carNum < MAXCARS) {
//				Car c = createCar(roads[i], carNum);
//				if (roads[i].addCar(c, step)) {
//					addCar(c);
//					carNum++;
//				}
//			}
//		}
//
//		// Display everything if specified
//		if (displayCars) {
//			try {
//				visualizer.repaint();
//				Thread.sleep(refreshRate);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		updateIntersectionCars(); // Update the list of cars which are at
//		// the intersection (this may be
//		// deprecated!!)
//		nextStep(); // Perform one step in the simulation
//		step++;
//	} while (step < MAXSTEPS && !allArrived() && !Runner.shouldTerminate());
//	dqt.adjustForCrashes();		
//}
//
//private void mode7Epoch() {
//	MAXCARS = 2;
//	step = 0; // Current step in the simulation
//	int carNum = 0; // Current number of cars in the simulation
//	do {
//		// Go through roads one by one and add cars
//		for (int i = 0; i < roads.length; i+=2) {
//			if (carNum < MAXCARS) {
//				if(carNum == 0) carType = 3;
//				else carType = 0;
//				Car c = createCar(roads[i], carNum);
//				if (roads[i].addCar(c, step)) {
//					addCar(c);
//					carNum++;
//				}
//			}
//		}
//
//		// Display everything if specified
//		if (displayCars) {
//			try {
//				visualizer.repaint();
//				Thread.sleep(refreshRate);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		updateIntersectionCars(); // Update the list of cars which are at
//		// the intersection (this may be
//		// deprecated!!)
//		nextStep(); // Perform one step in the simulation
//		step++;
//	} while (step < MAXSTEPS && !allArrived() && !Runner.shouldTerminate());
//	dqt.adjustForCrashes();		
//}

// public static AtomicInteger a = new AtomicInteger(0);
// public static AtomicInteger b = new AtomicInteger(0);
// private float avgVel = 0;
// private boolean naiveCar;
// private static boolean thousandRuns = false;

// public float getAvgVel() {
// return avgVel;
// }

// public void useNaive(boolean choice) {
// naiveCar = choice;
// }

// public void newRun() {
// init();
//
// steps = 0;
// int carNum = 0;
//
// do {
//
// for(Road road : roads) {
// if(carNum < maxCars) {
// Car other = road.getLastAddedCar();
// Car c;
// if(other != null) {
// if(carType == 0){
// float otherVelo = other.getVelocity();
// float velocity = otherVelo + (otherVelo * -0.1f + otherVelo *
// (float)Math.random() * 0.2f);
// if(velocity <= 0) continue;
// c = new NaiveCar(road, 4, 2, velocity,(float)(( minVel + Math.random() *
// velDev )*10f/36f));
// ((NaiveCar)c).setRunner(this);
// } else if(carType == 1) {
// int velocity = (int)( minVel + Math.random() * velDev );
// c = new Car(road, 4, 2, velocity);
// } else {
// int velocity = (int)( minVel + Math.random() * velDev );
// c = new LearnCar(road, 4, 2, velocity, ass[carNum]);
// }
// }
// else {
// int velocity = (int)( minVel + Math.random() * velDev );
// if(carType == 0) {
// c = new NaiveCar(road, 4, 2, velocity);
// ((NaiveCar)c).setRunner(this);
// } else if(carType == 1){
// c = new Car(road, 4, 2, velocity);
// } else {
// c = new LearnCar(road, 4, 2, velocity, ass[carNum]);
// }
// }
//
// if(road.addCar(c,steps)) {
// addCar(c);
// carNum++;
// }
// }
// }
//
//
// updateIntersectionCars();
// nextStep();
// steps++;
//
// } while(steps < maxSteps && !allArrived() && !Runner.exit());
//
// data.setRefs(roads, allCars, carType);
// data.doStuff();
// threadCount.incrementAndGet();
//
// }
//
// public void run() {
// //newRun();
//
//
// Vis2 v = null;
// init();
//
// if(!thousandRuns) {
// v = new Vis2();
// addRoadsToVisualizer(v);
// }
//
// steps = 0;
// int carNum = 0;
// int rand = (int)(Math.random()*maxCars);
//
// //data.setRefs(roads, cars, 1);
// //avgVel = 0;
// do {
//
// for(Road road : roads) {
// if(carNum < maxCars) {
// Car other = road.getLastAddedCar();
// Car c;
// if(other != null) {
// if(naiveCar){
// float otherVelo = other.getVelocity();
// float velocity = otherVelo + (otherVelo * -0.1f + otherVelo *
// (float)Math.random() * 0.2f);
// if(velocity <= 0) continue;
// c = new NaiveCar(road, 4, 2, velocity,(float)(( minVel + Math.random() *
// velDev )*10f/36f));
// ((NaiveCar)c).setRunner(this);
// } else {
// int velocity = (int)( minVel + Math.random() * velDev );
// // if(steps == rand && learnerNum < 1) {
// // //System.out.println("Added learner");
// // c = new LearnCar(road, 4, 2, velocity,test);
// // //rand--;
// // //System.out.println(c.getClass());
// // }
// c = new Car(road, 4, 2, velocity);
// //c = new LearnCar(road, 4, 2, velocity, ass[carNum])
//
// }
// }
// else {
// int velocity = (int)( minVel + Math.random() * velDev );
// if(naiveCar) {
// c = new NaiveCar(road, 4, 2, velocity);
// ((NaiveCar)c).setRunner(this);
// } else {
// c = new Car(road, 4, 2, velocity);
// }
// }
//
// if(road.addCar(c,steps)) {
// //
// //System.exit(-1);
// //if(c.getClass().equals(LearnCar.class)) learnerNum++;
// addCar(c);
// carNum++;
// }
// // else if(steps == rand && learnerNum < 1) {
// // rand++;
// // }
// }
// }
//
// if(!thousandRuns) {
// v.setCars(this.cars);
// v.repaint();
// }
// updateIntersectionCars();
// nextStep();
//
// steps++;
//
// boolean allCarsStopped = true;
// for(Car c : cars) {
// if(c.getVelocity() > 0) {
// allCarsStopped = false;
// break;
// }
// }
// if(allCarsStopped) {
// for(Car c : cars) {
// //c.printVelocityHist();
// c.printVelCalc = true;
// }
// }
// if(!thousandRuns) {
// try {
// Thread.sleep(100);
// } catch (InterruptedException e) {
// // TODO Auto-generated catch block
// e.printStackTrace();
// }
// }
// } while(steps < maxSteps && !allArrived() && !Runner.exit());
// //System.out.println("Finished in " + steps/60 + " minutes and " + steps%60 +
// " seconds simulated time, with " + Utils.getNumCollisions() +
// " collision(s)");
// //System.exit(0);
//
// avgVel/=(float)maxCars;
// //System.out.println(avgVel);
// if(naiveCar) {
// a.addAndGet(steps);
// //avgNaiveVelo.set(avgNaiveVelo.get() + avg);
// }
// else {
// b.addAndGet(steps);
// }
//
// data.setRefs(roads, allCars, 0);
// data.doStuff();
// threadCount.incrementAndGet();
//
// }

//
// String s = "1: Car type, "+ "2: Number of cars, " +
// ", 3: avg travel time, 4: avg stopping time, 5: avg velocity, 6: avg variance in speed, 7: avg acceleration/deceleration, 8: avg velocity as % of desired speed, 9: avg road throughput, 10: avg collisions\n";
// for(carType = 0; carType < 3; carType++) {
// int max = 1000;
// if(carType == 2) max = 10000;
// for(int i = 0; i < max; i++) {
// Runner test = new Runner();
// test.useNaive(false);
// test.start();
// do {
// try {
// Thread.sleep(1);
// } catch (InterruptedException e) {
// e.printStackTrace();
// }
// } while (test.isAlive());
// }
// s += data.aggregateAndPrint();
// data.printShit();
// data.reset();
// }
// System.out.println(s);

/*
 * public static void main(String[] args) { //avgIntelVelo = new
 * Utils.AtomicFloat(0); if(!thousandRuns) { thousandRuns = true; ass = new
 * ActionSelector[maxCars]; for(int i = 0; i < maxCars; i++) { ass[i] = new
 * ActionSelector(6, 1); } for(int i = 0; i < 100; i++) { Runner test = new
 * Runner(); test.useNaive(false); test.start();
 * 
 * System.out.println(i); do { try { Thread.sleep(1); } catch
 * (InterruptedException e) { // TODO Auto-generated catch block
 * e.printStackTrace(); } } while (test.isAlive());
 * //System.out.println(test.isAlive()); }
 * 
 * // Scanner sc = new Scanner(System.in); // // while(!sc.hasNext()) { // try {
 * // Thread.sleep(1); // } catch (InterruptedException e) { // // TODO
 * Auto-generated catch block // e.printStackTrace(); // } // } // //
 * thousandRuns = false; // Runner test = new Runner(); // test.useNaive(false);
 * // test.start(); // //// Console c = System.console(); //// String bla =
 * c.readLine("test"); // // // do { // try { // Thread.sleep(1); // } catch
 * (InterruptedException e) { // // TODO Auto-generated catch block //
 * e.printStackTrace(); // } // } while (test.isAlive());
 * //System.out.println(test.getAvgVel()); data.printShit(); System.exit(0);
 * 
 * } else { ArrayList<Runner> naiveRunners = new ArrayList<>(1000);
 * ArrayList<Runner> intelRunners = new ArrayList<>(1000);
 * 
 * for(int i = 0; i < 1000; i++) { Runner runnerA = new Runner();
 * runnerA.useNaive(true); naiveRunners.add(runnerA); runnerA.start(); Runner
 * runnerB = new Runner(); runnerB.useNaive(false); intelRunners.add(runnerB);
 * runnerB.start(); }
 * 
 * while(threadCount.get() < 2000) { try { Thread.sleep(1000); } catch
 * (InterruptedException e) { // TODO Auto-generated catch block
 * e.printStackTrace(); } }
 * 
 * float c = a.get()/1000f; float d = b.get()/1000f;
 * 
 * System.out.println("AvgTimeNaive: " + c);
 * System.out.println("AvgTimeBetter: " + d);
 * 
 * float naiveAvg = 0; float intelAvg = 0; for(Runner r : naiveRunners) {
 * naiveAvg += r.getAvgVel(); } for(Runner r : intelRunners) { intelAvg +=
 * r.getAvgVel(); } naiveAvg /= (float)naiveRunners.size(); intelAvg /=
 * (float)intelRunners.size();
 * 
 * System.out.println("avgNaiveSpeed: " + naiveAvg);
 * System.out.println("avgintelSpeed: " + intelAvg);
 * 
 * } }
 */

