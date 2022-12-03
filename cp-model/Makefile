compile:
	mvn -q clean compile assembly:single

experiments: compile
	mkdir -p res
	java -jar target/miningDiv-1.0-SNAPSHOT-jar-with-dependencies.jar -dataset chess.dat -threshold 20 -closedDiv 0.05 -o res/closedDiv.out
	java -jar target/miningDiv-1.0-SNAPSHOT-jar-with-dependencies.jar -dataset chess.dat -threshold 20 -orientedRandom Exact -nb_itemsets 76 -o res/orientedSearch.out
	java -jar target/miningDiv-1.0-SNAPSHOT-jar-with-dependencies.jar -dataset chess.dat -threshold 20 -randomSearch -nb_itemsets 76 -o res/randomSearch.out
	java -jar target/miningDiv-1.0-SNAPSHOT-jar-with-dependencies.jar -dataset chess.dat -threshold 20 -postHocStrat orientedRandom -postHocFactor 2 -nb_itemsets 76 -o res/postHoc-oriented.out
	java -jar target/miningDiv-1.0-SNAPSHOT-jar-with-dependencies.jar -dataset chess.dat -threshold 20 -postHocStrat random -postHocFactor 2 -nb_itemsets 76 -o res/postHoc-random.out
	python3 scripts/compute_all_itemsets_covers.py res/closedDiv.out chess.dat
	python3 scripts/compute_all_itemsets_covers.py res/orientedSearch.out chess.dat
	python3 scripts/compute_all_itemsets_covers.py res/randomSearch.out chess.dat
	python3 scripts/compute_all_itemsets_covers.py res/postHoc-oriented.out chess.dat
	python3 scripts/compute_all_itemsets_covers.py res/postHoc-random.out chess.dat
	python3 scripts/compute_all_graphs.py res/
	python3 scripts/plots.py res/

clean:
	mvn -q clean
	rm -rf res
	rm -f legend.pdf experiments-average.pdf experiments-fullCDF.pdf
