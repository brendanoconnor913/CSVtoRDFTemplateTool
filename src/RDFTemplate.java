import java.util.Vector;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Model;

/**
 * Created by brendan on 8/17/17.
 */

public class RDFTemplate {
    private Vector<Entity> subject = new Vector<Entity>();
    private Vector<Entity> attributes = new Vector<Entity>();
    private Integer observations;
    private Model model;

    private Integer getObservations(Resource tempnode) throws Exception {
        String query = "SELECT ?x WHERE { "+ tempnode.toString() +" <http://umkc.edu/numObservations> ?x .}";
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();
        if (result.hasNext()) { // attribute not identified
            QuerySolution soln = result.nextSolution() ;
            Literal l = soln.getLiteral("x");
            return l.getInt();

        } else { throw new Exception("No found observations"); }
    }

    // change back to private later
    private Vector<Entity> getResults(Resource basenode, String pred) {
        String query;
        if (basenode.isAnon()) {
            query = "SELECT ?x WHERE { _:"+basenode.toString()+" <"+pred+"> ?x .}";
        }
        else {
            query = "SELECT ?x WHERE { "+basenode.toString()+" <"+pred+"> ?x .}";
        }
        Vector<Entity> outresults = new Vector<Entity>();
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();
        while (result.hasNext()) {
            QuerySolution soln = result.nextSolution() ;
            Resource r = soln.getResource("x");
            System.out.println(r.toString());
            outresults.add(new Entity(r));
        }
        return outresults;
    }

    private Vector<Entity> getSubject(Resource tempnode) throws Exception {
        Vector<Entity> tsubject = getResults(tempnode, "http://umkc.edu/subject");
        if (tsubject.isEmpty()) {throw new Exception("No subject found");}
        return tsubject;
    }

    // called on each attributeWithMeta anon node
    private Entity getMetaAttributes(Resource basenode) throws Exception {
        // valueAttribute will only appear once for each base meta node
        System.out.println(basenode.toString());
        Vector<Entity> value = getResults(basenode, "http://umkc.edu/valueAttribute");
        if (value.size() != 1) {
//            for (Entity e : value) { System.out.println(value.toString());}
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
        Vector<Entity> tAttributes = getResults(tempnode, "http://umkc.edu/attribute");

        String metaquery = "SELECT ?x WHERE { "+ tempnode.toString() +" <http://umkc.edu/attributeWithMeta> ?x .}";
        QueryExecution qmetaexec = QueryExecutionFactory.create(metaquery, model);
        ResultSet metaresult = qmetaexec.execSelect();
        while (metaresult.hasNext()) {
            QuerySolution metasoln = metaresult.nextSolution();
            Resource metar = metasoln.getResource("x");
            System.out.println(metar.toString());
            Entity e = getMetaAttributes(metar);
            tAttributes.add(getMetaAttributes(metar));
            for (Entity me : e.getMetadata()) {
                System.out.println(me.toString());
            }
        }
        if (tAttributes.isEmpty()) {throw new Exception("No subject found");}
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
        RDFTemplate template = new RDFTemplate(model.getResource("_:Be7fa923X3A15e065ee4c0X3AX2D7fff"), model);
    }

//    Boolean isEqual(RDFTemplate t2) {};
}
