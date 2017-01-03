import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Vector;

/**
 * Created by brendan on 10/9/16.
 */
public class Abstractor {
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
        // TODO: need to figure out how to handle prefixes and understand better how we will use w/ predicates
        // TODO: Need to get input on context name
        // TODO: Also need to think about handling values with empty/null and not creating a triple from them
            // would be handled in quad creation file
        // TODO: after flow is working go back through and work on quality of triples created

        String filename = args[0];
        String graphname = args[1];
        String context = args[2];
        try {
//            Vector<String> header = getHeader(filename);
//            AttributeHandler ah = new AttributeHandler(graphname);
//            Vector<Attribute> aMeta = ah.getUnits(header, ah.findAttributes(header));

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
            System.out.print("Enter subject column(s) (in the order you wish for them to appear): ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String[] subjectColIndexs = br.readLine().split(" ");
            Vector<Integer> subjects = new Vector();
            StringBuilder subject = new StringBuilder();
            for(String subCol : subjectColIndexs) {
                int num = Integer.parseInt(subCol);
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
                subject.append("${" + subLit + "}" + "-");
            }
            subject.deleteCharAt(subject.length()-1); // final subject string
            System.out.println("Resulting Subject: " + subject.toString());

            in.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
