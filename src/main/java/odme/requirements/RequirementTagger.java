package odme.requirements;

import java.io.File;
import java.util.List;

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

/**
 * Makes the SCENARIO FILE self-describing (per supervisor decision, Aug):
 * after every link change, a <TracedRequirements> block is (re)written into
 * the scenario's XML file  <project>/<scenario>/<project>.xml , listing the
 * linked requirement ids and their operational-scenario source, e.g.
 *
 *   <TracedRequirements>
 *     <Requirement id="REQ-01" source="OS-1"/>
 *   </TracedRequirements>
 *
 * A previously written block in behaviourxml.xml (earlier design) is removed
 * once, so the information lives in exactly one place.
 */
public final class RequirementTagger {

    private RequirementTagger() { }

    /** The scenario file: <fileLocation>/<proj>/<scenario>/<proj>.xml */
    public static File scenarioXmlFile(String scenarioName) {
        return new File(ODMEEditor.fileLocation + "/" + ODMEEditor.projName
                + "/" + scenarioName + "/" + ODMEEditor.projName + ".xml");
    }

    /** Old target of the block (kept only for cleanup). */
    public static File behaviourXmlFile(String scenarioName) {
        return new File(ODMEEditor.fileLocation + "/" + ODMEEditor.projName
                + "/" + scenarioName + "/behaviourxml.xml");
    }

    /** Rewrites the TracedRequirements block of the scenario's XML file. */
    public static void retag(String scenarioName) {
        File f = scenarioXmlFile(scenarioName);
        if (!f.exists()) {
            System.out.println("RequirementTagger: scenario file not found for "
                    + scenarioName + " (" + f.getAbsolutePath() + ") - tagging skipped.");
            return;
        }
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.parse(f);
            Element root = doc.getDocumentElement();

            removeBlock(root);

            Element block = doc.createElement("TracedRequirements");
            List<Requirement> requirements = RequirementsStore.load();
            for (TraceLink l : TraceLinksStore.load()) {
                if (!l.getScenarioName().equalsIgnoreCase(scenarioName)) continue;
                Element e = doc.createElement("Requirement");
                e.setAttribute("id", l.getRequirementId());
                for (Requirement r : requirements) {
                    if (r.getId().equalsIgnoreCase(l.getRequirementId())) {
                        e.setAttribute("source", r.getSource() == null ? "" : r.getSource());
                        break;
                    }
                }
                block.appendChild(e);
            }
            root.insertBefore(block, root.getFirstChild());

            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(doc), new StreamResult(f));
            System.out.println("RequirementTagger: scenario file of " + scenarioName
                    + " re-tagged (" + f.getName() + ").");

            cleanupOldBehaviourBlock(scenarioName);
            retagMergedFile(scenarioName);
        } catch (Exception ex) {
            System.out.println("RequirementTagger failed for " + scenarioName);
            ex.printStackTrace();
        }
    }

    /** Removes any TracedRequirements block previously written to behaviourxml.xml. */
    private static void cleanupOldBehaviourBlock(String scenarioName) {
        File f = behaviourXmlFile(scenarioName);
        if (!f.exists()) return;
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.parse(f);
            Element root = doc.getDocumentElement();
            NodeList old = root.getElementsByTagName("TracedRequirements");
            if (old.getLength() == 0) return;
            removeBlock(root);
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(doc), new StreamResult(f));
            System.out.println("RequirementTagger: old block removed from behaviourxml.xml.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void removeBlock(Element root) {
        NodeList old = root.getElementsByTagName("TracedRequirements");
        for (int i = old.getLength() - 1; i >= 0; i--) {
            old.item(i).getParentNode().removeChild(old.item(i));
        }
    }

    /** The merged execution file: <proj>/<scenario>/MergedXMLforxsd.xml */
    public static java.io.File mergedXmlFile(String scenarioName) {
        return new java.io.File(ODMEEditor.fileLocation + "/" + ODMEEditor.projName
                + "/" + scenarioName + "/MergedXMLforxsd.xml");
    }

    /** Writes the TracedRequirements block into the merged execution file, if it exists.
     *  Called after every link change and at the end of the Merge action, so the
     *  generated artifact always carries the requirement ids (supervisor request). */
    public static void retagMergedFile(String scenarioName) {
        java.io.File f = mergedXmlFile(scenarioName);
        if (!f.exists()) return;
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.parse(f);
            Element root = doc.getDocumentElement();
            removeBlock(root);
            Element block = doc.createElement("TracedRequirements");
            List<Requirement> requirements = RequirementsStore.load();
            for (TraceLink l : TraceLinksStore.load()) {
                if (!l.getScenarioName().equalsIgnoreCase(scenarioName)) continue;
                Element e = doc.createElement("Requirement");
                e.setAttribute("id", l.getRequirementId());
                for (Requirement r : requirements) {
                    if (r.getId().equalsIgnoreCase(l.getRequirementId())) {
                        e.setAttribute("source", r.getSource() == null ? "" : r.getSource());
                        break;
                    }
                }
                block.appendChild(e);
            }
            root.insertBefore(block, root.getFirstChild());
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(doc), new StreamResult(f));
            System.out.println("RequirementTagger: merged file of " + scenarioName + " re-tagged.");
        } catch (Exception ex) {
            System.out.println("RequirementTagger: merged-file tagging failed for " + scenarioName);
            ex.printStackTrace();
        }
    }

}
