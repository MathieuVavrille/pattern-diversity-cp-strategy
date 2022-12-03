package org.mvavrill.miningDiv.models;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

import java.util.Arrays;
import java.util.List;

public class ModelAndVars {

  private final Model model;
  private final IntVar[] vars;

  public ModelAndVars(final Model model, final IntVar[] vars) {
    this.model = model;
    this.vars = vars;
  }

  public Model getModel() {
    return model;
  }

  public IntVar[] getVars() {
    return vars;
  }
}
