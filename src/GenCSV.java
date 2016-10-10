import  java.io.*;
import java.util.Vector;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphSimpleMem;
import org.apache.jena.sparql.core.*;
import org.apache.jena.sparql.core.Quad;

import static java.lang.System.in;

// had to create two files due to how the CSV parser works
// it needs the header to identify the columns but I needed it too
// would be nice to find an approach that doesn't require opening the file twice

// Also I have the sense that we will have to tweak the naming for context and
// figure out how to configure the connections but I think this works well for now

public class GenCSV {
    public static void main(String args[]) {
        try {

            DatasetGraph dsg = new DatasetGraphSimpleMem();
            // use for data to go in quads
            Reader in = new FileReader("data.csv");
            // use to get header names
            Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);
            Reader tmp = new FileReader("data.csv");
            Iterable<CSVRecord> recordForHeader = CSVFormat.EXCEL.parse(tmp);
            Vector<String> predicates = new Vector<String>(); // container for column titles
            final String CONTEXT = "http://www.ers.usda.gov/data-products/beef-prod-database#";
            final String UMKC = "http://umkc.edu/";



            CSVRecord headerRecord = recordForHeader.iterator().next(); // get header line
            // load column titles into container
            for(int i = 0; i < headerRecord.size(); i++) {
                String s = headerRecord.get(i).trim();
                System.out.println(i + " : " + s);
                predicates.add(s);
            }

            // get subject column index
            System.out.print("Enter subject column: ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String subjectCol = br.readLine();
            Integer subjectColIndex = Integer.parseInt(subjectCol);

            // create and add all quads to graph
            for(CSVRecord record : records) {
                // store literal values for predicates
                // need to put into a container first because subject
                // will need to be used for the context
                Vector<String> recordLiterals = new Vector<String>();
                for (String predicate : predicates) {
                    String p = record.get(predicate).trim();
                    recordLiterals.add(p);
                }

                // create quads and add to graph
                String subject = recordLiterals.get(subjectColIndex);
                for (int i = 0; i < predicates.size(); i++) {
                    Quad q = new Quad(NodeFactory.createURI(CONTEXT+subject),
                            NodeFactory.createURI(UMKC+"resource.rdf#"+subject),
                            NodeFactory.createURI(UMKC+predicates.get(i)+".rdf#"),
                            NodeFactory.createLiteral(recordLiterals.get(i)));

                    dsg.add(q);
                }
            }
            // output quads to file
            PrintStream out = new PrintStream(new FileOutputStream("quads.txt"));
            System.setOut(out);
            RDFDataMgr.write(System.out, dsg, RDFFormat.NQUADS);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
}
