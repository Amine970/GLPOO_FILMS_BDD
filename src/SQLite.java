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
        String entry = "TITRE Harry Potter OU Star Wars, Marvel"; // input.nextLine();
        rechercheFilm.parseEntryRequest(entry);
        rechercheFilm.buildFinalRequest();
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

