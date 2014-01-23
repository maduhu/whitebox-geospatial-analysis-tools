/*
 * This is the optimization problem to find the best fit variables for semivariogram
 * 
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

//import com.sun.org.apache.xml.internal.resolver.helpers.Debug;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import plugins.Kriging;
import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.encodings.solutionType.BinaryRealSolutionType;
import jmetal.encodings.solutionType.RealSolutionType;
import static jmetal.problems.Water.LOWERLIMIT;
import static jmetal.problems.Water.UPPERLIMIT;
import jmetal.util.JMException;
import static plugins.Kriging.SemiVariogramType.Exponential;
import static plugins.Kriging.SemiVariogramType.Gaussian;
import static plugins.Kriging.SemiVariogramType.Spherical;
import plugins.KrigingPoint;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.geospatialfiles.shapefile.Point;
import whitebox.geospatialfiles.shapefile.attributes.DBFException;


public class SemiVariogramCurveFitterProblem extends Problem{
    // defining the lower and upper limits
    //Range, Sill, Nugget
  public static double [] LOWERLIMIT ;
  public static double [] UPPERLIMIT ;           
  double difMin = 1000000000;
  Kriging.SemiVariogramType SVType;
  double [][] Pnts;
  boolean Nugget;
  public static Kriging.Variogram Var;

  void SetData(double[][] Points, Kriging.SemiVariogramType SemiVType, boolean  Nugget ){
      this.Pnts = Points;
      this.SVType = SemiVType; 
      double Xmax=0; double Ymax=0; double Xmin=Double.MAX_VALUE;double Ymin=Double.MAX_VALUE;  
      for (int i = 0; i < Points.length; i++) {
          if (Points[i][0]<Xmin) {
              Xmin = Points[i][0];
          }
          if (Points[i][1]<Ymin) {
              Ymin = Points[i][1];
          }
          
          if (Points[i][0]>Xmax) {
              Xmax = Points[i][0];
          }
          if (Points[i][1]>Ymax) {
              Ymax = Points[i][1];
          }
      }
      if (Nugget) {
          LOWERLIMIT = new double [3];
          UPPERLIMIT = new double[3];
          LOWERLIMIT[0]=0;
          LOWERLIMIT[1]=0;
          LOWERLIMIT[2]=0;
          UPPERLIMIT[0]=Xmax*1.5;
          UPPERLIMIT[1]=Ymax;
          UPPERLIMIT[2]=Ymax;
      }
      else{
          LOWERLIMIT = new double [2];
          UPPERLIMIT = new double[2];          
          LOWERLIMIT[0]=0;
          LOWERLIMIT[1]=0;
          UPPERLIMIT[0]=Xmax*1.5;
          UPPERLIMIT[1]=Ymax;
      }

      
  } 
  
  /**
  * Constructor.
  * Creates a default instance of the Water problem.
  * @param solutionType The solution type must "Real" or "BinaryReal".
  */
  public SemiVariogramCurveFitterProblem(double[][] Points, Kriging.SemiVariogramType SVType, boolean  Nugget) {
      this.Nugget = Nugget;
      if (Nugget) {
          numberOfVariables_   = 3 ;
      }
      else{
          numberOfVariables_   = 2;
      }
      SetData(Points, SVType, Nugget);
    
    numberOfObjectives_  = 2 ;
    numberOfConstraints_ = 0 ;
    problemName_         = "SemiVariogramCurveFitter";
	        
    upperLimit_ = new double[numberOfVariables_];
    lowerLimit_ = new double[numberOfVariables_];
    upperLimit_ = new double[numberOfVariables_];
    lowerLimit_ = new double[numberOfVariables_];
      for (int i = 0; i < numberOfVariables_; i++) {
          lowerLimit_[i]=LOWERLIMIT[i];
          upperLimit_[i]=UPPERLIMIT[i];
      }
	        
    solutionType_ = new RealSolutionType(this) ;
 } // Roshani
  
	         /**
   * Evaluates a solution
   * @param solution The solution to evaluate
   * @throws JMException 
   */
  @Override
  public void evaluate(Solution solution) throws JMException {  
      double[] values = new double[Pnts.length];
      switch (SVType){
            case Exponential:
                for (int i = 0; i < Pnts.length; i++) {
                    if (Pnts[i][0]!=0) {
                        values[i]= (Nugget ? solution.getDecisionVariables()[2].getValue() : 0 ) + solution.getDecisionVariables()[1].getValue()*
                                (1-Math.exp(-Pnts[i][0]/solution.getDecisionVariables()[0].getValue()));
                    }
                    else{
                        values[i]= 0;
                    }
                }
                break;
            case Gaussian:
                for (int i = 0; i < Pnts.length; i++) {
                    if (Pnts[i][0]!=0) {
                        values[i]=(Nugget ? solution.getDecisionVariables()[2].getValue() : 0 ) + 
                                solution.getDecisionVariables()[1].getValue()*
                                (1-Math.exp(-(Math.pow(Pnts[i][0], 2))/(Math.pow(
                                solution.getDecisionVariables()[0].getValue(),2))));
                    }
                    else{
                        values[i]=0;
                    }
                }
                break;
            case Spherical:
                for (int i = 0; i < Pnts.length; i++) {
                    if (Pnts[0][0]>solution.getDecisionVariables()[0].getValue()) {
                        values[i]= (Nugget ? solution.getDecisionVariables()[2].getValue() : 0 ) + 
                                solution.getDecisionVariables()[1].getValue();

                    }
                    else if (0<Pnts[0][0] && Pnts[0][0] <=solution.getDecisionVariables()[0].getValue()) {
                        values[i]= (Nugget ? solution.getDecisionVariables()[2].getValue() : 0 ) + 
                                solution.getDecisionVariables()[1].getValue()*(1.5*Pnts[i][0]/
                                solution.getDecisionVariables()[0].getValue()-0.5*Math.pow((Pnts[i][0]/
                                solution.getDecisionVariables()[0].getValue()),3));
                    }
                    else
                    {
                        values[i]= 0;
                    }
                }
                break;
        }
      double mse = 0;
      for (int i = 0; i < Pnts.length; i++) {
          mse += Math.pow((values[i]-Pnts[i][1]), 2);
      }
      if (mse< difMin) {
          Kriging k = new Kriging();
          
          Var = k.SemiVariogram(SVType,
                  solution.getDecisionVariables()[0].getValue(),
                  solution.getDecisionVariables()[1].getValue(),
                  (Nugget)?solution.getDecisionVariables()[2].getValue():0,
                  false);
          difMin = mse;
      }

             
    solution.setObjective(0,mse);    
    solution.setObjective(1,mse);
    //System.out.println(mse);
  } // evaluate

  /** 
   * NOT USED Evaluates the constraint overhead of a solution 
   * @param solution The solution
   * @throws JMException 
   */  
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
