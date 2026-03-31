public class Move {

	public final Position init;
	public final Position target;

	public Move(Position init, Position target) {
		this.init = init;
		this.target = target;
	}

	public Move(String move) {
		String[] parts = move.split(" - ");
		this.init = new Position(parts[0]);
		this.target = new Position(parts[1]);
	}

	@Override
	public String toString() {
		return (init.toString() + " - " + target.toString());
	}
}
