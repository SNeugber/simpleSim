import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.JFrame;

public class Vis2 extends JFrame {

	Visualizer v;

	public Vis2() {
		setSize(1700, 1000);
		setVisible(true);
		v = new Visualizer();
		v.setVisible(true);
		// v.setSize(800, 500);
		add(v);
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(WindowEvent winEvt) {
				Runner.quit();
			}
		});
	}

	public void addRoads(Road[] r) {
		v.addRoads(r);
	}

	public void setCars(ArrayList<Car> cars) {
		v.setCars(cars);
	}

	public void repaint() {
		v.repaint();
	}
}
