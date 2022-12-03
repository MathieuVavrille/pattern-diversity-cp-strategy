package org.mvavrill.miningDiv.mining.structures;

import org.mvavrill.miningDiv.mining.util.DataSet;
import org.mvavrill.miningDiv.mining.util.Jaccard;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.variables.BoolVar;

import org.javatuples.Pair;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.io.FileWriter;
import java.io.IOException;
import java.util.BitSet;

/**
 * This class is mainly a list of ItemsetCover. It can be used to store the solutions of a problem.
 * It implements the IMonitorSolution interface to allow to automatically add the solutions found to the history.
 */
public class History implements IMonitorSolution {

  private final List<ItemsetCover> allItemsets;
  private final BoolVar[] vars;
  private final DataSet dataset;

  /** Basic constructor. It will not record the solutions */
  public History() {
    allItemsets = new ArrayList<ItemsetCover>();
    vars = null;
    dataset = null;
  }

  public History(final List<ItemsetCover> allItemsets) {
    this.allItemsets = allItemsets;
    vars = null;
    dataset = null;
  }

  /** Constructor to use when wanting to record the solutions. */
  public History(final Model model, final DataSet dataset, final BoolVar[] vars) {
    this.allItemsets = new ArrayList<ItemsetCover>();
    this.vars = vars;
    this.dataset = dataset;
    Solver solver = model.getSolver();
    if(!solver.getSearchMonitors().contains(this)) {
      solver.plugMonitor(this);
    }
  }

  /** Returns a new history containing all the ItemsetCovers h[i] for i in idsToExtract */
  public History extractFromIds(final List<Integer> idsToExtract) {
    List<ItemsetCover> extractedItemsets = new ArrayList<ItemsetCover>();
    for (int i: idsToExtract) {
      extractedItemsets.add(allItemsets.get(i));
    }
    return new History(extractedItemsets);
  }

  /** Returns a new history containing all the ItemsetCovers h[i] for start <= i < end */
  public History extractFromRange(final int start, final int end) {
    List<ItemsetCover> extractedItemsets = new ArrayList<ItemsetCover>();
    for (int i = start; i < end; i++) {
      extractedItemsets.add(allItemsets.get(i));
    }
    return new History(extractedItemsets);
  }

  @Override
  public void onSolution() { // Recompute the cover. This could be optimised
    BitSet itemset = new BitSet();
    for (int item = 0; item < dataset.getNbrVar(); item++) {
      if (vars[item].isInstantiatedTo(1))
        itemset.set(item);
    }
    ItemSet itemsetI = new ItemSet(itemset);
    allItemsets.add(new ItemsetCover(itemsetI, dataset.getCovers().getCoverOf(itemsetI)));
  }
	
  public void add(final ItemsetCover s) {
    allItemsets.add(s);
  }

  public ItemsetCover get(final int i) {
    return allItemsets.get(i);
  }
 
  public ItemsetCover[] getAllItemsets() {
    return allItemsets.toArray(new ItemsetCover[0]);
  }
  
  public int size() {
    return allItemsets.size();
  }

  @Override
  public String toString() {
    return allItemsets.toString();
  }

  /** Saves the itemsets (and not the transactions) to the file. The first line contains a first intetger : the number N of itemsets; and then multiple integers for running times T in nanoseconds.
   * Then there are N lines containing each an itemset, as integers separated by spaces.
   */
  public void saveToFile(final String fileName, long... runningTimes) throws IOException {
    FileWriter myWriter = new FileWriter(fileName);
    myWriter.write(""+allItemsets.size());
    for (long runningTime : runningTimes) {
      myWriter.write(" " + runningTime);
    }
    myWriter.write("\n");
    for (ItemsetCover ic : allItemsets) {
      ic.getItemSet().getBitSet().stream().forEach(i -> {try {myWriter.write((i+1) + " ");} catch (IOException e) {throw new RuntimeException(e);}});
      myWriter.write("\n");
    }
    myWriter.close();
  }

  /** Print all Lbs between a solution and all the previous ones */
  public void printLbs(final int minFreq) {
    for (int i = 1; i < allItemsets.size(); i++) {
      for (int j = 0; j < i; j++) {
        System.out.print(Jaccard.lb(allItemsets.get(i).getCover(),allItemsets.get(j).getCover(), minFreq) + " ");
      }
      System.out.println("");
    }
  }

  /** Return the jaccard (of the coverage) of all pairs of solutions in the history */
  private List<Double> computeAllJaccards() {
    List<Double> allJaccards = new ArrayList<Double>();
    for (int i = 0; i < allItemsets.size(); i++) {
      TransactionSet currentCover = allItemsets.get(i).getCover();
      for (int j = 0; j < i; j++) {
        allJaccards.add(Jaccard.exact(currentCover, allItemsets.get(j).getCover()));
      }
    }
    return allJaccards;
  }

  /** Return an array A of size 101 where A[n] (for 0 <= n <= 100) is the minimum j such that at least n% of the distances between two elements of the history have a Jaccard <= j*/
  public double[] computeJaccardPercentageList() {
    double[] A = new double[101];
    double[] allJaccardSorted = computeAllJaccards().stream().mapToDouble(v -> v).sorted().toArray();
    for (int i = 0; i <= 100; i++) {
      A[i] = allJaccardSorted[(int)((allJaccardSorted.length-1)*i/100L)];
    }
    return A;
  }

  public double[] averageJaccards() {
    int cpt = 0;
    double totalSum = 0.;
    double[] averages = new double[allItemsets.size()-1];
    for (int i = 1; i < allItemsets.size(); i++) {
      TransactionSet currentCover = allItemsets.get(i).getCover();
      for (int j = 0; j < i; j++) {
        totalSum += Jaccard.exact(currentCover, allItemsets.get(j).getCover());
        cpt++;
      }
      averages[i-1] = totalSum/cpt;
      if (averages[i-1] == 1.)
        throw new IllegalStateException("the average cannot be 1");
    }
    return averages;
  }

  
  // Functions for a MST of given size
  /** Update the array containing the maximum Jaccard to the current tree, by incrementing the tree with a new id. */
  private void updateMinima(final double[] maxToTree, final int newId) {
    TransactionSet newCover = allItemsets.get(newId).getCover();
    for (int i = 0; i < maxToTree.length; i++) {
      maxToTree[i] = (newId == i) ? Double.POSITIVE_INFINITY : Math.max(maxToTree[i], Jaccard.exact(allItemsets.get(i).getCover(), newCover));
    }
  }
  /** Updates all the structures after a new element has been chosen */
  private void updateStructures(final int newId, final double[] maxToTree, final List<Integer> treeElements, final List<Double> edgesWeights) {
    double newWeight = maxToTree[newId];
    edgesWeights.add(newWeight);
    treeElements.add(newId);
    updateMinima(maxToTree, newId);
  }
  /** Return the minimum edge among all the pairs of elements of the history */
  private Pair<Integer, Integer> getMinimumEdge() {
    double currentMinimum = 2.;
    int start = -1;
    int end = -1;
    for (int i = 1; i < allItemsets.size(); i++) {
      TransactionSet iCover = allItemsets.get(i).getCover();
      for (int j = 0; j < i; j++) {
        double currentJaccard = Jaccard.exact(iCover, allItemsets.get(j).getCover());
        if (currentJaccard == 0.)
          return new Pair<Integer, Integer>(j, i);
        if (currentJaccard < currentMinimum) {
          currentMinimum = currentJaccard;
          start = j;
          end = i;
        }
      }
    }
    return new Pair<Integer, Integer>(start, end);
  }
  /** Returns the index with the minimum distance to the current Tree (argmin(maxToTree)) */
  private int bestIdForNext(final double[] maxToTree) {
    double bestDist = Double.POSITIVE_INFINITY;
    int bestId = -1;
    for (int i = 0; i < maxToTree.length; i++) {
      if (maxToTree[i] < bestDist) {
        bestDist = maxToTree[i];
        bestId = i;
      }
    }
    return bestId;
  }
  /** Returns a minimum spanning tree, based on the complete graph of all the pairs of elements in the history, using Prim's algorithm.
   * @param A parameter `nbElements` controls the size of the resulting tree. If this parameter is set to the number of elements in the history, then the output is a MST. Otherwise, it is a heuristical approach to find a set of distant elements.
   * @return A pair containing the list indices of the output set, and a list containing the weight of the edges of the tree. The actual tree cannot be extracted from this representation. The goal is to use the list of weights as a measure of quality of diversification.
   */
  public Pair<List<Integer>, List<Double>> setFromMST(final int nbElements) {
    double[] maxToTree = new double[allItemsets.size()];
    Pair<Integer, Integer> startingSet = getMinimumEdge();
    List<Integer> treeElements = new ArrayList<Integer>();
    List<Double> edgesWeights = new ArrayList<Double>();
    treeElements.add(startingSet.getValue0());
    updateMinima(maxToTree, startingSet.getValue0());
    updateStructures(startingSet.getValue1(), maxToTree, treeElements, edgesWeights);
    while (treeElements.size() < nbElements) {
      int newId = bestIdForNext(maxToTree);
    updateStructures(newId, maxToTree, treeElements, edgesWeights);
    }
    return new Pair<List<Integer>, List<Double>>(treeElements, edgesWeights);
  }
}
