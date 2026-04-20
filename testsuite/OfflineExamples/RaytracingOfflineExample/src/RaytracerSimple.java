/**
An instructional Ray-Tracing Renderer written for MIT 6.837  Fall '98 by Leonard McMillan.
Modified by Tomas Lozano-Perez for Fall '01
Modified by Oscar Chavarro for Spring '04
Modified by Oscar Chavarro for PUJ Vitral '05, '06, '10
*/

// Java classes
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

// Vitral classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.processing.StopWatch;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.RaytraceProfiling;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.gui.ProgressMonitorConsole;
import vsdk.toolkit.render.Raytracer;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.io.geometry.ReaderMitScene;

public class RaytracerSimple {
    private static final String NO_SAVE_FLAG = "nosave";
    private static final String DEFAULT_SCENE_FILE =
        "../../../etc/geometry/mitscenes/balls.ray";
    private static final String SCENE_SAMPLES_PATH =
        "../../../etc/geometry/mitscenes/";
    private static final String OUTPUT_FILE_NAME = "./output.ppm";
    private static final int ELAPSED_TIME_DECIMALS = 3;
    private static final int EXIT_CODE_READ_ERROR = -1;
    private static final int EXIT_CODE_IMAGE_ERROR = 1;

    private final ReaderMitScene readerMitScene;
    private final SimpleScene scene;

    public RaytracerSimple()
    {
        readerMitScene = new ReaderMitScene();
        scene = new SimpleScene();
    }

    private void optimizeRendererConfigurationForScene(
        RendererConfiguration rendererConfiguration)
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

        if ( !hasTextures ) {
            rendererConfiguration.setTexture(false);
        }
        if ( !hasNormalMaps ) {
            rendererConfiguration.setBumpMap(false);
        }
    }

    private void
    offlineExecution(String fileName, boolean save)
    {
        Raytracer visualizationEngine;
        RGBImage resultingImage;
        //- 1. Import the scene from scene description file to RAM -----
        System.out.println("Loading scene from " + fileName + ": ");
        try (InputStream is = new FileInputStream(fileName)) {
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
        if ( !resultingImage.initNoFill(
                  readerMitScene.viewportXSize, readerMitScene.viewportYSize) ) {
            System.err.println("Error creating image!");
            System.exit(EXIT_CODE_IMAGE_ERROR);
        }

        //- 3. Process the image from the scene data structure -----------
        ProgressMonitorConsole reporter = new ProgressMonitorConsole();
        RendererConfiguration rendererConfiguration = new RendererConfiguration();
        optimizeRendererConfigurationForScene(rendererConfiguration);

        visualizationEngine = new Raytracer();
        Camera activeCamera = scene.getActiveCamera();
        activeCamera.updateViewportResize(resultingImage.getXSize(), resultingImage.getYSize());

        StopWatch clock = new StopWatch();

        clock.start();
        visualizationEngine.execute(resultingImage, rendererConfiguration,
                                scene.getSimpleBodies(),
                                scene.getLights(),
                                scene.getActiveBackground(),
                                activeCamera, reporter, null);
        clock.stop();

        System.out.println(
            "Image generated in " +
            VSDK.formatDouble(clock.getElapsedRealTime(), ELAPSED_TIME_DECIMALS) +
            " seconds.");
        RaytraceProfiling.printSummary();

        //- 4. Export resulting image to an image file --------------------
        if ( save ) {
            File fd = new File(OUTPUT_FILE_NAME);

            System.out.print("Exporting result image to file \"" + OUTPUT_FILE_NAME + "\": ");
            if ( !ImagePersistence.exportPPM(fd, resultingImage) )
            {
                System.err.println("Error saving output image!");
                System.exit(EXIT_CODE_IMAGE_ERROR);
            }
            System.out.println(" OK!");
        }
    }

    public static void
    main(String[] args)
    {
        RaytracerSimple instance = new RaytracerSimple();
        boolean save = true;
        String sceneFile = DEFAULT_SCENE_FILE;

        for (String arg : args) {
            if (NO_SAVE_FLAG.equals(arg)) {
                save = false;
            }
            else {
                sceneFile = arg;
            }
        }

        instance.offlineExecution(sceneFile, save);
    }
}
