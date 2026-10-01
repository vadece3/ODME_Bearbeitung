package odme.sampling.model;

import lombok.Data;

/**
 * Represents a single parameter from the YAML file that needs to be sampled.
 * This can be a numerical parameter or a categorical one
 */
@Data // Lombok annotation to auto-generate getters, setters, toString, etc.
public class Parameter {
    private String name;
    private String type; // e.g., "int", "double", "float", "categorical"

    // For distribution parameters
    private String distributionType;
    private String distributionDetails;

    // For numerical parameters
    private Double min;
    private Double max;

    // For categorical parameters
    private java.util.List<String> options;

    // For constraints associated with a group of parameters
    private String constraint;
}
