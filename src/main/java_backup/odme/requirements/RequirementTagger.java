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
 * Makes the scenario's BT model XML self-describing: after every link change,
 * a <TracedRequirements> block is (re)written into behaviourxml.xml, listing
 * the linked requirement ids and their operational-scenario source, e.g.
 *
 *   <TracedRequirements>
 *     <Requirement id="REQ-01" source="OS-1"/>
 *   </TracedRequirements>
 *
 * Rationale: scenario artifacts should carry their own traceability metadata
 * (self-description lesson from the MSDL standardization, Durak et al. 2018).
 */
public final class RequirementTagger {

    private RequirementTagger() { }

    public static File behaviourXmlFile(String scenarioName) {
        return new File(ODMEEditor.fileLocation + "/" + ODMEEditor.projName
                + "/" + scenarioName + "/behaviourxml.xml");
    }

    /** Rewrites the TracedRequirements block of the scenario's behaviourxml.xml. */
    public static void retag(String scenarioName) {
        File f = behaviourXmlFile(scenarioName);
        if (!f.exists()) {
            System.out.println("RequirementTagger: no behaviourxml.xml yet for "
                    + scenarioName + " - tagging skipped (will apply after Save Graph).");
            return;
        }
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.parse(f);
            Element root = doc.getDocumentElement(); // <start>

            // remove any previous block
            NodeList old = root.getElementsByTagName("TracedRequirements");
            for (int i = old.getLength() - 1; i >= 0; i--) {
                old.item(i).getParentNode().removeChild(old.item(i));
            }

            // rebuild from the current links
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
            System.out.println("RequirementTagger: behaviourxml.xml of " + scenarioName
                    + " re-tagged.");
        } catch (Exception ex) {
            System.out.println("RequirementTagger failed for " + scenarioName);
            ex.printStackTrace();
        }
    }
}
