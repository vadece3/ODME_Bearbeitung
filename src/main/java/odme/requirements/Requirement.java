package odme.requirements;

/**
 * A requirement item derived from an operational scenario.
 * Part of the requirements-traceability extension (requirements <-> BT models).
 */
public class Requirement {

    private String id;      // e.g. "REQ-01"
    private String text;    // the requirement statement
    private String source;  // e.g. "OS-1" (the operational scenario it derives from)

    public Requirement(String id, String text, String source) {
        this.id = id;
        this.text = text;
        this.source = source;
    }

    public String getId() { return id; }
    public String getText() { return text; }
    public String getSource() { return source; }

    public void setId(String id) { this.id = id; }
    public void setText(String text) { this.text = text; }
    public void setSource(String source) { this.source = source; }

    @Override
    public String toString() {
        return id + ": " + text;
    }
}
