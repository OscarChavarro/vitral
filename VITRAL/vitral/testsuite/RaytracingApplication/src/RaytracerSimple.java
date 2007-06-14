           /***************************************************
            *   An instructional Ray-Tracing Renderer written
            *   for MIT 6.837  Fall '98 by Leonard McMillan.
            *   Modified by Tomas Lozano-Perez for Fall '01
            *   Modified by Oscar Chavarro for Spring '04 
            *   FUSM 05061.
            *   Modified by Oscar Chavarro for PUJ Vitral 
            *   VSDK '05, '06
            ****************************************************/

//===========================================================================

// Paquetes de java utilizados para el manejo de archivos
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

// VSDK classes
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.common.ProgressMonitorConsole;
import vsdk.toolkit.render.Raytracer;
import vsdk.toolkit.io.image.ImagePersistence;

public class RaytracerSimple {
//- Atributos ---------------------------------------------------------------

    // Modelo de la aplicacion
    private Universe la_escena;
    private RGBImage la_imagen_resultado;
    private Raytracer visualizationEngine;

//- Metodos -----------------------------------------------------------------

    public RaytracerSimple()
    {
        // Inicializacion del modelo (independiente de swing)
        la_escena = new Universe();
    }

    private void
    pintar_offline(String nombre_de_archivo)
    {
        //- 1. Leer la escena ---------------------------------------------
        System.out.println("Loading scene from " + nombre_de_archivo + ": ");
        InputStream is = null;
        try {
            is = new FileInputStream(new File(nombre_de_archivo));
            la_escena.leerArchivoDeEscena(is);
            is.close();
          }
          catch (IOException e) {
            System.err.println("Error leyendo " + nombre_de_archivo);
            System.exit(-1);
        }
        System.out.println("Scene loaded OK!");

        //- 2. Crear una imagen -------------------------------------------
        la_imagen_resultado = new RGBImage();
        if ( !la_imagen_resultado.init(
                  la_escena.viewportXSize, la_escena.viewportYSize) ) {
            System.err.println("Error creando la imagen!!");
            System.exit(1);
        }

        //- 3. Calcular la imagen / procesar la escena --------------------
        ProgressMonitorConsole reporter = new ProgressMonitorConsole();

        visualizationEngine = new Raytracer();
        la_escena.camara.updateViewportResize(la_imagen_resultado.getXSize(), 
                                              la_imagen_resultado.getYSize());
        visualizationEngine.execute(la_imagen_resultado,
                                la_escena.arr_cosas,
                                la_escena.arr_luces, 
                                la_escena.fondo,
                                la_escena.camara, reporter);

        //- 4. Exportar la imagen a un archivo ----------------------------
        File fd = new File("./output.ppm");

        System.out.print("Exporting result image to file: ");
        if ( !ImagePersistence.exportPPM(fd, la_imagen_resultado) )
        {
            System.err.println("Error grabando la imagen!!");
            System.exit(1);
        }
        System.out.println(" OK!");

        System.out.println("An image has been created in the file output.ppm");

        //- 5. Destruir las estructuras de datos --------------------------
        // 5.1. Destruir la imagen
        la_imagen_resultado.dispose();
        la_imagen_resultado = null;

        // 5.2. Destruir la escena
        visualizationEngine = null;
        la_escena.dispose();
        la_escena = null;

        // 5.3. Sugerir al recolector de basura de Java que libere la memoria
        System.gc();
    }

    public static void
    main(String args[])
    {
        RaytracerSimple instancia = new RaytracerSimple();

        if ( args.length < 1 ) {
            instancia.pintar_offline("./etc/object.ray");
          }
          else {
            instancia.pintar_offline(args[0]);
        }
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
