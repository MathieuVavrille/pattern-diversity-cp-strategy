package org.mvavrill.miningDiv.mining.models.closedpatterns;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

import org.chocosolver.solver.Cause;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;
import org.chocosolver.util.tools.ArrayUtils;

import org.mvavrill.miningDiv.mining.util.DataSet;
import org.mvavrill.miningDiv.mining.structures.covers.CoversComputation;
import org.mvavrill.miningDiv.mining.structures.ItemSet;
import org.mvavrill.miningDiv.mining.structures.TransactionSet;
//import contraintes.model.util.ClosedPatternsCauses;

public final class ClosedPatterns_with_Transactions extends Propagator<BoolVar> {
	BoolVar[] p;
	BoolVar[] T;
	DataSet dataset;
	int seuil;
	CoversComputation covers;
	int nbElt = 0;

	public ClosedPatterns_with_Transactions(DataSet dataset, int seuil, BoolVar[] p, BoolVar[] t) {
		super(ArrayUtils.concat(p, t));
		this.p = p;
		this.T = t;
		this.dataset = dataset;
		this.seuil = seuil;
		this.covers = dataset.getCovers();
	}

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		// cette structure permet de stocker les items libres avec leurs
		// projections par rapport a sigma_positif
		HashMap<Integer, TransactionSet> FreeItemsCover = new HashMap<Integer, TransactionSet>();
		// les structures internes
		BitSet Sigma_negatif = new BitSet();
		BitSet Sigma_libre = new BitSet();
		BitSet Sigma_positif = new BitSet();
		BitSet Sigma_libNeg = new BitSet();
		BitSet Sigma_libPos = new BitSet();
		BitSet beta_libre = new BitSet();
		//BitSet beta_Pos = new BitSet();
		/*
		 * on verifie l'etat des items s'ils sont instanciÃ©s a 0 on l'ajoute
		 * dans sigma_negatif s'ils sont instancies a 1, on les ajoute a
		 * sigma_positif sinon on les ajoute a sigma_libre.
		 */
		for (int item = 0; item < dataset.getNbrVar(); item++) {
			if (p[item].isInstantiatedTo(0)) {
				Sigma_negatif.set(item);

			} else if (p[item].isInstantiatedTo(1)) {
				Sigma_positif.set(item);
			} else {
				Sigma_libre.set(item);
			}
		}

		for (int id_tran = 0; id_tran < dataset.getTransactionsSize(); id_tran++) {
			if (!T[id_tran].isInstantiated()) {
				beta_libre.set(id_tran);
			}
		}
		
		// calcul de la couverture courante de sigma_positif
		TransactionSet coverSigmaPlus = covers.getCoverOf(new ItemSet(Sigma_positif));

		// on vérifie la cohérence entre les Tt et les couvertures des motifs
		BitSet coverSigmaPlusUFree = (BitSet) coverSigmaPlus.getTransactions().clone();
		for (int i : Sigma_libre.stream().toArray()) {
			coverSigmaPlusUFree.and(dataset.getVerticalDataBase()[i]);
		}
		for (int i = 0; i < coverSigmaPlus.getTransactions().length(); i++) {
			boolean t = coverSigmaPlus.getTransactions().get(i);
			if ((coverSigmaPlusUFree.get(i) && T[i].isInstantiatedTo(0)) || (!t && T[i].isInstantiatedTo(1)) ) {
				String msg = "\n\t\t*** Message from ClosedPatterns : ***\n"
						+ "\t\t\t an unconsistancy in transactions variable "
						+ "" + T[i].getName() + " detected\n";
				//System.out.println(msg);
				fails();
				fails(msg, T[i]);
			}
		}

		// Aribi
		TransactionSet projection = null;
		for (int item = Sigma_libre.nextSetBit(0); item != -1; item = Sigma_libre.nextSetBit(item + 1)) {

			// if (dataset.getVerticalDataBase()[item].cardinality() >= seuil) {
			// filtrage si l'item est un full extenstion de l'instanciaition courante
			if (covers.isIncludedIn(coverSigmaPlus, item)) {
				p[item].removeValue(0, Cause.Null);
				//p[item].removeValue(0, ClosedPatternsCauses.FullExtension);
				Sigma_libPos.set(item);
				covers.pushCover(new ItemSet(Sigma_positif), coverSigmaPlus);
			} else {
				// calcul de projection de l'item en entree avec la
				// couverture courante
				// Aribi
				// TransactionSet
				projection = covers.intersectCover(coverSigmaPlus, item);
				// filtrage par rapport au seuil

				if (projection.getTransactions().cardinality() < seuil) {
					p[item].removeValue(1, Cause.Null);
					//p[item].removeValue(1, ClosedPatternsCauses.InfrequentItems);
					Sigma_libNeg.set(item);
				} else {
					/*
					 * si l'item n'est pas filtre par regle 1 et 2 donc il reste
					 * libre on recupere leurs projections pour les utiliser
					 * dans la deuxieme boucle de la 3 eme regle.
					 */

					FreeItemsCover.put(item, projection);
				}
			}
		}

		Sigma_libre.andNot(Sigma_libPos);
		Sigma_libre.andNot(Sigma_libNeg);
		// on verifie s'il reste des items non filtre
		if (!Sigma_libre.isEmpty()) {
			/*
			 * on boucle sur tout les items instancie a 0
			 * 
			 */

			for (int i = Sigma_negatif.nextSetBit(0); i != -1; i = Sigma_negatif.nextSetBit(i + 1)) {
				// calcul de la projection des items negatifs.
				TransactionSet cover01 = covers.intersectCover(coverSigmaPlus, i);

				for (int j = Sigma_libre.nextSetBit(0); j != -1; j = Sigma_libre.nextSetBit(j + 1)) {
					// recuperation de la projection des items libres
					TransactionSet cover02 = FreeItemsCover.get(j);
					if (coverInclusion(cover01, cover02)) {
						p[j].removeValue(1, Cause.Null);
						//p[j].removeValue(1, ClosedPatternsCauses.AbsentItems);
					}
				}
			}
		}
		
		for (int id_tran = beta_libre.nextSetBit(0); id_tran != -1; 
				id_tran = beta_libre.nextSetBit(id_tran + 1)) {
				
			if (!coverSigmaPlus.getTransactions().get(id_tran)) {
				T[id_tran].removeValue(1, Cause.Null);
				//T[id_tran].removeValue(1, ClosedPatternsCauses.UncoveredTransactions);
			} 
			else if (coverSigmaPlus.getTransactions().get(id_tran) && Sigma_libre.isEmpty()) {
				// if
				// (cover_intersect_PosLib.getTransactions().get(id_tran)) {
				T[id_tran].removeValue(0, Cause.Null);
				//T[id_tran].removeValue(0, ClosedPatternsCauses.CoveredTransactions);
			}
			
		}

	}

	@Override
	public ESat isEntailed() {
		// List<Integer> Sigma_negatif = new ArrayList<Integer>();
		// List<Integer> Sigma_positif = new ArrayList<Integer>();
		// for (int item = 0; item < dataset.getMaxItem() + 1; item++) {
		// if (p[item].isInstantiatedTo(0)) {
		// Sigma_negatif.add(item);
		//
		// } else if (p[item].isInstantiatedTo(1)) {
		// Sigma_positif.add(item);
		// }
		// }
		//
		// boolean condition1 = covers.getCoverOf(new
		// ItemSet(Sigma_positif)).getTransactions()
		// .cardinality() < seuil;
		// boolean condition2 = condition(covers.getCoverOf(new
		// ItemSet(Sigma_positif)), Sigma_negatif);
		//
		// if (condition1 || condition2) {
		//
		// return ESat.FALSE;
		// }
		//
		// else
		return ESat.TRUE;

	}
	
	//*
	public void fails(String msg, Variable var) throws ContradictionException {
        //model.getSolver().getEngine().fails(this, null, msg);
        //model.getSolver().getEngine().fails(this, var, msg);
        model.getSolver().throwsException(this, var, msg);
    }
	//*/

	public boolean condition(TransactionSet coverPos1, List<Integer> Sigma_ne) {

		for (Integer item : Sigma_ne) {
			if (covers.isIncludedIn(coverPos1, item)) {

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

	public TransactionSet getIntersection(TransactionSet CoverCurant, BitSet Sigma_libre0) {
		BitSet coverPosLib = new BitSet();
		coverPosLib = (BitSet) CoverCurant.getTransactions().clone();
		for (int Item = Sigma_libre0.nextSetBit(0); Item != -1; Item = Sigma_libre0.nextSetBit(Item + 1)) {
			// intersection entre coverture de sigma_{+} & couverture de
			coverPosLib.and(dataset.getVerticalDataBase()[Item]);
		}
		return new TransactionSet(coverPosLib);
	}

}
