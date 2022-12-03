package org.mvavrill.miningDiv.models;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.expression.discrete.arithmetic.ArExpression;
import org.chocosolver.solver.variables.IntVar;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * The abstract class to wrap all the models.
 * This class helps to add constraints in the model such as the hashMod constraints, or random tables.
 */
public abstract class ModelGenerator {

  public Model generateModel() {
    return this.generateModelAndVars().getModel();
  }

  public abstract ModelAndVars generateModelAndVars();

  public abstract String getName();
  
  protected Model createModel(final String modelName) {
    return new Model(modelName);
  }
  
  /**
   * Get the maximum range of the variables, i.e. max_x UB(x)-LB(x)
   */
  public int getMaxRange() {
    ModelAndVars modelAndVars = this.generateModelAndVars();
    Model model = modelAndVars.getModel();
    IntVar[] vars = modelAndVars.getVars();
    int maxRange = vars[0].getUB() - vars[0].getLB();
    for (int i = 1; i < vars.length; i++) {
      if (vars[i].getUB() - vars[i].getLB() > maxRange)
        maxRange = vars[i].getUB() - vars[i].getLB();
    }
    return maxRange;
  }

  /** Get the minimum lower bound */
  public int getMinLB() {
    IntVar[] vars = this.generateModelAndVars().getVars();
    int minLB = vars[0].getLB();
    for (int i = 1; i < vars.length; i++) {
      if (vars[i].getLB() < minLB)
        minLB = vars[i].getLB();
    }
    return minLB;
  }

  public int getNbVars() {
    return this.generateModelAndVars().getVars().length;
  }
}
