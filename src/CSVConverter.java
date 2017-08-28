import dnl.utils.text.table.TextTable;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import java.io.*;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;


/**
 * Created by brendan on 10/9/16.
 */

//TODO: Modify to seperately ask for units
//TODO: Modify to ask for subject first?

public class CSVConverter {
    // function to return vector containing header for each column
    static String stripFileExtension(String filename) {
        char[] fstring = filename.toCharArray();
        StringBuilder ns = new StringBuilder();
        for (int i = 0; i < (fstring.length-4); i++) {
            ns.append(fstring[i]);
        }
        return ns.toString();
    }

    static Vector<String> getFormattedHeader(String fname) {
        Vector<String> header = new Vector<String>();
        try {
            Reader in = new FileReader(fname);
            Iterable<CSVRecord> recordForHeader = CSVFormat.EXCEL.parse(in);
            CSVRecord headerRecord = recordForHeader.iterator().next();
            for(int i = 0; i < headerRecord.size(); i++) {
                String s = EntityHandler.formatEntity(headerRecord.get(i).trim());
                header.add(s);
            }
            in.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return header;
    }

    static Vector<String> getHeader(String fname) {
        Vector<String> header = new Vector<String>();
        try {
            Reader in = new FileReader(fname);
            Iterable<CSVRecord> recordForHeader = CSVFormat.EXCEL.parse(in);
            CSVRecord headerRecord = recordForHeader.iterator().next();
            for(int i = 0; i < headerRecord.size(); i++) {
                String s = headerRecord.get(i).trim();
                header.add(s);
            }
            in.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return header;
    }

    // gets first row from data file being used
    static Vector<String> getFirstDataRow(String fname) {
        Vector<String> dRow = new Vector<String>();
        try {
            Reader in = new FileReader(fname);
            Iterable<CSVRecord> recordForHeader = CSVFormat.EXCEL.parse(in);
            CSVRecord headerRecord = recordForHeader.iterator().next();
            CSVRecord dataRecord = recordForHeader.iterator().next();
            for(int i = 0; i < dataRecord.size(); i++) {
                String s = dataRecord.get(i).trim();
                dRow.add(s);
            }
            in.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return dRow;
    }

    public void tripToQuad(String triplesfile, String outputfile, String contexturl) {
        System.out.println("Adding context to triples ...");
        try {
            final String CONTEXT = " <"+contexturl+"> ";
            String line = "";
            BufferedReader br = new BufferedReader(new FileReader(triplesfile));

            PrintWriter pw = new PrintWriter(new FileOutputStream(new File(outputfile)));
            while((line = br.readLine()) != null){
                String modline = line.substring(0,line.length()-2);
                String quad = new StringBuilder(modline).append(CONTEXT+".").toString();
                pw.println(quad);
            }
            br.close();
            pw.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        String fname = args[0];
        String ontologygraph = args[1];
        String templategraph = args[2];
        CSVConverter converter = new CSVConverter();
        Template2RDF templateToRDF = new Template2RDF(true);
        File inputtedFile = new File(fname);
//        IdentityChecker ic = new IdentityChecker();
        TemplateHandler handler = new TemplateHandler(templategraph);

        File quads = new File("output-quads");
        if (!quads.exists()) {
            quads.mkdir();
        }

        if(inputtedFile.isDirectory()) {
            File[] files = inputtedFile.listFiles();
            for(File f : files) {
//                String filepath = inputtedFile.getAbsolutePath()+"/"+f.getName();
                String fileroot = stripFileExtension(f.getName());
                List<Integer> subcols = handler.createTemplateFile(inputtedFile.getAbsolutePath(), f.getName(), ontologygraph);
                Scanner scan = new Scanner(System.in);
                System.out.print("Please give the url source for " + f.getName() + ": ");
                String url = scan.nextLine().trim();
                String outtrips = "output-triples/"+fileroot+"-triples.nt";
                templateToRDF.run("output-templates/"+fileroot+"-template.nt", f.toString(), outtrips, subcols);
                String outquads = "output-quads/"+fileroot+"-quads.nq";
                converter.tripToQuad(outtrips,outquads,url);
                // ic.addRelations(outquads, "output-quads/identity-quads.nq");
            }
        }
        else { // inputtedFile = single input file
            String fileroot = stripFileExtension(fname);
            handler.createTemplateFile("", fname, ontologygraph);
            List<Integer> subcols = handler.getSubjectIndicies(fname);
            Scanner scan = new Scanner(System.in);
            System.out.print("Please give the url source for " + fname + ":");
            String url = scan.nextLine().trim();
            String outtrips = "output-triples/"+fileroot+"-triples.nt";
            templateToRDF.run("output-templates/"+fileroot+"-template.nt", fname, outtrips, subcols);
            String outquads = "output-quads/"+fileroot+"-quads.nq";
            converter.tripToQuad(outtrips,outquads,url);
            // ic.addRelations(outquads, "output-quads/identity-quads.nq");
        }
    }
}
