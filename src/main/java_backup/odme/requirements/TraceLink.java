package odme.requirements;

import java.util.Objects;

/**
 * One trace link: requirement <-> BT model (identified by its scenario name).
 * The set of all links is the traceability relation of the project.
 */
public class TraceLink {

    private final String requirementId;
    private final String scenarioName;

    public TraceLink(String requirementId, String scenarioName) {
        this.requirementId = requirementId;
        this.scenarioName = scenarioName;
    }

    public String getRequirementId() { return requirementId; }
    public String getScenarioName() { return scenarioName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TraceLink)) return false;
        TraceLink other = (TraceLink) o;
        return requirementId.equalsIgnoreCase(other.requirementId)
                && scenarioName.equalsIgnoreCase(other.scenarioName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requirementId.toLowerCase(), scenarioName.toLowerCase());
    }

    @Override
    public String toString() {
        return requirementId + " -> " + scenarioName;
    }
}
