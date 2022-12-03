package org.mvavrill.miningDiv.mining.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.mvavrill.miningDiv.mining.structures.covers.*;
import org.mvavrill.miningDiv.mining.structures.ItemSet;
import org.mvavrill.miningDiv.mining.structures.TransactionSet;

public class DataSet {
  private final BitSet[] verticalDataBase;
  private final long[][] verticalLongDataBase;
  private final List<ItemSet> horizontalBase;

  private int maxItem = 0;
  private final TransactionSet allTransactions;
  private final CoversComputation covers;

  public DataSet(final String dataSetPath) throws IOException {
    this(dataSetPath, 0);
  }
	
  public DataSet(final String dataSetPath, final int coversMode) throws IOException {
    horizontalBase = new ArrayList<ItemSet>();
    BufferedReader br = new BufferedReader(new FileReader(dataSetPath));
    String items;
    while ((items = br.readLine()) != null) { // iterate over the lines to build the transaction
      if (items.equals("[EOF]"))
        break;
      // if the line is a comment, is empty or is metadata
      if (items.isEmpty() == true || items.charAt(0) == '#' || items.charAt(0) == '%' || items.charAt(0) == '@') {
        continue;
      }
      horizontalBase.add(createTransaction(items));
    }
    br.close();

    // Vertical Representation
    verticalDataBase = new BitSet[getNbrVar()];
    for (int item = 0; item < getNbrVar(); item++) {
      verticalDataBase[item] = new BitSet();
    }
    // Fill the vertical representation
    for (int i = 0; i < horizontalBase.size(); i++) {
      ItemSet itemSet = horizontalBase.get(i);
      BitSet transaction = itemSet.getBitSet();
      for (int item = transaction.nextSetBit(0); item != -1; item = transaction.nextSetBit(item + 1)) {
        verticalDataBase[item].set(i);
      }
    }
    verticalLongDataBase = new long[getNbrVar()][];
    for (int i = 0; i < verticalDataBase.length; i++)
      verticalLongDataBase[i] = verticalDataBase[i].toLongArray();
    
    // Full BitSet for allTransactions
    BitSet tempAllTransactions = new BitSet();
    tempAllTransactions.set(0,horizontalBase.size());
    allTransactions = new TransactionSet(tempAllTransactions);

    if (coversMode == 0)
      covers = new CoversBase(verticalDataBase, allTransactions);
    else if (coversMode == 1)
      covers = new CoversQueued(verticalDataBase, allTransactions);
    else
      covers = new CoversFull(verticalDataBase, allTransactions);
  }

  public List<String> getTokensWithCollection(String str) {
    return Collections.list(new StringTokenizer(str, " ")).stream().map(token -> (String) token)
      .collect(Collectors.toList());
  }

  /**
   * Create a transaction object from a line from the input file
   * @param line a line from input file
   * @return a transaction (an itemset)
   */
  private ItemSet createTransaction(final String line) {
    BitSet items = new BitSet();
    getTokensWithCollection(line).forEach(elt -> {
        int eltVal = Integer.parseInt(elt);
        items.set(eltVal-1);
        maxItem = Math.max(maxItem,eltVal);
      });
    return new ItemSet(items);
  }

  public void init() {
    covers.init();
  }

  public int getMaxItem() {
    return maxItem;
  }

  public int getNbrVar() {
    return maxItem;
  }

  public int getTransactionsSize() {
    return horizontalBase.size();
  }

  public BitSet[] getVerticalDataBase() {
    return verticalDataBase;
  }

  public long[][] getVerticalLongDataBase() {
    return verticalLongDataBase;
  }

  public TransactionSet getAllTransactions() {
    return allTransactions;
  }

  public CoversComputation getCovers() {
    return covers;
  }

  @Override
  public String toString() {
    StringBuilder DataSetContent = new StringBuilder();

    for (ItemSet transaction : horizontalBase) {
      DataSetContent.append(transaction);
      DataSetContent.append("\n");
    }
    return DataSetContent.toString();
  }
}
