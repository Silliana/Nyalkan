package net.nyalkan.render.chunk.build.task;

import net.nyalkan.render.chunk.RenderSection;
import net.nyalkan.render.chunk.build.RenderRegion;
import net.nyalkan.render.chunk.build.TaskDispatcher;
import net.nyalkan.render.chunk.build.thread.BuilderResources;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class ChunkTask {
    public static final boolean BENCH = true;

    protected static TaskDispatcher taskDispatcher;

    public static BuildTask createBuildTask(RenderSection renderSection, RenderRegion renderRegion, boolean highPriority, int generation) {
        return new BuildTask(renderSection, renderRegion, highPriority, generation);
    }

    protected AtomicBoolean cancelled = new AtomicBoolean(false);
    protected final RenderSection section;
    public boolean highPriority = false;

    ChunkTask(RenderSection renderSection) {
        this.section = renderSection;
    }

    public abstract String name();

    public abstract Result runTask(BuilderResources builderResources);

    public void cancel() {
        this.cancelled.set(true);
    }

    public static void setTaskDispatcher(TaskDispatcher dispatcher) {
        taskDispatcher = dispatcher;
    }
    
    public enum Result {
        CANCELLED,
        SUCCESSFUL
    }
}
