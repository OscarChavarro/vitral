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
import vsdk.toolkit.media.ZBuffer;

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

Optionally (see `setDepthBufferMode`), each call also exports the depth of
the primary ray of every pixel into a `ZBuffer` of the size of the image:
native ray distances, or depth values normalized as OpenGL would store them
for the same camera (with its near and far planes and the configured
`glDepthRange`), so the raytraced image can be composited with rasterized
geometry (see `DepthBufferMode`). Rows of the depth buffer follow the rows of
the image: row 0 is the top one.
*/
public class ParallelRaytracer
{
    /** Bands per thread: more bands balance the load better */
    private static final int BANDS_PER_THREAD = 8;

    private final int numberOfThreads;
    private ExecutorService executorService;

    private DepthBufferMode depthBufferMode;
    private double openGlDepthRangeNear;
    private double openGlDepthRangeFar;
    /// Depth buffer of the last `execute` call, reused while its size fits
    private ZBuffer depthBuffer;

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
        this.depthBufferMode = DepthBufferMode.NONE;
        this.openGlDepthRangeNear = 0.0;
        this.openGlDepthRangeFar = 1.0;
        this.depthBuffer = null;
    }

    /**
    Selects whether (and how) the next `execute` calls export a depth buffer.
    @param mode kind of depth buffer; null is taken as `NONE`
    */
    public synchronized void setDepthBufferMode(DepthBufferMode mode)
    {
        depthBufferMode = (mode == null) ? DepthBufferMode.NONE : mode;
        if ( depthBufferMode == DepthBufferMode.NONE ) {
            depthBuffer = null;
        }
    }

    /**
    @return kind of depth buffer exported by `execute`
    */
    public synchronized DepthBufferMode getDepthBufferMode()
    {
        return depthBufferMode;
    }

    /**
    Sets the mapping from normalized device depth to window depth used by
    `DepthBufferMode.OPENGL_DEPTH`, as `glDepthRange`. By default [0, 1].
    @param near window depth of the near plane
    @param far window depth of the far plane
    */
    public synchronized void setOpenGlDepthRange(double near, double far)
    {
        openGlDepthRangeNear = near;
        openGlDepthRangeFar = far;
    }

    /**
    @return window depth of the near plane for `DepthBufferMode.OPENGL_DEPTH`
    */
    public synchronized double getOpenGlDepthRangeNear()
    {
        return openGlDepthRangeNear;
    }

    /**
    @return window depth of the far plane for `DepthBufferMode.OPENGL_DEPTH`
    */
    public synchronized double getOpenGlDepthRangeFar()
    {
        return openGlDepthRangeFar;
    }

    /**
    @return depth buffer filled by the last `execute` call, of the size of
    its image, or null if the depth buffer mode is `NONE` or nothing was
    rendered yet. It is reused (overwritten) by the next calls.
    */
    public synchronized ZBuffer getDepthBuffer()
    {
        return depthBuffer;
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
    @param reportProgress true to report in the console the number of
    threads and the progress
    */
    public void execute(RGBImageUncompressed resultingImage,
                        RendererConfiguration rendererConfiguration,
                        SimpleSceneSnapshot sceneSnapshot,
                        boolean reportProgress)
    {
        DepthBufferEncoder depthEncoder = null;
        ZBuffer outDepth = null;

        if ( resultingImage.getXSize() <= 0 || resultingImage.getYSize() <= 0 ) {
            return;
        }
        synchronized ( this ) {
            if ( depthBufferMode != DepthBufferMode.NONE ) {
                if ( depthBuffer == null ||
                     depthBuffer.getXSize() != resultingImage.getXSize() ||
                     depthBuffer.getYSize() != resultingImage.getYSize() ) {
                    depthBuffer = new ZBuffer(resultingImage.getXSize(),
                        resultingImage.getYSize());
                }
                outDepth = depthBuffer;
                depthEncoder = new DepthBufferEncoder(depthBufferMode,
                    sceneSnapshot.getCameraSnapshot(), openGlDepthRangeNear, openGlDepthRangeFar);
            }
        }
        execute(resultingImage, outDepth, depthEncoder, rendererConfiguration,
            sceneSnapshot, reportProgress);
    }

    /**
    Raytraces the scene into the whole image and into a depth buffer given by
    the caller, independently of the configured depth buffer mode.
    @param resultingImage image to fill; its size gives the resolution
    @param outDepth depth buffer of the size of the image to fill
    @param depthMode kind of depth values to write; `NONE` leaves `outDepth`
    untouched
    @param rendererConfiguration quality settings
    @param sceneSnapshot scene to render, seen from its camera snapshot
    @param reportProgress true to report in the console the number of
    threads and the progress
    */
    public void execute(RGBImageUncompressed resultingImage,
                        ZBuffer outDepth,
                        DepthBufferMode depthMode,
                        RendererConfiguration rendererConfiguration,
                        SimpleSceneSnapshot sceneSnapshot,
                        boolean reportProgress)
    {
        DepthBufferEncoder depthEncoder = null;

        if ( outDepth != null && depthMode != null && depthMode != DepthBufferMode.NONE ) {
            if ( outDepth.getXSize() != resultingImage.getXSize() ||
                 outDepth.getYSize() != resultingImage.getYSize() ) {
                throw new IllegalArgumentException(
                    "Depth buffer size must match the image size");
            }
            depthEncoder = new DepthBufferEncoder(depthMode,
                sceneSnapshot.getCameraSnapshot(),
                getOpenGlDepthRangeNear(), getOpenGlDepthRangeFar());
        }
        execute(resultingImage, outDepth, depthEncoder, rendererConfiguration,
            sceneSnapshot, reportProgress);
    }

    private void execute(RGBImageUncompressed resultingImage,
                         ZBuffer outDepth,
                         DepthBufferEncoder depthEncoder,
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
            System.out.println(
                "Starting parallel raytracing with " + numberOfThreads + " threads.");
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
                    depthEncoder != null ? outDepth : null,
                    depthEncoder,
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
            totalElements += tile.getHeight();
        }
        return totalElements;
    }

    private record TileWorker(
        ConcurrentLinkedQueue<RasterTileArea> pendingTiles,
        RGBImageUncompressed resultingImage,
        ZBuffer depthBuffer,
        DepthBufferEncoder depthEncoder,
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
                    depthBuffer,
                    depthEncoder,
                    tile.getStartX(),
                    tile.getStartY(),
                    tile.getEndX(),
                    tile.getEndY());
            }

            return null;
        }
    }
}
