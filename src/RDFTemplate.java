import java.util.Iterator;
import java.util.Vector;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;

/**
 * Created by brendan on 8/17/17.
 */

public class RDFTemplate {
    private Vector<Entity> subject = new Vector<Entity>();
    private Vector<Entity> attributes = new Vector<Entity>();
    private Integer observations;
    private Model model;

    private Integer getObservations(Resource tempnode) throws Exception {
        // each template should only have one observation node
        StmtIterator itr = tempnode.listProperties(model.getProperty("http://umkc.edu/numObservations"));
        Literal observations = itr.nextStatement().getObject().asLiteral();
        return observations.getInt();
    }

    private Vector<Entity> getResults(Resource basenode, String pred) {
        Vector<Entity> outresults = new Vector<Entity>();
        StmtIterator itr = basenode.listProperties(model.getProperty(pred));
        while (itr.hasNext()) {
            Resource result = itr.nextStatement().getObject().asResource();
            outresults.add(new Entity(result));
        }
        return outresults;
    }

    private Vector<Entity> getSubject(Resource tempnode) throws Exception {
        Vector<Entity> tsubject = getResults(tempnode, "http://umkc.edu/subject");
        if (tsubject.isEmpty()) {throw new Exception("No subject found");}
        return tsubject;
    }

    // called on each attributeWithMeta anon node
    // TODO: keep getting valueattribute from multiple anon nodes, need to figure out
    private Entity getMetaAttributes(Resource basenode) throws Exception {
        // valueAttribute will only appear once for each base meta node
        Vector<Entity> value = getResults(basenode, "http://umkc.edu/valueAttribute");
//        Vector<Entity> value = new Vector<Entity>();
//        String query = "SELECT * WHERE { ?x <http://umkc.edu/attributeWithMeta> ?s ." +
//                                        "?s <http://umkc.edu/valueAttribute> ?o . }";
//        QueryExecution qexec = QueryExecutionFactory.create(query, model);
//        ResultSet result = qexec.execSelect();
//
//        while(result.hasNext()){
//            try {
//                QuerySolution soln = result.nextSolution() ;
//                Resource x = soln.getResource("x");
//                Resource s = soln.getResource("s");
//                Resource o = soln.getResource("o");
//                System.out.println("base:"+basenode.toString());
//                System.out.println("x: "+x.toString()+" s: "+s.toString()+" o: "+o.toString());
//                if (s.equals(basenode)) {
//                    value.add(new Entity(o));
//                    break;
//                }
//            }
//            catch (Exception e) {}
//        }

        if (value.size() != 1) {
            System.out.println("FAILED");
            for (Entity e : value) { System.out.println(e.toString());}
            throw new Exception("Invalid number of values");
        }
        Entity valueAttribute = value.firstElement(); // get value entity

        // add all meta entities to value meta list
        Vector<Entity> metaAttributes = getResults(basenode, "http://umkc.edu/metaAttribute");
        for (Entity e : metaAttributes) {
            e.setMetaEntity();
            valueAttribute.addMeta(e);
        }
        Vector<Entity> metaWithMeta = getResults(basenode, "http://umkc.edu/metaAttributeWithMeta");
        // get all of the metaAttribute that have meta attributes themselves
        for (Entity e : metaWithMeta) {
            Entity meta = getMetaAttributes(e.getResource());
            meta.setMetaEntity();
            valueAttribute.addMeta(meta);
        }
        return valueAttribute;
    }

    private Vector<Entity> getAttributes(Resource tempnode) throws Exception {
        Vector<Entity> tAttributes = new Vector<Entity>();
        StmtIterator itr = tempnode.listProperties(model.getProperty("http://umkc.edu/attributeWithMeta"));
        while (itr.hasNext()) {
            Resource metar = itr.nextStatement().getObject().asResource();
            Entity e = getMetaAttributes(metar);
            tAttributes.add(getMetaAttributes(metar));
            // testing output, delete later
            for (Entity me : e.getMetadata()) {
                System.out.println("META: "+me.toString());
                if (me.hasMetaData()) {
                    for (Entity mm : me.getMetadata()){
                        System.out.println("MM: "+mm.toString());
                    }
                }
            }
        }
        if (tAttributes.isEmpty()) {throw new Exception("No attributes found");}
        return tAttributes;
    }

    RDFTemplate(Resource tempnode, Model m) {
        model = m;
        try {
            subject = getSubject(tempnode);
            attributes = getAttributes(tempnode);
            observations = getObservations(tempnode);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        Model model = ModelFactory.createDefaultModel();
        model.read("templates.nt");
        String query;
        query = "SELECT ?s ?x WHERE { ?s <http://umkc.edu/numObservations> ?x . }";
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();
        while (result.hasNext()) {
            System.out.println("new temp");
            QuerySolution soln = result.nextSolution();
            Resource s = soln.getResource("s");
            new RDFTemplate(s, model);
        }

    }

//    Boolean isEqual(RDFTemplate t2) {};
}
