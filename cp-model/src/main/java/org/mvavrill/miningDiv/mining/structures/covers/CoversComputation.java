package org.mvavrill.miningDiv.mining.structures.covers;

import org.mvavrill.miningDiv.mining.structures.ItemSet;
import org.mvavrill.miningDiv.mining.structures.TransactionSet;

import java.util.BitSet;

public abstract class CoversComputation {
  
  protected final BitSet[] itemsCovers;
  protected final TransactionSet allTransactions;

  public CoversComputation(final BitSet[] itemsCovers, final TransactionSet allTransactions) {
    this.itemsCovers = itemsCovers;
    this.allTransactions = allTransactions;
  }
  
  /** 
   * Add to the CoverComputation a pair itemset/transactionSet of an already computed cover.
   * This allows to memoïze the computations. 
   */
  public void pushCover(final ItemSet itemset, final TransactionSet transactionSet) {}

  /**
   * Re-initialize the cover (to be used as if it was created from scratch)
   */
  public void init() {}
  
  /** 
   * Return the cover associated to the itemSet.
   * It may or may not use memoïzation depending on the implementation.
   */
  public abstract TransactionSet getCoverOf(final ItemSet itemSet);

  /** Checks if the cover is included in the cover of `item` */
  public boolean isIncludedIn(final TransactionSet cover, final int item) {
    return cover.isIncludedIn(itemsCovers[item]);
  }
    
  /**
   * @param cover the current cover
   * @param item an item
   * @return intersection of cover and the cover of `item` 
   */
  public synchronized TransactionSet intersectCover(final TransactionSet cover, final Integer item) {
    return cover.getIntersection(itemsCovers[item]);
  }
  
}
