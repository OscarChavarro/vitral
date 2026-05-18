package vsdk.toolkit.render.jogl;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.light.Light;

public class Jogl4LightRenderer extends Jogl4Renderer
{
    private static double scale = 1.0;

    public static void activate(GL2 gl, Light light)
    {
        Jogl2LightRenderer.activate(gl, light);
    }

    public static void draw(GL2 gl, Light light)
    {
        draw(gl, light, null);
    }

    public static void draw(GL2 gl, Light light, Camera camera)
    {
        if ( gl == null ) {
            return;
        }
        draw(gl.getGL4(), light, camera);
    }

    public static void draw(GL4 gl, Light light, Camera camera)
    {
        if ( gl == null || light == null ) {
            return;
        }

        GL4 gl4 = gl;
        int[] viewport = new int[4];
        gl4.glGetIntegerv(GL4.GL_VIEWPORT, viewport, 0);

        int viewportWidth = Math.max(viewport[2], 1);
        int viewportHeight = Math.max(viewport[3], 1);

        Matrix4x4d modelViewProjection = Matrix4x4d.identityMatrix();
        if ( camera != null ) {
            camera.updateViewportResize(viewportWidth, viewportHeight);
            modelViewProjection = camera.calculateProjectionMatrix();
        }

        double halfAxisLength = calculateHalfAxisLength(light, camera,
            viewportWidth, viewportHeight) * scale;

        Vector3Dd p = light.getPosition();
        ColorRgb c = light.getSpecular();

        float px = (float)p.x();
        float py = (float)p.y();
        float pz = (float)p.z();
        float d = (float)halfAxisLength;

        float[] positions = new float[] {
            px - d, py, pz,
            px + d, py, pz,

            px, py - d, pz,
            px, py + d, pz,

            px, py, pz - d,
            px, py, pz + d
        };

        float cr = (float)c.r();
        float cg = (float)c.g();
        float cb = (float)c.b();
        float[] colors = new float[] {
            cr, cg, cb,
            cr, cg, cb,
            cr, cg, cb,
            cr, cg, cb,
            cr, cg, cb,
            cr, cg, cb
        };

        Jogl4LineRenderer.drawLines(gl4, modelViewProjection, positions, colors,
            2.0f);
    }

    public static double getScale()
    {
        return scale;
    }

    public static void setScale(double newScale)
    {
        scale = newScale;
    }

    private static double calculateHalfAxisLength(Light light, Camera camera,
                                                   int viewportWidth,
                                                   int viewportHeight)
    {
        final double viewportFraction = 0.05;
        final double targetPixels = viewportFraction
            * Math.min(viewportWidth, viewportHeight);

        if ( camera == null ) {
            return Math.max(0.05, targetPixels / Math.max(viewportHeight, 1));
        }

        if ( camera.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL ) {
            double worldViewHeight = 2.0 / camera.getOrthogonalZoom();
            double worldPerPixel = worldViewHeight / Math.max(viewportHeight, 1);
            return Math.max(1e-5, 0.5 * targetPixels * worldPerPixel);
        }

        Vector3Dd toLight = light.getPosition().subtract(camera.getPosition());
        double depth = Math.abs(toLight.dotProduct(camera.getFront()));
        depth = Math.max(depth, camera.getNearPlaneDistance());

        double fovRadians = Math.toRadians(camera.getFov());
        double worldViewHeightAtDepth = 2.0 * depth * Math.tan(fovRadians / 2.0);
        double worldPerPixel = worldViewHeightAtDepth / Math.max(viewportHeight, 1);
        return Math.max(1e-5, 0.5 * targetPixels * worldPerPixel);
    }
}
