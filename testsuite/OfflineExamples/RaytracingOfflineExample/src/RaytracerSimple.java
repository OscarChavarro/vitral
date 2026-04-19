/**
An instructional Ray-Tracing Renderer written
for MIT 6.837  Fall '98 by Leonard McMillan.
Modified by Tomas Lozano-Perez for Fall '01
Modified by Oscar Chavarro for Spring '04
FUSM 05061.
Modified by Oscar Chavarro for PUJ Vitral
VSDK '05, '06, '10
*/

// Java classes
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

// Vitral classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.processing.StopWatch;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.gui.ProgressMonitorConsole;
import vsdk.toolkit.render.Raytracer;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.io.geometry.ReaderMitScene;

public class RaytracerSimple {
    private final ReaderMitScene readerMitScene;
    private final SimpleScene scene;

    public RaytracerSimple()
    {
        readerMitScene = new ReaderMitScene();
        scene = new SimpleScene();
    }

    private void
    offlineExecution(String fileName, boolean save)
    {
        Raytracer visualizationEngine;
        RGBImage resultingImage;
        //- 1. Import the scene from scene description file to RAM -----
        System.out.println("Loading scene from " + fileName + ": ");
        try {
            InputStream is = new FileInputStream(fileName);
            readerMitScene.importEnvironment(is, scene);
            is.close();
          }
          catch ( Exception e ) {
            System.err.println("Error reading " + fileName);
            System.err.println("There are scene samples on ../../../etc/geometry/mitscenes/");
            System.exit(-1);
        }
        System.out.println("Scene loaded OK!");

        //- 2. Create an empty image --------------------------------------
        resultingImage = new RGBImage();
        if ( !resultingImage.initNoFill(
                  readerMitScene.viewportXSize, readerMitScene.viewportYSize) ) {
            System.err.println("Error creating image!");
            System.exit(1);
        }

        //- 3. Process the image from the scene data structure -----------
        ProgressMonitorConsole reporter = new ProgressMonitorConsole();
        RendererConfiguration rendererConfiguration = new RendererConfiguration();

        visualizationEngine = new Raytracer();
        scene.getActiveCamera().updateViewportResize(
            resultingImage.getXSize(), resultingImage.getYSize());

        StopWatch clock = new StopWatch();

        clock.start();
        visualizationEngine.execute(resultingImage, rendererConfiguration,
                                scene.getSimpleBodies(),
                                scene.getLights(),
                                scene.getActiveBackground(),
                                scene.getActiveCamera(), reporter, null);
        clock.stop();

        System.out.println("Image generated in " + VSDK.formatDouble(clock.getElapsedRealTime(), 3) + " seconds.");

        //- 4. Export resulting image to an image file --------------------
        if ( save ) {
            File fd = new File("./output.ppm");

            System.out.print("Exporting result image to file \"output.ppm\": ");
            if ( !ImagePersistence.exportPPM(fd, resultingImage) )
            {
                System.err.println("Error saving output image!");
                System.exit(1);
            }
            System.out.println(" OK!");
        }
    }

    public static void
    main(String[] args)
    {
        RaytracerSimple instance = new RaytracerSimple();
        boolean save = true;

        for (String arg : args) {
            if (arg.equals("nosave")) {
                save = false;
                break;
            }
        }

        if ( args.length < 1 ) {
            instance.offlineExecution("../../../etc/geometry/mitscenes/object.ray", save);
          }
          else {
            instance.offlineExecution(args[0], save);
        }
    }
}
