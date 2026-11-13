package behaviourtreetograph;

import com.mxgraph.model.mxCell;
import odme.core.FileConvertion;

import odme.odmeeditor.DynamicTree;
import odme.odmeeditor.ODMEEditor;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

//import static behaviourtreetograph.JtreeToGraphModify.modifiyBehaviourXML;
import static behaviourtreetograph.JTreeToGraphBehaviour.benhaviourGraph;
import static odeme.behaviour.BehaviourToTree.selectedScenario;
//import static odme.jtreetograph.JtreeToGraphGeneral.childNodes;
import static odme.jtreetograph.JtreeToGraphVariables.*;
import static odme.odmeeditor.ODMEEditor.fileLocation;

public class JtreeToGraphConvert {

    public static void nodeToRootPathVar(mxCell cell) {
        Object[] incoming = benhaviourGraph.getIncomingEdges(cell);
        if (incoming.length != 0) {
            Object source = benhaviourGraph.getModel().getTerminal(incoming[incoming.length - 1], true);
            mxCell sourceCell = (mxCell) source;
            pathToRoot.add((String) sourceCell.getValue());
            nodeToRootPathVar(sourceCell);
        }
    }


    public static void rootToEndVariableAddition(mxCell varCell) {
        if (varCell.isVertex()) {
            pathToRoot.add((String) varCell.getValue());
            nodeToRootPathVar(varCell);

            String[] stringArray = pathToRoot.toArray(new String[0]);
            ArrayList<String> pathToRootRev = new ArrayList<String>();

            for (int i = stringArray.length - 1; i >= 0; i--) {
                pathToRootRev.add(stringArray[i]);
            }

            String[] stringArrayRev = pathToRootRev.toArray(new String[0]);
            TreePath treePathForVariable = JtreeToGraphGeneral.getTreeNodePath(stringArrayRev);
            
            DefaultMutableTreeNode currentNode =
                    (DefaultMutableTreeNode) (treePathForVariable.getLastPathComponent());

            // -------------------------------------------------------
            TreeNode[] nodes = currentNode.getPath();

            String[] nodesToSelectedNode = new String[100];
            String variables = "";
            int b = 0;

            for (TreePath key : DynamicTree.varMap.keySet()) {
                int a = 0;

                for (String value : DynamicTree.varMap.get(key)) {

                    DefaultMutableTreeNode currentNode2 =
                            (DefaultMutableTreeNode) (key.getLastPathComponent());

                    TreeNode[] nodes2 = currentNode2.getPath();

                    if (nodes.length == nodes2.length) {
                        int aa = 1;
                        for (int i = 0; i < nodes.length; i++) {
                            if (!nodes[i].toString().equals(nodes2[i].toString())) {
                                aa = 0;
                                break;
                            } 
                        }
                        a = aa;
                    }

                    if (a == 1) {
                        nodesToSelectedNode[b] = value;
                        b++;
                    }
                }
            }

            // java 8 way null removal
            nodesToSelectedNode =
                    Arrays.stream(nodesToSelectedNode).filter(s -> (s != null && s.length() > 0))
                            .toArray(String[]::new);

            for (String value : nodesToSelectedNode) {
                variables = variables + "<" + value + "Var/>" + "\n";
            }

            // treePathForVariable
            FileConvertion fileConversion = new FileConvertion();
            fileConversion.variableAdditionToNode(treePathForVariable, variables);

            pathToRoot.clear();
        }
    }

    // start of constraint addition
    public static void rootToEndConstraintAddition(mxCell varCell) {
        if (varCell.isVertex()) {
            String selectedNode = (String) varCell.getValue();

            pathToRoot.add((String) varCell.getValue());
            nodeToRootPathVar(varCell);

            String[] stringArray = pathToRoot.toArray(new String[0]);
            ArrayList<String> pathToRootRev = new ArrayList<String>();

            for (int i = stringArray.length - 1; i >= 0; i--) {
                pathToRootRev.add(stringArray[i]);
            }

            String[] stringArrayRev = pathToRootRev.toArray(new String[0]);

            TreePath treePathForVariable = JtreeToGraphGeneral.getTreeNodePath(stringArrayRev);
            DefaultMutableTreeNode currentNode =
                    (DefaultMutableTreeNode) (treePathForVariable.getLastPathComponent());

            // -------------------------------------------------------
            TreeNode[] nodes = currentNode.getPath();

            String[] nodesToSelectedNode = new String[100];

            String variables = "";
            int b = 0;

            for (TreePath key : DynamicTree.interConstraintsList.keySet()) {
                int a = 0;

                for (String value : DynamicTree.interConstraintsList.get(key)) {
                    DefaultMutableTreeNode currentNode2 =
                            (DefaultMutableTreeNode) (key.getLastPathComponent());

                    TreeNode[] nodes2 = currentNode2.getPath();

                    if (nodes.length == nodes2.length) {
                        int aa = 1;
                        for (int i = 0; i < nodes.length; i++) {
                            if (!nodes[i].toString().equals(nodes2[i].toString())) {
                                aa = 0;
                                break;
                            } 
                        }
                        a = aa;
                    }

                    if (a == 1) {
                        nodesToSelectedNode[b] = value;
                        b++;
                    }
                }
            }

            // java 8 way null removal
            nodesToSelectedNode =
                    Arrays.stream(nodesToSelectedNode).filter(s -> (s != null && s.length() > 0))
                            .toArray(String[]::new);

            for (String value : nodesToSelectedNode) {
                variables = variables + "<" + value + "Con/>" + "\n";
            }

            // treePathForVariable
            if (variables.length() > 8) {
                FileConvertion fileConversion = new FileConvertion();
                fileConversion.constraintAdditionToNode(selectedNode,
                        variables); // sending treePath is correct way, the
                // above one. have to check.
            }
            pathToRoot.clear();
        }
    }
    // end of solving sequence problem--------------------------------------
    
    public static void nodeToRootPath(mxCell cell) {
        Object[] incoming = benhaviourGraph.getIncomingEdges(cell);
        if (incoming.length != 0) {
            Object source = benhaviourGraph.getModel().getTerminal(incoming[incoming.length - 1], true);
            mxCell sourceCell = (mxCell) source;
            path.add((String) sourceCell.getValue());
            nodeToRootPath(sourceCell);
        }
    }

    public static void nodeToRootPathBehavior(mxCell cell) {
        Object[] incoming = benhaviourGraph.getIncomingEdges(cell);
        if (incoming.length != 0) {
            Object source = benhaviourGraph.getModel().getTerminal(incoming[incoming.length - 1], true);
            mxCell sourceCell = (mxCell) source;
            behaviourPath.add((String) sourceCell.getValue());
            System.out.println("JtreeToGraphConvert in nodeToRootPathBehavior path =  " + behaviourPath.toString());
            nodeToRootPathBehavior(sourceCell);
        }
    }

 // for solving sequence problem--------------------------------------
    public static void rootToEndNodeSequenceSolve() {
        Object[] cells = benhaviourGraph.getChildVertices(benhaviourGraph.getDefaultParent());
        mxCell rootcell = null;

        for (Object c : cells) {
            mxCell cell2 = (mxCell) c; // casting
            if (cell2.isVertex()) {
                String val = cell2.getId();
                if (val.equals("rootnode")) {
                    rootcell = cell2;
                }
            }
        } // end of for

        if (rootcell != null) {
            JtreeToGraphNext.nextChild(rootcell);
        }
    }
    
 // this function is used by other caller for inserting node in specific position
    public static mxCell rootToSelectedNode(String[] nodesToSelectedNodeOrg) {
        nodesToSelectedNode = nodesToSelectedNodeOrg;
        totalNodes = nodesToSelectedNode.length;
        nodeReached = 1; // 1 because rootnode is reached by default

        Object[] cells = benhaviourGraph.getChildVertices(benhaviourGraph.getDefaultParent());
        mxCell rootcell = null;

        for (Object c : cells) {
            mxCell cell2 = (mxCell) c; // casting
            if (cell2.isVertex()) {
                String val = cell2.getId();
                if (val.equals("rootnode")) {
                    rootcell = cell2;
                }

            }
        } // end of for

        if (rootcell != null) {
            JtreeToGraphNext.nextChildNodeInPath(rootcell); // important func call
        }

        if (totalNodes == 1) {
            return rootcell;
        } 
        else {
            return lastNodeInPath;
        }
    }

    //check it later now i comment it and use previous code
    public static void behaviourGraphToXML() {
        Object[] cells = benhaviourGraph.getChildVertices(benhaviourGraph.getDefaultParent());
        mxCell rootcell = null;

        Object cell = benhaviourGraph.getDefaultParent();
        mxCell ce = (mxCell)cell;

//        because cells donot have root value therefore it is null set root value first then call this function.
        for (Object c : cells) {
            mxCell cell2 = (mxCell) c; // casting
            if (cell2.isVertex()) {
                String val = cell2.getId();
                if (val.equals("rootnode")) {
                    rootcell = cell2;
                }
            }
        }

        Document calendarDOMDoc = null;
        try {
            DOMImplementation domImpl =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();

            calendarDOMDoc = domImpl.createDocument(null, "start", null);

        }
        catch (ParserConfigurationException e1) {
            e1.printStackTrace(System.err);
        }
        catch (DOMException e2) {
            e2.printStackTrace(System.err);
        }

        calendarDOMDoc.getDocumentElement().appendChild(JtreeToGraphGeneral.behaviourChildNodes(calendarDOMDoc, rootcell));

        try {
            String path = new String();

            path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName +  "/" + selectedScenario   + "/behaviourxml.xml";

            JtreeToGraphSave.saveToXMLFile(calendarDOMDoc, path);
        }
        catch (TransformerException ex) {
            Logger.getLogger(ODMEEditor.class.getName()).log(Level.SEVERE, null, ex);
        }

        // converting XML tags according to requirement.
        JtreeToGraphModify.modifiyXMLFile();

    }

    /**
     * Merges the pruned scenario (xmlforxsd.xml) with the saved behaviour tree
     * (OutputBehaviourxml.xml) into MergedXMLforxsd.xml, then re-tags it with the
     * traced requirements. The merge is done on parsed XML (see ScenarioBehaviourMerger);
     * the old version concatenated text lines and could produce empty or broken files.
     */
    public static boolean mergeBehaviourWithXSD(){
        String folder = fileLocation + "/" + ODMEEditor.projName + "/" + selectedScenario;
        File scenarioXml = new File(folder + "/xmlforxsd.xml");
        File behaviourXml = new File(folder + "/OutputBehaviourxml.xml");
        File mergedXml = new File(folder + "/MergedXMLforxsd.xml");

        ScenarioBehaviourMerger.Result result = ScenarioBehaviourMerger.merge(scenarioXml, behaviourXml, mergedXml);
        System.out.println(result.message);

        if (!result.ok) {
            javax.swing.JOptionPane.showMessageDialog(null, result.message, "Merge",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            return false;
        }

        odme.requirements.RequirementTagger.retagMergedFile(selectedScenario);
        return true;
    }

}
