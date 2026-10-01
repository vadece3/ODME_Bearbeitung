package odme.requirements;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import odme.odmeeditor.ODMEEditor;

public final class TraceLinksStore {

    private TraceLinksStore() { }

    public static File traceLinksFile() {
        return new File(ODMEEditor.fileLocation + "/" + ODMEEditor.projName
                + "/tracelinks.xml");
    }

    public static List<TraceLink> load() {
        List<TraceLink> result = new ArrayList<>();
        File f = traceLinksFile();
        if (!f.exists()) return result;
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.parse(f);
            NodeList nodes = doc.getElementsByTagName("link");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = (Element) nodes.item(i);
                result.add(new TraceLink(e.getAttribute("requirement"),
                        e.getAttribute("scenario")));
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        return result;
    }

    public static boolean save(List<TraceLink> links) {
        File f = traceLinksFile();
        try {
            f.getParentFile().mkdirs();
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.newDocument();
            Element root = doc.createElement("tracelinks");
            doc.appendChild(root);
            for (TraceLink l : links) {
                Element e = doc.createElement("link");
                e.setAttribute("requirement", l.getRequirementId());
                e.setAttribute("scenario", l.getScenarioName());
                root.appendChild(e);
            }
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(doc), new StreamResult(f));
            return true;
        } catch (Exception ex) { ex.printStackTrace(); return false; }
    }

    public static boolean isLinked(String requirementId, String scenarioName) {
        return load().contains(new TraceLink(requirementId, scenarioName));
    }

    public static void addLink(String requirementId, String scenarioName) {
        List<TraceLink> links = load();
        TraceLink l = new TraceLink(requirementId, scenarioName);
        if (!links.contains(l)) {
            links.add(l);
            save(links);
            System.out.println("Trace link added: " + l);
            RequirementTagger.retag(scenarioName);   // keep scenario XML self-describing
        }
    }

    public static void removeLink(String requirementId, String scenarioName) {
        List<TraceLink> links = load();
        if (links.remove(new TraceLink(requirementId, scenarioName))) {
            save(links);
            System.out.println("Trace link removed: " + requirementId + " -> " + scenarioName);
            RequirementTagger.retag(scenarioName);   // keep scenario XML self-describing
        }
    }

    public static List<String> scenariosFor(String requirementId) {
        Set<String> result = new LinkedHashSet<>();
        for (TraceLink l : load()) {
            if (l.getRequirementId().equalsIgnoreCase(requirementId)) {
                result.add(l.getScenarioName());
            }
        }
        return new ArrayList<>(result);
    }

    public static List<String> linkedScenarios() {
        Set<String> result = new LinkedHashSet<>();
        for (TraceLink l : load()) result.add(l.getScenarioName());
        return new ArrayList<>(result);
    }

    public static List<String> btModelScenarios() {
        List<String> result = new ArrayList<>();
        File projectDir = new File(ODMEEditor.fileLocation + "/" + ODMEEditor.projName);
        File[] subdirs = projectDir.listFiles(File::isDirectory);
        if (subdirs == null) return result;
        for (File d : subdirs) {
            if (new File(d, "behaviourxml.xml").exists()) result.add(d.getName());
        }
        return result;
    }

    public static List<String> orphanModels() {
        List<String> models = btModelScenarios();
        models.removeAll(linkedScenarios());
        return models;
    }
}
