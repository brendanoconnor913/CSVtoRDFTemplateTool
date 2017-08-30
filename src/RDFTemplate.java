import java.io.*;
import java.util.HashSet;
import java.util.Vector;

import dnl.utils.text.table.TextTable;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
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
    private Model templategraph  = ModelFactory.createDefaultModel();
    private String graphname;
    private Boolean newTemplate;
    private Resource templatenode;

    // Template constructor from previously saved template
    RDFTemplate(Resource tempnode, String graph) {
        graphname = graph;
        templategraph.read(graphname);
        newTemplate = false;
        templatenode = tempnode;
        try {
            subject = getSubject(tempnode);
            attributes = getAttributes(tempnode);
            observations = getObservations(tempnode);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Constructor for newly created template
    RDFTemplate(Vector<Entity> nSubject, Vector<Entity> nAttributes, String graph) {
        subject = nSubject;
        attributes = nAttributes;
        observations = 1;
        graphname = graph;
        templategraph.read(graphname);
        newTemplate = true;
    }

    private void writeToModel() {
        try {
            PrintWriter pw = new PrintWriter(graphname);
            templategraph.write(pw, "NT");
            pw.close();
        }
        catch(Exception exec) {
            exec.printStackTrace();
        }
    }

    // adds pred,obj to subj and store in graph
    private void createTriple(Resource r, String predicate, String object){
        r.addProperty(ResourceFactory.createProperty(predicate),
                ResourceFactory.createPlainLiteral(object));
        writeToModel();
    }

    private void createPredicate(Resource s, String predicate, Resource o){
        s.addProperty(ResourceFactory.createProperty(predicate), o);
        writeToModel();
    }

    private Integer getObservations(Resource tempnode) throws Exception {
        // each template should only have one observation node
        StmtIterator itr = tempnode.listProperties(templategraph.getProperty("http://umkc.edu/numObservations"));
        Literal observations = itr.nextStatement().getObject().asLiteral();
        return observations.getInt();
    }

    Integer getSubjectSize() {return subject.size();}

    Vector<Entity> getTemplateSubject() {return subject;}

    Boolean sameEntities(Vector<Entity> header) {
        HashSet<String> allEntities = new HashSet<String>();
        Vector<Entity> newSub = new Vector<Entity>();
        Vector<Entity> newAttr = new Vector<Entity>();

        for (Entity e : subject) {
            allEntities.add(e.toString());
        }
        HashSet<String> headerEntities = new HashSet<String>();
        for (Entity e : header) {
            String estr = e.toString();
            // allEntities only subjects for now
            if (allEntities.contains(estr)) {
                newSub.add(e);
            }
            else {
                newAttr.add(e);
            }
            headerEntities.add(estr);
        }

        for (Entity e : attributes) {
            allEntities.add(e.toString());
        }

        Boolean same =  allEntities.equals(headerEntities);
        if (same) {
            subject = newSub;
            attributes = newAttr;
        }
        return same;
    }

    private Vector<Entity> getResults(Resource basenode, String pred) {
        Vector<Entity> outresults = new Vector<Entity>();
        StmtIterator itr = basenode.listProperties(templategraph.getProperty(pred));
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
    private Entity getMetaAttributes(Resource basenode) throws Exception {
        // valueAttribute will only appear once for each base meta node
        Vector<Entity> value = getResults(basenode, "http://umkc.edu/valueAttribute");
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
        StmtIterator itr = tempnode.listProperties(templategraph.getProperty("http://umkc.edu/attribute"));
        while (itr.hasNext()) {
            Resource attr = itr.nextStatement().getObject().asResource();
//            Entity e = getMetaAttributes(metar);
            tAttributes.add(new Entity(attr));
        }
        if (tAttributes.isEmpty()) {throw new Exception("No attributes found");}
        return tAttributes;
    }

    private void writeMetaNodes(Entity currentEntity,  Resource metaAnon) {
        createPredicate(metaAnon, "http://umkc.edu/valueAttribute", currentEntity.getResource());
        for (Entity meta : currentEntity.getMetadata()) {
            if (meta.hasMetaData()) {
                Resource nextmeta = templategraph.createResource();
                createPredicate(metaAnon, "http://umkc.edu/metaAttributeWithMeta", nextmeta);
                writeMetaNodes(meta, nextmeta);
            }
            else { // add all entities w/o metadata to anon node
                createPredicate(metaAnon, "http://umkc.edu/metaAttribute", meta.getResource());
            }
        }
    }

    private String getMetaString(String baseEntity, Entity currentEntity) {
        StringBuilder accstring = new StringBuilder();
        String anonNode = "_:metadata"+Integer.toString(attributes.indexOf(currentEntity));
        accstring.append(baseEntity + " <" + currentEntity.getResource() + "> "+ anonNode +" .\n");
        accstring.append(anonNode + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> " +
                "\"${" + currentEntity.getCSVAlias() + "}\"" + currentEntity.datatype + " .\n");
        for (Entity meta : currentEntity.getMetadata()) {
            if (meta.hasMetaData()) {
                accstring.append(getMetaString(anonNode, meta));
            }
            else { // add all entities w/o metadata to anon node
                accstring.append(anonNode + " <" + meta.getResource() + "> \"${" +
                        meta.getCSVAlias() + "}\""+ meta.datatype +" .\n");
            }

        }
        return accstring.toString();
    }

    public void writeToTemplateGraph() {
        if (!newTemplate) {
            templatenode.removeAll(templategraph.getProperty("http://umkc.edu/numObservations"));
            createTriple(templatenode,"http://umkc.edu/numObservations", Integer.toString(observations+1));
            writeToModel();
            return;
        }

        Resource tempAnonNode = templategraph.createResource();
        createTriple(tempAnonNode, "http://umkc.edu/numObservations", observations.toString());
        try {
            for (Entity e : subject) {
                createPredicate(tempAnonNode, "http://umkc.edu/subject", e.getResource());
            }

            for (Entity e : attributes) {
//                if (e.isMetaEntity()) {
//                    continue;
//                }
//                else if (e.hasMetaData()) {
//                    Resource metaAnon = templategraph.createResource();
//                    createPredicate(tempAnonNode, "http://umkc.edu/attributeWithMeta", metaAnon);
//                    writeMetaNodes(e, metaAnon);
//                } else {
                createPredicate(tempAnonNode, "http://umkc.edu/attribute", e.getResource());
//                }
            }
        }
        catch (Exception e) { e.printStackTrace();}
        writeToModel();
    }

    public void writeToTemplateFile(String templatefile) {
        try {
            StringBuilder subjectbuilder = new StringBuilder();
            subjectbuilder.append("<http://umkc.edu/subject/");
            for (Entity e : subject) {
                subjectbuilder.append("${" + e.getCSVAlias() + "}" + "-");
            }
            subjectbuilder.deleteCharAt(subjectbuilder.length() - 1); // final subject string
            subjectbuilder.append(">");

            StringBuilder template = new StringBuilder();
            final String MADEOF = "<http://umkc.edu/composedOf>";
            if (subject.size() > 1) {
                template.append(subjectbuilder.toString() + " " + MADEOF + " _:comp . \n");
                for (Entity e : subject) {
                    template.append("_:comp <" + e.toString() + "> \"${"
                            + e.getCSVAlias() + "}\" . \n");
                }
            }

            final String subjectnode = "_:subject";
            final String GROUPEDREC = "<http://umkc.edu/groupedRecord>";
            template.append(subjectbuilder.toString() + " " + GROUPEDREC + " " + subjectnode + " . \n");

            for (Entity e : attributes) {
                if (e.isMetaEntity()) {
                    continue;
                }
                // add any anonymous node for metadata
                else if (e.hasMetaData()) {
                    String metastruct = getMetaString(subjectnode, e);
                    template.append(metastruct);
                } else {
                    template.append(subjectnode + " <" + e.toString() + "> \"${" +
                            e.getCSVAlias() + "}\"" + e.datatype + " .\n");
                }
            }

            String noSuffix = CSVConverter.stripFileExtension(templatefile);

            File tempdir = new File("output-templates");
            if (!tempdir.exists()) {
                tempdir.mkdir();
            }

            File outfile = new File(tempdir, noSuffix + "-template.nt");
            PrintStream out = new PrintStream(new FileOutputStream(outfile));
            out.print(template.toString());
            out.close();
        }
        catch (Exception e) { e.printStackTrace();}
    }

    public static void main(String args[]) {
//        Model model = ModelFactory.createDefaultModel();
//        model.read("templates.nt");
//        String query;
//        query = "SELECT ?s ?x WHERE { ?s <http://umkc.edu/numObservations> ?x . }";
//        QueryExecution qexec = QueryExecutionFactory.create(query, model);
//        ResultSet result = qexec.execSelect();
//        while (result.hasNext()) {
//            System.out.println("new temp");
//            QuerySolution soln = result.nextSolution();
//            Resource s = soln.getResource("s");
//            new RDFTemplate(s, "templates.nt");
//        }

    }
}
