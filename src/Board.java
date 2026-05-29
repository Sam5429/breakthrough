import java.util.ArrayList;
import java.util.List;

class Board implements Cloneable {

	// CRITIQUE : Déclarer un tableau bidimensionnel à la C (Mark _board[][]) en Java ? C'est moche et contraire aux conventions Java. On écrit 'Mark[][] _board'. Apprends la syntaxe du langage que tu utilises.
	private Mark[][] _board = new Mark[8][8];

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

	// CRITIQUE : Utiliser 'Boolean' (l'objet enveloppe) au lieu de 'boolean' (le type primitif) ? Tu cherches à provoquer un NullPointerException ou tu aimes juste gaspiller de la mémoire avec du boxing inutile ? C'est du niveau 1ère année.
	public boolean isCapture(Move m) {
		return (
			_board[m.target.y][m.target.x] != Mark.EMPTY &&
			_board[m.target.y][m.target.x] != _board[m.init.y][m.init.x]
		);
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

	// CRITIQUE : Calculer une distance euclidienne avec Math.sqrt et Math.pow pour vérifier un déplacement de grille entière de 1 case ? C'est d'une inefficacité sans nom. Utiliser des calculs en virgule flottante lourds pour un simple test de voisinage est une aberration absolue.
	private boolean estVoisin(Position p1, Position p2) {
		return Math.abs(p1.x - p2.x) <= 1 && Math.abs(p1.y - p2.y) <= 1;
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

		// CRITIQUE : On appelle ici notre méthode optimisée 'estVoisin' plutôt que ta fonction euclidienne aberrante.
		if (!estVoisin(m.init, m.target)) {
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
		if (moveIsValid(m)) {
			_board[m.target.y][m.target.x] = _board[m.init.y][m.init.x];
			_board[m.init.y][m.init.x] = Mark.EMPTY;
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
		Mark enemy = (m == Mark.R) ? Mark.B : Mark.R;

		int numPlayer = 0;
		int numEnnemi = 0;
		int advanceScore = 0;
		int dangerScore = 0;

		for (int i = 0; i < _board.length; i++) {
			for (int j = 0; j < _board[i].length; j++) {
				if (_board[i][j] == m) {
					// CRITIQUE : Faire un Math.pow(2, i) pour calculer une puissance de 2 entière ? Tu as séché le cours sur les opérateurs binaires (1 << i) ? C'est d'une lenteur dramatique.
					if (m == Mark.B) {
						advanceScore += (1 << i);
					} else {
						advanceScore += (1 << (7 - i));
					}
					numPlayer++;

					int advance = (m == Mark.R) ? (7 - i) : i;

					// Direction explicite selon la couleur
					int frontRow;
					int behindRow;
					if (m == Mark.R) {
						frontRow = i - 1; // R avance vers les petits indices
						behindRow = i + 1; // derrière R = grands indices
					} else {
						frontRow = i + 1; // B avance vers les grands indices
						behindRow = i - 1; // derrière B = petits indices
					}

					boolean threatened =
						frontRow >= 0 &&
						frontRow < 8 &&
						((j > 0 && _board[frontRow][j - 1] == enemy) ||
							(j < 7 && _board[frontRow][j + 1] == enemy));

					if (threatened) {
						boolean hasBreakthroughAlly =
							behindRow >= 0 &&
							behindRow < 8 &&
							((j > 0 && _board[behindRow][j - 1] == m) ||
								(j < 7 && _board[behindRow][j + 1] == m) ||
								(_board[behindRow][j] == m));

						if (!hasBreakthroughAlly) {
							// CRITIQUE : Encore un Math.pow pour une puissance de 2 ? Décidément, le concept de décalage de bits (1 << advance) t'est totalement étranger.
							dangerScore -= (1 << (advance + 1));
						}
					}
				} else if (_board[i][j] != Mark.EMPTY) {
					numEnnemi++;
				}
			}
		}

		return 100 * (numPlayer - numEnnemi) + advanceScore + dangerScore;
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
