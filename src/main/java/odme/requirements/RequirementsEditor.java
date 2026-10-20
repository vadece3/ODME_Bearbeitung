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
import javax.swing.JComboBox;
import javax.swing.JDialog;
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
import odme.requirements.OperationalScenariosStore.OperationalScenario;

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
        JButton osBtn = new JButton("Operational Scenarios...");

        osBtn.addActionListener(e -> manageOperationalScenarios());
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
        right.add(osBtn);
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
        List<String> osIds = OperationalScenariosStore.ids();
        for (Requirement r : requirements) {
            List<String> scenarios = TraceLinksStore.scenariosFor(r.getId());
            String coveredText;
            if (scenarios.isEmpty()) {
                coveredText = "NOT COVERED";
            } else {
                coveredText = String.join(", ", scenarios);
                covered++;
            }
            String sourceText = osIds.contains(r.getSource())
                    ? r.getSource() : r.getSource() + " (undefined OS)";
            model.addRow(new Object[] { r.getId(), r.getText(), sourceText, coveredText });
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
        List<String> osIds = OperationalScenariosStore.ids();
        if (osIds.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "No operational scenario is defined yet.\n"
                            + "Every requirement must originate from one - define it first\n"
                            + "via 'Operational Scenarios...'.",
                    "Provenance", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        JComboBox<String> sourceBox = new JComboBox<>(osIds.toArray(new String[0]));
        if (existing != null && osIds.contains(existing.getSource())) {
            sourceBox.setSelectedItem(existing.getSource());
        }

        Object[] message = {
                "ID:", idField,
                "Requirement text:", new JScrollPane(textArea),
                "Source (operational scenario):", sourceBox };

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
        return new Requirement(id, text, String.valueOf(sourceBox.getSelectedItem()));
    }

    // ------------------------------------------------------------------
    // Operational scenarios: the citable origin of every requirement
    // (chain OS -> REQ -> BT model). Stored in <project>/operationalscenarios.xml.
    // ------------------------------------------------------------------

    private void manageOperationalScenarios() {
        final List<OperationalScenario> list = new ArrayList<>(OperationalScenariosStore.load());

        final DefaultTableModel osModel = new DefaultTableModel(
                new String[] { "ID", "Operational scenario (full text)" }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        final JTable osTable = new JTable(osModel);
        osTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        osTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        osTable.getColumnModel().getColumn(1).setPreferredWidth(620);

        final Runnable fill = () -> {
            osModel.setRowCount(0);
            for (OperationalScenario os : list) {
                osModel.addRow(new Object[] { os.getId(), os.getText() });
            }
        };
        fill.run();

        final JDialog dialog = new JDialog(frame,
                "Operational Scenarios - " + ODMEEditor.projName, true);

        JButton addOs = new JButton("Add");
        JButton editOs = new JButton("Edit");
        JButton deleteOs = new JButton("Delete");
        JButton saveOs = new JButton("Save");
        JButton closeOs = new JButton("Close");

        addOs.addActionListener(e -> {
            OperationalScenario os = showOsDialog(dialog, null, list);
            if (os != null) {
                list.add(os);
                fill.run();
            }
        });
        editOs.addActionListener(e -> {
            int row = osTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(dialog, "Select an operational scenario first.");
                return;
            }
            OperationalScenario existing = list.get(row);
            OperationalScenario edited = showOsDialog(dialog, existing, list);
            if (edited == null) return;
            if (!existing.getId().equals(edited.getId())
                    && !requirementsUsing(existing.getId()).isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        existing.getId() + " is referenced by "
                                + String.join(", ", requirementsUsing(existing.getId()))
                                + " - the id cannot be renamed.", "Provenance",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            existing.setId(edited.getId());
            existing.setText(edited.getText());
            fill.run();
        });
        deleteOs.addActionListener(e -> {
            int row = osTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(dialog, "Select an operational scenario first.");
                return;
            }
            String id = list.get(row).getId();
            List<String> users = requirementsUsing(id);
            if (!users.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        id + " is referenced by " + String.join(", ", users)
                                + " - delete or re-source those requirements first.",
                        "Provenance", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int ok = JOptionPane.showConfirmDialog(dialog, "Delete " + id + "?",
                    "Delete", JOptionPane.OK_CANCEL_OPTION);
            if (ok == JOptionPane.OK_OPTION) {
                list.remove(row);
                fill.run();
            }
        });
        saveOs.addActionListener(e -> {
            if (OperationalScenariosStore.save(list)) {
                JOptionPane.showMessageDialog(dialog, "Operational scenarios saved to:\n"
                        + OperationalScenariosStore.osFile().getAbsolutePath());
                refreshTable();   // Source column: undefined-OS markers disappear
            } else {
                JOptionPane.showMessageDialog(dialog, "Saving failed - see console.",
                        "Save", JOptionPane.ERROR_MESSAGE);
            }
        });
        closeOs.addActionListener(e -> dialog.dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(addOs);
        buttons.add(editOs);
        buttons.add(deleteOs);
        buttons.add(saveOs);
        buttons.add(closeOs);

        JScrollPane scroll = new JScrollPane(osTable);
        scroll.setPreferredSize(new Dimension(700, 200));
        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private OperationalScenario showOsDialog(JDialog owner, OperationalScenario existing,
                                             List<OperationalScenario> list) {
        JTextField idField = new JTextField(existing == null ? nextOsId(list) : existing.getId());
        JTextArea textArea = new JTextArea(existing == null ? "" : existing.getText(), 8, 50);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        Object[] message = {
                "ID:", idField,
                "Operational scenario (full natural-language text):", new JScrollPane(textArea) };

        int option = JOptionPane.showConfirmDialog(owner, message,
                existing == null ? "Add Operational Scenario" : "Edit Operational Scenario",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option != JOptionPane.OK_OPTION) return null;

        String id = idField.getText().trim();
        String text = textArea.getText().trim();
        if (id.isEmpty() || text.isEmpty()) {
            JOptionPane.showMessageDialog(owner, "ID and text must not be empty.");
            return null;
        }
        for (OperationalScenario other : list) {
            if (other != existing && other.getId().equalsIgnoreCase(id)) {
                JOptionPane.showMessageDialog(owner, "ID " + id + " already exists.");
                return null;
            }
        }
        return new OperationalScenario(id, text);
    }

    /** Ids of the requirements whose source is the given operational scenario. */
    private List<String> requirementsUsing(String osId) {
        List<String> result = new ArrayList<>();
        for (Requirement r : requirements) {
            if (osId.equalsIgnoreCase(r.getSource())) result.add(r.getId());
        }
        return result;
    }

    private String nextOsId(List<OperationalScenario> list) {
        int max = 0;
        for (OperationalScenario os : list) {
            String s = os.getId().replaceAll("[^0-9]", "");
            try { max = Math.max(max, Integer.parseInt(s)); }
            catch (NumberFormatException ignored) { }
        }
        return "OS-" + (max + 1);
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