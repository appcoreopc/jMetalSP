package org.uma.jmetalsp.examples.dynamictsp;

import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.SelectionOperator;
import org.uma.jmetal.operator.impl.crossover.PMXCrossover;
import org.uma.jmetal.operator.impl.crossover.SBXCrossover;
import org.uma.jmetal.operator.impl.mutation.PermutationSwapMutation;
import org.uma.jmetal.operator.impl.mutation.PolynomialMutation;
import org.uma.jmetal.operator.impl.selection.BinaryTournamentSelection;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetalsp.DataConsumer;
import org.uma.jmetalsp.DynamicAlgorithm;
import org.uma.jmetalsp.DynamicProblem;
import org.uma.jmetalsp.JMetalSPApplication;
import org.uma.jmetalsp.StreamingDataSource;
import org.uma.jmetalsp.algorithm.indm2.InDM2;
import org.uma.jmetalsp.algorithm.indm2.InDM2Builder;
import org.uma.jmetalsp.consumer.ChartConsumer;
import org.uma.jmetalsp.consumer.ChartInDM2Consumer;
import org.uma.jmetalsp.consumer.LocalDirectoryOutputConsumer;
import org.uma.jmetalsp.examples.streamingdatasource.SimpleStreamingCounterDataSource;
import org.uma.jmetalsp.examples.streamingdatasource.SimpleStreamingDataSourceFromKeyboard;
import org.uma.jmetalsp.impl.DefaultRuntime;
import org.uma.jmetalsp.observeddata.AlgorithmObservedData;
import org.uma.jmetalsp.observeddata.SingleObservedData;
import org.uma.jmetalsp.observer.impl.DefaultObservable;
import org.uma.jmetalsp.problem.fda.FDA2;
import org.uma.jmetalsp.problem.tsp.DynamicMultiobjectiveTSP;
import org.uma.jmetalsp.problem.tsp.MultiobjectiveTSPBuilderFromTSPLIBFiles;
import org.uma.jmetalsp.problem.tsp.TSPMatrixData;
import org.uma.jmetalsp.util.restartstrategy.RestartStrategy;
import org.uma.jmetalsp.util.restartstrategy.impl.CreateNRandomSolutions;
import org.uma.jmetalsp.util.restartstrategy.impl.RemoveNRandomSolutions;
import org.uma.jmetalsp.util.restartstrategy.impl.RemoveNSolutionsAccordingToTheHypervolumeContribution;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Example of SparkSP application.
 * Features:
 * - Algorithm: InDM2
 * - Problem: Bi-objective TSP (using data files from TSPLIB)
 * - Default streaming runtime (Spark is not used)
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
public class InDM2RunnerForTSP {

  public static void main(String[] args) throws IOException, InterruptedException {
    // STEP 1. Create the problem
    DynamicProblem<PermutationSolution<Integer>, SingleObservedData<TSPMatrixData>> problem;
    problem = new MultiobjectiveTSPBuilderFromTSPLIBFiles("data/kroA100.tsp", "data/kroB100.tsp")
            .build();

    // STEP 2. Create and configure the algorithm
    List<Double> referencePoint = new ArrayList<>();
    referencePoint.add(0.0);
    referencePoint.add(0.0);

    CrossoverOperator<PermutationSolution<Integer>> crossover;
    MutationOperator<PermutationSolution<Integer>> mutation;

    crossover = new PMXCrossover(0.9);

    double mutationProbability = 0.2;
    mutation = new PermutationSwapMutation<Integer>(mutationProbability);


    InDM2<PermutationSolution<Integer>> algorithm = new InDM2Builder<>(crossover, mutation, referencePoint, new DefaultObservable<>())
            .setMaxIterations(25000)
            .setPopulationSize(50)
            .build(problem);

    algorithm.setRestartStrategy(new RestartStrategy<>(
            //new RemoveFirstNSolutions<>(50),
            new RemoveNSolutionsAccordingToTheHypervolumeContribution<>(50),
            //new RemoveNSolutionsAccordingToTheCrowdingDistance<>(50),
            //new RemoveNRandomSolutions(50),
            new CreateNRandomSolutions<>()));

    algorithm.setRestartStrategyForReferencePointChange(new RestartStrategy<>(
            new RemoveNRandomSolutions<>(100),
            new CreateNRandomSolutions<PermutationSolution<Integer>>()));

    // STEP 3. Create a streaming data source for the problem and register
    StreamingTSPSource streamingTSPSource = new StreamingTSPSource(new DefaultObservable<>(), 2000);

    streamingTSPSource.getObservable().register(problem);

    // STEP 4. Create a streaming data source for the algorithm and register
    StreamingDataSource<SingleObservedData<List<Double>>> keyboardstreamingDataSource =
            new SimpleStreamingDataSourceFromKeyboard() ;

    keyboardstreamingDataSource.getObservable().register(algorithm);

    // STEP 5. Create the data consumers and register into the algorithm
    DataConsumer<AlgorithmObservedData<PermutationSolution<Integer>>> localDirectoryOutputConsumer =
            new LocalDirectoryOutputConsumer<PermutationSolution<Integer>>("outputdirectory", algorithm);
    DataConsumer<AlgorithmObservedData<PermutationSolution<Integer>>> chartConsumer =
            new ChartConsumer<PermutationSolution<Integer>>(algorithm);

    algorithm.getObservable().register(localDirectoryOutputConsumer);
    algorithm.getObservable().register(chartConsumer) ;

    // STEP 6. Create the application and run
    JMetalSPApplication<
            PermutationSolution<Integer>,
            DynamicProblem<PermutationSolution<Integer>, SingleObservedData<Integer>>,
            DynamicAlgorithm<List<PermutationSolution<Integer>>, AlgorithmObservedData<PermutationSolution<Integer>>>> application;

    application = new JMetalSPApplication<>();

    application.setStreamingRuntime(new DefaultRuntime())
            .setProblem(problem)
            .setAlgorithm(algorithm)
            .addStreamingDataSource(streamingTSPSource)
            .addStreamingDataSource(keyboardstreamingDataSource)
            .addAlgorithmDataConsumer(localDirectoryOutputConsumer)
            .addAlgorithmDataConsumer(chartConsumer)
            .run();
  }
}
