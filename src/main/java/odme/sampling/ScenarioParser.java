package odme.sampling;

import odme.sampling.model.Parameter;
import odme.sampling.model.Scenario;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Parses a scenario's .yaml file into a structured Scenario object.
 */
public class ScenarioParser {

    @SuppressWarnings("unchecked")
    public Scenario parse(String yamlFilePath) throws FileNotFoundException {
        Yaml yaml = new Yaml();

        // use try-with-resources to ensure the stream is closed
        try (InputStream inputStream = new FileInputStream(yamlFilePath)) {
            Object loaded = yaml.load(inputStream);

            // if file is empty or load returned null -> return empty scenario
            if (!(loaded instanceof Map)) {
                // Optional: throw an exception or return an empty Scenario
                Scenario empty = new Scenario();
                empty.setParameters(new ArrayList<>());
                return empty;
            }

            Map<String, Object> data = (Map<String, Object>) loaded;

            // determine root key: prefer "Scenario", otherwise take the first key available
            String rootKey = data.containsKey("Scenario") ? "Scenario" :
                    (data.isEmpty() ? null : data.keySet().iterator().next());

            Scenario scenario = new Scenario();
            scenario.setParameters(new ArrayList<>());

            if (rootKey == null) {
                // nothing to parse
                return scenario;
            }

            Object rootValue = data.get(rootKey);

            // if the root object is not a Map, treat it as empty (avoids ClassCastException)
            Map<String, Object> scenarioData;
            if (rootValue instanceof Map) {
                scenarioData = (Map<String, Object>) rootValue;
            } else {
                scenarioData = new LinkedHashMap<>();
            }

            for (Map.Entry<String, Object> entry : scenarioData.entrySet()) {
                parseEntity(entry.getKey(), entry.getValue(), scenario);
            }

            // Optionally keep track of which root was used:
            // if (scenario instanceof HasName) { ((HasName) scenario).setName(rootKey); }
            // (Uncomment / adapt if Scenario supports setting a name)

            return scenario;

        } catch (IOException e) {
            // FileNotFoundException is declared; wrap/convert other IOExceptions as runtime or rethrow
            throw new RuntimeException("Failed to read YAML file: " + yamlFilePath, e);
        }
    }


    @SuppressWarnings("unchecked")
    private void parseEntity(String entityName, Object entityValue, Scenario scenario) {
        if (entityValue == null) {
            return;
        }

        // Case 1: entity value is a list of parameter entries (old working case)
        if (entityValue instanceof List) {
            for (Object item : (List<?>) entityValue) {
                if (item instanceof Map) {
                    Map<String, Object> paramMap = (Map<String, Object>) item;
                    // each map in the list should have a single key, the parameter name
                    if (!paramMap.isEmpty()) {
                        String paramKey = paramMap.keySet().iterator().next();
                        Object paramValue = paramMap.get(paramKey);
                        // keep same behavior: Parameter name will be entityName_paramKey
                        parseParameter(entityName, paramKey, paramValue, scenario);
                    }
                }
            }
            return;
        }

        // Case 2: entity value is a map (more complex — may contain sub-entities or direct parameters)
        if (entityValue instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) entityValue;

            // If the map looks like simple categorical options:
            // all values are null or empty maps -> treat as categorical options under entityName
            boolean allValuesNullOrEmptyMap = true;
            for (Object v : map.values()) {
                if (v != null) {
                    if (v instanceof Map) {
                        if (!((Map<?, ?>) v).isEmpty()) {
                            allValuesNullOrEmptyMap = false;
                            break;
                        }
                    } else {
                        // found a non-null, non-map value -> not the simple categorical case
                        allValuesNullOrEmptyMap = false;
                        break;
                    }
                }
            }
            if (allValuesNullOrEmptyMap && !map.isEmpty()) {
                // create a categorical parameter for the whole entityName
                Parameter categoricalParam = new Parameter();
                categoricalParam.setName(entityName);
                categoricalParam.setType("categorical");
                categoricalParam.setOptions(new ArrayList<>(map.keySet()));
                scenario.getParameters().add(categoricalParam);
                return;
            }

            // Otherwise, iterate each sub-entry and decide how to handle it
            for (Map.Entry<String, Object> e : map.entrySet()) {
                String subKey = e.getKey();
                Object subVal = e.getValue();

                if (subVal == null) {
                    // A null sub-value - treat this as a categorical leaf: create entityName_subKey with no options
                    // (Usually this won't be useful, so we skip it. If you want a parameter with zero options, you can add it)
                    continue;
                }

                if (subVal instanceof Map) {
                    Map<String, Object> subMap = (Map<String, Object>) subVal;

                    // If subMap looks like a parameter details map (has keys like "type", "min", "max", "options", "HasConstraint")
                    if (isParameterDetailMap(subMap)) {
                        // This is a parameter definition under entityName: call parseParameter(entityName, subKey, subMap)
                        parseParameter(entityName, subKey, subMap, scenario);
                    } else {
                        // If subMap's values are null or empty -> treat it as categorical options for entityName_subKey
                        boolean subAllNullOrEmpty = true;
                        for (Object vv : subMap.values()) {
                            if (vv != null) {
                                if (vv instanceof Map) {
                                    if (!((Map<?, ?>) vv).isEmpty()) {
                                        subAllNullOrEmpty = false;
                                        break;
                                    }
                                } else {
                                    subAllNullOrEmpty = false;
                                    break;
                                }
                            }
                        }
                        if (subAllNullOrEmpty && !subMap.isEmpty()) {
                            Parameter p = new Parameter();
                            p.setName(entityName + "_" + subKey);
                            p.setType("categorical");
                            p.setOptions(new ArrayList<>(subMap.keySet()));
                            scenario.getParameters().add(p);
                        } else {
                            // deeper nested structure: recurse, propagate parent context so names keep hierarchy
                            // e.g., parseEntity("Environment_Season", subMap, scenario)
                            parseEntity(entityName + "_" + subKey, subMap, scenario);
                        }
                    }
                } else if (subVal instanceof List) {
                    // subVal is a list (e.g., EgoAC: - Latitude: { type:..., ... })
                    // Recurse, but preserve parent context so final parameter names include parent
                    parseEntity(entityName + "_" + subKey, subVal, scenario);
                } else {
                    // Primitive value (string/number) under a key — unlikely as structure but skip
                    // Optionally you could create a parameter capturing this, depending on your needs.
                }
            }

            return;
        }

        // Fallback: unknown structure — do nothing
    }

    private boolean isParameterDetailMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return false;
        // keys that indicate a parameter detail
        Set<String> indicatorKeys = new HashSet<>(Arrays.asList(
                "type", "min", "max", "options", "HasConstraint", "mean", "variance" // add any other keys you use
        ));
        for (String key : map.keySet()) {
            if (indicatorKeys.contains(key)) return true;
        }
        return false;
    }

    /**
     * Parses the details of a single parameter.
     * This method is mostly unchanged but we keep it robust to different numeric types.
     */
    @SuppressWarnings("unchecked")
    private void parseParameter(String entityName, String paramKey, Object paramValue, Scenario scenario) {
        if ("HasConstraint".equals(paramKey)) {
            if (paramValue instanceof Map) {
                Object intra = ((Map<?, ?>) paramValue).get("IntraConstraint");
                if (intra instanceof String) {
                    scenario.setConstraint((String) intra);
                }
            }
            return;
        }

        // If paramValue is a Map with details (type/min/max/options...)
        if (paramValue instanceof Map) {
            Map<String, Object> details = (Map<String, Object>) paramValue;

            // If the details indicate categorical options (e.g., options: { ... } or map of nulls)
            Object optionsObj = details.get("options");
            if (optionsObj instanceof List) {
                Parameter param = new Parameter();
                param.setName(entityName + "_" + paramKey);
                param.setType("categorical");
                param.setOptions(new ArrayList<>((List<String>) optionsObj));
                scenario.getParameters().add(param);
                return;
            }

            // Otherwise treat as numerical (if min/max exist) or as typed parameter
            Parameter param = new Parameter();
            param.setName(entityName + "_" + paramKey);

            Object typeObj = details.get("type");
            String type = typeObj instanceof String ? (String) typeObj : "numerical";
            param.setType(type);

            // numeric bounds if present
            Object minVal = details.get("min");
            if (minVal instanceof Number) {
                param.setMin(((Number) minVal).doubleValue());
            } else if (minVal instanceof String) {
                try {
                    param.setMin(Double.parseDouble((String) minVal));
                } catch (NumberFormatException ignored) {
                }
            }

            Object maxVal = details.get("max");
            if (maxVal instanceof Number) {
                param.setMax(((Number) maxVal).doubleValue());
            } else if (maxVal instanceof String) {
                try {
                    param.setMax(Double.parseDouble((String) maxVal));
                } catch (NumberFormatException ignored) {
                }
            }

            scenario.getParameters().add(param);
        }
    }
}


