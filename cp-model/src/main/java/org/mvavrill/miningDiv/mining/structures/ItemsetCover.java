package org.mvavrill.miningDiv.mining.structures;

/**
 * A pair containing an itemSet, and a transactionSet.
 * Should be used as a way to represent an itemSet and its associated cover in the dataSet.
 * This can notably be useful to represent solutions.
 */
public class ItemsetCover {
  private final ItemSet itemSet;
  private final TransactionSet cover;
	
  public ItemsetCover(final ItemSet itemset, final TransactionSet cover) {
    this.itemSet = itemset;
    this.cover = cover;
  }  

  public ItemSet getItemSet() {
    return itemSet;
  }

  public TransactionSet getCover() {
    return cover;
  }
  
  @Override
  public String toString() {
    return "[" + itemSet + " " + cover + "]";
  }
}
