package render;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import model.ShadersModel;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.CameraSnapshot;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.LightType;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.MicroFacetedMaterial;
import vsdk.toolkit.environment.SimpleBackground;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleSceneSnapshot;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.IndexedColorImageUncompressed;
import vsdk.toolkit.media.NormalMap;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.SimpleRaytracer;
import vsdk.toolkit.render.Tile;
import vsdk.toolkit.render.TileGenerationStrategy;
import vsdk.toolkit.render.TileGenerator;

public class SoftwareRaycaster
{
    private static final Vector3D DEFAULT_BUMP_SCALE = new Vector3D(1.0, 1.0, 1.0);

    private final int numberOfThreads;
    private final NormalMap bumpNormalMap;

    public SoftwareRaycaster()
    {
        numberOfThreads = Math.max(1, Runtime.getRuntime().availableProcessors());
        bumpNormalMap = loadBumpNormalMap();
    }

    public void invalidateSnapshot()
    {
        // No-op by design: the snapshot is rebuilt on every software render
        // to keep camera/light/object transforms fully synchronized with the
        // interactive OpenGL path.
    }

    public void render(
        ShadersModel model,
        Camera activeCamera,
        Matrix4x4 modelRotation)
    {
        RGBImageUncompressed outputImage = model.getSoftwareFrameImage();
        if ( outputImage == null ) {
            return;
        }

        SimpleSceneSnapshot snapshot = buildSceneSnapshot(
            model,
            activeCamera,
            modelRotation,
            outputImage);
        TileGenerator tileGenerator = new TileGenerator(
            TileGenerationStrategy.LINEAR,
            outputImage,
            outputImage.getXSize(),
            outputImage.getYSize(),
            numberOfThreads);
        ConcurrentLinkedQueue<Tile> pendingTiles =
            new ConcurrentLinkedQueue<Tile>(tileGenerator.getTiles());
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        try {
            List<Future<Void>> futures = new ArrayList<Future<Void>>(numberOfThreads);
            for ( int i = 0; i < numberOfThreads; i++ ) {
                futures.add(executorService.submit(new TileWorker(
                    pendingTiles,
                    outputImage,
                    model.getQuality(),
                    snapshot)));
            }
            for ( Future<Void> future : futures ) {
                future.get();
            }
        }
        catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Software raytracing was interrupted", e);
        }
        catch ( ExecutionException e ) {
            throw new IllegalStateException("Software raytracing failed", e);
        }
        finally {
            executorService.shutdownNow();
        }
    }

    private SimpleSceneSnapshot buildSceneSnapshot(
        ShadersModel model,
        Camera activeCamera,
        Matrix4x4 modelRotation,
        RGBImageUncompressed outputImage)
    {
        int viewportWidth = outputImage.getXSize();
        int viewportHeight = outputImage.getYSize();
        CameraSnapshot cameraSnapshot = activeCamera.exportToCameraSnapshot(
            viewportWidth,
            viewportHeight);

        SimpleBody sphereBody = new SimpleBody();
        sphereBody.setGeometry(new Sphere(model.getSphere().getRadius()));
        Material activeMaterial = model.getActiveMaterialForCurrentShading();
        if ( activeMaterial instanceof MicroFacetedMaterial microFacetedMaterial ) {
            sphereBody.setMaterial(new MicroFacetedMaterial(microFacetedMaterial));
        }
        else {
            sphereBody.setMaterial(new Material(activeMaterial));
        }
        sphereBody.setTexture(model.getTextureMap());
        sphereBody.setNormalMap(bumpNormalMap);
        sphereBody.setRotation(modelRotation);

        ArrayList<SimpleBody> bodies = new ArrayList<SimpleBody>(1);
        bodies.add(sphereBody);

        ArrayList<Light> lights = new ArrayList<Light>(2);
        Light ambientLight = new Light(
            LightType.AMBIENT,
            new Vector3D(0, 0, 0),
            new ColorRgb(1, 1, 1));
        ambientLight.setId(0);
        lights.add(ambientLight);
        Light pointLight = new Light(
            model.getLight().getLightType(),
            model.getLight().getPosition(),
            model.getLight().getSpecular());
        pointLight.setId(1);
        lights.add(pointLight);

        SimpleBackground background = new SimpleBackground();
        background.setColor(0, 0, 0);

        return new SimpleSceneSnapshot(
            bodies,
            lights,
            background,
            cameraSnapshot);
    }

    private static NormalMap loadBumpNormalMap()
    {
        try {
            IndexedColorImageUncompressed bumpMap = ImagePersistence.importIndexedColor(
                new File("../../../etc/bumpmaps/earth.bw"));
            NormalMap normalMap = new NormalMap();
            normalMap.importBumpMap(bumpMap, DEFAULT_BUMP_SCALE);
            return normalMap;
        }
        catch ( Exception e ) {
            throw new IllegalStateException("Failed loading software bump map", e);
        }
    }

    private record TileWorker(
        ConcurrentLinkedQueue<Tile> pendingTiles,
        RGBImageUncompressed resultingImage,
        RendererConfiguration rendererConfiguration,
        SimpleSceneSnapshot sceneSnapshot)
        implements Callable<Void>
    {
        @Override
        public Void call()
        {
            Tile tile;
            SimpleRaytracer raytracer = new SimpleRaytracer();
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
