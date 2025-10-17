package odme.sampling;

import odme.sampling.model.Parameter;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.RankingAlgorithm;
import org.apache.commons.math3.util.MathArrays;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates normalized samples using the Latin Hypercube Sampling (LHS) method.
 * LHS ensures that each parameter's range is evenly covered.
 * The output samples are "normalized," meaning their values are between 0.0 and 1.0.
 */
public class LatinHypercubeSampler {

    /**
     * Generates a set of normalized samples using Latin Hypercube Sampling.
     *
     * @param numberOfParameters The number of variables we are sampling (e.g., Altitude, Luminosity, etc.).
     * @param numberOfSamples    The number of sample points (test cases) to generate.
     * @return A List of arrays, where each array represents a single sample point.
     * The values in the arrays are normalized (between 0.0 and 1.0).
     */
    public List<double[]> generateNormalizedSamples(int numberOfParameters, int numberOfSamples) {
        if (numberOfParameters <= 0 || numberOfSamples <= 0) {
            return new ArrayList<>();
        }

        RandomGenerator rng = new JDKRandomGenerator();
        double[][] randomMatrix = new double[numberOfSamples][numberOfParameters];

        // 1. Generate a matrix of random numbers between 0 and 1
        for (int i = 0; i < numberOfSamples; i++) {
            for (int j = 0; j < numberOfParameters; j++) {
                randomMatrix[i][j] = rng.nextDouble();
            }
        }

        // 2. For each parameter (column), rank the random numbers
        RankingAlgorithm ranking = new NaturalRanking();
        for (int j = 0; j < numberOfParameters; j++) {
            double[] column = new double[numberOfSamples];
            for (int i = 0; i < numberOfSamples; i++) {
                column[i] = randomMatrix[i][j];
            }
            double[] ranks = ranking.rank(column);
            for (int i = 0; i < numberOfSamples; i++) {
                randomMatrix[i][j] = ranks[i];
            }
        }

        // 3. Create the final LHS samples by scaling the ranks
        // Each rank is shifted by a random value within its "bin" to ensure good distribution
        UniformRealDistribution dist = new UniformRealDistribution(rng, -0.5, 0.5);
        for (int i = 0; i < numberOfSamples; i++) {
            for (int j = 0; j < numberOfParameters; j++) {
                randomMatrix[i][j] = (randomMatrix[i][j] - 1.0 + (dist.sample() + 0.5)) / numberOfSamples;
            }
        }

        // 4. Shuffle the rows for each column independently to create the final hypercube
        List<double[]> result = new ArrayList<>(numberOfSamples);
        for (int i = 0; i < numberOfSamples; i++) {
            result.add(new double[numberOfParameters]);
        }

        // Shuffle each column independently
        for (int j = 0; j < numberOfParameters; j++) {
            List<Double> columnValues = new ArrayList<>(numberOfSamples);
            for (int i = 0; i < numberOfSamples; i++) {
                columnValues.add(randomMatrix[i][j]);
            }

            // Shuffle the values for this column
            Collections.shuffle(columnValues, new java.util.Random(rng.nextInt()));


            // Reassign shuffled values to the result
            for (int i = 0; i < numberOfSamples; i++) {
                result.get(i)[j] = columnValues.get(i);
            }
        }

        return result;
    }

    /**
     * A simple main method to test the sampler independently.
     */
    public static void main(String[] args) {
        LatinHypercubeSampler sampler = new LatinHypercubeSampler();
        int parameters = 3; // e.g., Altitude, Luminosity, Rain
        int samples = 5;

        System.out.println("Generating " + samples + " normalized samples for " + parameters + " parameters...");
        System.out.println("----------------------------------------------------------");

        List<double[]> normalizedSamples = sampler.generateNormalizedSamples(parameters, samples);

        int sampleNum = 1;
        for (double[] sample : normalizedSamples) {
            System.out.printf("Sample %d: ", sampleNum++);
            for (double value : sample) {
                // All values should be between 0.0 and 1.0
                System.out.printf("%.4f  ", value);
            }
            System.out.println();
        }
        System.out.println("----------------------------------------------------------");
    }
}
