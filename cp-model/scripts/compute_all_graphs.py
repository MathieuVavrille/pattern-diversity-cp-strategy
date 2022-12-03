"""
@author Mathieu Vavrille
@date 16/03/2022

From .gph files, in the results folder structure, generate graphes using pyplot.
"""


from compute_pairwise_jaccards import compute_pairwise_jaccards
#from jaccards_to_graph import *
import os,sys
import json

def pairwise_jaccards_to_cdf(pairwises, nb_values=1000):
    pairwises.sort()
    pairwise_index = 0
    cdf = []
    current_sum = 0
    for _x in range(0,nb_values+1):
        x = 100*_x/nb_values
        while (pairwise_index < len(pairwises) and pairwises[pairwise_index] <= x):
            current_sum += 1
            pairwise_index += 1
        cdf.append((x,current_sum/len(pairwises)))
    return cdf

def pairwise_jaccards_to_iterated_average(pairwises, nb_values=13):
    res = []
    sum_vals = 0
    nb_vals = 0
    for i in range(1,nb_values):
        for j in range(i):
            pair = f"{i}-{j}"
            if pair in pairwises:
                sum_vals += pairwises[pair]
                nb_vals += 1
        res.append((i,sum_vals/nb_vals))
    return res



def compute_one_dataset_threshold(input_folder, output_folder):
    dir_list = os.listdir(input_folder)
    for d in dir_list:
        full_path = os.path.join(input_folder, d)
        if full_path[-4:] == ".sol":
            compute_solution_file(full_path, output_folder)
    

def compute_solution_file(input_file, output_folder):
    pairwise_jaccards_file = os.path.join(output_folder, os.path.basename(input_file)[:-4]+".par")
    out_restricted_file = pairwise_jaccards_file[:-4]
    compute_pairwise_jaccards(input_file, pairwise_jaccards_file)
    if os.path.exists(pairwise_jaccards_file):
        with open(pairwise_jaccards_file,"r") as f:
            pairwises = json.load(f)
            with open(out_restricted_file+"-fullCDF.gph","w") as f:
                json.dump(pairwise_jaccards_to_cdf(list(pairwises.values())), f)
            with open(out_restricted_file+f"-average.gph","w") as f:
                json.dump(pairwise_jaccards_to_iterated_average(pairwises), f)

if __name__ == "__main__":
    input_folder = output_folder = sys.argv[1]
    compute_one_dataset_threshold(input_folder, output_folder)
"""if __name__ == "__main__":
    json_file = sys.argv[1]
    with open(json_file, "r") as f:
        pairwises = json.load(f)
    out_restricted_file = json_file[:-4]
    with open(out_restricted_file+"-average10.gph","w") as f:
        json.dump(pairwise_jaccards_to_iterated_average(pairwises), f)
    with open(out_restricted_file+"-fullCDF.gph","w") as f:
        json.dump(pairwise_jaccards_to_cdf(list(pairwises.values())), f)"""
