/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package whitebox.structures;

/**
 *
 * @author johnlindsay
 */
public class RowPriorityGridCell implements Comparable<RowPriorityGridCell> {

    public int row;
    public int col;
    public double z;
    
    public RowPriorityGridCell(int row, int col, double z) {
        this.row = row;
        this.col = col;
        this.z = z;
    }

    @Override
    public int compareTo(RowPriorityGridCell cell) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        if (this.row < cell.row) {
            return BEFORE;
        } else if (this.row > cell.row) {
            return AFTER;
        }

        if (this.col < cell.col) {
            return BEFORE;
        } else if (this.col > cell.col) {
            return AFTER;
        }
        
        if (this.z < cell.z) {
            return BEFORE;
        } else if (this.z > cell.z) {
            return AFTER;
        }
        
        return EQUAL;
    }

}