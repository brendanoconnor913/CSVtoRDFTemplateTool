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
public class EntityHandler {
    private Model model = ModelFactory.createDefaultModel();
    private String graphname;

    EntityHandler(String graph) {
        graphname = graph; // file path to ontology graph
        model.read(graphname);
    }

    // takes in column header and puts in uniform format
    public static String formatEntity(String attr) {
        char[] symbols = {' ','-',',','<','>','%','(',')','/','\\'};// symbols to be removed from entities
        StringBuilder sb = new StringBuilder();
        // remove symbols, spaces -> _ , and convert to all lower case
        for(char c : attr.trim().toCharArray()) {
            if(ArrayUtils.contains(symbols, c)) {
                if (sb.length() > 0) {
                    char prev = sb.charAt(sb.length()-1);
                    if(prev != '_') {
                        sb.append('_');
                    }
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
            PrintWriter pw = new PrintWriter(graphname);
            model.write(pw, "NT");
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
        createTriple(r,"http://www.w3.org/2000/01/rdf-schema#label",alias);
    }

    // Create new entity and add to graph
    private Resource createEntity(String name){
        // Capitalize the first letter then create as entity
        String pname = name.replaceAll("\uFEFF","").trim();
        String rname = "http://umkc.edu/resource/"+pname;
        Resource r = model.createResource(rname);
        r.addProperty(ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#label"),
                ResourceFactory.createPlainLiteral(name));
        writeToModel();
        return r;
    }

    private String getEntityInput(String unidentified, String sample) {
        System.out.println("\nThe system was unable to identify \"" + unidentified +
                "\" (Sample: \"" + sample + "\")\n\t(Press ENTER if you wish to keep given entity name)");
        String userin;
        System.out.print(unidentified + " : ");
        Scanner s = new Scanner(System.in);
        String line = s.nextLine().trim();
        if(line.equals("")) { // if enter inputted use old entity
            userin = unidentified;
        }
        else {
            userin = formatEntity(line);
        }
        return userin;
    }

    // Searches metagraph to try and find entity if not found asks for user input for other possible alias
    // if found attaches new and old alias to resource if not creates new alias
    public Vector<Entity> findEntities(Vector<String> header, Vector<String> firstrow) {
        Vector<Entity> entities = new Vector<Entity>(header.size());
        try {
            for(int i = 0; i < header.size(); i++) {
                String current = header.get(i);
                String query = "SELECT ?x WHERE { ?x <http://www.w3.org/2000/01/rdf-schema#label> \"" + current + "\".}";
                QueryExecution qexec = QueryExecutionFactory.create(query, model);
                ResultSet results = qexec.execSelect();
                if (!results.hasNext()) { // entity not identified
                    String in = getEntityInput(current, firstrow.get(i)); // get new entity name and search again
                    String secondsearch = "SELECT ?x WHERE { ?x <http://www.w3.org/2000/01/rdf-schema#label> \"" + in + "\".}";
                    QueryExecution qex = QueryExecutionFactory.create(secondsearch, model);
                    ResultSet sndresult = qex.execSelect();
                    if (!sndresult.hasNext()) { // entity not identified
                        Resource nr = createEntity(in);
                        createAlias(nr,current);
                        entities.add(new Entity(nr,i));
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
                            entities.add(new Entity(r,i));
                            rescnt++;
                        }
                    }
                }
                else { // alias found on first try
                    int rescnt = 0;
                    for( ; results.hasNext(); ) {
                        if(rescnt > 0) { // multiple results returned for alias name
                            throw new Exception("Ambigious label (alias found under multiple resources" +
                                    " please remove duplicate before continuing.");
                        }
                        QuerySolution soln = results.nextSolution() ;
                        Resource r = soln.getResource("x");
                        entities.add(new Entity(r,i));
                        System.out.println(r);
                        rescnt++;
                    }
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return entities;
    }

    // Get input to identify if column unit dependant on another column
    private Vector<Entity> checkDependency(Vector<String> header,
                                              Vector<Entity> entities,
                                              Vector<String> firstrow) throws Exception {
        if(header.size() != entities.size()){
            throw new Exception("Header size needs to match entity size... entity may have been" +
                    " lost in process");
        }
        // Store updates in dEntities to be returned at the end
        Vector<Entity> dEntities = (Vector<Entity>)entities.clone();

        // Output column headers
        for(int i = 0; i < header.size(); i++) {
            String s = header.get(i).trim();
            System.out.println(i + " : " + s + " (Sample: \"" + firstrow.get(i) + "\")");
        }
        // Get input for meta columns
        System.out.println("\nPlease indicate if any column specifies metadata for another column");
        System.out.println("If there aren't any metadata columns simply press enter");
        System.out.println("The format to do so is META_COL_NUM,VAL_COL:" +
                "META_COL2,VAL_COL2:META_COL3,VAL_COL3\n");
        System.out.print("Enter dependencies as instructed above: ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String line = br.readLine().trim();
        if(!line.equals("")) {
            String[] dependencies = line.split(":");
            if(!dependencies[0].equals("")){
                for(int i = 0; i < dependencies.length; i++){
                    String [] pair = dependencies[i].split(",");
                    if(!pair[0].equals("")) {
                        if(pair.length != 2 || (pair[0].equals(pair[1]))) {
                            System.out.println(pair.length);
                            System.out.println("\""+pair[0]+"\"");
                            throw new Exception("Columns did not adhere to format given.");
                        }
                        Integer meta = Integer.parseInt(pair[0].trim());
                        Integer col = Integer.parseInt(pair[1].trim());
                        dEntities.get(col).addMeta(dEntities.get(meta));
                        dEntities.get(meta).setMetaEntity();
                    }
                }
            }
        }

        return dEntities;
    }

    // Calls API to attempt to indetify if entity is a quantity
//    private Boolean isResourceAMeasurement(Resource r) {
//        Boolean isMes = false;
//        String query = "SELECT ?x WHERE { <"+r.toString()+"> <http://www.w3.org/2000/01/rdf-schema#label> ?x .}";
//        QueryExecution qexec = QueryExecutionFactory.create(query, model);
//        ResultSet results = qexec.execSelect();
//        for( ; results.hasNext(); ) {
//            QuerySolution soln = results.nextSolution() ;
//            Boolean m = APICall.isMeasurement(soln.getLiteral("x").toString());
//            isMes = isMes || m;
//        }
//        return isMes;
//    }

    // function to get name of unit if measurement indentified for entity
    public Vector<Entity> getUnits(Vector<String> header,
                                      Vector<Entity> entities,
                                      Vector<String> firstrow) {
//        final String UNIT = "http://umkc.edu/unit";
        Vector<Entity> attrsWithUnits = new Vector<Entity>(entities.size());
        try {
            Vector<Entity> tmp = checkDependency(header, entities, firstrow);
            for(int i = 0; i < tmp.size(); i++) {
                Entity e = tmp.get(i);
//                if(!a.isMeta && !a.hasMetaData()) {
                String query = "SELECT ?x WHERE { "+"<"+e.toString()+">"+" <http://umkc.edu/unit> ?x .}";
                QueryExecution qexec = QueryExecutionFactory.create(query, model);
                ResultSet results = qexec.execSelect();
//                    if (!results.hasNext()) {
////                        Boolean measurement = isResourceAMeasurement(a.resource);
//                        if(measurement) { // Gets unit from user, adds to graph and entity
//                            System.out.println("\n" + header.get(i) + " has been identified as a measurement.");
//                            System.out.print("Please enter the unit name (input NA if not a measurement): ");
//                            Scanner s = new Scanner(System.in);
//                            if (!s.nextLine().equals("NA")) {
//                                String unit = formatEntity(s.nextLine().trim());
//                                createTriple(a.resource, UNIT, unit);
//                                a.metadata.put(UNIT, unit);
//                            }
//                        }
//                    }
//                    else {
                // TODO: Expand this to let user choose which literal if multi results returned
                if (results.hasNext()) {
                    QuerySolution solution = results.nextSolution();
                    Literal unitlit = solution.getLiteral("x");
                    String unitstr = unitlit.toString();
                    e.unit = unitstr;
                }
//                    }
                System.out.print("Col " + i + " processed\n");
                attrsWithUnits.add(e);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return attrsWithUnits;
    }
    // helper function to determine if string contains boolean values
    public boolean isBool(String s) {
        Vector<String> bools = new Vector<String>();
        bools.add("true");
        bools.add("false");
        return (bools.contains(s.trim().toLowerCase()));
    }
    // helper function to determine if string contains string values
    public boolean isString(String s) {
        char[] alphabet = {' ','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w',
                'x','y','z'};
        char[] word = s.trim().toLowerCase().toCharArray();
        for (char c : word) {
            if (!ArrayUtils.contains(alphabet,c)) {
                return false;
            }
        }
        return true;
    }

    // helper function to determine if string contains numerical values
    public boolean isNumber(String s) {
        char[] nums = {'1','2','3','4','5','6','7','8','9','0','-',',','.','+'};
        char[] word = s.trim().toLowerCase().toCharArray();
        for (char c : word) {
            if (!ArrayUtils.contains(nums,c)) {
                return false;
            }
        }
        return true;
    }

    // helper function to determine if string contains decimal values
    public boolean isDecimal(String s) {
        return s.contains(".");
    }

    // Takes in vector of formed entities, scans to figure out data type and attaches to given entity
    public Vector<Entity> addDataType(Vector<String> dataRow, Vector<Entity> pEntities) throws Exception {
        Vector<Entity> dEntities = (Vector<Entity>)pEntities.clone();
        for(int i = 0; i < dataRow.size();i++) {
            if (dataRow.size() != dEntities.size()) {
                throw new Exception("Vector sizes don't match in data and entities");
            }
            String s = dataRow.get(i);
            Entity a = dEntities.get(i);

            if (isString(s)) {
                if (isBool(s)) {
                    a.datatype = "^^<http://www.w3.org/2001/XMLSchema#boolean>";
                }
                else {
                    a.datatype = "^^<http://www.w3.org/2001/XMLSchema#string>";
                }
            }
            else if (isNumber(s)) {
                if(isDecimal(s)) {
                    a.datatype = "^^<http://www.w3.org/2001/XMLSchema#decimal>";
                }
                else {
                    a.datatype = "^^<http://www.w3.org/2001/XMLSchema#integer>";
                }
            }
        }
        return dEntities;
    }
}
