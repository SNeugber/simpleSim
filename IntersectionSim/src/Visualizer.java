import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;

import javax.swing.JPanel;

public class Visualizer extends JPanel {

	private static final long serialVersionUID = -5622357231631476698L;
	private Road[] roads;
	private Dimension size;
	private Dimension roadBounds;
	private int xOffset, yOffset;
	private ArrayList<Car> cars;

	public Visualizer() {
		size = new Dimension(1600, 900);
		setMinimumSize(size);
		cars = new ArrayList<Car>();

	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setBackground(new Color(10, 250, 10));
		g2d.setColor(new Color(0, 0, 255));

		try {
			for (Road r : roads) {
				g2d.drawLine(
						(int) (size.width / (float) roadBounds.width * (xOffset + r
								.getStart()[0])),
						(int) (size.height / (float) roadBounds.height * (yOffset + r
								.getStart()[1])),
						(int) (size.width / (float) roadBounds.width * (xOffset + r
								.getEnd()[0])),
						(int) (size.height / (float) roadBounds.height * (yOffset + r
								.getEnd()[1])));
			}
		} catch (Exception e) {
			//System.out.println("road null");
		}

		try {
			for (Car r : cars) {
				if (r.beenInCrash() && !r.getClass().equals(LearnCar.class))
					g2d.setColor(new Color(255, 0, 0));
				else if (r.getClass().equals(LearnCar.class) || r.getClass().equals(DistLearnCar.class))
					g2d.setColor(new Color(10, 10, 255));
				else
					g2d.setColor(new Color(0, 150, 100));
				g2d.fillOval(
						(int) (size.width / (float) roadBounds.width * (xOffset + r
								.getPosition()[0])), (int) (size.height
								/ (float) roadBounds.height * (yOffset + r
								.getPosition()[1])), 4, 4);
			}
		} catch (Exception e) {
			System.out.println("Car lists modified while trying to display");
		}

	}

	public void addRoads(Road[] roads) {
		this.roads = roads;
		float[] bounds = Utils.findRoadBounds(roads);
		roadBounds = new Dimension((int) (bounds[0] - bounds[1]),
				(int) (bounds[2] - bounds[3]));
		xOffset = -1 * (int) bounds[1];
		yOffset = -1 * (int) bounds[3];
	}

	public void setCars(ArrayList<Car> cars) {
		this.cars = cars;
	}

}
