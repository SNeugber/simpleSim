import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class DataAggregator { 

	private Road[] roads;
	private ArrayList<Car> cars;

	private ArrayList<Float> avgTravelTimes = new ArrayList<Float>();
	private ArrayList<Float> avgStopTimes = new ArrayList<Float>();
	private ArrayList<Float> avgVelocities = new ArrayList<Float>();
	private ArrayList<Float> avgAccelerations = new ArrayList<Float>();
	private ArrayList<Float> desiredSpeedDiffs = new ArrayList<Float>();
	private ArrayList<Float> avgRoadThroughput = new ArrayList<Float>();
	private ArrayList<Float> avgSpeedVariance = new ArrayList<Float>();

	private ArrayList<Integer> numCars = new ArrayList<Integer>();
	private ArrayList<Integer> carTypeHist = new ArrayList<Integer>();
	private ArrayList<Integer> collisionHist = new ArrayList<Integer>();

	private ArrayList<Integer> carTypeCounts = new ArrayList<Integer>();


	private int minCollision = Integer.MAX_VALUE;
	private float minTravelTime = Float.MAX_VALUE;
	private float minSpeedVar = Float.MAX_VALUE;

	private float runs = 0;

	private static ChartWindow demo;
	private static long timeSinceLastUpdate;
	private float runningAvg = 0;
	private int dataSeries;

	public DataAggregator(int dataSeries) {
		for(int i = 0; i < Runner.NUMCARTYPES; i++) {
			carTypeCounts.add(0);
		}
		timeSinceLastUpdate = System.currentTimeMillis();
		this.dataSeries = dataSeries;
	}

	public void setRefs(Road[] roads, ArrayList<Car> cars, int carType) {
		this.roads = roads;
		this.cars = cars;
		carTypeHist.add(carType);
	}

	public void reset() {
		avgTravelTimes = new ArrayList<Float>();
		avgStopTimes = new ArrayList<Float>();
		avgVelocities = new ArrayList<Float>();
		avgAccelerations = new ArrayList<Float>();
		desiredSpeedDiffs = new ArrayList<Float>();
		avgRoadThroughput = new ArrayList<Float>();
		avgSpeedVariance = new ArrayList<Float>();

		numCars = new ArrayList<Integer>();
		carTypeHist = new ArrayList<Integer>();
		collisionHist = new ArrayList<Integer>();

		carTypeCounts = new ArrayList<Integer>();
		for(int i = 0; i < Runner.NUMCARTYPES; i++) {
			carTypeCounts.add(0);
		}

		runs = 0;
	}

	/**
	 * Calculates the data for a current set of cars and appends that data to
	 * the according list
	 */
	public boolean pushData() {

		// AVERAGES
		float avgVel = 0;
		float avgAccel = 0;
		float avgStopTime = 0;
		float avgTravelTime = 0;
		float desiredSpeedDiff = 0;

		int collisionNum = 0;

		for (Car c : cars) {
			avgVel += getAvg(c.getVelLog());
			avgAccel += getAvg(c.getAcellLog());
			avgStopTime += c.getStopTime();
			avgTravelTime += c.getTravelTime();
			desiredSpeedDiff += avgSpeedDiff(c.getVelLog(),
					c.getDesiredVelocity());
			if (c.beenInCrash())
				collisionNum++;

			if(c.getClass() == NaiveCar.class) {
				carTypeCounts.set(0, carTypeCounts.get(0)+1);
			} else if(c.getClass() == YieldCar.class) {
				carTypeCounts.set(1, carTypeCounts.get(1)+1);
			} else if(c.getClass() == LearnCar.class) {
				carTypeCounts.set(2, carTypeCounts.get(2)+1);
			} else if(c.getClass() == DistLearnCar.class) {
				carTypeCounts.set(3, carTypeCounts.get(3)+1);
			}
		}
		collisionHist.add((collisionNum = collisionNum / 2));

		desiredSpeedDiffs.add(desiredSpeedDiff / (float) cars.size());
		avgVelocities.add(avgVel / (float) cars.size());
		avgAccelerations.add(avgAccel / (float) cars.size());
		avgStopTimes.add(avgStopTime / (float) cars.size());
		avgTravelTimes.add((avgTravelTime = avgTravelTime / (float) cars.size()));

		// STANDARD DEVIATIONS^
		float avgSpeedStDev = 0;
		for (Car c : cars) {
			avgSpeedStDev += getStdDev(c.getVelLog(), getAvg(c.getVelLog()));
		}
		avgSpeedVariance.add((avgSpeedStDev = avgSpeedStDev / (float) cars.size()));

		float avgThroughput = 0;
		for (Road r : roads) {
			avgThroughput += r.getNumCars() / (float) r.getOnRoadTime();
		}
		avgRoadThroughput.add(avgThroughput / (float) roads.length);

		numCars.add(cars.size());

		if(runningAvg == 0) runningAvg = avgTravelTime;
		runningAvg = 0.9f*runningAvg + 0.1f*(avgTravelTime);
		if(dataSeries == -1) {
			demo.addData(0, (int)runs, runningAvg);
			demo.addData(1, (int)runs, collisionNum);
		}
		else {
			demo.addData(dataSeries, (int)runs, runningAvg);
		}
		runs++;

		//		if(System.currentTimeMillis() - timeSinceLastUpdate > 1000) {
		//			timeSinceLastUpdate = System.currentTimeMillis();
		//			demo.refresh();
		//		}

		if(collisionNum <= minCollision && avgTravelTime <= minTravelTime && avgSpeedStDev < minSpeedVar) {
			minCollision = collisionNum;
			minTravelTime = avgTravelTime;
			minSpeedVar = avgSpeedStDev;
			return true;
		}
		return false;
		// collisionHist.add(Utils.getNumCollisions());
		// Utils.resetCollisions();
	}

	public static void initChart(int numSeries, String name, String[] seriesNames, String fileName) {
		demo = new ChartWindow(name, numSeries, seriesNames, fileName);
		demo.refresh();
	}

	public boolean pushData(Car c) {

		// AVERAGES
		//		float avgVel = 0;
		//		float avgAccel = 0;
		//		float avgStopTime = 0;
		float avgTravelTime = 0;
		//		float desiredSpeedDiff = 0;

		int collisionNum = 0;

		//			avgVel += getAvg(c.getVelLog());
		//			avgAccel += getAvg(c.getAcellLog());
		//			avgStopTime += c.getStopTime();
		avgTravelTime += c.getTravelTime();
		//			desiredSpeedDiff += avgSpeedDiff(c.getVelLog(),c.getDesiredVelocity());
		//			if (c.beenInCrash())
		//				collisionNum++;

		//			if(c.getClass() == NaiveCar.class) {
		//				carTypeCounts.set(0, carTypeCounts.get(0)+1);
		//			} else if(c.getClass() == YieldCar.class) {
		//				carTypeCounts.set(1, carTypeCounts.get(1)+1);
		//			} else if(c.getClass() == LearnCar.class) {
		//				carTypeCounts.set(2, carTypeCounts.get(2)+1);
		//			} else if(c.getClass() == DistLearnCar.class) {
		//				carTypeCounts.set(3, carTypeCounts.get(3)+1);
		//			}
		//		collisionHist.add((collisionNum = collisionNum / 2));

		//		desiredSpeedDiffs.add(desiredSpeedDiff / (float) cars.size());
		//		avgVelocities.add(avgVel / (float) cars.size());
		//		avgAccelerations.add(avgAccel / (float) cars.size());
		//		avgStopTimes.add(avgStopTime / (float) cars.size());
		avgTravelTimes.add(avgTravelTime);

		// STANDARD DEVIATIONS^
		//		float avgSpeedStDev = 0;
		//			avgSpeedStDev += getStdDev(c.getVelLog(), getAvg(c.getVelLog()));
		//		avgSpeedVariance.add((avgSpeedStDev = avgSpeedStDev / (float) cars.size()));

		//		float avgThroughput = 0;
		//		for (Road r : roads) {
		//			avgThroughput += r.getNumCars() / (float) r.getOnRoadTime();
		//		}
		//		avgRoadThroughput.add(avgThroughput / (float) roads.length);
		//
		//		numCars.add(cars.size());
		avgTravelTime = getAvg(avgTravelTimes);
		//		if(runningAvg == 0) runningAvg = avgTravelTime;
		//		runningAvg = 0.9f*runningAvg + 0.1f*(avgTravelTime);
		demo.addData(dataSeries, (int)runs, avgTravelTime);
		runs++;

		//		if(System.currentTimeMillis() - timeSinceLastUpdate > 1000) {
		//			timeSinceLastUpdate = System.currentTimeMillis();
		//			demo.refresh();
		//		}

		//		if(collisionNum <= minCollision && avgTravelTime/(float)cars.size() <= minTravelTime && avgSpeedStDev/(float)cars.size() < minSpeedVar) {
		//			minCollision = collisionNum;
		//			minTravelTime = avgTravelTime/(float)cars.size();
		//			minSpeedVar = avgSpeedStDev/(float)cars.size();
		//			return true;
		//		}
		return false;
		// collisionHist.add(Utils.getNumCollisions());
		// Utils.resetCollisions();
	}

	/**
	 * Prints all the data that has been collected so far
	 */
	public void printEntireDataSet() {
		System.out.println("1: Car type, 2: # of Cars, 3:  avg travel time, 4: avg time stopped, 5: avg velocity, 6: avg variance in speed, 7: avg acceleration/deceleration, 8: avg velocity as % of desired speed, 9: avg road throughput, 10: # of collisions, 11: % of collided cars");
		for (int i = 0; i < avgTravelTimes.size(); i++) {
			System.out.print(carTypeHist.get(i) + ", ");
			System.out.print(numCars.get(i) + ", ");
			System.out.print(avgTravelTimes.get(i) + ", ");
			System.out.print(avgStopTimes.get(i) + ", ");
			System.out.print(avgVelocities.get(i) + ", ");
			System.out.print(avgSpeedVariance.get(i) + ", ");
			System.out.print(avgAccelerations.get(i) + ", ");
			System.out.print(desiredSpeedDiffs.get(i) + ", ");
			System.out.print(avgRoadThroughput.get(i) + ", ");
			System.out.print(collisionHist.get(i) + ", ");
			System.out.print(collisionHist.get(i)*2f/(float)numCars.get(i));
			System.out.println();
		}
		Utils.resetCollisions();
		// System.out.println();
	}

	/**
	 * Calculates the averages etc. over all the data that has been collected so
	 * far
	 * 
	 * @return The results as a formatted string
	 */
	public String aggregateAndPrint() {

		float avgNumCars = 0;
		float avgTravelTime = 0;
		float avgStopTime = 0;
		float avgVelocity = 0;
		float avgSpeedVar = 0;
		float avgAccel = 0;
		float avgDesSpeedDiff = 0;
		float avgRoadThrough = 0;
		float avgColls = 0;
		float avgCollPercent = 0;

		for (int i = 0; i < avgTravelTimes.size(); i++) {

			avgNumCars += numCars.get(i);
			avgTravelTime += avgTravelTimes.get(i);
			avgStopTime += avgStopTimes.get(i);
			avgVelocity += avgVelocities.get(i);
			avgSpeedVar += avgSpeedVariance.get(i);
			avgAccel += avgAccelerations.get(i);
			avgDesSpeedDiff += desiredSpeedDiffs.get(i);
			avgRoadThrough += avgRoadThroughput.get(i);
			avgColls += collisionHist.get(i);
			avgCollPercent += collisionHist.get(i)*2f/(float)numCars.get(i);

		}

		avgNumCars /= runs;
		avgTravelTime /= runs;
		avgStopTime /= runs;
		avgVelocity /= runs;
		avgSpeedVar /= runs;
		avgAccel /= runs;
		avgDesSpeedDiff /= runs;
		avgRoadThrough /= runs;
		avgColls /= runs;
		avgCollPercent /= runs;

		String out = "1: # of Cars, 2:  avg travel time, 3: avg time stopped, 4: avg velocity, 5: avg variance in speed, 6: avg acceleration/deceleration, 7: avg velocity as % of desired speed, 8: avg road throughput, 9: # of collisions, 10: % of collided cars";

		for(int i = 0; i < carTypeCounts.size(); i++) {
			out += ", " + (11+i) + ": # of cars of type " + i;
		}
		out += "\n";

		out += avgNumCars + ", "
				+ avgTravelTime + ", " + avgStopTime + ", " + avgVelocity
				+ ", " + avgSpeedVar + ", " + avgAccel + ", " + avgDesSpeedDiff
				+ ", " + avgRoadThrough + ", " + avgColls + ", " + avgCollPercent;

		for(Integer i : carTypeCounts) {
			out += ", " + (i/runs);
		}

		out += "\n\n";
		out += "minTravelTime" + ", minSpeedVar: " + ", minCrashes: " + "\n";  
		out += minTravelTime + ", " + minSpeedVar + ", " + minCollision + "\n";
		return out;
	}

	private float avgSpeedDiff(ArrayList<Float> vels, float desVel) {
		float avg = 0;
		for (float v : vels) {
			avg += 1.0f - Math.abs(desVel - v) / desVel;
		}
		return avg / (float) vels.size();
	}

	private float getAvg(ArrayList<Float> vals) {
		float avg = 0;
		for (float f : vals) {
			avg += Math.abs(f);
		}
		avg /= (float) vals.size();
		return avg;
	}

	private float getStdDev(ArrayList<Float> vals, float avg) {
		double stdev = 0;
		for (float f : vals) {
			stdev += Math.pow(f - avg, 2);
		}
		return (float) Math.sqrt(stdev / (double) vals.size());
	}

	public float getScore() {


		return 0;

	}

	public void saveImage() {
		try {
			File out = new File(demo.getFileName() + ".png");
			int count = 1;
			while(out.exists()) {
				out = new File(demo.getFileName() + "__" + (count++) + ".png");
			}
			org.jfree.chart.ChartUtilities.saveChartAsPNG(out, demo.getChart(), 1200, 900);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void killChart() {
		demo.dispose();
	}

}
