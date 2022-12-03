package org.mvavrill.miningDiv;

import org.mvavrill.miningDiv.models.PatternMining;
import org.mvavrill.miningDiv.mining.util.DataSet;
import org.mvavrill.miningDiv.mining.structures.History;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.io.FileWriter;
import java.util.Random;
import java.util.concurrent.Callable;


/** This is the command line interface to generate diverse closed itemsets.*/
@Command(name = "miningDiv", mixinStandardHelpOptions = true, version = "mining-diversity 1.0",
         description = "Runs one of the approaches to generate diverse closed itemsets.")
public class MiningDiv implements Callable<Integer> {

  @Option(names = {"-dataset", "-d"}, required = true, description = "The dataset to use. You should input the path.")
  private String datasetName;

  @Option(names = {"-threshold", "-f"}, required = true, description = "The threshold for frequent itemsets, in percentage. The actual frequency will be #transactions*threshold/100.")
  private double threshold;
  
  @Option(names = {"-output", "-o"}, description = "The itemsets output file. By default it will use the dataset name, but change the .dat into .out.")
  private String outItemsetsFile = null;

  //@Option(names = {"-out-time"}, description = "The time output file. By default it will use the output file name, but change the .out into .time.")
  //private String outTimeFile = null;

  @Option(names = {"-time_limit", "-t"}, description = "The time limit (in seconds) for a single experiment. If the time limit is met, the output file will only contain a single line with a 0. Default is one minute")
  private long timeLimit = 60L;

  /** Strategy to use when solving */
  @ArgGroup(exclusive = true, multiplicity = "1")
  RunningStrategy strategy;
  static class RunningStrategy {
    @Option(names = "-closedDiv", required=true, description="Runs the closed diversity algorithm. Need to specify the maximum Jaccard.") double jMax = -1.;
    @Option(names = "-randomSearch", required=true, description="Runs the Random search strategy. The number of solutions has to be specified.") boolean runRandomSearch = false;
    @Option(names = "-orientedDet", required=true, description="Runs the oriented deterministic strategy. Can be specified to use the exact Jaccard (with 'Exact') or the UB (with 'UB') as the scoring function.")  String runOrientedDet;
    @Option(names = "-orientedRandom", required=true, description="Runs the oriented randomized strategy. Can be specified to use the exact Jaccard (with 'Exact') or the UB (with 'UB') as the scoring function.")  String runOrientedRandom;
    @ArgGroup(exclusive = false)
    RunningPostHoc runPostHoc;
    static class RunningPostHoc {
      @Option(names = "-postHocFactor", required=true, description="The factor of the number of solutions to generate. The approach will generate `postHocFactor*nbItemsets` itemsets, and then pick the `nbItemsets` most distant (using a heuristic).") double postHocFactor;
      @Option(names = "-postHocStrat", required=true, description="The strategy to use within the PostHoc algorithm. Can be 'all', 'random', 'orientedDet', or 'orientedRandom'. All corresponds to searching for all itemsets (with the limit of `postHocFactor*nbItemsets` itemsets).") String postHocStrategy;
    }
    @Option(names = "-transactionOriented", required=true, description="Runs the transactionOriented search strategy. The number of solutions has to be specified.") boolean transactionOriented = false;
  }

  @Option(names = {"-nb_itemsets", "-n"}, description = "The number of itemsets to return. Has to be specified for every strategy except closedDiv")
  private long nbItemsets = 0;

  @Option(names = {"-random_seed", "-r"}, description = "Random seed to use to initialize the random number generator. Default is 97")
  private Long seed = 97L;

  @Option(names = {"-verbose", "-v"}, description = "If set, will print stuff to stdout.")
  private boolean verbose;
  

  @Override
  public Integer call() throws Exception, IOException {
    final Random random = new Random(seed);
    if (outItemsetsFile == null)
      outItemsetsFile = datasetName.substring(0, datasetName.length()-4)+"-"+threshold
        +"-"+(strategy.jMax>=0 ? "closedDiv" : "")
        +(strategy.runRandomSearch ? "randomSearch":"")
        +(strategy.runOrientedDet != null ? "orientedDet"+strategy.runOrientedDet:"")
        +(strategy.runOrientedRandom != null ? "orientedRandom"+strategy.runOrientedRandom:"")
        +(strategy.runPostHoc != null ? "-postHoc("+strategy.runPostHoc.postHocStrategy+","+strategy.runPostHoc.postHocFactor+")":"")
        +".out";
    checkParameters();
    final DataSet dataset = new DataSet(datasetName, 1);
    final int minFreq = (int) Math.ceil(threshold*dataset.getTransactionsSize()/100);
    runApproach(dataset, minFreq, random);
    return 0;
  }

  private void runApproach(final DataSet dataset, final int minFreq, final Random random) throws IOException {
    long timeLimitNano = timeLimit*1000000000L;
    History history = null;
    Long startTime = System.nanoTime();
    if (strategy.jMax >= 0.) {
      if (verbose)
        System.out.println("Running closedDiv with jMax="+strategy.jMax);
      history = PatternMining.baseDiversity(dataset, minFreq, strategy.jMax, false).findAllSolutionsTimeLimit(timeLimitNano);
    }
    else if (strategy.runRandomSearch) {
      if (verbose)
        System.out.println("Running randomSearch");
      history = PatternMining.randomSearch(dataset, minFreq, random).findAllSolutions(nbItemsets, timeLimitNano);
    }
    else if (strategy.transactionOriented) {
      if (verbose)
        System.out.println("Running transactionOriented");
      history = PatternMining.transactionOrientedSearch(dataset, minFreq).findAllSolutions(nbItemsets, timeLimitNano);
    }
    else if (strategy.runOrientedDet != null) {
      if (verbose)
        System.out.println("Running orientedDet " + strategy.runOrientedDet);
      history = PatternMining.orientedDeterministicSearch(dataset, minFreq, strategy.runOrientedDet.equals("Exact")).findAllSolutions(nbItemsets, timeLimitNano);
    }
    else if (strategy.runOrientedRandom != null) {
      if (verbose)
        System.out.println("Running orientedRandom " + strategy.runOrientedDet);
      history = PatternMining.orientedRandomSearch(dataset, minFreq, strategy.runOrientedRandom.equals("Exact"), random).findAllSolutions(nbItemsets, timeLimitNano);
    }
    else {
      if (verbose)
        System.out.println("Running PostHoc(" + strategy.runPostHoc.postHocStrategy + "," + strategy.runPostHoc.postHocFactor + ")");
      long bigNbItemsets = Math.round(nbItemsets * strategy.runPostHoc.postHocFactor);
      History bigHistory;
      switch (strategy.runPostHoc.postHocStrategy) {
      case "all":
        bigHistory = PatternMining.baseModel(dataset, minFreq).findAllSolutions(bigNbItemsets, timeLimitNano);
        break;
      case "random":
        bigHistory = PatternMining.randomSearch(dataset, minFreq, random).findAllSolutions(bigNbItemsets, timeLimitNano);
        break;
      case "orientedDet":
        bigHistory = PatternMining.orientedDeterministicSearch(dataset, minFreq, true).findAllSolutions(bigNbItemsets, timeLimitNano);
        break;
      case "orientedRandom":
        bigHistory = PatternMining.orientedRandomSearch(dataset, minFreq, true, random).findAllSolutions(bigNbItemsets, timeLimitNano);
        break;
      default:
        throw new IllegalArgumentException("The postHoc strategy is not allowed. This should be caught on the checkParameters() function");
      }
      long middleTime = System.nanoTime();
      if (bigHistory != null) {// if bigHistory == null, then history = null
        history = bigHistory.extractFromIds(bigHistory.setFromMST((int) nbItemsets).getValue0());
      }
      if (history != null) {
        long endTime = System.nanoTime();
        history.saveToFile(outItemsetsFile, endTime-startTime, endTime-middleTime);
        return;
      }
    }
    if (history != null) {
      history.saveToFile(outItemsetsFile, System.nanoTime()-startTime);
    }
    else {
      FileWriter myWriter = new FileWriter(outItemsetsFile);
      myWriter.write("0 " + timeLimitNano + "\n");
      myWriter.close();
    }
  }

  private void checkParameters() {
    if ((strategy.jMax>=0.) == (nbItemsets!=0))
      throw new IllegalArgumentException("Either you specified a number of itemsets and tried to solve with ClosedDiv, or you tried to solve with an other approach without specifying a number of itemsets. Both are illegal.");
    if (strategy.runPostHoc != null) {
      String strat = strategy.runPostHoc.postHocStrategy;
      if (!strat.equals("all") && !strat.equals("random") && !strat.equals("orientedDet") && !strat.equals("orientedRandom"))
        throw new IllegalArgumentException("The postHoc strategy " + strat + " is not allowed. It must be 'all', 'random', 'orientedDet', or 'orientedRandom'.");
      if (strategy.runPostHoc.postHocFactor <= 1.)
        throw new IllegalArgumentException("The factor in the postHoc strategy must be greater than 1.");
    }
    if (strategy.runOrientedDet != null && !strategy.runOrientedDet.equals("Exact") && !strategy.runOrientedDet.equals("UB"))
      throw new IllegalArgumentException("The orientedDet strategy should be 'Exact' or 'UB'");
    if (strategy.runOrientedRandom != null && !strategy.runOrientedRandom.equals("Exact") && !strategy.runOrientedRandom.equals("UB"))
      throw new IllegalArgumentException("The orientedRandom strategy should be 'Exact' or 'UB'");
  }

  public static void main(String... args) {
    int exitCode = new CommandLine(new MiningDiv()).execute(args);
    System.exit(exitCode);
  }
}
