# -*- coding: utf-8 -*-
"""
Created on 1 march 2022

@author: hien
@author : Mathieu Vavrille

Interpreter: Python 3
"""

#from include import *
from datetime import date, datetime
import json
import multiprocessing as mp
import os


# number of parallel launches
npr = 4

nb_max_computed = 1000

def parseResults(path):
    allPatterns = []
    allCoverSet = []
    if (os.path.exists(path)):
        with open(path, 'r') as f:
            content = f.readlines()
            allPatterns = []
            allCovers = []
            for line in content:
                if len(line) == 0:
                    continue
                elements = line.split(" ] [ ")
                allPatterns.append(elements[0].replace("[", " ").strip())
                allCovers.append(elements[1].replace("]", " ").strip())
        allCoverSet = getAllCoversInt(allCovers)
    
    return allPatterns, allCoverSet

def getAllCoversInt(covers):
    coverInt = []
    for cover in covers:
        coverInt.append(set(map(int, set(cover.split(" ")))))
    return coverInt

def evaluate_Jaccard(covX, covY):
    temp = covX
    covX = covX.union(covY)
    covY = covY.intersection(temp)
    jaccard = 0
    if(len(covX) > 0):
        jaccard = len(covY)/len(covX)
    
    return jaccard

def evaluate_one_itemset_diversity(queue, allPairwises, itemset_index, coversSetTab):    
    queue.put(mp.current_process().name)
    
    jaccard = 0.0
    first = False
    for j in range(0, itemset_index):
        jac = evaluate_Jaccard(coversSetTab[itemset_index], coversSetTab[j]) * 100
        cle = str(itemset_index) + "-" + str(j)
        allPairwises[cle] = jac


def evaluate_all_itemsets_diversity(allPairwises, coversSetTab, limit_computation = None):
    procs = dict()
    if limit_computation:
        max_nb_itemsets = min(len(coversSetTab),limit_computation)
    else:
        max_nb_itemsets = len(coversSetTab)
    
    queue = mp.Queue()
    
    curr = 0 # iterator on itemsets to evaluate
    for iterPar in range(0, npr):
        if curr < max_nb_itemsets:
            proc = mp.Process(target=evaluate_one_itemset_diversity, 
                           args=(queue, allPairwises, curr, coversSetTab,))
            proc.start()
            procs[proc.name] = proc
            curr += 1
    
    # using the queue, launch a new process whenever
    # an old process finishes its workload
    while procs:
        name = queue.get()
        proc = procs[name]
        proc.join()
        del procs[name]
        if curr < max_nb_itemsets:
            proc = mp.Process(target=evaluate_one_itemset_diversity, 
                           args=(queue, allPairwises, curr, coversSetTab,))
            proc.start()
            procs[proc.name] = proc
            curr += 1


def managerDict_to_real_dict(md):
    return md.copy()
    """d = {}
    for k,v in md.items():
        d[k] = v
    return d"""

def write_final_results(outputFile, allPairwises):
    with open(outputFile, 'w') as f:
        json.dump(allPairwises, f)

def compute_pairwise_jaccards(input_file, output_file, limit_computation = None):
    _, patterns_covers_tab = parseResults(input_file)
    if len(patterns_covers_tab) > nb_max_computed and limit_computation == None:
        return
    manager = mp.Manager()
    allPairwises_dict = manager.dict()
    evaluate_all_itemsets_diversity(allPairwises_dict, patterns_covers_tab, limit_computation)
    write_final_results(output_file, managerDict_to_real_dict(allPairwises_dict))
        

if __name__ == "__main__":
    sol_file = sys.argv[1]
    pairwises_file = sys.argv[2]
    
    print("\n\n####################################################")
    print("########### diversity evaluation Pairwises ###########")
    print("Date =", date.today(), "- Time =", datetime.now())
    
    all_patterns, patterns_covers_tab = parseResults(sol_file)
    
    manager = mp.Manager()
    allPairwises_dict = manager.dict()
    
    evaluate_all_itemsets_diversity(allPairwises_dict, 
                                    patterns_covers_tab)
    
    all_plotData_file = pairwises_file.replace(".pairs", "_plotData")
    write_final_results(all_plotData_file, managerDict_to_real_dict(allPairwises_dict))
    
    #perl_file = os.path.abspath(os.path.join(project_dir, "scripts", "cdf_div", "jaccard-plots.pl"))
    
    #subprocess.run(["perl " + perl_file + " " + pairwises_file.replace(".pairs", "_plotData") + " > " + pairwises_file.replace(".pairs", ".plt")], shell=True, check=True)
    
    print("\n########## END ##########\n")
    

