// Java classes
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

// Vitral classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.processing.StopWatch;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.RaytraceStatistics;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.CameraSnapshot;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.environment.scene.SimpleSceneSnapshot;
import vsdk.toolkit.gui.ProgressMonitorConsole;
import vsdk.toolkit.render.SimpleRaytracer;
import vsdk.toolkit.io.geometry.ReaderMitScene;

public class RaytracerSimple {
    private static final String SCENE_SAMPLES_PATH = "../../../etc/geometry/mitscenes/";
    private static final int ELAPSED_TIME_DECIMALS = 3;
    private static final int EXIT_CODE_READ_ERROR = -1;
    private static final int EXIT_CODE_IMAGE_ERROR = 1;
    private static final int EXIT_CODE_ARGUMENT_ERROR = 2;

    private final SimpleScene scene;

    public RaytracerSimple()
    {
        scene = new SimpleScene();
    }

    private void optimizeRendererConfigurationForScene(RendererConfiguration rendererConfiguration)
    {
        boolean hasTextures = false;
        boolean hasNormalMaps = false;
        ArrayList<SimpleBody> bodies = scene.getSimpleBodies();

        for ( SimpleBody body : bodies ) {
            if ( body.getTexture() != null ) {
                hasTextures = true;
            }
            if ( body.getNormalMap() != null ) {
                hasNormalMaps = true;
            }
            if ( hasTextures && hasNormalMaps ) {
                break;
            }
        }

        rendererConfiguration.setTexture(hasTextures);
        rendererConfiguration.setBumpMap(hasNormalMaps);
    }

    private void
    offlineExecution(
        String fileName,
        boolean save,
        String outputFileName,
        boolean parallel)
    {
        RGBImage resultingImage;

        //- 1. Import the scene from scene description file to RAM -----
        System.out.println("Loading scene from " + fileName + ": ");
        try (InputStream is = new FileInputStream(fileName)) {
            ReaderMitScene readerMitScene = new ReaderMitScene();
            readerMitScene.importEnvironment(is, scene);
          }
          catch ( Exception e ) {
            System.err.println("Error reading " + fileName);
            System.err.println("There are scene samples on " + SCENE_SAMPLES_PATH);
            System.exit(EXIT_CODE_READ_ERROR);
        }
        System.out.println("Scene loaded OK!");

        //- 2. Create an empty image --------------------------------------
        resultingImage = new RGBImage();
        Camera activeCamera = scene.getActiveCamera();
        if ( !resultingImage.initNoFill(
            (int)activeCamera.getViewportXSize(),
            (int)activeCamera.getViewportYSize()) ) {
            System.err.println("Error creating image!");
            System.exit(EXIT_CODE_IMAGE_ERROR);
        }

        //- 3. Process the image from the scene data structure -----------
        ProgressMonitorConsole reporter = new ProgressMonitorConsole();
        RendererConfiguration rendererConfiguration = new RendererConfiguration();
        optimizeRendererConfigurationForScene(rendererConfiguration);

        SimpleRaytracer visualizationEngine = new SimpleRaytracer();
        CameraSnapshot cameraSnapshot = activeCamera.exportToCameraSnapshot(
            resultingImage.getXSize(), resultingImage.getYSize());
        SimpleSceneSnapshot sceneSnapshot =
            scene.exportToSimpleSceneSnapshot(
                cameraSnapshot,
                scene.getActiveBackground());

        StopWatch clock = new StopWatch();
        RaytracerExecutor raytracerExecutor =
            parallel ? new RaytracerParallelExecutor() : new RaytracerSerialExecutor();

        clock.start();
        raytracerExecutor.run(
            visualizationEngine,
            resultingImage,
            rendererConfiguration,
            sceneSnapshot,
            reporter);
        clock.stop();

        System.out.println(
            "Image generated in " +
            VSDK.formatDouble(clock.getElapsedRealTime(), ELAPSED_TIME_DECIMALS) +
            " seconds.");
        RaytraceStatistics.printSummary();

        //- 4. Export resulting image to an image file --------------------
        if ( save ) {
            ImageExporter imageExporter = new ImageExporter();
            if ( !imageExporter.export(outputFileName, resultingImage) ) {
                System.err.println("Error saving output image!");
                System.exit(EXIT_CODE_IMAGE_ERROR);
            }
        }
    }

    public static void
    main(String[] args)
    {
        RaytracerSimple instance = new RaytracerSimple();
        CommandOptionsProcessor options = CommandOptionsProcessor.process(args);
        if ( options.shouldShowHelp() ) {
            CommandOptionsProcessor.printUsage();
            if ( args.length > 0 ) {
                System.exit(EXIT_CODE_ARGUMENT_ERROR);
            }
        }
        instance.offlineExecution(
            options.getSceneFile(),
            options.shouldSave(),
            options.getOutputFile(),
            options.shouldUseParallelExecutor());
    }
}
