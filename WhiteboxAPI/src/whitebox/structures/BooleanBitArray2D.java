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

import whitebox.utilities.BitOps;

/**
 * This class is used to create a 2-D array of booleans and is stored using 
 * individual bits, resulting in a much lower memory footprint than would 
 * occur with the equivalent boolean[][] array.
 * @author johnlindsay
 */
public class BooleanBitArray2D {
    private int rows = 0;
    private int columns = 0;
    private int columnsInBytes = 0;
    private byte[][] data;
    
    public BooleanBitArray2D(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        columnsInBytes = (int)(Math.ceil(columns / 8.0));
        data = new byte[rows][columnsInBytes];
    }
    
    public void setValue(int row, int column, boolean value) {
        if (row < 0 || row >= rows || column < 0 || column >= columns) {
            return;
        }
        // which byte-row and byte-col will it be in?
        int colB = column / 8;
        byte colOffset = (byte)(column % 8);
        
        byte val = data[row][colB];
        if (value) {
            val = BitOps.setBit(val, colOffset);
        } else {
            val = BitOps.clearBit(val, colOffset);
        }
        data[row][colB] = val;
    }
    
    public boolean getValue(int row, int column) {
        if (row < 0 || row >= rows || column < 0 || column >= columns) {
            return false;
        }
        // which byte-row and byte-col will it be in?
        int colB = column / 8;
        int colOffset = column % 8;
        
        byte val = data[row][colB];
        return BitOps.checkBit(val, colOffset);
    }
}
