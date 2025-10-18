/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package odme.contextmenus;

import com.mxgraph.model.mxCell;

import odme.jtreetograph.JtreeToGraphAdd;
import odme.jtreetograph.JtreeToGraphCheck;
import odme.jtreetograph.JtreeToGraphDelete;
import odme.jtreetograph.JtreeToGraphGeneral;
import odme.jtreetograph.JtreeToGraphPrune;
import odme.jtreetograph.JtreeToGraphVariables;
import odme.odmeeditor.ODMEEditor;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * <h1>GraphCellPopUp</h1>
 * <p>
 * This class implements right click action of the mouse on the node in the
 * graphical editor. It initiates all the mouse actions such as add variable,
 * delete variable, rename, delete node etc which are used in the graphical
 * editor for modifying SES nodes.
 * </p>
 *
 * @author ---
 * @version ---
 */
public class GraphCellPopUp extends JPopupMenu {

	private static final long serialVersionUID = 1L;

	public GraphCellPopUp(Object pos) {

    	String [] items = buildMenuItems(pos);
    	JMenuItem item;

    	if (ODMEEditor.toolMode == "ses") {
    		for (int i=0; i<items.length; i++) {
    			if (items[i]  == null)
    				continue;
				//adding the Distribution menu which containes a submenu
				if ("Add Distribution".equals(items[i])) {
					JMenu distributionMenu = new JMenu("Add Distribution");
					addDistributionSubMenu(distributionMenu, pos);
					add(distributionMenu);
				} else {
					item = new JMenuItem(items[i]);

					add(item);
					if (i < items.length - 1 && items[i + 1] != null)
						add(new JSeparator());

					item.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							switch (ae.getActionCommand()) {
								case "Add Variable":
									JtreeToGraphAdd.addVariableFromGraphPopup(pos);
									break;
								case "Add Behaviour":
									JtreeToGraphAdd.addBehaviourFromGraphPopup(pos);
									break;
								case "Delete Variable":
									JtreeToGraphDelete.deleteVariableFromGraphPopup(pos);
									break;
								case "Delete All Variables":
									JtreeToGraphDelete.deleteAllVariablesFromGraphPopup(pos);
									break;
								case "Rename":
									JtreeToGraphGeneral.renameCell(pos);
									break;
								case "Delete Branch":
									JtreeToGraphDelete.deleteNodeFromGraphPopup(pos);
									break;
								case "Add Module":
									JtreeToGraphAdd.addModuleFromOtherModelAsXML(pos);
									break;
								case "Save Module":
									JtreeToGraphGeneral.writeSaveModuleToFileAsXML(pos);
									break;
								case "Add Inter-Entity Constraint...":
									JtreeToGraphAdd.addInterEntityConstraintFromGraphPopup(pos);
									break;
								case "Delete All Inter-Entity Constraints":
									JtreeToGraphDelete.deleteInterEntityConstraintFromGraphPopup(pos);
									break;
								case "Add Intra-Entity Constraint...":
									JtreeToGraphAdd.addIntraEntityConstraintFromGraphPopup(pos);
									break;
								case "Delete All Intra-Entity Constraints":
									JtreeToGraphDelete.deleteAllIntraEntityConstraintFromGraphPopup(pos);
									break;

								case "Delete Edge":
									JtreeToGraphDelete.deleteEdgeFromGraphPopup(pos);
									break;
							}
						}
					});
				}
    		}
    	}

    	else {
    		JMenuItem itemPrune = new JMenuItem("Prune It");

    		mxCell cell = (mxCell) pos;
    		String cellName = (String) cell.getValue();

    		if (!cellName.endsWith("Dec"))
    				add(itemPrune);

    		itemPrune.addActionListener(new ActionListener() {
    			public void actionPerformed(ActionEvent ae) {
    				if (cellName.endsWith("Dec")) {
    				}
    				else if (cellName.endsWith("MAsp")) {

    					JtreeToGraphPrune.pruneMAspNodeFromGraphPopup(pos);
    				}
    				else if (cellName.endsWith("Spec")) {
    					JtreeToGraphPrune.pruneNodeFromGraphPopup(pos);
    				}
    				else {
    					JtreeToGraphPrune.pruneSiblingsFromGraphPopup(pos);
    				}
    			}
    		});
    	}
    }

	public void addDistributionSubMenu(JMenu parentMenu, Object pos) {
		// First-level submenu items
		JMenuItem normalDist = new JMenuItem("Normal Distribution");
		JMenuItem uniformDist = new JMenuItem("Uniform Distribution");

//		// Example of a nested submenu
//		JMenu advancedMenu = new JMenu("Advanced");
//		JMenuItem betaDist = new JMenuItem("Beta Distribution");
//		JMenuItem gammaDist = new JMenuItem("Gamma Distribution");
//
//		advancedMenu.add(betaDist);
//		advancedMenu.add(gammaDist);

		// Add actions
		normalDist.addActionListener(e -> {
			JtreeToGraphAdd.addNormalDistribution(pos);
		});

		uniformDist.addActionListener(e -> {
				JtreeToGraphAdd.addUniformDistribution(pos);
		});

		// Add everything into the "Add Distribution" menu
		parentMenu.add(normalDist);
		parentMenu.add(uniformDist);
		parentMenu.addSeparator();
//		parentMenu.add(advancedMenu); // submenu inside submenu
	}


	private String[] buildMenuItems(Object pos) {
    	mxCell cell = (mxCell) pos;
        String cellName = (String) cell.getValue();
        boolean connected = JtreeToGraphCheck.isConnectedToRoot(cell);
        JtreeToGraphVariables.connectedToRoot = false; // have to assign false because isConnectedToRoot() function
        // assign true during calling

    	String[] items = new String[11];

    	if (cell.isVertex()) {
    		if (cell.getId().startsWith("uniformity") && connected) {
    			if (cell.getId().endsWith("RefNode"))
    				items[0] = "Delete";
    		}
    		else {
    			items[0] = "Add Variable";
    			items[1] = "Rename";
    			items[2] = "Delete Variable";
    			items[3] = "Delete All Variables";
    			items[4] = "Delete Branch";
    			items[5] = "Add Module";
    			items[6] = "Save Module";
    			items[7] = "Add Behaviour";
				items[10] = "Add Distribution";

				if (cellName.endsWith("Dec") || cellName.endsWith("Spec") || cellName.endsWith("MAsp")) {
//					items[8] = "Add Inter-Entity Constraint...";
//					items[9] = "Delete All Inter-Entity Constraints";
				}
				else  {
					items[8] = "Add Intra-Entity Constraint...";
					items[9] = "Delete All Intra-Entity Constraints";
				}
    		}
    	}
    	else
    		items[0] = "Delete Edge";

    	return items;
    }
}
