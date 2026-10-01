package odme.sampling.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the entire scenario model parsed from the YAML file.
 * It holds lists of all the parameters that define the sampling space.
 */
@Data // Lombok annotation to auto-generate getters, setters, toString, etc.
public class Scenario {
    private List<Parameter> parameters = new ArrayList<>();
    private List<String> constraint = new ArrayList<>();
}