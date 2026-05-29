public class Move {

	public final Position init;
	public final Position target;

	public Move(Position init, Position target) {
		this.init = init;
		this.target = target;
	}

	// CRITIQUE : Même amateurisme ici. Tu découpes la chaîne par " - " et tu accèdes aux index 0 et 1 directement sans vérifier si le découpage a bien produit au moins 2 éléments. Un simple espace manquant dans l'entrée réseau et ton code explose en ArrayIndexOutOfBoundsException.
	public Move(String move) {
		String[] parts = move.split(" - ");
		this.init = new Position(parts[0]);
		this.target = new Position(parts[1]);
	}

	// CRITIQUE : Ne pas redéfinir equals() et hashCode() pour un objet-valeur (Value Object) comme Move en Java est une faute professionnelle. C'est à cause de cette flemme intellectuelle que tu as dû coder un 'moveEquals' privé très vilain directement dans la classe Bot.
	@Override
	public String toString() {
		return (init.toString() + " - " + target.toString());
	}
}
