package whitebox.parallel;

/**
 * Java Parallel.For Parallel.ForEach Parallel.Tasks
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.Collection;
import java.util.List;

import java.util.concurrent.ExecutionException;

/**
 * A Java Parallel for SMP
 *
 * [24-Jan-13] This class is under development and its parallel methods should
 * be used with caution, if at all. In the near term, it is chiefly being used
 * to provide a place where plugins with parallel algorithms may find out how
 * many processors to use (e.g., how many threads to create). That limit is
 * managed by the Timing Profiler window that can be popped up from the main
 * GUI's Tools menu. Ordinary users will not care to use this tool, thus
 * parallelized plugins will utilize all available processors by default.
 */
public class Parallel {

    static int iCPU = Runtime.getRuntime().availableProcessors();

    /*
     * The get method is for plugins with adjustable parallelism to find the
     * no. of processors that they should use at run time.
     */
    public static int getPluginProcessors() {
        return iCPU;
    }

    /*
     * The set method is for use by Timing Profiler, so it can limit the no.
     * of processors for plugins to use for the purpose of timing tests. 
     */
    public static void setPluginProcessors(int iCPU) {
        Parallel.iCPU = iCPU;
    }

    /**
     * Parallel.Tasks
     */
    public static void Tasks(final Task[] tasks) {
        ExecutorService executor = Executors.newFixedThreadPool(iCPU);
        ArrayList<Future<?>> futures = new ArrayList<>();

        for (final Task task : tasks) {
            Future<?> future = executor.submit(new Runnable() {
                @Override
                public void run() {
                    task.run();
                }
            });
            futures.add(future);
        }

        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException e) {
            } catch (ExecutionException e) {
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
        }
    }

    /**
     * Parallel.ForEach
     * This method using a blocking Callable form of the loopBody and therefore
     * will complete all assigned tasks before continuing.
     */
    public static <T> void ForEach(Iterable<T> parameters, final CallableLoopBody<T> loopBody) {
        ExecutorService executor = Executors.newFixedThreadPool(iCPU);

        ArrayList<Callable<Boolean>> tasks = new ArrayList<>();

        for (final T param : parameters) {
            tasks.add(new Callable() {
                @Override
                public Boolean call() {
                    return loopBody.call(param);
                }
            });
        }
        int success = 0;
        int failure = 0;
        try {
            List<Future<Boolean>> futures = executor.invokeAll(tasks);
            for (Future<Boolean> fut : futures) {
                int ignore = fut.get() ? success++ : failure++;
            }

        } catch (InterruptedException | ExecutionException e) {
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Parallel.ForEach
     */
    public static <T> void ForEach(Iterable<T> parameters, final LoopBody<T> loopBody) {
        ExecutorService executor = Executors.newFixedThreadPool(iCPU);
        ArrayList<Future<?>> futures = new ArrayList<>();

        for (final T param : parameters) {
            Future<?> future = executor.submit(new Runnable() {
                @Override
                public void run() {
                    loopBody.run(param);
                }
            });
            futures.add(future);
        }

        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException e) {
            } catch (ExecutionException e) {
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
//            System.out.println("Executor terminated status: " + executor.isTerminated());
        } catch (InterruptedException e) {
        }
    }

    /**
     * Parallel.For
     */
    public static void For(int start, int end, int step, final LoopBody<Integer> loopBody) {
        ExecutorService executor = Executors.newFixedThreadPool(iCPU);
        ArrayList<Future<?>> futures = new ArrayList<>();
        ArrayList<Partition> partitions = create(start, end, iCPU);

        for (final Partition p : partitions) {
            Future<?> future = executor.submit(new Runnable() {
                @Override
                public void run() {
                    for (int i = p.start; i < p.end; i++) {
                        loopBody.run(i);
                    }
                }
            });
            futures.add(future);
        }

        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException e) {
            } catch (ExecutionException e) {
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
        }
    }

    /**
     * Create Partitions To Turn Parallel.For To Parallel.ForEach
     */
    public static ArrayList<Partition> create(int inclusiveStart, int exclusiveEnd) {
        return create(inclusiveStart, exclusiveEnd, iCPU);
    }

    public static ArrayList<Partition> create(int inclusiveStart, int exclusiveEnd, int cores) {
        //increment
        int total = exclusiveEnd - inclusiveStart;
//        for (int a = inclusiveStart; a < exclusiveEnd; a += step) {
//            total++;
//        }
        double dc = (double) total / cores;
        int ic = (int) dc;

        if (ic <= 0) {
            ic = 1;
        }
        if (dc > ic) {
            ic++;
        }

        //partitions
        ArrayList<Partition> partitions = new ArrayList<>();
        if (total <= cores) {
            for (int i = inclusiveStart; i < exclusiveEnd; i++) {
                Partition p = new Partition();
                p.start = i;
                p.end = i + 1;
                //p.step = step;
                partitions.add(p);
            }
            return partitions;
        }

        int count = inclusiveStart;
        while (count < exclusiveEnd) {
            Partition p = new Partition();
            p.start = count;
            p.end = count + ic;
            //p.step = step;

            partitions.add(p);
            count += ic;

            //boundary check
            if (p.end >= exclusiveEnd) {
                p.end = exclusiveEnd;
                break;
            }
        }

        return partitions;
    }

    /**
     * Unit Test
     */
    public static void main(String[] argv) {
        //sample data
        final ArrayList<String> ss = new ArrayList<>();

        String[] s = {"a", "b", "c", "d", "e", "f", "g"};
        ss.addAll(Arrays.asList(s));
        int m = ss.size();

        Parallel.For(13, 20, 1, new LoopBody<Integer>() {
            @Override
            public void run(Integer i) {
                System.out.println(i);
            }
        });

        //parallel-for loop
        System.out.println("Parallel.For loop:");
        Parallel.For(2, m, 1, new LoopBody<Integer>() {
            @Override
            public void run(Integer i) {
                System.out.println(i + "\t" + ss.get(i));
            }
        });

//        //parallel for-each loop
//        System.out.println("Parallel.ForEach loop:");
//        Parallel.ForEach(ss, new LoopBody<String>() {
//
//            @Override
//            public void run(String p) {
//                System.out.println(p);
//            }
//        });
//
//        //partitioned parallel loop
//        System.out.println("Partitioned Parallel loop:");
//        Parallel.ForEach(Parallel.create(0, m), new LoopBody<Partition>() {
//
//            @Override
//            public void run(Partition p) {
//                for (int i = p.start; i < p.end; i++) {
//                    System.out.println(i + "\t" + ss.get(i));
//                }
//            }
//        });
//
//        //parallel tasks
//        System.out.println("Parallel Tasks:");
//        Parallel.Tasks(new Task[]{
//                    //task-1
//                    new Task() {
//
//            @Override
//                public void run() {
//                    for (int i = 0; i < 3; i++) {
//                        System.out.println(i + "\t" + ss.get(i));
//                    }
//                }
//            },
//                    //task-2
//                    new Task() {
//
//            @Override
//                public void run() {
//                    for (int i = 3; i < 6; i++) {
//                        System.out.println(i + "\t" + ss.get(i));
//                    }
//                }
//            }
//                });
    }
}