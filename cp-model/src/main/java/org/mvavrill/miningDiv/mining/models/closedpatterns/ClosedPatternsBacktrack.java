package org.mvavrill.miningDiv.mining.models.closedpatterns;

import org.mvavrill.miningDiv.mining.models.FreeItemsExtensions;
import org.mvavrill.miningDiv.mining.util.DataSet;
import org.mvavrill.miningDiv.mining.util.IBitSet;
import org.mvavrill.miningDiv.mining.structures.ItemSet;
import org.mvavrill.miningDiv.mining.structures.TransactionSet;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.solver.variables.events.PropagatorEventType;
import org.chocosolver.util.ESat;

import java.util.BitSet;
import java.util.Map;
import java.util.HashMap;

public final class ClosedPatternsBacktrack extends Propagator<BoolVar> implements FreeItemsExtensions {

  private final long[][] itemCovers;
  private final IBitSet cover;
  private final int minFreq;
  private final boolean doWeakConsistency;

  private final Map<Integer, TransactionSet> freeItemsCover = new HashMap<Integer, TransactionSet>();

  public ClosedPatternsBacktrack(final DataSet dataset, final int minFreq, final BoolVar[] vars) {
    this(dataset, minFreq, vars, false);
  }

  public ClosedPatternsBacktrack(final DataSet dataset, final int minFreq, final BoolVar[] vars, final boolean doWeakConsistency) {
    super(vars, PropagatorPriority.QUADRATIC, true);
    this.itemCovers = dataset.getVerticalLongDataBase();
    this.cover = new IBitSet(model, dataset.getTransactionsSize(), true);
    this.minFreq = minFreq;
    this.doWeakConsistency = doWeakConsistency;
  }

  @Override
  public TransactionSet getFreeItemsCover(final int item) {
    if (!freeItemsCover.containsKey(item))
      freeItemsCover.put(item, new TransactionSet(cover.getIntersection(itemCovers[item])));
    return freeItemsCover.get(item);
  }

  @Override
  public BitSet getCurrentCover() {
    return cover.getBitSet();
  }

  @Override
  public void propagate(int vIdx, int mask) throws ContradictionException {
    if (vars[vIdx].getValue() == 1) {
      cover.and(itemCovers[vIdx]);
      if (cover.cardinality() < minFreq)
        this.fails();
    }
    forcePropagate(PropagatorEventType.CUSTOM_PROPAGATION);
  }

  @Override
  public void propagate(int evtmask) throws ContradictionException {
    BitSet free_items = new BitSet();
    BitSet filtered_items = new BitSet();
    for (int item = 0; item < vars.length; item++) {
      if (vars[item].isInstantiatedTo(0)) {
        filtered_items.set(item);
      } else if (vars[item].getDomainSize() == 2) {
        free_items.set(item);
      }
    }


    // Frequency is already checked in the fine propagator
    for (int i = filtered_items.nextSetBit(0); i != -1; i = filtered_items.nextSetBit(i + 1))
      if (cover.isSubsetOf(itemCovers[i])) // If the pattern should be extended with a filtered item
        this.fails();

    
    freeItemsCover.clear(); // re-initialize
    for (int item = free_items.nextSetBit(0); item != -1; item = free_items.nextSetBit(item + 1)) {
      if (cover.isSubsetOf(itemCovers[item])) { // full-extension
        vars[item].instantiateTo(1, this);
        free_items.clear(item);
      }
      else {
        TransactionSet projection = new TransactionSet(cover.getIntersection(itemCovers[item])); 
        // frequency filtering
        if (projection.getTransactions().cardinality() < minFreq ) {
          vars[item].instantiateTo(0, this);
          filtered_items.set(item);
          free_items.clear(item);
        } else {
          freeItemsCover.put(item, projection); // Memoize the covers of free items for later
        }
      }
    }

    if (!doWeakConsistency) {
      for (int i = filtered_items.nextSetBit(0); i != -1; i = filtered_items.nextSetBit(i + 1)) {
        BitSet coverFiltered = cover.getIntersection(itemCovers[i]);
        for (int j = free_items.nextSetBit(0); j != -1; j = free_items.nextSetBit(j + 1)) {
          TransactionSet coverFree = freeItemsCover.get(j);
          if (coverFree.isIncludedIn(coverFiltered))
            vars[j].removeValue(1, this);
        }
      }
    }
  }

  @Override
  public ESat isEntailed() {
    return ESat.TRUE;
  }

  @Override
  public int getPropagationConditions(int vIdx) {
    return IntEventType.instantiation();
  }
}
