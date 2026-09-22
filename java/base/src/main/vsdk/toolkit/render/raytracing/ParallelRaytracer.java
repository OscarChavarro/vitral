package vsdk.toolkit.render.raytracing;

// Java basic classes
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

// VSDK classes
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.scene.SimpleSceneSnapshot;
import vsdk.toolkit.gui.feedback.ProgressMonitor;
import vsdk.toolkit.gui.feedback.parallel.ParallelProgressMonitorConsumer;
import vsdk.toolkit.gui.feedback.parallel.ParallelProgressMonitorEvent;
import vsdk.toolkit.gui.feedback.parallel.ParallelProgressMonitorProducer;
import vsdk.toolkit.media.RGBImageUncompressed;

/**
Raytraces an image with as many threads as processors are available to the
virtual machine. The image is split in horizontal bands (see
`RasterTileGenerator`), several per thread, and each thread takes the next
pending band when it finishes one, so threads stay busy even if some areas of
the image are much more expensive than others. Each thread uses its own
`SimpleRaytracer` over the same (read only) scene snapshot, and writes
disjoint rows of the image.

The threads are created once and reused by every call, so this class suits
interactive use (one image per frame). They are daemon threads; `dispose`
stops them.
*/
public class ParallelRaytracer
{
    /** Bands per thread: more bands balance the load better */
    private static final int BANDS_PER_THREAD = 8;

    private final int numberOfThreads;
    private ExecutorService executorService;

    /**
    Creates a raytracer with one thread per available processor.
    */
    public ParallelRaytracer()
    {
        this(Runtime.getRuntime().availableProcessors());
    }

    /**
    @param numberOfThreads number of threads to render with (at least 1)
    */
    public ParallelRaytracer(int numberOfThreads)
    {
        if ( numberOfThreads <= 0 ) {
            throw new IllegalArgumentException("numberOfThreads must be > 0");
        }
        this.numberOfThreads = numberOfThreads;
        this.executorService = null;
    }

    /**
    @return number of threads used to render
    */
    public int getNumberOfThreads()
    {
        return numberOfThreads;
    }

    private synchronized ExecutorService getExecutorService()
    {
        if ( executorService == null ) {
            AtomicInteger threadCount = new AtomicInteger();
            ThreadFactory factory = runnable -> {
                Thread thread = new Thread(runnable,
                    "parallel-raytracer-" + threadCount.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            };
            executorService = Executors.newFixedThreadPool(numberOfThreads, factory);
        }
        return executorService;
    }

    /**
    Raytraces the scene into the whole image, returning when it is complete.
    @param resultingImage image to fill; its size gives the resolution
    @param rendererConfiguration quality settings
    @param sceneSnapshot scene to render, seen from its camera snapshot
    @param reportProgress true to report the progress in the console
    */
    public void execute(RGBImageUncompressed resultingImage,
                        RendererConfiguration rendererConfiguration,
                        SimpleSceneSnapshot sceneSnapshot,
                        boolean reportProgress)
    {
        if ( resultingImage.getXSize() <= 0 || resultingImage.getYSize() <= 0 ) {
            return;
        }
        RasterTileGenerator tileGenerator = new RasterTileGenerator(
            RasterTileGenerationStrategy.LINEAR,
            resultingImage,
            resultingImage.getXSize(),
            resultingImage.getYSize(),
            numberOfThreads * BANDS_PER_THREAD);
        List<RasterTileArea> generatedTiles = tileGenerator.getTiles();
        ConcurrentLinkedQueue<RasterTileArea> pendingTiles =
            new ConcurrentLinkedQueue<>(generatedTiles);

        ParallelProgressMonitorProducer producer = null;
        Thread consumerThread = null;

        if ( reportProgress ) {
            ConcurrentLinkedQueue<ParallelProgressMonitorEvent> progressEvents =
                new ConcurrentLinkedQueue<>();
            producer = new ParallelProgressMonitorProducer(progressEvents);
            consumerThread = new Thread(
                new ParallelProgressMonitorConsumer(progressEvents),
                "parallel-progress-monitor-consumer");
            producer.init(calculateTotalProgressElements(generatedTiles));
            consumerThread.start();
        }

        try {
            List<Future<Void>> futures = new ArrayList<>(numberOfThreads);
            for ( int i = 0; i < numberOfThreads; i++ ) {
                futures.add(getExecutorService().submit(new TileWorker(
                    pendingTiles,
                    resultingImage,
                    rendererConfiguration,
                    sceneSnapshot,
                    producer)));
            }
            for ( Future<Void> future : futures ) {
                future.get();
            }
            if ( !pendingTiles.isEmpty() ) {
                throw new IllegalStateException("Parallel raytracing finished with pending tiles");
            }
        }
        catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Parallel raytracing was interrupted", e);
        }
        catch ( ExecutionException e ) {
            throw new IllegalStateException("Parallel raytracing failed", e);
        }
        finally {
            if ( producer != null ) {
                producer.finish();
                try {
                    consumerThread.join();
                }
                catch ( InterruptedException e ) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
    Stops the threads. The next `execute` creates them again.
    */
    public synchronized void dispose()
    {
        if ( executorService != null ) {
            executorService.shutdownNow();
            executorService = null;
        }
    }

    private static long calculateTotalProgressElements(List<RasterTileArea> generatedTiles)
    {
        long totalElements = 0;

        for ( RasterTileArea tile : generatedTiles ) {
            totalElements += tile.getDy();
        }
        return totalElements;
    }

    private record TileWorker(
        ConcurrentLinkedQueue<RasterTileArea> pendingTiles,
        RGBImageUncompressed resultingImage,
        RendererConfiguration rendererConfiguration,
        SimpleSceneSnapshot sceneSnapshot,
        ProgressMonitor progressReporter)
        implements Callable<Void> {

        @Override
        public Void call()
        {
            RasterTileArea tile;
            SimpleRaytracer raytracer = new SimpleRaytracer();

            while ( (tile = pendingTiles.poll()) != null ) {
                raytracer.execute(
                    resultingImage,
                    rendererConfiguration,
                    sceneSnapshot,
                    progressReporter,
                    null,
                    tile.getX0(),
                    tile.getY0(),
                    tile.getX1(),
                    tile.getY1());
            }

            return null;
        }
    }
}
