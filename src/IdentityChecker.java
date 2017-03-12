import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import java.lang.String;
import java.io.*;
import java.net.URLEncoder;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IdentityChecker {
    public static void addRelations(String input, String output) {
        final String CONTEXT = "<http://umkc.edu/identitychecker>";
        final File data = new File(input);
        final File quads = new File(output);

        try {
            final FileInputStream fi = new FileInputStream(data);
            final Scanner scanner = new Scanner(fi);
            final FileWriter fw = new FileWriter(quads, true);
            final BufferedWriter writer = new BufferedWriter(fw);

            while (scanner.hasNextLine()) {

                final String line1 = scanner.nextLine();
                final String[] triple = line1.split(" ");

                if (!scanner.hasNextLine()) {
                    // print uneven lines
                    writer.append(triple[0]);
                    writer.append(" ");
                    writer.append(triple[1]);
                    writer.append(" ");
                    writer.append(triple[2]);
                    writer.append(" ");
                    // writer.write(String.valueOf(similarity));
                    writer.append(" .");
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

                        if (s.equals(o) || s.startsWith(":") || o.startsWith(":")) {
                            continue;
                        }

                        System.out.print("Subject:  " + s + "\t");
                        System.out.println("Object:    " + o);

                        maxWeight = 0.0;
                        final double subjWeight = calculateWeight(s, o);
                        final double objWeight = calculateWeight(o, s);

                        double similarity = Math.max(subjWeight, objWeight);

                        if (line1.startsWith("_:")) {
                            continue;
                        } else if (line2.startsWith("_:")) {
                            continue;
                        } else {
                            if (similarity > 0 && similarity < 2) {
                                writeQuad(writer, triple[0], "<weak_relation>", triplee[0],CONTEXT);
                            } else if (similarity > 2 && similarity < 3) {
                                writeQuad(writer, triple[0], "<related_somehow>", triplee[0],CONTEXT);
                            } else if (similarity > 3 && similarity < 4) {
                                writeQuad(writer, triple[0], "<related_to>", triplee[0],CONTEXT);
                            } else if (similarity > 4 && similarity < 5) {
                                writeQuad(writer, triple[0], "<type_of>", triplee[0],CONTEXT);
                            } else if (similarity > 5 && similarity < 6) {
                                writeQuad(writer, triple[0], "<same_as>", triplee[0],CONTEXT);
                            } else if (similarity > 6 && similarity < 7) {
                                writeQuad(writer, triple[0], "<identical>", triplee[0],CONTEXT);
                            } else if (similarity > 7 && similarity < 8) {
                                writeQuad(writer, triple[0], "<identical>", triplee[0],CONTEXT);
                            } else if (similarity > 8 && similarity < 9) {
                                writeQuad(writer, triple[0], "<strong_relation>", triplee[0],CONTEXT);
                            }
//                            else {
//                                writeQuad(writer, triple[0], "<no_relation>", triplee[0],CONTEXT);
//                            }
                        }
                    }
                }
            }
            fi.close();
            fw.close();
        }

        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    static private final HttpClient client = new HttpClient();
    static private Double maxWeight = 0.0;
    static private Integer maxIntegration = 4;

    static public void writeQuad(BufferedWriter writer, String s,
                            String p, String o, String c) {
        try {
            writer.append(s + " " + p + " " + o + " " + c + "\n");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    static private double calculateWeight(String src, String target)
    {
        accumulateWeight(src, target, 0.0, maxIntegration);

        return maxWeight;
    }

    static private void accumulateWeight(String key, String target, Double accWeight, int integration)
    {
        if (key.equals(target)) {
            final Double averageWeight = accWeight / (maxIntegration - integration);
            if (averageWeight > maxWeight) {
                maxWeight = averageWeight;
            }
            return;
        }

        if (integration == 0) {
            if (key.equals(target)) {
                final Double averageWeight = accWeight / (maxIntegration - integration);

                if (averageWeight > maxWeight) {
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
            try {
                client.executeMethod(getMethod);

                final String jsonStr = getMethod.getResponseBodyAsString();
                final JsonObject root = new Gson().fromJson(jsonStr, JsonObject.class);

                for (JsonElement item : root.getAsJsonArray("edges")) {
                    final JsonObject entity = item.getAsJsonObject();
                    final String[] start = entity.getAsJsonPrimitive("start").getAsString().substring(1).split("\\/");

                    if (start[1].equals("en") && start[2].equals(key)) {
                        final String[] objs = entity.getAsJsonPrimitive("end").getAsString().substring(1).split("\\/");

                        if (objs[1].equals("en")) {
                            final Double weight = entity.getAsJsonPrimitive("weight").getAsDouble();

                            accumulateWeight(objs[2].replace("\\", ""), target, accWeight + weight, integration - 1);
                        }
                    }
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
