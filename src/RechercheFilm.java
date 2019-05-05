import javax.sound.sampled.Line;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RechercheFilm
{
    private Connection conn = null;
    private Map<String, StringBuilder> languageMap = new HashMap<>();
    private Map<String, InfoFilm2> infoFilmsMap = new HashMap<>();  // string titre
    private Map<String, ArrayList<NomPersonne>> filmsActeurs = new HashMap<>(); // clé : film, valeurs : noms acteurs
    private Map<String, ArrayList<NomPersonne>> filmsRealisateurs = new HashMap<>();
    public RechercheFilm(String monFichierSQLite)
    {
        try {
            String url = "jdbc:sqlite:"+monFichierSQLite;
            conn = DriverManager.getConnection(url);
            System.out.println("Connection to SQLite has been established.");

        } catch (SQLException e) {
            System.out.println(e.getMessage());}
//        } finally {
//            try {
//                if (conn != null) {
//                    conn.close();
//                }
//            } catch (SQLException ex) {
//                System.out.println(ex.getMessage());
//            }
//        }
    }
    public void fermeBase()
    {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }
    public void parseEntryRequest(String request)
    {
        String keyWords = "TITRE|DE|AVEC|PAYS|EN|AVANT|APRES";
        for(String keyWord : keyWords.split("[|]"))
            languageMap.put(keyWord, new StringBuilder());
        String keyWordsRegex = "((TITRE)|(DE)|(AVEC)|(PAYS)|(EN)|(AVANT)|(APRES)).*";
        for(String keyWord : keyWords.split("[|]"))
            for(String subRequest : request.split(keyWord))
                languageMap.get(keyWord).append(subRequest.replaceAll(keyWordsRegex,""));

        for(String k : languageMap.keySet())
        {
            //System.out.println(k + ":" + languageMap.get(k));
        }
    }
    public String retrouve(String requete)
    {
        PreparedStatement preparedStatement;
        ResultSet resultSet;
        String          infoFilm;

        String                  _titre;
        String          _realisateurs="";
        String          _acteurs="";
        String                  _pays;
        int                     _annee;
        int                     _duree;
        String          _autres_titres;
        try {
            preparedStatement =
                    conn.prepareStatement(requete);
            resultSet = preparedStatement.executeQuery();
            while(resultSet.next())
            {
                _realisateurs="";
                _acteurs="";
                _autres_titres = "";
                //System.out.println("role : " + resultSet.getString("role") );
                _titre = resultSet.getString("titre");
                if(resultSet.getString("role").equals("R")) {
                    _realisateurs = resultSet.getString("prenom") + "|" + resultSet.getString("nom");

                }
                else
                    _acteurs = resultSet.getString("prenom") + "|" + resultSet.getString("nom");
                _pays = resultSet.getString("nomPays");
                _annee = resultSet.getInt("annee");
                _duree = resultSet.getInt("duree");
                _autres_titres += resultSet.getString("autres_titres");
                addInfoFilm(_titre, _realisateurs, _acteurs, _pays, _annee, _duree, _autres_titres);
            }
            String retour = "{\"resultat\":[";
            for(String s : infoFilmsMap.keySet())
            {
                //System.out.println("ici titre " + s);
                retour += infoFilmsMap.get(s);
                retour += ",";
            }
            retour = retour.substring(0, retour.length()-1);
            retour += "]}";
            return retour;
        } catch (SQLException e) {
            e.printStackTrace();
            return "";
        }


    }
    public void addInfoFilm(String _titre, String _realisateurs, String _acteurs, String _pays, int _annee, int _duree, String _autres_titres)
    {
        ArrayList<NomPersonne> acteurs = new ArrayList<>(), realisateurs = new ArrayList<>();
        ArrayList<String> autres_titres = new ArrayList<>();
        autres_titres.add(_autres_titres);
        //System.out.println("real : " + _realisateurs + " act : " + _acteurs);
        if(_acteurs.isEmpty())
            realisateurs.add(new NomPersonne( _realisateurs.split("[|]")[1], _realisateurs.split("[|]")[0]));
        else
            acteurs.add(new NomPersonne( _acteurs.split("[|]")[1], _acteurs.split("[|]")[0]));
        if(infoFilmsMap.get(_titre) == null)    // il n'y a pas encore ce film dans la map
        {
            InfoFilm2 infoFilm = new InfoFilm2(_titre, realisateurs, acteurs, _pays, _annee, _duree, autres_titres);
            infoFilmsMap.put(_titre, infoFilm);
        }
        else // ce film est présent dans la map, il faut donc ajouter des réalisateurs ou acteurs
        {
            InfoFilm2 old = infoFilmsMap.get(_titre);
            if(_acteurs.isEmpty())
                infoFilmsMap.get(_titre)._realisateurs.add(realisateurs.get(0));
            else
                infoFilmsMap.get(_titre)._acteurs.add(acteurs.get(0));
        }
    }
    public String toSQLStatement(String simplifiedRequest)
    {
        String titleCondition = toSQLTitleCondition("STAR WARS");
        String fin = ""+//"with filtre as"  + titleCondition +
                "         SELECT " +
                "         films.id_film, films.titre, films.annee, films.duree," +
                "         pays.nom AS nomPays, pays.code AS codePays, pays.continent," +
                "         group_concat(autres_titres.titre, '|') AS autres_titres," +
                "         personnes.prenom, personnes.nom, " +
                "         generique.role " +
                "                FROM films " +
               // "                JOIN films on films.id_film = filtre.id_film " +
                "                LEFT JOIN autres_titres on autres_titres.id_film = films.id_film " +
                "                JOIN pays on pays.code = films.pays " +
                "                JOIN generique on generique.id_film = films.id_film " +
                "                JOIN personnes on personnes.id_personne = generique.id_personne " +
                "                GROUP BY films.titre, films.annee, films.duree, " +
                "                    pays.nom, pays.code, pays.continent, " +
                "                    personnes.prenom, personnes.nom, generique.role";
        fin = "with filtre as (SELECT recherche_titre.id_film from recherche_titre where titre match 'star wars')\n" +
                "         SELECT \n" +
                "         films.id_film, films.titre, films.annee, films.duree,\n" +
                "         pays.nom AS nomPays, pays.code AS codePays, pays.continent,\n" +
                "         group_concat(autres_titres.titre, '|') AS autres_titres,\n" +
                "         personnes.prenom, personnes.nom, \n" +
                "         generique.role \n" +
                "                FROM filtre\n" +
                "                JOIN films on films.id_film = filtre.id_film\n" +
                "                LEFT JOIN autres_titres on autres_titres.id_film = films.id_film\n" +
                "                JOIN pays on pays.code = films.pays\n" +
                "                JOIN generique on generique.id_film = films.id_film\n" +
                "                JOIN personnes on personnes.id_personne = generique.id_personne\n" +
                "                GROUP BY films.titre, films.titre, films.annee, films.duree,\n" +
                "                    pays.nom, pays.code, pays.continent,\n" +
                "                    personnes.prenom, personnes.nom,\n" +
                "                    generique.role";
        return fin;
    }
    public String toSQLTitleCondition(String titles)
    {
        return " (select id_film from recherche_titre where titre match " + titles.trim() + " )";
    }
}
