public class Position {

	public final int x;
	public final int y;

	public Position(int x, int y) {
		this.x = x;
		this.y = y;
	}

	// CRITIQUE : Zéro validation d'entrée. Si la chaîne 'pos' passée en paramètre fait moins de 2 caractères ou est nulle, c'est un crash immédiat (StringIndexOutOfBoundsException ou NullPointerException). Sécuriser ses API est élémentaire.
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
