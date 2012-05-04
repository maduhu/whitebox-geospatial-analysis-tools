//package whiteboxgis;
//
//import java.awt.Component;
//import java.awt.event.MouseEvent;
//import java.util.Arrays;
//import java.util.EventObject;
//
//import javax.swing.AbstractCellEditor;
//import javax.swing.JSpinner;
//import javax.swing.JTable;
//import javax.swing.SpinnerListModel;
//import javax.swing.table.TableCellEditor;
//
//class SpinnerEditor extends AbstractCellEditor implements TableCellEditor {
//
//    final JSpinner spinner = new JSpinner();
//
//    public SpinnerEditor(String[] items) {
//        spinner.setModel(new SpinnerListModel(Arrays.asList(items)));
//    }
//
//    @Override
//    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
//            int row, int column) {
//        spinner.setValue(value);
//        return spinner;
//    }
//
//    @Override
//    public boolean isCellEditable(EventObject evt) {
//        if (evt instanceof MouseEvent) {
//            return ((MouseEvent) evt).getClickCount() >= 2;
//        }
//        return true;
//    }
//
//    @Override
//    public Object getCellEditorValue() {
//        return spinner.getValue();
//    }
//}