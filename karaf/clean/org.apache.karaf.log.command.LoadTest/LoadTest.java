import org.apache.karaf.log.command.*;


import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clear the last log entries.
 */
@Command(scope = "log", name = "load-test", description = "Load test log.")
@Service
public class LoadTest implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadTest.class);

    @Option(name = "--threads")
    private int threads = 1;

    @Option(name = "--messaged")
    private int messages = 1000;

    @Override
    public Object execute() throws Exception {
        Thread[] th = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            final int idxThread = i;
            th[i] = new Thread(() -> {
                for (int i1 = 0; i1 < messages; i1++) {
                    LOGGER.info("Message {} / {}", idxThread, i1);
                }
            });
        }
        long t0 = System.currentTimeMillis();
        for (Thread thread : th) {
            thread.start();
        }
        for (Thread thread : th) {
            thread.join();
        }
        long t1 = System.currentTimeMillis();
        System.out.println("Time: " + (t1 - t0) + " ms");
        System.out.println("Throughput: " + ((messages * threads) / (t1 - t0 + 0.0)) + " msg/ms");
        return null;
    }

}
