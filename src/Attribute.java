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
    public String unit;
    public Integer index;
    // If empty no metadata, all integers are column indicies of metdata for this attribute
    public Vector<Attribute> metadata = new Vector<Attribute>();
    Attribute(Resource r, Integer indx) {
        resource = r;
        isMeta = false;
        datatype = "";
        unit = "";
        index = indx;
    }

    public Boolean hasMetaData() {
        return !metadata.isEmpty();
    }
}
