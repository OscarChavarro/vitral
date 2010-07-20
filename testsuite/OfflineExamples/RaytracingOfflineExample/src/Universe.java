//===========================================================================

// Java classes
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;

// VSDK classes
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Background;
import vsdk.toolkit.environment.SimpleBackground;
import vsdk.toolkit.environment.CubemapBackground;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.geometry.Box;
import vsdk.toolkit.environment.geometry.Cone;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.io.image.ImagePersistence;

public class Universe
{
    // Debug flag
    private static final boolean showDebugMessages = false;

    // Simple scene
    public Camera currentCamera;
    public Background currentBackground;

    // Viewport size information
    public int viewportXSize;
    public int viewportYSize;

    public Universe()
    {
        currentCamera = new Camera();
        currentBackground = new SimpleBackground();
        ((SimpleBackground)currentBackground).setColor(0, 0, 0);

        viewportXSize = 320;
        viewportYSize = 240;
    }

    private void
    showDebugMessage(String m)
    {
        if ( showDebugMessages ) {
            System.out.println(m);
        }
    }

    private float
    readNumber(StreamTokenizer st) throws IOException {
        if (st.nextToken() != StreamTokenizer.TT_NUMBER) {
            System.err.println("ERROR: number expected in line "+st.lineno());
            throw new IOException(st.toString());
        }
        return (float)(st.nval);
    }

    public void
    importEnvironment(InputStream is, SimpleScene theScene) throws Exception {
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
        SimpleBody thing;
        Matrix4x4 R, Ri;
        double yaw_actual = 0;
        double pitch_actual = 0;
        double roll_actual = 0;

        while ( !fin_de_lectura ) {
          switch ( st.nextToken() ) {
            case StreamTokenizer.TT_WORD:
              if ( st.sval.equals("sphere") ) {
                  Vector3D c = new Vector3D((float) readNumber(st), 
                                            (float) readNumber(st), 
                                            (float) readNumber(st));
                  float r = (float)readNumber(st);

                  showDebugMessage("sphere");
                  thing = new SimpleBody();
                  thing.setGeometry(new Sphere(r));
                  thing.setMaterial(material_actual);

                  R = new Matrix4x4();
                  R.eulerAnglesRotation(yaw_actual, pitch_actual, roll_actual);
                  thing.setRotation(R);
                  Ri = new Matrix4x4(R);
                  Ri.invert();
                  thing.setRotationInverse(Ri);
                  thing.setPosition(c);
                  theScene.addBody(thing);
                }
                else if ( st.sval.equals("cube") ) {
                  Vector3D c = new Vector3D((float) readNumber(st), 
                                            (float) readNumber(st), 
                                            (float) readNumber(st));
                  float r = (float)readNumber(st);

                  showDebugMessage("cube");
                  thing = new SimpleBody();
                  thing.setGeometry(new Box(r, r, r));
                  thing.setMaterial(material_actual);
                  R = new Matrix4x4();
                  R.eulerAnglesRotation(yaw_actual, pitch_actual, roll_actual);
                  thing.setRotation(R);
                  Ri = new Matrix4x4(R);
                  Ri.invert();
                  thing.setRotationInverse(Ri);
                  thing.setPosition(c);
                  theScene.addBody(thing);
                } 
                else if ( st.sval.equals("cylinder") ) {
                  Vector3D c = new Vector3D((float) readNumber(st), 
                                            (float) readNumber(st), 
                                            (float) readNumber(st));
                  float r1 = (float)readNumber(st);
                  float r2 = (float)readNumber(st);
                  float h = (float)readNumber(st);

                  showDebugMessage("cylinder");
                  thing = new SimpleBody();
                  thing.setGeometry(new Cone(r1, r2, h));
                  thing.setMaterial(material_actual);
                  R = new Matrix4x4();
                  R.eulerAnglesRotation(yaw_actual, pitch_actual, roll_actual);
                  thing.setRotation(R);
                  Ri = new Matrix4x4(R);
                  Ri.invert();
                  thing.setRotationInverse(Ri);
                  thing.setPosition(c);
                  theScene.addBody(thing);
                }
                /*
                else if (st.sval.equals("triangles")) {
                  showDebugMessage("triangles");
                  thing = new SimpleBody();
                  thing.setGeometry(new MESH(st));
                  thing.setMaterial(material_actual);
                  theScene.addBody(thing);
                } 
                */
                else if (st.sval.equals("viewport")) {
                  showDebugMessage("viewport");

                  viewportXSize = (int)readNumber(st);
                  viewportYSize = (int)readNumber(st);
                }
                else if (st.sval.equals("eye")) {
                  showDebugMessage("eye");
                  currentCamera.setPosition(new Vector3D(readNumber(st), 
                                                  readNumber(st), 
                                                  readNumber(st)));
                }
                else if (st.sval.equals("lookat")) {
                  showDebugMessage("lookat");
                  currentCamera.setFocusedPositionMaintainingOrthogonality(new Vector3D(readNumber(st), 
                                                      readNumber(st), 
                                                      readNumber(st)));
                }
                else if (st.sval.equals("up")) {
                  showDebugMessage("up");
                  currentCamera.setUpDirect(new Vector3D(readNumber(st), 
                                            readNumber(st), 
                                            readNumber(st)));
                }
                else if (st.sval.equals("fov")) {
                  showDebugMessage("fov");
                  currentCamera.setFov(readNumber(st));
                }
                else if (st.sval.equals("background")) {
                  showDebugMessage("background");
                  currentBackground = new SimpleBackground();
                  ((SimpleBackground)currentBackground).setColor(readNumber(st), 
                                 readNumber(st), 
                                 readNumber(st));
                }
                else if (st.sval.equals("backgroundcubemap")) {

            RGBAImage front, right, back, left, down, up;

                    try {

            System.out.print("  - Loading background images: 1");
            front = ImagePersistence.importRGBA(
                        new File("../../../etc/cubemaps/dorise1/entorno0.jpg"));
            System.out.print("2");
            right = ImagePersistence.importRGBA(
                        new File("../../../etc/cubemaps/dorise1/entorno1.jpg"));
            System.out.print("3");
            back = ImagePersistence.importRGBA(
                        new File("../../../etc/cubemaps/dorise1/entorno2.jpg"));
            System.out.print("4");
            left = ImagePersistence.importRGBA(
                        new File("../../../etc/cubemaps/dorise1/entorno3.jpg"));
            System.out.print("5");
            down = ImagePersistence.importRGBA(
                        new File("../../../etc/cubemaps/dorise1/entorno4.jpg"));
            System.out.print("6");
            up = ImagePersistence.importRGBA(
                        new File("../../../etc/cubemaps/dorise1/entorno5.jpg"));
            System.out.println(" OK!");

            currentBackground = 
                new CubemapBackground(currentCamera, 
                                      front, right, back, left, down, up);

                    }
                    catch (Exception e) {
                        System.err.println("Error armando el cubemap!");
                        System.exit(0);
                    }

                }
                else if (st.sval.equals("light")) {
                  showDebugMessage("light");
                  float r = readNumber(st);
                  float g = readNumber(st);
                  float b = readNumber(st);
                  if ( st.nextToken() != StreamTokenizer.TT_WORD ) {
                      System.err.println("ERROR: in line "+st.lineno() + 
                                         " at "+st.sval);
                      throw new IOException(st.toString());
                  }
                  if ( st.sval.equals("ambient") ) {
                      showDebugMessage("ambient");
                      theScene.addLight(new Light(Light.AMBIENT, null, new ColorRgb(r,g,b)));
                    }
                    else if ( st.sval.equals("directional") ) {
                      showDebugMessage("directional");
                      Vector3D v = new Vector3D(readNumber(st), 
                                            readNumber(st), 
                                            readNumber(st));
                      theScene.addLight(new Light(Light.DIRECTIONAL, v, new ColorRgb(r,g,b)));
                    } 
                    else if ( st.sval.equals("point") ) {
                      showDebugMessage("point");
                      Vector3D v = new Vector3D(readNumber(st), 
                                            readNumber(st), 
                                            readNumber(st));
                      theScene.addLight(new Light(Light.POINT, v, new ColorRgb(r, g, b)));
                    } 
                    else {
                      System.err.println("ERROR: in line " + 
                                         st.lineno()+" at "+st.sval);
                      throw new IOException(st.toString());
                    }
                  ;
                }

                else if ( st.sval.equals("rotation") ) {
                  showDebugMessage("rotation");
                  yaw_actual = readNumber(st);
                  pitch_actual = readNumber(st);
                  roll_actual = readNumber(st);
                }
                else if ( st.sval.equals("surface") ) {
                  showDebugMessage("surface");
                  float r = readNumber(st);
                  float g = readNumber(st);
                  float b = readNumber(st);
                  float ka = readNumber(st);
                  float kd = readNumber(st);
                  float ks = readNumber(st);
                  float ns = readNumber(st);
                  float kr = readNumber(st);
                  float kt = readNumber(st);
                  float index = readNumber(st);
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

        f = new Vector3D(currentCamera.getFront());
        u = new Vector3D(currentCamera.getUp());
        l = new Vector3D(f.crossProduct(u));
        l = l.multiply(-1);
        l.normalize();

        currentCamera.setLeftDirect( l );

        theScene.addBackground(currentBackground);
        theScene.addCamera(currentCamera);
        theScene.setActiveCameraIndex(0);
        theScene.setActiveBackgroundIndex(0);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
