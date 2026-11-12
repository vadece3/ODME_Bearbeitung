package odeme.behaviour;

import behaviourtreetograph.JTreeToGraphBehaviour;
import behaviourtreetograph.JtreeToGraphConvert;
import com.mxgraph.util.mxCellRenderer;
import odme.odmeeditor.ODMEEditor;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Menu bar of the behaviour tree window.
 * (Before, addMenu() created an empty "File" menu: none of the items existed.)
 */
public class MenuBarBehaviour {

	private final JMenuBar menuBar;
	private final JFrame frame;

	public MenuBarBehaviour(JFrame frame) {
		this.frame = frame;
		menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
	}

	public void show() {
		JMenu file = new JMenu("File");
		file.setMnemonic(KeyEvent.VK_F);
		file.setBorder(new EmptyBorder(10, 20, 10, 20));

		file.add(item("Open Behaviour Tree...", "control O", "open_icon", e -> openBehaviourTree()));
		file.addSeparator();
		file.add(item("Save", "control S", "save_icon", e -> save(true)));
		file.add(item("Export Behaviour XML...", null, "save_icon", e -> exportXml()));
		file.add(item("Save as PNG", null, "png_icon", e -> saveAsPng()));
		file.addSeparator();
		file.add(item("Close Window", null, "exit_icon", e -> frame.dispose()));

		menuBar.add(file);
	}

	private JMenuItem item(String text, String accelerator, String icon,
						   java.awt.event.ActionListener action) {
		JMenuItem menuItem = new JMenuItem(text);
		if (accelerator != null) {
			menuItem.setAccelerator(KeyStroke.getKeyStroke(accelerator));
		}
		URL imageUrl = ODMEEditor.class.getResource("/images/" + icon + ".png");
		if (imageUrl != null) {
			Image img = new ImageIcon(imageUrl).getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
			menuItem.setIcon(new ImageIcon(img));
		}
		menuItem.addActionListener(action);
		return menuItem;
	}

	/** Folder of the scenario whose behaviour tree is open. */
	private File scenarioFolder() {
		return new File(ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/" + BehaviourToTree.selectedScenario);
	}

	/**
	 * Loads a saved behaviour tree (*behaviourGraph.xml) onto the canvas, e.g. to reuse
	 * the tree of another scenario. It is only written to the open scenario when you Save.
	 */
	private void openBehaviourTree() {
		JFileChooser chooser = new JFileChooser(new File(ODMEEditor.fileLocation + "/" + ODMEEditor.projName));
		chooser.setDialogTitle("Open Behaviour Tree");
		chooser.setFileFilter(new FileNameExtensionFilter("ODME behaviour graph (*behaviourGraph.xml)", "xml"));
		if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File chosen = chooser.getSelectedFile();

		int answer = JOptionPane.showConfirmDialog(frame,
				"Replace the tree on the canvas with\n" + chosen.getName() + " ?\n\n"
						+ "Unsaved changes on the canvas will be lost. The loaded tree is saved to scenario '"
						+ BehaviourToTree.selectedScenario + "' only when you click Save.",
				"Open Behaviour Tree", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (answer != JOptionPane.OK_OPTION) {
			return;
		}
		try {
			JTreeToGraphBehaviour.loadBehaviourGraph(chosen);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(frame, "Could not open the behaviour tree:\n" + ex.getMessage(),
					"Open Behaviour Tree", JOptionPane.ERROR_MESSAGE);
		}
	}

	/** Same as the "Save Graph" toolbar button. */
	private void save(boolean showMessage) {
		ODMEBehaviourEditor.treePanel.saveTreeModel();
		JtreeToGraphConvert.behaviourGraphToXML();
		if (showMessage) {
			JOptionPane.showMessageDialog(frame, "Saved Successfully.", "Save", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	/** Saves, then copies the behaviour tree XML (OutputBehaviourxml.xml) to a file the user chooses. */
	private void exportXml() {
		save(false);
		File source = new File(scenarioFolder(), "OutputBehaviourxml.xml");
		if (!source.exists() || source.length() == 0) {
			JOptionPane.showMessageDialog(frame, "No behaviour tree XML found:\n" + source.getPath(),
					"Export", JOptionPane.WARNING_MESSAGE);
			return;
		}
		JFileChooser chooser = new JFileChooser(scenarioFolder());
		chooser.setDialogTitle("Export Behaviour Tree XML");
		chooser.setFileFilter(new FileNameExtensionFilter("XML Files (*.xml)", "xml"));
		chooser.setSelectedFile(new File(BehaviourToTree.selectedScenario + "_behaviourTree.xml"));
		if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File target = chooser.getSelectedFile();
		if (!target.getName().toLowerCase().endsWith(".xml")) {
			target = new File(target.getPath() + ".xml");
		}
		try {
			Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
			JOptionPane.showMessageDialog(frame, "Exported to:\n" + target.getPath(), "Export",
					JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(frame, "Export failed: " + ex.getMessage(), "Export",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	/** Saves the drawn behaviour tree as <scenario>/behaviourTree.png. */
	private void saveAsPng() {
		try {
			// Render only the real tree: leave out the two invisible "End of Canvas" cells
			// (ids hideV / hideH at 50000 px), otherwise the picture would be 50000 px wide.
			com.mxgraph.view.mxGraph g = JTreeToGraphBehaviour.benhaviourGraph;
			java.util.List<Object> cells = new java.util.ArrayList<>();
			for (Object c : g.getChildCells(g.getDefaultParent())) {
				String id = ((com.mxgraph.model.mxCell) c).getId();
				if (!"hideV".equals(id) && !"hideH".equals(id)) {
					cells.add(c);
				}
			}
			BufferedImage image = mxCellRenderer.createBufferedImage(g, cells.toArray(), 1, Color.WHITE, true, null);
			if (image == null) {
				JOptionPane.showMessageDialog(frame, "The behaviour tree is empty.", "Save PNG",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			File out = new File(scenarioFolder(), "behaviourTree.png");
			ImageIO.write(image, "PNG", out);
			JOptionPane.showMessageDialog(frame, "Saved to:\n" + out.getPath(), "Save PNG",
					JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage(), "Save PNG",
					JOptionPane.ERROR_MESSAGE);
		}
	}
}
