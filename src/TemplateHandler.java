import dnl.utils.text.table.TextTable;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import static org.apache.jena.enhanced.BuiltinPersonalities.model;
import static org.apache.jena.sparql.vocabulary.VocabTestQuery.query;

/**
 * Created by brendan on 8/13/17.
 */

public class TemplateHandler {
    private String templateGraph;

    TemplateHandler(String graph) {
        templateGraph = graph; // file path to template graph
    }

    // search for template and if found save to file and update template graph
    // current default behavior to use any complete subject found, may want to modify down the line
    boolean templateFound(Vector<Entity> header, String templatefilepath) {
        // create a map from each template node to the entities in header that appear in its subject
        HashMap<Resource, Vector<Entity>> tempToEntities = new HashMap<Resource, Vector<Entity>>();
        Model templatemodel = ModelFactory.createDefaultModel();
        templatemodel.read(templateGraph);

        for (Entity e : header) {
            String query = "SELECT ?s WHERE { ?s <http://umkc.edu/subject> <"+e.toString()+"> . }";
            QueryExecution qexec = QueryExecutionFactory.create(query, templatemodel);
            ResultSet result = qexec.execSelect();
            while (result.hasNext()) {
                QuerySolution soln = result.nextSolution();
                Resource s = soln.getResource("s");
                if (tempToEntities.containsKey(s)) {
                    tempToEntities.get(s).add(e);
                }
                else {
                    Vector<Entity> v = new Vector<Entity>();
                    v.add(e);
                    tempToEntities.put(s, v);
                }
            }
        }

        // filter out all nodes in which the entire subject isn't contained in the header
        //      (subject size != map val size)
        Vector<RDFTemplate> templates = new Vector<RDFTemplate>();
        for (Resource node : tempToEntities.keySet()) {
            StmtIterator itr = node.listProperties(templatemodel.getProperty("http://umkc.edu/subject"));
            Integer subjectSize = itr.toList().size();
            if (tempToEntities.get(node).size() == subjectSize) {
                templates.add(new RDFTemplate(node, templateGraph));
            }
        }

        if (templates.isEmpty()) {return false;}
        // attempt to find exact match of entities in header and template
        for (RDFTemplate t : templates) {
            // if found use template
            if (t.sameEntities(header)) {
                t.writeToTemplateFile(templatefilepath);
                t.writeToTemplateGraph();
                return true;
            }
        }

        // get biggest subject(s)
        Vector<RDFTemplate> maxSubject = new Vector<RDFTemplate>();
        for (RDFTemplate t : templates) {
            if (maxSubject.isEmpty()) {
                maxSubject.add(t);
            }
            else if (t.getSubjectSize().equals(maxSubject.get(0).getSubjectSize())) {
                maxSubject.add(t);
            }
            else if (t.getSubjectSize() > maxSubject.get(0).getSubjectSize()) {
                maxSubject = new Vector<RDFTemplate>();
                maxSubject.add(t);
            }
        }
        // for now just take one of the max subjects, ideally take one with  most overlapping attributes
        if (!maxSubject.isEmpty()) {
            Vector<Entity> maxsubj = maxSubject.get(0).getTemplateSubject();
            Vector<Entity> attributes = new Vector<Entity>();
            for (Entity e : header) {
                if (!maxsubj.contains(e)) {
                    attributes.add(e);
                }
            }
            RDFTemplate template = new RDFTemplate(maxsubj, attributes, templateGraph);
            template.writeToTemplateFile(templatefilepath);
            template.writeToTemplateGraph();
            return true;
        }
        return false;
    }

//    Vector<Entity> predictSubject() {};
//    Vector<Entity> predictMetaRelationships(Vector<Entity> attributes) {};

//    RDFTemplate predictTemplate(Vector<Entity> header) {
//        return pTemplate;
//    }

    private void printTable(String filepath) {
        final int ROWS = 17;
        try {
            Reader in = new FileReader(filepath);
            Iterable<CSVRecord> rows = CSVFormat.EXCEL.parse(in);
            System.out.println("\nPreview of table:");
            CSVRecord row = rows.iterator().next();
            String[] cols = new String[row.size()];
            for (int i = 0; i < row.size(); i++) {
                cols[i] = row.get(i).trim();
            }
            String[][] data = new String[ROWS][row.size()];
            for (int i = 0; i < ROWS; i++) {
                row = rows.iterator().next();
                for (int j = 0; j < row.size(); j++) {
                    data[i][j] = row.get(j).trim();
                }
            }
            TextTable tt = new TextTable(cols, data);
            tt.printTable();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Get input to identify if column provides meta data on another column
    private Vector<Entity> getMetaRelationships(Vector<String> header,
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
                        Entity e = dEntities.get(meta);
                        e.setMetaEntity();
                        dEntities.get(col).addMeta(e);
                    }
                }
            }
        }

        return dEntities;
    }

    Vector<Integer> getSubjectIndicies(String filepath) {
        Vector<Integer> subjectcols = new Vector<Integer>();
        try {
            Reader in = new FileReader(filepath);
            Iterable<CSVRecord> recordForHeader = CSVFormat.EXCEL.parse(in);

            CSVRecord headerRecord = recordForHeader.iterator().next();
            printTable(filepath); // print table for help with determining subject
            System.out.println("\nEach subject column and their respective indicies:");
            for (int i = 0; i < headerRecord.size(); i++) {
                String s = headerRecord.get(i).trim();
                System.out.println(i + " : " + s);
            }
            System.out.println();

            // get subject column index
            // if multi indices then separate by a space\
            System.out.print("Enter subject column(s) in the order you wish to appear: ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String[] subjectColIndexs = br.readLine().split(" ");
            for (String subCol : subjectColIndexs) {
                int num = Integer.parseInt(subCol);
                // make sure in range
                if (num < 0 || num >= headerRecord.size()) {
                    throw new Exception("Outside of column index range");
                }
                subjectcols.add(num);
            }
        }
        catch (Exception e) {e.printStackTrace();}
        return subjectcols;
    }

    // TODO: Fill in function to write
    public void writeTemplatePredictionResults(RDFTemplate prediction, RDFTemplate usedtemplate) {
        if (prediction.equals(usedtemplate)) {
            // write correct prediction
        }
        else {
            // write each template to file
            // attach with node and prediction/used relationships
        }
    }


    // writes template for csv file to template file and saves template structure
    public Vector<Integer> createTemplateFile(String dirname, String filename, String ontologyGraph) {
        Vector<Integer> subjectIndicies = new Vector<Integer>();
        try {
            // process column headers (map to resources and gather metadata)
            String filepath = dirname + "/" + filename;
            Vector<String> fmtheader = CSVConverter.getFormattedHeader(filepath);
            Vector<String> rawheader = CSVConverter.getHeader(filepath);
            Vector<String> dataRow = CSVConverter.getFirstDataRow(filepath);
            EntityHandler eh = new EntityHandler(ontologyGraph);
            Vector<Entity> aData = eh.addDataType(dataRow, eh.findEntities(fmtheader, dataRow, rawheader));

            // If previous template not found and used, then ask user for template structure
            if (!templateFound(aData, filename)) {
//                RDFTemplate predictedTemp = predictTemplate(aData);
//                Vector<Entity> aMeta = getMetaRelationships(fmtheader, aData, dataRow);
                Vector<Entity> subject = new Vector<Entity>();
                Vector<Entity> attributes = new Vector<Entity>();
                subjectIndicies = getSubjectIndicies(filepath);
                for(Integer i = 0; i < aData.size(); i++) {
                    if (subjectIndicies.contains(i)) {
                        subject.add(aData.get(i));
                    }
                    else {
                        attributes.add(aData.get(i));
                    }
                }

                RDFTemplate template = new RDFTemplate(subject, attributes, templateGraph);
                template.writeToTemplateFile(filename);
                template.writeToTemplateGraph();
            }
        }

        catch(Exception exc) {
            exc.printStackTrace();
        }
        return subjectIndicies;
    }
}
