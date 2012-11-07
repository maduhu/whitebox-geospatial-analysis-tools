/*
 * Copyright 2011 Matt Crinklaw-Vogt
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package whitebox.utilities;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

public class Parallel {

    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService forEachPool = Executors.newFixedThreadPool(NUM_CORES * 2, new NamedThreadFactory("Parallel.For"));
    private static final ForkJoinPool fjPool = new ForkJoinPool(NUM_CORES, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);

    public static <T> void ForEach(final Iterable<T> elements, final Operation<T> operation) {
        try {
            forEachPool.invokeAll(createCallables(elements, operation));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static <T> void ForFJ(final Iterable<T> elements, final Operation<T> operation) {
        fjPool.invokeAll(createCallables(elements, operation));
    }

    public static <T> Collection<Callable<Void>> createCallables(final Iterable<T> elements, final Operation<T> operation) {
        List<Callable<Void>> callables = new LinkedList<Callable<Void>>();
        for (final T elem : elements) {
            callables.add(new Callable<Void>() {

                @Override
                public Void call() {
                    operation.perform(elem);
                    return null;
                }
            });
        }

        return callables;
    }

    public static interface Operation<T> {

        public void perform(T pParameter);
    }
    
    public static void For(int start, int stop, int step,
            final LoopBody<Integer> loopBody) {
        
        ExecutorService executor = Executors.newFixedThreadPool(NUM_CORES);
        List<Future<?>> futures = new LinkedList<Future<?>>();

        for (int i = start; i < stop; i += step) {
            final Integer k = i;
            Future<?> future = executor.submit(new Runnable() {

                @Override
                public void run() {
                    loopBody.run(k);
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
    }

    
    // this is only used for testing the tool
    public static void main(String[] args) {
        Parallel p = new Parallel();
        p.parallelForTest();
    }
    
    int k;
    private void parallelForTest() {
        k = 0;
        Parallel.For(0, 100, 5, new LoopBody <Integer>()
        {
            @Override
            public void run(Integer i)
            {
                k += i;
                System.out.println(i);          
            }
        });
        System.out.println("Sum = "+ k);
        
        // Collection of items to process in parallel
        Collection<Integer> elems = new LinkedList<Integer>();
        for (int i = 0; i < 40; ++i) {
            elems.add(i);
        }
        Parallel.ForEach(elems,
                // The operation to perform with each item
                new Parallel.Operation<Integer>() {

            @Override
            public void perform(Integer param) {
                System.out.println(param);
            }
        ;
    });
    }
    
    
    public interface LoopBody <T> {
        void run(T i);
    }

}
