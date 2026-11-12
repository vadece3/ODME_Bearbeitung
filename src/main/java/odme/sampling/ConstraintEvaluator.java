package odme.sampling;

import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.License;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates the constraint string from the scenario against a given sample of data.
 * Uses the mXparser library to safely evaluate mathematical expressions.
 *
 * Supported constraint forms (ODME syntax):
 *   if (@a > 5) then (@b < 1000) else true
 *   if (@a > 5) then (@b < 1000)                      (no else = "otherwise always valid")
 *   if (@a > 5) then (@b < 1000) else if (@a < 1) then (@b > 200) else true
 *   @a + @b <= 10                                      (plain condition, no if)
 * Logical operators: and / or / not (also &, |, ~, &&, ||). Negative numbers and
 * arithmetic with parentheses are kept unchanged.
 */
public class ConstraintEvaluator {

    static {
        // Needed once per application run.
        License.iConfirmNonCommercialUse("ODME Project - Clausthal University");
    }

    private static final Pattern AT_VARIABLE = Pattern.compile("@([A-Za-z][A-Za-z0-9_]*)");
    private static final Pattern UNDERSCORE_IDENTIFIER = Pattern.compile("\\b([A-Za-z][A-Za-z0-9]*(?:_[A-Za-z0-9]+)+)\\b");

    /**
     * Checks if a given sample of values satisfies the scenario's constraint expression.
     *
     * @param rawConstraint The constraint string, e.g., "if(@rain_intensity > 5) then (@luminosity < 1000) else true"
     * @param sampleValues  A map where the key is the parameter name (e.g., "Environment_rain_intensity")
     *                      and the value is the numerical value for this sample.
     * @return {@code true} if the sample is valid according to the constraint, {@code false} otherwise.
     * @throws IllegalArgumentException if the constraint cannot be understood (syntax error,
     *                                  unknown variable...), so the user sees the real problem
     *                                  instead of "the constraint may be too restrictive".
     */
    public boolean evaluate(String rawConstraint, Map<String, Double> sampleValues) {
        String formattedConstraint = formatExpression(rawConstraint);

        // Prepare the arguments. Names are cleaned the same way as in the expression.
        List<Argument> arguments = new ArrayList<>();
        for (Map.Entry<String, Double> entry : sampleValues.entrySet()) {
            arguments.add(new Argument(cleanName(entry.getKey()), entry.getValue()));
        }

        Expression expression = new Expression(formattedConstraint, arguments.toArray(new Argument[0]));

        // mXparser returns 1.0 for true and 0.0 for false.
        double result = expression.calculate();

        if (Double.isNaN(result)) {
            throw new IllegalArgumentException("The constraint cannot be evaluated:\n  " + rawConstraint
                    + "\nConverted to: " + formattedConstraint
                    + "\nAvailable variables: " + sampleValues.keySet()
                    + "\nDetails: " + expression.getErrorMessage());
        }

        return result != 0.0;
    }

    /**
     * Converts the ODME constraint syntax into the syntax mXparser understands.
     * Example: "if(@Environment_rain > 5) then (@Environment_lum < 1000) else true"
     *       -> "if(Environmentrain > 5, (Environmentlum < 1000), 1)"
     * Package-private so it can be tested.
     */
    static String formatExpression(String rawExpression) {
        if (rawExpression == null || rawExpression.trim().isEmpty()) {
            return "1"; // An empty constraint is considered true
        }
        String s = rawExpression.trim();

        // 1. Variable names: "@Environment_rain" -> "Environmentrain" (same cleaning as the argument names)
        Matcher m = AT_VARIABLE.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(cleanName(m.group(1))));
        }
        m.appendTail(sb);
        s = sb.toString();

        // Names written without '@' but with underscores are cleaned too
        m = UNDERSCORE_IDENTIFIER.matcher(s);
        sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(cleanName(m.group(1))));
        }
        m.appendTail(sb);
        s = sb.toString();

        // 2. Boolean words -> numbers / mXparser operators (whole words only)
        s = s.replaceAll("(?i)\\btrue\\s*\\(\\s*\\)", "1")
             .replaceAll("(?i)\\bfalse\\s*\\(\\s*\\)", "0")
             .replaceAll("(?i)\\btrue\\b", "1")
             .replaceAll("(?i)\\bfalse\\b", "0")
             .replaceAll("(?i)\\band\\b", "&")
             .replaceAll("(?i)\\bor\\b", "|")
             .replaceAll("(?i)\\bnot\\b", "~");

        // 3. if ... then ... else ...  ->  if(condition, thenPart, elsePart)
        return convertIfThenElse(s);
    }

    /** Recursively converts "if (c) then (a) else b" (also nested "else if"). */
    private static String convertIfThenElse(String s) {
        s = s.trim();
        if (!s.toLowerCase().matches("^if\\s*\\(.*")) {
            return s; // plain expression
        }
        int open = s.indexOf('(');
        int close = matchingParen(s, open);
        if (close < 0) {
            return s; // unbalanced: let mXparser report the error
        }
        String condition = s.substring(open + 1, close).trim();
        String rest = s.substring(close + 1).trim();

        // Already in mXparser form "if(c, a, b)": no "then" keyword follows
        if (!rest.toLowerCase().startsWith("then")) {
            return s;
        }
        rest = rest.substring(4).trim();

        // Split "thenPart else elsePart" at the first "else" that is not inside parentheses
        int elsePos = findTopLevelWord(rest, "else");
        String thenPart = elsePos < 0 ? rest : rest.substring(0, elsePos).trim();
        String elsePart = elsePos < 0 ? "1" : rest.substring(elsePos + 4).trim();

        return "if(" + condition + ", " + convertIfThenElse(thenPart) + ", " + convertIfThenElse(elsePart) + ")";
    }

    private static int matchingParen(String s, int openIndex) {
        int depth = 0;
        for (int i = openIndex; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static int findTopLevelWord(String s, String word) {
        int depth = 0;
        String lower = s.toLowerCase();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (depth == 0 && lower.startsWith(word, i)) {
                boolean startOk = i == 0 || !Character.isLetterOrDigit(s.charAt(i - 1));
                int end = i + word.length();
                boolean endOk = end >= s.length() || !Character.isLetterOrDigit(s.charAt(end));
                if (startOk && endOk) return i;
            }
        }
        return -1;
    }

    /** Removes everything except letters and digits: "Environment_rain_intensity" -> "Environmentrainintensity". */
    private static String cleanName(String name) {
        return name.replaceAll("[^A-Za-z0-9]", "");
    }

    /**
     * A simple main method to test the evaluator independently.
     */
    public static void main(String[] args) {
        ConstraintEvaluator evaluator = new ConstraintEvaluator();
        String constraint = "if(@Environment_rain_intensity > 5) then (@Environment_luminosity < 1000) else true";
        System.out.println("Constraint to test: " + constraint);
        System.out.println("Converted: " + formatExpression(constraint));
        System.out.println("------------------------------------------");

        check(evaluator, constraint, 8.0, 500.0, true);    // rain > 5, luminosity < 1000
        check(evaluator, constraint, 8.0, 1500.0, false);  // rain > 5, but luminosity >= 1000
        check(evaluator, constraint, 3.0, 1500.0, true);   // rain <= 5 -> else true

        // Negative numbers are now kept (the old version turned "-5" into "5")
        String negative = "if(@Environment_rain_intensity > -5) then (@Environment_luminosity < 1000) else false";
        System.out.println("Converted: " + formatExpression(negative));
        check(evaluator, negative, -3.0, 500.0, true);
        check(evaluator, negative, -8.0, 500.0, false);

        // Plain condition without if (the old version rejected every sample)
        String plain = "@Environment_rain_intensity + @Environment_luminosity / 1000 <= 6";
        System.out.println("Converted: " + formatExpression(plain));
        check(evaluator, plain, 4.0, 1500.0, true);
        check(evaluator, plain, 6.0, 1500.0, false);

        // Nested else if
        String nested = "if (@Environment_rain_intensity > 5) then (@Environment_luminosity < 1000) "
                + "else if (@Environment_rain_intensity < 1) then (@Environment_luminosity > 200) else true";
        System.out.println("Converted: " + formatExpression(nested));
        check(evaluator, nested, 0.5, 100.0, false);
        check(evaluator, nested, 0.5, 300.0, true);
        System.out.println("------------------------------------------");
    }

    private static void check(ConstraintEvaluator e, String c, double rain, double lum, boolean expected) {
        boolean result = e.evaluate(c, Map.of("Environment_rain_intensity", rain, "Environment_luminosity", lum));
        System.out.printf("rain=%.1f, lum=%.1f -> %s (expected %s) %s%n",
                rain, lum, result, expected, result == expected ? "OK" : "<-- WRONG");
    }
}
