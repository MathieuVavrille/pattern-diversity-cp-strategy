package org.mvavrill.miningDiv.mining.structures.covers;

import org.mvavrill.miningDiv.mining.structures.ItemSet;
import org.mvavrill.miningDiv.mining.structures.TransactionSet;
import org.mvavrill.miningDiv.mining.structures.ItemsetCover;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Deque;

/**
 * This class implements the computation of covers using a queue to memoïze the previously found ItemsetCovers.
 * `pushCover` will add the ItemsetCover to a queue.
 * `getCover` will pop the elements of the queue until either the itemset is found, or a superset of the itemset is found.
 * In this case it will compute the cover by starting from the cover of the superset.
 */
public class CoversQueued extends CoversComputation{
  private final Deque<ItemsetCover> currentCovers = new ArrayDeque<ItemsetCover>();

  public CoversQueued(final BitSet[] itemsCovers, final TransactionSet allTransactions) {
    super(itemsCovers, allTransactions);
  }

  @Override
  public void pushCover(final ItemSet itemSet, final TransactionSet transactionSet) {
    currentCovers.push(new ItemsetCover(itemSet, transactionSet));
  }

  @Override
  public void init() {
    currentCovers.clear();
  }

  @Override
  public TransactionSet getCoverOf(final ItemSet itemSet) {
    if (itemSet.getBitSet().isEmpty())
      return allTransactions;
    while (!currentCovers.isEmpty()) {
      ItemsetCover topObject = currentCovers.peek();
      if (topObject.getItemSet().isEqualItemSet(itemSet)) { // If the cover of the itemset is at the top of the queue
        return topObject.getCover();
      } 
      else if (itemSet.subItemSet(topObject.getItemSet())) { // If a subset of the cover is at the top of the queue, compute the cover
        BitSet diffItemSet = (BitSet) itemSet.getBitSet().clone();
        diffItemSet.andNot(topObject.getItemSet().getBitSet());
        TransactionSet res = topObject.getCover();
        for (int item = diffItemSet.nextSetBit(0); item != -1; item = diffItemSet.nextSetBit(item + 1)) {
          res = res.getIntersection(new TransactionSet(itemsCovers[item]));
        }			
        pushCover(itemSet, res);
        return res;
      } else
        currentCovers.pop();
    }
    // if the itemset's cover is not found (have not been stored), we compute it 
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
