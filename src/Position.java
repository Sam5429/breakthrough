public class Position {

	public final int x;
	public final int y;

	public Position(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public Position(String pos) {
		this.x = pos.charAt(0) - 'A';
		this.y = 8 - Character.getNumericValue(pos.charAt(1));
	}

	@Override
	public String toString() {
		return "" + (char) ('A' + x) + (8 - y);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Position)) return false;
		Position pos = (Position) obj;
		return x == pos.x && y == pos.y;
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(x, y);
	}
}
