/*
 * Copyright (C) 2011 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class GridCell implements Comparable<GridCell> {

    public int row;
    public int col;
    public double z;
    public boolean isRGB = false;
    public double noDataValue;
    public int layerNum = -1;

    public GridCell(int Row, int Col, double Z, double NoData, int LayerNum) {
        row = Row;
        col = Col;
        z = Z;
        noDataValue = NoData;
        layerNum = LayerNum;
    }
    
    public boolean isValueNoData() {
        if (z == noDataValue) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(GridCell cell) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        if (this.z < cell.z) {
            return BEFORE;
        } else if (this.z > cell.z) {
            return AFTER;
        }

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

        return EQUAL;
    }

}
