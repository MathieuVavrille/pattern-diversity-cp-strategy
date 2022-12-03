package org.mvavrill.miningDiv.mining.models.closeddiversity;

import org.mvavrill.miningDiv.mining.models.FreeItemsExtensions;
import org.mvavrill.miningDiv.mining.util.DataSet;
import org.mvavrill.miningDiv.mining.util.IBitSet;
import org.mvavrill.miningDiv.mining.util.Jaccard;
import org.mvavrill.miningDiv.mining.structures.*;

import org.chocosolver.memory.IStateInt;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.solver.variables.events.PropagatorEventType;
import org.chocosolver.util.ESat;

import org.javatuples.Pair;

import java.util.List;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Map;
import java.util.HashMap;

public final class ClosedDiversityBacktrack extends Propagator<BoolVar> implements FreeItemsExtensions {

  //public static int jaccardCpt = 0;

  private final long[][] itemCovers;
  private final IBitSet cover;
  private final int minFreq;
  private final boolean doWeakConsistency;
  
  private final double jMax; // Maximum Jaccard distance
  private final History history;
  private final boolean checkExactJaccard; // Whether to check the actual Jaccard on solutions or not
  private final boolean checkUB; // Whether to check the upper bounds (to deactivate the diversity check when UB < jMax)
  private final List<Integer> historyIndices; // Indices in the history. Used to deactivate the diversity (we do not want to touch the history)
  private final IStateInt firstHistoryIndexNotUB; // First index in the history that is not satisfied with UB.

  private final Map<Integer, TransactionSet> freeItemsCover = new HashMap<Integer, TransactionSet>();

  public ClosedDiversityBacktrack(final DataSet dataset, final History history, final int minFreq, final double jMax, final BoolVar[] vars) {
    this(dataset, history, minFreq, jMax, vars, true, false, false);
  }

  public ClosedDiversityBacktrack(final DataSet dataset, final History history, final int minFreq, final double jMax, final BoolVar[] vars, final boolean doWeakConsistency, final boolean checkExactJaccard, final boolean checkUB) {
    super(vars, PropagatorPriority.QUADRATIC, true);
    this.itemCovers = dataset.getVerticalLongDataBase();
    this.cover = new IBitSet(model, dataset.getTransactionsSize(), true);
    this.minFreq = minFreq;
    this.jMax = jMax;
    this.history = history;
    this.doWeakConsistency = doWeakConsistency;
    this.checkExactJaccard = checkExactJaccard;
    this.checkUB = checkUB;
    firstHistoryIndexNotUB = model.getEnvironment().makeInt(0);
    historyIndices = new ArrayList<Integer>();
    for (int i = 0; i < history.size(); i++)
      historyIndices.add(i);
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
      if (cover.cardinality() < minFreq || !isLBSatisfied(new TransactionSet(cover.getBitSet()), checkUB)) // Possible only with other constraints involved, otherwise forward checking is sufficient
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


    // Frequency and LB is already checked in the fine propagator
    for (int i = filtered_items.nextSetBit(0); i != -1; i = filtered_items.nextSetBit(i + 1))
      if (cover.isSubsetOf(itemCovers[i])) // If the pattern should be extended with a filtered item
        this.fails();
    
    freeItemsCover.clear(); // re-initialise
    for (int item = free_items.nextSetBit(0); item != -1; item = free_items.nextSetBit(item + 1)) {
      TransactionSet projection = new TransactionSet(cover.getIntersection(itemCovers[item]));
      if (projection.getTransactions().cardinality() < minFreq || !isLBSatisfied(projection, false)) { // frequency and LB filtering
        vars[item].instantiateTo(0, this);
        filtered_items.set(item);
        free_items.clear(item);
      }
      else if (cover.isSubsetOf(itemCovers[item])) { // full-extension
        vars[item].instantiateTo(1, this);
        cover.and(itemCovers[item]);
        free_items.clear(item);
      }
      else {
        freeItemsCover.put(item, projection); // Memoize the covers of free items for later
      }
    }

    if (!doWeakConsistency) {
      for (int i = filtered_items.nextSetBit(0); i != -1; i = filtered_items.nextSetBit(i + 1)) {
        BitSet coverFiltered = cover.getIntersection(itemCovers[i]);
        for (int j = free_items.nextSetBit(0); j != -1; j = free_items.nextSetBit(j + 1)) {
          TransactionSet coverFree = freeItemsCover.get(j);
          if (coverFree.isIncludedIn(coverFiltered))
            vars[j].instantiateTo(0, this);
        }
      }
    }
    
    if (free_items.isEmpty() && checkExactJaccard && !isExactSatisfied(new TransactionSet(cover.getBitSet())))
      this.fails();

    if (!isLBSatisfied(new TransactionSet(cover.getBitSet()), checkUB))
      this.fails();
  }

  @Override
  public ESat isEntailed() {
    if (!isLBSatisfied(new TransactionSet(cover.getBitSet()), checkUB))
       return ESat.FALSE;
    return ESat.TRUE;
  }

  private boolean isLBSatisfied(final TransactionSet covX, final boolean reduceUB) {
    if (reduceUB) {
      int firstIndex = firstHistoryIndexNotUB.get();
      for(int i = firstIndex; i < history.size(); i++) {
        if (i >= historyIndices.size())
          historyIndices.add(i); // This it should be enough to add only one element.
        int currentHistoryIndex = historyIndices.get(i);
        //jaccardCpt++;
        Pair<Double,Double> lbAndUb = Jaccard.lbAndUb(covX, history.get(currentHistoryIndex).getCover(), minFreq);
        double lb = lbAndUb.getValue0();
        if((lb > jMax) || ((lb == jMax) && (lb == 0.0)))
          return false;
        double ub = lbAndUb.getValue1();
        if (ub < jMax) { // do not need to check again history[historyIndices[i]], so swap it, and increment firstIndex
          historyIndices.set(i, historyIndices.get(firstIndex));
          historyIndices.set(firstIndex, currentHistoryIndex); // if i == firstIndex, then nothing happens
          firstIndex++;
        }
      }
      firstHistoryIndexNotUB.set(firstIndex); // We only set it at the end. If the function returned earlier, it means a fail, then we don't need to update the firstIndex.
    }
    else {
      for(int i = firstHistoryIndexNotUB.get(); i < history.size(); i++) {
        //jaccardCpt++;
        double lb = Jaccard.lb(covX, history.get(i).getCover(), minFreq);
        if((lb > jMax) || ((lb == jMax) && (lb == 0.0)))
          return false;
      }
    }
    return true;
  }

  private boolean isExactSatisfied(final TransactionSet covX) {
    for(int i = firstHistoryIndexNotUB.get(); i < history.size(); i++) {
      if (i > historyIndices.size())
        historyIndices.add(i); // This it should be enough to add only one element.
      double exact = Jaccard.exact(covX, history.get(historyIndices.get(i)).getCover());
      //jaccardCpt++;
      if((exact > jMax) || ((exact == jMax) && (exact == 0.0)))
        return false;
    }
    return true;
  }

  public double getjMax() {
    return jMax;
  }

  @Override
  public int getPropagationConditions(int vIdx) {
    return IntEventType.instantiation();
  }
}
