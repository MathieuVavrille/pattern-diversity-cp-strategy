'''
@author: 
@date: 12/08/2022
'''

import os
import sys
import multiprocessing as mp
from multiprocessing import Process

#***************************************************************************************************************************************************
#***************************************************************************************************************************************************

def nbrTrans(dataset_file):
    with open(dataset_file, "r") as infile:
        nbr_trans = 0
        for line in infile:
            if line.rstrip() and line[0] != '@':
                nbr_trans += 1
        return nbr_trans

#*************************************************************************************************************
#*************************************************************************************************************

def get_one_pattern_cover(dataset_transactions, pattern, all_coverages_dict):
    if pattern in all_coverages_dict:
        return all_coverages_dict[pattern]
    #---------------------------------------------------------------------------------------------
    cov = set()
    if pattern != "":
        pattern_by_item = pattern.split(" ")
        for item in pattern_by_item:
            if item not in all_coverages_dict:
                cov_item = set()
                for tr in range(0, len(dataset_transactions)):
                    if item in set(dataset_transactions[tr]):
                        cov_item.add(tr+1)
                all_coverages_dict[item] = cov_item
            #
            if len(cov) == 0:
                cov = all_coverages_dict[item]
            else:
                cov = cov.intersection(all_coverages_dict[item])
    else:
        nb_lines = len(dataset_transactions)
        map(lambda t: cov.add(t), [i for i in range(1, nb_lines+1)]) # empty pattern covers all transactions
    #---------------------------------------------------------------------------------------------
    all_coverages_dict[pattern] = cov
    #return list(cov)

#*************************************************************************************************************
#*************************************************************************************************************

def compute_one_batch_of_patterns_covers(params):
    (queue, dataset_transactions, all_patterns, all_covers_dict, begin, end) = params
    try:
        for i in range(begin, end):
            pattern = all_patterns[i]
            get_one_pattern_cover(dataset_transactions, pattern, all_covers_dict)
    finally:
        queue.put(mp.current_process().name)
    
    

#*************************************************************************************************************
#*************************************************************************************************************

def compute_all_patterns_covers(dataset_transactions, all_patterns, all_covers_dict):
    npr = 5
    curr = 0
    procs = dict()
    queue = mp.Queue()
    params = (queue, dataset_transactions, all_patterns, all_covers_dict)
    for i in range(0, npr):
        if curr < len(all_patterns):
            begin = curr
            end = min(curr+100, len(all_patterns)) # take all batches of 100 patterns till the end
            proc = Process(target=compute_one_batch_of_patterns_covers, args=(params + (begin, end), ))
            proc.start()
            procs[proc.name] = proc
            curr += 100
    # using the queue, launch a new process whenever an old process finishes its workload
    while procs:
        name = queue.get()
        proc = procs[name]
        proc.join()
        del procs[name]
        if curr < len(all_patterns):
            begin = curr
            end = min(curr+100, len(all_patterns)) # take all batches of 100 patterns till the end
            proc = Process(target=compute_one_batch_of_patterns_covers, args=(params + (begin, end), ))
            proc.start()
            procs[proc.name] = proc
            curr += 100


#*************************************************************************************************************
#*************************************************************************************************************

def read_file(u_file, code):
    """
        this function is used to read input and dataset file
        we use a code to distinguish between the two files : 'd' for dataset and 'i' for input file
    """
    all_lines = []
    with open(u_file, "r") as lines:
        first_line = True
        for line in lines:
            if first_line and (code == 'i'): # ignore first line
                first_line = False
                continue
            #--------------------------------------------------------------------------
            line_items = ""
            if code == 'i':
                line_items = line.strip()
            elif code == 'd':
                line_items = line.strip().split(" ")
            all_lines.append(line_items)
    
    return all_lines

#*************************************************************************************************************
#*************************************************************************************************************

def save_to_output_file(output_file, all_patterns, all_covers_dict):
    output_dir = os.path.dirname(os.path.realpath(output_file))
    os.makedirs(output_dir, exist_ok=True)
    #----------------------------------------------------------------------------------
    with open(output_file, 'w') as f:
        for i in range(0, len(all_patterns)):
            pattern = all_patterns[i]
            cov = " ".join(list(map(str, list(all_covers_dict[pattern]))))
            line = "[ " + all_patterns[i]+ " ] [ " + cov + " ]"
            f.write(line+"\n")
    #----------------------------------------------------------------------------------
    #print("\nResults saved at", os.path.abspath(os.path.join(output_dir, os.path.basename(output_file))))

#*************************************************************************************************************
#*************************************************************************************************************

def compute_from_files(input_file, dataset_file, output_file):
    if not os.path.exists(dataset_file):
        print("ERROR: dataset file is missing!")
        sys.exit(1)
    if not os.path.exists(input_file):
        print("ERROR: input file is missing!")
        sys.exit(1)
    #----------------------------------------------------------------------------------
    manager = mp.Manager()
    all_covers_dict = manager.dict() # a dictionnary to save all patterns cover
    #----------------------------------------------------------------------------------
    all_patterns = read_file(input_file, 'i')
    dataset_transactions = read_file(dataset_file, 'd')
    compute_all_patterns_covers(dataset_transactions, all_patterns, all_covers_dict)
    #----------------------------------------------------------------------------------
    save_to_output_file(output_file, all_patterns, all_covers_dict)
    

if __name__ == '__main__':
    input_file = sys.argv[1]
    dataset_file = sys.argv[2]
    compute_from_files(input_file, dataset_file, input_file[:-4]+".sol")
    
    

