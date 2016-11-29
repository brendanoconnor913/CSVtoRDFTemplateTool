import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.jena.atlas.io.CharStream;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import java.io.*;
import java.util.*;
import java.util.Vector;

/**
 * Created by brendan on 10/30/16.
 */
public class AttributeHandler {
    private Model model = ModelFactory.createDefaultModel();
    private String filename;

    AttributeHandler(String graph) {
        filename = graph;
        model.read(filename);
    }

    // takes in column header and puts in uniform format
    public static String formatAttribute(String attr) {
        char[] symbols = {' ','-',',','<','>','%','(',')','/','\\'};// symbols to be removed from attributes
        StringBuilder sb = new StringBuilder();
        // remove symbols, spaces -> _ , and convert to all lower case
        for(char c : attr.toCharArray()) {
            if(ArrayUtils.contains(symbols, c)) {
                char prev = sb.charAt(sb.length()-1);
                if(prev != '_') {
                    sb.append('_');
                }
            }
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

    // adds pred,obj to subj and store in graph
    private void createTriple(Resource r, String predicate, String object){
        r.addProperty(ResourceFactory.createProperty(predicate),
                ResourceFactory.createPlainLiteral(object));
        writeToModel();
    }

    // adds an alias to existing metagraph
    private void createAlias(Resource r, String alias){
        createTriple(r,"http://umkc.edu/alias.rdf",alias);
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
        System.out.println("\nThe system was unable to identify \"" + unidentified +
                "\"\n\t(Press ENTER if you wish to keep given attribute name)");
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
    public Vector<Attribute> findAttributes(Vector<String> header) {
        Vector<Attribute> attributes = new Vector<Attribute>(header.size());
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
                    else {
                        int rescnt = 0;
                        for( ; sndresult.hasNext(); ) {
                            if(rescnt > 0) { // multiple results returned for alias name
                                throw new Exception("Ambigious alias (alias found under multiple resources" +
                                        " please remove duplicate before continuing.");
                            }
                            QuerySolution soln = sndresult.nextSolution() ;
                            Resource r = soln.getResource("x");
                            createAlias(r,current);
                            System.out.println(r + " was identified from " + in + "(originally was \""
                                    + current + "\").");
                            attributes.add(new Attribute(r));
                            rescnt++;
                        }
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

    private Vector<Attribute> checkDependency(Vector<String> header, Vector<Attribute> attributes) throws Exception {
        if(header.size() != attributes.size()){
            throw new Exception("Header size needs to match attribute size... attribute may have been" +
                    " lost in process");
        }
        Vector<Attribute> dAttributes = (Vector<Attribute>)attributes.clone();


        for(int i = 0; i < header.size(); i++) {
            String s = header.get(i).trim();
            System.out.println(i + " : " + s);
        }
        System.out.println("\nPlease indicate if any column specifies the unit of measurement for another column");
        System.out.println("If there aren't any unit columns simply press enter");
        System.out.println("The format to do so is \"UNIT_COL_NUM,MEASURE_COL_NUM:" +
                "UNIT_COL2,MEASURE_COL2:UNIT_COL3,MEASURE_COL3\"\n");
        System.out.print("Enter dependencies as instructed above: ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String line = br.readLine().trim();
        if(!line.equals("")) {
            String[] dependencies = line.split(":");
            if(!dependencies[0].equals("")){
                for(int i = 0; i < dependencies.length; i++){
                    String [] pair = dependencies[i].split(",");
                    if(!pair[0].equals("")) {
                        if(pair.length != 2) {
                            System.out.println(pair.length);
                            System.out.println("\""+pair[0]+"\"");
                            throw new Exception("Columns did not adhere to format given.");
                        }
                        Integer unit = Integer.parseInt(pair[0].trim());
                        Integer col = Integer.parseInt(pair[1].trim());
                        Attribute a = dAttributes.get(col);
                        a.unitcol = unit;
                        a.metadata.put("Unit", "DEPENDENCY");
                    }
                }
            }
        }
        return dAttributes;
    }

    private Boolean isResourceAMeasurement(Resource r) {
        Boolean isMes = false;
        String query = "SELECT ?x WHERE { <"+r.toString()+"> <http://umkc.edu/alias.rdf> ?x .}";
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet results = qexec.execSelect();
        for( ; results.hasNext(); ) {
            QuerySolution soln = results.nextSolution() ;
            Boolean m = APICall.isMeasurement(soln.getLiteral("x").toString());
            isMes = isMes || m;
        }
        return isMes;
    }

    public Vector<Attribute> getUnits(Vector<String> header, Vector<Attribute> attributes) {
        Vector<Attribute> attrsWithUnits = new Vector<Attribute>(attributes.size());
        try {
            Vector<Attribute> tmp = checkDependency(header,attributes);
            for(int i = 0; i < tmp.size();i++) {
                Attribute a = tmp.get(i);
                if(a.unitcol.equals(-1)) {
                    Boolean measurement = isResourceAMeasurement(a.resource);
                    if(i % 7 == 0) {
                        System.out.print("\n");
                    }
                    System.out.print("Col " + i + " processed, ");
                    if(measurement) { // Gets unit from user, adds to graph and attribute
                        System.out.println("\n" + header.get(i) + " has been identified as a measurement.");
                        System.out.print("Please enter the unit name: ");
                        Scanner s = new Scanner(System.in);
                        String unit = formatAttribute(s.nextLine().trim());
                        createTriple(a.resource, "http://umkc.edu/unit.rdf", unit);
                        a.metadata.put("Unit", unit);
                    }
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return attrsWithUnits;
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
            header.add("yard");

            AttributeHandler ah = new AttributeHandler("sample.nt");
            Vector<Attribute> aMeta = ah.getUnits(header, ah.findAttributes(header));
//            for(Attribute a : aMeta) {
//                System.out.println(a.unitcol);
//            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
