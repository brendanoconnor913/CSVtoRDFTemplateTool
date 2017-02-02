import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.Vector;

/**
 * Created by brendan on 10/9/16.
 */
public class Abstractor {
    // function to return vector containing header for each column
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
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return dRow;
    }

    private void createTemplate(String filename, String graphname) {
        try {
            // process column headers (map to resources and gather metadata)
            Vector<String> fmtheader = getFormattedHeader(filename);
            Vector<String> rawHeader = getHeader(filename);
            Vector<String> dataRow = getFirstDataRow(filename);
            AttributeHandler ah = new AttributeHandler(graphname);
            Vector<Attribute> aMeta = ah.getUnits(rawHeader, fmtheader, ah.findAttributes(fmtheader));
            Vector<Attribute> aData = ah.addDataType(dataRow, aMeta);

            // get header row
            Reader in = new FileReader(filename);
            Iterable<CSVRecord> recordForHeader = CSVFormat.EXCEL.parse(in);
            CSVRecord headerRecord = recordForHeader.iterator().next();
            Vector<String> header = new Vector();
            for(int i = 0; i < headerRecord.size(); i++) {
                String s = headerRecord.get(i).trim();
                header.add(s);
                System.out.println(i + " : " + s);
            }

            // get subject column index
            // if multi indices then separate by a space
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
            for (Integer n : subjects) {
                allTrips.append(subject.toString() + " <http://rdf/label> <" + aData.get(n).resource + "> .\n");
            }

            for (int i = 0; i < aData.size(); i++) {
                Attribute a = aData.get(i);
                if (subjects.contains(i) || (a.isMeta)) { // modify if mult subj cols
                    continue;
                }
                // add any anonymous node for metadata
                else if (a.hasMetaData()) {
                    allTrips.append(subject.toString() + " <" + a.resource + "> _:metadata" + i +" .\n");
                    allTrips.append("_:metadata" + i + " <" + a.resource +
                            "> \"${" + headerRecord.get(i).trim() + "}\"" + a.datatype + " .\n");
                    for (String k : a.metadata.keySet()) {
                        allTrips.append("_:metadata" + i + " <" + k + "> \"" + a.metadata.get(k) +
                                "\"" + a.datatype + " .\n");
                    }
                }
                else {
                    allTrips.append(subject.toString() + " <" + a.resource + "> \"${" +
                            headerRecord.get(i).trim() + "}\""+ a.datatype +" .\n");
                }
            }

            // Outputs created template to file
            char[] fstring = filename.toCharArray();
            StringBuilder ns = new StringBuilder();
            for (int i = 0; i < (fstring.length-4); i++) {
                ns.append(fstring[i]);
            }
            String noSuffix = ns.toString();

            File tempdir = new File("templates");
            tempdir.mkdir();
            PrintStream out = new PrintStream(new FileOutputStream(new File(tempdir, noSuffix + "-template.nt")));
            out.print(allTrips.toString());
            out.close();
            in.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        String dirname = args[0];
        String graphname = args[1];
        String context = args[2]; // for quad creation
        Abstractor aForAbstractor = new Abstractor();
        File dir = new File(dirname);
        if(dir.isDirectory()) {
            File[] files = dir.listFiles();
            for(File f : files) {
                aForAbstractor.createTemplate(f.getName(),graphname);
            }
        }
        else {
            aForAbstractor.createTemplate(dirname, graphname);
        }
    }
}
