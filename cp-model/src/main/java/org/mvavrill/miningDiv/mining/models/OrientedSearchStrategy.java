
package org.mvavrill.miningDiv.mining.models;

import org.mvavrill.miningDiv.mining.structures.*;
import org.mvavrill.miningDiv.mining.util.DataSet;
import org.mvavrill.miningDiv.mining.util.Jaccard;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.memory.IStateBool;
import org.chocosolver.solver.search.strategy.decision.Decision;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;

/**
 * A search random search strategy oriented to spaces with better Jaccard.
 * In practice, it will choose the item (i.e. a variable) that minimizes the Upper bound of the Jaccard compared to all the covers in the history, and instantiate it to 1.
 */
public class OrientedSearchStrategy extends AbstractStrategy<IntVar> {

  private final static double EPSILON = 0.000000001;

  private final int threshold;
  private final History history;
  private final DataSet dataset;
  private final boolean useExact;
  private final Random random; // If random== null, then a deterministic decision is done. Otherwise, a random oriented decision is done
  private final FreeItemsExtensions itemsExtensions;

  // Deal with initial values
  private final IStateBool isInitialPropagation;
  private int[] initialFreeItemsIds = null;
  private TransactionSet[] initialFreeItemsExtensions = null;
  private double[] initialDecisionWeights = null;
  private int processedHistorySize = 0;

  /**
   * @param vars the variables of the strategy (the items)
   * @param threshold the minimum size of the cover (used for Jaccard upper bound)
   * @param the history of solutions
   * @param the dataset (to compute the covers)
   * @param useExact a boolean to set to true if the exact Jaccard should be used in the computation. Otherwise it will be the upper bound.
   * @param random a random number generator. If set, the search will be randomly weighted by the jaccard, otherwise (if set to null), the search will pick the best item at each step
   */
  public OrientedSearchStrategy(final Model model, final BoolVar[] vars, final int threshold, final History history, final DataSet dataset, final boolean useExact, final FreeItemsExtensions itemsExtensions, final Random random) {
    super(vars);
    this.threshold = threshold;
    this.history = history;
    this.dataset = dataset;
    this.useExact = useExact;
    this.itemsExtensions = itemsExtensions;
    this.random = random;
    initialDecisionWeights = new double[vars.length];
    isInitialPropagation = model.getEnvironment().makeBool(true);
  }
  
  @Override
  protected Decision<IntVar> computeDecision(IntVar var) {
    throw new IllegalStateException("It is currently not possible to use computeDecision in OrientedSearchStrategy (we could force to instantiate to 1, but no)");
  }

  private BitSet getFreeItems() {
    BitSet freeItems = new BitSet();
    for (int item = 0; item < dataset.getNbrVar(); item++) {
      if (vars[item].getDomainSize() == 2) {
        freeItems.set(item);
      }
    }
    return freeItems;
  }

  public Decision<IntVar> getDecision() {
    if (isInitialPropagation.get()) {
      if (initialFreeItemsIds == null)
        initializeInitialData(getFreeItems());
      updateInitialBounds();
      Decision<IntVar> dec = makeIntDecision(vars[(random == null) ? deterministicInitialDecision() : randomInitialDecision()], 1);
      isInitialPropagation.set(false);
      return dec;
    }
    else {
      BitSet freeItems = getFreeItems();
      if (freeItems.isEmpty()) // All variables are instantiated
        return null;
      Decision<IntVar> dec = makeIntDecision(vars[(random == null) ? deterministicDecision(freeItems) : randomDecision(freeItems)], 1);
      return dec;
    }
  }

  private void initializeInitialData(final BitSet freeItems) {
    initialFreeItemsIds = new int[freeItems.cardinality()];
    initialFreeItemsExtensions = new TransactionSet[freeItems.cardinality()];
    initialDecisionWeights = new double[freeItems.cardinality()];
    int cpt = 0;
    for (int item = freeItems.nextSetBit(0); item != -1; item = freeItems.nextSetBit(item+1)) {
      int currentId = cpt++;
      initialFreeItemsIds[currentId] = item;
      initialFreeItemsExtensions[currentId] = itemsExtensions.getFreeItemsCover(item);
      //initialDecisionWeights[currentId] = 0.; // Useless because double is initialized at 0.
    }
  }

  private void updateInitialBounds() {
    for (int i = processedHistorySize; i < history.size(); i++) {
      for (int v = 0; v < initialDecisionWeights.length; v++) {
        initialDecisionWeights[v] = Math.max(initialDecisionWeights[v], getBound(initialFreeItemsExtensions[v], history.get(i).getCover()));
      }
    }
    processedHistorySize = history.size();
  }

  private int deterministicInitialDecision() {
    double minBound = Double.POSITIVE_INFINITY; // bigger than 1
    int bestItem = -1;
    for (int i = 0; i < initialDecisionWeights.length; i++) {
      if (initialDecisionWeights[i] < minBound) {
        minBound = initialDecisionWeights[i];
        bestItem = initialFreeItemsIds[i];
      }
    }
    return bestItem;
  }

  private int deterministicDecision(final BitSet freeItems) {
    double minBound = Double.POSITIVE_INFINITY; // bigger than 1
    int bestItem = -1;
    for (int item = freeItems.nextSetBit(0); item != -1; item = freeItems.nextSetBit(item+1)) {
      double itemMaxBound = getMaxBound(itemsExtensions.getFreeItemsCover(item));
      if (itemMaxBound < minBound) {
        minBound = itemMaxBound;
        bestItem = item;
      }
    }
    return bestItem;
  }

  private int randomInitialDecision() {
    double[] weights = new double[initialDecisionWeights.length];
    for (int i = 0; i < initialDecisionWeights.length; i++) {
      weights[i] = 1/(initialDecisionWeights[i]+EPSILON);
    }
    double r = random.nextDouble()*Arrays.stream(weights).sum();
    int i = 0;
    while (r >= weights[i]) {
      r -= weights[i];
      i++;
    }
    return initialFreeItemsIds[i];
  }

  private int randomDecision(final BitSet freeItems) {
    double[] bounds = new double[freeItems.cardinality()];
    int[] itemIds = new int[freeItems.cardinality()];
    int itemId = 0;
    for (int item = freeItems.nextSetBit(0); item != -1; item = freeItems.nextSetBit(item+1)) {
      itemIds[itemId] = item;
      bounds[itemId] = 1/(getMaxBound(itemsExtensions.getFreeItemsCover(item))+EPSILON);
      itemId++;
    }
    double r = random.nextDouble()*Arrays.stream(bounds).sum();
    int i = 0;
    while (r >= bounds[i]) {
      r -= bounds[i];
      i++;
    }
    return itemIds[i];
  }

  private double getMaxBound(final TransactionSet cover) {
    double maxBound = 0.;
    for (ItemsetCover ic : history.getAllItemsets())
      maxBound = Math.max(maxBound,getBound(cover, ic.getCover()));
    return maxBound;
  }

  private double getBound(final TransactionSet cover, final TransactionSet h) {
    return useExact ? Jaccard.exact(cover, h) : Jaccard.ub(cover, h, threshold);
  }
}
