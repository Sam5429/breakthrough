import java.util.ArrayList;
import java.util.List;

class BotNew {

	private Mark _player;
	private Mark _ennemie;

	// Le constructeur reçoit en paramètre le
	// joueur MAX (X ou O)
	public BotNew(Mark cpu) {
		_player = cpu;
		if (cpu == Mark.R) {
			_ennemie = Mark.B;
		} else {
			_ennemie = Mark.R;
		}
	}

	public Mark getPlayerMark() {
		return _player;
	}

	public Mark getEnnemieMark() {
		return _ennemie;
	}

	public ArrayList<Move> getNextMoveAB(Board board) {
		List<Move> move_list = board.getMoves(_player);
		ArrayList<Move> best_move_list = new ArrayList<>();
		int max_score = Integer.MIN_VALUE;
		int alpha = Integer.MIN_VALUE;
		int beta = Integer.MAX_VALUE;

		for (Move move : move_list) {
			Board current_board = board.clone();
			current_board.move(move);
			int score = minMaxAB(current_board, _ennemie, alpha, beta, 1);
			alpha = Math.max(alpha, score);
			if (max_score == score) {
				best_move_list.add(move);
			} else if (max_score < score) {
				max_score = score;
				best_move_list.clear();
				best_move_list.add(move);
			}
		}

		System.out.println("le score du coup : " + max_score);
		return best_move_list;
	}

	private int minMaxAB(
		Board board,
		Mark player,
		int alpha,
		int beta,
		int profondeur
	) {
		if (board.hasWinner() != null) {
			if (board.hasWinner() == _player) {
				return 100 - profondeur;
			} else {
				return -100 - profondeur;
			}
		}
		if (profondeur > 7) {
			return board.evaluateNew(_player);
		}

		List<Move> move_list = board.getMoves(player);

		if (player == _player) {
			int max_score = Integer.MIN_VALUE;
			for (Move move : move_list) {
				Board current_board = board.clone();
				current_board.move(move);
				max_score = Math.max(
					max_score,
					minMaxAB(
						current_board,
						_ennemie,
						alpha,
						beta,
						profondeur + 1
					)
				);
				if (max_score >= beta) {
					break;
				}
				alpha = Math.max(max_score, alpha);
			}
			return max_score;
		} else {
			int min_score = Integer.MAX_VALUE;
			for (Move move : move_list) {
				Board current_board = board.clone();
				current_board.move(move);
				min_score = Math.min(
					min_score,
					minMaxAB(
						current_board,
						_player,
						alpha,
						beta,
						profondeur + 1
					)
				);
				if (min_score <= alpha) {
					break;
				}
				beta = Math.min(min_score, beta);
			}
			return min_score;
		}
	}
}
