package vsdk.toolkit.render.jogl;

import java.nio.FloatBuffer;
import java.util.List;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.Arrow;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.gui.gizmo.RayGizmo;

/**
Renders a {@link RayGizmo} as a lit arrow (cylinder shaft + cone head) using
OpenGL 4 shaders.

The arrow mesh is tessellated procedurally around the +Z axis.  The
{@link SimpleBody} transform stored inside the gizmo maps +Z to the actual
ray direction and translates the origin to the ray position.

When the gizmo has a current {@link RayGizmo.RaySnapshot}:
  - rays[i] and intersections[i] are always parallel lists of the same size.
  - If a bounce hit geometry (intersections[i] != null), the arrow is Z-scaled
    so its tip reaches the surface exactly.
  - If a bounce missed all geometry, the full-length arrow is drawn and three
    dot markers are placed beyond its tip as "ellipsis" indicators.
  - Reflection bounces (index >= 1) are drawn in a distinct blue material.

Arrow and sphere rendering are delegated to {@link Jogl4ArrowRenderer} and
{@link Jogl4SphereRenderer} respectively.  This class only owns the indicator
fin mesh and the frame-level orchestration.

Usage (render thread, once per frame):
<pre>
    gizmo.acquireSnapshot();                  // apply pending network update
    Jogl4RayGizmoRenderer.draw(gl, gizmo, camera, lights);
</pre>
*/
public class Jogl4RayGizmoRenderer extends Jogl4Renderer {
    private static int indicatorVaoId;
    private static int indicatorPositionVboId;
    private static int indicatorNormalVboId;
    private static int indicatorUvVboId;
    private static boolean initialized;

    private static final float IND_OUTER_R  = 0.65f;
    private static final float IND_INNER_R  = 0.17f;
    private static final float IND_HALF_W   = 0.12f;
    private static final float IND_TIP_Z    = 0.30f;

    /**
     * Draws the ray gizmo for the current frame.  Must be called after
     * {@link RayGizmo#acquireSnapshot()} has been called for this frame.
     *
     * @param gl     GL4 context
     * @param gizmo  gizmo to draw
     * @param camera active camera
     * @param lights scene lights (only the first is used for sphere shading)
     */
    public static void draw(GL4 gl, RayGizmo gizmo, Camera camera, List<Light> lights) {
        if ( gl == null || gizmo == null || camera == null ) {
            return;
        }
        if ( lights == null || lights.isEmpty() ) {
            return;
        }
        if ( !gizmo.isVisible() ) {
            return;
        }

        ensureMesh(gl);

        RendererConfiguration quality = buildSurfaceQuality();
        Matrix4x4d primaryModelMatrix = gizmo.getBody().getTransformationMatrix();
        Matrix4x4d projection = Jogl4CameraRenderer.activate(gl, camera);

        SimpleScene scene = gizmo.buildScene();

        for ( SimpleBody body : scene.getSimpleBodies() ) {
            Geometry geom = body.getGeometry();
            Matrix4x4d modelMatrix = body.getTransformationMatrix();
            SimpleMaterial material = body.getMaterial();

            if ( geom instanceof Arrow arrowGeom ) {
                Jogl4ArrowRenderer.draw(gl, arrowGeom, modelMatrix, projection,
                        camera, lights, material, quality);
            } else if ( geom instanceof Sphere sphereGeom ) {
                Jogl4SphereRenderer.draw(gl, sphereGeom, camera, lights.get(0),
                        material, quality, null, null, modelMatrix, 16, 12);
            }
        }

        drawIndicator(gl, gizmo.getRotationAngleInRadians(), primaryModelMatrix,
                projection, camera, lights, quality);

        gl.glDepthMask(true);
        gl.glDepthFunc(GL.GL_LESS);
    }

    public static void dispose(GL4 gl) {
        int[] tmp = new int[1];

        Jogl4ArrowRenderer.dispose(gl);

        if ( indicatorPositionVboId != 0 ) {
            tmp[0] = indicatorPositionVboId;
            gl.glDeleteBuffers(1, tmp, 0);
            indicatorPositionVboId = 0;
        }
        if ( indicatorNormalVboId != 0 ) {
            tmp[0] = indicatorNormalVboId;
            gl.glDeleteBuffers(1, tmp, 0);
            indicatorNormalVboId = 0;
        }
        if ( indicatorUvVboId != 0 ) {
            tmp[0] = indicatorUvVboId;
            gl.glDeleteBuffers(1, tmp, 0);
            indicatorUvVboId = 0;
        }
        if ( indicatorVaoId != 0 ) {
            tmp[0] = indicatorVaoId;
            gl.glDeleteVertexArrays(1, tmp, 0);
            indicatorVaoId = 0;
        }

        initialized = false;
    }

    private static RendererConfiguration buildSurfaceQuality() {
        RendererConfiguration quality = new RendererConfiguration();
        quality.setSurfaces(true);
        quality.setWires(false);
        quality.setPoints(false);
        quality.setTexture(false);
        quality.setBumpMap(false);
        return quality;
    }

    private static void drawIndicator(
        GL4 gl,
        double rollAngleRadians,
        Matrix4x4d arrowModelMatrix,
        Matrix4x4d projection,
        Camera camera,
        List<Light> lights,
        RendererConfiguration quality)
    {
        Matrix4x4d rollRotation = new Matrix4x4d().axisRotation(
            rollAngleRadians, 0, 0, 1);
        Matrix4x4d indicatorModelMatrix = arrowModelMatrix.multiply(rollRotation);
        Matrix4x4d indicatorMvp         = projection.multiply(indicatorModelMatrix);
        Matrix4x4d indicatorModelIt     = indicatorModelMatrix.invert().transpose();

        int program = Jogl4RendererConfigurationShaderSelector.selectSurfaceShaderProgram(
            gl, quality, false, false);

        configureProgram(gl, program, indicatorMvp, indicatorModelMatrix, indicatorModelIt,
            camera, lights, indicatorMaterial(), quality);

        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthMask(true);
        gl.glDepthFunc(GL.GL_LESS);
        gl.glDisable(GL.GL_CULL_FACE);

        gl.glBindVertexArray(indicatorVaoId);
        gl.glDrawArrays(GL.GL_TRIANGLES, 0, 3);
        gl.glBindVertexArray(0);

        Jogl4RendererConfigurationShaderSelector.deactivateShader(gl);
    }

    private static void ensureMesh(GL4 gl) {
        if ( initialized ) {
            return;
        }

        uploadIndicatorMesh(gl);
        initialized = true;
    }

    private static void uploadIndicatorMesh(GL4 gl) {
        // Fin triangle pointing in +X at the arrow base (z=0).
        // P0=tip, P1=base-left, P2=base-right.
        float[] positions = {
            IND_OUTER_R,  0.0f,         IND_TIP_Z,
            IND_INNER_R, -IND_HALF_W,   0.0f,
            IND_INNER_R,  IND_HALF_W,   0.0f
        };

        float[] normals = computeNormals();
        float[] uvs     = { 0.5f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f };
        int[] tmp = new int[1];

        gl.glGenVertexArrays(1, tmp, 0);
        indicatorVaoId = tmp[0];

        gl.glGenBuffers(1, tmp, 0);
        indicatorPositionVboId = tmp[0];

        gl.glGenBuffers(1, tmp, 0);
        indicatorNormalVboId = tmp[0];

        gl.glGenBuffers(1, tmp, 0);
        indicatorUvVboId = tmp[0];

        gl.glBindVertexArray(indicatorVaoId);
        uploadBuffer(gl, indicatorPositionVboId, 0, 3, positions);
        uploadBuffer(gl, indicatorNormalVboId,   1, 3, normals);
        uploadBuffer(gl, indicatorUvVboId,       2, 2, uvs);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        gl.glBindVertexArray(0);
    }

    private static float[] computeNormals() {
        float ax = IND_INNER_R - IND_OUTER_R;
        float ay = -IND_HALF_W;
        float az = -IND_TIP_Z;
        float bx = IND_INNER_R - IND_OUTER_R;
        float by =  IND_HALF_W;
        float bz = -IND_TIP_Z;
        float nx = ay * bz - az * by;
        float ny = az * bx - ax * bz;
        float nz = ax * by - ay * bx;
        float normalLength = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= normalLength;
        ny /= normalLength;
        nz /= normalLength;

        return new float[]{ nx, ny, nz, nx, ny, nz, nx, ny, nz };
    }

    private static void configureProgram(
        GL4 gl,
        int programId,
        Matrix4x4d modelViewProjection,
        Matrix4x4d modelViewLocal,
        Matrix4x4d modelViewITLocal,
        Camera camera,
        List<Light> lights,
        SimpleMaterial material,
        RendererConfiguration quality)
    {
        ColorRgb kd = material.getDiffuse();
        Jogl4RendererConfigurationShaderSelector.activateShader(
                gl, programId, modelViewProjection, quality,
                (float)kd.r(), (float)kd.g(), (float)kd.b());

        setMatrix(gl, programId, "modelViewLocal", modelViewLocal);
        setMatrix(gl, programId, "modelViewITLocal", modelViewITLocal);
        setVector3(gl, programId, "cameraPositionGlobal", camera.getPosition());

        int lightCount = 0;
        for ( Light light : lights ) {
            if ( light == null ) {
                continue;
            }
            setVector3(gl, programId, "lightPositionsGlobal[" + lightCount + "]",
                    light.getPosition());
            setVector3(gl, programId, "lightColorsGlobal[" + lightCount + "]",
                    light.getEmission());
            lightCount++;
        }
        setInt(gl, programId, "numberOfLights", lightCount);
        setVector3(gl, programId, "ambientColor", material.getAmbient());
        setVector3(gl, programId, "diffuseColor", material.getDiffuse());
        setVector3(gl, programId, "specularColor", material.getSpecular());
        setFloat(gl, programId, "phongExponent", (float)material.getPhongExponent());
        setInt(gl, programId, "withTexture", 0);
        setInt(gl, programId, "withBumpMap", 0);
    }

    private static SimpleMaterial indicatorMaterial() {
        SimpleMaterial m = new SimpleMaterial();
        m = m.withAmbient(new ColorRgb(0.3, 0.3, 0.0));
        m = m.withDiffuse(new ColorRgb(1.0, 0.9, 0.0));
        m = m.withSpecular(new ColorRgb(1.0, 1.0, 0.8));
        m = m.withPhongExponent(64.0);
        return m;
    }

    private static void uploadBuffer(GL4 gl, int bufferId, int attrib, int size, float[] data) {
        FloatBuffer buf = Buffers.newDirectFloatBuffer(data);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, (long)data.length * Float.BYTES, buf, GL.GL_STATIC_DRAW);
        gl.glEnableVertexAttribArray(attrib);
        gl.glVertexAttribPointer(attrib, size, GL.GL_FLOAT, false, 0, 0L);
    }

    private static void setMatrix(GL4 gl, int programId, String name, Matrix4x4d matrix) {
        int loc = gl.glGetUniformLocation(programId, name);
        if ( loc >= 0 ) {
            gl.glUniformMatrix4fv(loc, 1, false,
                    Jogl4MatrixRenderer.toColumnMajorFloatArray(matrix), 0);
        }
    }

    private static void setVector3(GL4 gl, int programId, String name, Vector3Dd value) {
        int loc = gl.glGetUniformLocation(programId, name);
        if ( loc >= 0 ) {
            gl.glUniform3f(loc, (float)value.x(), (float)value.y(), (float)value.z());
        }
    }

    private static void setVector3(GL4 gl, int programId, String name, ColorRgb value) {
        int loc = gl.glGetUniformLocation(programId, name);
        if ( loc >= 0 ) {
            gl.glUniform3f(loc, (float)value.r(), (float)value.g(), (float)value.b());
        }
    }

    private static void setInt(GL4 gl, int programId, String name, int value) {
        int loc = gl.glGetUniformLocation(programId, name);
        if ( loc >= 0 ) {
            gl.glUniform1i(loc, value);
        }
    }

    private static void setFloat(GL4 gl, int programId, String name, float value) {
        int loc = gl.glGetUniformLocation(programId, name);
        if ( loc >= 0 ) {
            gl.glUniform1f(loc, value);
        }
    }
}
