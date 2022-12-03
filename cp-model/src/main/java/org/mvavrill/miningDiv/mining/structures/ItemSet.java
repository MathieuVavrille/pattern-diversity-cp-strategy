package org.mvavrill.miningDiv.mining.structures;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

public class ItemSet {
  private final BitSet items;

  //List<Integer> items2;
  //Integer[] items;
	
  public ItemSet(final BitSet itemset) {
    this.items = itemset;
  }
  
  public BitSet getBitSet() {
    return items;
  }

  public boolean subItemSet(ItemSet itemset) {
    return testContains(items, itemset.getBitSet());
  }

  public boolean testContains(BitSet myItemSet, BitSet itemset) {
    BitSet T11 = (BitSet) itemset.clone();
    T11.andNot(myItemSet);
    return T11.isEmpty();
  }

  public boolean isEqualItemSet(final ItemSet itemset) {
    return items.equals(itemset.getBitSet());
  }

  @Override
  public String toString() {
    return items.toString();
  }
}
