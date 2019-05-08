import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 *
 * @author
 */
public class SQLite {
    /**
     * Connect to a sample database
     */
    public static String connect() {
        String res="";
        Connection conn = null;
        try {
            // db parameters
            String url = "jdbc:sqlite:C:/Users/amine/Documents/Intellij/GLPOO/bdfilm.sqlite";
            // create a connection to the database
            conn = DriverManager.getConnection(url);
            System.out.println("Connection to SQLite has been established.");

            /*PreparedStatement selectFilmByCountry =
                    conn.prepareStatement("SELECT * from films WHERE pays LIKE ?");*/
            PreparedStatement selectFilmByCountry =
                    conn.prepareStatement("SELECT id_film, titre from recherche_titre where titre match 'STAR WARS'");
            /*

            private String                 _titre;
    private ArrayList<NomPersonne> _realisateurs;
    private ArrayList<NomPersonne> _acteurs;
    private String                 _pays;
    private int                    _annee;
    private int                    _duree;
    private ArrayList<String>      _autres_titres;
             */
            //String statement = "SELECT films.titre, films.pays, films.annee, films.duree,   ";
            /*String statement = "select <données de la table films+ pays> titre, annee, pays, duree, code " +
                    "prenom, nom, role " +
                    "from films f " +
                    "join pays py on py.code_pays = f.pays " +
                    "join generique g on g.id_film = f.id_film " +
                    "join personnes p on p.id_personne = g.id_personne";*/

            //selectFilmByCountry.setString(1, "fr");
            ResultSet resultSet = selectFilmByCountry.executeQuery();
//            String querry = "SELECT * FROM films";
//            Statement statement = conn.createStatement();
//            ResultSet resultSet = statement.executeQuery(querry);

            while(resultSet.next())
            {
                        res+=
                        /*"id : " +
                        resultSet.getInt("id_film") +*/
                        ", titre : " +
                        resultSet.getString("titre") +
                        //", pays : " + resultSet.getString("pays") +
                                "\n";
            }



        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }
        return res;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
//        String data = connect();
//        //System.out.println(data);
        Scanner input = new Scanner(System.in);
        RechercheFilm rechercheFilm = new RechercheFilm("bdfilm.sqlite");
        System.out.println("Entrez requête simplifiée");
        String entry = "x"; // input.nextLine();
        rechercheFilm.parseEntryRequest(entry);
        String data = rechercheFilm.retrouve(rechercheFilm.toSQLStatement(entry));
        try (FileWriter writer = new FileWriter("sortie.json");
             BufferedWriter bw = new BufferedWriter(writer))
        {

            bw.write(data);

        } catch (IOException e) {
            System.err.format("IOException: %s%n", e);
        }

//        String res = ";rihanna".split(";")[0];
//        System.out.println(res);

    }


}

