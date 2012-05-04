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
package whitebox.utilities;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public final class BitOps {
    
    // On bytes
    public static byte setBit(byte value, byte pos) {
        return (byte)(value | (1 << pos));
    }
    
    public static boolean checkBit(byte value, byte pos) {
        return ((value >>> pos) & 1) != 0; //((value & (1 << pos)) != 0);
    }
    
    public static byte clearBit(byte value, byte pos) {
        return (byte)(value & ~(1 << pos));
    }
    
    public static byte toggleBit(byte value, byte pos) {
        return (byte)(value ^ (1 << pos));
    }
    
    // On ints
    public static int setBit(int value, int pos) {
        return (int)(value | (1 << pos));
    }
    
    public static boolean checkBit(int value, int pos) {
        return ((value >>> pos) & 1) != 0; //((value & (1 << pos)) != 0);
    }
    
    public static int clearBit(int value, int pos) {
        return (int)(value & ~(1 << pos));
    }
    
    public static int toggleBit(int value, int pos) {
        return (int)(value ^ (1 << pos));
    }
}
