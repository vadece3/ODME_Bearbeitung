package odme.sampling;

import odme.sampling.model.Parameter;
import odme.sampling.model.Scenario;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the entire constrained sampling process.
 * This class coordinates the other components (Parser, Sampler, Evaluator)
 * to generate a final set of valid samples and save them to a file.
 */
public class SamplingManager {

    private final ScenarioParser parser = new ScenarioParser();
    private final LatinHypercubeSampler sampler = new LatinHypercubeSampler();
    private final ConstraintEvaluator evaluator = new ConstraintEvaluator();
    private final Random random = new Random();

    /**
     * The main public method to generate constrained samples for a given scenario.
     *
     * @param yamlFilePath      The full path to the scenario's .yaml file.
     * @param numberOfSamples   The number of *valid* samples to generate.
     * @param outputCsvPath     The full path where the output .csv file will be saved.
     * @throws Exception        If any part of the process fails (e.g., file not found).
     */
    public void generateSamples(String yamlFilePath, int numberOfSamples, String outputCsvPath) throws Exception {
        // 1. Parse the YAML file to understand the sampling space
        Scenario scenario = parser.parse(yamlFilePath);
        String constraint = scenario.getConstraint();

        List<Parameter> numericalParams = scenario.getParameters().stream()
                .filter(p -> "int".equals(p.getType()) || "double".equals(p.getType() ))
                .collect(Collectors.toList());

        List<Parameter> categoricalParams = scenario.getParameters().stream()
                .filter(p -> "categorical".equals(p.getType()))
                .collect(Collectors.toList());

//        List<Parameter> constraintParams = scenario.getParameters().stream()
//                .filter(p -> p.getConstraint())
//                .collect(Collectors.toList());

        // 2. Generate all the necessary samples at once
        // This is much more efficient than generating one by one in a loop
        System.out.println("Generating base samples...");
        List<double[]> normalizedSamples = sampler.generateNormalizedSamples(numericalParams.size(), numberOfSamples);

        List<Map<String, Double>> finalSamples = new ArrayList<>();

        // If there's no constraint, all generated samples are valid.
        if (constraint == null || constraint.trim().isEmpty()) {
            System.out.println("No constraint found. Scaling all generated samples.");
            for (double[] normalizedSample : normalizedSamples) {
                Map<String, Double> scaledSample = scaleSample(normalizedSample, numericalParams);
                finalSamples.add(scaledSample);
            }
        }

        // If there IS a constraint, perform rejection sampling.
        else {
            System.out.println("Constraint found. Starting rejection sampling...");
            int attemptCount = 0;
            // Safety break to prevent infinite loops on impossible constraints
            int maxAttempts = numberOfSamples * 200;

            while (finalSamples.size() < numberOfSamples && attemptCount < maxAttempts) {
                // Generate a single new sample for this attempt
                double[] normalizedSample = sampler.generateNormalizedSamples(numericalParams.size(), 1).get(0);
                Map<String, Double> scaledSample = scaleSample(normalizedSample, numericalParams);

                // Check if the scaled sample satisfies the constraints
                if (evaluator.evaluate(constraint, scaledSample)) {
                    finalSamples.add(scaledSample);
                    System.out.printf("Found valid sample %d of %d%n", finalSamples.size(), numberOfSamples);
                }
                attemptCount++;
            }

            if (finalSamples.size() < numberOfSamples) {
                throw new RuntimeException("Could not generate the required number of valid samples. " +
                        "Only found " + finalSamples.size() + " after " + maxAttempts + " attempts. " +
                        "The constraint may be too restrictive.");
            }
        }

        // 3. Write the final results to a CSV file
        writeToCsv(finalSamples, numericalParams, categoricalParams, outputCsvPath);
        System.out.println("Successfully wrote " + numberOfSamples + " samples to " + outputCsvPath);
    }

    /**
     * Helper method to scale a normalized sample to its real-world values.
     */
    private Map<String, Double> scaleSample(double[] normalizedSample, List<Parameter> numericalParams) {
        Map<String, Double> scaledSample = new HashMap<>();
        for (int i = 0; i < numericalParams.size(); i++) {
            Parameter param = numericalParams.get(i);
            double scaledValue = param.getMin() + normalizedSample[i] * (param.getMax() - param.getMin());
            scaledSample.put(param.getName(), scaledValue);
        }
        return scaledSample;
    }

    /**
     * Writes the final list of valid samples to a CSV file.
     */

    private void writeToCsv(List<Map<String, Double>> samples,
                            List<Parameter> numericalParams,
                            List<Parameter> categoricalParams,
                            String outputCsvPath) throws IOException {

        // If no data -> skip writing
        if ((samples == null || samples.isEmpty()) && (categoricalParams == null || categoricalParams.isEmpty())) {
            System.out.println(" No samples or parameters to write!");
            return;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputCsvPath))) {
            //  Write header
            List<String> headers = new ArrayList<>();

            // Add numerical headers
            for (Parameter p : numericalParams) {
                headers.add(p.getName());
            }

            // Add categorical headers
            for (Parameter p : categoricalParams) {
                headers.add(p.getName());
            }

            writer.println(String.join(",", headers));

            //  Write sample rows
            for (Map<String, Double> sample : samples) {
                List<String> row = new ArrayList<>();

                // numerical values
                for (Parameter p : numericalParams) {
                    Double val = sample.get(p.getName());
                    row.add(val != null ? String.valueOf(val) : "");
                }

                // categorical values (for now, random option per sample)
                for (Parameter p : categoricalParams) {
                    if (p.getOptions() != null && !p.getOptions().isEmpty()) {
                        int idx = (int) (Math.random() * p.getOptions().size());
                        row.add(p.getOptions().get(idx)); // random category
                    } else {
                        row.add("");
                    }
                }

                writer.println(String.join(",", row));
            }

            writer.flush();
        }

        System.out.println(" CSV successfully written to: " + outputCsvPath);
    }

}

