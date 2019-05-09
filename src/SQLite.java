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
        String entry = "TITRE because you know OU TITRE yes bruh, PAYS France OU Italie, après 2018"; // input.nextLine();
        entry = "TITRE Star Wars OU TITRE Harry Potter OU Marvel OU Stagecoach, PAYS GB OU US, après 1980 OU EN 1980";
        entry = "AVEC Chow Yun-Fat";
//        rechercheFilm.parseEntryRequest(entry);
        //rechercheFilm.buildFinalRequest(entry.toLowerCase().replaceAll("è", "e").toUpperCase());
        String data = rechercheFilm.retrouve(rechercheFilm.toSQLStatement(entry.toLowerCase().replaceAll("è", "e").toUpperCase()));
        try (FileWriter writer = new FileWriter("sortie.json");
             BufferedWriter bw = new BufferedWriter(writer))
        {

            bw.write(data);

        } catch (IOException e) {
            System.err.format("IOException: %s%n", e);
        }

        //String res[] = "TITRE because you know OU TITRE yes bruh, PAYS France OU Italie, APRES 2018".split(",");
        //System.out.println(res.length);
        //for(String x : res)
            //System.out.println(x);
//        System.out.println(res);

    }


}

