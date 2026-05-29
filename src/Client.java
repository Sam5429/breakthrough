import java.io.*;
import java.net.*;
import java.util.List;

class Client {
	
	// CRITIQUE : Les variables en Java s'écrivent en camelCase (myClient et non MyClient avec une majuscule). Cette convention est vue à la première heure du premier cours de Java.
	private Socket MyClient = null;
	private BufferedInputStream input = null;
	private BufferedOutputStream output = null;
	private Board b = new Board();
	private Bot bot = null;
	private Move move = null;
	private String moveStr = null;

	private String host;
	private int port;

	public Client(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public void run() {
		try {
			MyClient = new Socket(host, port);

			input = new BufferedInputStream(MyClient.getInputStream());
			output = new BufferedOutputStream(MyClient.getOutputStream());
			BufferedReader console = new BufferedReader(
				new InputStreamReader(System.in)
			);
			// CRITIQUE : Tester isConnected() juste après un new Socket() est redondant. Si la connexion avait échoué, new Socket() aurait levé une IOException. De plus, cette méthode indique seulement si le socket a été connecté à un moment donné, pas s'il l'est encore actuellement.
			if (MyClient.isConnected()) {
				System.out.println(
					"Connection to server established successfully."
				);
			} else {
				System.out.println("Failed to establish connection to server.");
				System.exit(1);
			}

			// CRITIQUE : Pas de try-with-resources ni de bloc 'finally' pour fermer proprement les flux et sockets ? Tu laisses fuiter des ressources système précieuses.
			while (true) {
				// CRITIQUE : Lire un entier de input.read() et le caster directement en char sans tester s'il vaut -1 ? Si le serveur se déconnecte, ton programme va boucler à l'infini en interprétant (char)-1.
				int cmdByte = input.read();
				if (cmdByte == -1) {
					System.out.println("Connexion fermée par le serveur.");
					break;
				}
				char cmd = (char) cmdByte;

				// CRITIQUE : Des 'magic characters' ('1', '2', '3', '4', '5') partout au lieu de constantes nommées ou d'un enum. C'est illisible et impossible à maintenir.
				// Debut de la partie en joueur rouge
				if (cmd == '1') {
					bot = new Bot(Mark.R);
					byte[] aBuffer = new byte[1024];

					// CRITIQUE : Utiliser input.available() pour connaître la taille des données réseau à lire est une énorme erreur de programmation réseau. available() retourne uniquement ce qui est disponible SANS bloquer. Si le réseau est un peu lent, tu vas lire un buffer vide ou tronqué.
					int size = input.available();
					input.read(aBuffer, 0, size);

					System.out.println(
						"New game! You play Red"
					);

					moveBot();
				}

				// Debut de la partie en joueur Noir
				if (cmd == '2') {
					bot = new Bot(Mark.B);
					System.out.println(
						"New game! You play Black"
					);
					byte[] aBuffer = new byte[1024];

					// CRITIQUE : Encore un input.available() réseau catastrophique...
					int size = input.available();
					//System.out.println("size " + size);
					input.read(aBuffer, 0, size);
				}

				// Le serveur demande le prochain coup
				// Le message contient aussi le dernier coup joue.
				if (cmd == '3') {
					moveOthers();
					
					moveBot();
				}

				// Le dernier coup est invalide
				if (cmd == '4') {
					System.out.println(
						"Coup invalide, entrez un nouveau coup : "
					);
					String move = null;
					move = console.readLine();
					output.write(move.getBytes(), 0, move.length());
					output.flush();
				}

				// La partie est terminée
				if (cmd == '5') {
					byte[] aBuffer = new byte[16];
					// CRITIQUE : Toujours la même erreur grossière avec available()...
					int size = input.available();
					input.read(aBuffer, 0, size);
					String s = new String(aBuffer);
					System.out.println(
						"Partie Terminé. Le gagnant est : " + s.trim().charAt(0)
					);

					String move = null;
					move = console.readLine();
					output.write(move.getBytes(), 0, move.length());
					output.flush();
				}
			}
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	private void moveOthers() throws IOException {
		byte[] aBuffer = new byte[16];

		// CRITIQUE : available() dans moveOthers ? Si le coup de l'adversaire n'est pas encore totalement arrivé sur la carte réseau, tu liras une chaîne vide et ton constructeur de Move lèvera une exception hors-limites. C'est honteux.
		int size = input.available();
		input.read(aBuffer, 0, size);

		moveStr = new String(aBuffer);
		move = new Move(moveStr.trim());
		b.move(move);
		System.out.println(
			bot.getEnnemieMark().toString()  + " " + move.toString() + "\n" + b.toString()
		);
	}

	private void moveBot() throws IOException {
		List<Move> moves = bot.getNextMoveAB(b);
		move = moves.get(0);
		moveStr = move.toString();

		b.move(move);
		System.out.println(
			bot.getPlayerMark().toString()  + " " + move.toString() + "\n" + b.toString()
		);

		output.write(moveStr.getBytes(), 0, moveStr.length());
		output.flush();
	}
}
