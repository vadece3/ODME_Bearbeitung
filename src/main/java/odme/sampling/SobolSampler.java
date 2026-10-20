package odme.sampling;

import org.apache.commons.math3.random.SobolSequenceGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates normalized samples using a Sobol sequence (quasi-random, low-discrepancy).
 * Unlike LHS, the sequence is deterministic: running it twice gives the same points.
 * The output samples are "normalized", meaning their values are between 0.0 and 1.0.
 *
 * The sampler keeps its position in the sequence, so successive calls to
 * nextNormalizedSample() continue the sequence instead of restarting it.
 * This is needed for rejection sampling, where points are drawn one by one.
 */
public class SobolSampler {

    private SobolSequenceGenerator generator;
    private int dimension = -1;

    /**
     * Generates a set of normalized samples from a fresh Sobol sequence.
     *
     * @param numberOfParameters The number of variables we are sampling.
     * @param numberOfSamples    The number of sample points to generate.
     * @return A List of arrays, each array is one sample point with values in [0, 1).
     */
    public List<double[]> generateNormalizedSamples(int numberOfParameters, int numberOfSamples) {
        List<double[]> result = new ArrayList<>();
        if (numberOfParameters <= 0 || numberOfSamples <= 0) {
            return result;
        }
        reset(numberOfParameters);
        for (int i = 0; i < numberOfSamples; i++) {
            result.add(nextNormalizedSample(numberOfParameters));
        }
        return result;
    }

    /**
     * Returns the next point of the Sobol sequence.
     * Starts a new sequence if none exists yet or the number of parameters changed.
     */
    public double[] nextNormalizedSample(int numberOfParameters) {
        if (numberOfParameters <= 0) {
            return new double[0];
        }
        if (generator == null || dimension != numberOfParameters) {
            reset(numberOfParameters);
        }
        return generator.nextVector();
    }

    /**
     * Restarts the Sobol sequence from the beginning.
     * The very first Sobol point is the corner (0, 0, ..., 0); it is skipped
     * because it is a degenerate point that would always sit on the minimum of every range.
     */
    public void reset(int numberOfParameters) {
        generator = new SobolSequenceGenerator(numberOfParameters);
        dimension = numberOfParameters;
        generator.nextVector(); // skip the origin point
    }

    /**
     * A simple main method to test the sampler independently.
     */
    public static void main(String[] args) {
        SobolSampler sampler = new SobolSampler();
        int parameters = 3; // e.g., Altitude, Luminosity, Rain
        int samples = 8;

        System.out.println("Generating " + samples + " Sobol samples for " + parameters + " parameters...");
        System.out.println("----------------------------------------------------------");

        int sampleNum = 1;
        for (double[] sample : sampler.generateNormalizedSamples(parameters, samples)) {
            System.out.printf("Sample %d: ", sampleNum++);
            for (double value : sample) {
                System.out.printf("%.4f  ", value);
            }
            System.out.println();
        }
        System.out.println("----------------------------------------------------------");
    }
}
