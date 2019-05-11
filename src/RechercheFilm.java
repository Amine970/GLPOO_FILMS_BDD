import org.jetbrains.annotations.NotNull;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.*;
import java.util.*;

public class RechercheFilm
{
    private Connection conn = null;
    private Map<Integer, InfoFilm2> infoFilmsMap = new HashMap<>();  // string titre
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
    public String retrouve(String requete)
    {

        if(requete.substring(0,6).equals("fail :"))
            return "{\"erreur\":\""+requete+"\"}";
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
        String prenom, nom;
        try {
            preparedStatement =
                    conn.prepareStatement(requete);
            resultSet = preparedStatement.executeQuery();
            while(resultSet.next())
            {
                prenom = null;
                nom = null;
                _realisateurs="";
                _acteurs="";
                _autres_titres = "";
                //System.out.println("role : " + resultSet.getString("role") );
                _id = resultSet.getInt("id_film");
                _titre = resultSet.getString("titre");
                //System.out.println("ici titre : " + _titre);
                //
                if(resultSet.getString("role") != null && resultSet.getString("role").equals("R"))
                {
                    _realisateurs = "xD";
                    prenom = resultSet.getString("prenom") ;
                    nom = resultSet.getString("nom");
                }
                else if(resultSet.getString("role") != null && resultSet.getString("role").equals("A"))
                {
                    _acteurs = "xD";
                    prenom = resultSet.getString("prenom");
                    nom = resultSet.getString("nom");
                }
                //

                _pays = resultSet.getString("nomPays");
                _annee = resultSet.getInt("annee");
                _duree = resultSet.getInt("duree");
                _autres_titres = resultSet.getString("autres_titress")!=null?resultSet.getString("autres_titress"):"";
                //System.out.println(_autres_titres);
                addInfoFilm(_titre, prenom, nom, _realisateurs, _acteurs, _pays, _annee, _duree, _autres_titres, _id);
            }
            String retour = "{\"resultat\":[";
            List<InfoFilm2> infoFilm2List = new ArrayList<>();

            for(int s : infoFilmsMap.keySet())
            {
                infoFilm2List.add(infoFilmsMap.get(s));
            }
            Collections.sort(infoFilm2List);
            int size = infoFilm2List.size();
            for(int i = 0; i < Math.min(size, 100); i++)
            {
                retour += infoFilm2List.get(i);
                retour += ",";
            }
            retour = size==0?retour:retour.substring(0, retour.length()-1);
            retour += "]";
            if(size > 100)
                retour += ",\"info\":\"Résultat limité à 100 films\"}";
            else
                retour += "}";
            return retour;
        } catch (SQLException e) {
            e.printStackTrace();
            return "";
        }
    }
    public void addInfoFilm(String _titre, String prenom, String nom, String _realisateurs, String _acteurs, String _pays, int _annee, int _duree, String _autres_titres, int _id)
    {
        ArrayList<NomPersonne> acteurs = new ArrayList<>(), realisateurs = new ArrayList<>();
        ArrayList<String> autres_titres = new ArrayList<>();
        TreeSet<String> setAutresTitres = new TreeSet<>();
        //System.out.println("real : " + _realisateurs + " act : " + _acteurs);
        if(_acteurs.isEmpty() && !_realisateurs.isEmpty())
            realisateurs.add(new NomPersonne(nom, prenom));
        else if(!_acteurs.isEmpty() && _realisateurs.isEmpty())
            acteurs.add(new NomPersonne(nom, prenom));
        if(infoFilmsMap.get(_id) == null)    // il n'y a pas encore ce film dans la map, je le rajoute
        {
            if(!_autres_titres.isEmpty())
            {
                for(String s : _autres_titres.split("[|]"))
                    setAutresTitres.add(s);
                for(String s : setAutresTitres)
                    autres_titres.add(s);
            }
            InfoFilm2 infoFilm = new InfoFilm2(_titre, realisateurs, acteurs, _pays, _annee, _duree, autres_titres, _id);
            infoFilmsMap.put(_id, infoFilm);
        }
        else // ce film est présent dans la map, il faut donc ajouter des réalisateurs ou acteurs
        {
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
        String fin;
        String builtQuery = buildFinalRequest(simplifiedRequest);
        if(builtQuery.substring(0,6).equals("fail :"))
            return builtQuery;
        fin = "with filtre as (" + builtQuery + " )" +
                "         SELECT \n" +
                "         films.id_film, films.titre, films.annee, films.duree,\n" +
                "         pays.nom AS nomPays,\n" +
                "         group_concat(autres_titres.titre, '|') AS autres_titress,\n" +
                "         personnes.prenom, personnes.nom, \n" +
                "         generique.role\n" +
                "                FROM filtre\n" +
                "                JOIN films on films.id_film = filtre.id_film\n" +
                "                LEFT JOIN autres_titres on autres_titres.id_film = films.id_film\n" +
                "                JOIN pays on pays.code = films.pays\n" +
                "                LEFT JOIN generique on generique.id_film = films.id_film\n" +
                "                LEFT JOIN personnes on personnes.id_personne = generique.id_personne\n" +
                "                GROUP BY films.id_film, films.titre, films.annee, films.duree,\n" +
                "                    nomPays,\n" +
                "                    personnes.prenom, personnes.nom,\n" +
                "                    generique.role\n";

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
    private String getConditionNomPrenom(String ch1, String ch2, String role)
    {
        return "SELECT id_film\n" +
                "from generique " +
                "join personnes on generique.id_personne = personnes.id_personne " +
                "where generique.role = '"+role+"'" +
                " and (personnes.nom like '" + ch1.replaceAll("^MC", "MAC") + "' or personnes.nom_sans_accent like '" + ch1.replaceAll("^MC", "MAC") + "') " +
                " and (personnes.prenom like '" + ch2 + "%' or personnes.prenom_sans_accent like '" + ch2 + "%') " +
                " union " +
                "SELECT id_film\n" +
                "from generique " +
                "join personnes on generique.id_personne = personnes.id_personne " +
                "where generique.role = '"+role+"'" +
                " and (personnes.nom like '" + ch2.replaceAll("^MC", "MAC") + "' or personnes.nom_sans_accent like '" + ch2.replaceAll("^MC", "MAC") + "') " +
                " and (personnes.prenom like '" + ch1 + "%' or personnes.prenom_sans_accent like '" + ch1 + "%') ";
    }
    public String getConditionPersonnes(String nomPrenom, String role)
    {
        nomPrenom = nomPrenom.trim().replaceAll(" +", " ");
        String split[] = nomPrenom.split(" ");
        if(split.length == 1)   // Si un seul terme, on cherche uniquement le nom
        {
            //System.out.println("ici mdr");
            return  "SELECT id_film\n" +
                    "from generique " +
                    "join personnes on generique.id_personne = personnes.id_personne " +
                    "where generique.role = '"+role+"' and (personnes.nom like '" + split[0].replaceAll("^MC", "MAC") + "' or personnes.nom_sans_accent like '" + split[0].replaceAll("^MC", "MAC") + "') " ;
        }
        else if(split.length >= 2) // 2 termes // on considère le nom bon, le prénom n'est que le commencement, on check inverse
        {
            String res = "";
            res += "select id_film from (";
//            +
//                            getConditionNomPrenom(split[0], split[1]) +
            String tmp1 = "", tmp2 = "";
            for(int max = 0; max < split.length-1; max++)
            {
                for(int i = 0; i <= max; i++)
                {
                    tmp1 += split[i];
                    tmp1 += " ";
                }

                for(int i = max + 1; i < split.length; i++)
                {
                    tmp2 += split[i];
                    tmp2 += " ";
                }
                res += getConditionNomPrenom(tmp1.trim(), tmp2.trim(), role);
                if(max != split.length - 2)
                    res += " union ";
                tmp1 = "";
                tmp2 = "";
                //System.out.println("ici max : " + max + " et res : "  + res);
            }

            res += ")";
                    return res;
        }
        return "";
    }

    public String getConditionDe(String nomPrenom)
    {
        return "";
    }

    public String getConditionGeneral(String keyword, String condition)
    {
        condition = condition.trim();
        System.out.println("key word : " + keyword + " condition : " + condition);
        switch (keyword)
        {
            case "TITRE" :
                return getConditionTitre(condition);
            case "PAYS" :
                return getConditionPays(condition);
            case "EN" :
                return getConditionEn(condition);
            case "AVANT" :
                return getConditionAvant(condition);
            case "APRES" :
                return getConditionApres(condition);
            case "DE" :
                return getConditionPersonnes(condition, "R");
            case "AVEC" :
                return getConditionPersonnes(condition, "A");
        }

        return "fail : normalement j'arrive jamais ici";
    }
    public String buildFinalRequest(@NotNull String request)
    {
        String keyWords = "TITRE|DE|AVEC|PAYS|EN|AVANT|APRES";
        String finalS = "";
        String wordTmp = "";
        String splitOnAnd[] = request.split(",");
        String lastKeyWord = "";
        for(int i = 0; i < splitOnAnd.length; i++)
        {
            String splitOnAndTmp = splitOnAnd[i].trim(); // exemple [TITRE A OU TITRE B]
            String splitOnOu[] = splitOnAndTmp.split(" OU ");
            if(splitOnOu.length >= 2) // => il y a au moins un ou
                finalS += " select id_film from (\n";

            for(int j = 0; j < splitOnOu.length; j++)
            {
                //System.out.print(splitOnOu[j] + "|");
                String subOu = splitOnOu[j].trim();    // exemple [TITRE A]
                wordTmp = "";
                int spaceCounter = 0;
                for(int k = 0; k < subOu.length(); k++)
                {
                    //System.out.println("final : " + finalS);
                    //System.out.println("mot : " + wordTmp);
                    char c = subOu.charAt(k);
                    if(c == ' ' && spaceCounter == 0) // Quand on arrive sur le premier espace
                    {
                        spaceCounter++;
                        //System.out.println(wordTmp);
                        if(keyWords.indexOf(wordTmp) != -1)
                        {
                            lastKeyWord = wordTmp; // save le keyword au cas où l'utilisateur ne réécrit pas (ex TITRE A OU B)
                            wordTmp = "";
                        }
                        else
                        {
                            //System.out.println("ce n'est pas un mot clé c'est " + wordTmp);
                            wordTmp += c;
                            if(lastKeyWord.isEmpty()) // si le premier mot n'est pas un keyword et qu'il n'y a pas de lastKeyWord, fail
                                return "fail : pas de mot keyword détecté : (TITRE|DE|AVEC|PAYS|EN|AVANT|APRES)";
                        }
                    }
                    else
                        wordTmp += c;
                    if(k == subOu.length() - 1) // j'arrive à la fin du [TITRE A]
                    {
                       //System.out.println("ici j'ai : " + wordTmp);
                        if(!lastKeyWord.isEmpty())
                        {
                            finalS += getConditionGeneral(lastKeyWord, wordTmp);
                        }
                        else
                        {
                            return keyWords.indexOf(wordTmp) == -1 ? "fail : pas de mot keyword détecté : (TITRE|DE|AVEC|PAYS|EN|AVANT|APRES)" : "fail : keyword trouvé mais pas de condition";
                        }
                    }
                }
                if(j != splitOnOu.length - 1) // il reste encore des ou
                    finalS += " union ";
            }
            //System.out.print("  |||||   ");
            if(splitOnOu.length >= 2) // => il y a au moins un ou
                finalS += " )\n";
            if(i != splitOnAnd.length -1)
                finalS += " intersect ";
        }
        System.out.println(finalS.replaceAll(" +", " "));
        return finalS.replaceAll(" +", " ");
    }
}