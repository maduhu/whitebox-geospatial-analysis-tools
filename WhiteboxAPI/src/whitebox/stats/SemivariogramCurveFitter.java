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
 * This is to set up the SemiVariogram Curve fitter optimization problem
 */
package whitebox.stats;

/**
 *
 * @author Ehsan Roshani, Ph.D.
    Department of Geography 
    University of Guelph
    Guelph, Ont. N1G 2W1 CANADA
    Phone: (519) 824-4120 x53527
    Email: eroshani@uoguelph.ca
    * 
    * modified by John Lindsay
 */

import jmetal.core.Algorithm;
import jmetal.core.Operator;
import jmetal.core.Problem;
import jmetal.core.SolutionSet;
import jmetal.operators.crossover.CrossoverFactory;
import jmetal.operators.mutation.MutationFactory;
import jmetal.operators.selection.SelectionFactory;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.JMException;
import java.io.IOException;
import java.util.HashMap;
import jmetal.metaheuristics.nsgaII.NSGAII;


public class SemivariogramCurveFitter {
  
    public Kriging.Variogram Run(double[][] Points, Kriging.SemivariogramType SVType, boolean ConsiderNugget)throws 
                                  JMException, 
                                  SecurityException, 
                                  IOException, 
                                  ClassNotFoundException 
    {
        SemivariogramCurveFitter so = new SemivariogramCurveFitter();
        Problem   problem   ; // The problem to solve
        Algorithm algorithm ; // The algorithm to use
        Operator  crossover ; // Crossover operator
        Operator  mutation  ; // Mutation operator
        Operator  selection ; // Selection operator

        HashMap  parameters ; // Operator parameters

        QualityIndicator indicators ; // Object to get quality indicators

        indicators = null ;
        
        problem = new SemivariogramCurveFitterProblem(Points,SVType,ConsiderNugget);

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
        return SemivariogramCurveFitterProblem.var;

  }

    
  public static void main(String [] args) throws 
                                  JMException, 
                                  SecurityException, 
                                  IOException, 
                                  ClassNotFoundException {
      
  } //main
}
