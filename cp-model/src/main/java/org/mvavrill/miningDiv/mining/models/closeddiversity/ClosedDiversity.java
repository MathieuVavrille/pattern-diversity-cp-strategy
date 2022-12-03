package org.mvavrill.miningDiv.mining.models.closeddiversity;

import org.mvavrill.miningDiv.mining.util.DataSet;
import org.mvavrill.miningDiv.mining.models.FreeItemsExtensions;
import org.mvavrill.miningDiv.mining.structures.History;
import org.mvavrill.miningDiv.mining.structures.ItemSet;
import org.mvavrill.miningDiv.mining.structures.TransactionSet;

import org.chocosolver.memory.IStateInt;
import org.chocosolver.solver.Cause;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.loop.monitors.IMonitorRestart;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.util.ESat;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ClosedDiversity extends Propagator<BoolVar> implements FreeItemsExtensions, IMonitorRestart {
  private final int minFreq; // Minimum frequency
  // TODO remove static on jMax
  private static double jMax; // 
	
  private DataSet dataset;
  private History history;
	
  /*
    private int[] estimatedFrequenciesActual;
    private Deque<Integer> allCoverSigmaPlus;
    private Deque<int[]> allEstimatedFrequencies;
  //*/
	
  private IStateInt btrk_coverSigmaPlus;
  private IStateInt[] btrk_estimatedFrequencies;
	
  public int nextVar;
  public int jto = 0;
  public int numberVarFiltredByLB = 0;
	
  public BitSet cov = new BitSet();
  public BitSet itemset = new BitSet();

  private final Map<Integer, TransactionSet> freeItemsCover = new HashMap<Integer, TransactionSet>();
	
  public ClosedDiversity(final DataSet dataset, final int minFreq, final double ja, final History history, final BoolVar[] p) {
    super(p);
    this.minFreq = minFreq;
    this.dataset = dataset;

    // initializations for ClosedDiversity
    this.jMax = ja;
    this.history = history;
		
    // backtrackable structures : estimated frequencies and covers of X+
    btrk_estimatedFrequencies = new IStateInt[dataset.getNbrVar()];
    for (int item = 0; item < dataset.getNbrVar(); item++)
      btrk_estimatedFrequencies[item] = this.model.getEnvironment().makeInt(dataset.getVerticalDataBase()[item].cardinality());
    btrk_coverSigmaPlus = this.model.getEnvironment().makeInt(dataset.getTransactionsSize());
		
    nextVar = -1;
  }
	
  public void check_consistency(BitSet itemset, BitSet cover) throws ContradictionException {
    if(!itemset.isEmpty() && !growth_LB(cover)) {
      jto = 0;
      nextVar = -1;
      model.getSolver().setJumpTo(1);
      fails();
    }
  }

  @Override
  public void beforeRestart() {
    forcePropagationOnBacktrack();
  }

  @Override
  public TransactionSet getFreeItemsCover(final int item) {
    if (!freeItemsCover.containsKey(item))
      throw new IllegalStateException("The item is not in the map, and cannot be computed on the fly");
    return freeItemsCover.get(item);
  }

  @Override
  public BitSet getCurrentCover() {
    throw new IllegalStateException("Cannot return cover using base closedDiversity");
  }
	
  @Override
  public void propagate(int evtmask) throws ContradictionException {
    boolean change = false, fullExt = false;
		
    // les structures internes
    BitSet free_items = new BitSet();
    BitSet filtered_items = new BitSet();
    BitSet current_itemset_items = new BitSet();
    for (int item = 0; item < dataset.getNbrVar(); item++) {
      if (vars[item].isInstantiatedTo(0)) {
        filtered_items.set(item);
      } else if (vars[item].isInstantiatedTo(1)) {
        current_itemset_items.set(item);
      } else
        free_items.set(item);
    }
		
    // cov(X)
    TransactionSet coverPos1 = dataset.getCovers().getCoverOf(new ItemSet(current_itemset_items));
    check_consistency(current_itemset_items, coverPos1.getTransactions());
    freeItemsCover.clear();
		
    int diff = 0;
    diff = btrk_coverSigmaPlus.get() - coverPos1.getTransactions().cardinality();
    btrk_coverSigmaPlus.set(coverPos1.getTransactions().cardinality());
		
    int min_freq=coverPos1.getTransactions().cardinality()+1;
    BitSet free_items_prime = (BitSet) free_items.clone();
		
    for (int item=free_items.nextSetBit(0); item!=-1; item=free_items.nextSetBit(item+1)) {
		
      // Estimated frequencies
      int a = btrk_estimatedFrequencies[item].get() - diff;
      if(a < minFreq) {
        BitSet cov_XUx = dataset.getCovers().intersectCover(coverPos1, item).
          getTransactions();
				
        a = cov_XUx.cardinality();
        if (a < minFreq) { // frequency filtering
          btrk_estimatedFrequencies[item].set(a);
          vars[item].removeValue(1, Cause.Null);
          filtered_items.set(item);
          change = true;
          //System.out.println("filtrage - frequence : item " + item);
          continue;
        }
      }
      btrk_estimatedFrequencies[item].set(a);
			
      // full-extension
      if (dataset.getCovers().isIncludedIn(coverPos1, item)) {
        current_itemset_items.set(item);
        vars[item].removeValue(0, Cause.Null);
        dataset.getCovers().pushCover(new ItemSet(current_itemset_items), coverPos1);
        fullExt = true;
        //System.out.println("full-ext : item " + item);
        continue;
      }
      freeItemsCover.put(item,dataset.getCovers().intersectCover(coverPos1, item));
      // filtering by LB
      if (!growth_LB(freeItemsCover.get(item).getTransactions())) { 
        vars[item].removeValue(1, Cause.Null);
        filtered_items.set(item);
        change = true;
        numberVarFiltredByLB++;
        continue;
      }
      // Éléments absents
      for (int k = filtered_items.nextSetBit(0); k != -1; k = filtered_items.nextSetBit(k + 1)) {
        TransactionSet cover01 = dataset.getCovers().intersectCover(coverPos1, k); 
        if (coverInclusion(cover01, dataset.getCovers().intersectCover(coverPos1, item))) {
          vars[item].removeValue(1, Cause.Null);
          filtered_items.set(item);
          change = true;
          break;
        }
      }
			
      // Update the next variable to be instanciated to 1 : 
      // it will be (by default) the next free variable with the lowest estimated Frequency
      if(!change && a < min_freq) {
        min_freq = a;
        nextVar = item;
      }
    }
		
    int item = free_items.nextSetBit(0);
    if ((free_items.cardinality() == 1) && (free_items.equals(free_items_prime)) && 
        (!vars[item].isInstantiated())) {
      int val = -1;
			
      BitSet temp = (BitSet) current_itemset_items.clone();
      ArrayList<BitSet> h = new ArrayList<BitSet>();
      BitSet cop = new BitSet();
      temp.set(item);
			
      h.add(temp);
      cop = coverPos1.getTransactions();
      val = 0;
      if (calcul_LB(cop, h, h.size()) > jMax) {
        vars[item].removeValue(val, Cause.Null);
        nextVar = -1;
        change = true;
      }
    }
		
    if(!change && !fullExt)
      jto++;
		
    cov = (BitSet) coverPos1.getTransactions().clone();
    itemset = (BitSet) current_itemset_items.clone();
		
    BitSet s_positif = new BitSet(), s_negatif = new BitSet(), s_libre = new BitSet();
    for (int i = 0; i < dataset.getNbrVar(); i++) {
      if (vars[i].isInstantiatedTo(1))
        s_positif.set(i);
      else if (vars[i].isInstantiatedTo(0))
        s_negatif.set(i);
      else
        s_libre.set(i);
    }
  }


  @Override
  public ESat isEntailed() {
    return ESat.TRUE;
  }

  public boolean condition(TransactionSet coverPos1, List<Integer> Sigma_ne) {
    for (Integer item : Sigma_ne) {
      if (dataset.getCovers().isIncludedIn(coverPos1, item)) {
        return true;
      }
    }
    return false;
  }

  public boolean coverInclusion(TransactionSet cover1, TransactionSet cover2) {
    BitSet T2 = new BitSet();
    BitSet T11 = new BitSet();
    T2 = cover2.getTransactions();
    T11 = (BitSet) T2.clone();
    T11.andNot(cover1.getTransactions());

    if (T11.isEmpty()) {
      return true;
    }
    return false;
  }

  // lobnury
  public static boolean inclusion(BitSet pattern1, BitSet pattern2) {
    // pattern1 included in pattern2
    BitSet p = new BitSet();
    p = (BitSet) pattern1.clone();
    p.andNot(pattern2);

    if (p.isEmpty()) {
      return true;
    }
    return false;
  }
	
  // lobnury
  public boolean growth_LB(BitSet cov_XUx) {
    boolean growth_lb = true;

    for(int i = 0; i < history.size(); i++) {
      // list of transactions covered by history itemSet Hi
      BitSet cov_Hi = new BitSet();
      /*
        cov_Hi = dataset.getCovers().getCoverOf(new ItemSet(history[i])).getTransactions();
      //*/
      cov_Hi = dataset.getCovers().getCoverOf(history.get(i).getItemSet()).getTransactions();
      BitSet covP_XUx = (BitSet) cov_XUx.clone();
      covP_XUx.andNot(cov_Hi); // proper cover of X
      BitSet covP_Hi = (BitSet) cov_Hi.clone();
      covP_Hi.andNot(cov_XUx); // proper cover of Hi
			
      double val_lb = 0.0;
      double numerateur = 0.0, denominateur = 0.0;
			
      if(covP_XUx.cardinality() < minFreq) {
        numerateur = (double) minFreq - covP_XUx.cardinality();
        denominateur = (double) cov_Hi.cardinality() + covP_XUx.cardinality();
        val_lb = numerateur / denominateur;
      }
      /*else if(covP_Hi.cardinality() < minFreq) {
        numerateur = (double) minFreq - covP_Hi.cardinality();
        denominateur = (double) cov_Hi.cardinality() + covP_XUx.cardinality();
        val_lb = numerateur / denominateur;
        }*/
      //*/
			
      if((val_lb > jMax) || ((val_lb == jMax) && (val_lb == 0))) {
        growth_lb = false;
        break;
      }
    }

    return growth_lb;
  }


  public double calcul_LB(BitSet cov_X, ArrayList<BitSet> history, int nbElt_hist) {
    double val_lb = 0.0;
    if (nbElt_hist != 0) { // |H|
      Iterator<BitSet> iter = history.iterator(); // Historique H

      while (iter.hasNext()) {
        BitSet temp = (BitSet) iter.next().clone(); // Hi € Historique

        // list of transactions covered by history itemSet Hi
        BitSet cov_Hi = new BitSet();
        cov_Hi = dataset.getCovers().getCoverOf(new ItemSet(temp)).getTransactions();
        BitSet covP_x = (BitSet) cov_X.clone();
        covP_x.andNot(cov_Hi); // couverture propre de X
        BitSet covP_Hi = (BitSet) cov_Hi.clone();
        covP_Hi.andNot(cov_X); // couverture propre de Hi
				
        double numerateur = 0.0, denominateur = 0.0;
				
        if(covP_x.cardinality() < minFreq) {
          numerateur = (double) minFreq - covP_x.cardinality();
          denominateur = (double) cov_Hi.cardinality() + covP_x.cardinality();
          val_lb = numerateur / denominateur;
        }
        /*else if(covP_Hi.cardinality() < minFreq) {
          numerateur = (double) minFreq - covP_Hi.cardinality();
          denominateur = (double) cov_Hi.cardinality() + covP_x.cardinality();
          val_lb = numerateur / denominateur;
          }*/
        if ((val_lb > jMax)) //
          break;
      }
    }
    return val_lb;
  }

  public void setHistory(final History history) {
    this.history = history;
  }

  // TODO remove static on jMax
  public static double getjMax() {
    return jMax;
  }

  public int getMinFreq() {
    return minFreq;
  }

  public History getHistory() {
    return history;
  }

  public DataSet getDataset() {
    return dataset;
  }
}
