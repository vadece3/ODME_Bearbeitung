package odme.odmeeditor;

import static odme.jtreetograph.JtreeToGraphVariables.nodeNumber;
import static odme.jtreetograph.JtreeToGraphVariables.undoManager;
import static odme.odmeeditor.MenuBar.getScenarioJsonData;
import static odme.odmeeditor.XmlUtils.sesview;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.table.DefaultTableModel;

import odeme.behaviour.Behaviour;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.common.collect.ArrayListMultimap;
import com.mxgraph.util.mxUndoManager;
import com.mxgraph.util.svg.ParseException;

import odme.jtreetograph.JtreeToGraphGeneral;
import structuretest.BehaviourCoverageTest;
import structuretest.MultiAspectNodeTest;
import structuretest.SpecialisationNodeTest;
import structuretest.Test;


public class ScenarioList extends JPanel {

	private static final long serialVersionUID = 1L;
	private JTable dynamic_scenario_table;
	private JTable static_scenario_table;
    private DefaultTableModel dynamic_scenario_model;
	private DefaultTableModel static_scenario_model;

    public void createScenarioListWindow() {
    	
    	List<String[]> dataList = getJsonData();
		Path path = Path.of("").toAbsolutePath();
//		String folderPath = path+ "\\GeneratedScenarios\\" + ODMEEditor.projName + "_Scenarios";
		String folderPath = path+ "\\GeneratedScenarios";
		List<String[]> staticSenarioList = getStaticScenarioList(folderPath);

		dynamic_scenario_model = new DefaultTableModel(new String[]{"Name", "Risk", "Remarks"}, 0);
    	for (String[] arr: dataList)
			dynamic_scenario_model.addRow(arr);

		static_scenario_model = new DefaultTableModel(new String[]{"Name", "Path"}, 0);
		for (String[] arr: staticSenarioList) {
			String scenario = arr[0];
			if(Objects.equals(scenario, ODMEEditor.projName + "_Scenarios")){
				static_scenario_model.addRow(arr);
			}

			// Print to console
			System.out.println(
					"Folder Name: " + arr[0] +
							" | Path: " + arr[1]
			);

		}

		dynamic_scenario_table = new JTable(dynamic_scenario_model);
		dynamic_scenario_table.setShowVerticalLines(true);
		dynamic_scenario_table.setDefaultEditor(Object.class, null);
		dynamic_scenario_table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		dynamic_scenario_table.setAutoCreateRowSorter(true);

		static_scenario_table = new JTable(static_scenario_model);
		static_scenario_table.setShowVerticalLines(true);
		static_scenario_table.setDefaultEditor(Object.class, null);
		static_scenario_table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		static_scenario_table.setAutoCreateRowSorter(true);

        final JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem openItem = new JMenuItem("Open");
        JMenuItem deleteItem = new JMenuItem("Delete");
        
        openItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	try {

            		DynamicTree.varMap = ArrayListMultimap.create();

            		int row = dynamic_scenario_table.getSelectedRow();
            		String fileName = (String) dynamic_scenario_table.getModel().getValueAt(row, 0);

            		System.out.println("Selected file: " + fileName);
            		ODMEEditor.currentScenario = fileName;

            		nodeNumber = 1;
            		JtreeToGraphGeneral.openExistingProject(ODMEEditor.projName, ODMEEditor.projName);

            		undoManager = new mxUndoManager();

            		sesview.textArea.setText("");
            		Console.consoleText.setText(">>");
            		Variable.setNullToAllRows();
            		InterEntityConstraints.setNullToAllRows();
            		Behaviour.setNullToAllRows();

            		ODMEEditor.graphWindow.setTitle(fileName);
            		ODMEEditor.changePruneColor();
            	}
            	catch (Exception ex){
            	}
            }
        });
        
        popupMenu.add(openItem);
        popupMenu.add(deleteItem);
        
        deleteItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	int row = dynamic_scenario_table.getSelectedRow();
            	String fileName = (String) dynamic_scenario_table.getModel().getValueAt(row, 0);
            	if (fileName.equals(ODMEEditor.currentScenario)) {
            		JOptionPane.showMessageDialog(Main.frame, "The Scenario is currently opened!", "Error",
                            JOptionPane.ERROR_MESSAGE);
            		return;
            	}
            	
            	int dialogResult = -1;
            	dialogResult = JOptionPane.showConfirmDialog (null,
        				"Do you want to delete "+fileName+"?","Delete Scenario",JOptionPane.YES_NO_OPTION);
        		if(dialogResult == JOptionPane.YES_OPTION){
        			deleteFolder(new File(ODMEEditor.fileLocation + "/" +  fileName));  
        			deleteFromJson(fileName);
        		}
            }
        });

		dynamic_scenario_table.setComponentPopupMenu(popupMenu);

		dynamic_scenario_table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    JTable target = (JTable) e.getSource();
                    int row = dynamic_scenario_table.getSelectedRow();
                    
                    String name = (String) target.getModel().getValueAt(row, 0);
					String risk = (String) target.getModel().getValueAt(row, 1);
					String remarks = (String) target.getModel().getValueAt(row, 2);
					updateTableData(name, risk, remarks);

                }
            }
        });

		static_scenario_table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int row = static_scenario_table.getSelectedRow();
				if (row != -1) {
					String folderPath = static_scenario_table.getValueAt(row, 1).toString();
					openFilesWindow(folderPath);
				}
			}
		});

        
//        TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(table.getModel());
//        table.setRowSorter(sorter);
//
//        List<RowSorter.SortKey> sortKeys = new ArrayList<>(25);
//        sortKeys.add(new RowSorter.SortKey(3, SortOrder.ASCENDING));
//        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
//        sorter.setSortKeys(sortKeys);

		// Create coverage button
        JButton buttonCoverage = new JButton("Generate Coverage");

		// Set action on button
		buttonCoverage.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				performStructuralCoverage();
			}
		});

        JFrame frame = new JFrame("Scenario List");
        JPanel panelCenter = new JPanel();
                
        JScrollPane dynamic_scenario_scroll = new JScrollPane(dynamic_scenario_table);
		dynamic_scenario_scroll.setPreferredSize(new Dimension(480, 200));

		JScrollPane static_scenario_scroll = new JScrollPane(static_scenario_table);
		static_scenario_scroll.setPreferredSize(new Dimension(480, 100));

		// Title label
		JLabel dynamictitleLabel = new JLabel("Dynamic Scenarios");
		dynamictitleLabel.setFont(new Font("Arial", Font.BOLD, 16));
		dynamictitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel statictitleLabel = new JLabel("Statistic Scenarios");
		statictitleLabel.setFont(new Font("Arial", Font.BOLD, 16));
		statictitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);




		panelCenter.add(dynamictitleLabel);
		frame.add(Box.createVerticalStrut(5)); // space between
        panelCenter.add(dynamic_scenario_scroll);
		frame.add(Box.createVerticalStrut(10)); // space between
		panelCenter.add(statictitleLabel);
		frame.add(Box.createVerticalStrut(5)); // space between
		panelCenter.add(static_scenario_scroll);
		frame.add(Box.createVerticalStrut(20)); // space between
		panelCenter.add(buttonCoverage);

        panelCenter.setBorder(new EtchedBorder());

        int width = 500;
        int height = 500;
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screen.width - width) / 2;
        int y = (screen.height - height) / 2;

        frame.pack();
                
        frame.setBounds(x, y, width, height);
        frame.setSize(width, height);
                
        frame.add(panelCenter, BorderLayout.CENTER);

		frame.setResizable(false);
        frame.setVisible(true);
    }

	// Opens a small window listing files in the folder
	private static void openFilesWindow(String folderPath) {
		JFrame fileFrame = new JFrame("Files in " + folderPath);
		fileFrame.setSize(600, 400);
		fileFrame.setLayout(new BorderLayout());

		// ---- TABLE ----
		DefaultTableModel fileModel = new DefaultTableModel(
				new String[]{"File Name"}, 0
		);
		JTable fileTable = new JTable(fileModel);
		JScrollPane tableScroll = new JScrollPane(fileTable);

		// ---- TEXT AREA (file content) ----
		JTextArea fileContentArea = new JTextArea();
		fileContentArea.setEditable(false);
		JScrollPane textScroll = new JScrollPane(fileContentArea);

		// Split pane (table on top, content below)
		JSplitPane splitPane = new JSplitPane(
				JSplitPane.VERTICAL_SPLIT,
				tableScroll,
				textScroll
		);
		splitPane.setDividerLocation(150);

		// Load files
		File folder = new File(folderPath);
		File[] files = folder.listFiles();

		if (files != null) {
			for (File file : files) {
				if (file.isFile()) {
					fileModel.addRow(new Object[]{file.getName()});
				}
			}
		}

		// ---- CLICK ACTION ----
		fileTable.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				int row = fileTable.getSelectedRow();
				if (row != -1) {
					String fileName = fileTable.getValueAt(row, 0).toString();
					File selectedFile = new File(folderPath, fileName);
					displayFileContent(selectedFile, fileContentArea);
				}
			}
		});

		fileFrame.add(splitPane, BorderLayout.CENTER);
		fileFrame.setLocationRelativeTo(null);
		fileFrame.setVisible(true);
	}

	private static void displayFileContent(File file, JTextArea textArea) {
		try {
			textArea.setText(""); // clear previous content

			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;

			while ((line = reader.readLine()) != null) {
				textArea.append(line + "\n");
			}

			reader.close();
		} catch (Exception e) {
			textArea.setText("Error reading file: " + e.getMessage());
		}
	}


	//Get Static Scenario List from path
	private List<String[]> getStaticScenarioList(String basePath) {
		List<String[]> result = new ArrayList<>();

		File baseDir = new File(basePath);
		File[] files = baseDir.listFiles();

		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					result.add(new String[]{
							file.getName(),
							file.getAbsolutePath()
					});
				}
			}
		}

		return result;
	}

	private void performStructuralCoverage(){

		List<String[]> dataList = getScenarioJsonData();

		String path = ODMEEditor.fileLocation  + "/graphxml.xml";

		SpecialisationNodeTest specialisationNodeTest = new SpecialisationNodeTest(path);
		Map c = specialisationNodeTest.getSpecialisationNodes();

		specialisationNodeTest.checkMatchedNodes(dataList);

		//Now behaviour test
		BehaviourCoverageTest behaviourCoverageTest = new BehaviourCoverageTest();
		behaviourCoverageTest.checkCodeCoverageForBehaviours(dataList);

		//Now MultiAspect nodes
		MultiAspectNodeTest multiAspectNodeTest  = new MultiAspectNodeTest();
		multiAspectNodeTest.parseNodes(path);

		multiAspectNodeTest.checkCodeCoverageMultiAspect(dataList);

		Test t = new Test(dataList);
		Map<String, Integer> map = t.getBucketStatistics();

		int totalBuckets = map.get("totalBuckets");
		int totalCoveredBuckets = map.get("totalCoveredBuckets");
		System.out.println("totalBuckets " + totalBuckets);
		System.out.println("totalCoveredBuckets " + totalCoveredBuckets);


		double specialisationPercentage = (specialisationNodeTest.getTotalSpecialisationNode() > 0)
				? (((double) specialisationNodeTest.getMatchedSpecialisationNode()  / specialisationNodeTest.getTotalSpecialisationNode())) * 100
				: 0.0;


		double behaviourPercentage = (behaviourCoverageTest.getTotalBehaviours() > 0)
				? (behaviourCoverageTest.getMatchedBehaviours() * 100.0 / behaviourCoverageTest.getTotalBehaviours())
				: 0.0;

		double parameterPercentage = ((double) totalCoveredBuckets / totalBuckets) * 100;
		System.out.println("parameterPercentage = " + parameterPercentage);
		double variablePercent =  ((double) 408 / 925) * 100;
		double overAllPercentage = (specialisationPercentage + variablePercent)/2;

		// Creating the 2D array
		Object[][] data = {
				{"Structural Coverage ",null, null, null, null},

				{"         Specialisation Coverage", specialisationNodeTest.getMatchedSpecialisationNode() ,
						specialisationNodeTest.getTotalSpecialisationNode() - specialisationNodeTest.getMatchedSpecialisationNode() ,
						specialisationNodeTest.getTotalSpecialisationNode(),
						specialisationPercentage},

				{"         MultiAspect Coverage" , multiAspectNodeTest.getTotalCoveredChildren(), multiAspectNodeTest.getTotalUncoveredChildren(),
						multiAspectNodeTest.getTotalUncoveredChildren() + multiAspectNodeTest.getTotalCoveredChildren(),
						multiAspectNodeTest.getTotalPercentage()
				},
				{"Behaviours", behaviourCoverageTest.getMatchedBehaviours(),
						behaviourCoverageTest.getTotalBehaviours() - behaviourCoverageTest.getMatchedBehaviours(),
						behaviourCoverageTest.getTotalBehaviours(),
						behaviourPercentage},
				{
						"Parameter Coverage" ,  totalCoveredBuckets ,
						totalBuckets - totalCoveredBuckets , totalBuckets,
						parameterPercentage
//						((double) totalCoveredBuckets / totalBuckets) * 100
//						variableCoverageTest.getTotalCoveredBuckets(),variableCoverageTest.getTotalUnCoveredBuckets(),
//						variableCoverageTest.getTotalBuckets(),
//						variablePercentage
				},
				{
						"Overall Coverage" , null,
//						specialisationPercentage
//						specialisationNodeTest.getMatchedSpecialisationNode()+
//						multiAspectNodeTest.getTotalCoveredChildren()
//						+variableCoverageTest.getTotalCoveredBuckets(), //unCovered starts
						null,
//						(specialisationNodeTest.getTotalSpecialisationNode() - specialisationNodeTest.getMatchedSpecialisationNode()) +
//								multiAspectNodeTest.getTotalUncoveredChildren()
//								+variableCoverageTest.getTotalUnCoveredBuckets(), // Total starts
						null,
//						specialisationNodeTest.getTotalSpecialisationNode() + multiAspectNodeTest.getMultiAspectNodeCount()
//						+ variableCoverageTest.getTotalBuckets(),
						(specialisationPercentage + multiAspectNodeTest.getTotalPercentage() + behaviourPercentage + parameterPercentage)/ 4
//						overAllPercentage
				}
		};

		CodeCoverageLayout layout = new CodeCoverageLayout(Main.frame , data);
		layout.setVisible(true);
	}

    private void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if(files!=null) {
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }
    
    @SuppressWarnings("unchecked")
	private void deleteFromJson(String scenario) {
    	List<String[]> dataList = getJsonData();
		
		JSONArray ja = new JSONArray();
		for (String[] arr: dataList) {
			JSONObject jo = new JSONObject();
			if (arr[0].equals(scenario)) {
				continue;
			}
			else {
				jo.put("name", arr[0]);
				jo.put("risk", arr[1]);
				jo.put("remarks", arr[2]);
				
				JSONObject jom = new JSONObject();
				jom.put("scenario", jo);
				ja.add(jom);
			}
		}
		
		try {
	         FileWriter file = new FileWriter(ODMEEditor.fileLocation  +  "/" + ODMEEditor.projName +  "/scenarios.json");
	         file.write(ja.toJSONString());
	         file.close();
	      } catch (IOException e) {
	         e.printStackTrace();
	      }
		
		DefaultTableModel dm = (DefaultTableModel)dynamic_scenario_table.getModel();
		while(dm.getRowCount() > 0) {
		    dm.removeRow(0);
		}
		
		List<String[]> newDataList = getJsonData();
		
		for (String[] arr: newDataList)
			dynamic_scenario_model.addRow(arr);
    }
    
    private List<String[]> getJsonData() {
    	JSONParser jsonParser = new JSONParser();
    	List<String[]> dataList = new ArrayList<String[]>();
    	
        try (FileReader reader = new FileReader(ODMEEditor.fileLocation +  "/" + ODMEEditor.projName +  "/scenarios.json")){

            Object obj = null;
			try {
				obj = jsonParser.parse(reader);
			} 
			catch (org.json.simple.parser.ParseException e) {
				e.printStackTrace();
			}
			
            JSONArray data = (JSONArray) obj;

            for (Object dtObj:data) {
            	dataList.add(parseObject((JSONObject)dtObj));
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
    	
    	return dataList;
    }
    
    private String[] parseObject(JSONObject obj) {
        JSONObject dataObject = (JSONObject) obj.get("scenario");
        
        String name = (String) dataObject.get("name");   
        String risk = (String) dataObject.get("risk");  
        String remarks = (String) dataObject.get("remarks");
        
        String[] arr = {name, risk, remarks};
        
        return arr;
    }
    
    @SuppressWarnings("unchecked")
	public void updateTableData(String name, String risk, String remarks) {
    	JTextField nameField = new JTextField();
    	JTextField riskField = new JTextField();
    	JTextField remarksField = new JTextField();

    	nameField.setEnabled(false);
    	
    	nameField.setText(name);
    	riskField.setText(risk);
    	remarksField.setText(remarks);


    	Object[] message = {"Scenario Name:", nameField, "Risk:", riskField, "Remarks:", remarksField};

    	int option = JOptionPane
    			.showConfirmDialog(Main.frame, message, "Update Scenario", JOptionPane.OK_CANCEL_OPTION,
    			JOptionPane.PLAIN_MESSAGE);

    	if (option == JOptionPane.OK_OPTION) {
    			name = nameField.getText();
    			risk = riskField.getText();
    			remarks = remarksField.getText();
    			
    			List<String[]> dataList = getJsonData();
    			
    			JSONArray ja = new JSONArray();
    			for (String[] arr: dataList) {
    				JSONObject jo = new JSONObject();
    				if (arr[0].equals(name)) {
    					jo.put("name", name);
    					jo.put("risk", risk);
    					jo.put("remarks", remarks);
    					
    					JSONObject jom = new JSONObject();
    					jom.put("scenario", jo);
    					ja.add(jom);
    				}
    				else {
    					jo.put("name", arr[0]);
    					jo.put("risk", arr[1]);
    					jo.put("remarks", arr[2]);
    					
    					JSONObject jom = new JSONObject();
    					jom.put("scenario", jo);
    					ja.add(jom);
    				}
    			}
    			
    			try {
    		         FileWriter file = new FileWriter(ODMEEditor.fileLocation  +  "/" + ODMEEditor.projName + "/scenarios.json");
    		         file.write(ja.toJSONString());
    		         file.close();
    		      } catch (IOException e) {

    		         e.printStackTrace();
    		      }
    			
    			DefaultTableModel dm = (DefaultTableModel)dynamic_scenario_table.getModel();
    			while(dm.getRowCount() > 0) {
    			    dm.removeRow(0);
    			}
    			
    			List<String[]> newDataList = getJsonData();
    			
    			for (String[] arr: newDataList)
					dynamic_scenario_model.addRow(arr);
    	}	
    }
}
