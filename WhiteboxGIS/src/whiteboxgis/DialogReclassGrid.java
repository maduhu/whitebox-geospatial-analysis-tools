/*
 * Copyright (C) 2011-2012 Dr. John Lindsay <jlindsay@uoguelph.ca>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package whiteboxgis;

import java.awt.event.*;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.Component;
import whitebox.interfaces.DialogComponent;
import whitebox.interfaces.Communicator;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class DialogReclassGrid extends JPanel implements ActionListener, DialogComponent {
    public static final String[] columnNames = {
        "New Value", "From", "To Less Than", ""
    };
    protected JTable table;
    protected JScrollPane scroller;
    protected ReclassTableModel tableModel;
    private int numArgs = 2;
    private String name;
    private String description;
    //private String value = "";
    private Communicator hostDialog = null;
    
    public DialogReclassGrid(Communicator host) {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setMaximumSize(new Dimension(2500, 180));
        this.setPreferredSize(new Dimension(350, 180));
        hostDialog = host;
         
    }
    
    private void createUI() {
        try {
            Border border = BorderFactory.createEmptyBorder(5, 5, 5, 5);
            this.setBorder(border);

            tableModel = new ReclassTableModel(columnNames);
            tableModel.addTableModelListener(new DialogReclassGrid.InteractiveTableModelListener());
            table = new JTable() {

                @Override
                public Component prepareRenderer(TableCellRenderer renderer, int Index_row, int Index_col) {
                    Component comp = super.prepareRenderer(renderer, Index_row, Index_col);
                    //even index, selected or not selected
                    if (Index_row % 2 == 0) {// && !isCellSelected(Index_row, Index_col)) {
                        comp.setBackground(Color.WHITE);
                        comp.setForeground(Color.BLACK);
                    } else {
                        comp.setBackground(new Color(210, 230, 255));
                        comp.setForeground(Color.BLACK);
                    }
                    if (isCellSelected(Index_row, Index_col)) {
                        comp.setForeground(Color.RED);
                    }
                    return comp;
                }
            };
            table.setModel(tableModel);
            table.setSurrendersFocusOnKeystroke(true);
            table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
            table.setAutoCreateRowSorter(true);
            table.setShowGrid(false);
            table.setShowVerticalLines(true);
            table.setShowHorizontalLines(false);
            table.setGridColor(Color.DARK_GRAY);
            if (!tableModel.hasEmptyRow()) {
                tableModel.addEmptyRow();
            }

            scroller = new javax.swing.JScrollPane(table);
            table.setPreferredScrollableViewportSize(new java.awt.Dimension(500, 300));
            TableColumn hidden = table.getColumnModel().getColumn(ReclassTableModel.HIDDEN_INDEX);
            hidden.setMinWidth(2);
            hidden.setPreferredWidth(2);
            hidden.setMaxWidth(2);
            hidden.setCellRenderer(new InteractiveRenderer(ReclassTableModel.HIDDEN_INDEX));

            setLayout(new BorderLayout());
            this.add(scroller, BorderLayout.CENTER);

        } catch (Exception e) {
            System.out.println(e.getCause());
        }
    }
    
    @Override
    public String getValue() {
        String retString = "";
        String str;
        for (int row = 0; row < DialogReclassGrid.this.tableModel.getRowCount(); row++) {
            for (int col = 0; col < 3; col++) {
                str = (String)DialogReclassGrid.this.tableModel.getValueAt(row, col);
                if (row == 0 && col == 0) {
                    if (!str.trim().equals("")) {
                        retString += str;
                    } else {
                        retString += "not specified";
                    }
                } else {
                    if (!str.trim().equals("")) {
                        retString += "\t" + str;
                    } else {
                        retString += "\tnot specified";
                    }
                }
            }
        }
        return retString;
    }
    
    @Override
    public String getComponentName() {
        return name;
    }
    
    @Override
    public boolean getOptionalStatus() {
        return false;
    }
    
    @Override
    public boolean setArgs(String[] args) {
        try {
            // first make sure that there are the right number of args
            if (args.length != numArgs) {
                return false;
            }
            name = args[0];
            description = args[1];
            
            createUI();
            String descriptionAndInstruction = description + ". Press the right-arrow "
                    + "key when in the last column to add a new row.";
            this.setToolTipText(descriptionAndInstruction);
            table.setToolTipText(descriptionAndInstruction);
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String[] getArgsDescriptors() {
        String[] argsDescriptors = new String[numArgs];
        argsDescriptors[0] = "String name";
        argsDescriptors[1] = "String description";
        return argsDescriptors;
    }
    
    private boolean validateValue(String val) {
        String ret = "";
        try {
            if (!val.equals("")) {
                Double dbl = Double.parseDouble(val);
            }
            return true;
        } catch (Exception e) {
            return false;
        }

    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        String actionCommand = e.getActionCommand();
        if (actionCommand.equals("open")) {
               
        }
    }
    
    public void highlightLastRow(int row) {
         int lastrow = tableModel.getRowCount();
         if (row == lastrow - 1) {
             table.setRowSelectionInterval(lastrow - 1, lastrow - 1);
         } else {
             table.setRowSelectionInterval(row + 1, row + 1);
         }

         table.setColumnSelectionInterval(0, 0);
     }
    
    class InteractiveRenderer extends DefaultTableCellRenderer {

        protected int interactiveColumn;

        public InteractiveRenderer(int interactiveColumn) {
            this.interactiveColumn = interactiveColumn;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            Component c = super.getTableCellRendererComponent(table, value, 
                    isSelected, hasFocus, row, column);
            //if (column == interactiveColumn && hasFocus) {
                if ((DialogReclassGrid.this.tableModel.getRowCount() - 1) == row
                        && !DialogReclassGrid.this.tableModel.hasEmptyRow()) {
                    DialogReclassGrid.this.tableModel.addEmptyRow();
                }

                //highlightLastRow(row);
            //}
            
            return c;
        }
    }

    public class InteractiveTableModelListener implements TableModelListener {

        @Override
        public void tableChanged(TableModelEvent evt) {
            if (evt.getType() == TableModelEvent.UPDATE) {
//                int column = evt.getColumn();
//                int row = evt.getFirstRow();
//                String val = (String)DialogReclassGrid.this.tableModel.getValueAt(row, column);
//                if (!validateValue(val)) {
//                    hostDialog.showFeedback("Only numeric values can be entered into the reclass table.");
//                    DialogReclassGrid.this.tableModel.setValueAt("", row, column);
//                }
//                table.setColumnSelectionInterval(column + 1, column + 1);
//                table.setRowSelectionInterval(row, row);
            } else { 
                //System.out.println(evt.getType());
            }
        }
    }
}
