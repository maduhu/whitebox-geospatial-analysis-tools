/*
 * Copyright (C) 2011-2013 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
package whitebox.stats;

import java.util.Arrays;

/**
 * The following code is based on Press et al. Numerical Recipes in C.
 *
 * @author johnlindsay
 */
public class TwoSampleKSTest {

    double dmax = -1.0;
    double pvalue = -1.0;
    double[] data1;
    double[] data2;
    long n1 = 0;
    long n2 = 0;
    final double EPS1 = 0.001;
    final double EPS2 = 1.0e-8;

    public TwoSampleKSTest() {
    }

    public TwoSampleKSTest(double[] data1, double[] data2) {
        this.data1 = data1.clone();
        this.data2 = data2.clone();
        this.n1 = this.data1.length;
        this.n2 = this.data2.length;
    }

    // properties' getters and setters
    public double[] getData1() {
        return data1.clone();
    }

    public void setData1(double[] data1) {
        dmax = -1.0;
        pvalue = -1.0;
        this.data1 = data1.clone();
    }

    public double[] getData2() {
        return data2.clone();
    }

    public void setData2(double[] data2) {
        dmax = -1.0;
        pvalue = -1.0;
        this.data2 = data2.clone();
    }

    public double getDmax() {
        if (dmax < 0) {
            calculateDMax();
        }
        return dmax;
    }

    public double getPvalue() {
        if (dmax < 0) {
            calculateDMax();
        }
        return pvalue;
    }

    public long getN1() {
        return n1;
    }

    public long getN2() {
        return n2;
    }

    // private methods
    private void calculateDMax() {
        try {
            int j1 = 0;
            int j2 = 0;
            double d1, d2, dt, en1, en2, en, fn1 = 0.0, fn2 = 0.0;

            // sort data1 and data2
            Arrays.sort(data1);
            Arrays.sort(data2);

            en1 = n1;
            en2 = n2;

            while (j1 < n1 && j2 < n2) {
                d1 = data1[j1];
                d2 = data2[j2];
                if (d1 <= d2) {
                    j1++;
                    fn1 = j1 / en1;
                }

                if (d2 <= d1) {
                    j2++;
                    fn2 = j2 / en2;
                }
                dt = Math.abs(fn2 - fn1);
                if (dt > dmax) {
                    dmax = dt;
                }
            }

            en = Math.sqrt(en1 * en2 / (en1 + en2));

            calculatePValue((en + 0.12 + 0.11 / en) * dmax);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void calculatePValue(double alam) {
        int j;
        double a2, fac = 2.0, sum = 0.0, term, termbf = 0.0;

        a2 = -2.0 * alam * alam;
        for (j = 1; j <= 100; j++) {
            term = fac * Math.exp(a2 * j * j);
            sum += term;
            if (Math.abs(term) <= EPS1 * termbf || Math.abs(term) <= EPS2 * sum) {
                pvalue = sum;
                return;
            }
            fac = -fac;
            termbf = Math.abs(term);
        }
        pvalue = 1.0;
    }
    
    public static void main(String[] args) {
        /* This is used for testing purposes.
         * 
         */
        
        try {
        double[] x = {-0.87399622, -0.06073305, -0.82809841,  0.36246144,  0.61187679, 
        -0.36278161,  2.65692271, -0.04878119, -0.29685874,  0.09778020, 
        -0.79740043,  0.86220642, -0.08187849, -0.49417868, -0.68428830, 
        0.50215073, -0.02778265, -1.13114516, -0.30488283, -0.47912706, 
        1.10121522,  0.72200371, -0.12419619,  0.88308067,  1.24170482};
        
        double[] y = {2.64614212,  2.40133975, -0.24951630, -1.05281579,  0.60464690,
        0.42801624,  0.06603241,  1.82728020,  2.05485682,  1.71798776,
        1.34008775,  1.52282631,  1.11934889,  0.34031629,  0.76826312,
        -0.20036927,  0.87902700,  0.77086493,  1.29494406,  0.07522084,
        -1.10084977, -0.12663182,  0.66229069,  0.44319635,  0.62638824};
        
        // D = 0.4, p-value = 0.026 (notice that R calculates a p-value of 0.03561 
        // but other online k-s test functions calculate a p-value of 0.026)
        TwoSampleKSTest ks = new TwoSampleKSTest(x, y);
        double Dmax = ks.getDmax();
        double pValue = ks.getPvalue();
        System.out.println("Dmax = " + Dmax);
        System.out.println("p-value = " + pValue);
        assert ((int)Math.round(Dmax * 100) == 40) : "Test failed based on Dmax";
        assert ((int)Math.round(pValue * 1000) == 26) : "Test failed based on p-value";
        
        System.out.println("Tests passed");
        } catch (AssertionError ae) {
            System.out.println(ae.getMessage());
        }
    }
}
