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
        entry = "TITRE Star Wars OU TITRE Harry Potter OU Marvel OU Stagecoach, PAYS GB OU US, après 1980 OU EN 1980";
        //entry = "AVEC Kirk Douglas, Audrey Hepburn OU Marilyn Monroe";
        entry = "EN 2000 ou 2001, pays france, avec a%";
//        rechercheFilm.parseEntryRequest(entry);
        //rechercheFilm.buildFinalRequest(entry.toLowerCase().replaceAll("è", "e").toUpperCase());
        String data = rechercheFilm.retrouve(entry);
        try (FileWriter writer = new FileWriter("sortie.json");
             BufferedWriter bw = new BufferedWriter(writer))
        {

            bw.write(data);

        } catch (IOException e) {
            System.err.format("IOException: %s%n", e);
        }
        System.out.println("abcdefghi".compareTo("b"));

    }


}

