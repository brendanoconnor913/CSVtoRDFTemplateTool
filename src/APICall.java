import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.json.*;

/**
 * Created by brendan on 11/14/16.
 */
public class APICall {

    // function that checks for the presence of pred-obj in given json array
    public static boolean isPresent(JSONArray ja, String pred, String obj) {
        boolean present = false;
        for(int i = 0; i < ja.length(); i++) {
            JSONObject jo = (JSONObject)ja.get(i);
            if(jo.get("rel").equals(pred)){
                String end = jo.get("end").toString();
                if(end.contains(obj)){
                    present = true;
                }
            }
        }
        return present;
    }

    // function that calls concept net and returns a bool based on prescence of key words that indicate
    // object given is a measurement
    public static boolean isMeasurement(String object) {
        try {

            String url = "http://conceptnet5.media.mit.edu/data/5.3/c/en/"+object;

            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(url);

            // add request header
            request.addHeader("User-Agent", "Mozilla/5.0");

            HttpResponse response = client.execute(request);

            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            JSONObject job = new JSONObject(result.toString());
            JSONArray jsarray = job.getJSONArray("edges");
            // check for multiple words
            boolean a = isPresent(jsarray,"/r/IsA","measurement");
            boolean b = isPresent(jsarray,"/r/RelatedTo","measurement");
            boolean c = isPresent(jsarray,"/r/RelatedTo","count");
            boolean d = isPresent(jsarray,"/r/RelatedTo","quantity");
            boolean isMeasurement =  a || b || c || d;
            rd.close();
            return isMeasurement;
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

//    public static void main(String args[]) {
//        APICall ac = new APICall();
//        AttributeHandler ah = new AttributeHandler();
//        String test = ah.formatAttribute("yard");
//        System.out.print(ac.isMeasurement(test));
//    }
}
