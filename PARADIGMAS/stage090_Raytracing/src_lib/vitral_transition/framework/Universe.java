//===========================================================================

package vitral_transition.framework;

// Paquetes de java utilizados para la agregacion multiple
import java.util.Vector;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.StreamTokenizer;
import java.io.IOException;

// Paquetes internos al sistema de raytracing / modelamiento
import vitral.toolkits.common.Vector3D;
import vitral.toolkits.common.ColorRgb;
import vitral.toolkits.environment.Camera;
import vitral_transition.toolkits.environment.Light2;
import vitral.toolkits.environment.Material;
import vitral.toolkits.environment.SimpleBackground;
import vitral.toolkits.geometry.Geometry;
import vitral.toolkits.geometry.Sphere;

public class Universe
{
    // Variables especificas para la depuracion / desarrollo
    private static final boolean depurar = false;

    // El modelo del mundo
    public Vector<Geometry> arr_cosas;
    public Vector<Light2> arr_luces;
    public SimpleBackground fondo;
    public Camera camara;

    public Universe()
    {
        camara = new Camera();
        fondo = new SimpleBackground();
        fondo.set_color(0, 0, 0);

        int CHUNKSIZE = 100; // Incremento de arreglos

        // Arreglo de Geometrys
        arr_cosas = new Vector<Geometry>(CHUNKSIZE, CHUNKSIZE);  
        // Arreglo de LIGHTes
        arr_luces = new Vector<Light2>(CHUNKSIZE, CHUNKSIZE);
    }

    public void dispose()
    {
        // OJO: Falta ver si la destruccion de la escena esta OK...
        camara = null;
        fondo = null;
        arr_cosas = null;
        arr_luces = null;
        System.gc();
    }

    private void
    imprimirMensaje(String m)
    {
        if ( depurar ) {
            System.out.println(m);
        }
    }

    private float
    leerNumero(StreamTokenizer st) throws IOException {
        if (st.nextToken() != StreamTokenizer.TT_NUMBER) {
            System.err.println("ERROR: number expected in line "+st.lineno());
            throw new IOException(st.toString());
        }
        return (float)st.nval;
    }

    public void
    leerArchivoDeEscena(InputStream is) throws IOException {
        Reader parsero = new BufferedReader(new InputStreamReader(is));
        StreamTokenizer st = new StreamTokenizer(parsero);
        st.commentChar('#');
        boolean fin_de_lectura = false;
        Material material_actual;

        // Material por defecto...
        /*
        material_actual = new Material(0.8f, 0.2f, 0.9f, 
                                       0.2f, 0.4f, 0.4f, 
                                       10.0f, 0f, 0f, 1f);
    */
        material_actual = new Material();
        material_actual.setAmbient(new ColorRgb(0.8*0.2, 0.2*0.2, 0.9*0.2));
        material_actual.setDiffuse(new ColorRgb(0.8*0.4, 0.2*0.4, 0.9*0.4));
        material_actual.setSpecular(new ColorRgb(0.8*0.4, 0.2*0.4, 0.9*0.4));
        material_actual.setReflectionCoefficient(0);
        material_actual.setRefractionCoefficient(0);
        material_actual.setPhongExponent(10);

        while ( !fin_de_lectura ) {
          switch ( st.nextToken() ) {
            case StreamTokenizer.TT_WORD:
              if (st.sval.equals("sphere")) {
                  Vector3D c = new Vector3D((float) leerNumero(st), 
                                        (float) leerNumero(st), 
                                        (float) leerNumero(st));
                  float r = (float)leerNumero(st);

                  imprimirMensaje("sphere");
                  arr_cosas.addElement(new Sphere(material_actual, c, r));
                } 
                /*
                else if (st.sval.equals("triangles")) {
                  imprimirMensaje("triangles");
                  arr_cosas.addElement(new MESH(material_actual, st));
                } 
                */
                else if (st.sval.equals("eye")) {
                  imprimirMensaje("eye");
                  camara.setPosition(new Vector3D(leerNumero(st), 
                                                  leerNumero(st), 
                                                  leerNumero(st)));
                }
                else if (st.sval.equals("lookat")) {
                  imprimirMensaje("lookat");
                  camara.setFocusedPositionMaintainingOrthogonality(new Vector3D(leerNumero(st), 
                                                      leerNumero(st), 
                                                      leerNumero(st)));
                }
                else if (st.sval.equals("up")) {
                  imprimirMensaje("up");
                  camara.setUpDirect(new Vector3D(leerNumero(st), 
                                            leerNumero(st), 
                                            leerNumero(st)));
                }
                else if (st.sval.equals("fov")) {
                  imprimirMensaje("fov");
                  camara.setFov(leerNumero(st));
                }
                else if (st.sval.equals("background")) {
                  imprimirMensaje("background");
                  fondo = new SimpleBackground();
                  fondo.set_color((float)leerNumero(st), 
                                  (float)leerNumero(st), 
                                  (float)leerNumero(st));
                }
                else if (st.sval.equals("light")) {
                  imprimirMensaje("light");
                  float r = leerNumero(st);
                  float g = leerNumero(st);
                  float b = leerNumero(st);
                  if ( st.nextToken() != StreamTokenizer.TT_WORD ) {
                      System.err.println("ERROR: in line "+st.lineno() + 
                                         " at "+st.sval);
                      throw new IOException(st.toString());
                  }
                  if ( st.sval.equals("ambient") ) {
                      imprimirMensaje("ambient");
                      arr_luces.addElement(new Light2(Light2.AMBIENTE, null, r,g,b));
                    }
                    else if ( st.sval.equals("directional") ) {
                      imprimirMensaje("directional");
                      Vector3D v = new Vector3D(leerNumero(st), 
                                            leerNumero(st), 
                                            leerNumero(st));
                      arr_luces.addElement(new Light2(Light2.DIRECCIONAL, v, r,g,b));
                    } 
                    else if ( st.sval.equals("point") ) {
                      imprimirMensaje("point");
                      Vector3D v = new Vector3D(leerNumero(st), 
                                            leerNumero(st), 
                                            leerNumero(st));
                      arr_luces.addElement(new Light2(Light2.PUNTUAL, v, r, g, b));
                    } 
                    else {
                      System.err.println("ERROR: in line " + 
                                         st.lineno()+" at "+st.sval);
                      throw new IOException(st.toString());
                    }
                  ;
                }
                else if ( st.sval.equals("surface") ) {
                  imprimirMensaje("surface");
                  float r = leerNumero(st);
                  float g = leerNumero(st);
                  float b = leerNumero(st);
                  float ka = leerNumero(st);
                  float kd = leerNumero(st);
                  float ks = leerNumero(st);
                  float ns = leerNumero(st);
                  float kr = leerNumero(st);
                  float kt = leerNumero(st);
                  float index = leerNumero(st);
                  /*
                  material_actual = new Material(r, g, b, 
                                                ka, kd, ks, 
                                                ns, kr, kt, index);
          */
                  material_actual = new Material();
                  material_actual.setAmbient(new ColorRgb(r*ka, g*ka, b*ka));
                  material_actual.setDiffuse(new ColorRgb(r*kd, g*kd, b*kd));
                  material_actual.setSpecular(new ColorRgb(r*ks, g*ks, b*ks));
                  material_actual.setPhongExponent(ns);
                  material_actual.setReflectionCoefficient(kr);
                  material_actual.setRefractionCoefficient(kt);
                }
              ;
              break;
            default:
              fin_de_lectura = true;
              break;
          } // switch
        } // while
        is.close();
        if ( st.ttype != StreamTokenizer.TT_EOF ) {
            System.err.println("ERROR: in line "+st.lineno()+" at "+st.sval);
            throw new IOException(st.toString());
        }

        Vector3D l, f, u;

        f = new Vector3D(camara.getFront());
        u = new Vector3D(camara.getUp());
        l = new Vector3D(f.crossProduct(u));
        l = l.multiply(-1);
        l.normalize();

        camara.setLeftDirect( l );
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
