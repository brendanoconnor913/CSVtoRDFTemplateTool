import org.apache.jena.rdf.model.Resource;
import org.javatuples.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Created by brendan on 11/7/16.
 */
public class Entity {
    private Resource resource;
    private Boolean isMeta;
    public String datatype;
    public String unit;
    public Integer index;
    // If empty no metadata, all integers are column indicies of metdata for this attribute
    private Vector<Entity> metadata = new Vector<Entity>();
    Entity(Resource r, Integer indx) {
        resource = r;
        isMeta = false;
        datatype = "";
        unit = "";
        index = indx;
    }

    Entity(Resource r) {
        isMeta = false;
        resource = r;
    }

    public String   toString() {return resource.toString();}
    public void     addMeta(Entity metaEnt) {metadata.addElement(metaEnt);}
//    public void     addMetaList(Vector<Entity> metaList) {metadata.addAll(metaList);}
    public Boolean  isMetaEntity() {return isMeta;}
    public Vector<Entity> getMetadata() { return metadata;}
    public void     setMetaEntity() {isMeta = true;}
    public Resource getResource() {return resource;}
    public Boolean  hasMetaData() {
        return !metadata.isEmpty();
    }
    public Boolean  equals(Entity a2) {
        return resource.equals(a2.resource);
    }
}
