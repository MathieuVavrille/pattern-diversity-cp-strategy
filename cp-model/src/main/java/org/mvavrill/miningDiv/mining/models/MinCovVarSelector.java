
package org.mvavrill.miningDiv.mining.models;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.search.strategy.selectors.variables.VariableSelector;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;

/**
 * A search random search strategy oriented to spaces with better Jaccard.
 * In practice, it will choose the item (i.e. a variable) that minimizes the Upper bound of the Jaccard compared to all the covers in the history, and instantiate it to 1.
 */
public class MinCovVarSelector<V extends Variable> implements VariableSelector<V> {

  private final FreeItemsExtensions itemsExtensions;

  public MinCovVarSelector(final FreeItemsExtensions itemsExtensions) {
    this.itemsExtensions = itemsExtensions;;
  }

  @Override
  public V getVariable(V[] variables) {
    int bestVarId = -1;
    int minCov = Integer.MAX_VALUE;
    for (int i = 0; i < variables.length; i++) {
      if (!variables[i].isInstantiated()) {
        int card = itemsExtensions.getFreeItemsCover(i).getTransactions().cardinality();
        if (card < minCov) {
          minCov = card;
          bestVarId = i;
        }
      }
    }
    if (bestVarId == -1)
      return null;
    return variables[bestVarId];
  }
}
