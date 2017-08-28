import dnl.utils.text.table.TextTable;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import java.io.*;
import java.util.List;
import java.util.Vector;

/**
 * Created by brendan on 8/13/17.
 */

public class TemplateHandler {
    private String templateGraph;

    TemplateHandler(String graph) {
        templateGraph = graph; // file path to template graph
    }

    //TODO: implement saving the graph template then move to searching graph for existing templates
    // search for template and if found save to file and update template graph
    boolean templateFound(Vector<Entity> header, String filepath) {
        return false;
    }

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
                        dEntities.get(col).addMeta(dEntities.get(meta));
                        dEntities.get(meta).setMetaEntity();
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
            if (!templateFound(aData, filepath)) {
                Vector<Entity> aMeta = getMetaRelationships(fmtheader, aData, dataRow);
                Vector<Entity> subject = new Vector<Entity>();
                Vector<Entity> attributes = new Vector<Entity>();
                subjectIndicies = getSubjectIndicies(filepath);
                for(Integer i = 0; i < aMeta.size()-1; i++) {
                    if (subjectIndicies.contains(i)) {
                        subject.add(aMeta.get(i));
                    }
                    else {
                        attributes.add(aMeta.get(i));
                    }
                }

                RDFTemplate template = new RDFTemplate(subject, attributes, templateGraph);
                template.writeToTemplateFile(filename);
            }
        }

        catch(Exception exc) {
            exc.printStackTrace();
        }
        return subjectIndicies;
    }
}
