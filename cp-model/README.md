
# Diversification using CP model

This folder contains the CP model and propagators for finding diverse closed patterns. There are multiple approaches having different properties and running time.

The main contribution of the article, OrientedSearch, is implemented in the file [OrientedSearchStrategy.java](src/main/java/org/mvavrill/miningDiv/mining/models/OrientedSearchStrategy.java)

## TLDR

`make experiments`

It will compile with `make compile`, run the 5 CP approaches with the dataset `chess.dat` and put the results in the folder `res/`, and then launch the python scripts to analyze the results. The python scripts generate a file containing the itemsets and their covers, then computes the values of the Jaccards indices, and then plots the CDF, the average and the scatter plots.

This does only run the CP approaches. The state of the art approaches have to be run independently.

`make clean` removes everything that was created by `make experiments`.

## Running

`make compile` will create a file `target/miningDiv-1.0-SNAPSHOT-jar-with-dependencies.jar` that can then be run using `java -ea -jar target/miningDiv-1.0-SNAPSHOT-jar-with-dependencies.jar [params]`. The list of parameters can be found using the `-h` option. The dataset and the strategy used are the only parameters. If the used strategy is not `closedDiv` then the number of itemsets to generate has to be specified.

## Approaches

### Closed Diversity

The parameter in the command line is `-closedDiv`. If used, the number of itemsets searched (parameter `nb_itemsets`) has to be set to 0 (its default value).

This approach relaxes the problem of finding solutions that have a pairwise Jaccard smaller than a given threshold. The actual Jaccard is not tested, and only the propagation is done using a lower bound of the Jaccard.

### Searched based approaches

The following approaches are search based, i.e. they control the decisions made during the search. When a itemset is found, it is kept, and the search is restarted from scratch. This process is done until the desired number of solutions is found.

These approaches can generate as many itemsets as the user wants, thus the parameter `nb_itemsets` has to be set. They iteratively construct the final set of solutions.

#### Random search

The parameter in the command line is `-randomSearch`.

This search strategy randomly chooses a non instantiated variable and sets it to one.

#### Oriented deterministic search

The parameter in the command line is `-orientedDet`, and it can be set to `Exact` (the default value) or `UB`.

Given a history $H$ of solutions, this search strategy will choose to instantiate to one the variable $X$ that minimizes the value $max_{h \in H} (J(h,P\cup \{X\}))$ where P is the current itemset. When using UB, the upper bound on the Jaccard is used instead of the exact Jaccard as the score.

#### Oriented randomized search

The parameter in the command line is `-orientedRandom`, and it can be set to `Exact` (the default value) or `UB`.

Given a history $H$ of solutions, for each uninstantiated variable $X$ we define $w_X = 1/(max_{h \in H}(J(h,P\cup \{X\}))+\epsilon)$ where P is the current itemset and $\epsilon is a small positive real number. We also define $w'_X = w_X/\sum_{Y} w_Y$. Then a variable is randomly using the probability distribution $(w_{x1}, \ldots, w_{x_k})$. When using UB, the upper bound on the Jaccard is used instead of the exact Jaccard in the weight.

### PostHoc approach

This approach is an ad-hoc algorithm to find diverse solutions. When searching for k solutions, an algorithm is first used to generate K solutions, and then the k most distant solutions from this set of K solutions are found.

The mandatory parameter `postHocFactor` allow to specify $K = postHocFactor * nb_itemsets$.

The algorithm to generate the solutions, set using the mandatory parameter `postHocStrategy` can be either one of the search based approaches, i.e. `random`, `orientedDet`, `orientedRandom`, or the base closed pattern resolution, using `all`. Using `all` is only here as a theoretical baseline, to be used with an infinite factor (10000 will probably do the job), but in practice it will not scale.
