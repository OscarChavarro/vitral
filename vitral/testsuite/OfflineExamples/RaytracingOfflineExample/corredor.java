/**
Programa de prueba que ilustra como ejecutar comandos del sistema operacional
desde JAVA, recuperando su salida estandar.

NOTA: Por alguna razon parece que comandos que redirigan las salidas a
      archivos no lo hacen (no se crean los archivos). Para hacerlos
      funcionar deben ejecutarse sus redirecciones desde scripts, y
      ejecutarse los scripts.

Oscar Chavarro, Fundacion Universidad San Martin, septiembre 27 de 2003.
Este codigo se rige bajo la licencia GNU/GPL (http://www.gnu.org)
*/

import java.io.*;

public class corredor {

    public Process lanzarProcesoDelSistemaOperacional(String comando) 
    {
        Process proceso;
        Runtime entornoDeEjecucion = Runtime.getRuntime();
        try {
            proceso = entornoDeEjecucion.exec(comando);
        }
        catch(Exception error){
            System.out.println(error);
        proceso = null;
        }
        return proceso;
    }

    public void imprimirSalidaEstandar(Process proceso) 
    {
        int i, n = 0;

        InputStream salida = proceso.getInputStream();
        byte arr[] = new byte[512];

    try {
            do {
                n = salida.read(arr);
                for ( i = 0; i < arr.length && i < n; i++ ) {
                    System.out.print((char)arr[i]);
                }
        } while ( n > 0 );
    }
    catch(IOException error) {
        System.out.println("ERROR intentando leer salida standard del comando!");
            System.out.println(error);
    }

    }

    public corredor() 
    {
        System.out.println("Programa de prueba para la ejecucion de comandos del sistema operacional.");
    }

    public static void main(String parametros[]) 
    {
        corredor a = new corredor();
    String comando = "ls -al /";

        Process proceso = a.lanzarProcesoDelSistemaOperacional(comando);

        System.out.println("- Contenido de la salida estandard del comandito ----------------------");
        a.imprimirSalidaEstandar(proceso);
        System.out.println("-----------------------------------------------------------------------");
    }
};
