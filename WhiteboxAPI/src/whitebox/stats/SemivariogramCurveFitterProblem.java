/*
 *  Copyright (C) 2014 Ehsan Roshani
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * This is the optimization problem to find the best fit variables for semivariogram
 * 
 */
package whitebox.stats;

/**
 *
 * @author Ehsan Roshani, Ph.D. Department of Geography University of Guelph
 * Guelph, Ont. N1G 2W1 CANADA Phone: (519) 824-4120 x53527 Email:
 * eroshani@uoguelph.ca
 *
 * modified by John Lindsay
 */
import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.encodings.solutionType.RealSolutionType;
import jmetal.util.JMException;
import static whitebox.stats.Kriging.SemivariogramType.EXPONENTIAL;
import static whitebox.stats.Kriging.SemivariogramType.GAUSSIAN;
import static whitebox.stats.Kriging.SemivariogramType.SPHERICAL;

public class SemivariogramCurveFitterProblem extends Problem {

    // defining the lower and upper limits
    //Range, Sill, Nugget
    public static double[] LOWERLIMIT;
    public static double[] UPPERLIMIT;
    double difMin = 1000000000;
    Kriging.SemivariogramType SVType;
    double[][] Pnts;
    boolean Nugget;
    public static Kriging.Variogram var;

    void SetData(double[][] Points, Kriging.SemivariogramType SemiVType, boolean Nugget) {
        this.Pnts = Points;
        this.SVType = SemiVType;
        double Xmax = 0;
        double Ymax = 0;
        double Xmin = Double.MAX_VALUE;
        double Ymin = Double.MAX_VALUE;
        for (int i = 0; i < Points.length; i++) {
            if (Points[i][0] < Xmin) {
                Xmin = Points[i][0];
            }
            if (Points[i][1] < Ymin) {
                Ymin = Points[i][1];
            }

            if (Points[i][0] > Xmax) {
                Xmax = Points[i][0];
            }
            if (Points[i][1] > Ymax) {
                Ymax = Points[i][1];
            }
        }
        if (Nugget) {
            LOWERLIMIT = new double[3];
            UPPERLIMIT = new double[3];
            LOWERLIMIT[0] = 0;
            LOWERLIMIT[1] = 0;
            LOWERLIMIT[2] = 0;
            UPPERLIMIT[0] = Xmax * 1.5;
            UPPERLIMIT[1] = Ymax;
            UPPERLIMIT[2] = Ymax;
        } else {
            LOWERLIMIT = new double[2];
            UPPERLIMIT = new double[2];
            LOWERLIMIT[0] = 0;
            LOWERLIMIT[1] = 0;
            UPPERLIMIT[0] = Xmax * 1.5;
            UPPERLIMIT[1] = Ymax;
        }

    }

    /**
     * Constructor. Creates a default instance of the Water problem.
     *
     * @param solutionType The solution type must "Real" or "BinaryReal".
     */
    public SemivariogramCurveFitterProblem(double[][] Points, Kriging.SemivariogramType SVType, boolean Nugget) {
        this.Nugget = Nugget;
        if (Nugget) {
            numberOfVariables_ = 3;
        } else {
            numberOfVariables_ = 2;
        }
        SetData(Points, SVType, Nugget);

        numberOfObjectives_ = 2;
        numberOfConstraints_ = 0;
        problemName_ = "SemiVariogramCurveFitter";

        upperLimit_ = new double[numberOfVariables_];
        lowerLimit_ = new double[numberOfVariables_];
        upperLimit_ = new double[numberOfVariables_];
        lowerLimit_ = new double[numberOfVariables_];
        for (int i = 0; i < numberOfVariables_; i++) {
            lowerLimit_[i] = LOWERLIMIT[i];
            upperLimit_[i] = UPPERLIMIT[i];
        }

        solutionType_ = new RealSolutionType(this);
    } // Roshani

    /**
     * Evaluates a solution
     *
     * @param solution The solution to evaluate
     * @throws JMException
     */
    @Override
    public void evaluate(Solution solution) throws JMException {
        double[] values = new double[Pnts.length];
        switch (SVType) {
            case EXPONENTIAL:
                for (int i = 0; i < Pnts.length; i++) {
                    if (Pnts[i][0] != 0) {
                        values[i] = (Nugget ? solution.getDecisionVariables()[2].getValue() : 0) + solution.getDecisionVariables()[1].getValue()
                                * (1 - Math.exp(-Pnts[i][0] / solution.getDecisionVariables()[0].getValue()));
                    } else {
                        values[i] = 0;
                    }
                }
                break;
            case GAUSSIAN:
                for (int i = 0; i < Pnts.length; i++) {
                    if (Pnts[i][0] != 0) {
                        values[i] = (Nugget ? solution.getDecisionVariables()[2].getValue() : 0)
                                + solution.getDecisionVariables()[1].getValue()
                                * (1 - Math.exp(-(Math.pow(Pnts[i][0], 2)) / (Math.pow(
                                                solution.getDecisionVariables()[0].getValue(), 2))));
                    } else {
                        values[i] = 0;
                    }
                }
                break;
            case SPHERICAL:
                for (int i = 0; i < Pnts.length; i++) {
                    if (Pnts[0][0] > solution.getDecisionVariables()[0].getValue()) {
                        values[i] = (Nugget ? solution.getDecisionVariables()[2].getValue() : 0)
                                + solution.getDecisionVariables()[1].getValue();

                    } else if (0 < Pnts[0][0] && Pnts[0][0] <= solution.getDecisionVariables()[0].getValue()) {
                        values[i] = (Nugget ? solution.getDecisionVariables()[2].getValue() : 0)
                                + solution.getDecisionVariables()[1].getValue() * (1.5 * Pnts[i][0]
                                / solution.getDecisionVariables()[0].getValue() - 0.5 * Math.pow((Pnts[i][0]
                                        / solution.getDecisionVariables()[0].getValue()), 3));
                    } else {
                        values[i] = 0;
                    }
                }
                break;
        }
        double mse = 0;
        for (int i = 0; i < Pnts.length; i++) {
            mse += Math.pow((values[i] - Pnts[i][1]), 2);
        }
        if (mse < difMin) {
            Kriging k = new Kriging();

            var = k.getSemivariogram(SVType,
                    solution.getDecisionVariables()[0].getValue(),
                    solution.getDecisionVariables()[1].getValue(),
                    (Nugget) ? solution.getDecisionVariables()[2].getValue() : 0,
                    false);
            var.mse = mse;
            difMin = mse;
        }

        solution.setObjective(0, mse);
        solution.setObjective(1, mse);
    }

    /**
     * NOT USED Evaluates the constraint overhead of a solution
     *
     * @param solution The solution
     * @throws JMException
     */
    @Override
    public void evaluateConstraints(Solution solution) throws JMException {

//      
//      
//    double [] constraint = new double[1]; // 7 constraints
//    
//      for (int i = 0; i < k.Pairs.size(); i++) {
//          if (k.Pairs.get(i).Distance<= k.resolution) {
//              constraint[0]+=(k.resolution - k.Pairs.get(i).Distance);
//          }
//      }
// 
//    solution.setOverallConstraintViolation(constraint[0]);    
//    solution.setNumberOfViolatedConstraint(1);  
//    System.out.println(constraint[0]);
    } // evaluateConstraints   

}
