import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.Vector;

/**
 * Created by brendan on 10/9/16.
 */
public class Abstractor {
    // function to return vector containing header for each column
    private static Vector<String> getHeader(String fname) {
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

    public static void main(String args[]) {
        // TODO: after flow is working go back through and work on quality of triples created
        String filename = args[0];
        String graphname = args[1];
        String context = args[2]; // for quad creation
        try {
            // process column headers (map to resources and gather metadata)
            Vector<String> tmpheader = getHeader(filename);
            AttributeHandler ah = new AttributeHandler(graphname);
            Vector<Attribute> aMeta = ah.getUnits(tmpheader, ah.findAttributes(tmpheader));

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
            System.out.print("Enter subject column: ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String[] subjectColIndexs = br.readLine().split(" ");
            Vector<Integer> subjects = new Vector();
            StringBuilder subject = new StringBuilder();
            int num = -1; // for subj col
            String subj = ""; // for subject string
            for(String subCol : subjectColIndexs) {
                num = Integer.parseInt(subCol);
                // make sure in range
                if(num < 0 || num >= headerRecord.size()) {
                    throw new Exception ("Outside of column index range");
                }
                subjects.add(num);

                // Make sure no spaces in column name
                String subLit = header.get(num);
                String[] holder = subLit.split(" ");
                if(holder.length > 1) {
                    StringBuilder sb = new StringBuilder();
                    for(String s : holder) {
                        sb.append(s);
                    }
                    subLit = sb.toString();
                }
                // Create subject and first triple
                subj = "<http://umkc.edu/subject#${" + subLit + "}>";
                subject.append(subj + " <http://rdf/label> <" + aMeta.get(num).resource + "> .\n");
            }

            for (int i = 0; i < aMeta.size(); i++) {
                Attribute a = aMeta.get(i);
                if (i == num || (a.unitcol.equals(a.ISUNIT))) { // modify if mult subj cols
                    continue;
                }
                else if (!a.unitcol.equals(a.NOUNIT)) {
                    subject.append(subj + " <" + aMeta.get(i).resource + "> _:metadata .\n");
                    subject.append("_:metadata" + " <" + aMeta.get(i).resource +
                            "> \"${" + headerRecord.get(i).trim() + "}\" .\n");
                    subject.append("_:metadata" + " <" + aMeta.get(a.unitcol).resource +
                            "> \"${" + headerRecord.get(a.unitcol).trim() + "}\" .\n");
                }
                else {
                    subject.append(subj + " <" + aMeta.get(i).resource + "> \"${" +
                            headerRecord.get(i).trim() + "}\" .\n");
                }
            }

            // Outputs created template to file
            PrintStream out = new PrintStream(new FileOutputStream("triptemp.nt"));
            System.setOut(out);
            System.out.print(subject.toString());
            out.close();
            in.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
