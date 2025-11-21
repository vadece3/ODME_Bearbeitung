package odme.sampling.distribution;

import java.util.Random;

public class DistributionSampling {

    public static void main(String[] args) {
        double mean = 3.35;               // Example mean
        double stdDev = 5.12;             // Example standard deviation
        int numberOfTests = 10;           // How many test cases?

        generateTestCases(mean, stdDev, numberOfTests);
    }

    public static void generateTestCases(double mean, double stdDev, int numTests) {
        Random random = new Random();

        System.out.println("---- Rainfall Test Case Generator (Using SD Thresholds) ----");
        System.out.println("Mean (μ): " + mean);
        System.out.println("Standard Deviation (σ): " + stdDev);
        System.out.println("Requested Test Cases: " + numTests);
        System.out.println("------------------------------------------------------------");

        for (int i = 1; i <= numTests; i++) {
            // Pick a random standard deviation multiplier
            int sdLevel = random.nextInt(5);   // 0,1,2,3,4

            double thresholdValue = 0;

            switch (sdLevel) {
                case 0:  thresholdValue = mean - stdDev; break;      // μ − σ
                case 1:  thresholdValue = mean; break;               // μ
                case 2:  thresholdValue = mean + stdDev; break;      // μ + σ
                case 3:  thresholdValue = mean + 2 * stdDev; break;  // μ + 2σ
                case 4:  thresholdValue = mean + 3 * stdDev; break;  // μ + 3σ
            }

            String category = getCategory(sdLevel);

            System.out.printf("Test Case %d: Rainfall = %.3f mm  → %s%n",
                    i, thresholdValue, category);
        }
    }

    private static String getCategory(int sdLevel) {
        switch (sdLevel) {
            case 0:  return "Low Rain (μ - σ)";
            case 1:  return "Normal (μ)";
            case 2:  return "High Rain (μ + σ)";
            case 3:  return "Very High Rain (μ + 2σ)";
            case 4:  return "Extreme Rain (μ + 3σ)";
            default: return "Unknown";
        }
    }
}
