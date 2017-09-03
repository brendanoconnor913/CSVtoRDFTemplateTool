import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import java.lang.String;

import java.io.*;
import java.net.URLEncoder;
import java.util.Scanner;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ReadData
{
    public static void main(String[] args)
    {
        final File data = new File("input.txt");
        final File quads = new File("output.txt");
        String ResultString;
        String ResultStringg;


        try (final FileInputStream fi = new FileInputStream(data);
             final Scanner scanner = new Scanner(fi);
             final FileWriter fw = new FileWriter(quads);
             final BufferedWriter writer = new BufferedWriter(fw)) {


            while (scanner.hasNextLine()) {


                final String line1 = scanner.nextLine();
                final String[] triple = line1.split(" ");


                if (!scanner.hasNextLine()) {
                    // print uneven lines
                    writer.write(triple[0]);
                    writer.write(" ");
                    writer.write(triple[1]);
                    writer.write(" ");
                    writer.write(triple[2]);
                    writer.write(" ");
                    // writer.write(String.valueOf(similarity));
                    writer.write(" .");
                    writer.newLine();
                    break;
                }

                final String line2 = scanner.nextLine();

                final String[] triplee = line2.split(" ");


                final String subj = triple[0].substring(1, triple[0].length() - 1);
                //final String obj = triple[2].substring(1, triple[2].length() - 1);
                final String obj = triplee[0].substring(1, triplee[0].length() - 1);


                String s = subj;
                String o = obj;
                Pattern regexSub = Pattern.compile("[^/]+$");
                Matcher regexMatcherSub = regexSub.matcher(s);

                Pattern regexObj = Pattern.compile("[^/]+$");
                Matcher regexMatcherObj = regexObj.matcher(o);

                if (regexMatcherSub.find()) {
                    s = regexMatcherSub.group();
                    if (regexMatcherObj.find()) {
                        o = regexMatcherObj.group();
                        //  s.substring(s.length()-1);


                        System.out.print("test    " + s);
                        System.out.println();
                        System.out.print("test    " + o);

                        maxWeight = 0.0;
                        final double subjWeight = calculateWeight(s, o);
                        final double objWeight = calculateWeight(o, s);


                        double similarity = Math.max(subjWeight, objWeight);


                        writer.write(triple[0]);
                        writer.write(" ");
                        writer.write(triple[1]);
                        writer.write(" ");
                        writer.write(triple[2]);
                        writer.write(" ");
                        writer.write(triple[3]);
                        writer.write(" ");
                        writer.write(triple[4]);
                        writer.newLine();


                        writer.write(triplee[0]);
                        writer.write(" ");
                        writer.write(triplee[1]);
                        writer.write(" ");
                        writer.write(triplee[2]);
                        writer.write(" ");
                        writer.write(triplee[3]);
                        writer.write(" ");
                        writer.write(triplee[4]);
                        writer.newLine();


                        if (line1.startsWith("_:")) {
                            continue;
                        } else if (line2.startsWith("_:")) {
                            continue;
                        } else {

                            if (similarity > 0 && similarity < 2) {
                                writer.write(triple[0]);
                                writer.write(" ");
                                writer.write("<weak_relation>");
                                writer.write(" ");
                                writer.write(triplee[0]);
                                writer.write(" ");
                                writer.write(String.valueOf(similarity));
                                writer.newLine();

                            } else if (similarity > 2 && similarity < 3) {
                                writer.write(triple[0]);
                                writer.write(" ");
                                writer.write("<related_somehow>");
                                writer.write(" ");
                                writer.write(triplee[0]);
                                writer.write(" ");
                                writer.write(String.valueOf(similarity));
                                writer.newLine();

                            } else if (similarity > 3 && similarity < 4) {
                                writer.write(triple[0]);
                                writer.write(" ");
                                writer.write("<related_to>");
                                writer.write(" ");
                                writer.write(triplee[0]);
                                writer.write(" ");
                                writer.write(String.valueOf(similarity));
                                writer.newLine();

                            } else if (similarity > 4 && similarity < 5) {
                                writer.write(triple[0]);
                                writer.write(" ");
                                writer.write("<type_of>");
                                writer.write(" ");
                                writer.write(triplee[0]);
                                writer.write(" ");
                                writer.write(String.valueOf(similarity));
                                writer.newLine();

                            } else if (similarity > 5 && similarity < 6) {
                                writer.write(triple[0]);
                                writer.write(" ");
                                writer.write("<same_as>");
                                writer.write(" ");
                                writer.write(triplee[0]);
                                writer.write(" ");
                                writer.write(String.valueOf(similarity));
                                writer.newLine();

                            } else if (similarity > 6 && similarity < 7) {
                                similarity = 9999;
                                writer.write(triple[0]);
                                writer.write(" ");
                                writer.write("<identical>");
                                writer.write(" ");
                                writer.write(triplee[0]);
                                writer.write(" ");
                                writer.write(String.valueOf(similarity));
                                writer.newLine();

                            } else if (similarity > 7 && similarity < 8) {
                                similarity = 9999;
                                writer.write(triple[0]);
                                writer.write(" ");
                                writer.write("<identical>");
                                writer.write(" ");
                                writer.write(triplee[0]);
                                writer.write(" ");
                                writer.write(String.valueOf(similarity));
                                writer.newLine();

                            } else if (similarity > 8 && similarity < 9) {
                                similarity = 9999;
                                writer.write(triple[0]);
                                writer.write(" ");
                                writer.write("<strong_relation>");
                                writer.write(" ");
                                writer.write(triplee[0]);
                                writer.write(" ");
                                writer.write(String.valueOf(similarity));
                                writer.newLine();

                            } else {
                                writer.write(triple[0]);
                                writer.write(" ");
                                writer.write("<No_relation>");
                                writer.write(" ");
                                writer.write(triplee[0]);
                                writer.write(" ");
                                writer.write(String.valueOf(similarity));
                                writer.newLine();


                            }
                        }
                    }
                }
            }
        }

        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    static private final HttpClient client = new HttpClient();
    static private Double maxWeight = 0.0;
    static private Integer maxIntegration = 4;

    static private double calculateWeight(String src, String target)
    {
        accumulateWeight(src, target, 0.0, maxIntegration);

        return maxWeight;
    }

    static private void accumulateWeight(String key, String target, Double accWeight, int integration)
    {
        if (key.equals(target))
        {
            final Double averageWeight = accWeight / (maxIntegration - integration);

            if (averageWeight > maxWeight)
            {
                maxWeight = averageWeight;
            }

            return;
        }

        if (integration == 0)
        {
            if (key.equals(target))
            {
                final Double averageWeight = accWeight / (maxIntegration - integration);

                if (averageWeight > maxWeight)
                {
                    maxWeight = averageWeight;
                }

                return;
            }

            return;

        }

        try {

            // final GetMethod getMethod = new GetMethod(URLEncoder.encode("http://conceptnet5.media.mit.edu/data/5.3/c/en/" + key, "ISO-8859-1"));
            final GetMethod getMethod = new GetMethod(
                    "http://conceptnet5.media.mit.edu/data/5.3/c/en/" +
                            URLEncoder.encode(key, "ISO-8859-1"));
            try
            {
                client.executeMethod(getMethod);

                final String jsonStr = getMethod.getResponseBodyAsString();
                final JsonObject root = new Gson().fromJson(jsonStr, JsonObject.class);

                for (JsonElement item : root.getAsJsonArray("edges"))
                {
                    final JsonObject entity = item.getAsJsonObject();
                    final String[] start = entity.getAsJsonPrimitive("start").getAsString().substring(1).split("\\/");

                    if (start[1].equals("en") && start[2].equals(key))
                    {
                        final String[] objs = entity.getAsJsonPrimitive("end").getAsString().substring(1).split("\\/");

                        if (objs[1].equals("en"))
                        {
                            final Double weight = entity.getAsJsonPrimitive("weight").getAsDouble();

                            accumulateWeight(objs[2].replace("\\", ""), target, accWeight + weight, integration - 1);
                        }
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
    }
}
