package odme.odmeeditor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import odme.jtreetograph.*;
import odme.module.importFromCameo.FileImporter;
import odme.sampling.GenerateSamplesPanel;
import odme.sampling.SamplingManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.common.collect.ArrayListMultimap;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxUndoManager;
import com.mxgraph.util.svg.ParseException;

import static odme.odmeeditor.ODDManager.currentXsdToYamlTemp;
import static odme.odmeeditor.ODMEEditor.*;
import static odme.odmeeditor.XmlUtils.sesview;

public class MenuBar {
	
	private JMenuBar menuBar;
	public static List<JMenuItem> fileMenuItems= new ArrayList<>();

	private static JFrame mainFrame = null;
	private static JTextField numSamplesField;
	private static String csvPath = null;
	private static String yamlFilePath = null;

	public MenuBar(JFrame frame) {
		menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);
		mainFrame = frame;
    }
	
	public void show() {
		// File Menu
		final String[] items_file =  {"Save"       , "Save As"    , "Save as PNG" , null, "Exit"       };
		final int[] keyevents_file = {KeyEvent.VK_S, KeyEvent.VK_A, 0             , 0   , KeyEvent.VK_X};
		final String[] keys_file =   {"control S"  ,"control A"   , null          , null, "control X"  };
		final String[] images_file = {"save_icon"  , "save_icon"  , "png_icon"    , null, "exit_icon"  };

		addMenu("File", KeyEvent.VK_F, items_file, keyevents_file, keys_file, images_file);

		// Domain Modelling Menu
		final String[] items_domain_modelling =  {"New Project", "Open"       , "Import Template", "Save as Template", "Import From Cameo"};
		final int[] keyevents_domain_modelling = {KeyEvent.VK_N, KeyEvent.VK_O, KeyEvent.VK_I    , KeyEvent.VK_E    , KeyEvent.VK_C     };
		final String[] keys_domain_modelling =   {"control N"  , "control O"  , "control I"      , "control E"      , "control C"       };
		final String[] images_domain_modelling = {"new_icon"   , "open_icon"  , "import_icon"    , "export_icon"    , "cimport_icon"     };

		addMenu("Domain Modelling", 0, items_domain_modelling, keyevents_domain_modelling, keys_domain_modelling, images_domain_modelling);

		// Scenario Modelling Menu
		final String[] items_scenario_modelling =  {"Save Scenario" };
		final int[] keyevents_scenario_modelling = {0};
		final String[] keys_scenario_modelling =   {null};
		final String[] images_scenario_modelling = {"save_scenario" };

		addMenu("Scenario Modelling", 0, items_scenario_modelling, keyevents_scenario_modelling, keys_scenario_modelling, images_scenario_modelling);

		//Behavior Modelling
		String[] items_behaviour_modelling = {"Sync Behaviour"};
		final int[] keyevents_behaviour_modelling = {KeyEvent.VK_B};
		final String[] keys_behaviour_modelling =   {"control B"};
		final String[] images_behaviour_modelling = {"sync"};

		addMenu("Behavior Modelling " , 0 ,items_behaviour_modelling,
				keyevents_behaviour_modelling,
				keys_behaviour_modelling,images_behaviour_modelling);

		// #ROY - adding a new ICON
		// Operation Design Domain Menu
		final String[] items_operation_design_domain =  {"Generate OD","ODD Manager"};
		final int[] keyevents_operation_design_domain = {0,0};
		final String[] keys_operation_design_domain =   {null,null};
		final String[] images_operation_design_domain = {"export_icon","list"};

		addMenu("Operation Design Domain", 0, items_operation_design_domain, keyevents_operation_design_domain, keys_operation_design_domain, images_operation_design_domain);

		// Scenario Manager Menu
		final String[] items_scenario_manager =  {"Scenarios List", "Execution", "Feedback Loop",  "Generate Samples"};
		final int[] keyevents_scenario_manager = {0               , 0         ,  0     , 0        };
		final String[] keys_scenario_manager =   {null            , null      ,  null  , null        };
		final String[] images_scenario_manager = {"list"          ,"executionIcon"      ,"feedbackLoopIcon"    , "list"   };

		addMenu("Scenario Manager", 0, items_scenario_manager, keyevents_scenario_manager, keys_scenario_manager, images_scenario_manager);

		// Help Menu
		final String[] items_help =  {"Manual"     , "About"   };
		final int[] keyevents_help = {KeyEvent.VK_M, KeyEvent.VK_T};
		final String[] keys_help =   {"control M"  , "control T"  };
		final String[] images_help = {"manual_icon", "about_icon"  };

		addMenu("Help", KeyEvent.VK_H, items_help, keyevents_help, keys_help, images_help);

	}

	private void addMenu(String name, int key_event, String[] items, int[] keyevents, String[] keys, String[] images) {
		JMenu menu = new JMenu(name);
		menu.setMnemonic(key_event);
		menu.setBorder(new EmptyBorder(10, 20, 10, 20));

		final int itemLength = 40, itemWidth = 200;
		final int iconWidth = 20; // Set the width of the icon
		final int iconHeight = 20; // Set the height of the icon

		for (int i = 0; i < items.length; i++) {
			if (items[i] == null) {
				menu.addSeparator();
				continue;
			}

			JMenuItem menuItem = new JMenuItem(items[i], keyevents[i]);
			KeyStroke ctrlSKeyStroke = KeyStroke.getKeyStroke(keys[i]);
			menuItem.setAccelerator(ctrlSKeyStroke);
			menuItem.setPreferredSize(new Dimension(itemWidth, itemLength));

			if (images[i] != null) {
				URL imageUrl = ODMEEditor.class.getResource("/images/" + images[i] + ".png");
				if (imageUrl != null) {
					ImageIcon originalIcon = new ImageIcon(imageUrl);
					Image image = originalIcon.getImage();
					Image newimg = image.getScaledInstance(iconWidth, iconHeight, java.awt.Image.SCALE_SMOOTH);
					ImageIcon newIcon = new ImageIcon(newimg);
					menuItem.setIcon(newIcon);
				} else {
					System.out.println("Image not found: " + images[i]);
				}
			}

			if (items[i]=="Save Scenario" || items[i]=="Scenarios List" || items[i]=="Execution" || items[i]=="Feedback Loop" || items[i]=="Generate Samples"
					|| items[i]=="Generate OD" || items[i]=="ODD Manager")
				menuItem.setEnabled(false);

			if (items[i]=="New Project" || items[i]=="Import Template" || items[i]=="Save Scenario" ||
					items[i]=="Open" || items[i]=="Save as Template" || items[i]=="Scenarios List" ||
					items[i]=="Execution" || items[i]=="Feedback Loop" ||  items[i]=="Generate Samples" || items[i]=="Export XML" ||
					items[i]=="Export Yaml" || items[i]=="Generate OD" || items[i]=="ODD Manager") {
				fileMenuItems.add(menuItem);
			}

			//adding the different generation scenario options as submenus
			if ("Save Scenario".equals(items[i])) {
				// Create the submenu instead of a normal menu item
				JMenu generateScenarioSubMenu = new JMenu("Generate Scenario");
				addGeneratedScenarioSubMenu(generateScenarioSubMenu);
				menu.add(generateScenarioSubMenu);
			}

			menuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					switch (e.getActionCommand()) {
						case "Save Scenario":
							saveScenario();
							break;
						case "Scenarios List":
							ScenarioList scenarioList = new ScenarioList();
							scenarioList.createScenarioListWindow();
							break;
						case "Execution":
							openExecutionWindow();
							break;
						case "Generate Samples":
							openGenerateSamplesWindow();
							break;
						case "New Project":
							newFunc();
							break;
						case "Open":
							openFunc();
							break;
						case "Save As":
							saveAsFunc();
							break;
						case "Save as PNG":
							saveAsPNGFunc();
							break;
						case "Import Template":
							importFunc();
							break;
						case "Sync Behaviour":
							BehaviourList b = new BehaviourList();
							b.createScenarioListWindow();
						case "Save as Template":
							exportFunc();
							break;
						case "Import From Cameo":
							cImportFunc();
							break;
						case "Exit":
							System.exit(1);
							break;
						case "Manual":
							manualFunc();
							break;
						case "About":
							About about = new About();
							about.aboutGUI();
							break;
						case "Generate OD":
							openODDManager("Generate OD");
							break;
						case "ODD Manager":
							openODDManager("ODD Manager");
							break;
					}
				}
			});
			menu.add(menuItem);
		}
		menuBar.add(menu);
	}

	public void addGeneratedScenarioSubMenu(JMenu parentMenu) {
		// First-level submenu items
		JMenuItem csvGenScen = new JMenuItem("From CSV");
		JMenuItem oddGenScen = new JMenuItem("Direct From Domain Model");

//		// Example of a nested submenu
//		JMenu advancedMenu = new JMenu("Advanced");
//		JMenuItem betaDist = new JMenuItem("Beta Distribution");
//		JMenuItem gammaDist = new JMenuItem("Gamma Distribution");
//
//		advancedMenu.add(betaDist);
//		advancedMenu.add(gammaDist);

		// Add actions
		csvGenScen.addActionListener(e -> {
			openGenerateScenarioWithCsvWindow();
		});

		oddGenScen.addActionListener(e -> {
			// Create the dialog
			JDialog dialog = new JDialog((Frame) null, "Generate Scenarios From Domain Model", true);
			dialog.setLayout(new GridBagLayout());
			dialog.setSize(500, 200);
			dialog.setLocationRelativeTo(null); // center on screen

			// Layout helper
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets = new Insets(8, 8, 8, 8);
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;

			// --- Scenario Name ---
			JLabel nameLabel = new JLabel("Enter Scenario Name:");
			JTextField nameField = new JTextField();

			gbc.gridx = 0;
			gbc.gridy = 0;
			dialog.add(nameLabel, gbc);
			gbc.gridx = 1;
			gbc.gridy = 0;
			dialog.add(nameField, gbc);

			// --- Buttons ---
			JButton okButton = new JButton("OK");
			JButton cancelButton = new JButton("Cancel");

			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			buttonPanel.add(okButton);
			buttonPanel.add(cancelButton);

			gbc.gridx = 0;
			gbc.gridy = 2;
			gbc.gridwidth = 3;
			dialog.add(buttonPanel, gbc);

			numSamplesField = new JTextField("100"); // Default value
			gbc.gridx = 1;
			gbc.gridy = 1;
			gbc.weightx = 1;
			dialog.add(numSamplesField, gbc);

			// --- OK button action ---
			okButton.addActionListener(ee -> {
				String scenarioName = nameField.getText().trim();
				//check if the scenario name is empty
				if (scenarioName.isEmpty()) {
					JOptionPane.showMessageDialog(dialog, "Please enter a scenario name.", "Missing Name", JOptionPane.WARNING_MESSAGE);
					return;
				}
				//accept only numbers in Number of samples
				if (!numSamplesField
						.getText().trim().matches("^[0-9]+")) {
					JOptionPane.showMessageDialog(dialog, "Number of samples should be a whole number", "Wrong Number", JOptionPane.WARNING_MESSAGE);
					return;
				}

				//begin: Save Current
				ODMEEditor.treePanel.saveTreeModel();

				JtreeToGraphConvert.convertTreeToXML();
				JtreeToGraphConvert.graphToXML();
				JtreeToGraphConvert.graphToXMLWithUniformity();
				//end

				//begin saving to the XSD-file
				// the new properties (e.g variables, distributions, behaviours) added to the nodes
				fileConversion.modifyXmlOutputForXSD();

				JtreeToGraphConvert.rootToEndNodeSequenceSolve();
				JtreeToGraphConvert
						.rootToEndNodeVariable(); // have to try using saving keys in a list like i did in
				JtreeToGraphConvert
						.rootToEndNodeDistribution();
				JtreeToGraphConvert
						.rootToEndNodeInterConstraint();
				JtreeToGraphConvert
						.rootToEndNodeIntraConstraint();
				JtreeToGraphModify.modifyXmlOutputFixForSameNameNode();

				fileConversion.xmlToXSDConversion();
				//end

				// begin Save
//				fileLocation = fileLocation+"/"+projName;
				JtreeToGraphVariables.newFileName = currentScenario;
				JtreeToGraphVariables.projectFileNameGraph = currentScenario;

				JtreeToGraphVariables.ssdFileGraph = new File(String.format("%s/%s/%sGraph.xml",
						fileLocation, currentScenario, projName));
				treePanel.ssdFile = new File(String.format("%s/%s/%s.xml",
						fileLocation, currentScenario, projName));
				treePanel.ssdFileVar = new File(String.format("%s/%s/%s.ssdvar",
						fileLocation, currentScenario, projName));
				treePanel.ssdFileDis = new File(String.format("%s/%s/%s.ssddis",
						fileLocation, currentScenario, projName));
				treePanel.ssdFileInterCon = new File(String.format("%s/%s/%s.ssdcon",
						fileLocation, currentScenario, projName));

				treePanel.ssdFileBeh = new File(String.format("%s/%s/%s.ssdbeh",
						fileLocation, currentScenario, projName));

				treePanel.ssdFileFlag = new File(String.format("%s/%s/%s.ssdflag",
						fileLocation, currentScenario, projName));

				File f = new File(fileLocation + "/" + projName + "/" + currentScenario);
				f.mkdirs();

				treePanel.saveTreeModel();

				JtreeToGraphConvert.convertTreeToXML();
				JtreeToGraphConvert.graphToXML();
				JtreeToGraphConvert.graphToXMLWithUniformity();

				tabbedPane.removeAll();
				tabbedPane.addTab("XML", XmlUtils.sesview);
				changePruneColor();
				ToolBar.btnScenario.setVisible(true);
				ODMEEditor.graphWindow.setTitle(currentScenario);
				nodeAddDetector = "";

				JTableHeader th = Variable.table.getTableHeader();
				TableColumnModel tcm = th.getColumnModel();
				TableColumn tc = tcm.getColumn(3);
				tc.setHeaderValue( "Value" );
				th.repaint();

				JtreeToGraphPrune.behMapTransfer = ArrayListMultimap.create();
				JtreeToGraphPrune.varMapTransfer = ArrayListMultimap.create();
				JtreeToGraphPrune.distributionMapTransfer = ArrayListMultimap.create();
				//end

				//begin: set YAML File
				String xsdPath = ODMEEditor.fileLocation + "/" + projName + "/xsdfromxml.xsd";
				yamlFilePath= ODMEEditor.fileLocation + "/" + projName + "Temp.yaml";
				File yamlFile = new File(yamlFilePath);
				try {
					yamlFile.createNewFile();
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}//Delete at the end of the operation
				ODMEEditor.saveFunc(false);
				ODMEEditor.updateState();
				JtreeToGraphConvert.convertTreeToXML();
				String yamlContent=currentXsdToYamlTemp(xsdPath);
				ODMEEditor.saveYamlTempFile(yamlContent,yamlFilePath, null);
				//end


				//begin: set CSV File
				String outputCsvPath = new String();
				if (ODMEEditor.toolMode == "ses")
					outputCsvPath = ODMEEditor.fileLocation + "/" + projName ;
				else
					outputCsvPath = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario ;

				csvPath = outputCsvPath + "/CSVTemp.csv"; //Delete at the end of the operation
				File csvFile = new File(csvPath);
                try {
					csvFile.createNewFile();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }

                // --- THE ACTUAL CALL TO YOUR BACK-END MODULE ---
				SamplingManager samplingManager = new SamplingManager();
                try {
                    samplingManager.generateSamplesforDomainModel(yamlFilePath, Integer.parseInt(numSamplesField.getText().trim()), csvPath);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
				//end


				System.out.println(" Scenario Name: " + scenarioName);
				System.out.println(" CSV File: " + csvPath);
				// Show confirmation dialog with the folder path where the Scenarios are saved
				JOptionPane.showMessageDialog(dialog,
						"You entered:\n\n" + ScenarioGeneration.generateScenarios(csvPath , scenarioName),
						"Your Message",
						JOptionPane.INFORMATION_MESSAGE);

				dialog.dispose(); // close dialog

				csvFile.delete();
				yamlFile.delete();
			});

			// --- Cancel button action ---
			cancelButton.addActionListener(ee -> dialog.dispose());

			dialog.setVisible(true);
		});

		// Add everything into the "Add Distribution" menu
		parentMenu.add(csvGenScen);
		parentMenu.add(oddGenScen);
		parentMenu.addSeparator();
//		parentMenu.add(advancedMenu); // submenu inside submenu
	}

	public static void openGenerateScenarioWithCsvWindow() {
		// Create the dialog
		JDialog dialog = new JDialog((Frame) null, "Generate Scenario Using CSV File", true);
		dialog.setLayout(new GridBagLayout());
		dialog.setSize(500, 200);
		dialog.setLocationRelativeTo(null); // center on screen

		// Layout helper
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(8, 8, 8, 8);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;

		// --- Scenario Name ---
		JLabel nameLabel = new JLabel("Enter Scenario Name:");
		JTextField nameField = new JTextField();

		gbc.gridx = 0;
		gbc.gridy = 0;
		dialog.add(nameLabel, gbc);
		gbc.gridx = 1;
		gbc.gridy = 0;
		dialog.add(nameField, gbc);

		// --- YAML File Path ---
		JLabel pathLabel = new JLabel("CSV File Path:");
		JTextField pathField = new JTextField();
		pathField.setEditable(false); // not editable

		JButton browseButton = new JButton("Browse...");
		browseButton.addActionListener(e -> {
			JFileChooser fileChooser = new JFileChooser(ODMEEditor.fileLocation);
			FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV files (*.csv)", "csv");
			fileChooser.setFileFilter(filter);
			fileChooser.setDialogTitle("Select CSV File");
			fileChooser.setMultiSelectionEnabled(false);
			fileChooser.setAcceptAllFileFilterUsed(false);

			int result = fileChooser.showOpenDialog(dialog);
			if (result == JFileChooser.APPROVE_OPTION) {
				File selectedFile = fileChooser.getSelectedFile();
				pathField.setText(selectedFile.getAbsolutePath());
			}
		});

		gbc.gridx = 0;
		gbc.gridy = 1;
		dialog.add(pathLabel, gbc);
		gbc.gridx = 1;
		gbc.gridy = 1;
		dialog.add(pathField, gbc);
		gbc.gridx = 2;
		gbc.gridy = 1;
		dialog.add(browseButton, gbc);

		// --- Buttons ---
		JButton okButton = new JButton("OK");
		JButton cancelButton = new JButton("Cancel");

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 3;
		dialog.add(buttonPanel, gbc);

		// --- OK button action ---
		okButton.addActionListener(e -> {
			String scenarioName = nameField.getText().trim();
			String csvPath = pathField.getText().trim();

			if (scenarioName.isEmpty()) {
				JOptionPane.showMessageDialog(dialog, "Please enter a scenario name.", "Missing Name", JOptionPane.WARNING_MESSAGE);
				return;
			}
			if (csvPath.isEmpty()) {
				JOptionPane.showMessageDialog(dialog, "Please select a CSV file.", "Missing File", JOptionPane.WARNING_MESSAGE);
				return;
			}

			System.out.println(" Scenario Name: " + scenarioName);
			System.out.println(" CSV File: " + csvPath);

			// Show confirmation dialog with the folder path where the Scenarios are saved
			JOptionPane.showMessageDialog(dialog,
					"You entered:\n\n" + ScenarioGeneration.generateScenarios(csvPath , scenarioName),
					"Your Message",
					JOptionPane.INFORMATION_MESSAGE);

			dialog.dispose(); // close dialog
		});

		// --- Cancel button action ---
		cancelButton.addActionListener(e -> dialog.dispose());

		dialog.setVisible(true);
	}


	private void openGenerateSamplesWindow() {
		ODMEEditor.saveFunc(false);
		ODMEEditor.updateState();
		JFrame f = new JFrame("Generate Samples");
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.getContentPane().add(new GenerateSamplesPanel()); // create this JPanel class
		f.pack();
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}

	/**
     * @author Roy
     * #ROY - adding new Functionality to see all the nodes 
     * */
    private void openODDManager(String mode) {
    	ODMEEditor.saveFunc(false); // save the results
    	ODMEEditor.updateState();
    	ODDManager nt=new ODDManager(mode);
    	JFrame jd = new JFrame();
    	jd.getContentPane().add(nt);
    	jd.pack();
    	jd.setVisible(true);
    	// jd.setLocation(128, 128);
    	jd.setLocationRelativeTo(null);
    	// jd.setAlwaysOnTop(true);
    }


	private void cImportFunc() {
		FileImporter fileImporter = new FileImporter();
		fileImporter.showImportDialog(mainFrame);
	}
    
    @SuppressWarnings("unchecked")
	private void saveScenario() {
    	JSONParser jsonParser = new JSONParser();
        
        try (FileReader reader = new FileReader(ODMEEditor.fileLocation + "/scenarios.json")){
            Object obj = null;
			try {
				obj = jsonParser.parse(reader);
			} 
			catch (org.json.simple.parser.ParseException e) {
				e.printStackTrace();
			}
			
            JSONArray data = (JSONArray) obj;
            
        	JTextField nameField = new JTextField();
        	nameField.setText("Scenario" + Integer.toString(data.size()+1));

        	Object[] message = {"Scenario Name:", nameField};

        	int option = JOptionPane
        			.showConfirmDialog(Main.frame, message, "Create Scenario", JOptionPane.OK_CANCEL_OPTION,
        			JOptionPane.PLAIN_MESSAGE);
        	
        	if (option == JOptionPane.OK_OPTION) {
            	createScenario(nameField.getText());
            	
        		JSONObject jo = new JSONObject();
        		jo.put("name", nameField.getText());
        		jo.put("risk", "");
        		jo.put("remarks", "");
				
				JSONObject jom = new JSONObject();
				jom.put("scenario", jo);
				data.add(jom);
        	}
        	
        	else
        		return;
        	
        	try {
		         FileWriter file = new FileWriter(ODMEEditor.fileLocation + "/scenarios.json");
		         file.write(data.toJSONString());
		         file.close();
		         ODMEEditor.graphWindow.setTitle(nameField.getText());
		      }
        	catch (IOException e) {
		         e.printStackTrace();
		      }

        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } 
        catch (IOException e) {
            e.printStackTrace();
        } 
        catch (ParseException e) {
            e.printStackTrace();
        } 
    }
    
    private void createScenario(String ScenarioName) {
        ODMEEditor.currentScenario = ScenarioName;
        
        JtreeToGraphVariables.ssdFileGraph = new File(String.format("%s/%s/%sGraph.xml",
    			 ODMEEditor.fileLocation, ScenarioName, projName));
    	 ODMEEditor.treePanel.ssdFile = new File(String.format("%s/%s/%s.xml",
    			 ODMEEditor.fileLocation,  ScenarioName, projName));
    	 ODMEEditor.treePanel.ssdFileVar = new File(String.format("%s/%s/%s.ssdvar",
    			 ODMEEditor.fileLocation,  ScenarioName, projName));
		 ODMEEditor.treePanel.ssdFileDis = new File(String.format("%s/%s/%s.ssddis",
				ODMEEditor.fileLocation,  ScenarioName, projName));
    	 ODMEEditor.treePanel.ssdFileInterCon = new File(String.format("%s/%s/%s.ssdcon",
    			 ODMEEditor.fileLocation,  ScenarioName, projName));
    	 ODMEEditor.treePanel.ssdFileFlag = new File(String.format("%s/%s/%s.ssdflag",
    			 ODMEEditor.fileLocation,  ScenarioName, projName));

		ODMEEditor.treePanel.ssdFileBeh = new File(String.format("%s/%s/%s.ssdbeh",
				ODMEEditor.fileLocation,  ScenarioName, projName));

        File f = new File(ODMEEditor.fileLocation + "/" +  ScenarioName);
        f.mkdirs();
        
        ODMEEditor.updateState();
        ODMEEditor.changePruneColor();
    }
    
    private void newFunc() {
    	DynamicTree.varMap = ArrayListMultimap.create();
    	NewProject newProject = new NewProject();
	  	newProject.createNewProjectWindow();
	  	// resetting undoManager so that it will remove its indexes from previous projects
	  	JtreeToGraphVariables.undoManager = new mxUndoManager();
    }
    
    private void openFunc() {
    	// filechooser
    	//DynamicTree.varMap = ArrayListMultimap.create();
    	//JtreeToGraphVariables.variableList = new String[100];
    	
    	
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setCurrentDirectory(
                new File(ODMEEditor.repFslas)); // this is ok because normally all the file will be
        // in default location. so don't need to add fileLocation

        int result = fileChooser.showOpenDialog(Main.frame);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String fileName = selectedFile.getName();
            System.out.println("Selected file: " + selectedFile.getName());

            String oldProjectTreeProjectName = projName;
            projName = fileName;
            ODMEEditor.fileLocation = selectedFile.getParentFile().getAbsolutePath();
            JtreeToGraphGeneral.openExistingProject(fileName, oldProjectTreeProjectName);

            JtreeToGraphVariables.undoManager = new mxUndoManager();
            sesview.textArea.setText("");
            Console.consoleText.setText(">>");
            Variable.setNullToAllRows();
            InterEntityConstraints.setNullToAllRows();
            
            if (ODMEEditor.toolMode == "pes")
            	ODMEEditor.applyGuiSES();
        }
    }
    
    private void saveAsFunc() {
    	JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(ODMEEditor.fileLocation));
        int result = fileChooser.showSaveDialog(Main.frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            ODMEEditor.fileLocation = selectedFile.getParentFile().getAbsolutePath();

            String newProjectName = selectedFile.getName();
            String oldProjectTreeProjectName = projName;

            projName = newProjectName;
            JtreeToGraphVariables.newFileName = newProjectName;
            JtreeToGraphVariables.projectFileNameGraph = newProjectName;

            JtreeToGraphVariables.ssdFileGraph = new File(String.format("%s/%s/%sGraph.xml",
            		ODMEEditor.fileLocation, projName, newProjectName));
            ODMEEditor.treePanel.ssdFile = new File(String.format("%s/%s/%s.xml",
            		ODMEEditor.fileLocation, projName, newProjectName));
            ODMEEditor.treePanel.ssdFileVar = new File(String.format("%s/%s/%s.ssdvar",
            		ODMEEditor.fileLocation, projName, newProjectName));
			ODMEEditor.treePanel.ssdFileDis = new File(String.format("%s/%s/%s.ssddis",
					ODMEEditor.fileLocation, projName, newProjectName));
            ODMEEditor.treePanel.ssdFileInterCon = new File(String.format("%s/%s/%s.ssdcon",
            		ODMEEditor.fileLocation, projName, newProjectName));
            ODMEEditor.treePanel.ssdFileFlag = new File(String.format("%s/%s/%s.ssdflag",
            		ODMEEditor.fileLocation, projName, newProjectName));

            ProjectTree.projectName = newProjectName;
            ODMEEditor.projectPanel.changeCurrentProjectFileName(newProjectName, oldProjectTreeProjectName);

            ODMEEditor.newProjectFolderCreation();
            ODMEEditor.treePanel.saveTreeModel();
            JtreeToGraphSave.saveGraph();

            // also it will convert after saving from here
            // this code is also present in convert to xml button click action.
            JtreeToGraphConvert.convertTreeToXML(); // this function is using for converting project tree into xml file
            JtreeToGraphConvert.graphToXML();
            JtreeToGraphConvert.graphToXMLWithUniformity();
            JOptionPane.showMessageDialog(Main.frame, "Saved Successfully.", "Save",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void saveAsPNGFunc() {
    	// remove border nodes
    	JtreeToGraphVariables.graph.getModel().beginUpdate();
    	Object position = JtreeToGraphVariables.graphComponent.getCellAt(0, 50000);
    	JtreeToGraphVariables.graph.removeCells(new Object[]{position});
		position = JtreeToGraphVariables.graphComponent.getCellAt(50000, 0);
		JtreeToGraphVariables.graph.removeCells(new Object[]{position});
		JtreeToGraphVariables.graph.getModel().endUpdate();

        // saving drawn graph as a png
    	try{
    		BufferedImage image = mxCellRenderer.createBufferedImage(JtreeToGraphVariables.graph, null, 1, Color.WHITE, true, null);
    		String path = new String();
        	if (ODMEEditor.toolMode == "ses")
        		path = ODMEEditor.fileLocation + "/" + projName  + "/graph.png";
        	else
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/graph.png";
        		
            ImageIO.write(image, "PNG", new File(path));
            JOptionPane.showMessageDialog(Main.frame, "Saved Successfully.", "Save PNG",
                    JOptionPane.INFORMATION_MESSAGE);
        } 
    	catch (Exception e) {
            System.out.println("Error:" + e);
            JOptionPane.showMessageDialog(Main.frame, "Error:" + e, "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    	
    	// add border nodes again
    	JtreeToGraphVariables.graph.getModel().beginUpdate();
    	JtreeToGraphVariables.graph.insertVertex(JtreeToGraphVariables.parent, "hideV", "End of Canvas", 0, 50000, 80, 30, "Entity");
    	JtreeToGraphVariables.graph.insertVertex(JtreeToGraphVariables.parent, "hideH", "End of Canvas", 50000, 0, 80, 30, "Entity");
    	JtreeToGraphVariables.graph.getModel().endUpdate();	
    }

    private void importFunc() {
    	ImportProject impProj = new ImportProject();
        impProj.importProject();
    }

    private void exportFunc() {
    	ToolBar.validation();
        String fileName = projName; // don't know why not fetching the file name here
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter xmlfilter = new FileNameExtensionFilter("xml files (*.xml)", "xml");
        fileChooser.setFileFilter(xmlfilter);
        fileChooser.setSelectedFile(new File(fileName)); // not working because filename is null
        fileChooser.setCurrentDirectory(new File(ODMEEditor.fileLocation + "/" + projName));
        int result = fileChooser.showSaveDialog(Main.frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            System.out.println("Exported file path: " + selectedFile.getAbsolutePath());
            PrintWriter f0 = null;
            try {
                f0 = new PrintWriter(new FileWriter(
                        selectedFile.getAbsolutePath() + ".xml"));
            }
            catch (IOException e1) {
                e1.printStackTrace();
            }
            Scanner in = null;
            try {
                in = new Scanner(new File(
                        ODMEEditor.fileLocation + "/" + projName + "/xmlforxsd.xml"));
            } catch (FileNotFoundException e2) {
                e2.printStackTrace();
            }
            while (in.hasNext()) { // Iterates each line in the file
                String line = in.nextLine();
                f0.println(line);
            }
            in.close();
            f0.close();
        }
    }

    private void manualFunc() {
    	File pdfTemp = null;
        if (Desktop.isDesktopSupported()) {
            try {
            	java.net.URL resource = ODMEEditor.class.getClassLoader().getResource("docs/manual.pdf");
            	
            	try {
					pdfTemp = new File(resource.toURI());
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
                // Open the PDF
                Desktop.getDesktop().open(pdfTemp);
            }
            catch (IOException e1) {
                System.out.println("erreur : " + e1);
            }
        }
    }

	private void openExecutionWindow() {
		Execution executionWindow = new Execution();
		executionWindow.setVisible(true);
	}
}
