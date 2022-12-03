
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
public class TransactionWeightStrategy extends AbstractStrategy<IntVar> {

  private final static double EPSILON = 0.000000001;

  private final History history;
  private final FreeItemsExtensions itemsExtensions;

  // Deal with initial values
  private final IStateBool isInitialPropagation;
  private final BitSet[] verticalDatabase;
  private int[] sumsPresenceOfTransaction;
  private TransactionSet[] initialFreeItemsExtensions = null;
  private int processedHistorySize = 0;

  /**
     Computes the item that minimizes the cardinal of the sum of the intersections with the history
   */
  public TransactionWeightStrategy(final Model model, final BoolVar[] vars, final History history, final DataSet dataset, final FreeItemsExtensions itemsExtensions) {
    super(vars);
    this.history = history;
    this.verticalDatabase = dataset.getVerticalDataBase();
    this.itemsExtensions = itemsExtensions;
    isInitialPropagation = model.getEnvironment().makeBool(true);
    sumsPresenceOfTransaction = new int[dataset.getTransactionsSize()];
  }
  
  @Override
  protected Decision<IntVar> computeDecision(IntVar var) {
    throw new IllegalStateException("It is currently not possible to use computeDecision in OrientedSearchStrategy (we could force to instantiate to 1, but no)");
  }


  public Decision<IntVar> getDecision() {
    if (isInitialPropagation.get()) {
      /*if (initialFreeItemsIds == null)
        initializeInitialData(getFreeItems());*/
      updateSums();
      isInitialPropagation.set(false);
    }
    IntVar bestVar = null;
    long minimumIntersection = Long.MAX_VALUE;
    for (int i = 0; i < vars.length; i++) {
      if (!vars[i].isInstantiated()) {
        BitSet transactionsCovered = itemsExtensions.getFreeItemsCover(i).getTransactions();
        long sum = 0;
        for (int transaction = transactionsCovered.nextSetBit(0); transaction != -1; transaction = transactionsCovered.nextSetBit(transaction+1)) {
          sum += sumsPresenceOfTransaction[transaction];
        }
        if (sum < minimumIntersection) {
          bestVar = vars[i];
          minimumIntersection = sum;
        }
      }
    }
    if (bestVar == null)
      return null;
    return makeIntDecision(bestVar, 1);
  }

  private void updateSums() {
    for (int i = processedHistorySize; i < history.size(); i++) {
      BitSet t = history.get(i).getCover().getTransactions();
      for (int transaction = t.nextSetBit(0); transaction != -1; transaction = t.nextSetBit(transaction+1)) {
        sumsPresenceOfTransaction[transaction] += 1;
      }
    }
    processedHistorySize = history.size();
  }
}
