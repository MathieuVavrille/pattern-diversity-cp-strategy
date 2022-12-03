package org.mvavrill.miningDiv.mining.models;

import org.mvavrill.miningDiv.mining.structures.TransactionSet;

import java.util.BitSet;

/** An interface to allow strategies to access the covers computed by the propagators.
 * For every free item, the propagators compute the cover of the current itemset with the item.
 * These covers can now be accessed using this interface.
 * It assumes that the propagator are executed before calling `getFreeItemsCover`, otherwise it is not the right cover
 */
public interface FreeItemsExtensions {
  public TransactionSet getFreeItemsCover(final int item);
  public BitSet getCurrentCover();
}
