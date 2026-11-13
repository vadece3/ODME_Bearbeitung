package odme.sampling;

import odme.sampling.model.Parameter;
import odme.sampling.model.Scenario;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the entire constrained sampling process.
 * This class coordinates the other components (Parser, Sampler, Evaluator)
 * to generate a final set of valid samples and save them to a file.
 *
 * How one sample is built:
 *  1. The sampler (LHS or Sobol) gives a point u in [0,1)^d, one coordinate per parameter
 *     (numerical parameters first, then categorical ones).
 *  2. Each coordinate is mapped to a real value:
 *       - range parameter:         min + u * (max - min)   (integers: whole numbers min..max)
 *       - normal distribution:     inverse normal CDF of u (cut to [min, max] if a range is given)
 *       - uniform distribution:    a + u * (b - a)
 *       - categorical parameter:   option number floor(u * numberOfOptions)
 *  3. If the YAML has constraints, the sample is kept only if it satisfies ALL of them.
 * Because every value comes from the design point, the stratification (LHS) or
 * low discrepancy (Sobol) applies to all parameters, including distributions and categories.
 */
public class SamplingManager {

    private final ScenarioParser parser = new ScenarioParser();
    private final LatinHypercubeSampler sampler = new LatinHypercubeSampler();
    private final SobolSampler sobolSampler = new SobolSampler();
    private final ConstraintEvaluator evaluator = new ConstraintEvaluator();

    /** Names of the available sampling techniques (shown in the Generate Samples window). */
    public static final String TECHNIQUE_LHS = "Latin Hypercube Sampling (LHS)";
    public static final String TECHNIQUE_SOBOL = "Sobol Sequence";

    /** Seed actually used by the last run (useful to reproduce it). */
    private long lastSeed;

    public long getLastSeed() {
        return lastSeed;
    }

    // ------------------------------------------------------------------------------------
    // Public entry points
    // ------------------------------------------------------------------------------------

    /**
     * Generates constrained samples for a given scenario with LHS and a random seed.
     *
     * @param yamlFilePath      The full path to the scenario's .yaml file.
     * @param numberOfSamples   The number of *valid* samples to generate.
     * @param outputCsvPath     The full path where the output .csv file will be saved.
     */
    public void generateSamples(String yamlFilePath, int numberOfSamples, String outputCsvPath) throws Exception {
        generateSamples(yamlFilePath, numberOfSamples, outputCsvPath, TECHNIQUE_LHS, null);
    }

    /** Same as above, with a chosen technique (TECHNIQUE_LHS or TECHNIQUE_SOBOL) and a random seed. */
    public void generateSamples(String yamlFilePath, int numberOfSamples, String outputCsvPath, String technique) throws Exception {
        generateSamples(yamlFilePath, numberOfSamples, outputCsvPath, technique, null);
    }

    /**
     * Same as above, with a chosen technique and seed.
     *
     * @param seed the random seed for LHS; null = pick a random seed (it is printed so the run can be repeated).
     *             Sobol is deterministic and does not use the seed.
     */
    public void generateSamples(String yamlFilePath, int numberOfSamples, String outputCsvPath,
                                String technique, Long seed) throws Exception {
        run(yamlFilePath, numberOfSamples, outputCsvPath, TECHNIQUE_SOBOL.equals(technique), seed);
    }

    /** Sampling used by "Generate Scenario > Direct From Domain Model" (LHS, random seed). */
    public void generateSamplesforDomainModel(String yamlFilePath, int numberOfSamples, String outputCsvPath) throws Exception {
        generateSamplesforDomainModel(yamlFilePath, numberOfSamples, outputCsvPath, TECHNIQUE_LHS, null);
    }

    /** Domain-model sampling with a chosen technique (random seed). */
    public void generateSamplesforDomainModel(String yamlFilePath, int numberOfSamples, String outputCsvPath,
                                              String technique) throws Exception {
        generateSamplesforDomainModel(yamlFilePath, numberOfSamples, outputCsvPath, technique, null);
    }

    /** Domain-model sampling with a chosen technique and seed. */
    public void generateSamplesforDomainModel(String yamlFilePath, int numberOfSamples, String outputCsvPath,
                                              String technique, Long seed) throws Exception {
        System.out.println("Sampling from the domain model");
        run(yamlFilePath, numberOfSamples, outputCsvPath, TECHNIQUE_SOBOL.equals(technique), seed);
    }

    // ------------------------------------------------------------------------------------
    // The sampling run
    // ------------------------------------------------------------------------------------

    private void run(String yamlFilePath, int numberOfSamples, String outputCsvPath,
                     boolean useSobol, Long seed) throws Exception {
        System.out.println("Sampling technique: " + (useSobol ? TECHNIQUE_SOBOL : TECHNIQUE_LHS));

        // Seed: always set one, so every LHS run can be reproduced
        lastSeed = (seed != null) ? seed : new Random().nextLong();
        if (useSobol) {
            System.out.println("Sobol is deterministic: the seed is not used.");
        } else {
            sampler.setSeed(lastSeed);
            System.out.println("Seed used: " + lastSeed + "  (enter it again to reproduce exactly this run)");
        }

        // 1. Parse the YAML file to understand the sampling space
        Scenario scenario = parser.parse(yamlFilePath);
        List<String> constraints = scenario.getConstraint();

        List<Parameter> numericalParams = scenario.getParameters().stream()
                .filter(p -> "int".equals(p.getType()) || "double".equals(p.getType())
                        || "float".equals(p.getType()) || "distribution".equals(p.getType()))
                .collect(Collectors.toList());

        List<Parameter> categoricalParams = scenario.getParameters().stream()
                .filter(p -> "categorical".equals(p.getType()))
                .collect(Collectors.toList());

        System.out.println("Numerical parameters: " + numericalParams.size()
                + ", categorical parameters: " + categoricalParams.size()
                + ", constraints: " + (constraints == null ? 0 : constraints.size()));

        // 2. Draw the samples (with rejection if there are constraints)
        List<Map<String, Double>> finalSamples =
                drawSamples(numericalParams, categoricalParams, constraints, numberOfSamples, useSobol);

        // 3. Write the final results to a CSV file
        writeToCsv(finalSamples, numericalParams, categoricalParams, outputCsvPath);
        System.out.println("Successfully wrote " + finalSamples.size() + " samples to " + outputCsvPath);
    }

    /**
     * Draws candidate points and keeps those that satisfy ALL constraints, each exactly once.
     * Without constraints every candidate is kept.
     *
     * LHS:   candidates come in full Latin Hypercube batches of numberOfSamples points,
     *        so every batch is stratified.
     * Sobol: candidates are the consecutive points of one Sobol sequence.
     */
    private List<Map<String, Double>> drawSamples(List<Parameter> numericalParams,
                                                  List<Parameter> categoricalParams,
                                                  List<String> constraints,
                                                  int numberOfSamples,
                                                  boolean useSobol) {
        int dimensions = numericalParams.size() + categoricalParams.size();
        boolean constrained = constraints != null && !constraints.isEmpty();

        List<Map<String, Double>> accepted = new ArrayList<>();
        int attemptCount = 0;
        // Safety break to prevent infinite loops on impossible constraints
        int maxAttempts = constrained ? numberOfSamples * 200 : numberOfSamples;

        if (useSobol) {
            sobolSampler.reset(Math.max(1, dimensions));
        }
        if (constrained) {
            System.out.println("Constraint(s) found: " + constraints.size() + ". Starting rejection sampling...");
        }

        while (accepted.size() < numberOfSamples && attemptCount < maxAttempts) {
            List<double[]> candidates;
            if (useSobol) {
                candidates = new ArrayList<>();
                candidates.add(sobolSampler.nextNormalizedSample(dimensions));
            } else {
                candidates = sampler.generateNormalizedSamples(dimensions, numberOfSamples);
                if (candidates.isEmpty()) {
                    candidates.add(new double[0]); // no parameters at all
                }
            }

            for (double[] candidate : candidates) {
                if (accepted.size() >= numberOfSamples || attemptCount >= maxAttempts) {
                    break;
                }
                attemptCount++;
                Map<String, Double> sample = scaleSample(candidate, numericalParams, categoricalParams);
                if (!constrained || satisfiesAllConstraints(constraints, sample)) {
                    accepted.add(sample);
                }
            }
        }

        if (constrained) {
            System.out.println("Rejection sampling: " + accepted.size() + " accepted out of "
                    + attemptCount + " candidates.");
        }

        if (accepted.size() < numberOfSamples) {
            throw new RuntimeException("Could not generate the required number of valid samples. " +
                    "Only found " + accepted.size() + " after " + maxAttempts + " attempts. " +
                    "The constraint may be too restrictive.");
        }
        return accepted;
    }

    /** A sample is valid only if it satisfies every constraint (logical AND). */
    private boolean satisfiesAllConstraints(List<String> constraints, Map<String, Double> sample) {
        for (String c : constraints) {
            if (!evaluator.evaluate(c, sample)) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------------------
    // Mapping a design point to real values
    // ------------------------------------------------------------------------------------

    /**
     * Turns a normalized design point into real values.
     * Numerical parameters use coordinates 0..n-1, categorical parameters the following ones.
     * Categorical values are stored as the index of the chosen option (0, 1, 2, ...).
     */
    private Map<String, Double> scaleSample(double[] u,
                                            List<Parameter> numericalParams,
                                            List<Parameter> categoricalParams) {
        Map<String, Double> sample = new LinkedHashMap<>();

        for (int i = 0; i < numericalParams.size(); i++) {
            Parameter param = numericalParams.get(i);
            sample.put(param.getName(), scaleNumerical(param, u[i]));
        }

        int offset = numericalParams.size();
        for (int j = 0; j < categoricalParams.size(); j++) {
            Parameter param = categoricalParams.get(j);
            List<String> options = param.getOptions();
            int k = (options == null) ? 0 : options.size();
            double index = -1;
            if (k > 0) {
                index = Math.min(k - 1, (int) Math.floor(u[offset + j] * k));
            }
            sample.put(param.getName(), index);
        }
        return sample;
    }

    private double scaleNumerical(Parameter param, double u) {
        String distribution = param.getDistributionName();
        String details = param.getDistributionDetails();

        if (details != null && distribution != null) {
            Map<String, Double> d = parseDistributionDetails(details);

            if (distribution.equals("normalDistribution")) {
                double mean = firstValue(d, 0, param);
                double std = firstValue(d, 1, param);
                if (std <= 0) {
                    return mean;
                }
                NormalDistribution normal = new NormalDistribution(mean, std);
                double p = u;
                // If min and/or max are defined, keep the value inside them (truncated normal),
                // e.g. min = 0 for a rain intensity that can never be negative
                double lo = (param.getMin() != null) ? normal.cumulativeProbability(param.getMin()) : 0.0;
                double hi = (param.getMax() != null) ? normal.cumulativeProbability(param.getMax()) : 1.0;
                if (hi > lo) {
                    p = lo + u * (hi - lo);
                }
                p = Math.min(1 - 1e-12, Math.max(1e-12, p));
                return roundIfInt(param, normal.inverseCumulativeProbability(p));
            }

            if (distribution.equals("uniformDistribution")) {
                double a = firstValue(d, 0, param);
                double b = firstValue(d, 1, param);
                return roundIfInt(param, a + u * (b - a));
            }

            throw new IllegalArgumentException("Unknown distribution '" + distribution
                    + "' for parameter " + param.getName());
        }

        if (param.getMin() == null || param.getMax() == null) {
            throw new IllegalArgumentException("Parameter " + param.getName()
                    + " has no min/max range and no distribution, so it cannot be sampled.");
        }
        double min = param.getMin();
        double max = param.getMax();

        if ("int".equals(param.getType())) {
            // Every whole number from min to max gets the same share of [0,1)
            long lo = (long) Math.ceil(min);
            long hi = (long) Math.floor(max);
            long value = lo + (long) Math.floor(u * (hi - lo + 1));
            return Math.min(hi, value);
        }
        return min + u * (max - min);
    }

    private double roundIfInt(Parameter param, double value) {
        return "int".equals(param.getType()) ? Math.round(value) : value;
    }

    /**
     * Reads distribution details written as "key1=value1___key2=value2"
     * (e.g. "mean=3.35___stdDeviation=5.12" or "min=0___max=10"), keeping their order.
     */
    private Map<String, Double> parseDistributionDetails(String details) {
        Map<String, Double> values = new LinkedHashMap<>();
        for (String part : details.split("___")) {
            String[] kv = part.split("=");
            if (kv.length == 2) {
                values.put(kv[0].trim(), Double.parseDouble(kv[1].trim()));
            }
        }
        return values;
    }

    private double firstValue(Map<String, Double> values, int position, Parameter param) {
        if (values.size() <= position) {
            throw new IllegalArgumentException("Distribution details of " + param.getName()
                    + " are incomplete: " + param.getDistributionDetails());
        }
        return new ArrayList<>(values.values()).get(position);
    }

    // ------------------------------------------------------------------------------------
    // CSV output
    // ------------------------------------------------------------------------------------

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
            for (Parameter p : numericalParams) {
                headers.add(p.getName());
            }
            for (Parameter p : categoricalParams) {
                headers.add(p.getName());
            }
            writer.println(String.join(",", headers));

            //  Write sample rows
            for (Map<String, Double> sample : samples) {
                List<String> row = new ArrayList<>();

                for (Parameter p : numericalParams) {
                    Double val = sample.get(p.getName());
                    if (val == null) {
                        row.add("");
                    } else if ("int".equals(p.getType())) {
                        row.add(String.valueOf(val.longValue()));
                    } else {
                        row.add(String.valueOf(val));
                    }
                }

                for (Parameter p : categoricalParams) {
                    Double index = sample.get(p.getName());
                    if (index == null || index < 0 || p.getOptions() == null) {
                        row.add("");
                    } else {
                        row.add(p.getOptions().get(index.intValue()));
                    }
                }

                writer.println(String.join(",", row));
            }

            writer.flush();
        }

        System.out.println(" CSV successfully written to: " + outputCsvPath);
    }

}
