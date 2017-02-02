import org.apache.jena.rdf.model.Resource;
import org.javatuples.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Created by brendan on 11/7/16.
 */
public class Attribute {
    public Resource resource;
    public Boolean isMeta;
    public String datatype;
    // If empty no metadata, all integers are column indicies of metdata for this attribute
    public Map<String,String> metadata = new HashMap<String, String>();
    Attribute(Resource r) {
        resource = r;
        isMeta = false;
        datatype = "";
    }

    public Boolean hasMetaData() {
        return !metadata.isEmpty();
    }
}
