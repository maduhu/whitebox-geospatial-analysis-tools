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
public class BooleanBitArray1D {
    private int columns = 0;
    private int columnsInBytes = 0;
    private byte[] data;
    
    public BooleanBitArray1D(int size) {
        this.columns = size;
        columnsInBytes = (int)(Math.ceil(columns / 8.0));
        data = new byte[columnsInBytes];
    }
    
    public void setValue(int location, boolean value) {
        if (location < 0 || location >= columns) {
            return;
        }
        // which byte-row and byte-col will it be in?
        int colB = location / 8;
        byte colOffset = (byte)(location % 8);
        
        byte val = data[colB];
        if (value) {
            val = BitOps.setBit(val, colOffset);
        } else {
            val = BitOps.clearBit(val, colOffset);
        }
        data[colB] = val;
    }
    
    public boolean getValue(int location) {
        if (location < 0 || location >= columns) {
            return false;
        }
        // which byte-row and byte-col will it be in?
        int colB = location / 8;
        int colOffset = location % 8;
        
        byte val = data[colB];
        return BitOps.checkBit(val, colOffset);
    }
}
