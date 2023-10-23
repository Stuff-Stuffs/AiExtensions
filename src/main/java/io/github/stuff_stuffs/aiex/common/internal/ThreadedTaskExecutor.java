package io.github.stuff_stuffs.aiex.common.internal;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ForkJoinPool;

public class ThreadedTaskExecutor {
    private final ForkJoinPool pool;
    private final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();

    public ThreadedTaskExecutor() {
        pool = new ForkJoinPool(ForkJoinPool.getCommonPoolParallelism());
    }

    public void tick() {
        final int min = Math.min(queue.size(), pool.getPoolSize());
        final CyclicBarrier barrier = new CyclicBarrier(min + 1);
        for (int i = 0; i < min; i++) {
            queue.add(() -> {
                Runnable r;
                while ((r = queue.poll()) != null) {
                    r.run();
                }
                try {
                    barrier.await();
                } catch (final InterruptedException | BrokenBarrierException ignored) {
                }
            });
        }
        Runnable r;
        while ((r = queue.poll()) != null) {
            r.run();
        }
        try {
            barrier.await();
        } catch (final InterruptedException | BrokenBarrierException ignored) {
        }
    }

    public void submit(final Runnable task) {
        queue.add(task);
    }
}
