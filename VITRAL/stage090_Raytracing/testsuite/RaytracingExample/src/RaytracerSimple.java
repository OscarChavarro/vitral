           /***************************************************
            *   An instructional Ray-Tracing Renderer written
            *   for MIT 6.837  Fall '98 by Leonard McMillan.
            *   Modified by Tomas Lozano-Perez for Fall '01
            *   Modified by Oscar Chavarro for Spring '04 
            *   FUSM 05061.
            *   Modified by Oscar Chavarro for PUJ Vitral
            ****************************************************/

//===========================================================================

// Paquetes de java utilizados para el manejo de archivos
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

// Paquetes de java utilizados para la agregacion multiple
import java.util.Vector;

// Paquetes internos al sistema de raytracing / modelamiento
import vitral_transition.toolkits.media.RGBImage;
import vitral_transition.framework.visual.RaytracerMIT;
import vitral_transition.framework.Universe;
import vitral.toolkits.environment.Camera;

public class RaytracerSimple {
//- Atributos ---------------------------------------------------------------

    // Modelo de la aplicacion
    private Universe la_escena;
    private RGBImage la_imagen_resultado;
    private RaytracerMIT el_visualizador;

//- Metodos -----------------------------------------------------------------

    public RaytracerSimple()
    {
        // Inicializacion del modelo (independiente de swing)
        la_escena = new Universe();
    }

    private void
    pintar_offline(String nombre_de_archivo)
    {
        //- 1. Crear una imagen -------------------------------------------
        File fd = new File("./salida.ppm");

        la_imagen_resultado = new RGBImage();
        if ( !la_imagen_resultado.init(320, 240) ) {
            System.err.println("Error creando la imagen!!");
            System.exit(1);
        }

        //- 2. Calcular la imagen -----------------------------------------
        // 2.1. Leer la escena
        System.out.println("Leyendo " + nombre_de_archivo);
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

        // 2.2. Procesar la escena
        el_visualizador = new RaytracerMIT();
        la_escena.camara.updateViewportResize(la_imagen_resultado.xtam(), 
                                         la_imagen_resultado.ytam());
        el_visualizador.ejecutar(la_imagen_resultado,
                                 la_escena.arr_cosas,
                                 la_escena.arr_luces, 
                                 la_escena.fondo,
                                 la_escena.camara);

        //- 3. Exportar la imagen a un archivo ----------------------------
        if ( !la_imagen_resultado.exportar_ppm(fd) )
        {
            System.err.println("Error grabando la imagen!!");
            System.exit(1);
        }
        System.out.println("Se ha generado una imagen llamada salida.ppm");
        System.out.println("En Unix / Linux puede verla con el comando");
        System.out.println("  display salida.ppm");

        //- 4. Destruir las estructuras de datos --------------------------
        // 4.1. Destruir la imagen
        la_imagen_resultado.dispose();
        la_imagen_resultado = null;

        // 4.2. Destruir la escena
        el_visualizador = null;
        la_escena.dispose();
        la_escena = null;

        // 4.3. Sugerir al recolector de basura de Java que libere la memoria
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
