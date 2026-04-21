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
import vsdk.toolkit.media.RGBPixel;
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
            new ConcurrentLinkedQueue<Tile>(tileGenerator.getTiles());
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        if ( reporter != null ) {
            reporter.begin();
        }
        try {
            List<Future<List<TileRenderResult>>> futures =
                startWorkers(
                    executorService,
                    numberOfThreads,
                    pendingTiles,
                    resultingImage,
                    rendererConfiguration,
                    sceneSnapshot);
            joinRenderedTiles(resultingImage, pendingTiles, futures);
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

    private List<Future<List<TileRenderResult>>> startWorkers(
        ExecutorService executorService,
        int numberOfThreads,
        ConcurrentLinkedQueue<Tile> pendingTiles,
        RGBImage resultingImage,
        RendererConfiguration rendererConfiguration,
        SimpleSceneSnapshot sceneSnapshot)
    {
        ArrayList<Future<List<TileRenderResult>>> futures =
            new ArrayList<Future<List<TileRenderResult>>>(numberOfThreads);

        for ( int i = 0; i < numberOfThreads; i++ ) {
            futures.add(executorService.submit(new TileWorker(
                pendingTiles,
                resultingImage.getXSize(),
                resultingImage.getYSize(),
                rendererConfiguration,
                sceneSnapshot)));
        }
        return futures;
    }

    private void joinRenderedTiles(
        RGBImage resultingImage,
        ConcurrentLinkedQueue<Tile> pendingTiles,
        List<Future<List<TileRenderResult>>> futures)
        throws InterruptedException, ExecutionException
    {
        for ( Future<List<TileRenderResult>> future : futures ) {
            copyTiles(resultingImage, future.get());
        }
        if ( !pendingTiles.isEmpty() ) {
            throw new IllegalStateException("Parallel raytracing finished with pending tiles");
        }
    }

    private void copyTiles(RGBImage resultingImage, List<TileRenderResult> tileResults)
    {
        RGBPixel pixel = new RGBPixel();

        for ( TileRenderResult tileResult : tileResults ) {
            Tile tile = tileResult.tile;
            RGBImage tileImage = tileResult.image;
            int x1 = tile.getX0() + tile.getDx();
            int y1 = tile.getY0() + tile.getDy();

            for ( int y = tile.getY0(); y < y1; y++ ) {
                for ( int x = tile.getX0(); x < x1; x++ ) {
                    tileImage.getPixelRgb(x, y, pixel);
                    resultingImage.putPixelRgb(x, y, pixel);
                }
            }
        }
    }

    private static final class TileWorker
        implements Callable<List<TileRenderResult>>
    {
        private final ConcurrentLinkedQueue<Tile> pendingTiles;
        private final int imageWidth;
        private final int imageHeight;
        private final RendererConfiguration rendererConfiguration;
        private final SimpleSceneSnapshot sceneSnapshot;

        private TileWorker(
            ConcurrentLinkedQueue<Tile> pendingTiles,
            int imageWidth,
            int imageHeight,
            RendererConfiguration rendererConfiguration,
            SimpleSceneSnapshot sceneSnapshot)
        {
            this.pendingTiles = pendingTiles;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
            this.rendererConfiguration = rendererConfiguration;
            this.sceneSnapshot = sceneSnapshot;
        }

        @Override
        public List<TileRenderResult> call()
        {
            ArrayList<TileRenderResult> renderedTiles =
                new ArrayList<TileRenderResult>();
            Tile tile;

            while ( (tile = pendingTiles.poll()) != null ) {
                RGBImage tileImage = new RGBImage();
                if ( !tileImage.initNoFill(imageWidth, imageHeight) ) {
                    throw new IllegalStateException("Error creating tile image");
                }
                Raytracer raytracer = new Raytracer();
                raytracer.execute(
                    tileImage,
                    rendererConfiguration,
                    sceneSnapshot,
                    null,
                    null,
                    tile.getX0(),
                    tile.getY0(),
                    tile.getX0() + tile.getDx(),
                    tile.getY0() + tile.getDy());
                renderedTiles.add(new TileRenderResult(tile, tileImage));
            }

            return renderedTiles;
        }
    }

    private static final class TileRenderResult {
        private final Tile tile;
        private final RGBImage image;

        private TileRenderResult(Tile tile, RGBImage image)
        {
            this.tile = tile;
            this.image = image;
        }
    }
}
