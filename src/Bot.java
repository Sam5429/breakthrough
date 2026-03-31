import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class Bot {

	private Mark _player;
	private Mark _ennemie;

	// Limite de temps stricte : 5 secondes en nanosecondes
	private static final long TIME_LIMIT_NS = 5_000_000_000L;

	// Timestamp de début de recherche
	private long _startTime;

	// Flag d'interruption : true quand le temps est écoulé
	private boolean _timeout;

	// Table de transpositions : clé = hash du plateau, valeur = entrée cachée
	private HashMap<Long, TTEntry> _transpositionTable;

	// Killer moves : [profondeur][slot 0 ou 1]
	// Mémorise les coups qui ont causé une coupure bêta récemment
	private Move[][] _killerMoves;
	private static final int MAX_DEPTH = 30;

	// Entrée dans la table de transpositions
	private static class TTEntry {

		int score;
		int depth;
		int flag; // EXACT=0, LOWER=1, UPPER=2
		Move bestMove;

		TTEntry(int score, int depth, int flag, Move bestMove) {
			this.score = score;
			this.depth = depth;
			this.flag = flag;
			this.bestMove = bestMove;
		}
	}

	public Bot(Mark cpu) {
		_player = cpu;
		_ennemie = (cpu == Mark.R) ? Mark.B : Mark.R;
		_transpositionTable = new HashMap<>(1 << 20); // ~1M entrées
		_killerMoves = new Move[MAX_DEPTH][2];
	}

	public Mark getPlayerMark() {
		return _player;
	}

	public Mark getEnnemieMark() {
		return _ennemie;
	}

	/**
	 * Iterative Deepening Alpha-Beta avec limite de temps stricte de 5s.
	 * Explore profondeur 1, 2, 3... jusqu'à épuisement du temps.
	 * Retourne toujours le meilleur coup trouvé à la profondeur complète précédente.
	 */
	public ArrayList<Move> getNextMoveAB(Board board) {
		_startTime = System.nanoTime();
		_timeout = false;
		_transpositionTable.clear();
		_killerMoves = new Move[MAX_DEPTH][2];

		ArrayList<Move> bestMoveList = new ArrayList<>();
		int bestScore = Integer.MIN_VALUE;

		List<Move> moveList = board.getMoves(_player);
		if (moveList.isEmpty()) return bestMoveList;

		// Résultat de la profondeur précédente (toujours valide)
		ArrayList<Move> lastCompleteMoveList = new ArrayList<>();
		int lastCompleteScore = Integer.MIN_VALUE;

		// Iterative deepening : on augmente la profondeur tant qu'on a du temps
		for (int depth = 1; depth < MAX_DEPTH; depth++) {
			bestScore = Integer.MIN_VALUE;
			bestMoveList = new ArrayList<>();

			int alpha = Integer.MIN_VALUE;
			int beta = Integer.MAX_VALUE;

			// Tri des coups dès le niveau racine
			List<Move> orderedMoves = orderMoves(moveList, board, 0);

			for (Move move : orderedMoves) {
				if (isTimeout()) break;

				Board current_board = board.clone();
				current_board.move(move);
				int score = minMaxAB(
					current_board,
					_ennemie,
					alpha,
					beta,
					1,
					depth
				);

				if (_timeout) break; // Ne pas utiliser un score partiel

				if (score == bestScore) {
					bestMoveList.add(move);
				} else if (score > bestScore) {
					bestScore = score;
					bestMoveList.clear();
					bestMoveList.add(move);
				}

				alpha = Math.max(alpha, bestScore);
			}

			// Si on a complété la profondeur sans timeout, on sauvegarde
			if (!_timeout) {
				lastCompleteMoveList = new ArrayList<>(bestMoveList);
				lastCompleteScore = bestScore;
				System.out.println(
					"Profondeur " +
						depth +
						" complétée, score: " +
						bestScore +
						", temps: " +
						(System.nanoTime() - _startTime) / 1_000_000 +
						"ms"
				);
			} else {
				System.out.println(
					"Timeout à profondeur " +
						depth +
						" — on utilise profondeur " +
						(depth - 1)
				);
				break;
			}

			// Victoire/défaite certaine trouvée : inutile d'aller plus loin
			if (Math.abs(bestScore) > 90) break;
		}

		System.out.println("Score final : " + lastCompleteScore);
		return lastCompleteMoveList.isEmpty()
			? bestMoveList
			: lastCompleteMoveList;
	}

	/**
	 * Alpha-Beta récursif avec :
	 * - Table de transpositions
	 * - Killer moves
	 * - Move ordering
	 * - Vérification du timeout à chaque nœud
	 */
	private int minMaxAB(
		Board board,
		Mark player,
		int alpha,
		int beta,
		int profondeur,
		int maxDepth
	) {
		if (isTimeout()) return 0;

		// Consultation de la table de transpositions
		long hash = board.getZobristHash();
		TTEntry cached = _transpositionTable.get(hash);
		if (cached != null && cached.depth >= (maxDepth - profondeur)) {
			if (cached.flag == 0) return cached.score; // Score exact
			if (cached.flag == 1) alpha = Math.max(alpha, cached.score); // Borne inférieure
			if (cached.flag == 2) beta = Math.min(beta, cached.score); // Borne supérieure
			if (alpha >= beta) return cached.score;
		}

		// Cas terminal : victoire/défaite
		Mark winner = board.hasWinner();
		if (winner != null) {
			return (winner == _player)
				? (100 - profondeur)
				: (-100 + profondeur);
		}

		// Profondeur max atteinte : évaluation statique
		if (profondeur >= maxDepth) {
			return board.evaluate(_player);
		}

		List<Move> moveList = board.getMoves(player);
		List<Move> orderedMoves = orderMoves(moveList, board, profondeur);

		int originalAlpha = alpha;
		Move bestMove = null;

		if (player == _player) {
			// Nœud MAX
			int maxScore = Integer.MIN_VALUE;
			for (Move move : orderedMoves) {
				if (isTimeout()) return 0;

				Board childBoard = board.clone();
				childBoard.move(move);
				int score = minMaxAB(
					childBoard,
					_ennemie,
					alpha,
					beta,
					profondeur + 1,
					maxDepth
				);

				if (score > maxScore) {
					maxScore = score;
					bestMove = move;
				}
				if (maxScore >= beta) {
					// Coupure bêta : mémoriser ce coup comme killer
					storeKillerMove(move, profondeur);
					break;
				}
				alpha = Math.max(maxScore, alpha);
			}

			// Stockage dans la table de transpositions
			storeTransposition(
				hash,
				maxScore,
				maxDepth - profondeur,
				alpha,
				beta,
				originalAlpha,
				bestMove
			);
			return maxScore;
		} else {
			// Nœud MIN
			int minScore = Integer.MAX_VALUE;
			for (Move move : orderedMoves) {
				if (isTimeout()) return 0;

				Board childBoard = board.clone();
				childBoard.move(move);
				int score = minMaxAB(
					childBoard,
					_player,
					alpha,
					beta,
					profondeur + 1,
					maxDepth
				);

				if (score < minScore) {
					minScore = score;
					bestMove = move;
				}
				if (minScore <= alpha) {
					storeKillerMove(move, profondeur);
					break;
				}
				beta = Math.min(minScore, beta);
			}

			storeTransposition(
				hash,
				minScore,
				maxDepth - profondeur,
				alpha,
				beta,
				originalAlpha,
				bestMove
			);
			return minScore;
		}
	}

	/**
	 * Trie les coups pour maximiser les coupures alpha-bêta :
	 * 1. Captures (coups diagonaux qui prennent une pièce adverse)
	 * 2. Killer moves (coups qui ont causé des coupures à cette profondeur)
	 * 3. Avances simples
	 */
	private List<Move> orderMoves(List<Move> moves, Board board, int depth) {
		Move killer1 = (depth < MAX_DEPTH) ? _killerMoves[depth][0] : null;
		Move killer2 = (depth < MAX_DEPTH) ? _killerMoves[depth][1] : null;

		List<Move> captures = new ArrayList<>();
		List<Move> killers = new ArrayList<>();
		List<Move> others = new ArrayList<>();

		for (Move move : moves) {
			if (isCapture(move, board)) {
				captures.add(move);
			} else if (moveEquals(move, killer1) || moveEquals(move, killer2)) {
				killers.add(move);
			} else {
				others.add(move);
			}
		}

		List<Move> ordered = new ArrayList<>(
			captures.size() + killers.size() + others.size()
		);
		ordered.addAll(captures);
		ordered.addAll(killers);
		ordered.addAll(others);
		return ordered;
	}

	/** Retourne true si le coup capture une pièce adverse (déplacement diagonal). */
	private boolean isCapture(Move move, Board board) {
		return move.init.x != move.target.x; // déplacement latéral = capture potentielle
	}

	/** Stocke un killer move à la profondeur donnée (2 slots en rotation). */
	private void storeKillerMove(Move move, int depth) {
		if (depth >= MAX_DEPTH) return;
		if (!moveEquals(_killerMoves[depth][0], move)) {
			_killerMoves[depth][1] = _killerMoves[depth][0];
			_killerMoves[depth][0] = move;
		}
	}

	/** Stocke le résultat dans la table de transpositions. */
	private void storeTransposition(
		long hash,
		int score,
		int depth,
		int alpha,
		int beta,
		int originalAlpha,
		Move bestMove
	) {
		int flag;
		if (score <= originalAlpha) flag = 2;
		// Borne supérieure (UPPER)
		else if (score >= beta) flag = 1;
		// Borne inférieure (LOWER)
		else flag = 0; // Score exact (EXACT)

		TTEntry existing = _transpositionTable.get(hash);
		if (existing == null || depth >= existing.depth) {
			_transpositionTable.put(
				hash,
				new TTEntry(score, depth, flag, bestMove)
			);
		}
	}

	private boolean moveEquals(Move a, Move b) {
		if (a == null || b == null) return false;
		return (
			a.init.y == b.init.y &&
			a.init.x == b.init.x &&
			a.target.y == b.target.y &&
			a.target.x == b.target.x
		);
	}

	private boolean isTimeout() {
		if (_timeout) return true;
		if (System.nanoTime() - _startTime >= TIME_LIMIT_NS) {
			_timeout = true;
			return true;
		}
		return false;
	}
}
