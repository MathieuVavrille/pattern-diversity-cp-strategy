package org.mvavrill.miningDiv.mining.models;

import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.util.ESat;

import org.mvavrill.miningDiv.mining.util.DataSet;
import org.mvavrill.miningDiv.mining.structures.*;
import org.mvavrill.miningDiv.mining.util.Jaccard;

import java.util.BitSet;

/**
 * A propagator for multiple Jaccard constraints.
 * The Jaccard is between the current instantiation, and a history of solutions, such that
 * $\forall h \in history, J(cov(X),cov(h)) \le Jmax
 */
public class MultipleJaccardPropagator extends Propagator<BoolVar> {

  private final int minFreq;
  private final double jMax; // Maximum Jaccard distance
	
  private final DataSet dataset;
  private final History history;
  
  private final boolean checkExactJaccard;
  
  // Data maintained after propagation
  public BitSet itemset; // Current itemset
  public TransactionSet cover; // Current coverage

  /**
   * @param vars the variables representing the itemset
   * @param jMax the bound on the pairwise Jaccard index
   * @param minFreq the threshold on the frequency. Used for the computation of the lower and upper bounds of the Jaccard
   * @param dataset the dataset of transactions
   * @param history a history of previously found solutions, used for the pairwise computations of the Jaccard
   * @param automaticSolutionRecord a boolean to enable automatic recording of solutions. If that is the case, solutions found during the resolution will be directely added to the history
   * @param checkRealJaccard The propagation is based on a relaxation of the Jaccard constraint. Set this boolean to <code>true</code> to check the actual value of the Jaccard when all variables are instantiated.
   */
  public MultipleJaccardPropagator(final BoolVar[] vars, final double jMax, final int minFreq, final DataSet dataset, final History history, final boolean checkExactJaccard) {
    super(vars);
    this.minFreq = minFreq;
    this.dataset = dataset;
    this.history = history;
    this.jMax = jMax;
    this.checkExactJaccard = checkExactJaccard;
  }
		
  @Override
  public void propagate(int evtmask) throws ContradictionException {
    // Compute free/filtered/current itemsets
    BitSet free_items = new BitSet();
    BitSet filtered_items = new BitSet();
    itemset = new BitSet();
    for (int item = 0; item < dataset.getNbrVar(); item++) {
      if (vars[item].isInstantiatedTo(0)) {
        filtered_items.set(item);
      } else if (vars[item].isInstantiatedTo(1)) {
        itemset.set(item);
      } else
        free_items.set(item);
    }
    
    cover = dataset.getCovers().getCoverOf(new ItemSet(itemset));
    if (!isLBSatisfied(cover))
      this.fails();
    
    for (int item=free_items.nextSetBit(0); item!=-1; item=free_items.nextSetBit(item+1)) { // Forward checking
      if (!isLBSatisfied(dataset.getCovers().intersectCover(cover, item))) { 
        vars[item].removeValue(1, this);
        filtered_items.set(item);
        free_items.clear(item);
      }
    }
    if (free_items.isEmpty() && checkExactJaccard && !isExactSatisfied(cover))
      this.fails();
  }


  @Override
  public ESat isEntailed() {
    // Compute free/filtered/current itemsets
    boolean allInstantiated = true;
    itemset = new BitSet();
    for (int item = 0; item < dataset.getNbrVar() && allInstantiated; item++) {
      if (vars[item].isInstantiatedTo(1)) {
        itemset.set(item);
      } else if (!vars[item].isInstantiatedTo(0))
        allInstantiated = false;
    }
    if (allInstantiated) {
      cover = dataset.getCovers().getCoverOf(new ItemSet(itemset));
      if (checkExactJaccard && isExactSatisfied(cover) || !checkExactJaccard && isLBSatisfied(cover))
          return ESat.TRUE;
        else
          return ESat.FALSE;
    }
    else
      return ESat.UNDEFINED;
  }

  private boolean isLBSatisfied(final TransactionSet covX) {
    for(int i = 0; i < history.size(); i++) {			
      double lb = Jaccard.lb(covX, history.get(i).getCover(), minFreq);
      if((lb > jMax) || ((lb == jMax) && (lb == 0.0)))
        return false;
    }
    return true;
  }

  private boolean isExactSatisfied(final TransactionSet covX) {
    for(int i = 0; i < history.size(); i++) {			
      double lb = Jaccard.exact(covX, history.get(i).getCover());
      if((lb > jMax) || ((lb == jMax) && (lb == 0.0)))
        return false;
    }
    return true;
  }
}
