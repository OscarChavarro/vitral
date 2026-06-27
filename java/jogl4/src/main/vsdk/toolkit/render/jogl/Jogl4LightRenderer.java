package vsdk.toolkit.render.jogl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.light.LightType;
import vsdk.toolkit.gui.gizmo.LightGizmoOmniBillboard;
import vsdk.toolkit.gui.gizmo.LightGizmoStyle;
import vsdk.toolkit.media.Calligraphic2DBuffer;

public class Jogl4LightRenderer extends Jogl4Renderer
{
    private static final LinkedHashMap<Integer, Light> ACTIVE_LIGHTS =
        new LinkedHashMap<Integer, Light>();
    private static double scale = 1.0;

    public static void activate(GL gl, Light light)
    {
        if ( light == null ) {
            return;
        }
        ACTIVE_LIGHTS.put(light.getId(), copyLight(light));
    }

    public static void draw(GL gl, Light light)
    {
        draw(gl, light, null, LightGizmoStyle.CROSS);
    }

    public static void draw(GL gl, Light light, Camera camera)
    {
        draw(gl, light, camera, LightGizmoStyle.CROSS);
    }

    public static void draw(GL gl, Light light, Camera camera,
                            LightGizmoStyle lightGizmoStyle)
    {
        if ( gl == null || !gl.isGL4() ) {
            return;
        }
        draw(gl.getGL4(), light, camera, lightGizmoStyle);
    }

    public static void draw(GL4 gl, Light light, Camera camera)
    {
        draw(gl, light, camera, LightGizmoStyle.CROSS);
    }

    public static void draw(GL4 gl, Light light, Camera camera,
                            LightGizmoStyle lightGizmoStyle)
    {
        if ( gl == null || light == null ) {
            return;
        }

        if ( lightGizmoStyle == LightGizmoStyle.OMNI_BILLBOARD ) {
            drawOmniBillboard(gl, light, camera);
            return;
        }

        drawCross(gl, light, camera);
    }

    public static double getScale()
    {
        return scale;
    }

    public static void setScale(double newScale)
    {
        scale = newScale;
    }

    public static List<Light> getActiveLights()
    {
        if ( ACTIVE_LIGHTS.isEmpty() ) {
            ArrayList<Light> defaults = new ArrayList<Light>();
            defaults.add(defaultLight());
            return defaults;
        }

        ArrayList<Light> out = new ArrayList<Light>();
        for ( Light light : ACTIVE_LIGHTS.values() ) {
            out.add(copyLight(light));
        }
        return out;
    }

    private static void drawCross(GL4 gl, Light light, Camera camera)
    {
        int[] viewport = new int[4];
        gl.glGetIntegerv(GL4.GL_VIEWPORT, viewport, 0);

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

        float[] colors = buildUniformColorArray(c, positions.length / 3);

        Jogl4LineRenderer.drawLines(gl, modelViewProjection, positions, colors,
            2.0f);
    }

    private static void drawOmniBillboard(GL4 gl, Light light, Camera camera)
    {
        int[] viewport = new int[4];
        gl.glGetIntegerv(GL4.GL_VIEWPORT, viewport, 0);

        int viewportWidth = Math.max(viewport[2], 1);
        int viewportHeight = Math.max(viewport[3], 1);

        if ( camera == null ) {
            drawCross(gl, light, null);
            return;
        }

        camera.updateViewportResize(viewportWidth, viewportHeight);
        Matrix4x4d modelViewProjection = camera.calculateProjectionMatrix();

        double worldHalfSize = calculateHalfAxisLength(light, camera,
            viewportWidth, viewportHeight) * scale;
        double worldFullSize = 2.0 * worldHalfSize;

        Vector3Dd lightPosition = light.getPosition();
        Vector3Dd right = camera.getLeft().multiply(-1.0).normalized();
        Vector3Dd up = camera.getUp().normalized();

        Calligraphic2DBuffer pattern = LightGizmoOmniBillboard.createLinePattern();

        int lineCount = pattern.getNumLines();
        if ( lineCount <= 0 ) {
            return;
        }

        float[] positions = new float[lineCount * 2 * 3];
        int write = 0;
        for ( int i = 0; i < lineCount; i++ ) {
            Vector3Dd[] line = pattern.get2DLine(i);

            Vector3Dd p0 = mapPatternPointToWorld(line[0], lightPosition, right, up,
                worldFullSize);
            Vector3Dd p1 = mapPatternPointToWorld(line[1], lightPosition, right, up,
                worldFullSize);

            positions[write++] = (float)p0.x();
            positions[write++] = (float)p0.y();
            positions[write++] = (float)p0.z();
            positions[write++] = (float)p1.x();
            positions[write++] = (float)p1.y();
            positions[write++] = (float)p1.z();
        }

        float[] colors = buildUniformColorArray(light.getSpecular(), positions.length / 3);
        Jogl4LineRenderer.drawLines(gl, modelViewProjection, positions, colors, 2.0f);
    }

    private static Vector3Dd mapPatternPointToWorld(Vector3Dd point,
                                                     Vector3Dd center,
                                                     Vector3Dd right,
                                                     Vector3Dd up,
                                                     double worldSize)
    {
        double localX = (point.x() - 0.5) * worldSize;
        double localY = (point.y() - 0.5) * worldSize;

        Vector3Dd rightContribution = right.multiply(localX);
        Vector3Dd upContribution = up.multiply(localY);

        return center.add(rightContribution).add(upContribution);
    }

    private static float[] buildUniformColorArray(ColorRgb c, int vertexCount)
    {
        float cr = (float)c.r();
        float cg = (float)c.g();
        float cb = (float)c.b();

        float[] colors = new float[vertexCount * 3];
        for ( int i = 0; i < vertexCount; i++ ) {
            int base = i * 3;
            colors[base] = cr;
            colors[base + 1] = cg;
            colors[base + 2] = cb;
        }
        return colors;
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

    private static Light copyLight(Light light)
    {
        Light copy = new Light(light.getLightType(), light.getPosition(),
            light.getSpecular());
        copy.setId(light.getId());
        copy.setAmbient(light.getAmbient());
        copy.setDiffuse(light.getDiffuse());
        copy.setSpecular(light.getSpecular());
        copy.setName(light.getName());
        return copy;
    }

    private static Light defaultLight()
    {
        Light light = new Light(LightType.POINT, new Vector3Dd(10, 10, 10),
            new ColorRgb(1, 1, 1));
        light.setId(0);
        light.setAmbient(new ColorRgb(0, 0, 0));
        light.setDiffuse(new ColorRgb(1, 1, 1));
        return light;
    }
}
