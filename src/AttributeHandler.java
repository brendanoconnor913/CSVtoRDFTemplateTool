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

import java.io.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by brendan on 10/30/16.
 */
public class AttributeHandler {
    // TODO: After finding resources need to search graph for any meta data and add to attribute
    // TODO: Need to write a function to add any new data as triples to resource/entity

    private Vector<Attribute> attributes;
    private Model model = ModelFactory.createDefaultModel();
    private Vector<String> header;
    private String filename;

    AttributeHandler(Vector<String> header, String graph) {
        filename = graph;
        model.read(filename);
        this.header = header;
        attributes = new Vector<Attribute>(header.size());
    }

    // takes in column header and puts in uniform format
    private static String formatAttribute(String attr) {
        char[] symbols = {'-',','};// symbols to be removed from attributes
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

    // Output changes to model file
    private void writeToModel() {
        try {
            PrintWriter pw = new PrintWriter(filename);
            model.write(pw, "N-TRIPLES");
            pw.close();
        }
        catch(Exception exec) {
            exec.printStackTrace();
        }
    }

    // adds an alias to existing metagraph
    private void createAlias(Resource r, String alias){
        r.addProperty(ResourceFactory.createProperty("http://umkc.edu/alias.rdf"),
                ResourceFactory.createPlainLiteral(alias));
        writeToModel();
    }

    // Create new entity and add to graph
    private Resource createEntity(String name){
        // Capitalize the first letter then create as entity
        char[] modname = name.toCharArray();
        modname[0] = Character.toUpperCase(modname[0]);
        String frname = new String(modname);
        String rname = "http://umkc.edu/resource/"+frname;
        Resource r = model.createResource(rname);
        r.addProperty(ResourceFactory.createProperty("http://umkc.edu/alias.rdf"),
                ResourceFactory.createPlainLiteral(name));
        writeToModel();
        return r;
    }

    private String getAttributeInput(String unidentified) {
        System.out.println("\nThe system was unable to identify" + unidentified +
                "\n (Press ENTER if you wish to keep given attribute name)");
        String userin;
        System.out.print(unidentified + " : ");
        Scanner s = new Scanner(System.in);
        String line = s.nextLine().trim();
        if(line.equals("")) { // if enter inputted use old attribute
            userin = unidentified;
        }
        else {
            userin = formatAttribute(line);
        }
        return userin;
    }

    // Searches metagraph to try and find attribute if not found asks for user input for other possible alias
    // if found attaches new and old alias to resource if not creates new alias
    public Vector<Attribute> findAttributes() {
        try {
            for(int i = 0; i < header.size(); i++) {
                String current = header.get(i);
                String query = "SELECT ?x WHERE { ?x <http://umkc.edu/alias.rdf> \"" + current + "\".}";
                QueryExecution qexec = QueryExecutionFactory.create(query, model);
                ResultSet results = qexec.execSelect();
                if (!results.hasNext()) { // attribute not identified
                    String in = getAttributeInput(current); // get new attribute name and search again
                    String secondsearch = "SELECT ?x WHERE { ?x <http://umkc.edu/alias.rdf> \"" + in + "\".}";
                    QueryExecution qex = QueryExecutionFactory.create(secondsearch, model);
                    ResultSet sndresult = qex.execSelect();
                    if (!sndresult.hasNext()) { // attribute not identified
                        Resource nr = createEntity(in);
                        createAlias(nr,current);
                        attributes.add(new Attribute(nr));
                    }
                    int rescnt = 0;
                    for( ; results.hasNext(); ) {
                        if(rescnt > 0) { // multiple results returned for alias name
                            throw new Exception("Ambigious alias (alias found under multiple resources" +
                                    " please remove duplicate before continuing.");
                        }
                        QuerySolution soln = results.nextSolution() ;
                        Resource r = soln.getResource("x");
                        createAlias(r,current);
                        System.out.println(r + " was identified from " + in + "(originally was \""
                                + current + "\").");
                        attributes.add(new Attribute(r));
                        rescnt++;
                    }
                }
                else { // alias found on first try
                    int rescnt = 0;
                    for( ; results.hasNext(); ) {
                        if(rescnt > 0) { // multiple results returned for alias name
                            throw new Exception("Ambigious alias (alias found under multiple resources" +
                                    " please remove duplicate before continuing.");
                        }
                        QuerySolution soln = results.nextSolution() ;
                        Resource r = soln.getResource("x");
                        attributes.add(new Attribute(r));
                        System.out.println(r);
                        rescnt++;
                    }
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return attributes;
    }

    public static void main(String args[]) {
        // Reading in the attributes this will be done in the abstractor
        try {
            Reader in = new FileReader(args[0]);
            Iterable<CSVRecord> recordForHeader = CSVFormat.EXCEL.parse(in);
            CSVRecord headerRecord = recordForHeader.iterator().next();
            Vector<String> header = new Vector<String>();
            for(int i = 0; i < headerRecord.size(); i++) {
                String s = formatAttribute(headerRecord.get(i).trim());
                header.add(s);
            }

            AttributeHandler ah = new AttributeHandler(header,"sample.nt");

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
