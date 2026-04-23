package vsdk.toolkit.render.jogl;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;

public class Jogl4CameraRenderer extends Jogl4Renderer {

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

    private static void drawVolume(GL4 gl, Camera cam, Matrix4x4 mvp)
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

        List<Float> positions = new ArrayList<>();
        List<Float> colors = new ArrayList<>();

        double delta = 0.1;
        addLine(positions, colors, 0, 0, 0, delta, 0, 0, 1f, 0.5f, 0.5f);
        addLine(positions, colors, 0, -delta, 0, 0, delta, 0, 1f, 0.5f, 0.5f);
        addLine(positions, colors, 0, 0, -delta, 0, 0, delta, 1f, 0.5f, 0.5f);

        addLoopRectangle(positions, colors, npd, xn, yn, 0f, 1f, 1f);
        addLine(positions, colors, npd, -xn/10, -yn/10, npd, xn/10, yn/10, 0f, 1f, 1f);
        addLine(positions, colors, npd, xn/10, -yn/10, npd, -xn/10, yn/10, 0f, 1f, 1f);
        addLine(positions, colors, npd, -xn/10, yn/2, npd, 0, yn/2 + yn/10, 0f, 1f, 1f);
        addLine(positions, colors, npd, 0, yn/2 + yn/10, npd, xn/10, yn/2, 0f, 1f, 1f);

        addLoopRectangle(positions, colors, fpd, xf, yf, 0f, 1f, 1f);
        addLine(positions, colors, fpd, -xf/10, -yf/10, fpd, xf/10, yf/10, 0f, 1f, 1f);
        addLine(positions, colors, fpd, xf/10, -yf/10, fpd, -xf/10, yf/10, 0f, 1f, 1f);
        addLine(positions, colors, fpd, -xf/10, yf/2, fpd, 0, yf/2 + yf/10, 0f, 1f, 1f);
        addLine(positions, colors, fpd, 0, yf/2 + yf/10, fpd, xf/10, yf/2, 0f, 1f, 1f);

        addLine(positions, colors, npd, -xn/2, -yn/2, fpd, -xf/2, -yf/2, 0f, 1f, 1f);
        addLine(positions, colors, npd, xn/2, -yn/2, fpd, xf/2, -yf/2, 0f, 1f, 1f);
        addLine(positions, colors, npd, xn/2, yn/2, fpd, xf/2, yf/2, 0f, 1f, 1f);
        addLine(positions, colors, npd, -xn/2, yn/2, fpd, -xf/2, yf/2, 0f, 1f, 1f);

        float[] pos = toArray(positions);
        float[] col = toArray(colors);
        Jogl4LineRenderer.drawLines(gl, mvp, pos, col, 2.0f);
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
        float r,
        float g,
        float b)
    {
        addLine(positions, colors, x, -w/2, -h/2, x, w/2, -h/2, r, g, b);
        addLine(positions, colors, x, w/2, -h/2, x, w/2, h/2, r, g, b);
        addLine(positions, colors, x, w/2, h/2, x, -w/2, h/2, r, g, b);
        addLine(positions, colors, x, -w/2, h/2, x, -w/2, -h/2, r, g, b);
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
        float r,
        float g,
        float b)
    {
        addVertex(positions, colors, x1, y1, z1, r, g, b);
        addVertex(positions, colors, x2, y2, z2, r, g, b);
    }

    private static void addVertex(
        List<Float> positions,
        List<Float> colors,
        double x,
        double y,
        double z,
        float r,
        float g,
        float b)
    {
        positions.add((float)x);
        positions.add((float)y);
        positions.add((float)z);

        colors.add(r);
        colors.add(g);
        colors.add(b);
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
