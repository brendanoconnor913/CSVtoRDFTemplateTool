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
//    public String unit;
    private String csvAlias;

    // If empty no metadata, all integers are column indicies of metdata for this attribute
    private Vector<Entity> metadata = new Vector<Entity>();
    // Entity created from csv file
    Entity(Resource r, String alias) {
        resource = r;
        isMeta = false;
        datatype = "";
        csvAlias = alias;
        //unit = "";
    }
    // Entity created from saved graph
    Entity(Resource r) {
        isMeta = false;
        resource = r;
    }

    public String   toString() {return resource.toString();}
    public String   getCSVAlias() {return csvAlias;}
    public void     addCSVAlias(String alias) {csvAlias = alias;}
    public void     addMeta(Entity metaEnt) {
        metaEnt.setMetaEntity();
        metadata.addElement(metaEnt);
    }
//    public void     addMetaList(Vector<Entity> metaList) {metadata.addAll(metaList);}
    public Boolean  isMetaEntity() {return isMeta;}
    public Vector<Entity> getMetadata() { return metadata;}
    public void     setMetaEntity() {isMeta = true;}
    public Resource getResource() {return resource;}
    public Boolean  hasMetaData() {
        return !metadata.isEmpty();
    }
    // Down the line equality will need to search entity resolution graph to see if same entity
    public Boolean  equals(Entity a2) {
        return resource.equals(a2.resource);
    }
}
