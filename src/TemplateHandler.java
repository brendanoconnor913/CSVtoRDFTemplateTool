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
    // Potentially move create template function here
    // Functions:
    //      Search Previous Templates given column headers
    //      Save new template to template graph
    //
    private Model model = ModelFactory.createDefaultModel();
    private String graphname;
    private List<Integer> subjectcols = new Vector<Integer>();

    TemplateHandler(String graph) {
        graphname = graph; // file path to template graph
        model.read(graphname);
    }

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

    private void createPredicate(Resource s, String predicate, Resource o){
        s.addProperty(ResourceFactory.createProperty(predicate), o);
        writeToModel();
    }

    public List<Integer> getSubjectColumns() {
        return subjectcols;
    }

    //TODO: implement saving the graph template then move to searching graph for existing templates
    // search for template and if found save to file and update template graph
    boolean templateFound(Vector<Attribute> header, String filepath) {

        return false;
    }

    private String getMeta(String baseEntity, Attribute currentEntity, CSVRecord header, Integer index, Resource metaAnon) {
        StringBuilder accstring = new StringBuilder();
        String anonNode = "_:metadata"+Integer.toString(index);
        createPredicate(metaAnon, "http://umkc.edu/valueAttribute", currentEntity.resource);
        accstring.append(baseEntity + " <" + currentEntity.resource + "> "+ anonNode +" .\n");
        accstring.append(anonNode + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> " +
                "\"${" + header.get(index).trim() + "}\"" + currentEntity.datatype + " .\n");
        for (Attribute meta : currentEntity.metadata) {
            if (meta.hasMetaData()) {
                Resource nextmeta = model.createResource();
                createPredicate(metaAnon, "http://umkc.edu/attributeWithMeta", nextmeta);
                accstring.append(getMeta(anonNode, meta, header, meta.index, nextmeta));
            }
            else { // add all entities w/o metadata to anon node
                createPredicate(metaAnon, "http://umkc.edu/metaAttribute", meta.resource);
                accstring.append(anonNode + " <" + meta.resource + "> \"${" +
                        header.get(meta.index).trim() + "}\""+ meta.datatype +" .\n");
            }

        }
        return accstring.toString();
    }

    // writes template for csv file to template file and saves template structure
    public void createTemplate(String filename, String graphname, String dirname) {
        try {
            // process column headers (map to resources and gather metadata)
            String filepath = dirname + "/" + filename;
            Vector<String> fmtheader = CSVConverter.getFormattedHeader(filepath);
            Vector<String> dataRow = CSVConverter.getFirstDataRow(filepath);
            AttributeHandler ah = new AttributeHandler(graphname);
            Vector<Attribute> aData = ah.addDataType(dataRow, ah.findAttributes(fmtheader, dataRow));
            Vector<Attribute> aMeta = ah.getUnits(fmtheader, aData, dataRow);

            // If previous template not found and used, then ask user for template structure
            if (!templateFound(aMeta, filepath)) {
                Resource tempAnonNode = model.createResource();
                createTriple(tempAnonNode, "http://umkc.edu/numObservations", "1");
                // get header row
                Reader in = new FileReader(filepath);
                Iterable<CSVRecord> recordForHeader = CSVFormat.EXCEL.parse(in);

                CSVRecord headerRecord = recordForHeader.iterator().next();
                Vector<String> header = new Vector<String>();
                CSVConverter.printTable(filepath); // print table for help with determining subject
                System.out.println("\nEach subject column and their respective indicies:");
                for (int i = 0; i < headerRecord.size(); i++) {
                    String s = headerRecord.get(i).trim();
                    header.add(s);
                    System.out.println(i + " : " + s);
                }
                System.out.println();

                // get subject column index
                // if multi indices then separate by a space\
                System.out.print("Enter subject column(s) in the order you wish to appear: ");
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                String[] subjectColIndexs = br.readLine().split(" ");
                StringBuilder subject = new StringBuilder();
                subject.append("<http://umkc.edu/subject/");
                for (String subCol : subjectColIndexs) {
                    int num = Integer.parseInt(subCol);
                    // make sure in range
                    if (num < 0 || num >= headerRecord.size()) {
                        throw new Exception("Outside of column index range");
                    }
                    subjectcols.add(num);
                    createPredicate(tempAnonNode, "http://umkc.edu/subject", aMeta.get(num).resource);
                    String ent = header.get(num).trim();
                    subject.append("${" + ent + "}" + "-");
                }
                subject.deleteCharAt(subject.length() - 1); // final subject string
                subject.append(">");

                StringBuilder allTrips = new StringBuilder();
                final String MADEOF = "<http://umkc.edu/composedOf>";
                if (subjectcols.size() > 1) {
                    allTrips.append(subject.toString() + " " + MADEOF + " _:comp . \n");
                    for (Integer n : subjectcols) {
                        allTrips.append("_:comp <" + aMeta.get(n).resource.toString() + "> \"${"
                                + header.get(n) + "}\" . \n");
                    }
                }

                final String subjectnode = "_:subject";
                final String GROUPEDREC = "<http://umkc.edu/groupedRecord>";
                allTrips.append(subject.toString() + " " + GROUPEDREC + " " + subjectnode + " . \n");

//            for (Integer n : subjects) {
//               allTrips.append("<" + aMeta.get(n).resource + "> <http://www.w3.org/2001/XMLSchema#literal> \"${" +
//                       header.get(n) + "}\" .\n");
//            }

                for (int i = 0; i < aMeta.size(); i++) {
                    Attribute a = aMeta.get(i);
                    if (subjectcols.contains(i) || (a.isMeta)) {
                        continue;
                    }
                    // add any anonymous node for metadata
                    else if (a.hasMetaData()) {
                        Resource metaAnon = model.createResource();
                        createPredicate(tempAnonNode, "http://umkc.edu/attributeWithMeta", metaAnon);
                        String metastruct = getMeta(subjectnode, a, headerRecord, i, metaAnon);
                        allTrips.append(metastruct);
//                    allTrips.append(subject.toString() + " <" + a.resource + "> _:metadata" + i +" .\n");
//                    allTrips.append("_:metadata" + i + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> " +
//                            "\"${" + headerRecord.get(i).trim() + "}\"" + a.datatype + " .\n");
//                    for (String k : a.metadata.keySet()) {
//                        allTrips.append("_:metadata" + i + " <" + k + "> \"" + a.metadata.get(k) +
//                                "\" .\n");
//                    }
                    } else {
                        createPredicate(tempAnonNode,"http://umkc.edu/attribute", a.resource);
                        allTrips.append(subjectnode + " <" + a.resource + "> \"${" +
                                headerRecord.get(i).trim() + "}\"" + a.datatype + " .\n");
                    }
                }

                String noSuffix = CSVConverter.stripFileExtension(filename);

                File tempdir = new File("output-templates");
                if (!tempdir.exists()) {
                    tempdir.mkdir();
                }

                File outfile = new File(tempdir, noSuffix + "-template.nt");
                PrintStream out = new PrintStream(new FileOutputStream(outfile));
                out.print(allTrips.toString());
                out.close();
                in.close();
            }
        }

        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
