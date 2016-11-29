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
    public Map<String, String> metadata = new HashMap<String, String>();
    public Integer unitcol = -1;
    Attribute(Resource r) {
        resource = r;
    }
}
