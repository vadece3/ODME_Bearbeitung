package odme.requirements;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * "Small section on linking to the requirement" for the Behaviour
 * Modeling window (deliverable 2). Scoped to one BT model: lists all
 * project requirements as checkboxes; ticking/unticking immediately
 * writes/removes the {requirement, scenario} pair in tracelinks.xml.
 */
public class LinkingPanel extends JPanel {

    private final String scenarioName;
    private final JLabel countLabel = new JLabel();
    private int total;

    public LinkingPanel(String scenarioName) {
        super(new BorderLayout());
        this.scenarioName = scenarioName;

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        JLabel title = new JLabel("Linked requirements \u2014 " + scenarioName);
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD));
        header.add(title);
        header.add(countLabel);
        add(header, BorderLayout.NORTH);

        List<Requirement> requirements = RequirementsStore.load();
        total = requirements.size();

        if (requirements.isEmpty()) {
            add(new JLabel("  No requirements defined yet \u2014 use the Requirements menu "
                    + "in the main window."), BorderLayout.CENTER);
        } else {
            JPanel boxes = new JPanel(new GridLayout(0, 2, 12, 2));
            for (Requirement r : requirements) {
                String label = r.getId() + "  \u2014  " + shorten(r.getText(), 42);
                JCheckBox cb = new JCheckBox(label,
                        TraceLinksStore.isLinked(r.getId(), scenarioName));
                cb.setToolTipText(r.getText());
                final String reqId = r.getId();
                cb.addItemListener(e -> {
                    if (cb.isSelected()) TraceLinksStore.addLink(reqId, scenarioName);
                    else TraceLinksStore.removeLink(reqId, scenarioName);
                    updateCount();
                });
                boxes.add(cb);
            }
            JScrollPane scroll = new JScrollPane(boxes);
            scroll.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            scroll.setPreferredSize(new Dimension(600, 110));
            add(scroll, BorderLayout.CENTER);
        }
        setBorder(BorderFactory.createTitledBorder(""));
        updateCount();
    }

    private void updateCount() {
        int linked = 0;
        for (TraceLink l : TraceLinksStore.load()) {
            if (l.getScenarioName().equalsIgnoreCase(scenarioName)) linked++;
        }
        countLabel.setText("(" + linked + " of " + total + " linked)");
    }

    private static String shorten(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "\u2026";
    }
}
