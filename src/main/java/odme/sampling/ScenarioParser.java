package odme.sampling;

import odme.sampling.model.Parameter;
import odme.sampling.model.Scenario;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses a scenario's .yaml file into a structured Scenario object.
 */
public class ScenarioParser {

    public Scenario parse(String yamlFilePath) throws FileNotFoundException {
        Yaml yaml = new Yaml();
        InputStream inputStream = new FileInputStream(yamlFilePath);
        Map<String, Object> data = yaml.load(inputStream);

        Scenario scenario = new Scenario();
        scenario.setParameters(new ArrayList<>());

        Map<String, Object> scenarioData = (Map<String, Object>) data.get("Scenario");

        for (Map.Entry<String, Object> entry : scenarioData.entrySet()) {
            parseEntity(entry.getKey(), entry.getValue(), scenario);
        }

        return scenario;
    }

    private void parseEntity(String entityName, Object entityValue, Scenario scenario) {
        if (entityValue == null) {
            return;
        }

        if (entityValue instanceof List) {
            // Handles entities like 'Environment' which contain a list of parameters.
            for (Object item : (List<?>) entityValue) {
                if (item instanceof Map) {
                    // Each item in the list is a Map, e.g., {- rain_intensity: {...}}
                    Map<String, Object> paramMap = (Map<String, Object>) item;
                    String paramKey = paramMap.keySet().iterator().next();
                    Object paramValue = paramMap.get(paramKey);

                    parseParameter(entityName, paramKey, paramValue, scenario);
                }
            }
        } else if (entityValue instanceof Map) {
            // This handles categorical parameters.
            Parameter categoricalParam = new Parameter();
            categoricalParam.setName(entityName);
            categoricalParam.setType("categorical");
            categoricalParam.setOptions(new ArrayList<>(((Map<String, Object>) entityValue).keySet()));
            scenario.getParameters().add(categoricalParam);
        }
    }

    /**
     * Parses the details of a single parameter.
     */
    private void parseParameter(String entityName, String paramKey, Object paramValue, Scenario scenario) {
        if ("HasConstraint".equals(paramKey)) {
            if (paramValue instanceof Map) {
                scenario.setConstraint((String) ((Map<?, ?>) paramValue).get("IntraConstraint"));
            }
            return;
        }

        if (paramValue instanceof Map) {
            Parameter param = new Parameter();
            param.setName(entityName + "_" + paramKey);
            param.setType("numerical");

            Map<String, Object> details = (Map<String, Object>) paramValue;

            Object minVal = details.get("min");
            if (minVal instanceof Number) {
                param.setMin(((Number) minVal).doubleValue());
            }

            Object maxVal = details.get("max");
            if (maxVal instanceof Number) {
                param.setMax(((Number) maxVal).doubleValue());
            }

            scenario.getParameters().add(param);
        }
    }
}

