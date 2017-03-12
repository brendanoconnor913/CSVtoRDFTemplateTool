import dnl.utils.text.table.TextTable;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.Scanner;
import java.util.Vector;


/**
 * Created by brendan on 10/9/16.
 */

public class Abstractor {
    // function to return vector containing header for each column
    static String stripFileExtension(String filename) {
        char[] fstring = filename.toCharArray();
        StringBuilder ns = new StringBuilder();
        for (int i = 0; i < (fstring.length-4); i++) {
            ns.append(fstring[i]);
        }
        return ns.toString();
    }

    private static Vector<String> getFormattedHeader(String fname) {
        Vector<String> header = new Vector<String>();
        try {
            Reader in = new FileReader(fname);
            Iterable<CSVRecord> recordForHeader = CSVFormat.EXCEL.parse(in);
            CSVRecord headerRecord = recordForHeader.iterator().next();
            for(int i = 0; i < headerRecord.size(); i++) {
                String s = AttributeHandler.formatAttribute(headerRecord.get(i).trim());
                header.add(s);
            }
            in.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return header;
    }

    private static Vector<String> getHeader(String fname) {
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
    private static Vector<String> getFirstDataRow(String fname) {
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

    public void printTable(String filepath) {
        final int ROWS = 17;
        try {
            Reader in = new FileReader(filepath);
            Iterable<CSVRecord> rows = CSVFormat.EXCEL.parse(in);
            System.out.println("\nPreview of table:");
            CSVRecord row = rows.iterator().next();
            String[] cols = new String [row.size()];
            for(int i = 0;i < row.size();i++) {
                cols[i] = row.get(i).trim();
            }
            String[][] data = new String[ROWS][row.size()];
            for(int i = 0;i < ROWS;i++) {
                row = rows.iterator().next();
                for(int j = 0;j<row.size();j++){
                    data[i][j] = row.get(j).trim();
                }
            }
            TextTable tt = new TextTable(cols,data);
            tt.printTable();
            in.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createTemplate(String filename, String graphname, String dirname) {
        try {
            // process column headers (map to resources and gather metadata)
            String filepath = dirname + "/" + filename;
            Vector<String> fmtheader = getFormattedHeader(filepath);
            Vector<String> rawHeader = getHeader(filepath);
            Vector<String> dataRow = getFirstDataRow(filepath);
            AttributeHandler ah = new AttributeHandler(graphname);
            Vector<Attribute> aData = ah.addDataType(dataRow, ah.findAttributes(fmtheader, dataRow));
            Vector<Attribute> aMeta = ah.getUnits(rawHeader, fmtheader, aData, dataRow);


            // get header row
            Reader in = new FileReader(filepath);
            Iterable<CSVRecord> recordForHeader = CSVFormat.EXCEL.parse(in);
            CSVRecord headerRecord = recordForHeader.iterator().next();
            Vector<String> header = new Vector();
            printTable(filepath); // print table for help with determining subject
            System.out.println("\nEach subject column and their respective indicies:");
            for(int i = 0; i < headerRecord.size(); i++) {
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
            Vector<Integer> subjects = new Vector();
            StringBuilder subject = new StringBuilder();
            subject.append("<http://umkc.edu/subject/");
            for(String subCol : subjectColIndexs) {
                int num = Integer.parseInt(subCol);
                // make sure in range
                if(num < 0 || num >= headerRecord.size()) {
                    throw new Exception ("Outside of column index range");
                }
                subjects.add(num);
                subject.append("${" + header.get(num) + "}" + "-");
            }
            subject.deleteCharAt(subject.length()-1); // final subject string
            subject.append(">");

            StringBuilder allTrips = new StringBuilder();
            final String MADEOF = "<http://umkc.edu/composedOf>";
            if (subjects.size() > 1) {
                allTrips.append(subject.toString() + " " + MADEOF + " _:comp . \n");
                for (Integer n : subjects) {
                    allTrips.append("_:comp " + MADEOF + " <" + aMeta.get(n).resource.toString() + "> . \n");
                }
            }

            for (Integer n : subjects) {
               allTrips.append("<" + aMeta.get(n).resource + "> <http://www.w3.org/2001/XMLSchema#literal> \"${" +
                       header.get(n) + "}\" .\n");
            }

            for (int i = 0; i < aMeta.size(); i++) {
                Attribute a = aMeta.get(i);
                if (subjects.contains(i) || (a.isMeta)) { // modify if mult subj cols
                    continue;
                }
                // add any anonymous node for metadata
                else if (a.hasMetaData()) {
                    allTrips.append(subject.toString() + " <" + a.resource + "> _:metadata" + i +" .\n");
                    allTrips.append("_:metadata" + i + " <" + a.resource +
                            "> \"${" + headerRecord.get(i).trim() + "}\"" + a.datatype + " .\n");
                    for (String k : a.metadata.keySet()) {
                        allTrips.append("_:metadata" + i + " <" + k + "> " + a.metadata.get(k) +
                                " .\n");
                    }
                }
                else {
                    allTrips.append(subject.toString() + " <" + a.resource + "> \"${" +
                            headerRecord.get(i).trim() + "}\""+ a.datatype +" .\n");
                }
            }

            String noSuffix = stripFileExtension(filename);

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
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void tripToQuad(String triplesfile, String outputfile, String contexturl) {
        System.out.println("Adding context to triples ...");
        try {
            final String CONTEXT = " <"+contexturl+"> ";
            String line = "";
            BufferedReader br = new BufferedReader(new FileReader(triplesfile));

            PrintWriter pw = new PrintWriter(new FileOutputStream(new File(outputfile)));
            while((line = br.readLine()) != null){
                String[] trip = line.split(" ");
                if (trip.length != 4) {
                    throw new Exception ("Invalid number of \"words\" in line");
                }
                String quad = trip[0] + " " + trip[1] + " " + trip[2] + CONTEXT + trip[3];
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
        String dirname = args[0];
        String graphname = args[1];
        Abstractor aForAbstractor = new Abstractor();
        CSV2RDF cr = new CSV2RDF(true);
        File dir = new File(dirname);
        IdentityChecker ic = new IdentityChecker();
        File quads = new File("output-quads");
        if (!quads.exists()) {
            quads.mkdir();
        }
        if(dir.isDirectory()) {
            File[] files = dir.listFiles();
            for(File f : files) {
                String fileroot = stripFileExtension(f.getName());
                aForAbstractor.createTemplate(f.getName(),graphname, dir.getAbsolutePath());
                Scanner scan = new Scanner(System.in);
                System.out.print("Please give the url source for " + f.getName() + ": ");
                String url = scan.nextLine().trim();
                String outtrips = "output-triples/"+fileroot+"-triples.nt";
                String outquads = "output-quads/"+fileroot+"-quads.nq";
                cr.run("output-templates/"+fileroot+"-template.nt", f.toString(), outtrips);
                aForAbstractor.tripToQuad(outtrips,outquads,url);
                ic.addRelations(outquads, "output-quads/identity-quads.nq");
            }
        }
        else { // dirfile = single input file
            String fileroot = stripFileExtension(dirname);
            aForAbstractor.createTemplate(dirname, graphname, "");
            Scanner scan = new Scanner(System.in);
            System.out.print("Please give the url source for " + dirname + ":");
            String url = scan.nextLine().trim();
            String outtrips = "output-triples/"+fileroot+"-triples.nt";
            String outquads = "output-quads/"+fileroot+"-quads.nq";
            cr.run("output-templates/"+fileroot+"-template.nt", dirname, outtrips);
            aForAbstractor.tripToQuad(outtrips,outquads,url);
            ic.addRelations(outquads, "output-quads/identity-quads.nq");
        }

        // Concat all quad files into one
        // probably not feasible to do locally
//        File finalout = new File("output-final/allquads.nq");
//        File[] quadfiles = quads.listFiles();
//        try {
//            for (File q : quadfiles) {
//                String qstring = FileUtils.readFileToString(q);
//                FileUtils.write(finalout, qstring, true);
//            }
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}
