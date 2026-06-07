import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.scene.SimpleSceneSnapshot;
import vsdk.toolkit.gui.feedback.ProgressMonitor;
import vsdk.toolkit.gui.feedback.parallel.ParallelProgressMonitorConsumer;
import vsdk.toolkit.gui.feedback.parallel.ParallelProgressMonitorEvent;
import vsdk.toolkit.gui.feedback.parallel.ParallelProgressMonitorProducer;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.raytracing.SimpleRaytracer;
import vsdk.toolkit.render.raytracing.RasterTileArea;
import vsdk.toolkit.render.raytracing.RasterTileGenerationStrategy;
import vsdk.toolkit.render.raytracing.RasterTileGenerator;

final class RaytracerParallelExecutor implements RaytracerExecutor {
    @Override
    public void run(SimpleRaytracer visualizationEngine,
                    RGBImageUncompressed resultingImage,
                    RendererConfiguration rendererConfiguration,
                    SimpleSceneSnapshot sceneSnapshot,
                    ProgressMonitor reporter)
    {
        int numberOfThreads = Runtime.getRuntime().availableProcessors();
        RasterTileGenerator tileGenerator = new RasterTileGenerator(
            RasterTileGenerationStrategy.LINEAR,
            resultingImage,
            resultingImage.getXSize(),
            resultingImage.getYSize(),
            numberOfThreads);
        System.out.println(
            "Starting parallel raytracing with " + numberOfThreads + " threads.");
        List<RasterTileArea> generatedTiles = tileGenerator.getTiles();
        ConcurrentLinkedQueue<RasterTileArea> pendingTiles =
            new ConcurrentLinkedQueue<>(generatedTiles);
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        ConcurrentLinkedQueue<ParallelProgressMonitorEvent> progressEvents =
            new ConcurrentLinkedQueue<>();
        ParallelProgressMonitorProducer producer =
            new ParallelProgressMonitorProducer(progressEvents);
        ParallelProgressMonitorConsumer consumer =
            new ParallelProgressMonitorConsumer(progressEvents);
        Thread consumerThread = new Thread(consumer, "parallel-progress-monitor-consumer");
        producer.init(calculateTotalProgressElements(generatedTiles));
        consumerThread.start();

        try {
            List<Future<Void>> futures =
                startWorkers(
                    executorService,
                    numberOfThreads,
                    pendingTiles,
                    resultingImage,
                    rendererConfiguration,
                    sceneSnapshot,
                    producer);
            awaitWorkers(pendingTiles, futures);
        }
        catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Parallel raytracing was interrupted", e);
        }
        catch ( ExecutionException e ) {
            throw new IllegalStateException("Parallel raytracing failed", e);
        }
        finally {
            producer.finish();
            executorService.shutdownNow();
            try {
                consumerThread.join();
            }
            catch ( InterruptedException e ) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private long calculateTotalProgressElements(List<RasterTileArea> generatedTiles)
    {
        long totalElements = 0;

        for ( RasterTileArea tile : generatedTiles ) {
            totalElements += tile.getDy();
        }
        return totalElements;
    }

    private List<Future<Void>> startWorkers(
        ExecutorService executorService,
        int numberOfThreads,
        ConcurrentLinkedQueue<RasterTileArea> pendingTiles,
        RGBImageUncompressed resultingImage,
        RendererConfiguration rendererConfiguration,
        SimpleSceneSnapshot sceneSnapshot,
        ProgressMonitor progressReporter)
    {
        ArrayList<Future<Void>> futures =
            new ArrayList<>(numberOfThreads);

        for ( int i = 0; i < numberOfThreads; i++ ) {
            futures.add(executorService.submit(new TileWorker(
                pendingTiles,
                resultingImage,
                rendererConfiguration,
                sceneSnapshot,
                progressReporter)));
        }
        return futures;
    }

    private void awaitWorkers(
        ConcurrentLinkedQueue<RasterTileArea> pendingTiles,
        List<Future<Void>> futures)
        throws InterruptedException, ExecutionException
    {
        for ( Future<Void> future : futures ) {
            future.get();
        }
        if ( !pendingTiles.isEmpty() ) {
            throw new IllegalStateException("Parallel raytracing finished with pending tiles");
        }
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
