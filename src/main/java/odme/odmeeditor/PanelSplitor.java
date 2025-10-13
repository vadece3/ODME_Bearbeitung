package odme.odmeeditor;

import odeme.behaviour.Behaviour;

import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;



public class PanelSplitor {
	
    public static int dividerLocation = 0;
    private JSplitPane projectPane, grapConsole, graphtree, variableAndCOnstraint,variableAndBehaviour, variableAndBehaviourAndDistribution, xml, graphVariable;
    
    
    private JSplitPane createSpliPane(int orientation, Component leftComponent,Component rightComponent, int dividerLocation) {
    	
    	JSplitPane pane = new JSplitPane(orientation, leftComponent, rightComponent);
    	pane.setOneTouchExpandable(true);
    	pane.setDividerLocation(dividerLocation); // define project Explorer height
    	pane.setDividerSize(6); // width of the line which split the window
    	pane.setBorder(null);
        
    	return pane;
    }
    
    public JSplitPane addSplitor(ProjectTree projectPanel, DynamicTree treePanel,
								 GraphWindow graphWindow, Console console,
								 Variable scenarioVariable, Distribution scenarioDistribution, Behaviour scenarioBehaviour,
								 InterEntityConstraints scenarioInterEntityConstraints, IntraEntityConstraint scenarioIntraEntityConstraint, JTabbedPane tabbedPane) {
    	
    	projectPane = createSpliPane(JSplitPane.VERTICAL_SPLIT, projectPanel, treePanel, 250);
    	grapConsole = createSpliPane(JSplitPane.VERTICAL_SPLIT, graphWindow, console, 750);
    	graphtree = createSpliPane(JSplitPane.HORIZONTAL_SPLIT, projectPane, grapConsole, 200);
    	graphtreeFunc();
    	variableAndBehaviour =  createSpliPane(JSplitPane.VERTICAL_SPLIT, scenarioVariable, scenarioBehaviour, 150);
		variableAndBehaviourAndDistribution =  createSpliPane(JSplitPane.VERTICAL_SPLIT, variableAndBehaviour, scenarioDistribution, 150);
		JSplitPane allConstraintsPane = createSpliPane(JSplitPane.VERTICAL_SPLIT,
				scenarioInterEntityConstraints, scenarioIntraEntityConstraint, 150);
		variableAndCOnstraint = createSpliPane(JSplitPane.VERTICAL_SPLIT,
				variableAndBehaviourAndDistribution, allConstraintsPane, 150);
    	xml = createSpliPane(JSplitPane.VERTICAL_SPLIT, variableAndCOnstraint, tabbedPane, 300);
    	graphVariable = createSpliPane(JSplitPane.HORIZONTAL_SPLIT, graphtree, xml, 1400);

        return graphVariable;
    }
    
    private void graphtreeFunc() {
    	dividerLocation = graphtree.getDividerLocation();
        graphtree.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
        	@Override
        	public void propertyChange(PropertyChangeEvent evt) {
        		SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                	dividerLocation = graphtree.getDividerLocation();
                	}
                });
        		}
        	});
    }
}
