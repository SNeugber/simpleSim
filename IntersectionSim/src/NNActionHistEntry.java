
public class NNActionHistEntry {
	
	private final double[] state;
	private final int action;

	public NNActionHistEntry(double[] state, int action) {
		this.state = state;
		this.action = action;
	}

	public double[] getState() {
		return state;
	}

	public int getAction() {
		return action;
	}

}
