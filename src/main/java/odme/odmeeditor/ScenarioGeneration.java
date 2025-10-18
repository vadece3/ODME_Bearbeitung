package odme.odmeeditor;

import java.io.*;
import java.util.Scanner;

public class ScenarioGeneration {

    public static String[] rawScenarioList = null;
    public static String currentNewScenario = "NewScenarios";

    /**
     * This function gets the CSV file which contains the sampled data.
     * It reads the CSV file and gets all information to transform into scenarios.
     * Multiple Scenarois here will be generated at once.
     * The number of generated scenarois depends on the number of rows present in the CSV file
     * */
    public static void generateScenarios ( String csvPath, String nameScenarioList) {

        String pathParent = ODMEEditor.fileLocation + "/Scenarios/" + nameScenarioList ;
        File folder = new File(pathParent);

        // Check if folder exists
        if (folder.exists()) {
            String newPathParent = ODMEEditor.fileLocation + "/Scenarios/" + nameScenarioList + "1" ;
            folder = new File(newPathParent);
        }

        // Try to create it
        boolean created = folder.mkdirs(); // use mkdirs() to also create parent folders
        if (created) {
            System.out.println("Folder created: " + folder.getAbsolutePath());
        } else {
            System.out.println("Failed to create folder: " + folder.getAbsolutePath());
        }

        for (int index = 0 ; index == rawScenarioList.length ; index++) {
            try {
                String path = pathParent + "/" + nameScenarioList + index + ".xml";
                File file = new File("example.txt");
                file.createNewFile();
                xmlOutput(path,index);
            } catch (IOException e) {
                System.out.println("An error occurred while creating the new file.");
                e.printStackTrace();
            }
        }

    }


    // fix this file according to the generated result from Sampling
    public static void xmlOutput( String path , int scenarioIndexNumber) {
        PrintWriter f0 = null;
        try {
            f0 = new PrintWriter(
                    new FileWriter(path));
        }
        catch (IOException e1) {
            e1.printStackTrace();
        }


        String[] scenarioValues = rawScenarioList[scenarioIndexNumber].split(",");

        Scanner in = null;
        int first = 0;
        f0.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
        f0.println("<entity xmlns:vc=\"http://www.w3.org/2007/XMLSchema-versioning\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:noNamespaceSchemaLocation=\"ses.xsd\" name=\"" + scenarioValues[0] + "\">");

        for (String result: scenarioValues) { // Iterates each value in the list
            String mod = null;

            //to open the tag
            if (result.endsWith("Dec")) {
                mod = "<aspect name=\"" + result + "\">";
            } else if (result.endsWith("MAsp")) {
                mod = "<multiAspect name=\"" + result + "\">";
            } else if (result.endsWith("Spec")) {
                mod = "<specialization name=\"" + result + "\">";
            }
            f0.println(mod);



            //to close the tag
            if (result.endsWith("Dec")) {
                mod = "</aspect>";
            } else if (result.endsWith("MAsp")) {
                mod = "</multiAspect>";
            } else if (result.endsWith("Spec")) {
                mod = "</specialization>";
            } else {
                if (result.endsWith("Seq")) {
                    continue;
                }
                mod = "</entity>";
            }
            f0.println(mod);



            //stays in the middle
            if (result.endsWith("Var")) {
                String novarresult = result.replace("Var", "");

                String[] properties = novarresult.split("<->");
                if (properties[1].equals("string") || properties[1].equals("boolean")) {

                    f0.println("<var name=\"" + properties[0] + "\" type=\"" + properties[1]
                            + "\" default=\"" + properties[2]
                            + "\"> </var>");
                } else {

                    if (properties.length > 5) {
                        f0.println("<var name=\"" + properties[0] + "\" type=\"" + properties[1]
                                + "\" default=\"" + properties[2]
                                + "\" lower=\"" + properties[3]
                                + "\" " + "upper=\"" + properties[4]
                                + "\" distributionType=\"" + properties[5]
                                + "\" distributionDetails=\"" + properties[6] + "\"> </var>");
                    } else {
                        f0.println("<var name=\"" + properties[0] + "\" type=\"" + properties[1]
                                + "\" default=\"" + properties[2]
                                + "\" lower=\"" + properties[3] + "\" " + "upper=\""
                                + properties[4] + "\"> </var>");
                    }
                }

            } else if (result.endsWith("Distion")) {
                String novarresult = result.replace("Distion", "");
                String[] properties = novarresult.split(",");
                f0.println("<distion variablename=\"" + properties[0]
                        + "\" distributiontype=\"" + properties[1]
                        + "\" details=\"" + properties[2] + "\"> </distion>");
            } else if (result.endsWith("InterCon")) {
                String novarresult = result.replace("InterCon", "");
                String[] properties = novarresult.split("<->");
                f0.println("<InterCon intercontraintname=\"" + properties[0] + "\"> </InterCon>");
            } else if (result.endsWith("IntraCon")) {
                String novarresult = result.replace("IntraCon", "");
                String[] properties = novarresult.split("<->");
                f0.println("<IntraCon intracontraintname=\"" + properties[0] + "\"> </IntraCon>");
            } else if (result.endsWith("Behaviour")) {
                String novarresult = result.replace("Behaviour", "");
                String[] properties = novarresult.split("<->");
                f0.println("<behaviour name=\"" + properties[0] + "\"> </behaviour>");
            } else if (result.endsWith("RefNode")) {
                String noRefNoderesult = result.replace("RefNode", "");

                if (noRefNoderesult.endsWith("Dec")) {
                    f0.println("<aspect name=\"" + noRefNoderesult + "\" ref=\"" + noRefNoderesult
                            + "\"/>");
                } else if (noRefNoderesult.endsWith("MAsp")) {
                    f0.println(
                            "<multiAspect name=\"" + noRefNoderesult + "\" ref=\"" + noRefNoderesult
                                    + "\"/>");
                } else if (noRefNoderesult.endsWith("Spec")) {
                    f0.println("<specialization name=\"" + noRefNoderesult + "\" ref=\""
                            + noRefNoderesult + "\"/>");
                } else {
                    f0.println("<entity name=\"" + noRefNoderesult + "\" ref=\"" + noRefNoderesult
                            + "\"/>");
                }
            }


        }
        f0.close();
    }
}
