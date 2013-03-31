import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class Utils {

	private static int collisions = 0;

	public static void resetCollisions() {
		collisions = 0;
	}

	public static double dist(float[] a, float[] b) {
		double d = 0;
		if (a.length != b.length)
			return 0;
		for (int i = 0; i < a.length; i++) {
			d += Math.pow((a[i] - b[i]), 2);
		}
		return Math.sqrt(d);
	}

	public static boolean withinSecureDist(Car self, Car inFront) {

		float selfRoadFract = self.getPositionInPercent() + 10
				* Math.abs(self.getVelocity() / self.getRoad().getLength());
		float otherRoadFract = inFront.getPositionInPercent();

		if (selfRoadFract < otherRoadFract)
			return true;
		else
			return false;
	}

	public static float dot(float[] a, float[] b) {
		float out = 0;
		if (a.length != b.length)
			return 0;
		for (int i = 0; i < a.length; i++) {
			out += a[i] * b[i];
		}

		return out;
	}

	public static float[] minus(float[] a, float[] b) {
		if (a.length != b.length)
			return null;
		float[] out = new float[a.length];
		for (int i = 0; i < a.length; i++) {
			out[i] = a[i] - b[i];
		}

		return out;
	}

	public static float[] findRoadBounds(Road[] roads) {
		if (roads.length == 0)
			return null;

		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;
		for (Road r : roads) {
			if (r.getStart()[0] > maxX)
				maxX = r.getStart()[0];
			if (r.getEnd()[0] > maxX)
				maxX = r.getEnd()[0];
			if (r.getStart()[0] < minX)
				minX = r.getStart()[0];
			if (r.getEnd()[0] < minX)
				minX = r.getEnd()[0];

			if (r.getStart()[1] > maxY)
				maxY = r.getStart()[1];
			if (r.getEnd()[1] > maxY)
				maxY = r.getEnd()[1];
			if (r.getStart()[1] < minY)
				minY = r.getStart()[1];
			if (r.getEnd()[1] < minY)
				minY = r.getEnd()[1];
		}

		return new float[] { maxX, minX, maxY, minY };
	}

	public boolean willCollide(Car a, Car b) {

		return false;
	}

	public static void checkCollisions(HashMap<Integer, Car> cars) {
		int collisions = 0;
		Set<Integer> keySet = cars.keySet();
		for (Integer i : keySet) {
			for (Integer j : keySet) {
				if (i == j)
					continue;

				Car a = cars.get(i);
				Car b = cars.get(j);
				if (dist(a.getPosition(), b.getPosition()) < 7) {
					a.crash();
					b.crash();
					// System.out.println("Collision between Car: " +
					// a.hashCode() + " and Car: " + b.hashCode());
				}
			}
		}
		// System.out.println("Collisions: " + collisions);
	}

	public static void checkCollisions(ArrayList<Car> cars) {
		for (Car a : cars) {
			for (Car b : cars) {
				if (a.equals(b)) continue;
				
				if (dist(a.getPosition(), b.getPosition()) < (a.getCarLength()
						/ 2f + b.getCarLength() / 2f)) {
					a.crash();
					b.crash();
					// System.out.println("Collision between Car: " +
					// a.hashCode() + " and Car: " + b.hashCode());
					// a.printPositionHist();
					// b.printPositionHist();
					// a.printVelocityHist();
					// b.printVelocityHist();
					// System.out.println("breakpoint");
					// //a.printVelDiff();
					// //b.printVelDiff();
					if (!a.checkAndAdd(b.hashCode())) {
						b.checkAndAdd(a.hashCode());
						collisions++;
					}
				}
			}
		}
		// System.out.println("Collisions: " + collisions);
	}
	
	public static int arrayToInt(int[] in) {
		String s = "";
		for (int i : in) s += i;
		return Integer.parseInt(s);
	}
	
//	public static void decideIntersectionNew(ArrayList<Car> cars) {
//		if(cars.size() == 4) {
//			for(Car c : cars) {
//				if(((NaiveCar) c).hasToken());
//			}
//		}
//	}

	public static Car[] decideIntersection(ArrayList<Car> cars) {

		ArrayList<Car> canDrive = new ArrayList<Car>();
		if (cars.size() == 0)
			return new Car[0];
		else if (cars.size() == 1)
			return new Car[] { cars.get(0) };
		else {
			for (int i = 0; i < cars.size(); i++) {
				boolean go = true;
				for (int j = 0; j < cars.size(); j++) {
					if (i == j)
						continue;
					// float orientDiff = cars.get(i).getOrientation() -
					// cars.get(j).getOrientation();
					float x = cars.get(i).getOrientation() + 90;
					if (x >= 360)
						x -= 360;
					if (x == cars.get(j).getOrientation()) {
						// System.out.println("Conflict here");
						go = false;
						break;
					}
				}
				if (go) {
					canDrive.add(cars.get(i));
					// ((NaiveCar)cars.get(i)).driveAtInter();
				}
			}
		}
		// Car[] out = canDrive.toArray(new Car[canDrive.size()]);
		// for(Car c : out) {
		// System.out.println(c.hashCode());
		// }
		return (Car[]) canDrive.toArray(new Car[canDrive.size()]);
	}

	public static int getNumCollisions() {
		return collisions;
	}

}
