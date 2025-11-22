package odme.sampling;

import odme.odmeeditor.ODMEEditor;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * This class creates the user interface panel for the "Generate Samples" window.
 * It allows the user to input the number of samples and choose a save location.
 */
public class GenerateSamplesPanel extends JPanel {

    private final JTextField numSamplesField;
    private final JTextField filePathField;
    private final JTextField filePathFieldyaml;
    private final JButton browseButton;
    private final JButton browseButtonyaml;
    private final JButton generateButton;
    private final JButton cancelButton;
    private final JCheckBox notUseDistributionCheckBox;
    private final JCheckBox useDistributionCheckBox;

    public GenerateSamplesPanel() {
        // Use GridBagLayout for flexible component placement
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        //  Select YAML file
        JLabel yamlLabel = new JLabel("Select YAML:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        add(yamlLabel, gbc);

        filePathFieldyaml = new JTextField();
        filePathFieldyaml.setEditable(false); // User should use the browse button
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 4;
        add(filePathFieldyaml, gbc);

        browseButtonyaml = new JButton("Browse...");
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 0;
        add(browseButtonyaml, gbc);

        //  Number of Samples Input
        JLabel numSamplesLabel = new JLabel("Number of Samples:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        add(numSamplesLabel, gbc);

        numSamplesField = new JTextField("100"); // Default value
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1;
        add(numSamplesField, gbc);

        //Use distribution or not
        notUseDistributionCheckBox = new JCheckBox("Use Normal Range");
        notUseDistributionCheckBox.setSelected(true); // Use Normal Range checked by default
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1;
        add(notUseDistributionCheckBox, gbc);

        useDistributionCheckBox = new JCheckBox("Use Distribution");
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1;
        add(useDistributionCheckBox, gbc);

        // Make them mutually exclusive (if I select one, the other unselects alone)
        useDistributionCheckBox.addActionListener(e -> {
            if (useDistributionCheckBox.isSelected()) {
                notUseDistributionCheckBox.setSelected(false);
            }
        });

        notUseDistributionCheckBox.addActionListener(e -> {
            if (notUseDistributionCheckBox.isSelected()) {
                useDistributionCheckBox.setSelected(false);
            }
        });

        //  Output File Path Input
        JLabel filePathLabel = new JLabel("Save As:");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        add(filePathLabel, gbc);

        filePathField = new JTextField();
        filePathField.setEditable(false); // User should use the browse button
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weightx = 1;
        add(filePathField, gbc);

        browseButton = new JButton("Browse...");
        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.weightx = 0;
        add(browseButton, gbc);

        // Action Buttons (Generate, Cancel)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        generateButton = new JButton("Generate");
        cancelButton = new JButton("Cancel");
        buttonPanel.add(generateButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        add(buttonPanel, gbc);

        // Add Action Listeners
        addListeners();
    }

    private void addListeners() {
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser(ODMEEditor.fileLocation);
                fileChooser.setDialogTitle("Save Sampled Data As");
                fileChooser.setSelectedFile(new File("Zgenerated_samples.csv"));
                int userSelection = fileChooser.showSaveDialog(GenerateSamplesPanel.this);

                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    filePathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });

        browseButtonyaml.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser(ODMEEditor.fileLocation);
                // Create a filter for .txt files
                FileNameExtensionFilter filter = new FileNameExtensionFilter("YAML Files (*.yaml)", "yaml");
                // Apply the filter
                fileChooser.setFileFilter(filter);
                // Optional: disable "All files" option
                fileChooser.setAcceptAllFileFilterUsed(false);
                fileChooser.setDialogTitle("Select YAML File");

                int userSelection = fileChooser.showSaveDialog(GenerateSamplesPanel.this);

                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    filePathFieldyaml.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });

        generateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // --- STEP 1: Declare all variables in the top-level scope of the method ---
                int numberOfSamples;
                String outputCsvPath;
                String yamlFilePath;

                // --- STEP 2: Get and validate user input, assigning values to our variables ---
                outputCsvPath = filePathField.getText().trim();
                if (outputCsvPath.isEmpty()) {
                    JOptionPane.showMessageDialog(GenerateSamplesPanel.this, "Please select an output file path.", "Input Required", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    numberOfSamples = Integer.parseInt(numSamplesField.getText().trim());
                    if (numberOfSamples <= 0) {
                        JOptionPane.showMessageDialog(GenerateSamplesPanel.this, "Please enter a positive number for samples.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(GenerateSamplesPanel.this, "Invalid number format for samples.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Construct the path to the YAML file
                yamlFilePath = filePathFieldyaml.getText().trim();

                // --- STEP 3: Use the variables inside the try-catch block ---
                try {
                    System.out.println("Calling sampling logic with:");
                    System.out.println("  - Samples: " + numberOfSamples);
                    System.out.println("  - Input YAML: " + yamlFilePath);
                    System.out.println("  - Output CSV: " + outputCsvPath);

                    // --- THE ACTUAL CALL TO YOUR BACK-END MODULE ---
                    SamplingManager samplingManager = new SamplingManager();

                     if(notUseDistributionCheckBox.isSelected()) {
                         samplingManager.generateSamples(yamlFilePath, numberOfSamples, outputCsvPath,0);
                     }
                     if(useDistributionCheckBox.isSelected()){
                         samplingManager.generateSamples(yamlFilePath, numberOfSamples, outputCsvPath,1);
                     }

                    // --- THIS CODE ONLY RUNS IF THE ABOVE LINE SUCCEEDS ---
                    JOptionPane.showMessageDialog(GenerateSamplesPanel.this,
                            "Successfully generated " + numberOfSamples + " valid samples!",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);

                    // Close the window on success
                    SwingUtilities.getWindowAncestor(GenerateSamplesPanel.this).dispose();

                } catch (Exception ex) {
                    // --- THIS CODE ONLY RUNS IF generateSamples() THROWS AN EXCEPTION ---
                    JOptionPane.showMessageDialog(GenerateSamplesPanel.this,
                            "An error occurred during sample generation:\n" + ex.getMessage(),
                            "Sampling Error",
                            JOptionPane.ERROR_MESSAGE);

                    ex.printStackTrace();
                }
            }

        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Get the parent window and close it
                SwingUtilities.getWindowAncestor(GenerateSamplesPanel.this).dispose();
            }
        });
    }
}
