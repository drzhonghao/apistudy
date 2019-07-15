import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.benchmark.byTask.*;



import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.benchmark.byTask.utils.Algorithm;
import org.apache.lucene.benchmark.byTask.utils.Config;

/**
 * Run the benchmark algorithm.
 * <p>Usage: java Benchmark  algorithm-file
 * <ol>
 * <li>Read algorithm.</li>
 * <li> Run the algorithm.</li>
 * </ol>
 * Things to be added/fixed in "Benchmarking by tasks":
 * <ol>
 * <li>TODO - report into Excel and/or graphed view.</li>
 * <li>TODO - perf comparison between Lucene releases over the years.</li>
 * <li>TODO - perf report adequate to include in Lucene nightly build site? (so we can easily track performance changes.)</li>
 * <li>TODO - add overall time control for repeated execution (vs. current by-count only).</li>
 * <li>TODO - query maker that is based on index statistics.</li>
 * </ol>
 */
public class Benchmark {

  private PerfRunData runData;
  private Algorithm algorithm;
  private boolean executed;
  
  public Benchmark (Reader algReader) throws Exception {
    // prepare run data
    try {
      runData = new PerfRunData(new Config(algReader));
    } catch (Exception e) {
      e.printStackTrace();
      throw new Exception("Error: cannot init PerfRunData!",e);
    }
    
    // parse algorithm
    try {
      algorithm = new Algorithm(runData);
    } catch (Exception e) {
      throw new Exception("Error: cannot understand algorithm!",e);
    }
  }
  
  /**
   * Execute this benchmark 
   */
  public synchronized void  execute() throws Exception {
    if (executed) {
      throw new IllegalStateException("Benchmark was already executed");
    }
    executed = true;
    runData.setStartTimeMillis();
    algorithm.execute();
  }
  
  /**
   * Run the benchmark algorithm.
   * @param args benchmark config and algorithm files
   */
  public static void main(String[] args) {
    exec(args);
  }

  /**
   * Utility: execute benchmark from command line
   * @param args single argument is expected: algorithm-file
   */
  public static void exec(String[] args) {
    // verify command line args
    if (args.length < 1) {
      System.err.println("Usage: java Benchmark <algorithm file>");
      System.exit(1);
    }
    
    // verify input files 
    Path algFile = Paths.get(args[0]);
    if (!Files.isReadable(algFile)) {
      System.err.println("cannot find/read algorithm file: "+algFile.toAbsolutePath()); 
      System.exit(1);
    }
    
    System.out.println("Running algorithm from: "+algFile.toAbsolutePath());
    
    Benchmark benchmark = null;
    try {
      benchmark = new Benchmark(Files.newBufferedReader(algFile, StandardCharsets.UTF_8));
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }

    System.out.println("------------> algorithm:");
    System.out.println(benchmark.getAlgorithm().toString());

    // execute
    try {
      benchmark.execute();
    } catch (Exception e) {
      System.err.println("Error: cannot execute the algorithm! "+e.getMessage());
      e.printStackTrace();
    }

    System.out.println("####################");
    System.out.println("###  D O N E !!! ###");
    System.out.println("####################");
  }

  /**
   * @return Returns the algorithm.
   */
  public Algorithm getAlgorithm() {
    return algorithm;
  }

  /**
   * @return Returns the runData.
   */
  public PerfRunData getRunData() {
    return runData;
  }

}
