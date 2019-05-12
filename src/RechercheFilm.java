import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.*;
import java.util.*;

public class RechercheFilm
{
    private Connection conn = null;
    private Map<Integer, InfoFilm2> infoFilmsMap = new HashMap<>();  // string titre
    private List<String> arguments = new ArrayList<>();
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
    private void fetchData(String requete) throws SQLException {
        ResultSet resultSet;
        String autres_titres, prenom, nom;
        PreparedStatement preparedStatement =
                conn.prepareStatement(requete);
        System.out.println(arguments.size());
        for(int i = 1; i <= arguments.size(); i++)
            preparedStatement.setString(i,arguments.get(i-1));
        resultSet = preparedStatement.executeQuery();
        while(resultSet.next())
        {
            prenom = null;
            nom = null;
            if(resultSet.getString("role") != null && resultSet.getString("role").equals("R"))
            {
                prenom = resultSet.getString("prenom") ;
                nom = resultSet.getString("nom");
            }
            else if(resultSet.getString("role") != null && resultSet.getString("role").equals("A"))
            {
                prenom = resultSet.getString("prenom");
                nom = resultSet.getString("nom");
            }
            //
            autres_titres = resultSet.getString("autres_titress")!=null?resultSet.getString("autres_titress"):"";
            addInfoFilm(resultSet.getString("titre"), prenom, nom, resultSet.getString("role"), resultSet.getString("nomPays"), resultSet.getInt("annee"),
                    resultSet.getInt("duree"), autres_titres, resultSet.getInt("id_film"));

        }
    }
    public String retrouve(String requete)
    {
        requete = toSQLStatement(requete);
        if(requete.substring(0,6).equals("fail :"))
            return "{\"erreur\":\""+requete+"\"}";

        try {
            fetchData(requete);
        }
        catch (SQLException e) {
            e.printStackTrace();
            return "";
        }
        StringBuilder retour = new StringBuilder("{\"resultat\":[");
        List<InfoFilm2> infoFilm2List = new ArrayList<>();

        for(int s : infoFilmsMap.keySet())
        {
            infoFilm2List.add(infoFilmsMap.get(s));
        }
        Collections.sort(infoFilm2List);
        int size = infoFilm2List.size();
        for(int i = 0; i < Math.min(size, 100); i++)
        {
            retour.append(infoFilm2List.get(i));
            retour.append(",");
        }
        retour = new StringBuilder(size == 0 ? retour.toString() : retour.substring(0, retour.length() - 1));
        retour.append("]");
        if(size > 100)
            retour.append(",\"info\":\"Résultat limité à 100 films\"}");
        else
            retour.append("}");
        return retour.toString();
    }
    private void addInfoFilm(String _titre, String prenom, String nom, String role, String _pays, int _annee, int _duree, String _autres_titres, int _id)
    {
        ArrayList<NomPersonne> acteurs = new ArrayList<>(), realisateurs = new ArrayList<>();
        ArrayList<String> autres_titres = new ArrayList<>();
        TreeSet<String> setAutresTitres = new TreeSet<>();
        if(role != null)
            if(role.equals("A"))
                acteurs.add(new NomPersonne(nom, prenom));
            else
                realisateurs.add(new NomPersonne(nom, prenom));
        if(!_autres_titres.isEmpty())
        {
            Collections.addAll(setAutresTitres, _autres_titres.split("[|]"));
            autres_titres.addAll(setAutresTitres);
        }
        if(infoFilmsMap.get(_id) == null)    // il n'y a pas encore ce film dans la map, je le rajoute
        {
            InfoFilm2 infoFilm = new InfoFilm2(_titre, realisateurs, acteurs, _pays, _annee, _duree, autres_titres, _id);
            infoFilmsMap.put(_id, infoFilm);
        }
        else // ce film est présent dans la map, il faut donc ajouter des réalisateurs ou acteurs
        {
            ArrayList<NomPersonne> tmp;
            if(role != null)
            {
                if(role.equals("A"))
                {
                    tmp = new ArrayList<>(infoFilmsMap.get(_id).get_acteurs());
                    tmp.add(acteurs.get(0));
                    infoFilmsMap.put(_id, new InfoFilm2(_titre, infoFilmsMap.get(_id).get_realisateurs() , tmp, _pays, _annee, _duree, autres_titres, _id));
                }
                else
                {
                    tmp = new ArrayList<>(infoFilmsMap.get(_id).get_realisateurs());
                    tmp.add(realisateurs.get(0));
                    infoFilmsMap.put(_id, new InfoFilm2(_titre, tmp, infoFilmsMap.get(_id).get_acteurs(), _pays, _annee, _duree, autres_titres, _id));
                }
            }

        }
    }
    public String toSQLStatement(String simplifiedRequest)
    {
        String fin;
        StringBuilder retour = new StringBuilder();
        simplifiedRequest = simplifiedRequest.toLowerCase().replaceAll("è", "e").toUpperCase();
        String builtQuery = simpleRequestToSQL(simplifiedRequest);
        if(builtQuery.substring(0,6).equals("fail :"))
            return builtQuery;
        fin = "with filtre as (" + builtQuery + " )" +
                "         SELECT " +
                "         films.id_film, films.titre, films.annee, films.duree, " +
                "         pays.nom AS nomPays, " +
                "         group_concat(autres_titres.titre, '|') AS autres_titress, " +
                "         personnes.prenom, personnes.nom,  " +
                "         generique.role " +
                "                FROM filtre " +
                "                JOIN films on films.id_film = filtre.id_film " +
                "                LEFT JOIN autres_titres on autres_titres.id_film = films.id_film " +
                "                JOIN pays on pays.code = films.pays " +
                "                LEFT JOIN generique on generique.id_film = films.id_film " +
                "                LEFT JOIN personnes on personnes.id_personne = generique.id_personne " +
                "                GROUP BY films.id_film, films.titre, films.annee, films.duree, " +
                "                    nomPays, " +
                "                    personnes.prenom, personnes.nom, " +
                "                    generique.role ";

        return fin;
    }
    private String getConditionTitre(String titre)   // filtre pour avoir ids des films qui match le titre
    {
        arguments.add(titre);
        return " select id_film from recherche_titre where titre match ? ";
    }
    private String getConditionPays(String pays)
    {
        arguments.add(pays);
        arguments.add(pays);
        return  " SELECT films.id_film from films join pays on films.pays = pays.code WHERE films.pays like ? or pays.nom like ? ";
    }
    private String getConditionAnnee(String annee, String operator)
    {
        try
        {
            Integer.parseInt(annee);
        }
        catch (Exception e)
        {
            return "fail : les mots clés APRES, AVANT et EN doivent être suivi d'un nombre entier (l'année)";
        }
        arguments.add(annee);
        return " SELECT films.id_film from films  WHERE films.annee " + operator + " ? ";
    }
    private String getConditionNomPrenom(String ch1, String ch2, String role)
    {
        arguments.add(role);
        arguments.add(ch1.replaceAll("^MC", "MAC"));
        arguments.add(ch1.replaceAll("^MC", "MAC"));
        arguments.add(ch2);
        arguments.add(ch2);
        arguments.add(role);
        arguments.add(ch2.replaceAll("^MC", "MAC"));
        arguments.add(ch2.replaceAll("^MC", "MAC"));
        arguments.add(ch1);
        arguments.add(ch1);

        return "SELECT id_film\n" +
                "from generique " +
                "join personnes on generique.id_personne = personnes.id_personne " +
                "where generique.role = ? " +
                " and (personnes.nom like ?  or personnes.nom_sans_accent like ? ) " +
                " and (personnes.prenom like ? || '%' or personnes.prenom_sans_accent like ? || '%' ) " +
                " union " +
                "SELECT id_film\n" +
                "from generique " +
                "join personnes on generique.id_personne = personnes.id_personne " +
                "where generique.role = ? " +
                " and (personnes.nom like ? or personnes.nom_sans_accent like ? ) " +
                " and (personnes.prenom like ? || '%' or personnes.prenom_sans_accent like ? || '%' ) ";
    }
    public String getConditionPersonnes(String nomPrenom, String role)
    {
        nomPrenom = nomPrenom.trim().replaceAll(" +", " ");
        String split[] = nomPrenom.split(" ");
        if(split.length == 1)   // Si un seul terme, on cherche uniquement le nom
        {
            arguments.add(role);
            arguments.add(split[0].replaceAll("^MC", "MAC"));
            arguments.add(split[0].replaceAll("^MC", "MAC"));

            return  "SELECT id_film\n" +
                    "from generique " +
                    "join personnes on generique.id_personne = personnes.id_personne " +
                    "where generique.role = ? and (personnes.nom like ? or personnes.nom_sans_accent like ?) " ;
        }
        else if(split.length >= 2) // 2 termes // on considère le nom bon, le prénom n'est que le commencement, on check inverse
        {
            StringBuilder res = new StringBuilder();
            res.append("select id_film from (");
            StringBuilder tmp1 = new StringBuilder();
            StringBuilder tmp2 = new StringBuilder();
            for(int max = 0; max < split.length-1; max++)
            {
                for(int i = 0; i <= max; i++)
                {
                    tmp1.append(split[i]);
                    tmp1.append(" ");
                }

                for(int i = max + 1; i < split.length; i++)
                {
                    tmp2.append(split[i]);
                    tmp2.append(" ");
                }
                res.append(getConditionNomPrenom(tmp1.toString().trim(), tmp2.toString().trim(), role));
                if(max != split.length - 2)
                    res.append(" union ");
                tmp1 = new StringBuilder();
                tmp2 = new StringBuilder();
            }

            res.append(")");
                    return res.toString();
        }
        return "";
    }

    public String switchOnKeyword(String keyword, String condition)
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
                return getConditionAnnee(condition,"=");
            case "AVANT" :
                return getConditionAnnee(condition, "<");
            case "APRES" :
                return getConditionAnnee(condition, ">");
            case "DE" :
                return getConditionPersonnes(condition, "R");
            case "AVEC" :
                return getConditionPersonnes(condition, "A");
        }

        return "fail : ... somehow";
    }
    private String simpleRequestToSQL(String request)
    {
        String keyWords = "TITRE|DE|AVEC|PAYS|EN|AVANT|APRES";
        StringBuilder finalS = new StringBuilder();
        String wordTmp;
        String[] splitOnAnd = request.split(",");
        String lastKeyWord = "";
        for(int i = 0; i < splitOnAnd.length; i++)
        {
            String splitOnAndTmp = splitOnAnd[i].trim(); // exemple [TITRE A OU TITRE B]
            String splitOnOu[] = splitOnAndTmp.split(" OU ");
            if(splitOnOu.length >= 2) // => il y a au moins un ou
                finalS.append(" select id_film from (\n");

            for(int j = 0; j < splitOnOu.length; j++)
            {
                //System.out.print(splitOnOu[j] + "|");
                String subOu = splitOnOu[j].trim();    // exemple [TITRE A]
                wordTmp = "";
                int spaceCounter = 0;
                for(int k = 0; k < subOu.length(); k++)
                {
                    char c = subOu.charAt(k);
                    if(c == ' ' && spaceCounter == 0) // Quand on arrive sur le premier espace
                    {
                        spaceCounter++;
                        if(keyWords.contains(wordTmp))
                        {
                            lastKeyWord = wordTmp; // save le keyword au cas où l'utilisateur ne réécrit pas (ex TITRE A OU B)
                            wordTmp = "";
                        }
                        else
                        {
                            wordTmp += c;
                            if(lastKeyWord.isEmpty()) // si le premier mot n'est pas un keyword et qu'il n'y a pas de lastKeyWord, fail
                                return "fail : pas de mot keyword détecté : (TITRE|DE|AVEC|PAYS|EN|AVANT|APRES)";
                        }
                    }
                    else
                        wordTmp += c;
                    if(k == subOu.length() - 1) // j'arrive à la fin du [TITRE A]
                    {
                        if(!lastKeyWord.isEmpty())
                        {
                            finalS.append(switchOnKeyword(lastKeyWord, wordTmp));
                        }
                        else
                        {
                            return !keyWords.contains(wordTmp) ? "fail : pas de mot keyword détecté : (TITRE|DE|AVEC|PAYS|EN|AVANT|APRES)" : "fail : keyword trouvé mais pas de condition";
                        }
                    }
                }
                if(j != splitOnOu.length - 1) // il reste encore des ou
                    finalS.append(" union ");
            }
            //System.out.print("  |||||   ");
            if(splitOnOu.length >= 2) // => il y a au moins un ou
                finalS.append(" )\n");
            if(i != splitOnAnd.length -1)
                finalS.append(" intersect ");
        }
        System.out.println(finalS.toString().replaceAll(" +", " "));
        return finalS.toString().replaceAll(" +", " ");
    }
}