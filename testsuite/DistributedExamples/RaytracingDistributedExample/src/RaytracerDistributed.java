//===========================================================================

// Java classes
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

// VSDK classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.StopWatch;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.gui.ProgressMonitorConsole;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.io.geometry.ReaderMitScene;

/**
This is a parallel / distributed application. Do not confuse this with
an stochastic raytracer (it is not).
*/
public class RaytracerDistributed {
    // Application model
    private ReaderMitScene theSceneReader;
    private SimpleScene theScene;
    private RGBImage theResultingImage;
    private DistributerByArea visualizationClient;

    public RaytracerDistributed()
    {
        theSceneReader = new ReaderMitScene();
        theScene = new SimpleScene();
        visualizationClient = new DistributerByArea();
    }

    private void
    offlineExecution(String nombre_de_archivo, boolean save)
    {
        //- 1. Import the scene from an scene description file to RAM -----
        System.out.println("Loading scene from " + nombre_de_archivo + ": ");
        InputStream is = null;
        try {
            is = new FileInputStream(new File(nombre_de_archivo));
            theSceneReader.importEnvironment(is, theScene);
            is.close();
          }
          catch ( Exception e ) {
            System.err.println("Error reading " + nombre_de_archivo);
            System.err.println("There are scene samples on ../../../etc/geometry/mitscenes/");
            System.exit(-1);
        }
        System.out.println("Scene loaded OK!");

        //- 2. Create an empty image --------------------------------------
        theResultingImage = new RGBImage();
        if ( !theResultingImage.initNoFill(
                  theSceneReader.viewportXSize, theSceneReader.viewportYSize) ) {
            System.err.println("Error creando la imagen!!");
            System.exit(1);
        }

        //- 3. Process the image from the escene data structure -----------
        ProgressMonitorConsole reporter = new ProgressMonitorConsole();
        RendererConfiguration rendererConfiguration = new RendererConfiguration();

        theScene.getActiveCamera().updateViewportResize(
            theResultingImage.getXSize(), theResultingImage.getYSize());

        StopWatch clock = new StopWatch();

        clock.start();
        visualizationClient.distributedControl(
            theResultingImage, rendererConfiguration,
            theScene, reporter);
        clock.stop();

        System.out.println("Image generated in " + VSDK.formatDouble(clock.getElapsedRealTime(), 3) + " seconds.");

        //- 4. Export resulting image to an image file --------------------
        if ( save == true ) {
            File fd = new File("./output.bmp");

            System.out.print("Exporting result image to file \"output.bmp\": ");
            if ( !ImagePersistence.exportBMP(fd, theResultingImage) )
            {
                System.err.println("Error grabando la imagen!!");
                System.exit(1);
            }
            System.out.println(" OK!");
        }
        //- 5. Destruir las estructuras de datos --------------------------
        // 5.1. Free image reference
        theResultingImage.finalize();
        theResultingImage = null;

        // 5.2. Free scene references
        theSceneReader = null;
        theScene = null;

        // 5.3. Suggest the garbage collector to free unused memory
        System.gc();
    }

    public static void
    main(String args[])
    {
        RaytracerDistributed instance = new RaytracerDistributed();
        boolean save = true;

        for ( int i = 0; i < args.length; i++ ) {
            if ( args[i].equals("nosave") ) {
                save = false;
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

//===========================================================================
//= EOF                                                                     =
//===========================================================================
