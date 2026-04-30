package vsdk.toolkit.render.jogl;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;

public class Jogl4CameraRenderer extends Jogl4Renderer {
    private static final float[] CAMERA_ORIGIN_COLOR = { 1.0f, 0.5f, 0.5f };
    private static final float[] FRUSTUM_COLOR = { 0.0f, 1.0f, 1.0f };

    public static Matrix4x4 activate(GL4 gl, Camera cam)
    {
        return cam.calculateProjectionMatrix();
    }

    public static Matrix4x4 activateCenter(GL4 gl, Camera cam)
    {
        Camera camera2 = new Camera(cam);
        camera2.setPosition(new Vector3D(0, 0, 0));
        camera2.setNearPlaneDistance(0.1);
        camera2.setFarPlaneDistance(10.0);
        return camera2.calculateProjectionMatrix();
    }

    public static void draw(GL4 gl, Camera cam)
    {
        Matrix4x4 projection = cam.calculateProjectionMatrix();
        draw(gl, cam, projection);
    }

    public static void draw(GL4 gl, Camera cam, Matrix4x4 projection)
    {
        Matrix4x4 model = cam.getRotation().withTranslation(cam.getPosition());
        Matrix4x4 modelViewProjection = projection.multiply(model);
        drawVolume(gl, cam, modelViewProjection);
    }

    public static void drawVolume(GL4 gl, Camera cam, Matrix4x4 mvp)
    {
        double npd = cam.getNearPlaneDistance();
        double fpd = cam.getFarPlaneDistance();

        double xn;
        double yn;
        double xf;
        double yf;

        if ( cam.getProjectionMode() != Camera.PROJECTION_MODE_ORTHOGONAL ) {
            yn = 2 * npd * Math.tan(Math.toRadians(cam.getFov()) / 2);
            xn = yn * (cam.getViewportXSize() / cam.getViewportYSize());
        }
        else {
            if ( cam.getViewportXSize() > cam.getViewportYSize() ) {
                xn = 1;
                yn = cam.getViewportYSize() / cam.getViewportXSize();
            }
            else {
                xn = cam.getViewportXSize() / cam.getViewportYSize();
                yn = 1;
            }
        }

        xf = xn;
        yf = yn;
        if ( cam.getProjectionMode() != Camera.PROJECTION_MODE_ORTHOGONAL ) {
            yf = 2 * fpd * Math.tan(Math.toRadians(cam.getFov()) / 2);
            xf = yf * (cam.getViewportXSize() / cam.getViewportYSize());
        }

        List<Float> originPositions = new ArrayList<>();
        List<Float> originColors = new ArrayList<>();

        double delta = 0.1;
        addLine(originPositions, originColors, 0, 0, 0, delta, 0, 0, CAMERA_ORIGIN_COLOR);
        addLine(originPositions, originColors, 0, -delta, 0, 0, delta, 0, CAMERA_ORIGIN_COLOR);
        addLine(originPositions, originColors, 0, 0, -delta, 0, 0, delta, CAMERA_ORIGIN_COLOR);

        List<Float> frustumPositions = new ArrayList<>();
        List<Float> frustumColors = new ArrayList<>();

        addLoopRectangle(frustumPositions, frustumColors, npd, xn, yn, FRUSTUM_COLOR);
        addLine(frustumPositions, frustumColors, npd, -xn/10, -yn/10, npd, xn/10, yn/10, FRUSTUM_COLOR);
        addLine(frustumPositions, frustumColors, npd, xn/10, -yn/10, npd, -xn/10, yn/10, FRUSTUM_COLOR);
        addLine(frustumPositions, frustumColors, npd, -xn/10, yn/2, npd, 0, yn/2 + yn/10, FRUSTUM_COLOR);
        addLine(frustumPositions, frustumColors, npd, 0, yn/2 + yn/10, npd, xn/10, yn/2, FRUSTUM_COLOR);

        addLoopRectangle(frustumPositions, frustumColors, fpd, xf, yf, FRUSTUM_COLOR);
        addLine(frustumPositions, frustumColors, fpd, -xf/10, -yf/10, fpd, xf/10, yf/10, FRUSTUM_COLOR);
        addLine(frustumPositions, frustumColors, fpd, xf/10, -yf/10, fpd, -xf/10, yf/10, FRUSTUM_COLOR);
        addLine(frustumPositions, frustumColors, fpd, -xf/10, yf/2, fpd, 0, yf/2 + yf/10, FRUSTUM_COLOR);
        addLine(frustumPositions, frustumColors, fpd, 0, yf/2 + yf/10, fpd, xf/10, yf/2, FRUSTUM_COLOR);

        addLine(frustumPositions, frustumColors, npd, -xn/2, -yn/2, fpd, -xf/2, -yf/2, FRUSTUM_COLOR);
        addLine(frustumPositions, frustumColors, npd, xn/2, -yn/2, fpd, xf/2, -yf/2, FRUSTUM_COLOR);
        addLine(frustumPositions, frustumColors, npd, xn/2, yn/2, fpd, xf/2, yf/2, FRUSTUM_COLOR);
        addLine(frustumPositions, frustumColors, npd, -xn/2, yn/2, fpd, -xf/2, yf/2, FRUSTUM_COLOR);

        Jogl4LineRenderer.drawLines(gl, mvp, toArray(originPositions), toArray(originColors), 1.0f);
        Jogl4LineRenderer.drawLines(gl, mvp, toArray(frustumPositions), toArray(frustumColors), 2.0f);
    }

    public static void dispose(GL4 gl)
    {
        Jogl4LineRenderer.release(gl);
    }

    private static void addLoopRectangle(
        List<Float> positions,
        List<Float> colors,
        double x,
        double w,
        double h,
        float[] rgb)
    {
        addLine(positions, colors, x, -w/2, -h/2, x, w/2, -h/2, rgb);
        addLine(positions, colors, x, w/2, -h/2, x, w/2, h/2, rgb);
        addLine(positions, colors, x, w/2, h/2, x, -w/2, h/2, rgb);
        addLine(positions, colors, x, -w/2, h/2, x, -w/2, -h/2, rgb);
    }

    private static void addLine(
        List<Float> positions,
        List<Float> colors,
        double x1,
        double y1,
        double z1,
        double x2,
        double y2,
        double z2,
        float[] rgb)
    {
        addVertex(positions, colors, x1, y1, z1, rgb);
        addVertex(positions, colors, x2, y2, z2, rgb);
    }

    private static void addVertex(
        List<Float> positions,
        List<Float> colors,
        double x,
        double y,
        double z,
        float[] rgb)
    {
        positions.add((float)x);
        positions.add((float)y);
        positions.add((float)z);

        colors.add(rgb[0]);
        colors.add(rgb[1]);
        colors.add(rgb[2]);
    }

    private static float[] toArray(List<Float> input)
    {
        float[] out = new float[input.size()];
        for ( int i = 0; i < input.size(); i++ ) {
            out[i] = input.get(i);
        }
        return out;
    }
}
