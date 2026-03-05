import java.util.ArrayList;
import java.util.List;

class Board implements Cloneable {

	private Mark _board[][] = new Mark[8][8];

	public Board() {
		for (int i = 0; i < _board.length; i++) {
			for (int j = 0; j < _board[i].length; j++) {
				if (i < 2) {
					_board[i][j] = Mark.B;
				} else if (i > 5) {
					_board[i][j] = Mark.R;
				} else {
					_board[i][j] = Mark.EMPTY;
				}
			}
		}
	}

	@Override
	public Board clone() {
		Board cloned;
		try {
			cloned = (Board) super.clone();
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException("superclass messed up", ex);
		}
		cloned._board = new Mark[_board.length][];
		for (int i = 0; i < _board.length; i++) {
			cloned._board[i] = _board[i].clone();
		}
		return cloned;
	}

	public List<Move> getMoves(Mark o) {
		ArrayList<Move> moves = new ArrayList<>();
		for (int i = 0; i < _board.length; i++) {
			for (int j = 0; j < _board[i].length; j++) {
				if (_board[i][j] == o) {
					List<Move> currentMoves = getMoves(new Position(j, i));
					for (Move move : currentMoves) {
						moves.add(move);
					}
				}
			}
		}
		return moves;
	}

	public List<Move> getMoves(Position p) {
		Mark o = _board[p.y()][p.x()];
		int[][] moves;
		if (o == Mark.B) {
			moves = new int[][] { { -1, 1 }, { 0, 1 }, { 1, 1 } };
		} else {
			moves = new int[][] { { -1, -1 }, { 0, -1 }, { 1, -1 } };
		}
		ArrayList<Move> nextMoves = new ArrayList<>();
		for (int[] move : moves) {
			int next_x = p.x() + move[0];
			int next_y = p.y() + move[1];

			// vérifie si pas outofbound
			if (
				(next_y >= 0 && next_y < _board.length) &&
				(next_x >= 0 && next_x < _board[next_y].length)
			) {
				Mark nextOwner = _board[next_y][next_x];
				// déplacement sur case vide
				if (nextOwner == Mark.EMPTY) {
					nextMoves.add(new Move(p, new Position(next_x, next_y)));
				}
				// déplacement sur case énemie
				else if (nextOwner != o && move[0] != 0) {
					nextMoves.add(new Move(p, new Position(next_x, next_y)));
				}
			}
		}
		return nextMoves;
	}

	private double distanceEuclidienne(Position p1, Position p2) {
		return Math.sqrt(
			Math.powExact(p1.x() - p2.x(), 2) +
				Math.powExact(p1.y() - p2.y(), 2)
		);
	}

	public boolean moveIsValid(Move m) {
		if (
			(m.target().x() < 0 ||
				m.target().x() >= _board[m.target().x()].length) ||
			(m.target().y() < 0 || m.target().y() >= _board.length)
		) {
			System.err.println(m.toString());
			System.err.println("le move sort du plateau il n'est pas valide");
			return false;
		}

		Mark oInit = _board[m.init().y()][m.init().x()];
		Mark oTarg = _board[m.target().y()][m.target().x()];

		if (distanceEuclidienne(m.init(), m.target()) >= 2) {
			System.err.println(m.toString());
			System.err.println("On ne se deplace pas de plus de 1 case");
			return false;
		}

		if (oInit == Mark.EMPTY) {
			System.err.println(m.toString());
			System.err.println("la position initial du move est vide");
			return false;
		} else {
			if (oTarg != Mark.EMPTY) {
				if (oTarg != oInit) {
					if (m.init().x() == m.target().x()) {
						System.err.println(m.toString());
						System.err.println(
							"On ne peu pas manger un adversaire en face de nous"
						);
						return false;
					}
				} else {
					System.err.println(m.toString());
					System.err.println(
						"on ne peu pas se déplacer sur soit même"
					);
					return false;
				}
			}
		}

		if (oInit == Mark.R) {
			if (m.target().y() >= m.init().y()) {
				System.err.println(m.toString());
				System.err.println(
					"la position y du move pour R n'a pas changer dans le bon sens"
				);
				return false;
			}
		} else {
			if (m.target().y() <= m.init().y()) {
				System.err.println(m.toString());
				System.err.println(
					"la position y du move pour B n'a pas changer dans le bon sens"
				);
				return false;
			}
		}
		return true;
	}

	public void move(Move m) {
		if (moveIsValid(m)) {
			Mark oInit = _board[m.init().y()][m.init().x()];
			_board[m.target().y()][m.target().x()] = oInit;
			_board[m.init().y()][m.init().x()] = Mark.EMPTY;
		}
	}

	public Mark hasWinner() {
		for (Mark tile : _board[0]) {
			if (tile == Mark.R) {
				return Mark.R;
			}
		}
		for (Mark tile : _board[7]) {
			if (tile == Mark.B) {
				return Mark.B;
			}
		}
		return null;
	}

	public int evaluate(Mark m) {
		int numPlayer = 0;
		int numEnnemi = 0;
		int mostAdvenceMark;
		if (m == Mark.B) {
			mostAdvenceMark = 0;
		} else {
			mostAdvenceMark = 7;
		}
		for (int i = 0; i < _board.length; i++) {
			for (int j = 0; j < _board[i].length; j++) {
				if (_board[i][j] == m) {
					if (m == Mark.B) {
						if (i > mostAdvenceMark) {
							mostAdvenceMark = i;
						}
					} else {
						if (i < mostAdvenceMark) {
							mostAdvenceMark = i;
						}
					}
					numPlayer++;
				} else if (_board[i][j] != Mark.EMPTY) {
					numEnnemi++;
				}
			}
		}
		if (m == Mark.R) {
			mostAdvenceMark = 7 - mostAdvenceMark;
		}
		return (numPlayer - numEnnemi) + mostAdvenceMark;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Mark[] line : _board) {
			for (Mark tile : line) {
				sb.append(
					tile == Mark.EMPTY ? "." : tile == Mark.R ? "R" : "B"
				);
				sb.append("|");
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}
