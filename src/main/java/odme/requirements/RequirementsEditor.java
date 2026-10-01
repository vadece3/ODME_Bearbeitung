package odme.requirements;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import odme.odmeeditor.ODMEEditor;

/**
 * Requirements Modeling Editor (deliverable 1), extended with the
 * coverage view and the CSV traceability-matrix export.
 * Coverage is computed live from tracelinks.xml.
 */
public class RequirementsEditor {

    private JFrame frame;
    private JTable table;
    private DefaultTableModel model;
    private JLabel summaryLabel;
    private final List<Requirement> requirements = new ArrayList<>();

    public void open() {
        requirements.clear();
        requirements.addAll(RequirementsStore.load());

        model = new DefaultTableModel(
                new String[] { "ID", "Requirement", "Source", "Covered" }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(model);
        table.setShowVerticalLines(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(380);
        table.getColumnModel().getColumn(2).setPreferredWidth(60);
        table.getColumnModel().getColumn(3).setPreferredWidth(130);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                                                           boolean selected, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(
                        t, value, selected, focus, row, col);
                String covered = String.valueOf(t.getValueAt(row, 3));
                if ("NOT COVERED".equals(covered) && !selected) {
                    c.setForeground(new Color(170, 30, 30));
                } else if (!selected) {
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        });

        JButton addBtn = new JButton("Add");
        JButton editBtn = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");
        JButton saveBtn = new JButton("Save");
        JButton refreshBtn = new JButton("Refresh");
        JButton exportBtn = new JButton("Export Matrix (CSV)");

        addBtn.addActionListener(e -> addRequirement());
        editBtn.addActionListener(e -> editSelected());
        deleteBtn.addActionListener(e -> deleteSelected());
        refreshBtn.addActionListener(e -> refreshTable());
        saveBtn.addActionListener(e -> {
            if (RequirementsStore.save(requirements)) {
                JOptionPane.showMessageDialog(frame, "Requirements saved.");
            } else {
                JOptionPane.showMessageDialog(frame, "Saving failed - see console.",
                        "Save", JOptionPane.ERROR_MESSAGE);
            }
        });
        exportBtn.addActionListener(e -> exportMatrix());

        summaryLabel = new JLabel();

        JPanel south = new JPanel(new BorderLayout());
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        left.add(summaryLabel);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.add(addBtn);
        right.add(editBtn);
        right.add(deleteBtn);
        right.add(saveBtn);
        right.add(refreshBtn);
        right.add(exportBtn);
        south.add(left, BorderLayout.WEST);
        south.add(right, BorderLayout.EAST);

        frame = new JFrame("Requirements - " + ODMEEditor.projName);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(760, 260));
        frame.add(scroll, BorderLayout.CENTER);
        frame.add(south, BorderLayout.SOUTH);
        frame.pack();

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((screen.width - frame.getWidth()) / 2,
                (screen.height - frame.getHeight()) / 2);
        refreshTable();
        frame.setVisible(true);
    }

    /** Rebuilds the table incl. live coverage from tracelinks.xml. */
    private void refreshTable() {
        TraceLinksStore.purgeStaleLinks();
        model.setRowCount(0);
        int covered = 0;
        for (Requirement r : requirements) {
            List<String> scenarios = TraceLinksStore.scenariosFor(r.getId());
            String coveredText;
            if (scenarios.isEmpty()) {
                coveredText = "NOT COVERED";
            } else {
                coveredText = String.join(", ", scenarios);
                covered++;
            }
            model.addRow(new Object[] { r.getId(), r.getText(), r.getSource(), coveredText });
        }
        int total = requirements.size();
        int percent = total == 0 ? 0 : (int) Math.round(100.0 * covered / total);
        List<String> orphans = TraceLinksStore.orphanModels();
        summaryLabel.setText("Coverage: " + covered + " / " + total
                + " (" + percent + "%)   Orphan BT models: "
                + (orphans.isEmpty() ? "none" : String.join(", ", orphans)));
    }

    /** Writes the requirements x BT-models matrix as CSV (';' separated). */
    private void exportMatrix() {
        List<String> models = TraceLinksStore.btModelScenarios();
        for (String s : TraceLinksStore.linkedScenarios()) {
            if (!models.contains(s)) models.add(s);
        }
        File out = new File(ODMEEditor.fileLocation + "/" + ODMEEditor.projName
                + "/traceability_matrix.csv");
        try (PrintWriter w = new PrintWriter(out, "UTF-8")) {
            StringBuilder header = new StringBuilder("Requirement");
            for (String m : models) header.append(';').append(m);
            header.append(";Covered");
            w.println(header);

            for (Requirement r : requirements) {
                StringBuilder line = new StringBuilder(r.getId());
                boolean any = false;
                for (String m : models) {
                    boolean linked = TraceLinksStore.isLinked(r.getId(), m);
                    line.append(';').append(linked ? "X" : "");
                    any = any || linked;
                }
                line.append(';').append(any ? "yes" : "NO");
                w.println(line);
            }

            StringBuilder orphanLine = new StringBuilder("Orphan (no requirement)");
            List<String> orphans = TraceLinksStore.orphanModels();
            for (String m : models) {
                orphanLine.append(';').append(orphans.contains(m) ? "ORPHAN" : "");
            }
            orphanLine.append(';');
            w.println(orphanLine);

            JOptionPane.showMessageDialog(frame,
                    "Matrix exported to:\n" + out.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Export failed - see console.",
                    "Export", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addRequirement() {
        Requirement r = showDialog(null);
        if (r != null) {
            requirements.add(r);
            refreshTable();
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(frame, "Select a requirement first.");
            return;
        }
        Requirement existing = requirements.get(row);
        Requirement edited = showDialog(existing);
        if (edited != null) {
            existing.setId(edited.getId());
            existing.setText(edited.getText());
            existing.setSource(edited.getSource());
            refreshTable();
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(frame, "Select a requirement first.");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(frame,
                "Delete " + requirements.get(row).getId() + "?",
                "Delete", JOptionPane.OK_CANCEL_OPTION);
        if (ok == JOptionPane.OK_OPTION) {
            requirements.remove(row);
            refreshTable();
        }
    }

    private Requirement showDialog(Requirement existing) {
        JTextField idField = new JTextField(existing == null ? nextId() : existing.getId());
        JTextArea textArea = new JTextArea(existing == null ? "" : existing.getText(), 4, 34);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JTextField sourceField = new JTextField(existing == null ? "OS-1" : existing.getSource());

        Object[] message = {
                "ID:", idField,
                "Requirement text:", new JScrollPane(textArea),
                "Source (operational scenario):", sourceField };

        int option = JOptionPane.showConfirmDialog(frame, message,
                existing == null ? "Add Requirement" : "Edit Requirement",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option != JOptionPane.OK_OPTION) return null;

        String id = idField.getText().trim();
        String text = textArea.getText().trim();
        if (id.isEmpty() || text.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "ID and text must not be empty.");
            return null;
        }
        for (Requirement other : requirements) {
            if (other != existing && other.getId().equalsIgnoreCase(id)) {
                JOptionPane.showMessageDialog(frame, "ID " + id + " already exists.");
                return null;
            }
        }
        return new Requirement(id, text, sourceField.getText().trim());
    }

    private String nextId() {
        int max = 0;
        for (Requirement r : requirements) {
            String s = r.getId().replaceAll("[^0-9]", "");
            try { max = Math.max(max, Integer.parseInt(s)); }
            catch (NumberFormatException ignored) { }
        }
        return String.format("REQ-%02d", max + 1);
    }
}