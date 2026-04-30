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

package vsdk.toolkit.io.geometry;

// Java classes
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.HashMap;

// VSDK classes
import vsdk.toolkit.common.Triangle;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Background;
import vsdk.toolkit.environment.SimpleBackground;
import vsdk.toolkit.environment.CubemapBackground;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.geometry.volume.Torus;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.surface.QuadMesh;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.media.IndexedColorImageUncompressed;
import vsdk.toolkit.media.NormalMap;
import vsdk.toolkit.media.RGBAImageUncompressed;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.io.PersistenceElement;
import vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;
import vsdk.toolkit.processing.polyhedralBoundedSolidOperators.SimpleTestGeometryLibrary;
import vsdk.toolkit.environment.LightType;

/**
This class implements an scene reader based on the instructional raytracer
from Tomas Lozano-Perez from computer graphics class at MIT on spring 2001,
and from Leonard McMillan 1998. This material was adapted by Oscar Chavarro
for computer graphics classes at Colombia, and later as anoter scene reader
for Vitral.
*/
public class ReaderMitScene extends PersistenceElement
{
    private final class ImportContext {
        Camera currentCamera;
        Background currentBackground;
        int viewportXSize;
        int viewportYSize;
        Vector3D importedEye;
        Vector3D importedLookAt;
        Vector3D importedUp;
        double importedHorizontalFov;

        ImportContext()
        {
            currentCamera = new Camera();
            currentBackground = new SimpleBackground();
            ((SimpleBackground)currentBackground).setColor(0, 0, 0);

            viewportXSize = 320;
            viewportYSize = 240;
            importedEye = new Vector3D(0, 0, 10);
            importedLookAt = new Vector3D(0, 0, 0);
            importedUp = new Vector3D(0, 1, 0);
            importedHorizontalFov = 30;
        }
    }

    public ReaderMitScene()
    {
    }

    private void
    showDebugMessage(String m)
    {
        if ( showDebugMessages() ) {
            System.out.println(m);
        }
    }

    private boolean
    showDebugMessages()
    {
        return false;
    }

    private double
    readNumber(StreamTokenizer st) throws IOException {
        if (st.nextToken() != StreamTokenizer.TT_NUMBER) {
            System.err.println("ERROR: number expected in line "+st.lineno());
            throw new IOException(st.toString());
        }
        return st.nval;
    }

    private String
    readStringToken(StreamTokenizer st) throws IOException
    {
        int tokenType = st.nextToken();
        if ( tokenType == StreamTokenizer.TT_WORD || tokenType == '"' ) {
            return st.sval;
        }
        System.err.println("ERROR: string expected in line " + st.lineno());
        throw new IOException(st.toString());
    }

    private Material
    readSurfaceDefinition(StreamTokenizer st) throws IOException
    {
        double r = readNumber(st);
        double g = readNumber(st);
        double b = readNumber(st);
        double ka = readNumber(st);
        double kd = readNumber(st);
        double ks = readNumber(st);
        double ns = readNumber(st);
        double kr = readNumber(st);
        double kt = readNumber(st);
        double index = readNumber(st);

        Material material = new Material();
        material.setAmbient(new ColorRgb(r*ka, g*ka, b*ka));
        material.setDiffuse(new ColorRgb(r*kd, g*kd, b*kd));
        material.setSpecular(new ColorRgb(ks, ks, ks));
        material.setPhongExponent(ns);
        material.setReflectionCoefficient(kr);
        material.setRefractionCoefficient(kt);
        return material;
    }

    private RGBImageUncompressed
    loadRgbTexture(
        String texturePath,
        HashMap<String, RGBImageUncompressed> textureCache) throws Exception
    {
        RGBImageUncompressed texture = textureCache.get(texturePath);
        if ( texture == null ) {
            texture = ImagePersistence.importRGB(new File(texturePath));
            textureCache.put(texturePath, texture);
        }
        return texture;
    }

    private NormalMap
    loadNormalMapFromBump(
        String bumpPath,
        Vector3D bumpScale,
        HashMap<String, NormalMap> normalMapCache) throws Exception
    {
        String cacheKey =
            bumpPath + "|" + bumpScale.x() + "|" + bumpScale.y() + "|" + bumpScale.z();
        NormalMap normalMap = normalMapCache.get(cacheKey);
        if ( normalMap == null ) {
            IndexedColorImageUncompressed bumpMap = ImagePersistence.importIndexedColor(new File(bumpPath));
            normalMap = new NormalMap();
            normalMap.importBumpMap(bumpMap, bumpScale);
            normalMapCache.put(cacheKey, normalMap);
        }
        return normalMap;
    }

    private void
    applyBodyTransform(SimpleBody thing,
                       double yaw, double pitch, double roll,
                       Vector3D position)
    {
        Matrix4x4 R = new Matrix4x4();
        R = R.eulerAnglesRotation(yaw, pitch, roll);
        thing.setRotation(R);
        Matrix4x4 Ri = new Matrix4x4(R);
        Ri = Ri.invert();
        thing.setRotationInverse(Ri);
        thing.setPosition(position);
    }

    private void
    flushTriangleBatch(SimpleScene theScene,
                       ArrayList<Vertex> vertices,
                       ArrayList<Triangle> triangles,
                       Material material,
                       double yaw, double pitch, double roll)
    {
        if ( triangles.isEmpty() ) {
            return;
        }

        TriangleMesh mesh = new TriangleMesh();
        mesh.setVertexes(vertices.toArray(new Vertex[0]), false, false, false, false);
        mesh.setTriangles(triangles.toArray(new Triangle[0]));
        mesh.calculateNormals();

        SimpleBody thing = new SimpleBody();
        thing.setGeometry(mesh);
        thing.setMaterial(material);
        applyBodyTransform(thing, yaw, pitch, roll, new Vector3D());
        theScene.addBody(thing);
    }

    private void
    addImportedObj(SimpleScene theScene,
                   String objectPath,
                   Material fallbackMaterial,
                   double yaw, double pitch, double roll,
                   Vector3D translation,
                   double uniformScale) throws Exception
    {
        SimpleScene importedScene = new SimpleScene();
        EnvironmentPersistence.importEnvironment(new File(objectPath), importedScene);

        Matrix4x4 sceneRotation = new Matrix4x4().eulerAnglesRotation(yaw, pitch, roll);
        for ( SimpleBody importedBody : importedScene.getSimpleBodies() ) {
            SimpleBody thing = new SimpleBody();
            thing.setName(importedBody.getName());
            thing.setGeometry(importedBody.getGeometry());
            thing.setMaterial(importedBody.getMaterial() != null
                ? importedBody.getMaterial() : fallbackMaterial);
            thing.setTexture(importedBody.getTexture());
            thing.setNormalMap(importedBody.getNormalMap());

            Matrix4x4 composedRotation = sceneRotation.multiply(importedBody.getRotation());
            thing.setRotation(composedRotation);
            thing.setRotationInverse(new Matrix4x4(composedRotation).invert());

            Vector3D sourceScale = importedBody.getScale();
            thing.setScale(new Vector3D(
                sourceScale.x() * uniformScale,
                sourceScale.y() * uniformScale,
                sourceScale.z() * uniformScale));

            Vector3D translatedPosition =
                sceneRotation.multiply(importedBody.getPosition().multiply(uniformScale))
                    .add(translation);
            thing.setPosition(translatedPosition);
            theScene.addBody(thing);
        }
    }

    private void
    flushQuadBatch(SimpleScene theScene,
                   ArrayList<Vertex> vertices,
                   ArrayList<int[]> quads,
                   Material material,
                   double yaw, double pitch, double roll)
    {
        if ( quads.isEmpty() ) {
            return;
        }

        QuadMesh mesh = new QuadMesh();
        mesh.setVertexes(vertices.toArray(new Vertex[0]));
        mesh.initQuadArrays(quads.size());
        int[] quadIndices = mesh.getQuadIndices();
        for ( int i = 0; i < quads.size(); i++ ) {
            int[] q = quads.get(i);
            quadIndices[4*i] = q[0];
            quadIndices[4*i+1] = q[1];
            quadIndices[4*i+2] = q[2];
            quadIndices[4*i+3] = q[3];
        }

        SimpleBody thing = new SimpleBody();
        thing.setGeometry(mesh);
        thing.setMaterial(material);
        applyBodyTransform(thing, yaw, pitch, roll, new Vector3D());
        theScene.addBody(thing);
    }

    private double
    convertMitHorizontalFovToVertical(
        double horizontalFov,
        int viewportXSize,
        int viewportYSize)
    {
        if ( viewportXSize <= 0 || viewportYSize <= 0 ) {
            return horizontalFov;
        }

        double aspect = (double)viewportXSize / (double)viewportYSize;
        double horizontalHalfAngle = Math.toRadians(horizontalFov / 2.0);
        double verticalHalfAngle = Math.atan(Math.tan(horizontalHalfAngle) / aspect);

        return Math.toDegrees(2.0 * verticalHalfAngle);
    }

    private void
    configureCurrentCameraFromMitView(ImportContext context)
    {
        context.currentCamera.setPosition(context.importedEye);
        context.currentCamera.setUpDirect(context.importedUp);
        context.currentCamera.setFocusedPositionMaintainingOrthogonality(
            context.importedLookAt);
        context.currentCamera.setFov(convertMitHorizontalFovToVertical(
            context.importedHorizontalFov,
            context.viewportXSize,
            context.viewportYSize));
        context.currentCamera.updateViewportResize(
            context.viewportXSize,
            context.viewportYSize);
    }

    public void
    importEnvironment(InputStream is, SimpleScene theScene) throws Exception {
        ImportContext context = new ImportContext();
        configureCurrentCameraFromMitView(context);

        Reader parsero = new BufferedReader(new InputStreamReader(is));
        StreamTokenizer st = new StreamTokenizer(parsero);
        st.commentChar('#');
        st.quoteChar('"');
        st.eolIsSignificant(true);
        boolean fin_de_lectura = false;
        Material currentMaterial;
        Material currentTrianglesMaterial = null;
        Material currentQuadsMaterial = null;
        RGBImageUncompressed currentTexture = null;
        NormalMap currentNormalMap = null;
        HashMap<String, RGBImageUncompressed> textureCache = new HashMap<String, RGBImageUncompressed>();
        HashMap<String, NormalMap> normalMapCache = new HashMap<String, NormalMap>();

        // Material por defecto...
        /*
        currentMaterial = new Material(0.8f, 0.2f, 0.9f, 
                                       0.2f, 0.4f, 0.4f, 
                                       10.0f, 0f, 0f, 1f);
        */
        currentMaterial = new Material();
        currentMaterial.setAmbient(new ColorRgb(0.8*0.2, 0.2*0.2, 0.9*0.2));
        currentMaterial.setDiffuse(new ColorRgb(0.8*0.4, 0.2*0.4, 0.9*0.4));
        currentMaterial.setSpecular(new ColorRgb(0.4, 0.4, 0.4));
        currentMaterial.setReflectionCoefficient(0);
        currentMaterial.setRefractionCoefficient(0);
        currentMaterial.setPhongExponent(10);

        boolean readingTriangles = false;
        boolean readingQuads = false;
        ArrayList<Vertex> triangleVertices = new ArrayList<Vertex>();
        ArrayList<Triangle> triangleFaces = new ArrayList<Triangle>();
        ArrayList<Vertex> quadVertices = new ArrayList<Vertex>();
        ArrayList<int[]> quadFaces = new ArrayList<int[]>();
        SimpleBody thing;
        double yaw_actual = 0;
        double pitch_actual = 0;
        double roll_actual = 0;

        while ( !fin_de_lectura ) {
          int tokenType = st.nextToken();
          switch ( tokenType ) {
            case StreamTokenizer.TT_EOL:
              break;
            case StreamTokenizer.TT_EOF:
              fin_de_lectura = true;
              break;
            case StreamTokenizer.TT_WORD:
              if ( readingTriangles ) {
                  if ( st.sval.equals("v") ) {
                      Vector3D p = new Vector3D(
                          readNumber(st),
                          readNumber(st),
                          readNumber(st));
                      triangleVertices.add(new Vertex(p));
                  }
                  else if ( st.sval.equals("f") ) {
                      ArrayList<Integer> polygonIndices = new ArrayList<Integer>();
                      while ( true ) {
                          int faceToken = st.nextToken();
                          if ( faceToken == StreamTokenizer.TT_EOL ) {
                              break;
                          }
                          if ( faceToken == StreamTokenizer.TT_EOF ) {
                              fin_de_lectura = true;
                              break;
                          }

                          int index;
                          if ( faceToken == StreamTokenizer.TT_NUMBER ) {
                              index = (int)st.nval;
                          }
                          else if ( faceToken == StreamTokenizer.TT_WORD ) {
                              index = Integer.parseInt(st.sval);
                          }
                          else {
                              System.err.println("ERROR: face index expected in line " + st.lineno());
                              throw new IOException(st.toString());
                          }
                          polygonIndices.add(Integer.valueOf(index));
                      }
                      if ( polygonIndices.size() < 3 ) {
                          System.err.println("ERROR: face with less than 3 vertices in line " + st.lineno());
                          throw new IOException(st.toString());
                      }
                      int anchor = polygonIndices.get(0).intValue();
                      for ( int i = 1; i < polygonIndices.size()-1; i++ ) {
                          int i1 = polygonIndices.get(i).intValue();
                          int i2 = polygonIndices.get(i+1).intValue();
                          triangleFaces.add(new Triangle(anchor, i1, i2));
                      }
                  }
                  else if ( st.sval.equals("surface") ) {
                      flushTriangleBatch(theScene, triangleVertices, triangleFaces,
                          currentTrianglesMaterial, yaw_actual, pitch_actual, roll_actual);
                      triangleFaces.clear();
                      currentMaterial = readSurfaceDefinition(st);
                      currentTrianglesMaterial = currentMaterial;
                  }
                  else if ( st.sval.equals("end") ) {
                      flushTriangleBatch(theScene, triangleVertices, triangleFaces,
                          currentTrianglesMaterial, yaw_actual, pitch_actual, roll_actual);
                      triangleVertices.clear();
                      triangleFaces.clear();
                      readingTriangles = false;
                  }
                  else {
                      System.err.println("ERROR: unsupported triangles token \"" + st.sval +
                          "\" in line " + st.lineno());
                      throw new IOException(st.toString());
                  }
                  break;
              }
              if ( readingQuads ) {
                  if ( st.sval.equals("v") ) {
                      Vector3D p = new Vector3D(
                          readNumber(st),
                          readNumber(st),
                          readNumber(st));
                      quadVertices.add(new Vertex(p));
                  }
                  else if ( st.sval.equals("q") ) {
                      int i0 = (int)readNumber(st);
                      int i1 = (int)readNumber(st);
                      int i2 = (int)readNumber(st);
                      int i3 = (int)readNumber(st);
                      quadFaces.add(new int[] {i0, i1, i2, i3});
                  }
                  else if ( st.sval.equals("surface") ) {
                      flushQuadBatch(theScene, quadVertices, quadFaces,
                          currentQuadsMaterial, yaw_actual, pitch_actual, roll_actual);
                      quadFaces.clear();
                      currentMaterial = readSurfaceDefinition(st);
                      currentQuadsMaterial = currentMaterial;
                  }
                  else if ( st.sval.equals("end") ) {
                      flushQuadBatch(theScene, quadVertices, quadFaces,
                          currentQuadsMaterial, yaw_actual, pitch_actual, roll_actual);
                      quadVertices.clear();
                      quadFaces.clear();
                      readingQuads = false;
                  }
                  else {
                      System.err.println("ERROR: unsupported quads token \"" + st.sval +
                          "\" in line " + st.lineno());
                      throw new IOException(st.toString());
                  }
                  break;
              }

              if ( st.sval.equals("sphere") ) {
                  Vector3D c = new Vector3D(readNumber(st), 
                                            readNumber(st), 
                                            readNumber(st));
                  double r = readNumber(st);

                  showDebugMessage("sphere");
                  thing = new SimpleBody();
                  thing.setGeometry(new Sphere(r));
                  thing.setMaterial(currentMaterial);
                  thing.setTexture(currentTexture);
                  thing.setNormalMap(currentNormalMap);
                  applyBodyTransform(thing, yaw_actual, pitch_actual, roll_actual, c);
                  theScene.addBody(thing);
                }
                else if ( st.sval.equals("cube") ) {
                  Vector3D c = new Vector3D(readNumber(st), 
                                            readNumber(st), 
                                            readNumber(st));
                  double r = readNumber(st);

                  showDebugMessage("cube");
                  thing = new SimpleBody();
                  thing.setGeometry(new Box(r, r, r));
                  thing.setMaterial(currentMaterial);
                  thing.setTexture(currentTexture);
                  thing.setNormalMap(currentNormalMap);
                  applyBodyTransform(thing, yaw_actual, pitch_actual, roll_actual, c);
                  theScene.addBody(thing);
                } 
                else if ( st.sval.equals("cylinder") ) {
                  Vector3D c = new Vector3D(readNumber(st), 
                                            readNumber(st), 
                                            readNumber(st));
                  double r1 = readNumber(st);
                  double r2 = readNumber(st);
                  double h = readNumber(st);

                  showDebugMessage("cylinder");
                  thing = new SimpleBody();
                  thing.setGeometry(new Cone(r1, r2, h));
                  thing.setMaterial(currentMaterial);
                  thing.setTexture(currentTexture);
                  thing.setNormalMap(currentNormalMap);
                  applyBodyTransform(thing, yaw_actual, pitch_actual, roll_actual, c);
                  theScene.addBody(thing);
                }
                else if ( st.sval.equals("torus") ) {
                  Vector3D c = new Vector3D(readNumber(st),
                                            readNumber(st),
                                            readNumber(st));
                  double majorRadius = readNumber(st);
                  double minorRadius = readNumber(st);

                  showDebugMessage("torus");
                  thing = new SimpleBody();
                  thing.setGeometry(new Torus(majorRadius, minorRadius));
                  thing.setMaterial(currentMaterial);
                  thing.setTexture(currentTexture);
                  thing.setNormalMap(currentNormalMap);
                  applyBodyTransform(thing, yaw_actual, pitch_actual, roll_actual, c);
                  theScene.addBody(thing);
                }
                else if ( st.sval.equals("polybox") ) {
                  Vector3D c = new Vector3D(readNumber(st),
                                            readNumber(st),
                                            readNumber(st));
                  double sx = readNumber(st);
                  double sy = readNumber(st);
                  double sz = readNumber(st);

                  showDebugMessage("polybox");
                  PolyhedralBoundedSolid solid =
                      new Box(sx, sy, sz).exportToPolyhedralBoundedSolid();
                  thing = new SimpleBody();
                  thing.setGeometry(solid);
                  thing.setMaterial(currentMaterial);
                  thing.setTexture(currentTexture);
                  thing.setNormalMap(currentNormalMap);
                  applyBodyTransform(thing, yaw_actual, pitch_actual, roll_actual, c);
                  theScene.addBody(thing);
                }
                else if ( st.sval.equals("appel") ) {
                  int appelId = (int)readNumber(st);
                  Vector3D c = new Vector3D(readNumber(st),
                                            readNumber(st),
                                            readNumber(st));
                  double uniformScale = 1.0;
                  int nextToken = st.nextToken();
                  if ( nextToken == StreamTokenizer.TT_NUMBER ) {
                      uniformScale = st.nval;
                  }
                  else {
                      st.pushBack();
                  }
                  if ( uniformScale <= 0 ) {
                      uniformScale = 1.0;
                  }

                  showDebugMessage("appel");
                  PolyhedralBoundedSolid solid;
                  if ( appelId == 1 ) {
                      solid = SimpleTestGeometryLibrary.createTestObjectAPPE1967_1();
                  }
                  else if ( appelId == 2 ) {
                      solid = SimpleTestGeometryLibrary.createTestObjectAPPE1967_2();
                  }
                  else {
                      solid = SimpleTestGeometryLibrary.createTestObjectAPPE1967_3();
                  }

                  // APPEL solids are defined in [0,1]^3; recenter for scene placement.
                  PolyhedralBoundedSolidModeler.applyTransformation(solid, new Matrix4x4().translation(-0.5, -0.5, -0.5));

                  thing = new SimpleBody();
                  thing.setGeometry(solid);
                  thing.setMaterial(currentMaterial);
                  thing.setTexture(currentTexture);
                  thing.setNormalMap(currentNormalMap);
                  thing.setScale(new Vector3D(uniformScale, uniformScale, uniformScale));
                  applyBodyTransform(thing, yaw_actual, pitch_actual, roll_actual, c);
                  theScene.addBody(thing);
                }
                else if ( st.sval.equals("obj") ) {
                  String objectPath = readStringToken(st);
                  Vector3D c = new Vector3D(readNumber(st),
                                            readNumber(st),
                                            readNumber(st));
                  double uniformScale = 1.0;
                  int nextToken = st.nextToken();
                  if ( nextToken == StreamTokenizer.TT_NUMBER ) {
                      uniformScale = st.nval;
                  }
                  else {
                      st.pushBack();
                  }
                  if ( uniformScale <= 0 ) {
                      uniformScale = 1.0;
                  }
                  addImportedObj(theScene, objectPath, currentMaterial,
                      yaw_actual, pitch_actual, roll_actual, c, uniformScale);
                }
                else if ( st.sval.equals("triangles") ) {
                  showDebugMessage("triangles");
                  readingTriangles = true;
                  triangleVertices.clear();
                  triangleFaces.clear();
                  currentTrianglesMaterial = currentMaterial;
                }
                else if ( st.sval.equals("quads") ) {
                  showDebugMessage("quads");
                  readingQuads = true;
                  quadVertices.clear();
                  quadFaces.clear();
                  currentQuadsMaterial = currentMaterial;
                }
                else if (st.sval.equals("viewport")) {
                  showDebugMessage("viewport");

                  context.viewportXSize = (int)readNumber(st);
                  context.viewportYSize = (int)readNumber(st);
                  configureCurrentCameraFromMitView(context);
                }
                else if (st.sval.equals("eye")) {
                  showDebugMessage("eye");
                  context.importedEye = new Vector3D(readNumber(st),
                                                     readNumber(st),
                                                     readNumber(st));
                  configureCurrentCameraFromMitView(context);
                }
                else if (st.sval.equals("lookat")) {
                  showDebugMessage("lookat");
                  context.importedLookAt = new Vector3D(readNumber(st),
                                                        readNumber(st),
                                                        readNumber(st));
                  configureCurrentCameraFromMitView(context);
                }
                else if (st.sval.equals("up")) {
                  showDebugMessage("up");
                  context.importedUp = new Vector3D(readNumber(st),
                                                    readNumber(st),
                                                    readNumber(st));
                  configureCurrentCameraFromMitView(context);
                }
                else if (st.sval.equals("fov")) {
                  showDebugMessage("fov");
                  context.importedHorizontalFov = readNumber(st);
                  configureCurrentCameraFromMitView(context);
                }
                else if (st.sval.equals("background")) {
                  showDebugMessage("background");
                  context.currentBackground = new SimpleBackground();
                  ((SimpleBackground)context.currentBackground).setColor(readNumber(st),
                                 readNumber(st),
                                 readNumber(st));
                }
                else if (st.sval.equals("backgroundcubemap")) {

            RGBAImageUncompressed front, right, back, left, down, up;

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

            context.currentBackground =
                new CubemapBackground(context.currentCamera,
                                      front, right, back, left, down, up);

                    }
                    catch (Exception e) {
                        System.err.println("Error armando el cubemap!");
                        System.exit(0);
                    }
                }
                else if (st.sval.equals("light")) {
                  showDebugMessage("light");
                  double r = readNumber(st);
                  double g = readNumber(st);
                  double b = readNumber(st);
                  if ( st.nextToken() != StreamTokenizer.TT_WORD ) {
                      System.err.println("ERROR: in line "+st.lineno() + 
                                         " at "+st.sval);
                      throw new IOException(st.toString());
                  }
                  if ( st.sval.equals("ambient") ) {
                      showDebugMessage("ambient");
                      theScene.addLight(new Light(LightType.AMBIENT, null, new ColorRgb(r,g,b)));
                    }
                    else if ( st.sval.equals("directional") ) {
                      showDebugMessage("directional");
                      Vector3D v = new Vector3D(readNumber(st), 
                                            readNumber(st), 
                                            readNumber(st));
                      theScene.addLight(new Light(LightType.DIRECTIONAL, v, new ColorRgb(r,g,b)));
                    } 
                    else if ( st.sval.equals("point") ) {
                      showDebugMessage("point");
                      Vector3D v = new Vector3D(readNumber(st), 
                                            readNumber(st), 
                                            readNumber(st));
                      theScene.addLight(new Light(LightType.POINT, v, new ColorRgb(r, g, b)));
                    } 
                    else {
                      System.err.println("ERROR: in line " + 
                                         st.lineno()+" at "+st.sval);
                      throw new IOException(st.toString());
                    }
                }
                else if ( st.sval.equals("rotation") ) {
                  showDebugMessage("rotation");
                  yaw_actual = readNumber(st);
                  pitch_actual = readNumber(st);
                  roll_actual = readNumber(st);
                }
                else if ( st.sval.equals("surface") ) {
                  showDebugMessage("surface");
                  currentMaterial = readSurfaceDefinition(st);
                }
                else if ( st.sval.equals("texture") ) {
                  String texturePath = readStringToken(st);
                  currentTexture = loadRgbTexture(texturePath, textureCache);
                }
                else if ( st.sval.equals("notexture") ) {
                  currentTexture = null;
                }
                else if ( st.sval.equals("bumpmap") ) {
                  String bumpPath = readStringToken(st);
                  Vector3D bumpScale = new Vector3D(1, 1, 0.2);
                  int nextToken = st.nextToken();
                  if ( nextToken == StreamTokenizer.TT_NUMBER ) {
                      double sx = st.nval;
                      double sy = readNumber(st);
                      double sz = readNumber(st);
                      bumpScale = new Vector3D(sx, sy, sz);
                  }
                  else {
                      st.pushBack();
                  }
                  currentNormalMap =
                      loadNormalMapFromBump(bumpPath, bumpScale, normalMapCache);
                }
                else if ( st.sval.equals("nobumpmap") ) {
                  currentNormalMap = null;
                }
                else {
                  System.err.println("ERROR: unsupported token \"" + st.sval +
                      "\" in line " + st.lineno());
                  throw new IOException(st.toString());
                }
              ;
              break;
            default:
              System.err.println("ERROR: in line "+st.lineno()+" at "+st.sval);
              throw new IOException(st.toString());
          } // switch
        } // while
        is.close();
        if ( st.ttype != StreamTokenizer.TT_EOF ) {
            System.err.println("ERROR: in line "+st.lineno()+" at "+st.sval);
            throw new IOException(st.toString());
        }

        configureCurrentCameraFromMitView(context);

        theScene.addBackground(context.currentBackground);
        theScene.addCamera(context.currentCamera);
        theScene.setActiveCameraIndex(0);
        theScene.setActiveBackgroundIndex(0);
    }
}
