import org.apache.jena.rdf.model.Resource;
import org.javatuples.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Created by brendan on 11/7/16.
 */
public class Attribute {
    public static final Integer NOUNIT = -1; // indicates attribute doesn't have unit col dependency
    public static final Integer ISUNIT = -2; // indicates attribute is a col that specifies unit
    public Resource resource;
    public Map<String, String> metadata = new HashMap<String, String>();
    // unitcol specifies index of col containing unit info for this attribute if value isn't a constant above
    public Integer unitcol;
    Attribute(Resource r) {
        unitcol = NOUNIT;
        resource = r;
    }
}
