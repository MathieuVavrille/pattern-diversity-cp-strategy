"""
@author Mathieu Vavrille
@date 16/03/2022

From .gph files, in the results folder structure, generate graphes using pyplot.
"""

import os
import json
import matplotlib.pyplot as plt
import sys
from pylab import MaxNLocator
import random

def get_graph(input_file):
    with open(input_file,"r") as f:
        return json.load(f)

def extract_all_plots(input_folder, output_folder, extension, **kwargs):
    fig, ax = plt.subplots()
    # Closed Diversity
    closedDiv = get_graph(os.path.join(input_folder, f"closedDiv{extension}"))
    ax.plot(*(kwargs["scaling"](*zip(*closedDiv))), label="ClosedDiv", linestyle="solid", color="black")
    randomSearch = get_graph(os.path.join(input_folder, f"randomSearch{extension}"))
    ax.plot(*(kwargs["scaling"](*zip(*randomSearch))), label="RandomSearch", linestyle="dashed", color="red")
    postHocRandomSearch = get_graph(os.path.join(input_folder, f"postHoc-random{extension}"))
    ax.plot(*(kwargs["scaling"](*zip(*postHocRandomSearch))), label="PostHoc-Random", linestyle="dashdot", color="red")
    orientedRandom = get_graph(os.path.join(input_folder, f"orientedSearch{extension}"))
    ax.plot(*(kwargs["scaling"](*zip(*orientedRandom))), label="OrientedSearch", linestyle="dashed", color="blue")
    postHocOrientedRandom = get_graph(os.path.join(input_folder, f"postHoc-oriented{extension}"))
    ax.plot(*(kwargs["scaling"](*zip(*postHocOrientedRandom))), label="PostHoc-Oriented", linestyle="dashdot", color="blue")
    ax.set_xlabel(kwargs["xlabel"])
    ax.set_ylabel(kwargs["ylabel"])
    if extension == "-fullCDF.gph":
        ax.set_xscale("log")
    ya = ax.get_xaxis()
    if extension == "-average.gph":
        ya.set_major_locator(MaxNLocator(integer=True))
    fig.savefig(f"{output_folder}/experiments{extension[:-4]}.pdf", bbox_inches='tight',pad_inches = 0)
    ax.legend()
    legendFig = plt.figure("Legend plot")
    legendFig.legend(ax.get_lines(), [t.get_text() for t in ax.get_legend().get_texts()],loc='center')
    #if len(ax.get_lines()) == 13:
    legendFig.savefig(f"{output_folder}/legend.pdf", bbox_inches='tight',pad_inches = 0)
    plt.close(legendFig)
    plt.close(fig)


def parse_file(input_file):
    patterns = []
    covers = []
    if (os.path.exists(input_file)):
        with open(input_file, 'r') as f:
            for line in list(f.readlines()):
                if len(line) == 0:
                    continue
                elements = line[1:-1].split(" ] [ ")
                patterns.append(len(elements[0].split(" ")))
                covers.append(len(elements[1].split(" ")))
    return patterns, covers

def scaling_all_jaccards(x,y):
    return [v/100 for v in x],y

average_nb = 12
def scaling_average(x,y):
    return [x[i]+1 for i in range(average_nb)], [y[i]/100 for i in range(average_nb)]

if __name__ == "__main__":
    input_folder = sys.argv[1]
    extract_all_plots(input_folder, "./", "-average.gph", **{"scaling":scaling_average, "xlabel":"#solutions","ylabel":"Average pairwise Jaccard"})
    extract_all_plots(input_folder, "./", "-fullCDF.gph", **{"scaling":scaling_all_jaccards,"xlabel":"Jaccard γ","ylabel":"Fraction of pairwise Jaccards ≤ γ"})
