public class ActionHistEntry {

	private final int[] state;
	private final int action;

	public ActionHistEntry(int[] state, int action) {
		this.state = state;
		this.action = action;
	}

	public int[] getState() {
		return state;
	}
	
	public int getStateAsInt() {
		String s = "";
		for (int i : state) s += i;
		return Integer.parseInt(s);
	}

	public int getAction() {
		return action;
	}

}
