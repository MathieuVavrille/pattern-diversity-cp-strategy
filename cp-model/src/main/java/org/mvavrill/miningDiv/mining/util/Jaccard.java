package org.mvavrill.miningDiv.mining.util;

import org.mvavrill.miningDiv.mining.structures.ItemSet;
import org.mvavrill.miningDiv.mining.structures.TransactionSet;

import org.javatuples.Pair;

import java.util.BitSet;

/**
 * This class contains static functions to compute the Jaccard between two sets.
 * There are functions to compute the exact Jaccard, the lower bound, or the upper bound.
 * Every function can be applied on BitSets, and is overloaded to be applied on TransactionSets and ItemSets
 */
public class Jaccard {

  /** The exact Jaccard between two sets $J(A,B) = |A \cap B|/|A \cup B|$ */
  public static double exact(final BitSet b1, final BitSet b2) {
    BitSet intersection = (BitSet) b1.clone();
    intersection.and(b2);
    return intersection.cardinality() / ((double) b1.cardinality() + b2.cardinality() - intersection.cardinality());
  }
  /** Exact Jaccard working on ItemSets */
  public static double exact(final ItemSet i1, final ItemSet i2) {
    return exact(i1.getBitSet(), i2.getBitSet());
  }
  /** Exact Jaccard working on TransactionSets */
  public static double exact(final TransactionSet t1, final TransactionSet t2) {
    return exact(t1.getTransactions(), t2.getTransactions());
  }

  /** A lower bound of the Jaccard, $J_{lb}(A,B) = max(0,\theta - |A\B|)/|A \cup B|
   * Remark that A and B do NOT play a symmetric role.
   * The interesting property of this lower bound is its monotonicity according to its first parameter :
   * $\forall C$ superset of $A$, then $J_{lb}(C,B) \ge J_{lb}(A,B)$
   */
  public static double lb(final BitSet b1, final BitSet b2, final int theta) {
    BitSet covP1 = (BitSet) b1.clone();
    covP1.andNot(b2); // proper cover of 1 under 2
    int covP1Card = covP1.cardinality();
    return Math.max(0,theta - covP1Card) / ((double) b2.cardinality() + covP1Card);
  }
  /** Lower bound working on ItemSets */
  public static double lb(final ItemSet i1, final ItemSet i2, final int theta) {
    return lb(i1.getBitSet(), i2.getBitSet(), theta);
  }
  /** Lower bound working on TransactionSets */
  public static double lb(final TransactionSet t1, final TransactionSet t2, final int theta) {
    return lb(t1.getTransactions(), t2.getTransactions(), theta);
  }

  /** An upper bound of the Jaccard, $J_{ub}(A,B) = |A \cap B|/(|A|+max(theta-|A \cap B|, 0)
   * Remark that A and B do NOT play a symmetric role.
   * The interesting property of this upper bound is its anti-monotonicity according to its first parameter :
   * $\forall C$ superset of $A$, then $J_{ub}(C,B) \le J_{ub}(A,B)$
   */
  public static double ub(final BitSet b1, final BitSet b2, final int theta) {
    BitSet cov1and2 = (BitSet) b1.clone();
    cov1and2.and(b2); // intersection of covers
    return cov1and2.cardinality() / ((double) b1.cardinality() + Math.max(theta-cov1and2.cardinality(),0));
  }
  /** Upper bound working on ItemSets */
  public static double ub(final ItemSet i1, final ItemSet i2, final int theta) {
    return ub(i1.getBitSet(), i2.getBitSet(), theta);
  }
  /** Upper bound working on TransactionSets */
  public static double ub(final TransactionSet t1, final TransactionSet t2, final int theta) {
    return ub(t1.getTransactions(), t2.getTransactions(), theta);
  }

  /** Return both the lb and the ub */
  public static Pair<Double,Double> lbAndUb(final BitSet b1, final BitSet b2, final int theta) {
    BitSet cov1and2 = (BitSet) b1.clone();
    cov1and2.and(b2); // proper cover of 1 under 2
    int size1 = b1.cardinality();
    int size2 = b2.cardinality();
    int size12 = cov1and2.cardinality();
    int sizeP1 = size1-size12;
    int sizeP2 = size2-size12;
    return new Pair<Double,Double>(Math.max(0,theta-sizeP1)/((double) size2+sizeP1), size12/((double) size1+Math.max(theta-size12,0)));
    //return new Pair<Double,Double>(lb(b1,b2,theta), ub(b1,b2,theta));
  }
  /** Upper bound working on ItemSets */
  public static Pair<Double,Double> lbAndUb(final ItemSet i1, final ItemSet i2, final int theta) {
    return lbAndUb(i1.getBitSet(), i2.getBitSet(), theta);
  }
  /** Upper bound working on TransactionSets */
  public static Pair<Double,Double> lbAndUb(final TransactionSet t1, final TransactionSet t2, final int theta) {
    return lbAndUb(t1.getTransactions(), t2.getTransactions(), theta);
  }
}
