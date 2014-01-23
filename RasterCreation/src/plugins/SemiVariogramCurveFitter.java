/*
 * This is to set up the SemiVariogram Curve fitter optimization problem
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

import jmetal.core.Algorithm;
import jmetal.core.Operator;
import jmetal.core.Problem;
import jmetal.core.SolutionSet;
import jmetal.operators.crossover.CrossoverFactory;
import jmetal.operators.mutation.MutationFactory;
import jmetal.operators.selection.SelectionFactory;
import jmetal.problems.ProblemFactory;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.Configuration;
import jmetal.util.JMException;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import jmetal.metaheuristics.nsgaII.NSGAII;
import static jmetal.metaheuristics.nsgaII.NSGAII_main.fileHandler_;
import static jmetal.metaheuristics.nsgaII.NSGAII_main.logger_;
import jmetal.problems.Roshani;
import whitebox.geospatialfiles.WhiteboxRaster;
import plugins.Kriging;


public class SemiVariogramCurveFitter {
  
    public Kriging.Variogram Run(double[][] Points, Kriging.SemiVariogramType SVType, boolean ConsiderNugget)throws 
                                  JMException, 
                                  SecurityException, 
                                  IOException, 
                                  ClassNotFoundException 
    {
        SemiVariogramCurveFitter so = new SemiVariogramCurveFitter();
        Problem   problem   ; // The problem to solve
        Algorithm algorithm ; // The algorithm to use
        Operator  crossover ; // Crossover operator
        Operator  mutation  ; // Mutation operator
        Operator  selection ; // Selection operator

        HashMap  parameters ; // Operator parameters

        QualityIndicator indicators ; // Object to get quality indicators

        indicators = null ;
        
        problem = new SemiVariogramCurveFitterProblem(Points,SVType,ConsiderNugget);

        algorithm = new NSGAII(problem);
        //algorithm = new ssNSGAII(problem);

        // Algorithm parameters
        algorithm.setInputParameter("populationSize",10);
        algorithm.setInputParameter("maxEvaluations", (10 * Points.length<1000)?1000:10 * Points.length);

        // Mutation and Crossover for Real codification 
        parameters = new HashMap() ;
        parameters.put("probability", 0.9) ;
        parameters.put("distributionIndex", 20.0) ;
        crossover = CrossoverFactory.getCrossoverOperator("SBXCrossover", parameters);                   

        parameters = new HashMap() ;
        parameters.put("probability", 1.0/problem.getNumberOfVariables()) ;
        parameters.put("distributionIndex", 20.0) ;
        mutation = MutationFactory.getMutationOperator("PolynomialMutation", parameters);                    

        // Selection Operator 
        parameters = null ;
        selection = SelectionFactory.getSelectionOperator("BinaryTournament2", parameters) ;                           

        // Add the operators to the algorithm
        algorithm.addOperator("crossover",crossover);
        algorithm.addOperator("mutation",mutation);
        algorithm.addOperator("selection",selection);

        // Add the indicator object to the algorithm
        algorithm.setInputParameter("indicators", indicators) ;

        // Execute the Algorithm
        //long initTime = System.currentTimeMillis();
        SolutionSet population = algorithm.execute();
        //long estimatedTime = System.currentTimeMillis() - initTime;
        return SemiVariogramCurveFitterProblem.Var;

  }

    
  public static void main(String [] args) throws 
                                  JMException, 
                                  SecurityException, 
                                  IOException, 
                                  ClassNotFoundException {
      
  } //main
}
