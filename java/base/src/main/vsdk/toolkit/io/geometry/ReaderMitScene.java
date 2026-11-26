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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

// VSDK classes
import vsdk.toolkit.common.VSDKFatalException;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector2Dd;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.logging.Logger;
import vsdk.toolkit.environment.background.CubemapBackground;
import vsdk.toolkit.environment.background.SimpleBackground;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.SimpleTestGeometryLibrary;
import vsdk.toolkit.environment.geometry.surface.BezierPatchSubdivision;
import vsdk.toolkit.environment.geometry.surface.SubdividedBezierPatch;
import vsdk.toolkit.environment.geometry.volume.BlobComponent;
import vsdk.toolkit.environment.geometry.volume.BlobSolid;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.geometry.volume.HeightFieldSolid;
import vsdk.toolkit.environment.geometry.volume.PlanarHalfSpace;
import vsdk.toolkit.environment.geometry.volume.PolynomialSurface;
import vsdk.toolkit.environment.geometry.volume.Prism;
import vsdk.toolkit.environment.geometry.volume.QuadricSurface;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.geometry.volume.Torus;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.light.AmbientLight;
import vsdk.toolkit.environment.light.DirectionalLight;
import vsdk.toolkit.environment.light.PointLight;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.scene.AffinePlacementStatus;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.environment.scene.csg.CsgBody;
import vsdk.toolkit.environment.scene.csg.CsgBodyEvaluator;
import vsdk.toolkit.environment.scene.csg.CsgOperation;
import vsdk.toolkit.io.PersistenceElement;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.IndexedColorImageUncompressed;
import vsdk.toolkit.media.NormalMap;
import vsdk.toolkit.media.RGBAImageUncompressed;
import vsdk.toolkit.media.RGBImageUncompressed;

/**
Reader of the MIT 6.837 scene format as extended by VITRAL. Relative resource
files (textures, bump maps, altimetry, OBJ models, cubemap faces) are looked
up from the scene file's directory first and then from the working directory.
Textures and normal maps are scene resources shared by the bodies using them.

This class implements an scene reader based on the instructional raytracer
from Tomas Lozano-Perez from computer graphics class at MIT on spring 2001,
and from Leonard McMillan 1998. This material was adapted by Oscar Chavarro
for computer graphics classes at Colombia, and later as anoter scene reader
for Vitral.

The parsing follows the cpp reader line by line: each line is read as C
`fgets` with a 1024 byte buffer would, and its fields are scanned with the C
`sscanf` semantics of `_CScanf`.
*/
public class ReaderMitScene extends PersistenceElement
{
    private String sceneDirectory;
    // Euler angles of the last `rotation` line, applied to each later primitive.
    private double yaw;
    private double pitch;
    private double roll;
    private Image currentTexture;
    private NormalMap currentNormalMap;
    private final ArrayList<String> texturePaths = new ArrayList<String>();
    private final ArrayList<Image> textures = new ArrayList<Image>();
    private final ArrayList<String> normalMapKeys = new ArrayList<String>();
    private final ArrayList<NormalMap> normalMaps = new ArrayList<NormalMap>();

    public ReaderMitScene()
    {
        yaw = 0;
        pitch = 0;
        roll = 0;
        currentTexture = null;
        currentNormalMap = null;
        sceneDirectory = "";
    }

    private void resetState(String fileName)
    {
        int slash = fileName.lastIndexOf('/');
        sceneDirectory = slash < 0 ? "" : fileName.substring(0, slash + 1);
        yaw = 0;
        pitch = 0;
        roll = 0;
        currentTexture = null;
        currentNormalMap = null;
        texturePaths.clear();
        textures.clear();
        normalMapKeys.clear();
        normalMaps.clear();
    }

    private boolean hasRotation()
    {
        return yaw != 0 || pitch != 0 || roll != 0;
    }

    /** cpp `fopen(path, "rb")` succeeding. */
    private static boolean canOpen(String path)
    {
        return new File(path).canRead();
    }

    /** Resolved path of a relative resource, or the path as given when missing. */
    private String resolveResource(String path)
    {
        if ( path.isEmpty() || path.charAt(0) == '/' || sceneDirectory.isEmpty() ) return path;
        final String fromScene = sceneDirectory + path;
        if ( canOpen(fromScene) ) {
            return fromScene;
        }
        return path;
    }

    private Image loadTexture(String path, SimpleScene scene)
    {
        for ( int i = 0; i < texturePaths.size(); ++i )
            if ( texturePaths.get(i).equals(path) ) return textures.get(i);
        RGBImageUncompressed texture;
        try {
            texture = ImagePersistence.importRGB(new File(resolveResource(path)));
        }
        catch ( Exception e ) {
            texture = null;
        }
        // Unreadable files come back as empty images.
        if ( texture != null && (texture.getXSize() <= 0 || texture.getYSize() <= 0) ) {
            texture = null;
        }
        if ( texture == null ) return null;
        scene.addImage(texture);
        texturePaths.add(path);
        textures.add(texture);
        return texture;
    }

    private NormalMap loadNormalMap(String path, Vector3Dd scale, SimpleScene scene)
    {
        // The cpp key formats the scale with "%.17g"; any exact (round trip)
        // formatting identifies the same scales.
        final String key = path + "|" + Double.toString(scale.x()) + "|" +
            Double.toString(scale.y()) + "|" + Double.toString(scale.z());
        for ( int i = 0; i < normalMapKeys.size(); ++i )
            if ( normalMapKeys.get(i).equals(key) ) return normalMaps.get(i);
        IndexedColorImageUncompressed bumpMap;
        try {
            bumpMap = ImagePersistence.importIndexedColor(new File(resolveResource(path)));
        }
        catch ( Exception e ) {
            bumpMap = null;
        }
        if ( bumpMap != null && (bumpMap.getXSize() <= 0 || bumpMap.getYSize() <= 0) ) {
            bumpMap = null;
        }
        if ( bumpMap == null ) return null;
        NormalMap normalMap = new NormalMap();
        normalMap.importBumpMap(bumpMap, scale);
        scene.addNormalMap(normalMap);
        normalMapKeys.add(key);
        normalMaps.add(normalMap);
        return normalMap;
    }

    /** Applies the current rotation about the body's origin. */
    private void applyRotation(SimpleBody body)
    {
        if ( !hasRotation() ) return;
        final Matrix4x4d rotation = new Matrix4x4d().eulerAnglesRotation(yaw, pitch, roll);
        body.setRotation(rotation);
        body.setRotationInverse(rotation.inverse());
    }

    /** Applies the current rotation, texture and normal map. */
    private void dressBody(SimpleBody body)
    {
        applyRotation(body);
        if ( currentTexture != null ) body.setTextureReference(currentTexture);
        if ( currentNormalMap != null ) body.setNormalMapReference(currentNormalMap);
    }

    /**
    Moves the bodies of an OBJ file into the scene, placed by the current
    rotation, the translation and a uniform scale; false when unreadable.
    */
    private boolean importObj(String path, Vector3Dd translation,
        double uniformScale, SimpleMaterial fallbackMaterial, SimpleScene scene) throws Exception
    {
        final String resolved = resolveResource(path);
        if ( !canOpen(resolved) ) return false;
        SimpleScene imported = new SimpleScene();
        EnvironmentPersistence.importEnvironment(new File(resolved), imported);
        ArrayList<SimpleBody> bodies = imported.getSimpleBodies();
        if ( bodies.isEmpty() ) return false;
        final Matrix4x4d sceneRotation = new Matrix4x4d().eulerAnglesRotation(yaw, pitch, roll);
        for ( int i = 0; i < bodies.size(); ++i ) {
            SimpleBody body = bodies.get(i);
            if ( body.getMaterial() == null ) body.setMaterial(new SimpleMaterial(fallbackMaterial));
            final Matrix4x4d rotation = sceneRotation.multiply(body.getRotation());
            final Vector3Dd scale = body.getScale().multiply(uniformScale);
            final Vector3Dd position =
                sceneRotation.multiply(body.getPosition().multiply(uniformScale)).add(translation);
            body.setRotation(rotation);
            body.setRotationInverse(rotation.inverse());
            body.setScale(scale);
            body.setPosition(position);
            scene.addBody(body);
        }
        // The bodies now belong to the target scene.
        bodies.clear();
        return true;
    }

    private static String trimLine(String in)
    {
        int a = 0;
        while ( a < in.length() && (in.charAt(a) == ' ' || in.charAt(a) == '\t' ||
                in.charAt(a) == '\r' || in.charAt(a) == '\n') ) a++;
        int b = in.length();
        while ( b > a && (in.charAt(b-1) == ' ' || in.charAt(b-1) == '\t' ||
                in.charAt(b-1) == '\r' || in.charAt(b-1) == '\n') ) b--;
        return in.substring(a, b);
    }

    /** Strips the quotes around a file name. */
    private static String unquote(String text)
    {
        String value = text;
        if ( value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length()-1) == '"' )
            value = value.substring(1, value.length() - 1);
        return value;
    }

    private static void fail(String message)
    {
        throw new VSDKFatalException(message);
    }

    /**
    The successive results of C `fgets(buffer, 1024, file)`: lines up to and
    including their newline, split every 1023 bytes.
    */
    private static ArrayList<String> fgetsLines(InputStream is) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int count;
        while ( (count = is.read(chunk)) > 0 ) bytes.write(chunk, 0, count);
        // Bytes map one to one to characters, as the cpp reader sees them.
        final String text = new String(bytes.toByteArray(), StandardCharsets.ISO_8859_1);
        ArrayList<String> lines = new ArrayList<String>();
        int start = 0;
        while ( start < text.length() ) {
            int end = start;
            while ( end < text.length() && end - start < 1023 ) {
                end++;
                if ( text.charAt(end - 1) == '\n' ) break;
            }
            lines.add(text.substring(start, end));
            start = end;
        }
        return lines;
    }

    /** Reads the scene file; relative resources are resolved from its directory. */
    public void
    importEnvironment(String fileName, SimpleScene outScene) throws Exception
    {
        if ( outScene == null ) {
            Logger.reportMessage(this, Logger.ERROR, "importEnvironment", "outScene is null");
            throw new VSDKFatalException("ReaderMitScene::importEnvironment: outScene is null");
        }
        ArrayList<String> lines;
        try ( InputStream is = new FileInputStream(fileName) ) {
            lines = fgetsLines(is);
        }
        catch ( IOException e ) {
            Logger.reportMessage(this, Logger.ERROR, "importEnvironment", "could not open file");
            throw new VSDKFatalException("ReaderMitScene::importEnvironment: could not open file");
        }
        importLines(fileName, lines, outScene);
    }

    /**
    Java only: reads a scene from a stream. Without a file name, relative
    resources are resolved from the working directory.
    */
    public void
    importEnvironment(InputStream is, SimpleScene outScene) throws Exception
    {
        if ( outScene == null ) {
            Logger.reportMessage(this, Logger.ERROR, "importEnvironment", "outScene is null");
            throw new VSDKFatalException("ReaderMitScene::importEnvironment: outScene is null");
        }
        ArrayList<String> lines = fgetsLines(is);
        is.close();
        importLines("", lines, outScene);
    }

    private void importLines(String fileName, ArrayList<String> lines, SimpleScene outScene)
        throws Exception
    {
        outScene.clearOwnedElements();
        resetState(fileName);

        Camera camera =
            new Camera();
        SimpleBackground background = new SimpleBackground();
        outScene.addCamera(camera);
        outScene.addBackground(background);
        outScene.setActiveCameraIndex(0);
        outScene.setActiveBackgroundIndex(0);

        Vector3Dd eye = new Vector3Dd(0, 0, 10);
        Vector3Dd up = new Vector3Dd(0, 1, 0);
        Vector3Dd lookat = camera.getFocusedPosition();
        boolean hasEye = false;
        boolean hasUp = false;
        boolean hasLookat = false;
        boolean hasViewport = false;
        int viewportXSize = 320;
        int viewportYSize = 240;
        double importedHorizontalFov = 30.0;

        SimpleMaterial currentMaterial = new SimpleMaterial();
        ReaderMitTriangleGroup triangleGroup = new ReaderMitTriangleGroup();
        boolean inTriangles = false;
        ReaderMitQuadGroup quadGroup = new ReaderMitQuadGroup();
        boolean inQuads = false;
        // Open CSG body and its node stack; primitives inside become its leaves.
        CsgBody csgBody = null;
        ArrayList<Long> csgNodes = new ArrayList<Long>();
        // Open polynomial or blob block.
        PolynomialSurface polynomial = null;
        BlobSolid blob = null;
        // Open patch block: its subdivision settings and control points so far.
        boolean inPatch = false;
        BezierPatchSubdivision patchSubdivision = BezierPatchSubdivision.UniformGrid;
        int patchUSteps = 0;
        int patchVSteps = 0;
        double patchFlatness = 0;
        int patchPointCount = 0;
        Vector3Dd[][] patchPoints = new Vector3Dd[4][4];
        for ( int i = 0; i < 4; i++ ) for ( int j = 0; j < 4; j++ ) patchPoints[i][j] = new Vector3Dd();
        // Pending placement of the next primitive or outermost csg block.
        PendingPlacement pending = new PendingPlacement();

        for ( String lineBuf : lines ) {
            // Remove comment
            int sharp = lineBuf.indexOf('#');
            String javaLine = sharp >= 0 ? lineBuf.substring(0, sharp) : lineBuf;
            javaLine = trimLine(javaLine);
            if ( javaLine.isEmpty() ) {
                continue;
            }

            String[] cmdStr = {""};
            _CScanf.sscanf(javaLine, "%255s", (Object)cmdStr);
            String cmd = cmdStr[0];

            if ( polynomial != null && !cmd.equals("term") && !cmd.equals("end") )
                fail("ReaderMitScene: poly blocks only hold term lines");
            if ( blob != null && !cmd.equals("component") && !cmd.equals("end") )
                fail("ReaderMitScene: blob blocks only hold component lines");
            if ( inPatch && !cmd.equals("control") && !cmd.equals("end") )
                fail("ReaderMitScene: patch blocks only hold control lines");
            if ( inTriangles && !cmd.equals("v") && !cmd.equals("f") && !cmd.equals("surface") && !cmd.equals("end") )
                fail("ReaderMitScene: triangles blocks only hold v, f and surface lines");
            if ( inQuads && !cmd.equals("v") && !cmd.equals("q") && !cmd.equals("surface") && !cmd.equals("end") )
                fail("ReaderMitScene: quads blocks only hold v, q and surface lines");
            if ( cmd.equals("transform") ) {
                double[][] m = new double[12][1];
                if ( _CScanf.sscanf(javaLine, "%*s %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf",
                        m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7], m[8], m[9],
                        m[10], m[11]) != 12 ) fail("ReaderMitScene: transform requires twelve numbers");
                for ( int i = 0; i < 12; ++i )
                    if ( !Double.isFinite(m[i][0]) ) fail("ReaderMitScene: transform requires finite numbers");
                if ( pending.hasPlacement ) fail("ReaderMitScene: transform must be followed by a primitive");
                final double[][] rows = {{m[0][0], m[1][0], m[2][0], m[3][0]}, {m[4][0], m[5][0], m[6][0], m[7][0]},
                    {m[8][0], m[9][0], m[10][0], m[11][0]}, {0, 0, 0, 1}};
                pending.placement = new Matrix4x4d(rows);
                pending.hasPlacement = true;
            }
            else if ( cmd.equals("poly") ) {
                int[] order = {0};
                String[] option = {""};
                final int fields = _CScanf.sscanf(javaLine, "%*s %d %255s", order, option);
                if ( fields < 1 || !PolynomialSurface.isValidOrder(order[0]) ||
                    (fields == 2 && !option[0].equals("sturm")) )
                    fail("ReaderMitScene: poly requires an order from 2 to 7 and optionally sturm");
                polynomial = new PolynomialSurface(order[0], fields == 2);
            }
            else if ( cmd.equals("term") && polynomial != null ) {
                double[] coefficient = {0};
                int[] x = {0}, y = {0}, z = {0};
                if ( _CScanf.sscanf(javaLine, "%*s %lf %d %d %d", coefficient, x, y, z) != 4 ||
                    !Double.isFinite(coefficient[0]) )
                    fail("ReaderMitScene: term requires a coefficient and three powers");
                final int index = PolynomialSurface.getTermIndex(polynomial.getOrder(), x[0], y[0], z[0]);
                if ( index < 0 ) fail("ReaderMitScene: term powers exceed the polynomial order");
                polynomial.getCoefficients()[index] += coefficient[0];
            }
            else if ( cmd.equals("end") && polynomial != null ) {
                SimpleBody body = new SimpleBody();
                body.setGeometry(polynomial);
                polynomial = null;
                body.setMaterial(new SimpleMaterial(currentMaterial));
                attach(body, pending, csgBody, csgNodes, outScene);
            }
            else if ( cmd.equals("blob") ) {
                double[] threshold = {0};
                String[] option = {""};
                final int fields = _CScanf.sscanf(javaLine, "%*s %lf %255s", threshold, option);
                if ( fields < 1 || !Double.isFinite(threshold[0]) ||
                    (fields == 2 && !option[0].equals("sturm")) )
                    fail("ReaderMitScene: blob requires a threshold and optionally sturm");
                blob = new BlobSolid(threshold[0], fields == 2);
            }
            else if ( cmd.equals("component") && blob != null ) {
                double[] strength = {0}, radius = {0}, x = {0}, y = {0}, z = {0};
                if ( _CScanf.sscanf(javaLine, "%*s %lf %lf %lf %lf %lf", strength, radius, x, y, z) != 5 )
                    fail("ReaderMitScene: component requires strength, radius and center");
                final BlobComponent component = new BlobComponent(new Vector3Dd(x[0], y[0], z[0]), radius[0], strength[0]);
                if ( component.isDegenerate() ) fail("ReaderMitScene: degenerate blob component");
                blob.addComponent(component);
            }
            else if ( cmd.equals("end") && blob != null ) {
                if ( blob.getComponentCount() == 0 ) fail("ReaderMitScene: blob requires a component");
                SimpleBody body = new SimpleBody();
                body.setGeometry(blob);
                blob = null;
                body.setMaterial(new SimpleMaterial(currentMaterial));
                attach(body, pending, csgBody, csgNodes, outScene);
            }
            else if ( cmd.equals("patch") ) {
                if ( csgBody != null ) fail("ReaderMitScene: patches are open surfaces and cannot be csg operands");
                String[] method = {""};
                String[] option = {""};
                int[] uSteps = {0}, vSteps = {0};
                double[] flatnessValue = {0.1};
                final int fields = _CScanf.sscanf(javaLine, "%*s %255s %d %d %255s %lf", method, uSteps,
                    vSteps, option, flatnessValue);
                final String name = method[0];
                if ( name.equals("uniform") ) patchSubdivision = BezierPatchSubdivision.UniformGrid;
                else if ( name.equals("grid") ) patchSubdivision = BezierPatchSubdivision.PrecomputedGrid;
                else if ( name.equals("adaptive") ) patchSubdivision = BezierPatchSubdivision.Adaptive;
                else if ( name.equals("tree") ) patchSubdivision = BezierPatchSubdivision.AdaptiveTree;
                else if ( name.equals("smooth") ) patchSubdivision = BezierPatchSubdivision.SmoothGrid;
                else fail("ReaderMitScene: patch requires uniform, grid, adaptive, tree or smooth");
                if ( !(fields == 3 || (fields == 5 && option[0].equals("flatness") &&
                        Double.isFinite(flatnessValue[0]) && flatnessValue[0] >= 0)) ||
                    uSteps[0] < 1 || vSteps[0] < 1 || uSteps[0] > 64 || vSteps[0] > 64 )
                    fail("ReaderMitScene: patch requires steps from 1 to 64 and optionally flatness <value>");
                inPatch = true;
                patchUSteps = uSteps[0];
                patchVSteps = vSteps[0];
                patchFlatness = flatnessValue[0];
                patchPointCount = 0;
            }
            else if ( cmd.equals("control") && inPatch ) {
                double[] x = {0}, y = {0}, z = {0};
                if ( _CScanf.sscanf(javaLine, "%*s %lf %lf %lf", x, y, z) != 3 ||
                    !Double.isFinite(x[0]) || !Double.isFinite(y[0]) || !Double.isFinite(z[0]) )
                    fail("ReaderMitScene: control requires three finite coordinates");
                if ( patchPointCount >= 16 ) fail("ReaderMitScene: patch takes 16 control points");
                patchPoints[patchPointCount / 4][patchPointCount % 4] = new Vector3Dd(x[0], y[0], z[0]);
                patchPointCount++;
            }
            else if ( cmd.equals("end") && inPatch ) {
                if ( patchPointCount != 16 ) fail("ReaderMitScene: patch takes 16 control points");
                inPatch = false;
                SimpleBody body = new SimpleBody();
                body.setGeometry(new SubdividedBezierPatch(patchSubdivision, patchUSteps, patchVSteps,
                    patchFlatness, patchPoints));
                body.setMaterial(new SimpleMaterial(currentMaterial));
                attach(body, pending, csgBody, csgNodes, outScene);
            }
            else if ( cmd.equals("heightfield") ) {
                String[] path = {""};
                String[] option = {""};
                double[] water = {0};
                final int fields = _CScanf.sscanf(javaLine, "%*s %511s %255s %lf", path, option, water);
                if ( fields != 1 && !(fields == 3 && option[0].equals("water") && Double.isFinite(water[0])) )
                    fail("ReaderMitScene: heightfield requires a file and optionally water <level>");
                // Relative files are resolved from the scene file's directory.
                String resolved = path[0];
                final int slash = fileName.lastIndexOf('/');
                if ( !path[0].startsWith("/") && slash >= 0 )
                    resolved = fileName.substring(0, slash + 1) + resolved;
                HeightFieldSolid heightField = null;
                try {
                    heightField = ReaderAltimetry.importHeightField(resolved);
                }
                catch ( VSDKFatalException e ) {
                    fail("ReaderMitScene: could not read the heightfield altimetry file");
                }
                if ( fields == 3 ) {
                    Vector3Dd minimum = heightField.getMinimum().withY(water[0]);
                    Vector3Dd maximum = heightField.getMaximum();
                    if ( !(water[0] < maximum.y()) ) {
                        fail("ReaderMitScene: heightfield water level must lie below its highest point");
                    }
                    heightField.setBounds(minimum, maximum);
                    heightField.updateBlocks();
                }
                SimpleBody body = new SimpleBody();
                body.setGeometry(heightField);
                body.setMaterial(new SimpleMaterial(currentMaterial));
                attach(body, pending, csgBody, csgNodes, outScene);
            }
            else if ( cmd.equals("triangles") ) {
                if ( csgBody != null ) fail("ReaderMitScene: triangles cannot be csg operands");
                if ( pending.hasPlacement ) fail("ReaderMitScene: triangles cannot be transformed");
                if ( inTriangles || inQuads ) fail("ReaderMitScene: nested mesh block");
                triangleGroup.reset();
                inTriangles = true;
            }
            else if ( cmd.equals("v") && inTriangles ) {
                double[] x = {0}, y = {0}, z = {0};
                if ( _CScanf.sscanf(javaLine, "%*s %lf %lf %lf", x, y, z) != 3 ||
                    !triangleGroup.addVertex(new Vector3Dd(x[0], y[0], z[0])) ) {
                    throw new VSDKFatalException("ReaderMitScene: invalid triangle vertex");
                }
            }
            else if ( cmd.equals("f") && inTriangles ) {
                if ( !triangleGroup.addFace(javaLine.substring(1)) ) {
                    throw new VSDKFatalException("ReaderMitScene: invalid triangle face");
                }
            }
            else if ( cmd.equals("end") && inTriangles ) {
                SimpleBody mesh = triangleGroup.flush(outScene, currentMaterial);
                if ( mesh != null ) applyRotation(mesh);
                triangleGroup.reset();
                inTriangles = false;
            }
            else if ( cmd.equals("csg") ) {
                String[] operationStr = {""};
                String[] evaluatorStr = {""};
                final int fields = _CScanf.sscanf(javaLine, "%*s %255s %255s", operationStr, evaluatorStr);
                String operationName = operationStr[0];
                String evaluatorName = evaluatorStr[0];
                CsgOperation operation = CsgOperation.UNION;
                if ( operationName.equals("union") ) operation = CsgOperation.UNION;
                else if ( operationName.equals("intersection") ) operation = CsgOperation.INTERSECTION;
                else if ( operationName.equals("difference") ) operation = CsgOperation.DIFFERENCE;
                else fail("ReaderMitScene: csg requires union, intersection or difference");
                if ( csgBody == null ) {
                    CsgBodyEvaluator evaluator = CsgBodyEvaluator.ROTH;
                    if ( fields == 2 && evaluatorName.equals("pov") ) evaluator = CsgBodyEvaluator.CROSSING_MEMBERSHIP;
                    else if ( fields == 2 && !evaluatorName.equals("roth") ) fail("ReaderMitScene: csg evaluator must be roth or pov");
                    csgBody = new CsgBody(operation, evaluator);
                    csgBody.setMaterial(new SimpleMaterial(currentMaterial));
                    if ( !place(csgBody, pending) ) fail("ReaderMitScene: transform is not an invertible affine map");
                    csgNodes.add(0L);
                }
                else {
                    if ( fields == 2 ) fail("ReaderMitScene: only the outermost csg selects an evaluator");
                    if ( pending.hasPlacement ) fail("ReaderMitScene: nested csg blocks cannot be transformed");
                    final long node = csgBody.addNode(csgNodes.get(csgNodes.size()-1), operation);
                    if ( node < 0 ) fail("ReaderMitScene: invalid csg node");
                    csgNodes.add(node);
                }
            }
            else if ( cmd.equals("end") && csgBody != null ) {
                csgNodes.remove(csgNodes.size()-1);
                if ( csgNodes.size() == 0 ) {
                    outScene.addBody(csgBody);
                    csgBody = null;
                }
            }
            else if ( cmd.equals("viewport") ) {
                int[] w = {0}, h = {0};
                if ( _CScanf.sscanf(javaLine, "%*s %d %d", w, h) == 2 ) {
                    viewportXSize = w[0];
                    viewportYSize = h[0];
                    hasViewport = true;
                }
            }
            else if ( cmd.equals("eye") ) {
                double[] x = {0}, y = {0}, z = {0};
                if ( _CScanf.sscanf(javaLine, "%*s %lf %lf %lf", x, y, z) == 3 ) {
                    eye = new Vector3Dd(x[0], y[0], z[0]);
                    hasEye = true;
                }
            }
            else if ( cmd.equals("up") ) {
                double[] x = {0}, y = {0}, z = {0};
                if ( _CScanf.sscanf(javaLine, "%*s %lf %lf %lf", x, y, z) == 3 ) {
                    up = new Vector3Dd(x[0], y[0], z[0]);
                    hasUp = true;
                }
            }
            else if ( cmd.equals("lookat") ) {
                double[] x = {0}, y = {0}, z = {0};
                if ( _CScanf.sscanf(javaLine, "%*s %lf %lf %lf", x, y, z) == 3 ) {
                    lookat = new Vector3Dd(x[0], y[0], z[0]);
                    hasLookat = true;
                }
            }
            else if ( cmd.equals("fov") ) {
                double[] fov = {0};
                if ( _CScanf.sscanf(javaLine, "%*s %lf", (Object)fov) == 1 ) {
                    importedHorizontalFov = fov[0];
                }
            }
            else if ( cmd.equals("background") ) {
                double[] r = {0}, g = {0}, b = {0};
                if ( _CScanf.sscanf(javaLine, "%*s %lf %lf %lf", r, g, b) == 3 ) {
                    background.setColor(r[0], g[0], b[0]);
                    outScene.setActiveBackgroundIndex(0);
                }
            }
            else if ( cmd.equals("surface") ) {
                if ( inTriangles ) {
                    SimpleBody mesh = triangleGroup.flush(outScene, currentMaterial);
                    if ( mesh != null ) applyRotation(mesh);
                }
                if ( inQuads ) {
                    SimpleBody mesh = quadGroup.flush(outScene, currentMaterial);
                    if ( mesh != null ) applyRotation(mesh);
                }
                double[] r = {0}, g = {0}, b = {0};
                double[] ka = {0}, kd = {0}, ks = {0};
                double[] ns = {0};
                double[] kr = {0}, kt = {0}, index = {0};
                if ( _CScanf.sscanf(javaLine, "%*s %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf",
                            r, g, b, ka, kd, ks, ns, kr, kt, index) == 10 ) {
                    currentMaterial = new SimpleMaterial(
                        "surface",
                        new ColorRgb(r[0] * ka[0], g[0] * ka[0], b[0] * ka[0]),
                        new ColorRgb(r[0] * kd[0], g[0] * kd[0], b[0] * kd[0]),
                        new ColorRgb(ks[0], ks[0], ks[0]),
                        new ColorRgb(0.0, 0.0, 0.0),
                        new ColorRgb(0.0, 0.0, 0.0),
                        true,
                        kr[0],
                        kt[0],
                        index[0],
                        1.0,
                        ns[0]);
                }
            }
            else if ( cmd.equals("box") ) {
                double[] xmin = {0}, ymin = {0}, zmin = {0}, xmax = {0}, ymax = {0}, zmax = {0};
                if ( _CScanf.sscanf(javaLine, "%*s %lf %lf %lf %lf %lf %lf",
                        xmin, ymin, zmin, xmax, ymax, zmax) != 6 ||
                    !Double.isFinite(xmin[0]) || !Double.isFinite(ymin[0]) || !Double.isFinite(zmin[0]) ||
                    !Double.isFinite(xmax[0]) || !Double.isFinite(ymax[0]) || !Double.isFinite(zmax[0]) ||
                    xmin[0] >= xmax[0] || ymin[0] >= ymax[0] || zmin[0] >= zmax[0] ) {
                    throw new VSDKFatalException("ReaderMitScene: box requires ordered minimum and maximum corners");
                }
                SimpleBody b = new SimpleBody();
                b.setGeometry(new Box(new Vector3Dd(xmin[0], ymin[0], zmin[0]), new Vector3Dd(xmax[0], ymax[0], zmax[0])));
                b.setMaterial(new SimpleMaterial(currentMaterial));
                attach(b, pending, csgBody, csgNodes, outScene);
            }
            else if ( cmd.equals("halfspace") ) {
                double[] a = {0}, b = {0}, c = {0}, d = {0};
                if ( _CScanf.sscanf(javaLine, "%*s %lf %lf %lf %lf", a, b, c, d) != 4 ||
                    !Double.isFinite(a[0]) || !Double.isFinite(b[0]) ||
                    !Double.isFinite(c[0]) || !Double.isFinite(d[0]) ||
                    (a[0] == 0 && b[0] == 0 && c[0] == 0) ) {
                    throw new VSDKFatalException("ReaderMitScene: halfspace requires a nonzero normal and constant");
                }
                SimpleBody body = new SimpleBody();
                body.setGeometry(new PlanarHalfSpace(a[0], b[0], c[0], d[0]));
                body.setMaterial(new SimpleMaterial(currentMaterial));
                attach(body, pending, csgBody, csgNodes, outScene);
            }
            else if ( cmd.equals("quadric") ) {
                double[] xx = {0}, yy = {0}, zz = {0}, xy = {0}, xz = {0}, yz = {0}, x = {0}, y = {0}, z = {0}, c = {0};
                if ( _CScanf.sscanf(javaLine, "%*s %lf %lf %lf %lf %lf %lf %lf %lf %lf %lf",
                        xx, yy, zz, xy, xz, yz, x, y, z, c) != 10 ) {
                    throw new VSDKFatalException("ReaderMitScene: quadric requires ten coefficients");
                }
                SimpleBody b = new SimpleBody();
                b.setGeometry(new QuadricSurface(new Vector3Dd(xx[0], yy[0], zz[0]), new Vector3Dd(xy[0], xz[0], yz[0]),
                    new Vector3Dd(x[0], y[0], z[0]), c[0]));
                b.setMaterial(new SimpleMaterial(currentMaterial));
                attach(b, pending, csgBody, csgNodes, outScene);
            }
            else if ( cmd.equals("sphere") ) {
                double[] x = {0}, y = {0}, z = {0}, r = {0};
                if ( _CScanf.sscanf(javaLine, "%*s %lf %lf %lf %lf", x, y, z, r) == 4 ) {
                    SimpleBody b = new SimpleBody();
                    b.setGeometry(new Sphere(r[0]));
                    b.setPosition(new Vector3Dd(x[0], y[0], z[0]));
                    b.setMaterial(new SimpleMaterial(currentMaterial));
                    attach(b, pending, csgBody, csgNodes, outScene);
                }
                else fail("ReaderMitScene: sphere requires a center and a radius");
            }
            else if ( cmd.equals("light") ) {
                double[] r = {0}, g = {0}, b = {0};
                String[] typeStr = {""};
                if ( _CScanf.sscanf(javaLine, "%*s %lf %lf %lf %255s", r, g, b, typeStr) == 4 ) {
                    String type = typeStr[0];
                    if ( type.equals("ambient") ) {
                        outScene.addLight(new AmbientLight(new ColorRgb(r[0], g[0], b[0])));
                    }
                    else if ( type.equals("point") ) {
                        double[] x = {0}, y = {0}, z = {0};
                        if ( _CScanf.sscanf(javaLine, "%*s %*f %*f %*f %*s %lf %lf %lf", x, y, z) == 3 ) {
                            outScene.addLight(new PointLight(new Vector3Dd(x[0], y[0], z[0]), new ColorRgb(r[0], g[0], b[0])));
                        }
                    }
                    else if ( type.equals("directional") ) {
                        double[] x = {0}, y = {0}, z = {0};
                        if ( _CScanf.sscanf(javaLine, "%*s %*f %*f %*f %*s %lf %lf %lf", x, y, z) == 3 ) {
                            outScene.addLight(new DirectionalLight(new Vector3Dd(x[0], y[0], z[0]), new ColorRgb(r[0], g[0], b[0])));
                        }
                        else fail("ReaderMitScene: directional light requires a direction");
                    }
                    else fail("ReaderMitScene: light type must be ambient, point or directional");
                }
                else fail("ReaderMitScene: light requires a color and a type");
            }
            else if ( cmd.equals("cube") ) {
                double[] x = {0}, y = {0}, z = {0}, size = {0};
                if ( _CScanf.sscanf(javaLine, "%*s %lf %lf %lf %lf", x, y, z, size) != 4 ||
                    !Double.isFinite(size[0]) || size[0] <= 0 )
                    fail("ReaderMitScene: cube requires a center and a positive size");
                SimpleBody body = new SimpleBody();
                body.setGeometry(new Box(size[0], size[0], size[0]));
                body.setPosition(new Vector3Dd(x[0], y[0], z[0]));
                body.setMaterial(new SimpleMaterial(currentMaterial));
                attach(body, pending, csgBody, csgNodes, outScene);
            }
            else if ( cmd.equals("cylinder") ) {
                double[] x = {0}, y = {0}, z = {0}, bottomRadius = {0}, topRadius = {0}, height = {0};
                if ( _CScanf.sscanf(javaLine, "%*s %lf %lf %lf %lf %lf %lf",
                        x, y, z, bottomRadius, topRadius, height) != 6 ||
                    !(bottomRadius[0] >= 0) || !(topRadius[0] >= 0) || !(height[0] > 0) ||
                    (bottomRadius[0] == 0 && topRadius[0] == 0) )
                    fail("ReaderMitScene: cylinder requires a base center, two radii and a positive height");
                if ( csgBody != null ) fail("ReaderMitScene: cylinder cannot be a csg operand");
                SimpleBody body = new SimpleBody();
                body.setGeometry(new Cone(bottomRadius[0], topRadius[0], height[0]));
                body.setPosition(new Vector3Dd(x[0], y[0], z[0]));
                body.setMaterial(new SimpleMaterial(currentMaterial));
                attach(body, pending, csgBody, csgNodes, outScene);
            }
            else if ( cmd.equals("torus") ) {
                double[] x = {0}, y = {0}, z = {0}, majorRadius = {0}, minorRadius = {0};
                if ( _CScanf.sscanf(javaLine, "%*s %lf %lf %lf %lf %lf",
                        x, y, z, majorRadius, minorRadius) != 5 ||
                    !(majorRadius[0] > 0) || !(minorRadius[0] > 0) )
                    fail("ReaderMitScene: torus requires a center and two positive radii");
                if ( csgBody != null ) fail("ReaderMitScene: torus cannot be a csg operand");
                SimpleBody body = new SimpleBody();
                body.setGeometry(new Torus(majorRadius[0], minorRadius[0]));
                body.setPosition(new Vector3Dd(x[0], y[0], z[0]));
                body.setMaterial(new SimpleMaterial(currentMaterial));
                attach(body, pending, csgBody, csgNodes, outScene);
            }
            else if ( cmd.equals("prism") ) {
                // prism x y z height n x1 y1 ... xn yn: a simple polygon of n
                // vertices on the plane z = 0 of the body, placed at (x, y, z),
                // swept height units along +Z. It is a parametric solid, so it
                // can be a csg operand.
                int cursor = 0;
                while ( cursor < javaLine.length() && javaLine.charAt(cursor) != ' ' &&
                        javaLine.charAt(cursor) != '\t' ) cursor++;
                double[] values = new double[5];
                boolean ok = true;
                for ( int i = 0; i < 5 && ok; i++ ) {
                    double[] value = {0};
                    final int next = _CScanf.strtod(javaLine, cursor, value);
                    values[i] = value[0];
                    ok = next != cursor && Double.isFinite(values[i]);
                    cursor = next;
                }
                final int n = ok && values[4] >= 3 && values[4] <= 100000 ? (int)values[4] : 0;
                ok = ok && n >= 3 && values[4] == (double)n && values[3] > 0;
                ArrayList<Vector2Dd> polygon = new ArrayList<Vector2Dd>();
                for ( int i = 0; i < 2 * n && ok; i += 2 ) {
                    double[] value = {0};
                    int next = _CScanf.strtod(javaLine, cursor, value);
                    final double px = value[0];
                    ok = next != cursor && Double.isFinite(px);
                    cursor = next;
                    if ( !ok ) break;
                    next = _CScanf.strtod(javaLine, cursor, value);
                    final double py = value[0];
                    ok = next != cursor && Double.isFinite(py);
                    cursor = next;
                    polygon.add(new Vector2Dd(px, py));
                }
                while ( ok && cursor < javaLine.length() &&
                        (javaLine.charAt(cursor) == ' ' || javaLine.charAt(cursor) == '\t') ) cursor++;
                ok = ok && cursor == javaLine.length();
                if ( !ok ) fail("ReaderMitScene: prism requires a base position, a positive height, a vertex count of at least 3 and that many x y pairs");
                Prism prism = new Prism(polygon, values[3]);
                if ( !prism.hasValidParameters() ) {
                    fail("ReaderMitScene: prism base must be a simple polygon with non-zero area");
                }
                SimpleBody body = new SimpleBody();
                body.setGeometry(prism);
                body.setPosition(new Vector3Dd(values[0], values[1], values[2]));
                body.setMaterial(new SimpleMaterial(currentMaterial));
                attach(body, pending, csgBody, csgNodes, outScene);
            }
            else if ( cmd.equals("polybox") || cmd.equals("appel") ) {
                if ( csgBody != null ) fail("ReaderMitScene: polyhedral solids cannot be csg operands");
                double[] x = {0}, y = {0}, z = {0};
                double[] scale = {1.0};
                PolyhedralBoundedSolid solid = null;
                if ( cmd.equals("polybox") ) {
                    double[] sx = {0}, sy = {0}, sz = {0};
                    if ( _CScanf.sscanf(javaLine, "%*s %lf %lf %lf %lf %lf %lf",
                            x, y, z, sx, sy, sz) != 6 || !(sx[0] > 0) || !(sy[0] > 0) || !(sz[0] > 0) )
                        fail("ReaderMitScene: polybox requires a center and three positive sizes");
                    Box box = new Box(sx[0], sy[0], sz[0]);
                    solid = box.exportToPolyhedralBoundedSolid();
                }
                else {
                    int[] id = {0};
                    final int fields = _CScanf.sscanf(javaLine, "%*s %d %lf %lf %lf %lf",
                        id, x, y, z, scale);
                    if ( fields < 4 || (fields == 5 && !(scale[0] > 0)) || id[0] < 1 || id[0] > 3 )
                        fail("ReaderMitScene: appel requires an object 1 to 3, a center and optionally a positive scale");
                    if ( id[0] == 1 ) solid = SimpleTestGeometryLibrary.createTestObjectAPPE1967_1();
                    else if ( id[0] == 2 ) solid = SimpleTestGeometryLibrary.createTestObjectAPPE1967_2();
                    else solid = SimpleTestGeometryLibrary.createTestObjectAPPE1967_3();
                    // The APPEL objects are modelled in [0,1]^3; center them on the origin.
                    PolyhedralBoundedSolidModeler.applyTransformation(solid,
                        new Matrix4x4d().translation(-0.5, -0.5, -0.5));
                }
                if ( solid == null ) fail("ReaderMitScene: could not build the polyhedral solid");
                SimpleBody body = new SimpleBody();
                body.setGeometry(solid);
                body.setPosition(new Vector3Dd(x[0], y[0], z[0]));
                body.setScale(new Vector3Dd(scale[0], scale[0], scale[0]));
                body.setMaterial(new SimpleMaterial(currentMaterial));
                attach(body, pending, csgBody, csgNodes, outScene);
            }
            else if ( cmd.equals("obj") ) {
                String[] path = {""};
                double[] x = {0}, y = {0}, z = {0};
                double[] scale = {1.0};
                final int fields = _CScanf.sscanf(javaLine, "%*s %511s %lf %lf %lf %lf",
                    path, x, y, z, scale);
                if ( fields < 4 || (fields == 5 && !(scale[0] > 0)) )
                    fail("ReaderMitScene: obj requires a file, a position and optionally a positive scale");
                if ( csgBody != null || pending.hasPlacement ) fail("ReaderMitScene: obj models cannot be csg operands or transformed");
                if ( !importObj(unquote(path[0]), new Vector3Dd(x[0], y[0], z[0]), scale[0], currentMaterial, outScene) )
                    fail("ReaderMitScene: could not read the obj file");
            }
            else if ( cmd.equals("quads") ) {
                if ( csgBody != null ) fail("ReaderMitScene: quads cannot be csg operands");
                if ( pending.hasPlacement ) fail("ReaderMitScene: quads cannot be transformed");
                if ( inQuads || inTriangles ) fail("ReaderMitScene: nested mesh block");
                quadGroup.reset();
                inQuads = true;
            }
            else if ( cmd.equals("v") && inQuads ) {
                double[] x = {0}, y = {0}, z = {0};
                if ( _CScanf.sscanf(javaLine, "%*s %lf %lf %lf", x, y, z) != 3 ||
                    !quadGroup.addVertex(new Vector3Dd(x[0], y[0], z[0])) )
                    fail("ReaderMitScene: invalid quad vertex");
            }
            else if ( cmd.equals("q") && inQuads ) {
                if ( !quadGroup.addQuad(javaLine.substring(1)) ) fail("ReaderMitScene: invalid quad face");
            }
            else if ( cmd.equals("end") && inQuads ) {
                SimpleBody mesh = quadGroup.flush(outScene, currentMaterial);
                if ( mesh != null ) applyRotation(mesh);
                quadGroup.reset();
                inQuads = false;
            }
            else if ( cmd.equals("rotation") ) {
                double[] a = {0}, b = {0}, c = {0};
                if ( _CScanf.sscanf(javaLine, "%*s %lf %lf %lf", a, b, c) != 3 ||
                    !Double.isFinite(a[0]) || !Double.isFinite(b[0]) || !Double.isFinite(c[0]) )
                    fail("ReaderMitScene: rotation requires three finite angles");
                yaw = a[0];
                pitch = b[0];
                roll = c[0];
            }
            else if ( cmd.equals("texture") ) {
                String[] path = {""};
                if ( _CScanf.sscanf(javaLine, "%*s %511s", (Object)path) != 1 )
                    fail("ReaderMitScene: texture requires a file");
                currentTexture = loadTexture(unquote(path[0]), outScene);
                if ( currentTexture == null ) fail("ReaderMitScene: could not read the texture file");
            }
            else if ( cmd.equals("notexture") ) {
                currentTexture = null;
            }
            else if ( cmd.equals("bumpmap") ) {
                String[] path = {""};
                double[] sx = {1}, sy = {1}, sz = {0.2};
                final int fields = _CScanf.sscanf(javaLine, "%*s %511s %lf %lf %lf", path, sx, sy, sz);
                if ( fields != 1 && fields != 4 )
                    fail("ReaderMitScene: bumpmap requires a file and optionally three scale factors");
                currentNormalMap = loadNormalMap(unquote(path[0]), new Vector3Dd(sx[0], sy[0], sz[0]), outScene);
                if ( currentNormalMap == null ) fail("ReaderMitScene: could not read the bump map file");
            }
            else if ( cmd.equals("nobumpmap") ) {
                currentNormalMap = null;
            }
            else if ( cmd.equals("backgroundcubemap") ) {
                String[] directoryStr = {""};
                String directory = "../../cubemaps/dorise1";
                if ( _CScanf.sscanf(javaLine, "%*s %511s", (Object)directoryStr) == 1 ) directory = unquote(directoryStr[0]);
                RGBAImageUncompressed[] faces = new RGBAImageUncompressed[6];
                for ( int i = 0; i < 6; ++i ) {
                    String name = "/entorno" + i + ".jpg";
                    try {
                        faces[i] = ImagePersistence.importRGBA(new File(resolveResource(directory + name)));
                    }
                    catch ( Exception e ) {
                        faces[i] = null;
                    }
                    if ( faces[i] != null && (faces[i].getXSize() <= 0 || faces[i].getYSize() <= 0) ) {
                        faces[i] = null;
                    }
                    if ( faces[i] == null ) {
                        fail("ReaderMitScene: could not read the cubemap faces");
                    }
                }
                for ( int i = 0; i < 6; ++i ) outScene.addImage(faces[i]);
                outScene.addBackground(new CubemapBackground(camera,
                    faces[0], faces[1], faces[2], faces[3], faces[4], faces[5]));
                outScene.setActiveBackgroundIndex(outScene.getBackgrounds().size() - 1);
            }
            else fail("ReaderMitScene: unsupported command");
        }

        if ( polynomial != null ) fail("ReaderMitScene: unterminated poly block");
        if ( blob != null ) fail("ReaderMitScene: unterminated blob block");
        if ( inPatch ) fail("ReaderMitScene: unterminated patch block");
        if ( pending.hasPlacement ) fail("ReaderMitScene: transform must be followed by a primitive");
        if ( csgBody != null ) fail("ReaderMitScene: unterminated csg block");
        if ( inTriangles || inQuads ) {
            throw new VSDKFatalException("ReaderMitScene: unterminated mesh block");
        }

        if ( hasEye ) {
            camera.setPosition(eye);
        }
        camera.setUpDirect(up);
        if ( hasLookat ) {
            camera.setFocusedPositionMaintainingOrthogonality(lookat);
        }

        if ( viewportXSize > 0 && viewportYSize > 0 ) {
            double aspect = ((double)viewportXSize) / ((double)viewportYSize);
            double horizontalHalfAngle = importedHorizontalFov * Math.PI / 360.0;
            double verticalHalfAngle = Math.atan(Math.tan(horizontalHalfAngle) / aspect);
            double verticalFov = (verticalHalfAngle * 360.0) / Math.PI;
            camera.setFov(verticalFov);
        }
        if ( hasViewport ) {
            camera.updateViewportResize(viewportXSize, viewportYSize);
        }
        else {
            camera.updateVectors();
        }
    }

    /** The cpp `hasPlacement` and `placement` locals, shared with `place`. */
    private static class PendingPlacement {
        boolean hasPlacement = false;
        Matrix4x4d placement = new Matrix4x4d();
    }

    // cpp lambda `place`.
    private static boolean place(SimpleBody body, PendingPlacement pending)
    {
        if ( !pending.hasPlacement ) return true;
        pending.hasPlacement = false;
        final Matrix4x4d forward = pending.placement.multiply(body.getAffinePlacement());
        return body.setAffinePlacement(forward, forward.inverse()) == AffinePlacementStatus.Valid;
    }

    // cpp lambda `attach`: every primitive takes the current rotation,
    // texture and normal map.
    private void attach(SimpleBody body, PendingPlacement pending, CsgBody csgBody,
        ArrayList<Long> csgNodes, SimpleScene outScene)
    {
        dressBody(body);
        if ( !place(body, pending) ) { fail("ReaderMitScene: transform is not an invertible affine map"); }
        if ( csgBody == null ) { outScene.addBody(body); return; }
        if ( !csgBody.addLeaf(csgNodes.get(csgNodes.size()-1), body) )
            fail("ReaderMitScene: invalid csg operand");
    }
}
