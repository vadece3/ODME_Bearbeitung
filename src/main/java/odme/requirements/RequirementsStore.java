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
 * Reads and writes <project>/requirements.xml.
 *
 * Design decisions (thesis, ch. design):
 *  - human-readable XML instead of Java serialization (motivated by the
 *    silent-failure defects found in the .ssd* stores),
 *  - one canonical path builder so read and write can never diverge
 *    (motivated by the path-convention defects fixed in the Save Scenario flow).
 */
public final class RequirementsStore {

    private RequirementsStore() { }

    /** Single source of truth for the file location. */
    public static File requirementsFile() {
        return new File(ODMEEditor.fileLocation + "/" + ODMEEditor.projName
                + "/requirements.xml");
    }

    /** Loads all requirements; returns an empty list if the file does not exist yet. */
    public static List<Requirement> load() {
        List<Requirement> result = new ArrayList<>();
        File f = requirementsFile();
        if (!f.exists()) {
            System.out.println("requirements.xml not found (yet): " + f.getAbsolutePath());
            return result;
        }
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.parse(f);
            NodeList nodes = doc.getElementsByTagName("requirement");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = (Element) nodes.item(i);
                result.add(new Requirement(
                        e.getAttribute("id"),
                        e.getTextContent().trim(),
                        e.getAttribute("source")));
            }
        } catch (Exception ex) {
            System.out.println("Failed to read " + f.getAbsolutePath());
            ex.printStackTrace();
        }
        return result;
    }

    /** Writes the full list (overwrites the file). */
    public static boolean save(List<Requirement> requirements) {
        File f = requirementsFile();
        try {
            f.getParentFile().mkdirs();
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.newDocument();
            Element root = doc.createElement("requirements");
            doc.appendChild(root);
            for (Requirement r : requirements) {
                Element e = doc.createElement("requirement");
                e.setAttribute("id", r.getId());
                e.setAttribute("source", r.getSource() == null ? "" : r.getSource());
                e.setTextContent(r.getText());
                root.appendChild(e);
            }
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            t.transform(new DOMSource(doc), new StreamResult(f));
            System.out.println("Saved " + requirements.size() + " requirements to "
                    + f.getAbsolutePath());
            return true;
        } catch (Exception ex) {
            System.out.println("Failed to write " + f.getAbsolutePath());
            ex.printStackTrace();
            return false;
        }
    }
}
