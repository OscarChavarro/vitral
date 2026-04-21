import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.scene.SimpleSceneSnapshot;
import vsdk.toolkit.gui.ProgressMonitor;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.render.Raytracer;
import vsdk.toolkit.render.Tile;
import vsdk.toolkit.render.TileGenerationStrategy;
import vsdk.toolkit.render.TileGenerator;

final class RaytracerParallelExecutor implements RaytracerExecutor {
    @Override
    public void run(Raytracer visualizationEngine,
                    RGBImage resultingImage,
                    RendererConfiguration rendererConfiguration,
                    SimpleSceneSnapshot sceneSnapshot,
                    ProgressMonitor reporter)
    {
        int numberOfThreads = Runtime.getRuntime().availableProcessors();
        TileGenerator tileGenerator = new TileGenerator(
            TileGenerationStrategy.LINEAR,
            resultingImage,
            resultingImage.getXSize(),
            resultingImage.getYSize(),
            numberOfThreads);
        ConcurrentLinkedQueue<Tile> pendingTiles =
            new ConcurrentLinkedQueue<>(tileGenerator.getTiles());
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        if ( reporter != null ) {
            reporter.begin();
        }
        try {
            List<Future<Void>> futures =
                startWorkers(
                    executorService,
                    numberOfThreads,
                    pendingTiles,
                    resultingImage,
                    rendererConfiguration,
                    sceneSnapshot);
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
            executorService.shutdownNow();
            if ( reporter != null ) {
                reporter.end();
            }
        }
    }

    private List<Future<Void>> startWorkers(
        ExecutorService executorService,
        int numberOfThreads,
        ConcurrentLinkedQueue<Tile> pendingTiles,
        RGBImage resultingImage,
        RendererConfiguration rendererConfiguration,
        SimpleSceneSnapshot sceneSnapshot)
    {
        ArrayList<Future<Void>> futures =
            new ArrayList<>(numberOfThreads);

        for ( int i = 0; i < numberOfThreads; i++ ) {
            futures.add(executorService.submit(new TileWorker(
                pendingTiles,
                resultingImage,
                rendererConfiguration,
                sceneSnapshot)));
        }
        return futures;
    }

    private void awaitWorkers(
        ConcurrentLinkedQueue<Tile> pendingTiles,
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
        ConcurrentLinkedQueue<Tile> pendingTiles,
        RGBImage resultingImage,
        RendererConfiguration rendererConfiguration,
        SimpleSceneSnapshot sceneSnapshot)
        implements Callable<Void> {

        @Override
        public Void call()
        {
            Tile tile;
            Raytracer raytracer = new Raytracer();

            while ( (tile = pendingTiles.poll()) != null ) {
                raytracer.execute(
                    resultingImage,
                    rendererConfiguration,
                    sceneSnapshot,
                    null,
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
