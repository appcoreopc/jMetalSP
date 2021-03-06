package org.uma.jmetalsp;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetalsp.observer.Observable;
import org.uma.jmetalsp.util.restartstrategy.RestartStrategy;


/**
 * Created by ajnebro on 18/4/16.
 */
public interface DynamicAlgorithm<Result, T extends ObservedData<?>> extends Algorithm<Result>{
  DynamicProblem<?, ?> getDynamicProblem() ;

  void restart();
  void setRestartStrategy(RestartStrategy<?> restartStrategy);
  Observable<T> getObservable() ;
}
