import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.jena.atlas.io.CharStream;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.TDBLoader;
import org.apache.jena.util.FileManager;
import org.javatuples.Pair;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Scanner;
import java.util.Vector;

/**
 * Created by brendan on 10/30/16.
 */
public class AttributeHandler {
    // TODO: After finding resources need to check if any meta data to return
    // TODO: Need to write a function to add any new data as triples to resource/entity

    public static String formatAttribute(String attr) {
        char[] symbols = {'-',','}; // symbols to be removed from attributes
        StringBuilder sb = new StringBuilder();
        // remove symbols, spaces -> _ , and convert to all lower case
        for(char c : attr.toCharArray()) {
            if(c == ' ') {
                char prev = sb.charAt(sb.length()-1);
                if(prev != '_') {
                    sb.append('_');
                }
            }
            else if (ArrayUtils.contains(symbols, c)) continue;
            else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }
    public static void main(String args[]) {
        try {

            // Reading in the attributes
            String filename = args[0];
            Reader in = new FileReader(filename);
            Iterable<CSVRecord> recordForHeader = CSVFormat.EXCEL.parse(in);
            CSVRecord headerRecord = recordForHeader.iterator().next();
            Vector<String> header = new Vector();
            for(int i = 0; i < headerRecord.size(); i++) {
                String s = formatAttribute(headerRecord.get(i).trim());
                header.add(s);
            }

            // Array to store final attributes used to construct quads
            String[] finalAttrs = new String[headerRecord.size()];

            // First attempt to identify
            Vector<Pair<String, Integer>> unidentified = new Vector<Pair<String, Integer>>();
            Model model = ModelFactory.createDefaultModel();
            model.read("sample.nt");
            for(int i = 0; i < header.size(); i++) {
                String current = header.get(i);
                String query = "SELECT ?x WHERE { ?x <http://umkc.edu/alias.rdf#> \"" + current + "\".}";
                QueryExecution qexec = QueryExecutionFactory.create(query, model);
                ResultSet results = qexec.execSelect();
                if (!results.hasNext()) { // attribute not identified
                    System.out.println("Resource for " + current +
                            " was not found.");
                    unidentified.add(new Pair<String, Integer>(current,i));
                }
                for( ; results.hasNext(); ) {
                    // TODO: add resource to finalAttrs array (not sure if they may be multiple results)
                    QuerySolution soln = results.nextSolution() ;
                    Resource r = soln.getResource("x");
                    System.out.println(r);
                }
            }

            // Get attribute name from user
            Vector<Pair<String, Integer>> input = new Vector<Pair<String,Integer>>();
            System.out.println("The system was unable to identify the following items "+ unidentified.size()
                    + "." + " Please enter a name after \":\" to be used to create a new resource for each item.");
            int number = 1;
            for(Pair<String, Integer> p : unidentified) {
                String userin;
                System.out.print(number + ". " + p.getValue0() + " : ");
                Scanner s = new Scanner(System.in);
                String line = s.nextLine().trim();
                if(line.equals("")) { // if enter inputted use old attribute
                    userin = p.getValue0();
                }
                else {
                    userin = formatAttribute(line);
                }
                input.add(new Pair<String, Integer>(userin, p.getValue1()));
                number++;
            }

//            // Attempt to re-identify attribute from user input
//            for(String s : unidentified) {
//                String query = "SELECT ?x WHERE { ?x <http://umkc.edu/alias.rdf#> \"" + s + "\".}";
//                QueryExecution qexec = QueryExecutionFactory.create(query, model);
//                ResultSet results = qexec.execSelect();
//
//                for( ; results.hasNext(); ) {
//                    QuerySolution soln = results.nextSolution() ;
//                    Resource r = soln.getResource("x");
//                    System.out.println(r);
//                }
//            }

        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
