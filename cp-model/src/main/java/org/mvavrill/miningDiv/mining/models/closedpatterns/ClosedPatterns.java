package org.mvavrill.miningDiv.mining.models.closedpatterns;

import org.mvavrill.miningDiv.mining.models.FreeItemsExtensions;
import org.mvavrill.miningDiv.mining.util.DataSet;
import org.mvavrill.miningDiv.mining.structures.ItemSet;
import org.mvavrill.miningDiv.mining.structures.TransactionSet;

import org.chocosolver.solver.Cause;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.loop.monitors.IMonitorRestart;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.util.ESat;

import java.util.BitSet;
import java.util.Map;
import java.util.HashMap;

public final class ClosedPatterns extends Propagator<BoolVar> implements FreeItemsExtensions, IMonitorRestart {

  private final DataSet dataset;
  private final int minFreq;
  private final boolean doWeakConsistency;

  private final Map<Integer, TransactionSet> freeItemsCover = new HashMap<Integer, TransactionSet>();

  public ClosedPatterns(final DataSet dataset, final int minFreq, final BoolVar[] vars) {
    this(dataset, minFreq, vars, false);
  }

  public ClosedPatterns(final DataSet dataset, final int minFreq, final BoolVar[] vars, final boolean doWeakConsistency) {
    super(vars);
    this.dataset = dataset;
    this.minFreq = minFreq;
    this.doWeakConsistency = doWeakConsistency;
  }

  @Override
  public void beforeRestart() {
    forcePropagationOnBacktrack();
  }

  @Override
  public TransactionSet getFreeItemsCover(final int item) {
    if (!freeItemsCover.containsKey(item))
      throw new IllegalStateException("The item is not in the map, and cannot be computed");
    return freeItemsCover.get(item);
  }

  @Override
  public BitSet getCurrentCover() {
    throw new IllegalStateException("Cannot return cover using base closedPattern");
  }

  @Override
  public void propagate(int evtmask) throws ContradictionException {
    // handle free items and their cover wrt cov_X
    freeItemsCover.clear(); // re-initialise
		
    // internal structures
    BitSet free_items = new BitSet(); // free items
    BitSet filtered_items = new BitSet(); // items set to 0
    BitSet current_itemset = new BitSet(); // items set to 1 (current closed pattern)
		
    for (int item = 0; item < dataset.getNbrVar(); item++) {
      if (vars[item].isInstantiatedTo(0)) {
        filtered_items.set(item);
      } else if (vars[item].isInstantiatedTo(1)) {
        current_itemset.set(item);
      } else 
        free_items.set(item);
    }
    // cov(X)
    TransactionSet coverPos1 = dataset.getCovers().getCoverOf(new ItemSet(current_itemset));
    if (coverPos1.getTransactions().cardinality() < minFreq)
      this.fails();
    for (int i = filtered_items.nextSetBit(0); i != -1; i = filtered_items.nextSetBit(i + 1))
      if (dataset.getCovers().isIncludedIn(coverPos1, i)) // If the pattern should be extended with a filtered item
        this.fails();
		
    for (int item = free_items.nextSetBit(0); item != -1; item = free_items.nextSetBit(item + 1)) {
      if (dataset.getCovers().isIncludedIn(coverPos1, item)) { // full-extension
        vars[item].removeValue(0, Cause.Null);
        current_itemset.set(item);
        free_items.clear(item);
        dataset.getCovers().pushCover(new ItemSet(current_itemset), coverPos1);
      } else {
        TransactionSet projection = dataset.getCovers().intersectCover(coverPos1, item); 
        // frequency filtering
        if (projection.getTransactions().cardinality() < minFreq ) {
          vars[item].removeValue(1, this);
          filtered_items.set(item);
          free_items.clear(item);
        } else {
          freeItemsCover.put(item, projection); // Memoize the covers of free items for later
        }
      }
    }

    if (!doWeakConsistency) {
      for (int i = filtered_items.nextSetBit(0); i != -1; i = filtered_items.nextSetBit(i + 1)) {
        TransactionSet cover01 = dataset.getCovers().intersectCover(coverPos1, i);
        for (int j = free_items.nextSetBit(0); j != -1; j = free_items.nextSetBit(j + 1)) {
          TransactionSet cover02 = freeItemsCover.get(j);
          if (coverInclusion(cover01, cover02))
            vars[j].removeValue(1, Cause.Null);
        }
      }
    }
  }

  @Override
  public ESat isEntailed() {
    return ESat.TRUE;
  }

  public boolean coverInclusion(TransactionSet cover1, TransactionSet cover2) {
    BitSet T2 = new BitSet();
    BitSet T11 = new BitSet();
    T2 = cover2.getTransactions();
    T11 = (BitSet) T2.clone();
    T11.andNot(cover1.getTransactions());

    if (T11.isEmpty())
      return true;
    return false;
  }

}
