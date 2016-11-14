import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.*;

/**
 * Created by brendan on 11/14/16.
 */
public class APICall {
    //TODO: have working version of APIcall to conceptnet
    boolean isPresent(JSONArray ja, String pred, String obj) {
        boolean present = false;
        for(int i = 0; i < ja.length(); i++) {
            JSONObject jo = (JSONObject)ja.get(i);
            if(jo.get("rel").equals(pred)){
                String end = jo.get("end").toString();
                if(end.contains(obj)){
                    System.out.println(jo.get("end"));
                    present = true;
                }
            }
        }
        return present;
    }

    public void callConceptNet(String object) {
        try {

            String url = "http://conceptnet5.media.mit.edu/data/5.3/c/en/"+object;

            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(url);

            // add request header
            request.addHeader("User-Agent", "Mozilla/5.0");

            HttpResponse response = client.execute(request);

            System.out.println("\nSending 'GET' request to URL : " + url);
            System.out.println("Response Code : " +
                    response.getStatusLine().getStatusCode());

            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            JSONObject job = new JSONObject(result.toString());
            JSONArray jsarray = job.getJSONArray("edges");
            System.out.println(isPresent(jsarray,"/r/IsA","measurement"));
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        APICall ac = new APICall();
        ac.callConceptNet("time");
    }
}
