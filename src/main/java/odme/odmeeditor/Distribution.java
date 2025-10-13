package odme.odmeeditor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Collection;

/**
 * <h1>Distribution</h1>
 * <p>
 * Creates a window to show added variables in the SES model. The window
 * contains a table and variables of the selected node are displayed in that
 * table.
 * </p>
 *
 * @author
 * @version
 */
public class Distribution extends JPanel{

    private static final long serialVersionUID = 1L;
    public static JTable table;
    private static DefaultTableModel model;


    public Distribution() {
        setLayout(new GridLayout(1, 0)); // rows,cols

        String[] columnNames =
                {"Node Name", "Variable Name", "Distribution Name", "Details"};
        model = new DefaultTableModel(columnNames, 0);
        table = new JTable();
        table.setModel(model);

        table.setPreferredScrollableViewportSize(new Dimension());
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(true);
        table.setDefaultEditor(Object.class, null);
        table.setSelectionBackground(new Color(217, 237, 146));
        table.setSelectionForeground(new Color(188, 71, 73));

        // row listener
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

//        //modify value in table
//        table.addMouseListener(new MouseAdapter() {
//            public void mouseClicked(MouseEvent e) {
//                // Double click
//                if (e.getClickCount() == 2) {
//                    JTable target = (JTable) e.getSource();
//
//                    Point point = e.getPoint();
//                    int row = table.rowAtPoint(point);
//
//                    String nodeName = (String) target.getModel().getValueAt(0, 0);
//                    String variableName = (String) target.getModel().getValueAt(row, 1);
//                    String meanValue = (String) target.getModel().getValueAt(row, 2);
//                    String varianceValue = (String) target.getModel().getValueAt(row, 3);
//
//
////                    if (variableName != "")
////                        updateTableData(nodeName, variableName, meanValue, varianceValue);
//                }
//            }
//        });

        // Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);

        // Add the scroll pane to this panel.
        add(scrollPane);

        setNullRowsToDistributionTable();
    }

    public static void setNullToAllRows() {
        DefaultTableModel dtm = (DefaultTableModel) table.getModel();
        dtm.setRowCount(0); // for deleting previous table content

        for (int i = 0; i < 100; i++) {
            model.addRow(new Object[] {""});
        }
    }

    public static void setNullRowsToDistributionTable() {
        for (int i = 0; i < 100; i++) {
            model.addRow(new Object[] {"", "", ""});
        }
    }

    public void showNodeValuesInDistributionTable(String nodeName, String[] distributionDetails) {
        DefaultTableModel dtm = (DefaultTableModel) table.getModel();
        dtm.setRowCount(0); // for deleting previous table content
        String[] properties = null;
        int a = 0;

        for (String value : distributionDetails) {
            if (a == 0) {
                    properties = value.split(",");


                    model.addRow(new Object[]{nodeName, properties[0], properties[1], properties[2] });

                a = 1;
            }
        }


        for (String value : distributionDetails) {
            if (a == 1) {
                a = 0;
                continue;
            }
            if (value != null) {

                properties = value.split(",");

                model.addRow(new Object[]{nodeName, properties[0], properties[1], properties[2] });
            }

        }

        setNullRowsToDistributionTable();
    }
}
