           /***************************************************
            *   An instructional Ray-Tracing Renderer written
            *   for MIT 6.837  Fall '98 by Leonard McMillan.
            *   Modified by Tomas Lozano-Perez for Fall '01
            *   Modified by Oscar Chavarro for Spring '04 
            *   FUSM 05061.
            *   Modified by Oscar Chavarro for PUJ Vitral 
            *   VSDK '05, '06, '10
            ****************************************************/

//===========================================================================

// Paquetes de java utilizados para el manejo de archivos
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

// VSDK classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.gui.ProgressMonitorConsole;
import vsdk.toolkit.render.Raytracer;
import vsdk.toolkit.io.image.ImagePersistence;

public class RaytracerSimple {
    // Application model
    private MitSceneReader theSceneReader;
    private SimpleScene theScene;
    private RGBImage theResultingImage;
    private Raytracer visualizationEngine;

    public RaytracerSimple()
    {
        theSceneReader = new MitSceneReader();
        theScene = new SimpleScene();
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
            System.exit(-1);
        }
        System.out.println("Scene loaded OK!");

        //- 2. Create an empty image --------------------------------------
        theResultingImage = new RGBImage();
        if ( !theResultingImage.init(
                  theSceneReader.viewportXSize, theSceneReader.viewportYSize) ) {
            System.err.println("Error creando la imagen!!");
            System.exit(1);
        }

        //- 3. Process the image from the escene data structure -----------
        ProgressMonitorConsole reporter = new ProgressMonitorConsole();
        RendererConfiguration rendererConfiguration = new RendererConfiguration();

        visualizationEngine = new Raytracer();
        theScene.getActiveCamera().updateViewportResize(
            theResultingImage.getXSize(), theResultingImage.getYSize());

        long initialTime = System.currentTimeMillis();
        visualizationEngine.execute(theResultingImage, rendererConfiguration,
                                theScene.getSimpleBodies(),
                                theScene.getLights(),
                                theScene.getActiveBackground(),
                                theScene.getActiveCamera(), reporter, null);
        long finalTime = System.currentTimeMillis();
        System.out.println("Image generated in " + ((double)(finalTime-initialTime))/1000.0 + " seconds.");

        //- 4. Export resulting image to an image file --------------------
        if ( save == true ) {
            File fd = new File("./output.jpg");

            System.out.print("Exporting result image to file: ");
            if ( !ImagePersistence.exportJPG(fd, theResultingImage) )
            {
                System.err.println("Error grabando la imagen!!");
                System.exit(1);
            }
            System.out.println(" OK!");

            System.out.println("An image has been created in the file output.jpg");
        }
        //- 5. Destruir las estructuras de datos --------------------------
        // 5.1. Free image reference
        theResultingImage.finalize();
        theResultingImage = null;

        // 5.2. Free scene references
        visualizationEngine = null;
        theSceneReader = null;
        theScene = null;

        // 5.3. Suggest the garbage collector to free unused memory
        System.gc();
    }

    public static void
    main(String args[])
    {
        RaytracerSimple instance = new RaytracerSimple();
        boolean save = true;

        for ( int i = 0; i < args.length; i++ ) {
            if ( args[i].equals("nosave") ) {
                save = false;
            }
        }

        if ( args.length < 1 ) {
            instance.offlineExecution("./etc/object.ray", save);
          }
          else {
            instance.offlineExecution(args[0], save);
        }
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
