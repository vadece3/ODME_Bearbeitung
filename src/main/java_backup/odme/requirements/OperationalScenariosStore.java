package odme.requirements;

import java.io.File;
import java.util.ArrayList;
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
 * Operational scenarios as first-class artifacts (<project>/operationalscenarios.xml).
 * Every requirement's "source" must reference one of these, so the origin of each
 * requirement is itself a specified, citable object (chain: OS -> REQ -> BT model).
 */
public final class OperationalScenariosStore {

    /** Simple value object: an operational scenario. */
    public static class OperationalScenario {
        private String id;    // e.g. "OS-1"
        private String text;  // full natural-language specification

        public OperationalScenario(String id, String text) {
            this.id = id; this.text = text;
        }
        public String getId() { return id; }
        public String getText() { return text; }
        public void setId(String id) { this.id = id; }
        public void setText(String text) { this.text = text; }
        @Override public String toString() { return id; }
    }

    private OperationalScenariosStore() { }

    public static File osFile() {
        return new File(ODMEEditor.fileLocation + "/" + ODMEEditor.projName
                + "/operationalscenarios.xml");
    }

    public static List<OperationalScenario> load() {
        List<OperationalScenario> result = new ArrayList<>();
        File f = osFile();
        if (!f.exists()) return result;
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.parse(f);
            NodeList nodes = doc.getElementsByTagName("operationalScenario");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = (Element) nodes.item(i);
                result.add(new OperationalScenario(e.getAttribute("id"),
                        e.getTextContent().trim()));
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        return result;
    }

    public static boolean save(List<OperationalScenario> list) {
        File f = osFile();
        try {
            f.getParentFile().mkdirs();
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.newDocument();
            Element root = doc.createElement("operationalScenarios");
            doc.appendChild(root);
            for (OperationalScenario os : list) {
                Element e = doc.createElement("operationalScenario");
                e.setAttribute("id", os.getId());
                e.setTextContent(os.getText());
                root.appendChild(e);
            }
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(doc), new StreamResult(f));
            return true;
        } catch (Exception ex) { ex.printStackTrace(); return false; }
    }

    /** All defined OS ids, for the Source dropdown. */
    public static List<String> ids() {
        List<String> result = new ArrayList<>();
        for (OperationalScenario os : load()) result.add(os.getId());
        return result;
    }
}
