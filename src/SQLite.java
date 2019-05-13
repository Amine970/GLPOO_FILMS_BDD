import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 *
 * @author
 */
public class SQLite {

    public static void main(String[] args)
    {
//        String data = connect();
//        //System.out.println(data);
        Scanner input = new Scanner(System.in);
        RechercheFilm rechercheFilm = new RechercheFilm("bdfilm.sqlite");
        System.out.println("Entrez requête simplifiée");
        String entry = "TITRE because you know OU TITRE yes bruh, PAYS France OU Italie, après 2018"; // input.nextLine();
        entry = "en 2016, pays japon";
//        rechercheFilm.parseEntryRequest(entry);
        //rechercheFilm.buildFinalRequest(entry.toLowerCase().replaceAll("è", "e").toUpperCase());
        String data = rechercheFilm.retrouve(entry);
        rechercheFilm.fermeBase();
        try (FileWriter writer = new FileWriter("sortie.json");
             BufferedWriter bw = new BufferedWriter(writer))
        {

            bw.write(data);

        } catch (IOException e) {
            System.err.format("IOException: %s%n", e);
        }

    }


}

