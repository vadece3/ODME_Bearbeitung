package odme.sampling;

import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.License;
import org.mariuszgromada.math.mxparser.mXparser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates the constraint string from the scenario against a given sample of data.
 * Uses the mXparser library to safely evaluate mathematical expressions.
 */
public class ConstraintEvaluator {

    /**
     * Checks if a given sample of values satisfies the scenario's constraint expression.
     *
     * @param rawConstraint The constraint string, e.g., "if(@rain_intensity > 5) then (@luminosity < 1000) else true"
     * @param sampleValues  A map where the key is the parameter name (e.g., "Environment_rain_intensity")
     * and the value is the numerical value for this sample.
     * @return {@code true} if the sample is valid according to the constraint, {@code false} otherwise.
     */
    public boolean evaluate(String rawConstraint, Map<String, Double> sampleValues) {
        // This only needs to be called once per application run, but it's safe here.
        License.iConfirmNonCommercialUse("ODME Project - Clausthal University");

        // Prepare the arguments for the expression. The names must not contain special chars.
        List<Argument> arguments = new ArrayList<>();
        for (Map.Entry<String, Double> entry : sampleValues.entrySet()) {
            String cleanName = entry.getKey().replaceAll("[@_()-]", "");
            arguments.add(new Argument(cleanName, entry.getValue()));
        }

        // Format the constraint string into a syntax that mXparser understands
        String formattedConstraint = formatExpression(rawConstraint);
        if (formattedConstraint == null) {
            System.err.println("Could not parse the constraint structure: " + rawConstraint);
            return false; // Or handle as a critical error
        }

        // Create the expression and add the arguments
        Expression expression = new Expression(formattedConstraint, arguments.toArray(new Argument[0]));

        // Calculate the result. mXparser returns 1.0 for true and 0.0 for false.
        double result = expression.calculate();

        if (Double.isNaN(result)) {
            System.err.println("Syntax Error in constraint expression: " + expression.getErrorMessage());
            System.err.println("Formatted expression was: " + formattedConstraint);
            return false;
        }

        return result == 1.0;
    }

    /**
     * Converts the ODME constraint syntax "if(condition) then (result1) else result2"
     * into the mXparser format "if(condition, result1, result2)".
     *
     * @param rawExpression The raw constraint string from the YAML file.
     * @return A formatted string suitable for mXparser, or null if the structure is invalid.
     */
    private String formatExpression(String rawExpression) {
        if (rawExpression == null || rawExpression.trim().isEmpty()) {
            return "1"; // An empty constraint is considered true
        }

        // Regex to capture the three main parts of the if-then-else statement
        Pattern pattern = Pattern.compile("if\\s*\\((.*)\\)\\s*then\\s*\\((.*)\\)\\s*else\\s*(.*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(rawExpression);

        if (matcher.find()) {
            String condition = matcher.group(1);
            String thenPart = matcher.group(2);
            String elsePart = matcher.group(3);

            // Clean each part and reassemble in the correct format for mXparser
            String formatted = String.format("if(%s, %s, %s)",
                    cleanPart(condition),
                    cleanPart(thenPart),
                    cleanPart(elsePart));
            return formatted;
        }

        return null; // Return null if the pattern doesn't match
    }

    /**
     * Helper method to clean a part of the expression for mXparser.
     */
    private String cleanPart(String part) {
        return part.trim()
                .replaceAll("[@_()-]", "") // Remove special characters from variable names
                .replaceAll("true", "1")
                .replaceAll("false", "0");
    }


    /**
     * A simple main method to test the evaluator independently.
     */
    public static void main(String[] args) {
        ConstraintEvaluator evaluator = new ConstraintEvaluator();
        // NOTE: Corrected the test constraint to match the required structure
        String constraint = "if(@Environment_rain_intensity > 5) then (@Environment_luminosity < 1000) else true";

        System.out.println("Constraint to test: " + constraint);
        System.out.println("------------------------------------------");

        // Test Case 1: Should be VALID (rain > 5, luminosity < 1000)
        Map<String, Double> sample1 = Map.of(
                "Environment_rain_intensity", 8.0,
                "Environment_luminosity", 500.0
        );
        boolean result1 = evaluator.evaluate(constraint, sample1);
        System.out.println("Test Case 1 (rain=8, lum=500): Result = " + result1 + " (Expected: true)");

        // Test Case 2: Should be INVALID (rain > 5, but luminosity >= 1000)
        Map<String, Double> sample2 = Map.of(
                "Environment_rain_intensity", 8.0,
                "Environment_luminosity", 1500.0
        );
        boolean result2 = evaluator.evaluate(constraint, sample2);
        System.out.println("Test Case 2 (rain=8, lum=1500): Result = " + result2 + " (Expected: false)");

        // Test Case 3: Should be VALID (rain <= 5, the 'else true' part is triggered)
        Map<String, Double> sample3 = Map.of(
                "Environment_rain_intensity", 3.0,
                "Environment_luminosity", 1500.0
        );
        boolean result3 = evaluator.evaluate(constraint, sample3);
        System.out.println("Test Case 3 (rain=3, lum=1500): Result = " + result3 + " (Expected: true)");
        System.out.println("------------------------------------------");
    }
}