import java.util.ArrayList;
import java.util.Collections;

/**
 *   Information synthétique sur un film.
 */
public class InfoFilm implements Comparable<InfoFilm> {
    private String                 _titre;
    private ArrayList<NomPersonne> _realisateurs;
    private ArrayList<NomPersonne> _acteurs;
    private String                 _pays;
    private int                    _annee;
    private int                    _duree;
    private ArrayList<String>      _autres_titres;

    /**
     *  Constructeur.
     *
     *  @param titre Titre (français en général) du film
     *  @param realisateurs Liste des réalisateurs (peut être vide)
     *  @param acteurs Liste des acteurs (peut être vide)
     *  @param pays Nom (français) du pays
     *  @param annee Année de sortie
     *  @param duree Durée en minutes; 0 ou valeur négative si l'information n'est pas connue
     *  @param autres_titres Liste des titres alternatifs (peut être vide), type titre original ou titre anglais à; l'international
     */
    public InfoFilm(String titre,
                    ArrayList<NomPersonne> realisateurs,
                    ArrayList<NomPersonne> acteurs,
                    String pays,
                    int annee,
                    int duree,
                    ArrayList<String> autres_titres) {
        _titre = titre;
        _realisateurs = realisateurs;
        Collections.sort(_realisateurs);
        _acteurs = acteurs;
        Collections.sort(_acteurs);
        _pays = pays;
        _annee = annee;
        _duree = duree;
        _autres_titres = autres_titres;
        Collections.sort(_autres_titres);
    }

    /**
     *   Comparaison par titre, puis année, puis pays.
     *
     *    @return un entier inférieur, égal ou supérieur à; zéro suivant le cas
     */
    @Override
    public int compareTo(InfoFilm autre) {
        if (autre == null) {
            return 1;
        }
        int cmp = this._titre.compareTo(autre._titre);
        if (cmp == 0) {
            cmp = (this._annee < autre._annee ? -1
                    : (this._annee == autre._annee ? 0 : 1));
            if (cmp == 0) {
                cmp = this._pays.compareTo(autre._pays);
            }
        }
        return cmp;
    }

    /**
     *   Affiche sous forme d'objet JSON des informations sous un film.
     *   <p>
     *   Réalisateurs et acteurs sont triés par ordre alphabétique, la durée est convertie en heures et minutes.
     *
     *   @return Une chaîne de caractères représentant un objet JSON.
     */
    @Override
    public String toString() {
        boolean debut = true;
        StringBuilder sb = new StringBuilder();
        sb.append("{\"titre\":\"" + _titre.replace("\"", "\\\"") + "\",");
        sb.append("\"realisateurs\":[");
        for (NomPersonne nom: _realisateurs) {
            if (debut) {
                debut = false;
            } else {
                sb.append(',');
            }
            sb.append("\""+ nom.toString().replace("\"", "\\\"") + "\"");
        }
        sb.append("],\"acteurs\":[");
        debut = true;
        for (NomPersonne nom: _acteurs) {
            if (debut) {
                debut = false;
            } else {
                sb.append(',');
            }
            sb.append("\""+ nom.toString().replace("\"", "\\\"") + "\"");
        }
        sb.append("],\"pays\":\"");
        sb.append(_pays.replace("\"", "\\\""));
        sb.append("\",\"annee\":");
        sb.append(Integer.toString(_annee));
        sb.append(",\"duree\":");
        if (_duree > 0) {
            sb.append('"');
            int h = _duree / 60;
            sb.append(Integer.toString(h) + "h");
            int mn = _duree % 60;
            if (mn > 0) {
                sb.append(Integer.toString(mn) + "mn");
            }
            sb.append('"');
        } else {
            sb.append("null");
        }
        sb.append(",\"autres titres\":[");
        debut = true;
        for (String titre: _autres_titres) {
            if (debut) {
                debut = false;
            } else {
                sb.append(',');
            }
            sb.append("\""+ titre.replace("\"", "\\\"") + "\"");
        }
        sb.append("]}");
        return sb.toString();
    }
}
