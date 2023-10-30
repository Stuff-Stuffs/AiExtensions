package io.github.stuff_stuffs.aiex.common.internal;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.aiex.common.api.AiExApi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadedTaskExecutor {
    private final ExecutorService executor;
    private final List<AiExApi.Job> tasks = new ArrayList<>();

    public ThreadedTaskExecutor() {
        executor = Executors.newWorkStealingPool();
    }

    public void tick() {
        if (tasks.isEmpty()) {
            return;
        }
        final List<Callable<Unit>> callables = new ArrayList<>(tasks.size());
        for (final AiExApi.Job job : tasks) {
            job.preRun();
            callables.add(() -> {
                job.run();
                return Unit.INSTANCE;
            });
        }
        tasks.clear();
        if (callables.size() < 5) {
            for (final Callable<Unit> callable : callables) {
                try {
                    callable.call();
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            try {
                executor.invokeAll(callables);
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void submit(final AiExApi.Job task) {
        tasks.add(task);
    }
}
