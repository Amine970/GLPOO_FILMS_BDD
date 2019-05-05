/**
 *    Gestion des noms (noms de famille + prénom) des personnes.
 *    <p>
 *    La classe NomPersonne permet de gérer les noms et tient
 *    en particulier compte des préfixes des noms ('de', 'von', 'van')
 *    dans le tri.
 */
public class NomPersonne implements Comparable<NomPersonne>{
    private String _nom;
    private String _prenom;
    private int    _debutComp;

    /**
     *    Cré d'un nouveau NomPersonne. Attention, le prénom
     *    est passé en deuxième.
     *
     *    @param nom Nom de famille ou nom d'artiste
     *    @param prenom Prénom (peut être "null")
     */
    public NomPersonne(String nom, String prenom) {
        _nom = new String(nom);
        _prenom = new String(prenom);
        _debutComp = 0;
        // On regarde quel est le premier caractère en majuscules
        // pour trier 'von Stroheim' avec les S, 'de la Huerta'
        // avec les H et 'de Funès' avec les F.
        // 'De Niro' sera en revanche à D.
        while ((_debutComp < _nom.length())
                && (_nom.charAt(_debutComp)
                == Character.toLowerCase(_nom.charAt(_debutComp)))) {
            _debutComp++;
        }
    }

    /**   Comparateur qui tient compte des préfixes de noms.
     *
     *    @param autre NomPersonne qui est comparé à; l'objet courant
     *    @return un entier inférieur, égal ou supérieur à; zéro suivant le cas
     */
    @Override
    public int compareTo(NomPersonne autre) {
        if (autre == null) {
            return 1;
        }
        int cmp = this._nom.substring(this._debutComp)
                .compareTo(autre._nom.substring(autre._debutComp));
        if (cmp == 0) {
            return this._prenom.compareTo(autre._prenom);
        } else {
            return cmp;
        }
    }

    /**
     *   Retourne un nom affichable.
     *   <p>
     *   S'il y a une mention telle que (Jr.) qui dans la base est dans
     *   la colonne du prénom, elle est reportée à; 
     *   la fin.
     *
     *   @return La combinaison du prénom et du nom, dans cet ordre.
     */
    @Override
    public String toString() {
        int pos = -1;

        if (this._prenom != null) {
            // Les mentions entre parenthèses seront renvoyées
            // à la fin.
            pos = this._prenom.indexOf('(');
        }
        if (pos == -1) {
            if (this._prenom == null) {
                return this._nom;
            } else {
                return this._prenom + " " + this._nom;
            }
        } else {
            return this._prenom.substring(0, pos-1).trim()
                    + " " + this._nom
                    + " " + this._prenom.substring(pos).trim();
        }
    }
}
