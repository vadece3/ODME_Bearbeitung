package odme.sampling.distribution;

import java.util.Random;

public class DistributionSampling {

    public static double value;

    public static void main(String[] args) {
        double mean = 3.35;               // Example mean
        double stdDev = 5.12;             // Example standard deviation
        int numberOfTests = 10;           // How many test cases?

        normalDistributionSample(mean, stdDev, numberOfTests);
    }

    public static double normalDistributionSample(double mean, double stdDev, int numTests) {
        Random random = new Random();

        System.out.println("---- Rainfall Test Case Generator (Using SD Thresholds) ----");
        System.out.println("Mean (μ): " + mean);
        System.out.println("Standard Deviation (σ): " + stdDev);
        System.out.println("Requested Test Cases: " + numTests);
        System.out.println("------------------------------------------------------------");

        for (int i = 1; i <= numTests; i++) {
            value = -1;  // start with an invalid (negative) value
            int sdLevel = -1;

            // Keep generating until the variable value is >= 0
            while (value < 0) {

                sdLevel = random.nextInt(5);  // 0,1,2,3,4 → SD category

                switch (sdLevel) {
                    case 0: value = mean - stdDev; break;      // μ − σ
                    case 1: value = mean; break;               // μ
                    case 2: value = mean + stdDev; break;      // μ + σ
                    case 3: value = mean + 2 * stdDev; break;  // μ + 2σ
                    case 4: value = mean + 3 * stdDev; break;  // μ + 3σ
                }
            }

            String category = getCategory(sdLevel);

            System.out.printf("Test Case %d: Rainfall = %.1f mm  → %s%n",
                    i, value, category);
        }
        return value;
    }

    public static double uniformDistributionSample(double a, double b) {
        Random random = new Random();

        //Generate U(0,1)
        double u = random.nextDouble();       // value between 0 and 1

        //Convert to uniform in [a, b]
        double value = a + (b - a) * u;

        //print result in consol
        System.out.println("value = " + value);

        return value;
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
