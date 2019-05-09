import javax.sound.sampled.Line;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.*;
import java.util.*;

public class RechercheFilm
{
    private Connection conn = null;
    private Map<String, StringBuilder> languageMap = new HashMap<>();
    private Map<Integer, InfoFilm2> infoFilmsMap = new HashMap<>();  // string titre
    private Map<String, ArrayList<NomPersonne>> filmsActeurs = new HashMap<>(); // clé : film, valeurs : noms acteurs
    private Map<String, ArrayList<NomPersonne>> filmsRealisateurs = new HashMap<>();
    public RechercheFilm(String monFichierSQLite)
    {
        try
        {
            String url = "jdbc:sqlite:"+monFichierSQLite;
            conn = DriverManager.getConnection(url);
            System.out.println("Connection to SQLite has been established.");

        } catch (SQLException e)
        {
            System.out.println(e.getMessage());
        }
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
        for(String s : languageMap.keySet())
            languageMap.put(s, new StringBuilder(languageMap.get(s).toString().trim().replaceAll(" +"," ")));

        for(String s : languageMap.keySet())
            System.out.println(s + ":" + languageMap.get(s));
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
        int _id;

        try {
            preparedStatement =
                    conn.prepareStatement(requete);
            resultSet = preparedStatement.executeQuery();
            while(resultSet.next())
            {
                _realisateurs="";
                _acteurs="";
                _autres_titres = "null";
                //System.out.println("role : " + resultSet.getString("role") );
                _id = resultSet.getInt("id_film");
                _titre = resultSet.getString("titre");
                //System.out.println("ici titre : " + _titre);
                if(resultSet.getString("role") != null && resultSet.getString("role").equals("R"))
                {
                    _realisateurs = resultSet.getString("prenom") + "|" + resultSet.getString("nom");
                    if(_realisateurs.split("[|]").length == 1)
                    {
                        _realisateurs += "null";
                        //System.out.println("ici");
                    }

                }
                else if(resultSet.getString("role") != null && resultSet.getString("role").equals("A")) {
                    _acteurs = resultSet.getString("prenom") + "|" + resultSet.getString("nom");
                    if (_acteurs.split("[|]").length == 1) {
                        _acteurs += "null";
                        //System.out.println("ici");
                    }
                }
                _pays = resultSet.getString("nomPays");
                _annee = resultSet.getInt("annee");
                _duree = resultSet.getInt("duree");
                _autres_titres = resultSet.getString("autres_titress")!=null?resultSet.getString("autres_titress"):"null";
                addInfoFilm(_titre, _realisateurs, _acteurs, _pays, _annee, _duree, _autres_titres, _id);
            }
            String retour = "{\"resultat\":[";
            List<InfoFilm2> infoFilm2List = new ArrayList<>();

            for(int s : infoFilmsMap.keySet())
            {
                infoFilm2List.add(infoFilmsMap.get(s));
            }
            Collections.sort(infoFilm2List);
            int index = 1;
            for(InfoFilm2 x : infoFilm2List)
            {
                index = 2;
                //System.out.println(index++ + "    " +x._titre);
                retour += x;
                retour += ",";
            }
            /*for(String s : infoFilmsMap.keySet())
            {
                //System.out.println("ici titre " + s);
                retour += infoFilmsMap.get(s);
                retour += ",";
            }*/
            retour = index==1?retour:retour.substring(0, retour.length()-1);
            retour += "]}";
            return retour;
        } catch (SQLException e) {
            e.printStackTrace();
            return "";
        }


    }
    public void addInfoFilm(String _titre, String _realisateurs, String _acteurs, String _pays, int _annee, int _duree, String _autres_titres, int _id)
    {
        ArrayList<NomPersonne> acteurs = new ArrayList<>(), realisateurs = new ArrayList<>();
        ArrayList<String> autres_titres = new ArrayList<>();
        if(!_autres_titres.equals("null"))
            autres_titres.add(_autres_titres);
        //System.out.println("real : " + _realisateurs + " act : " + _acteurs);
        if(_acteurs.isEmpty() && !_realisateurs.isEmpty())
            realisateurs.add(new NomPersonne( _realisateurs.split("[|]")[1], _realisateurs.split("[|]")[0]));
        else if(!_acteurs.isEmpty() && _realisateurs.isEmpty())
            acteurs.add(new NomPersonne( _acteurs.split("[|]")[1], _acteurs.split("[|]")[0]));
        if(infoFilmsMap.get(_id) == null)    // il n'y a pas encore ce film dans la map, je le rajoute
        {
            InfoFilm2 infoFilm = new InfoFilm2(_titre, realisateurs, acteurs, _pays, _annee, _duree, autres_titres, _id);
            infoFilmsMap.put(_id, infoFilm);
        }
        else // ce film est présent dans la map, il faut donc ajouter des réalisateurs ou acteurs
        {
            InfoFilm2 old = infoFilmsMap.get(_id);
            if(_acteurs.isEmpty() && !_realisateurs.isEmpty())
            {
                infoFilmsMap.get(_id)._realisateurs.add(realisateurs.get(0));
            }
            else if(!_acteurs.isEmpty() && _realisateurs.isEmpty())
            {
                infoFilmsMap.get(_id)._acteurs.add(acteurs.get(0));
            }

        }
    }
    public String toSQLStatement(String simplifiedRequest)
    {
        String titleCondition = getConditionTitre("STAR WARS");
        String fin;
        fin = "with filtre as (SELECT id_film from " + getConditionPays("harry potter") + " )" +
                "         SELECT \n" +
                "         films.id_film, films.titre, films.annee, films.duree,\n" +
                "         pays.nom AS nomPays,\n" +
                "         group_concat(autres_titres.titre, '|') AS autres_titress,\n" +
                "         personnes.prenom, personnes.nom, \n" +
                "         generique.role \n" +
                "                FROM filtre\n" +
                "                JOIN films on films.id_film = filtre.id_film\n" +
                "                LEFT JOIN autres_titres on autres_titres.id_film = films.id_film\n" +
                "                JOIN pays on pays.code = films.pays\n" +
                "                LEFT JOIN generique on generique.id_film = films.id_film\n" +
                "                LEFT JOIN personnes on personnes.id_personne = generique.id_personne\n" +
                "                GROUP BY films.id_film, films.titre, films.annee, films.duree,\n" +
                "                    pays.nom,\n" +
                "                    personnes.prenom, personnes.nom,\n" +
                "                    generique.role\n" +
                "                ORDER BY films.annee ASC";
        fin = "with filtre as (" + buildFinalRequest() + " )" +
                "         SELECT \n" +
                "         films.id_film, films.titre, films.annee, films.duree,\n" +
                "         pays.nom AS nomPays,\n" +
                "         group_concat(autres_titres.titre, '|') AS autres_titress,\n" +
                "         personnes.prenom, personnes.nom, \n" +
                "         generique.role \n" +
                "                FROM filtre\n" +
                "                JOIN films on films.id_film = filtre.id_film\n" +
                "                LEFT JOIN autres_titres on autres_titres.id_film = films.id_film\n" +
                "                JOIN pays on pays.code = films.pays\n" +
                "                LEFT JOIN generique on generique.id_film = films.id_film\n" +
                "                LEFT JOIN personnes on personnes.id_personne = generique.id_personne\n" +
                "                GROUP BY films.id_film, films.titre, films.annee, films.duree,\n" +
                "                    pays.nom,\n" +
                "                    personnes.prenom, personnes.nom,\n" +
                "                    generique.role\n" +
                "                ORDER BY films.annee ASC";

        return fin;
    }
    public String getConditionTitre(String title)   // filtre pour avoir ids des films qui match le titre
    {
        return " select id_film from recherche_titre where titre match '" + title + "'  ";
    }
    public String getConditionPays(String pays)
    {
        return  " SELECT films.id_film from films\n" +
                "join pays on films.pays = pays.code\n" +
                "WHERE films.pays like '" + pays + "' or pays.nom like '" + pays + "' ";
    }
    public String getConditionEn(String annee)
    {
        return  " SELECT films.id_film\n" +
                "        from films\n" +
                "        WHERE films.annee = " + annee + "\n" +
                "     ";
    }
    public String getConditionAvant(String annee)
    {
        return  " SELECT films.id_film\n" +
                "        from films\n" +
                "        WHERE films.annee < " + annee + "\n" +
                "     ";
    }
    public String getConditionApres(String annee)
    {
        return  " SELECT films.id_film\n" +
                "        from films\n" +
                "        WHERE films.annee > " + annee + "\n" +
                "     ";
    }
    public String buildFinalRequest()
    {
        Map<String, String> ouMap = new HashMap<>();
        Map<String, String> etMap = new HashMap<>();
        String base;
        String finalS = "";
        String titreTmp = "";
        base = languageMap.get("TITRE").toString().replaceAll("(,$)|(OU$)","").trim();
        base = base.replaceAll("OU",";");
        //System.out.println((int)'§');
        System.out.println(base);
        int ou = 0, et = 0;
//        for(int i = 0; i < base.length(); i++)
//        {
//            char c = base.charAt(i);
//            if(c != ' ' && i != base.length() - 1)
//            {
//                titreTmp += c;
//            }
//            else
//            {
//                if(i == base.length() - 1)
//                    titreTmp += c;
//                if(titreTmp.equals("OU"))
//                {
//                    ou++;
//                    String tmp = "select id_film from ( ";
//                    tmp += finalS;
//                    finalS = tmp;
//                    finalS +=  " union ";
//                    titreTmp = "";
//                }
//                else if(titreTmp.equals(","))
//                {
//                    et++;
//                    finalS += " intersect select id_film form ( select id_film from";
//                    finalS += getConditionTitre(titreTmp);
//                }
//                else
//                    //finalS += " select id_film from " + getConditionTitre(titreTmp);
//                    finalS += titreTmp;
//                titreTmp = "";
//            }
//        }
        //for(int i = 0; i < ou; i++)
            //finalS += ")";
        for(int i = 0; i < base.length(); i++)
        {
            char c = base.charAt(i);
            //System.out.println("index : " + i + " c : " + c + " et et = " + et);
            if(c == ';' && et == 0 )// si on rencontre un ou sans avoir eu de et avant X ou Y
            {
                ou = 1;
                finalS += "select id_film from (";
                finalS += getConditionTitre(titreTmp);
                finalS += " union ";
                titreTmp = "";
            }
            else if(c == ';' && et == 1)// si on rencontre un ou après avoir eu un ET : X et Y ou Z
            {
                et = 0;
                ou = 1;
                finalS += "select id_film from (";
                finalS += getConditionTitre(titreTmp);
                finalS += " union ";
                titreTmp = "";
            }
            else if(c == ',' && ou == 0) // si on rencontre un et sans avoir eu de ou : X et Y
            {

                et = 1;
                finalS += getConditionTitre(titreTmp);
                finalS += " intersect ";
                titreTmp = "";
            }
            else if(c == ',' && ou == 1) // si on rencontre un et après avoir eu un ou :  X ou Y et Z
            {
                ou = 0;
                et = 1;
                finalS += getConditionTitre(titreTmp);
                finalS += ") ";
                finalS += " intersect ";
                titreTmp = "";
            }
            else if(i == base.length() - 1)
            {

                titreTmp+=c;
                finalS += getConditionTitre(titreTmp);
                if(ou == 1)
                {
                    finalS += ")";
                }
                titreTmp = "";
            }
            else
                titreTmp+=c;
        }
        System.out.println(finalS);
        return finalS;
    }
}