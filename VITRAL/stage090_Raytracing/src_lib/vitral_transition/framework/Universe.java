//===========================================================================

package vitral_transition.framework;

// Paquetes de java utilizados para la agregacion multiple
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.Vector;

// Paquetes internos al sistema de raytracing / modelamiento
import vitral.toolkits.common.Vector3D;
import vitral.toolkits.common.Matrix4x4;
import vitral.toolkits.common.ColorRgb;
import vitral.toolkits.environment.Camera;
import vitral.toolkits.environment.Light;
import vitral.toolkits.environment.Material;
import vitral.toolkits.environment.Background;
import vitral.toolkits.environment.SimpleBackground;
import vitral.toolkits.environment.CubemapBackground;
import vitral.toolkits.geometry.RayableObject;
import vitral.toolkits.geometry.Geometry;
import vitral.toolkits.geometry.Sphere;
import vitral.toolkits.geometry.Cube;
import vitral.toolkits.geometry.Cylinder;
import vitral.toolkits.media.RGBAImage;
import vitral.toolkits.media.RGBAImageBuilder;

public class Universe
{
    // Variables especificas para la depuracion / desarrollo
    private static final boolean depurar = false;

    // El modelo del mundo
    public Vector<RayableObject> arr_cosas;
    public Vector<Light> arr_luces;
    public Background fondo;
    public Camera camara;
    public int viewportXSize;
    public int viewportYSize;

    public Universe()
    {
        camara = new Camera();
        fondo = new SimpleBackground();
        ((SimpleBackground)fondo).setColor(0, 0, 0);

        int CHUNKSIZE = 100; // Incremento de arreglos

        // Arreglo de Geometrys
        arr_cosas = new Vector<RayableObject>(CHUNKSIZE, CHUNKSIZE);  
        // Arreglo de LIGHTes
        arr_luces = new Vector<Light>(CHUNKSIZE, CHUNKSIZE);

        viewportXSize = 320;
        viewportYSize = 240;

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
        RayableObject thing;
        Matrix4x4 R, Ri;
        double yaw_actual = 0;
        double pitch_actual = 0;
        double roll_actual = 0;

        while ( !fin_de_lectura ) {
          switch ( st.nextToken() ) {
            case StreamTokenizer.TT_WORD:
              if ( st.sval.equals("sphere") ) {
                  Vector3D c = new Vector3D((float) leerNumero(st), 
                                            (float) leerNumero(st), 
                                            (float) leerNumero(st));
                  float r = (float)leerNumero(st);

                  imprimirMensaje("sphere");
                  thing = new RayableObject();
          thing.setGeometry(new Sphere(r));
                  thing.setMaterial(material_actual);

                  R = new Matrix4x4();
                  R.eulerAnglesRotation(yaw_actual, pitch_actual, roll_actual);
                  thing.setRotation(R);
                  Ri = new Matrix4x4(R);
                  Ri.invert();
                  thing.setRotationInverse(Ri);
                  thing.setPosition(c);
                  arr_cosas.addElement(thing);
                }
            else if ( st.sval.equals("cube") ) {
                  Vector3D c = new Vector3D((float) leerNumero(st), 
                                            (float) leerNumero(st), 
                                            (float) leerNumero(st));
                  float r = (float)leerNumero(st);

                  imprimirMensaje("cube");
                  thing = new RayableObject();
          thing.setGeometry(new Cube(r));
                  thing.setMaterial(material_actual);
                  R = new Matrix4x4();
                  R.eulerAnglesRotation(yaw_actual, pitch_actual, roll_actual);
                  thing.setRotation(R);
                  Ri = new Matrix4x4(R);
                  Ri.invert();
                  thing.setRotationInverse(Ri);
                  thing.setPosition(c);
                  arr_cosas.addElement(thing);
                } 
            else if ( st.sval.equals("cylinder") ) {
                  Vector3D c = new Vector3D((float) leerNumero(st), 
                                            (float) leerNumero(st), 
                                            (float) leerNumero(st));
                  float r1 = (float)leerNumero(st);
                  float r2 = (float)leerNumero(st);
                  float h = (float)leerNumero(st);

                  imprimirMensaje("cylinder");
                  thing = new RayableObject();
          thing.setGeometry(new Cylinder(r1, r2, h));
                  thing.setMaterial(material_actual);
                  R = new Matrix4x4();
                  R.eulerAnglesRotation(yaw_actual, pitch_actual, roll_actual);
                  thing.setRotation(R);
                  Ri = new Matrix4x4(R);
                  Ri.invert();
                  thing.setRotationInverse(Ri);
                  thing.setPosition(c);
                  arr_cosas.addElement(thing);
                }
                /*
                else if (st.sval.equals("triangles")) {
                  imprimirMensaje("triangles");
                  thing = new RayableObject();
          thing.setGeometry(new MESH(st));
                  thing.setMaterial(material_actual);
                  arr_cosas.addElement(thing);
                } 
                */
                else if (st.sval.equals("viewport")) {
                  imprimirMensaje("viewport");

                  viewportXSize = (int)leerNumero(st);
                  viewportYSize = (int)leerNumero(st);
                }
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
                  ((SimpleBackground)fondo).setColor(leerNumero(st), 
                                 leerNumero(st), 
                                 leerNumero(st));
                }
                else if (st.sval.equals("backgroundcubemap")) {

            RGBAImage front, right, back, left, down, up;

            try {

            System.out.print("Loading background: 1");
            front = RGBAImageBuilder.buildImage(
                        new File("./etc/cubemaps/dorise1/entorno0_small.jpg"));
            System.out.print("2");
            right = RGBAImageBuilder.buildImage(
                        new File("./etc/cubemaps/dorise1/entorno1_small.jpg"));
            System.out.print("3");
            back = RGBAImageBuilder.buildImage(
                        new File("./etc/cubemaps/dorise1/entorno2_small.jpg"));
            System.out.print("4");
            left = RGBAImageBuilder.buildImage(
                        new File("./etc/cubemaps/dorise1/entorno3_small.jpg"));
            System.out.print("5");
            down = RGBAImageBuilder.buildImage(
                        new File("./etc/cubemaps/dorise1/entorno4_small.jpg"));
            System.out.print("6");
            up = RGBAImageBuilder.buildImage(
                        new File("./etc/cubemaps/dorise1/entorno5_small.jpg"));
            System.out.println(" OK!");

            fondo = 
                new CubemapBackground(camara, 
                                      front, right, back, left, down, up);

            }
            catch (Exception e) {
            System.err.println("Error armando el cubemap!");
            System.exit(0);
            }

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
                      arr_luces.addElement(new Light(Light.AMBIENTE, null, new ColorRgb(r,g,b)));
                    }
                    else if ( st.sval.equals("directional") ) {
                      imprimirMensaje("directional");
                      Vector3D v = new Vector3D(leerNumero(st), 
                                            leerNumero(st), 
                                            leerNumero(st));
                      arr_luces.addElement(new Light(Light.DIRECCIONAL, v, new ColorRgb(r,g,b)));
                    } 
                    else if ( st.sval.equals("point") ) {
                      imprimirMensaje("point");
                      Vector3D v = new Vector3D(leerNumero(st), 
                                            leerNumero(st), 
                                            leerNumero(st));
                      arr_luces.addElement(new Light(Light.PUNTUAL, v, new ColorRgb(r, g, b)));
                    } 
                    else {
                      System.err.println("ERROR: in line " + 
                                         st.lineno()+" at "+st.sval);
                      throw new IOException(st.toString());
                    }
                  ;
                }

                else if ( st.sval.equals("rotation") ) {
                  imprimirMensaje("rotation");
                  yaw_actual = leerNumero(st);
                  pitch_actual = leerNumero(st);
                  roll_actual = leerNumero(st);
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
