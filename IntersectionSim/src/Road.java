import java.util.ArrayList;

public class Road {

	private float[] start;
	private float[] end;
	private float[] intersectionPos;
	private float intersectionPosAsRoadFract;
	private float length;
	private ArrayList<Car> cars;
	private int startTime = 0;
	private int endTime = 2000;
	private int carNum;
	private Road roadToRight;
	private Road[] intersectingRoads;

	private Road() {
	};

	public Road(float[] start, float[] end) {
		this.start = start;
		this.end = end;
		length = (float) Math.sqrt(Math.pow((end[0] - start[0]), 2)
				+ Math.pow((end[1] - start[1]), 2));
		cars = new ArrayList<Car>();
	}
	
	public void setIntersectingRoads(Road roadToRight, Road[] intersectingRoads) {
		this.roadToRight = roadToRight;
		this.intersectingRoads = intersectingRoads;
	}

	public void setIntersection(float[] intersectionPos) {

		this.intersectionPos = intersectionPos;
		float dist = (float) Math.sqrt(Math.pow(
				(intersectionPos[0] - start[0]), 2)
				+ Math.pow((intersectionPos[1] - start[1]), 2));

		this.intersectionPosAsRoadFract = dist / length;
	}

	public float[] getIntersectionPos() {
		return intersectionPos;
	}

	public float getIntersectionPosAsRoadFract() {
		return intersectionPosAsRoadFract;
	}

	public float[] getStart() {
		return start;
	}

	public float[] getEnd() {
		return end;
	}

	public float getLength() {
		return length;
	}

	public boolean equals(Road other) {
		for (int i = 0; i < 2; i++) {
			if (start[i] != other.getStart()[i] || end[i] != other.getEnd()[i])
				return false;
		}
		return true;
	}

	public boolean addCar(Car c, int time) {
		if (cars.isEmpty()) {
			startTime = time;
			cars.add(c);
			carNum++;
			return true;
		} else if (c.predictPositionInPercent(3) < cars.get(cars.size() - 1)
				.getPositionInPercent()
				&& Utils.dist(c.getPosition(), cars.get(cars.size() - 1)
						.getPosition()) > c.getCarLength() / 2f + 10f
						+ cars.get(cars.size() - 1).getCarLength() / 2f) {
			cars.add(c);
			carNum++;
			return true;
		} else
			return false;
	}

	public boolean hasCarInFront(Car c) {
		if (cars.contains(c) && cars.indexOf(c) > 0)
			return true;
		return false;
	}

	public Car getCarInFront(Car self) {
		// Sanity check:
		for(int i = 0; i < cars.size()-1; i++) {
			if(cars.get(i).getPositionInPercent() < cars.get(i+1).getPositionInPercent()) {
				cars.get(i).printPositionHist();
				cars.get(i+1).printPositionHist();
				cars.get(i).printVelocityHist();
				cars.get(i+1).printVelocityHist();

				System.err.println("###FATAL_ERROR### Car got in front of one it shouldn't be in front! Exiting system, this needs fixing ASAP!");
				System.exit(0);
			}
		}
		
		if (cars.contains(self) && cars.indexOf(self) > 0)
			return cars.get(cars.indexOf(self) - 1);
		else
			return null;
	}
	
	public Car getClosesetToInterAndStopped() {
		Car closest = null;
		float dist = length;
		for (Car a : cars) {
			if(a.hasStopped()) {
				float d = intersectionPosAsRoadFract - a.getPositionInPercent();
				if (d * length > 0 && d * length < 10 && d < dist) {
					closest = a;
					dist = d;
				}
			}
		}
		return closest;
	}

	public boolean isClosestToInter(Car c) {
		Car closest = null;
		float dist = length;
		for (Car a : cars) {
			float d = intersectionPosAsRoadFract - a.getPositionInPercent();
			if (d * length > 0 && d < dist) {
				closest = a;
				dist = d;
			}
		}
		if (c.position[0] == closest.position[0] && c.position[1] == closest.position[1]) {
			System.out.println("closest");
			return true;
		}
		return false;
	}

	public void removeArrived(Car c, int time) {
		if (cars.contains(c)) {
			// System.out.println(cars.indexOf(c));
			cars.remove(c);
		}
		if (cars.isEmpty()) {
			endTime = time;
		}
	}

	public Car getLastAddedCar() {
		if (cars.size() > 0)
			return cars.get(cars.size() - 1);
		else
			return null;
	}

	public int getOnRoadTime() {
		return endTime - startTime;
	}

	public int getNumCars() {
		return carNum;
	}
	
	public Road getRoadToRight() {
		return roadToRight;
	}
	
	public Car getCarAtIntersection() {
		for(Car c : cars) {
			if(c.atInter()) return c;
//			if(c.getClass() == NaiveCar.class && ((NaiveCar)c).atInter()) return c;
//			else if(c.getClass() == DistLearnCar.class && ((DistLearnCar)c).getState()[1] == 3) return c;
		}
		return null;
	}
	
	public boolean interBlocked() {
		for(Road r : intersectingRoads) {
			if(r.blockingInter()) return true;
		}
		return false;
	}
	
	public boolean blockingInter() {
		for(Car c : cars) {
			if(c.blockingInter()) return true;
		}
		return false;
	}
}
