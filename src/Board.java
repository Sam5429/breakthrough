import java.util.ArrayList;
import java.util.List;

class Board implements Cloneable {

	private Mark _board[][] = new Mark[8][8];
	private static final long[][][] zobristTable = new long[8][8][2];
	private long _zobristHash = 0L;

	// Retourne le hash courant
	public long getZobristHash() {
		return _zobristHash;
	}

	public Board() {
		// init le plateau
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

		// init tableau aléatoire
		java.util.Random rng = new java.util.Random(123456789L); // seed fixe = reproductible
		for (int row = 0; row < 8; row++) for (
			int col = 0;
			col < 8;
			col++
		) for (
			int piece = 0;
			piece < 2;
			piece++
		) zobristTable[row][col][piece] = rng.nextLong();
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
		List<Move> moves = new ArrayList<>();

		final int rows = _board.length;
		for (int i = 0; i < rows; i++) {
			final int cols = _board[i].length;
			for (int j = 0; j < cols; j++) {
				if (_board[i][j] == o) {
					Position currentPos = new Position(j, i);
					moves.addAll(getMoves(currentPos));
				}
			}
		}

		moves.sort((move1, move2) -> {
			// For Mark.B, higher y-coordinate means more advanced
			if (o == Mark.B) {
				return Integer.compare(move2.init.y, move1.init.y);
			}
			// For Mark.R, lower y-coordinate means more advanced
			else {
				return Integer.compare(move1.init.y, move2.init.y);
			}
		});

		return moves;
	}

	public List<Move> getMoves(Position p) {
		final Mark o = _board[p.y][p.x];
		final int[][] moves = (o == Mark.B)
			? new int[][] { { -1, 1 }, { 0, 1 }, { 1, 1 } }
			: new int[][] { { -1, -1 }, { 0, -1 }, { 1, -1 } };

		List<Move> nextMoves = new ArrayList<>(moves.length);
		final int boardHeight = _board.length;

		for (int[] move : moves) {
			final int next_x = p.x + move[0];
			final int next_y = p.y + move[1];

			if (next_y >= 0 && next_y < boardHeight) {
				final int boardWidth = _board[next_y].length;
				if (next_x >= 0 && next_x < boardWidth) {
					final Mark nextOwner = _board[next_y][next_x];

					if (
						nextOwner == Mark.EMPTY ||
						(nextOwner != o && move[0] != 0)
					) {
						nextMoves.add(
							new Move(p, new Position(next_x, next_y))
						);
					}
				}
			}
		}
		return nextMoves;
	}

	private double distanceEuclidienne(Position p1, Position p2) {
		return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
	}

	public boolean moveIsValid(Move m) {
		if (
			(m.target.x < 0 || m.target.x >= _board[m.target.y].length) ||
			(m.target.y < 0 || m.target.y >= _board.length)
		) {
			System.err.println(m.toString());
			System.err.println("le move sort du plateau il n'est pas valide");
			return false;
		}

		Mark oInit = _board[m.init.y][m.init.x];
		Mark oTarg = _board[m.target.y][m.target.x];

		if (distanceEuclidienne(m.init, m.target) >= 2) {
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
					if (m.init.x == m.target.x) {
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
			if (m.target.y >= m.init.y) {
				System.err.println(m.toString());
				System.err.println(
					"la position y du move pour R n'a pas changer dans le bon sens"
				);
				return false;
			}
		} else {
			if (m.target.y <= m.init.y) {
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
		applyMoveZobrist(m);
		if (moveIsValid(m)) {
			_board[m.target.y][m.target.x] = _board[m.init.y][m.init.x];
			_board[m.init.y][m.init.x] = Mark.EMPTY;
		}
	}

	private int pieceIndex(Mark mark) {
		return (mark == Mark.R) ? 0 : 1;
	}

	private void applyMoveZobrist(Move m) {
		// Retire la pièce de sa case de départ
		_zobristHash ^= zobristTable[m.init.y][m.init.x][pieceIndex(
			_board[m.init.y][m.init.x]
		)];

		// Retire la pièce capturée si présente
		if (_board[m.target.y][m.target.x] != Mark.EMPTY) _zobristHash ^=
			zobristTable[m.target.y][m.target.x][pieceIndex(
				_board[m.target.y][m.target.x]
			)];

		// Place la pièce sur sa case d'arrivée
		_zobristHash ^= zobristTable[m.target.y][m.target.x][pieceIndex(
			_board[m.init.y][m.init.x]
		)];
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
		return (numPlayer - numEnnemi) + (2 * mostAdvenceMark);
	}

	public int evaluateNew(Mark m) {
		int numPlayer = 0;
		int numEnnemi = 0;
		int mostAdvanceMark = (m == Mark.B) ? 0 : 7; // Initialisation conditionnelle

		final int boardLength = _board.length;
		for (int i = 0; i < boardLength; i++) {
			final int rowLength = _board[i].length;
			for (int j = 0; j < rowLength; j++) {
				final Mark currentMark = _board[i][j];

				if (currentMark == m) {
					numPlayer++;
					if (m == Mark.B) {
						if (i > mostAdvanceMark) mostAdvanceMark = i;
					} else {
						if (i < mostAdvanceMark) mostAdvanceMark = i;
					}
				} else if (currentMark != Mark.EMPTY) {
					numEnnemi++;
				}
			}
		}

		// Ajustement pour les pièces rouges (si nécessaire)
		if (m == Mark.R) {
			mostAdvanceMark = 7 - mostAdvanceMark;
		}

		// Calcul final de l'évaluation
		return (numPlayer - numEnnemi) + mostAdvanceMark;
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
