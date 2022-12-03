package org.mvavrill.miningDiv.mining.structures.covers;

import org.mvavrill.miningDiv.mining.structures.ItemSet;
import org.mvavrill.miningDiv.mining.structures.TransactionSet;
import org.mvavrill.miningDiv.mining.structures.ItemsetCover;

import java.util.Map;
import java.util.HashMap;
import java.util.BitSet;

/**
 * This class implements the computation of covers using a queue to memoïze the previously found ItemsetCovers.
 * `pushCover` will add the ItemsetCover to a queue.
 * `getCover` will pop the elements of the queue until either the itemset is found, or a superset of the itemset is found.
 * In this case it will compute the cover by starting from the cover of the superset.
 */
public class CoversFull extends CoversComputation{
  private final Map<ItemSet, TransactionSet> covers = new HashMap<ItemSet, TransactionSet>();

  public CoversFull(final BitSet[] itemsCovers, final TransactionSet allTransactions) {
    super(itemsCovers, allTransactions);
  }

  @Override
  public void pushCover(final ItemSet itemSet, final TransactionSet transactionSet) {
    covers.put(itemSet, transactionSet);
  }

  @Override
  public void init() {
    covers.clear();
  }

  @Override
  public TransactionSet getCoverOf(final ItemSet itemSet) {
    if (itemSet.getBitSet().isEmpty())
      return allTransactions;
    if (covers.containsKey(itemSet))
      return covers.get(itemSet);
    BitSet itemSetB = itemSet.getBitSet();
    int item0 = itemSetB.nextSetBit(0);
    TransactionSet res = new TransactionSet(itemsCovers[item0]);
    for (int item=itemSetB.nextSetBit(item0); item!=-1; item=itemSetB.nextSetBit(item+1)) {
      res = res.getIntersection(itemsCovers[item]);
    }
    pushCover(itemSet, res);
    return res;
  }
}
