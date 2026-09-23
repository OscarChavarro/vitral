package vsdk.toolkit.render.jogl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.curve.ParametricCurve;
import vsdk.toolkit.environment.geometry.surface.FunctionalExplicitSurface;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.surface.ParametricBiCubicPatch;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.geometry.volume.Arrow;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.geometry.volume.Torus;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.media.RGBImageUncompressed;

/**
Renders the geometries of the toolkit with the GL4 pipeline: each one is
tessellated once into a mesh (cached by its parameters) that is drawn by
`Jogl4MeshRenderer`, so every geometry honors every bit of the
`RendererConfiguration` in the same way (surfaces, wires, points, normals,
bounding volume, texture, bump map and shading).

Supported geometries: `Sphere`, `Cone` (also cylinders and truncated cones),
`Arrow`, `Box`, `Torus`, `TriangleMesh`, and the open surfaces
`FunctionalExplicitSurface`, `ParametricBiCubicPatch` and `InfinitePlane`
(tessellated by their `Jogl4*Renderer`, visible from both sides). A
`ParametricCurve` has no surface: it is drawn as lines by
`Jogl4ParametricCurveRenderer`.
*/
public class Jogl4GeometryRenderer extends Jogl4Renderer {
    private static final int SLICES = 32;
    private static final int TORUS_MAJOR_SEGMENTS = 48;
    private static final int TORUS_MINOR_SEGMENTS = 24;
    private static final int MAX_CACHED_MESHES = 256;

    private static final Map<String, Jogl4MeshRenderer.Mesh> MESHES = new HashMap<>();
    private static final Set<String> REPORTED_UNSUPPORTED = new HashSet<>();

    private Jogl4GeometryRenderer() {
    }

    /**
    @param geometry a geometry
    @return true if this renderer can draw the geometry
    */
    public static boolean isSupported(Geometry geometry)
    {
        return geometry instanceof Sphere || geometry instanceof Cone ||
            geometry instanceof Arrow || geometry instanceof Box ||
            geometry instanceof Torus || geometry instanceof TriangleMesh ||
            geometry instanceof FunctionalExplicitSurface ||
            geometry instanceof ParametricBiCubicPatch ||
            geometry instanceof InfinitePlane ||
            geometry instanceof ParametricCurve;
    }

    /**
    Draws a geometry lit by a single light.
    See the overload taking a list of lights.

    @param gl OpenGL context
    @param geometry geometry to draw
    @param camera camera that views the geometry
    @param light light of the scene, or null to use a light at the camera
    @param material material of the geometry
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from geometry space to world space
    */
    public static void draw(
        GL4 gl,
        Geometry geometry,
        Camera camera,
        Light light,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap,
        Matrix4x4d localTransform)
    {
        List<Light> lights = null;

        if ( light != null ) {
            lights = new ArrayList<Light>();
            lights.add(light);
        }
        draw(gl, geometry, camera, lights, material, quality, textureMap,
            normalMap, localTransform);
    }

    /**
    Draws a geometry.

    @param gl OpenGL context
    @param geometry geometry to draw
    @param camera camera that views the geometry
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the geometry
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from geometry space to world space
    */
    public static void draw(
        GL4 gl,
        Geometry geometry,
        Camera camera,
        List<Light> lights,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap,
        Matrix4x4d localTransform)
    {
        if ( geometry == null ) {
            return;
        }
        if ( geometry instanceof Sphere sphere ) {
            Jogl4SphereRenderer.draw(gl, sphere, camera, lights, material, quality,
                textureMap, normalMap, localTransform, SLICES, SLICES / 2);
            return;
        }
        if ( geometry instanceof ParametricCurve curve ) {
            Jogl4ParametricCurveRenderer.draw(gl, curve, camera, quality, localTransform);
            return;
        }
        Jogl4MeshRenderer.Mesh mesh = obtainMesh(geometry);
        if ( mesh == null ) {
            return;
        }
        if ( geometry instanceof InfinitePlane plane && quality != null ) {
            drawInfinitePlaneMesh(gl, mesh, plane, camera, lights, material, quality,
                textureMap, normalMap, localTransform);
            return;
        }
        Jogl4MeshRenderer.draw(gl, mesh, geometry, camera, lights, material, quality,
            textureMap, normalMap, localTransform);
    }

    /**
    Draws the mesh of the square shown for an infinite plane. Its bounding
    volume is infinite, so the bounding volume and the selection corners are
    drawn around the square instead.
    */
    private static void drawInfinitePlaneMesh(
        GL4 gl,
        Jogl4MeshRenderer.Mesh mesh,
        InfinitePlane plane,
        Camera camera,
        List<Light> lights,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap,
        Matrix4x4d localTransform)
    {
        RendererConfiguration meshQuality = new RendererConfiguration();

        meshQuality.clone(quality);
        meshQuality.setBoundingVolume(false);
        meshQuality.setSelectionCorners(false);
        Jogl4MeshRenderer.draw(gl, mesh, plane, camera, lights, material, meshQuality,
            textureMap, normalMap, localTransform);

        double[] shown = Jogl4InfinitePlaneRenderer.calculateShownMinMax(plane);
        if ( quality.isBoundingVolumeSet() ) {
            Jogl4MinMaxRenderer.draw(gl, shown, camera, localTransform);
        }
        if ( quality.isSelectionCornersSet() ) {
            Jogl4SelectionCornersRenderer.draw(gl, shown, camera, localTransform);
        }
    }

    /**
    Releases the OpenGL resources of the meshes. PRE: the OpenGL context that
    created them is current.
    @param gl OpenGL context
    */
    public static void dispose(GL4 gl)
    {
        releaseMeshes(gl);
        Jogl4SphereRenderer.dispose(gl);
    }

    private static void releaseMeshes(GL4 gl)
    {
        for ( Jogl4MeshRenderer.Mesh mesh : MESHES.values() ) {
            Jogl4MeshRenderer.release(gl, mesh);
        }
        MESHES.clear();
    }

    private static Jogl4MeshRenderer.Mesh obtainMesh(Geometry geometry)
    {
        String key = keyOf(geometry);

        if ( key == null ) {
            String name = geometry.getClass().getSimpleName();
            if ( REPORTED_UNSUPPORTED.add(name) ) {
                Logger.reportMessage(null, VSDK.WARNING, "Jogl4GeometryRenderer",
                    "Geometry not supported by the GL4 pipeline yet: " + name);
            }
            return null;
        }
        Jogl4MeshRenderer.Mesh mesh = MESHES.get(key);

        if ( mesh == null ) {
            mesh = buildMesh(geometry);
            if ( mesh == null ) {
                return null;
            }
            if ( MESHES.size() >= MAX_CACHED_MESHES ) {
                // Stale meshes (i.e. from geometries edited many times) are
                // recreated on demand; their GPU objects are freed with the
                // context
                MESHES.clear();
            }
            MESHES.put(key, mesh);
        }
        return mesh;
    }

    /**
    @return a string that identifies the mesh of the geometry, or null if the
    geometry is not supported. Parametric geometries are identified by their
    parameters; triangle meshes by identity, as they are not edited while
    shown.
    */
    private static String keyOf(Geometry geometry)
    {
        if ( geometry instanceof Cone c ) {
            return "cone/" + c.getBottomRadius() + "/" + c.getTopRadius() + "/" + c.getHeight();
        }
        if ( geometry instanceof Arrow a ) {
            return "arrow/" + a.getBaseLength() + "/" + a.getHeadLength() + "/" +
                a.getBaseRadius() + "/" + a.getHeadRadius();
        }
        if ( geometry instanceof Box b ) {
            Vector3Dd s = b.getSize();
            return "box/" + s.x() + "/" + s.y() + "/" + s.z();
        }
        if ( geometry instanceof Torus t ) {
            return "torus/" + t.getMajorRadius() + "/" + t.getMinorRadius();
        }
        if ( geometry instanceof TriangleMesh ) {
            return "trianglemesh/" + System.identityHashCode(geometry);
        }
        if ( geometry instanceof FunctionalExplicitSurface f ) {
            return Jogl4FunctionalExplicitSurfaceRenderer.meshKey(f);
        }
        if ( geometry instanceof ParametricBiCubicPatch p ) {
            return Jogl4ParametricBiCubicPatchRenderer.meshKey(p);
        }
        if ( geometry instanceof InfinitePlane p ) {
            return Jogl4InfinitePlaneRenderer.meshKey(p);
        }
        return null;
    }

    private static Jogl4MeshRenderer.Mesh buildMesh(Geometry geometry)
    {
        if ( geometry instanceof Cone c ) {
            return buildCone(c);
        }
        if ( geometry instanceof Arrow a ) {
            return buildArrow(a);
        }
        if ( geometry instanceof Box b ) {
            return buildBox(b);
        }
        if ( geometry instanceof Torus t ) {
            return buildTorus(t);
        }
        if ( geometry instanceof TriangleMesh m ) {
            return buildTriangleMesh(m);
        }
        if ( geometry instanceof FunctionalExplicitSurface f ) {
            return Jogl4FunctionalExplicitSurfaceRenderer.buildMesh(f);
        }
        if ( geometry instanceof ParametricBiCubicPatch p ) {
            return Jogl4ParametricBiCubicPatchRenderer.buildMesh(p);
        }
        if ( geometry instanceof InfinitePlane p ) {
            return Jogl4InfinitePlaneRenderer.buildMesh(p);
        }
        return null;
    }

    private static Jogl4MeshRenderer.Mesh buildCone(Cone cone)
    {
        double r1 = cone.getBottomRadius();
        double r2 = cone.getTopRadius();
        double h = cone.getHeight();
        Jogl4MeshBuilder builder = new Jogl4MeshBuilder(Math.max(Math.max(r1, r2), h));

        builder.addFrustum(0, r1, h, r2, SLICES);
        builder.addDisk(0, 0, r1, false, SLICES);
        if ( r2 > 0.0 ) {
            builder.addDisk(h, 0, r2, true, SLICES);
        }
        return builder.build();
    }

    private static Jogl4MeshRenderer.Mesh buildArrow(Arrow arrow)
    {
        double h1 = arrow.getBaseLength();
        double h2 = arrow.getHeadLength();
        double r1 = arrow.getBaseRadius();
        double r2 = arrow.getHeadRadius();
        Jogl4MeshBuilder builder = new Jogl4MeshBuilder(h1 + h2);

        builder.addFrustum(0, r1, h1, r1, SLICES);
        builder.addDisk(0, 0, r1, false, SLICES);
        builder.addFrustum(h1, r2, h1 + h2, 0, SLICES);
        builder.addDisk(h1, r1, r2, false, SLICES);
        return builder.build();
    }

    private static Jogl4MeshRenderer.Mesh buildBox(Box box)
    {
        Vector3Dd size = box.getSize();
        double hx = size.x() / 2;
        double hy = size.y() / 2;
        double hz = size.z() / 2;
        Jogl4MeshBuilder builder = new Jogl4MeshBuilder(Math.max(hx, Math.max(hy, hz)) * 2);

        // Each face is given counterclockwise seen from outside
        addBoxFace(builder, new Vector3Dd(0, 0, -1),
            new Vector3Dd(-hx, -hy, -hz), new Vector3Dd(-hx, hy, -hz),
            new Vector3Dd(hx, hy, -hz), new Vector3Dd(hx, -hy, -hz));
        addBoxFace(builder, new Vector3Dd(0, 0, 1),
            new Vector3Dd(-hx, -hy, hz), new Vector3Dd(hx, -hy, hz),
            new Vector3Dd(hx, hy, hz), new Vector3Dd(-hx, hy, hz));
        addBoxFace(builder, new Vector3Dd(0, -1, 0),
            new Vector3Dd(-hx, -hy, hz), new Vector3Dd(-hx, -hy, -hz),
            new Vector3Dd(hx, -hy, -hz), new Vector3Dd(hx, -hy, hz));
        addBoxFace(builder, new Vector3Dd(-1, 0, 0),
            new Vector3Dd(-hx, hy, hz), new Vector3Dd(-hx, hy, -hz),
            new Vector3Dd(-hx, -hy, -hz), new Vector3Dd(-hx, -hy, hz));
        addBoxFace(builder, new Vector3Dd(0, 1, 0),
            new Vector3Dd(hx, hy, hz), new Vector3Dd(hx, hy, -hz),
            new Vector3Dd(-hx, hy, -hz), new Vector3Dd(-hx, hy, hz));
        addBoxFace(builder, new Vector3Dd(1, 0, 0),
            new Vector3Dd(hx, -hy, hz), new Vector3Dd(hx, -hy, -hz),
            new Vector3Dd(hx, hy, -hz), new Vector3Dd(hx, hy, hz));
        return builder.build();
    }

    /**
    Adds a face whose vertices are given in the order upper left, lower left,
    lower right, upper right as seen from outside (the Jogl2 texture layout),
    and which is wound so its front side looks outside.
    */
    private static void addBoxFace(
        Jogl4MeshBuilder builder,
        Vector3Dd n,
        Vector3Dd a,
        Vector3Dd b,
        Vector3Dd c,
        Vector3Dd d)
    {
        Vector3Dd winding = b.subtract(a).crossProduct(c.subtract(b));

        if ( winding.dotProduct(n) >= 0 ) {
            builder.addQuad(a, n, 0, 1, b, n, 0, 0, c, n, 1, 0, d, n, 1, 1);
        }
        else {
            builder.addQuad(d, n, 1, 1, c, n, 1, 0, b, n, 0, 0, a, n, 0, 1);
        }
    }

    private static Jogl4MeshRenderer.Mesh buildTorus(Torus torus)
    {
        double bigR = torus.getMajorRadius();
        double smallR = torus.getMinorRadius();
        Jogl4MeshBuilder builder = new Jogl4MeshBuilder(bigR + smallR);
        int nw = TORUS_MAJOR_SEGMENTS;
        int nv = TORUS_MINOR_SEGMENTS;

        for ( int i = 0; i < nw; i++ ) {
            double w0 = 2 * Math.PI * i / nw;
            double w1 = 2 * Math.PI * (i + 1) / nw;

            for ( int j = 0; j < nv; j++ ) {
                double v0 = 2 * Math.PI * j / nv;
                double v1 = 2 * Math.PI * (j + 1) / nv;
                Vector3Dd p00 = torusPoint(bigR, smallR, w0, v0);
                Vector3Dd p01 = torusPoint(bigR, smallR, w0, v1);
                Vector3Dd p11 = torusPoint(bigR, smallR, w1, v1);
                Vector3Dd p10 = torusPoint(bigR, smallR, w1, v0);
                Vector3Dd n00 = torusNormal(w0, v0);
                Vector3Dd n01 = torusNormal(w0, v1);
                Vector3Dd n11 = torusNormal(w1, v1);
                Vector3Dd n10 = torusNormal(w1, v0);

                builder.addQuad(
                    p00, n00, (double)i / nw, (double)j / nv,
                    p10, n10, (double)(i + 1) / nw, (double)j / nv,
                    p11, n11, (double)(i + 1) / nw, (double)(j + 1) / nv,
                    p01, n01, (double)i / nw, (double)(j + 1) / nv);
            }
        }
        return builder.build();
    }

    private static Vector3Dd torusPoint(double bigR, double smallR, double w, double v)
    {
        return new Vector3Dd(
            (bigR + smallR * Math.cos(v)) * Math.cos(w),
            (bigR + smallR * Math.cos(v)) * Math.sin(w),
            smallR * Math.sin(v));
    }

    private static Vector3Dd torusNormal(double w, double v)
    {
        return new Vector3Dd(Math.cos(v) * Math.cos(w), Math.cos(v) * Math.sin(w), Math.sin(v));
    }

    private static Jogl4MeshRenderer.Mesh buildTriangleMesh(TriangleMesh mesh)
    {
        double[] p = mesh.getVertexPositions();
        double[] n = mesh.getVertexNormals();
        double[] uv = mesh.getVertexUvs();
        int[] idx = mesh.getTriangleIndexes();

        if ( p == null || idx == null ) {
            return null;
        }
        double[] minmax = mesh.getMinMax();
        double size = 1.0;
        if ( minmax != null && minmax.length >= 6 ) {
            size = Math.max(Math.abs(minmax[3] - minmax[0]),
                Math.max(Math.abs(minmax[4] - minmax[1]), Math.abs(minmax[5] - minmax[2])));
        }
        Jogl4MeshBuilder builder = new Jogl4MeshBuilder(size);

        for ( int t = 0; t + 2 < idx.length; t += 3 ) {
            Vector3Dd[] pos = new Vector3Dd[3];
            Vector3Dd[] nor = new Vector3Dd[3];
            double[] u = new double[3];
            double[] v = new double[3];

            for ( int k = 0; k < 3; k++ ) {
                int i = idx[t + k];
                pos[k] = new Vector3Dd(p[3 * i], p[3 * i + 1], p[3 * i + 2]);
                nor[k] = (n != null && n.length >= 3 * i + 3)
                    ? new Vector3Dd(n[3 * i], n[3 * i + 1], n[3 * i + 2])
                    : new Vector3Dd(0, 0, 1);
                if ( uv != null && uv.length >= 2 * i + 2 ) {
                    u[k] = uv[2 * i];
                    v[k] = uv[2 * i + 1];
                }
            }
            builder.addTriangle(pos[0], nor[0], u[0], v[0], pos[1], nor[1], u[1], v[1],
                pos[2], nor[2], u[2], v[2]);
        }
        return builder.build();
    }
}
