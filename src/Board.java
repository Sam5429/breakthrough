import java.util.ArrayList;
import java.util.List;

class Board implements Cloneable {

	// Sérieusement ? Des underscores en Java ?? On est en C++ ou quoi ?
	// Même un débutant sait que c'est pas comme ça que ça marche.
	// Et pour les échecs t'as même pas de distinction entre les types de pièces,
	// la prise en passant c'est UNIQUEMENT pour les pions, pas n'importe quelle pièce !
	private Mark _board[][] = new Mark[8][8];

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

	// Le clone() est inutilement compliqué. T'aurais pu faire autrement
	// mais visiblement t'as pas cherché.
	// Et puis pour simuler la prise en passant t'as même pas cloné le dernier coup joué,
	// c'est pourtant LA donnée dont t'as besoin...
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

	// C'est une blague ? T'as jamais entendu parler des primitives ?
	// Je comprends pas comment on peut faire une erreur aussi basique.
	// Et isCapture() qui retourne juste "la case cible est occupée par l'ennemi"...
	// la prise en passant c'est exactement l'inverse : la case cible est VIDE !
	// T'as même pas compris le concept que t'es censé implémenter.
	public Boolean isCapture(Move m) {
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

	// Pourquoi t'as mis ça en français ?? On code en Java pas en baguette.
	// Et en plus t'utilises Math.pow pour des carrés comme un noob alors que (a*a) existe.
	// Mais surtout : pour valider un déplacement de pion d'une case, t'as BESOIN
	// d'une distance euclidienne ??? sqrt(1²+0²) = 1.0, bravo, félicitations.
	private double distanceEuclidienne(Position p1, Position p2) {
		return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
	}

	// moveIsValid c'est un désastre total. System.err.println partout dans une méthode
	// de validation ? C'est du code de collégien ça. T'as pas entendu parler des exceptions ?
	// Et nulle part tu vérifies que la prise en passant n'est valide
	// QUE si le pion adverse vient d'avancer de 2 cases au coup précédent.
	// C'est LA règle principale et t'as complètement oublié de l'implémenter.
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

	// move() ne supprime même pas le pion capturé en passant !
	// La case cible est vide donc t'écrase rien, et le pion adverse
	// reste tranquillement sur le plateau. Bravo, t'as inventé la prise fantôme.
	public void move(Move m) {
		if (moveIsValid(m)) {
			_board[m.target.y][m.target.x] = _board[m.init.y][m.init.x];
			_board[m.init.y][m.init.x] = Mark.EMPTY;
		}
	}

	// hasWinner hardcode _board[7] — Et si le plateau change de taille ?
	// Apparemment t'as pas réfléchi plus loin que le bout de ton nez.
	// Et aux échecs la "victoire" c'est un échec et mat, pas un pion qui atteint
	// la dernière rangée. T'as même pas compris le jeu que t'es censé simuler.
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

	// j'ai même pas envie de lire ça jusqu'au bout tellement c'est long et illisible.
	// C'est quoi ce Math.pow(2, -(i - 7)) sorti de nulle part ?
	// Et une fonction d'évaluation dans le code de la prise en passant ???
	// T'es en train de coder un moteur d'IA ou une règle du jeu ? Fais un choix.
	public int evaluate(Mark m) {
		Mark enemy = (m == Mark.R) ? Mark.B : Mark.R;

		int numPlayer = 0;
		int numEnnemi = 0;
		int advanceScore = 0;
		int dangerScore = 0;

		for (int i = 0; i < _board.length; i++) {
			for (int j = 0; j < _board[i].length; j++) {
				if (_board[i][j] == m) {
					if (m == Mark.B) {
						advanceScore += (int) Math.pow(2, i);
					} else {
						advanceScore += (int) Math.pow(2, -(i - 7));
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
							dangerScore -= 2 * Math.pow(2, advance);
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
