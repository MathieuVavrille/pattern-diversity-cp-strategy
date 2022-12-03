package org.mvavrill.miningDiv.models;

import org.mvavrill.miningDiv.mining.models.closedpatterns.ClosedPatterns;
import org.mvavrill.miningDiv.mining.models.closedpatterns.ClosedPatternsBacktrack;
import org.mvavrill.miningDiv.mining.models.closeddiversity.ClosedDiversity;
import org.mvavrill.miningDiv.mining.models.closeddiversity.ClosedDiversityBacktrack;
import org.mvavrill.miningDiv.mining.util.DataSet;
import org.mvavrill.miningDiv.mining.structures.History;
import org.mvavrill.miningDiv.mining.models.*;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.SearchState;
import org.chocosolver.solver.search.limits.SolutionCounter;
import org.chocosolver.solver.search.limits.TimeCounter;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainRandom;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainRandomBound;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMax;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.io.IOException;
import java.util.Random;

/**
 * This class can generate different pattern mining model. Every pattern mining model has a closedPattern (or closedDiversity when adding the Jaccard constraint) constraint.
 * The solutions are automatically recorded in a history, accessible through `getHistory()`
 * @author Mathieu Vavrille
 * @since 31/12/21
 */
public class PatternMining extends ModelGenerator {

  private final DataSet dataset; // The dataset (input data)
  private final int minFreq; // The minimum frequency for patterns
  private final boolean useBacktrackStructure; // Whether or not to use the backtrackable BitSets
  private final boolean doWeakConsistencyClosedPattern; // Whether or not do do the weak consistency of closedPattern (do not enforce rule 3)

  // Closed Diversity
  private final double jMax; // The maximum Jaccard distance between solutions
  private final boolean checkRealJaccard; // Whether or not to check the real Jaccard distance on solutions
  private final boolean checkUB;

  // Strategy based
  private final String searchStrategy; // The search strategy to use. Can be 'inputOrder', 'oriented', 'random', 'randomOne' (selects a random variable and instantiate it to 1
  private final boolean useExact; // In the oriented strategy, whether or not to use the exact Jaccard
  private final Random random; // Random number generator. Used in the strategies 'random', 'randomOne', and 'oriented' (if random==null, then the oriented strategy will be deterministic
  
  private History currentHistory = null; // The current history. Erased at model generation, it can be accessed through getHistory().
  
  public static PatternMining baseModel(final DataSet dataset, final int minFreq) {
    return PatternMining.baseModel(dataset, minFreq, true, true);
  }
  public static PatternMining baseModel(final DataSet dataset, final int minFreq, final boolean useBacktrackStructure, final boolean doWeakConsistencyClosedPattern) {
    return new PatternMining(dataset, minFreq, useBacktrackStructure, doWeakConsistencyClosedPattern, -1, false, false, "minCov", false, null);
  }

  public static PatternMining baseDiversity(final DataSet dataset, final int minFreq, final double jMax, final boolean checkRealJaccard) {
    return new PatternMining(dataset, minFreq, true, true, jMax, checkRealJaccard, false, "minCov", false, null);
  }
  public static PatternMining baseDiversity(final DataSet dataset, final int minFreq, final double jMax, final boolean checkRealJaccard, final boolean useBacktrackStructure, final boolean doWeakConsistencyClosedPattern, final boolean checkUB) {
    return new PatternMining(dataset, minFreq, useBacktrackStructure, doWeakConsistencyClosedPattern, jMax, checkRealJaccard, checkUB, "minCov", false, null);
  }

  /* Strategy based searches */
  public static PatternMining randomSearch(final DataSet dataset, final int minFreq, final Random random) {
    return new PatternMining(dataset, minFreq, true, true, -1, false, false, "random", false, random);
  }
  public static PatternMining orientedDeterministicSearch(final DataSet dataset, final int minFreq, final boolean useExact) {
    return new PatternMining(dataset, minFreq, true, true, -1, false, false, "oriented", useExact, null);
  }
  public static PatternMining orientedRandomSearch(final DataSet dataset, final int minFreq, final boolean useExact, final Random random) {
    return new PatternMining(dataset, minFreq, true, true, -1, false, false, "oriented", useExact, random);
  }
  public static PatternMining transactionOrientedSearch(final DataSet dataset, final int minFreq) {
    return new PatternMining(dataset, minFreq, true, true, -1, false, false, "transactionOriented", false, null);
  }
  
  public PatternMining(final DataSet dataset, final int minFreq, final boolean useBacktrackStructure, final boolean doWeakConsistencyClosedPattern, final double jMax, final boolean checkRealJaccard, final boolean checkUB, final String searchStrategy, final boolean useExact, final Random random) {
    this.dataset = dataset;
    this.minFreq = minFreq;
    this.useBacktrackStructure = useBacktrackStructure;
    this.doWeakConsistencyClosedPattern = doWeakConsistencyClosedPattern;
    this.jMax = jMax;
    this.checkRealJaccard = checkRealJaccard;
    this.checkUB = checkUB;
    this.searchStrategy = searchStrategy;
    this.useExact = useExact;
    this.random = random;
  }
  
  @Override
  public ModelAndVars generateModelAndVars() {
    final Model model = createModel("ClosedPattern");
    final BoolVar[] vars = model.boolVarArray("item", dataset.getNbrVar());
    model.sum(vars, ">=", 1).post();
    currentHistory = new History(model, dataset, vars);
    // Main constraint
    FreeItemsExtensions itemsExtensions;
    Constraint closed;
    if (jMax < 0) { // Base closed pattern
      if (useBacktrackStructure) {
        itemsExtensions = new ClosedPatternsBacktrack(dataset, minFreq, vars, doWeakConsistencyClosedPattern);
        closed = new Constraint("Closed Patterns Backtrack", (ClosedPatternsBacktrack) itemsExtensions);
      }
      else {
        itemsExtensions = new ClosedPatterns(dataset, minFreq, vars, doWeakConsistencyClosedPattern);
        closed = new Constraint("Closed Patterns", (ClosedPatterns) itemsExtensions);
      }
    }
    else { // closed diversity
      if (useBacktrackStructure) {
        itemsExtensions = new ClosedDiversityBacktrack(dataset, currentHistory, minFreq, jMax, vars, doWeakConsistencyClosedPattern, checkRealJaccard, checkUB);
        closed = new Constraint("Closed Diversity Backtrack", (ClosedDiversityBacktrack) itemsExtensions);
      }
      else {
        itemsExtensions = new ClosedDiversity(dataset, minFreq, jMax, currentHistory, vars); // I did not really check this class, so better not use it
        closed = new Constraint("Closed Diversity", (ClosedDiversity) itemsExtensions);
      }
    }
    model.post(closed);
    // Search strategy
    Solver solver = model.getSolver();
    if (searchStrategy.equals("inputOrder"))
      solver.setSearch(Search.inputOrderUBSearch(vars));//Search.intVarSearch(new ReverseOrder<>(model), new IntDomainMax(), vars));
    else if (searchStrategy.equals("minCov"))
      solver.setSearch(Search.intVarSearch(new MinCovVarSelector<IntVar>(itemsExtensions), new IntDomainMax(), vars));
    else {
      solver.setRestartOnSolutions();
      solver.setNoGoodRecordingFromRestarts();
      if (searchStrategy.equals("random")) {
        IntValueSelector value = new IntDomainRandom(random.nextLong());
        IntValueSelector bound = new IntDomainRandomBound(random.nextLong());
        IntValueSelector selector = var -> {
          if (var.hasEnumeratedDomain()) {
            return value.selectValue(var);
          } else {
            return bound.selectValue(var);
          }
        };
        solver.setSearch(Search.intVarSearch(new org.chocosolver.solver.search.strategy.selectors.variables.Random<>(random.nextLong()), selector, vars));
      }
      else if (searchStrategy.equals("randomOne")) {
        solver.setSearch(Search.intVarSearch(new org.chocosolver.solver.search.strategy.selectors.variables.Random<>(random.nextLong()), new IntDomainMax(), vars));
      }
      else if (searchStrategy.equals("transactionOriented")) {
        solver.setSearch(new TransactionWeightStrategy(model, vars, currentHistory, dataset, itemsExtensions));
      }
      else { // Oriented search
        solver.setSearch(new OrientedSearchStrategy(model, vars, minFreq, currentHistory, dataset, useExact, itemsExtensions, random));
      }
    }
    solver.setNoGoodRecordingFromSolutions(vars);
    return new ModelAndVars(model, vars);
  }

  public History findAllSolutions(final long solutionLimit) {
    ModelAndVars mv = generateModelAndVars();
    mv.getModel().getSolver().findAllSolutions(new SolutionCounter(mv.getModel(), solutionLimit));
    return currentHistory;
  }

  public History findAllSolutions(final long solutionLimit, final long timeLimit) {
    ModelAndVars mv = generateModelAndVars();
    Model model = mv.getModel();
    model.getSolver().findAllSolutions(new SolutionCounter(model, solutionLimit), new TimeCounter(model, timeLimit));
    if (model.getSolver().getSearchState() == SearchState.STOPPED && currentHistory.size() < solutionLimit) // Timeout
      return null;
    return currentHistory;
  }

  public History findAllSolutionsTimeLimit(final long timeLimit) {
    ModelAndVars mv = generateModelAndVars();
    Solver solver = mv.getModel().getSolver();
    solver.findAllSolutions(new TimeCounter(mv.getModel(), timeLimit));
    if (solver.getSearchState() == SearchState.STOPPED) // Timeout
      return null;
    return currentHistory;
  }

  public History findAllSolutions() {
    ModelAndVars mv = generateModelAndVars();
    mv.getModel().getSolver().findAllSolutions();
    return currentHistory;
  }
        
  @Override
  public String getName() {
    return "ClosedPatternModel";
  }

  public History getHistory() {
    return currentHistory;
  }
}
