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
import java.util.Locale;

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
import vsdk.toolkit.render.Raytracer;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.io.geometry.ReaderMitScene;

public class RaytracerSimple {
    private static final String DEFAULT_SCENE_FILE =
        "../../../etc/geometry/mitscenes/balls.ray";
    private static final String SCENE_SAMPLES_PATH =
        "../../../etc/geometry/mitscenes/";
    private static final String DEFAULT_OUTPUT_FILE_NAME = "./output.ppm";
    private static final int ELAPSED_TIME_DECIMALS = 3;
    private static final int EXIT_CODE_READ_ERROR = -1;
    private static final int EXIT_CODE_IMAGE_ERROR = 1;
    private static final int EXIT_CODE_ARGUMENT_ERROR = 2;

    private final SimpleScene scene;

    public RaytracerSimple()
    {
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
    offlineExecution(String fileName, boolean save, String outputFileName)
    {
        Raytracer visualizationEngine;
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

        visualizationEngine = new Raytracer();
        CameraSnapshot cameraSnapshot = activeCamera.exportToCameraSnapshot(
            resultingImage.getXSize(), resultingImage.getYSize());
        SimpleSceneSnapshot sceneSnapshot =
            scene.exportToSimpleSceneSnapshot(
                cameraSnapshot,
                scene.getActiveBackground());

        StopWatch clock = new StopWatch();

        clock.start();
        visualizationEngine.execute(resultingImage, rendererConfiguration,
                                sceneSnapshot, reporter, null);
        clock.stop();

        System.out.println(
            "Image generated in " +
            VSDK.formatDouble(clock.getElapsedRealTime(), ELAPSED_TIME_DECIMALS) +
            " seconds.");
        RaytraceStatistics.printSummary();

        //- 4. Export resulting image to an image file --------------------
        if ( save ) {
            File fd = new File(outputFileName);

            System.out.print("Exporting result image to file \"" + outputFileName + "\": ");
            if ( !exportImage(fd, resultingImage) ) {
                System.err.println("Error saving output image!");
                System.exit(EXIT_CODE_IMAGE_ERROR);
            }
            System.out.println(" OK!");
        }
    }

    private static boolean exportImage(File outputFile, RGBImage image)
    {
        String lowerName = outputFile.getName().toLowerCase(Locale.ROOT);
        if ( lowerName.endsWith(".png") ) {
            ImagePersistence.exportPNG(outputFile, image);
            return true;
        }
        if ( lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ) {
            return ImagePersistence.exportJPG(outputFile, image);
        }
        return ImagePersistence.exportPPM(outputFile, image);
    }

    private static void printUsage()
    {
        System.out.println("Usage: RaytracerSimple [options] [scene_file]");
        System.out.println("Options:");
        System.out.println("  --scene, -s <file>     MIT scene file (.ray)");
        System.out.println("  --output, -o <file>    Output image file (.ppm/.png/.jpg)");
        System.out.println("  --nosave, -n           Render only, no image file");
        System.out.println("  --help, -h             Show this help");
        System.out.println();
        System.out.println("Legacy compatibility:");
        System.out.println("  - `nosave` (without dashes) is still accepted.");
    }

    private static final class CliOptions {
        String sceneFile = DEFAULT_SCENE_FILE;
        String outputFile = DEFAULT_OUTPUT_FILE_NAME;
        boolean save = true;
        boolean showHelp = false;
    }

    private static CliOptions parseArguments(String[] args)
    {
        CliOptions options = new CliOptions();
        int positionalCount = 0;

        for ( int i = 0; i < args.length; i++ ) {
            String arg = args[i];
            if ( "nosave".equals(arg) || "--nosave".equals(arg) || "-n".equals(arg) ) {
                options.save = false;
                continue;
            }
            if ( "--help".equals(arg) || "-h".equals(arg) ) {
                options.showHelp = true;
                continue;
            }
            if ( "--scene".equals(arg) || "-s".equals(arg) ) {
                if ( i + 1 >= args.length ) {
                    System.err.println("Missing value for " + arg);
                    options.showHelp = true;
                    return options;
                }
                options.sceneFile = args[++i];
                continue;
            }
            if ( "--output".equals(arg) || "-o".equals(arg) ) {
                if ( i + 1 >= args.length ) {
                    System.err.println("Missing value for " + arg);
                    options.showHelp = true;
                    return options;
                }
                options.outputFile = args[++i];
                continue;
            }
            if ( arg.startsWith("-") ) {
                System.err.println("Unknown option: " + arg);
                options.showHelp = true;
                return options;
            }

            if ( positionalCount == 0 ) {
                options.sceneFile = arg;
            }
            else if ( positionalCount == 1 ) {
                options.outputFile = arg;
            }
            else {
                System.err.println("Unexpected argument: " + arg);
                options.showHelp = true;
                return options;
            }
            positionalCount++;
        }

        return options;
    }

    public static void
    main(String[] args)
    {
        RaytracerSimple instance = new RaytracerSimple();
        CliOptions options = parseArguments(args);
        if ( options.showHelp ) {
            printUsage();
            if ( args.length > 0 ) {
                System.exit(EXIT_CODE_ARGUMENT_ERROR);
            }
        }
        instance.offlineExecution(options.sceneFile, options.save, options.outputFile);
    }
}
