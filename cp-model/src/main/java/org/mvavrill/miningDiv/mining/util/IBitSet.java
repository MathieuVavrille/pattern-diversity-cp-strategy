package org.mvavrill.miningDiv.mining.util;

import org.chocosolver.memory.IStateLong;
import org.chocosolver.solver.Model;

import java.util.BitSet;
import java.util.Arrays;

/**
 * A backrackable BitSet implementation
 * WARNING !!! Initially the bitset is full of ones.
 * @author Charles Vernerey
 * @author Mathieu Vavrille
 */
public class IBitSet {

  private final IStateLong[] words;

  public IBitSet(final Model model, final int nbits, final boolean startValue) {
    BitSet temp = new BitSet(nbits);
    temp.set(0, nbits, startValue);
    long[] wordsValue = temp.toLongArray();
    words = new IStateLong[wordsValue.length];
    for (int i = 0; i < words.length; i++) {
      words[i] = model.getEnvironment().makeLong(wordsValue[i]);
    }
  }

  public boolean isEmpty() {
    for (IStateLong word : words)
      if (word.get() != 0)
        return false;
    return true;
  }

  public int cardinality() {
    int res = 0;
    for (IStateLong word : words)
      res += Long.bitCount(word.get());
    return res;
  }

  public void and(long[] andWords) {
    for (int j = 0; j < andWords.length; j++) {
      words[j].set(words[j].get() & andWords[j]);
    }
    for (int j = andWords.length; j < words.length; j++) { // andWords[j] = 0, but not represented
      words[j].set(0);
    }
  }

  public BitSet getIntersection(long[] andWords) {
    BitSet res = getBitSet();
    res.and(BitSet.valueOf(andWords));
    return res;
  }

  public int andCount(long[] andWords) {
    int res = 0;
    for (int j = 0; j < andWords.length; j++) {
      res += Long.bitCount(words[j].get() & andWords[j]);
    }
    return res;
  }

  public boolean isSubsetOf(long[] setWords) {
    for (int j = 0; j < setWords.length; j++) {
      long currentWord = words[j].get();
      if ((currentWord & setWords[j]) != currentWord)
        return false;
    }
    for (int j = setWords.length; j < words.length; j++) { // setWords[j] = 0, but not represented
      if (words[j].get() != 0)
        return false;
    }
    return true;
  }

  public BitSet getBitSet() {
    final long[] wordsCopy = new long[words.length];
    for (int i = 0; i < words.length; i++) {
      wordsCopy[i] = words[i].get();
    }
    return BitSet.valueOf(wordsCopy);
  }
}
