package behaviourtreetograph;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

/**
 * Merges a pruned scenario (xmlforxsd.xml) with its behaviour tree (OutputBehaviourxml.xml)
 * into one executable file (MergedXMLforxsd.xml).
 *
 * The behaviour tree's root element is appended as the last child of the scenario's root element
 * (the same structure the old text-based merge produced), but the merge works on the parsed XML
 * instead of on text lines. So it no longer depends on line breaks, blank lines or the position
 * of the closing tag, and it never writes a half-finished or empty file: if an input is missing,
 * empty or not valid XML, the old output is left untouched and a clear message is returned.
 */
public final class ScenarioBehaviourMerger {

    private ScenarioBehaviourMerger() {
    }

    /** Result of a merge: success flag and a message for the user / console. */
    public static final class Result {
        public final boolean ok;
        public final String message;

        Result(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }
    }

    public static Result merge(File scenarioXml, File behaviourXml, File output) {
        if (!scenarioXml.exists() || scenarioXml.length() == 0) {
            return new Result(false, "The scenario file is missing or empty:\n" + scenarioXml.getPath()
                    + "\n\nSave the scenario first (save button in the main window), then merge again.");
        }
        if (!behaviourXml.exists() || behaviourXml.length() == 0) {
            return new Result(false, "The behaviour tree file is missing or empty:\n" + behaviourXml.getPath()
                    + "\n\nSave the behaviour graph first (Save Graph in the behaviour window), then merge again.");
        }

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();

            Document scenario;
            try {
                scenario = db.parse(scenarioXml);
            } catch (Exception e) {
                return new Result(false, "The scenario file is not valid XML:\n" + scenarioXml.getPath()
                        + "\n" + e.getMessage());
            }

            Document behaviour;
            try {
                behaviour = db.parse(behaviourXml);
            } catch (Exception e) {
                return new Result(false, "The behaviour tree file is not valid XML:\n" + behaviourXml.getPath()
                        + "\n" + e.getMessage());
            }

            Element scenarioRoot = scenario.getDocumentElement();
            Element behaviourRoot = behaviour.getDocumentElement();

            // Remove a behaviour tree left from an earlier merge, if the input already contains one
            for (Node n = scenarioRoot.getFirstChild(); n != null; ) {
                Node next = n.getNextSibling();
                if (n instanceof Element && ((Element) n).getTagName().equals(behaviourRoot.getTagName())) {
                    scenarioRoot.removeChild(n);
                }
                n = next;
            }

            Node imported = scenario.importNode(behaviourRoot, true);
            scenarioRoot.appendChild(imported);

            // Write to a temporary file first, then replace the output, so a failure never
            // leaves a 0-byte MergedXMLforxsd.xml behind
            File tmp = new File(output.getPath() + ".tmp");
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            t.transform(new DOMSource(scenario), new StreamResult(tmp));

            if (output.exists() && !output.delete()) {
                tmp.delete();
                return new Result(false, "Could not replace " + output.getPath() + " (is it open in another program?)");
            }
            if (!tmp.renameTo(output)) {
                return new Result(false, "Could not write " + output.getPath());
            }
            return new Result(true, "Merged scenario and behaviour tree into " + output.getPath());

        } catch (Exception e) {
            return new Result(false, "Merge failed: " + e.getMessage());
        }
    }
}
