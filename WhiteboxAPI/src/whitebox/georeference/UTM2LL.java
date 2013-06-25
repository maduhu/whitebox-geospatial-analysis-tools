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
public class UTM2LL {
    /*
     * Based on Steven Dutch (2010) http://www.uwgb.edu/dutchs/UsefulData/ConvertUTMNoOZ.HTM;
     */
    
    // global variables
    private Ellipsoid ellipsoid;
    private final double drad = Math.PI / 180;
    private double latitude;
    private double longitude;
    
    // constructors
    public UTM2LL() {
        // no-args constructor
    }
    
    
    public UTM2LL(Ellipsoid ellipsoid) {
        this.ellipsoid = ellipsoid;
    }

    // Properties
    public Ellipsoid getEllipsoid() {
        return ellipsoid;
    }

    public void setEllipsoid(Ellipsoid ellipsoid) {
        this.ellipsoid = ellipsoid;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }

    // Methods
    public void convertUTMCoordinates(double x, double y, String utmZone) {
        double M, a, b, f, e, e0, esq, e0sq, zcm, e1, M0, mu, 
                phi1, C1, T1, N1, R1, D, phi, utmz;
        boolean isNorthern = true;
        double k0 = 0.9996;
        double k = 1;//local scale
        
        a = ellipsoid.majorAxis();
        b = ellipsoid.minorAxis();
        e = ellipsoid.firstEccentricity();
        e0 = e / Math.sqrt(1 - e * e);//Called e prime in reference
	esq = (1 - (b / a) * (b/a));//e squared for use in expansions
	e0sq = e * e / (1 - e * e);// e0 squared - always even powers
	if (x < 160000 || x > 840000){
            //Outside permissible range of easting values. Results may be unreliable Use with caution;
            return;
        } 
	if (y < 0) {
            //Negative values not allowed. Results may be unreliable. Use with caution
            return;
        }
	if (y > 10000000) {
            //Northing may not exceed 10,000,000. Results may be unreliable. Use with caution
            return;
        }
	//utmz = parseFloat(document.getElementById("UTMzBox1").value);
        if (utmZone.toLowerCase().contains("n")) {
            isNorthern = true;
            utmz = Double.parseDouble(utmZone.toLowerCase().replace("n", ""));
        } else {
            isNorthern = false;
            utmz = Double.parseDouble(utmZone.toLowerCase().replace("s", ""));
        }
	
	zcm = 3 + 6*(utmz-1) - 180;//Central meridian of zone
	e1 = (1 - Math.sqrt(1 - e*e))/(1 + Math.sqrt(1 - e*e));//Called e1 in USGS PP 1395 also
	M0 = 0;//In case origin other than zero lat - not needed for standard UTM
	M = M0 + y/k0;//Arc length along standard meridian. 
        
        if (!isNorthern){
            M = M0 + (y - 10000000) / k;
        }
	mu = M / (a * (1 - esq * (1 / 4.0 + esq * (3.0 / 64.0 + 5 * esq / 256.0))));
	phi1 = mu + e1*(3.0 / 2.0 - 27*e1*e1 / 32.0)*Math.sin(2*mu) + e1*e1*(21/16 -55*e1*e1/32)*Math.sin(4*mu);//Footprint Latitude
	phi1 = phi1 + e1 * e1 * e1 * (Math.sin(6 * mu) * 151.0 / 96.0 + e1 * Math.sin(8.0 * mu) * 1097.0 / 512.0);
	C1 = e0sq * Math.pow(Math.cos(phi1), 2);
	T1 = Math.pow(Math.tan(phi1), 2);
	N1 = a / Math.sqrt(1 - Math.pow(e * Math.sin(phi1), 2));
	R1 = N1 * (1 - e * e) / (1 - Math.pow(e * Math.sin(phi1), 2));
	D = (x - 500000) / (N1 * k0);
	phi = (D * D) * (1.0 / 2.0 - D * D * (5 + 3 * T1 + 10 * C1 - 4.0 * C1 * C1 - 9 * e0sq) / 24.0);
	phi = phi + Math.pow(D , 6) * (61.0 + 90.0 * T1 + 298.0 * C1 + 45.0 * T1 * T1 - 252.0 * e0sq - 3 * C1 * C1) / 720.0;
	phi = phi1 - (N1 * Math.tan(phi1) / R1) * phi;
        
        latitude = Math.floor(1000000 * phi / drad) / 1000000;
        
        double lng = D * (1 + D * D * ((-1 - 2 * T1 - C1) / 6.0 + D * D * (5.0 - 2.0 * C1 + 28.0 * T1 - 3.0 * C1 * C1 + 8.0 * e0sq + 24.0 * T1 * T1) / 120.0)) / Math.cos(phi1);
	double lngd = zcm + lng / drad;
	
        longitude =  Math.floor(1000000 * lngd) / 1000000;
    
    }
    
    public static void main(String[] args) {
        UTM2LL utm2ll = new UTM2LL(Ellipsoid.WGS_84);

        double easting = 627103.0885902103;
        double northing = 4484335.479356929;
        String utmZone = "18N";
        utm2ll.convertUTMCoordinates(easting, northing, utmZone);
        System.out.println("Latitude: " + utm2ll.latitude + "\tLongitude: " + utm2ll.longitude);

        easting = 560560.5471820442;
        northing = 4822000.781513004;
        utmZone = "17N";
        utm2ll.convertUTMCoordinates(easting, northing, utmZone);
        System.out.println("Latitude: " + utm2ll.latitude + "\tLongitude: " + utm2ll.longitude);

    }
}
