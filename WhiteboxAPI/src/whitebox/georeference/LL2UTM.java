/*
 * Copyright (C) 2013 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
package whitebox.georeference;

/*
 * Based on Steven Dutch (2010) http://www.uwgb.edu/dutchs/UsefulData/ConvertUTMNoOZ.HTM;
 * 
 * The conversion from latitude and longitude to UTM is probably accurate to a 
 * meter or so. The reverse conversion is a bit less accurate, but is 
 * generally accurate within ten meters.
 * 
 * Check accuracy for your purposes by inputting one set of coordinates, then 
 * converting back. See how close the values are to the original values
 */
/**
 *
 * @author johnlindsay
 */
public class LL2UTM {
    // Global variables

    //private static final double deg2Rad = Math.PI / 180;
    private final double drad = Math.PI / 180;
    private Ellipsoid ellipsoid;
    private double easting;
    private double northing;
    private int zone = -1;
    private String hemisphere;
    boolean updateZone = true;
    
    // Constructors
    public LL2UTM() {
        // no-args constructor
    }

    public LL2UTM(Ellipsoid ellipsoid) {
        this.ellipsoid = ellipsoid;
    }

    // Properties
    public Ellipsoid getEllipsoid() {
        return ellipsoid;
    }

    public void setEllipsoid(Ellipsoid ellipsoid) {
        this.ellipsoid = ellipsoid;
    }

    private void handleError() {
        easting = -1;
        northing = -1;
        zone = -1;
        hemisphere = "none";
    }
    
    public double getEasting() {
        return easting;
    }
    
    public double getNorthing() {
        return northing;
    }
    
    public int getZone() {
        return zone;
    }
    
    public String getHemisphere() {
        return hemisphere;
    }

    public boolean isZoneLocked() {
        return updateZone;
    }

    public void lockZone() {
        this.updateZone = false;
    }
    
    public void unlockZone() {
        this.updateZone = true;
    }
    
    // Methods
    public void convertLatLongDecDegrees(double latitude, double longitude) {

        if (latitude < -90 || latitude > 90) {
            handleError();
            return;
        }
        if (longitude < -180 || longitude > 180) {
            handleError();
            return;
        }
        
        hemisphere = "N";
        if (latitude < 0) {
            hemisphere = "S";
        }

        double a, b, f, e, k0, x, y, T, M, C, N, A, M0, phi, lng, utmz, e0, esq, e0sq, zcm;
                
        //double latz, yg, ym, ys, ydd, xdd, xs, xm;
        //Convert Latitude and Longitude to UTM
        k0 = 0.9996; //scale on central meridian
        a = ellipsoid.majorAxis();
        b = ellipsoid.minorAxis();
        e = ellipsoid.firstEccentricity();

        phi = latitude * drad;//Convert latitude to radians
        lng = longitude * drad;//Convert longitude to radians
        if (zone < 0 || updateZone) {
            zone = (int)(1 + Math.floor((longitude + 180) / 6.0));//calculate utm zone
        }
//        latz = 0;//Latitude zone: A-B S of -80, C-W -80 to +72, X 72-84, Y,Z N of 84
//        if (latitude > -80 && latitude < 72) {
//            latz = Math.floor((latitude + 80) / 8) + 2;
//        }
//        if (latitude > 72 && latitude < 84) {
//            latz = 21;
//        }
//        if (latitude > 84) {
//            latz = 23;
//        }

        zcm = 3 + 6 * (zone - 1) - 180;//Central meridian of zone
        //Calculate Intermediate Terms
        e0 = e / Math.sqrt(1 - e * e);//Called e prime in reference
        esq = (1 - (b / a) * (b / a));//e squared for use in expansions
        e0sq = e * e / (1 - e * e);// e0 squared - always even powers
        N = a / Math.sqrt(1 - Math.pow(e * Math.sin(phi), 2));
        T = Math.pow(Math.tan(phi), 2);
        C = e0sq * Math.pow(Math.cos(phi), 2);
        A = (longitude - zcm) * drad * Math.cos(phi);
        //Calculate M
        M = phi * (1 - esq * (1 / 4.0 + esq * (3 / 64.0 + 5 * esq / 256.0)));
        M = M - Math.sin(2 * phi) * (esq * (3 / 8.0 + esq * (3 / 32.0 + 45 * esq / 1024.0)));
        M = M + Math.sin(4 * phi) * (esq * esq * (15 / 256.0 + esq * 45 / 1024.0));
        M = M - Math.sin(6 * phi) * (esq * esq * esq * (35 / 3072.0));
        M = M * a;//Arc length along standard meridian
        M0 = 0;//M0 is M for some origin latitude other than zero. Not needed for standard UTM
        //Calculate UTM Values
        x = k0 * N * A * (1 + A * A * ((1 - T + C) / 6.0 + A * A * (5 - 18 * T + T * T + 72 * C - 58 * e0sq) / 120.0));//Easting relative to CM
        x = x + 500000;//Easting standard 
        y = k0 * (M - M0 + N * Math.tan(phi) * (A * A * (1 / 2.0 + A * A * ((5 - T + 9 * C + 4 * C * C) / 24.0 + A * A * (61 - 58 * T + T * T + 600 * C - 330 * e0sq) / 720.0))));//Northing from equator
//        yg = y + 10000000;//yg = y global, from S. Pole
        if (y < 0) {
            y = 10000000 + y;
        }
        
        easting = x;
        northing = y;
        
    }

    public static void main(String[] args) {
        LL2UTM ll2utm = new LL2UTM(Ellipsoid.WGS_84);

        double lat = 40.5;
        double lon = -73.5;
//        ll2utm.convertLatLongDecDegrees(lat, lon);
//        System.out.println("Easting: " + ll2utm.easting + "\tNorthing: " + ll2utm.northing
//                + "\tzone " + ll2utm.zone + ll2utm.hemisphere);

        lat = 43.5485;
        lon = -80.2503;
        ll2utm.convertLatLongDecDegrees(lat, lon);
        System.out.println("Easting: " + ll2utm.easting + "\tNorthing: " + ll2utm.northing
                + "\tzone " + ll2utm.zone + ll2utm.hemisphere);

//        lat = -9.02056;
//        lon = -69.608;
//        ll2utm.convertLatLongDecDegrees(lat, lon);
        /*
         * E = 433174.9
         * N = 9002819.2
         * Z = 19
         */
        System.out.println("Easting: " + ll2utm.easting + "\tNorthing: " + ll2utm.northing
                + "\tzone " + ll2utm.zone + ll2utm.hemisphere);
    }
}
