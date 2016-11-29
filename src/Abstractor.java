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
    public static void main(String args[]) {
        //TODO: function to read in file and remove any whitespaces in header and save file
        // note: just cut logic from below and use it to overwrite the file then use file from there
        // ?: are we just going to have a "prefix" file that reads in prefix each time and creates a map for usage?
        // TODO: need to figure out how to handle prefixes and understand better how we will use w/ predicates
        // TODO: refactor getting head names into function
        // TODO: Not sure if I should ask about context/graph info to make quads here or worry about later (I think worry about later)
        // TODO: but may make sense to get that information up front and pass it to the quad creator script
        // TODO: Also need to think about handling values with empty/null and not creating a triple from them

        String filename = args[0];
        String subjectprefix = args[1];
        try {
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
