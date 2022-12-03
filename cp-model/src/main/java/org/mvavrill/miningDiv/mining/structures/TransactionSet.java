package org.mvavrill.miningDiv.mining.structures;

import java.util.BitSet;

public class TransactionSet {
  private final BitSet transactions;
	
  public TransactionSet() {
    this.transactions = new BitSet();
  }

  public TransactionSet(final BitSet b) {
    this.transactions = (BitSet) b.clone();
  }

  /**
   * Returns the intersection @this and an other transactions as a new transaction
   */
  public TransactionSet getIntersection(final TransactionSet t2) {
    return getIntersection((BitSet) t2.transactions);
  }
  /**
   * Returns the intersection of a the transactions of @this and a BitSet as a new transaction
   */
  public TransactionSet getIntersection(final BitSet b2) {
    BitSet intersection = (BitSet) transactions.clone();
    intersection.and(b2);
    return new TransactionSet(intersection);
  }

  /** Tests if this is included in the TransactionSet t2 */
  public boolean isIncludedIn(final TransactionSet t2) {
    return isIncludedIn(t2.transactions);
  }
  /** Tests if this is included in the bitset b2 */
  public boolean isIncludedIn(final BitSet b2) {
    BitSet removed = (BitSet) transactions.clone();
    removed.andNot(b2);
    return removed.isEmpty();
  }

  /**
   * Equality test between this and an other transaction set
   */
  public boolean equals(final TransactionSet t2) {
    return equals(t2.transactions);
  }
  /**
   * Equality test between this and an bitset
   */
  public boolean equals(final BitSet b2) {
    return transactions.equals(b2);
  }
  
  public BitSet getTransactions() {
    return (BitSet) transactions.clone();
  }
	
  @Override
  public String toString() {
    return transactions.toString();
  }

}
