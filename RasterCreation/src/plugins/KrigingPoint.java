/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins;

/**
 *
 * @author Ehsan Roshani, Ph.D.
    Department of Geography 
    University of Guelph
    Guelph, Ont. N1G 2W1 CANADA
    Phone: (519) 824-4120 x53527
    Email: eroshani@uoguelph.ca
 */

public class KrigingPoint {
    public double x;
    public double y;
    public double z;
    public double v;    //Kriging Variance Eq 12.20 P 290
            
    
    
    
    public KrigingPoint(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
