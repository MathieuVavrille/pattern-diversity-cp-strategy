package org.mvavrill.miningDiv.mining.structures.covers;

import org.mvavrill.miningDiv.mining.structures.ItemSet;
import org.mvavrill.miningDiv.mining.structures.TransactionSet;

import java.util.BitSet;

/** 
 * This class is the default implementation of the computation of the covers.
 * When asking to compute a cover, it will intersect the covers of all the items contained in the itemset.
 */
public class CoversBase extends CoversComputation {

  public CoversBase(final BitSet[] itemsCovers, final TransactionSet allTransactions) {
    super(itemsCovers, allTransactions);
  }
  
  @Override
  public TransactionSet getCoverOf(final ItemSet itemSet) {
    if (itemSet.getBitSet().isEmpty())
      return allTransactions;
    BitSet itemSetB = itemSet.getBitSet();
    int item0 = itemSetB.nextSetBit(0);
    TransactionSet res = new TransactionSet(itemsCovers[item0]);
    for (int item=itemSetB.nextSetBit(item0); item!=-1; item=itemSetB.nextSetBit(item+1)) {
      res = res.getIntersection(itemsCovers[item]);
    }
    return res;
  }
}
