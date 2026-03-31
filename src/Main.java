import java.util.List;

class Main {

	public static void main(String[] args) {
		if (args[0].compareTo("--client") == 0) {
			Client client = new Client();
			client.run();
		} else if (args[0].compareTo("--local") == 0) {
			Board b = new Board();
			System.out.println(b.toString());
			Mark oTurn = Mark.B;
			Bot bot = new Bot(Mark.B);
			BotNew nb = new BotNew(Mark.R);
			while (b.hasWinner() == null) {
				Move move;
				if (oTurn == Mark.B) {
					List<Move> moves = bot.getNextMoveAB(b);
					move = moves.get(0);
				} else {
					List<Move> moves = nb.getNextMoveAB(b);
					move = moves.get(0);
				}
				System.out.println(
					(oTurn == Mark.B ? "B" : "R") + " " + move.toString()
				);
				b.move(move);
				System.out.println(b.toString());
				oTurn = oTurn == Mark.B ? Mark.R : Mark.B;
			}
			System.out.println(
				"Winner : " + (b.hasWinner() == Mark.B ? "B" : "R")
			);
		}
	}
}
