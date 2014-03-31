/*
 * Copyright (C) 2014 Dr. John Lindsay <jlindsay@uoguelph.ca>
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

package whitebox.structures;

/**
 * This class is used to create a 2-D array of 4-bit integers, known as nibbles. Nibbles 
 * can hold the numbers 0-15 and are relatively compact.
 * @author johnlindsay
 */
public class NibbleArray2D {
    private int rows = 0;
    private int columns = 0;
    private int columnsInBytes = 0;
    private byte[][] data;
    
    public NibbleArray2D(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        columnsInBytes = (int)(Math.ceil(columns / 2.0));
        data = new byte[rows][columnsInBytes];
    }
    
    public void setValue(int row, int column, int value) {
        if (row < 0 || row >= rows || column < 0 || column >= columns) {
            return;
        }
        if (value > 15 || value < 0) {
            return;
        }
        // which byte-row and byte-col will it be in?
        int colB = column / 2;
        int colOffset = (column % 2);
        
        byte existingval = data[row][colB];
        byte newVal;
        if (colOffset == 0) {
            newVal = (byte)((existingval & 240) | value);
        } else {
            newVal = (byte)((existingval & 15) | (value << 4));
        }
        data[row][colB] = newVal;
    }
    
    public int getValue(int row, int column) {
        if (row < 0 || row >= rows || column < 0 || column >= columns) {
            return -1;
        }
        // which byte-row and byte-col will it be in?
        int colB = column / 2;
        int colOffset = column % 2;
        
        byte val = data[row][colB];
        if (colOffset == 0) {
            return val & 15;
        } else {
            return (val & 240) >> 4;
        }
    }
    
    public static void main(String[] arg) {
        NibbleArray2D nibble = new NibbleArray2D(100, 100);
        
        nibble.setValue(5, 4, 4);
        nibble.setValue(5, 4, 8);
        nibble.setValue(5, 5, 12);
        nibble.setValue(6, 76, 9);
        
        System.out.println(nibble.getValue(5, 4));
        System.out.println(nibble.getValue(5, 5));
        System.out.println(nibble.getValue(6, 76));
        
    }
}
