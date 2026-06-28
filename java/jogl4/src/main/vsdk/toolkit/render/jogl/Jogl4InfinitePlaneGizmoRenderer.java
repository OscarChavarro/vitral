package vsdk.toolkit.render.jogl;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.gui.gizmo.InfinitePlaneGizmo;

public class Jogl4InfinitePlaneGizmoRenderer extends Jogl4Renderer {
    private static final double VIEWPORT_AREA_FRACTION = 0.125;
    private static final double MIN_PROJECTED_COSINE = 0.10;
    private static final float LINE_WIDTH = 2.0f;

    public static void draw(GL4 gl, InfinitePlaneGizmo gizmo, Camera camera) {
        if ( gl == null || gizmo == null || camera == null || !gizmo.isVisible() ) {
            return;
        }

        Vector3Dd center = gizmo.getPoint();
        Vector3Dd normal = gizmo.getNormal();
        if ( center == null || normal == null || normal.length() < VSDK.EPSILON ) {
            return;
        }

        camera.updateVectors();
        Vector3Dd n = normal.normalized();
        Vector3Dd u = buildTangent(n, camera);
        Vector3Dd v = n.crossProduct(u).normalized();
        double halfSide = calculateHalfSide(camera, center, n);

        Vector3Dd p0 = center.add(u.multiply(-halfSide)).add(v.multiply(-halfSide));
        Vector3Dd p1 = center.add(u.multiply( halfSide)).add(v.multiply(-halfSide));
        Vector3Dd p2 = center.add(u.multiply( halfSide)).add(v.multiply( halfSide));
        Vector3Dd p3 = center.add(u.multiply(-halfSide)).add(v.multiply( halfSide));

        ColorRgb color = gizmo.getFrameColor();
        if ( color == null ) {
            color = InfinitePlaneGizmo.DEFAULT_FRAME_COLOR;
        }

        float[] positions = new float[24];
        float[] colors = new float[24];
        addLine(positions, colors, 0, p0, p1, color);
        addLine(positions, colors, 6, p1, p2, color);
        addLine(positions, colors, 12, p2, p3, color);
        addLine(positions, colors, 18, p3, p0, color);

        Matrix4x4d projection = Jogl4CameraRenderer.activate(gl, camera);
        Jogl4LineRenderer.drawLines(gl, projection, positions, colors, LINE_WIDTH);
    }

    public static void dispose(GL4 gl) {
        Jogl4LineRenderer.release(gl);
    }

    private static Vector3Dd buildTangent(Vector3Dd normal, Camera camera) {
        Vector3Dd tangent = normal.crossProduct(camera.getFront());
        if ( tangent.length() < VSDK.EPSILON ) {
            tangent = normal.crossProduct(camera.getUp());
        }
        if ( tangent.length() < VSDK.EPSILON ) {
            tangent = normal.crossProduct(camera.getLeft());
        }
        if ( tangent.length() < VSDK.EPSILON ) {
            tangent = new Vector3Dd(1, 0, 0);
        }
        return tangent.normalized();
    }

    private static double calculateHalfSide(Camera camera, Vector3Dd center, Vector3Dd normal) {
        double aspect = Math.max(1.0e-6, camera.getViewportXSize() / camera.getViewportYSize());
        double visibleHeight;
        double visibleWidth;

        if ( camera.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL ) {
            visibleHeight = 2.0 / Math.max(camera.getOrthogonalZoom(), 1.0e-6);
        }
        else {
            double depth = center.subtract(camera.getPosition()).dotProduct(camera.getFront());
            depth = Math.max(camera.getNearPlaneDistance(), Math.abs(depth));
            visibleHeight = 2.0 * depth * Math.tan(Math.toRadians(camera.getFov()) / 2.0);
        }
        visibleWidth = visibleHeight * aspect;

        double projectedCosine = Math.abs(normal.normalized().dotProduct(camera.getFront().normalized()));
        projectedCosine = Math.max(MIN_PROJECTED_COSINE, projectedCosine);
        double side = Math.sqrt(visibleWidth * visibleHeight * VIEWPORT_AREA_FRACTION / projectedCosine);
        return side * 0.5;
    }

    private static void addLine(
        float[] positions,
        float[] colors,
        int offset,
        Vector3Dd a,
        Vector3Dd b,
        ColorRgb color)
    {
        addVertex(positions, colors, offset, a, color);
        addVertex(positions, colors, offset + 3, b, color);
    }

    private static void addVertex(
        float[] positions,
        float[] colors,
        int offset,
        Vector3Dd p,
        ColorRgb color)
    {
        positions[offset] = (float)p.x();
        positions[offset + 1] = (float)p.y();
        positions[offset + 2] = (float)p.z();
        colors[offset] = (float)color.r();
        colors[offset + 1] = (float)color.g();
        colors[offset + 2] = (float)color.b();
    }
}
