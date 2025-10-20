package odme.odmeeditor;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScenarioGeneration {

    public static int fileExistValidator = 0;
    public static String scenarioName = null;

    /**
     * This function gets the CSV file which contains the sampled data.
     * It reads the CSV file and gets all information to transform into scenarios.
     * Multiple Scenarois here will be generated at once.
     * The number of generated scenarois depends on the number of rows present in the CSV file
     * */
    public static String generateScenarios ( String csvPath, String nameScenarioList) {

        scenarioName = nameScenarioList;
        String pathParent = ODMEEditor.fileLocation + "/Scenarios/" + nameScenarioList ;
        File folder = new File(pathParent);

        // Check if folder exists
        if (folder.exists()) {
            fileExistValidator = 1;
            String newPathParent = ODMEEditor.fileLocation + "/Scenarios/" + nameScenarioList + "1" ;
            folder = new File(newPathParent);
        }

        // Try to create it
        boolean created = folder.mkdirs(); // use mkdirs() to also create parent folders
        if (created) {
            System.out.println("Folder created: " + folder.getAbsolutePath());
        } else {
//            System.out.println("Failed to create folder: " + folder.getAbsolutePath());
            fileExistValidator = 0;
            return "Failed to create folder: " + folder.getAbsolutePath()+"\n" +
                    ">>Restart the Process and Enter a NEW Scenario Name<<";
        }
        return importScenarioDatasFromCSVFile( csvPath , folder.getAbsolutePath() );

    }

    public static String importScenarioDatasFromCSVFile(String csvFilePath, String outputDirectory) {
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            // Read header (titles)
            String headerLine = br.readLine();
            if (headerLine == null) {
                System.out.println("CSV is empty!");
                return "CSV is empty!";
            }

            String[] headers = headerLine.split(",");

            // Ensure output folder exists
            File outputDir = new File(outputDirectory);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            String line;
            int fileCount = 1;

            // Read each data line
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] values = line.split(",");
                Map<String, String> data = new LinkedHashMap<>();

                for (int i = 0; i < headers.length && i < values.length; i++) {
                    data.put(headers[i].trim(), values[i].trim());
                }

                // Generate XML content for this line
                String xmlContent = buildXMLScenarioContent(data);

                // Create and write file
                String fileName = "Scenario_" + fileCount + ".xml";
                File outputFile = new File(outputDir, fileName);

                try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
                    writer.write(xmlContent);
                }

//                System.out.println("Created file: " + outputFile.getAbsolutePath());
                fileCount++;
            }

            String result = "Files saved in " + outputDirectory;
            if (fileExistValidator == 1){
                result = "Files saved in " + outputDirectory +
                        "\n NB: \n- The Scenario name " + scenarioName + " you entered already exist.\n" +
                        "- The new Scenario Name is "
                                + scenarioName + "1. \n" +
                        "- Next time use a different Scenario name";
                fileExistValidator = 0;
                return result;
            }
            fileExistValidator = 0;
            return result;

        } catch (IOException e) {
            fileExistValidator = 0;
//            System.err.println("Error: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }



    private static String buildXMLScenarioContent(Map<String, String> data) {
        StringBuilder xml = new StringBuilder();

        // XML header
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
        xml.append("<entity xmlns:vc=\"http://www.w3.org/2007/XMLSchema-versioning\" ")
                .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
                .append("xsi:noNamespaceSchemaLocation=\"ses.xsd\" name=\"Scenario\">\n");
        xml.append("<aspect name=\"scenarioDec\">\n");

        // Group all parameters by their entity prefix
        Map<String, List<Map.Entry<String, String>>> entityGroups = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = entry.getKey();
            if (!key.contains("_")) continue; // Skip invalid format

            // Split "EgoAC_Altitude" → ["EgoAC", "Altitude"]
            String[] parts = key.split("_", 2);
            String entityName = parts[0].trim();
            String varName = parts[1].trim();

            entityGroups
                    .computeIfAbsent(entityName, k -> new ArrayList<>())
                    .add(Map.entry(varName, entry.getValue().trim()));
        }

        // Build XML for each entity group
        for (Map.Entry<String, List<Map.Entry<String, String>>> entity : entityGroups.entrySet()) {
            String entityName = entity.getKey();
            xml.append("<entity name=\"").append(entityName).append("\">\n");

            for (Map.Entry<String, String> variable : entity.getValue()) {
                String varName = variable.getKey();
                String value = variable.getValue();

                xml.append("<var name=\"")
                        .append(varName)
                        .append("\" default=\"")
                        .append(value)
                        .append("\"> </var>\n");
            }

            xml.append("</entity>\n");
        }

        // Close XML structure
        xml.append("</aspect>\n</entity>\n");

        return xml.toString();
    }
}
