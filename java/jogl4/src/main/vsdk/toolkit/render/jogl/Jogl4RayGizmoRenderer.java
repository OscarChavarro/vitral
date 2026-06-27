package vsdk.toolkit.render.jogl;

import java.nio.FloatBuffer;
import java.util.List;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.gui.gizmo.RayGizmo;

/**
Renders a {@link RayGizmo} as a lit arrow (cylinder shaft + cone head) using
OpenGL 4 shaders.

The arrow mesh is tessellated procedurally around the +Z axis.  The
{@link SimpleBody} transform stored inside the gizmo maps +Z to the actual
ray direction and translates the origin to the ray position.

Usage (render thread, once per frame):
<pre>
    gizmo.acquireSnapshot();                  // apply pending network update
    Jogl4RayGizmoRenderer.draw(gl, gizmo, camera, lights);
</pre>
*/
public class Jogl4RayGizmoRenderer extends Jogl4Renderer {

    private static final int SLICES = 16;
    private static final float SURFACE_POLYGON_OFFSET_FACTOR = 1.0f;
    private static final float SURFACE_POLYGON_OFFSET_UNITS = 1.0f;

    private static int vaoId;
    private static int positionVboId;
    private static int normalVboId;
    private static int uvVboId;
    private static int vertexCount;
    private static boolean initialized;

    /**
     * Draws the ray gizmo.  Acquires any pending snapshot, tessellates the
     * arrow mesh if needed, and issues draw calls.
     *
     * @param gl     GL4 context
     * @param gizmo  gizmo to draw
     * @param camera active camera
     * @param lights scene lights (only the first is used)
     */
    public static void draw(GL4 gl, RayGizmo gizmo, Camera camera, List<Light> lights) {
        if ( gl == null || gizmo == null || camera == null ) {
            return;
        }
        if ( lights == null || lights.isEmpty() ) {
            return;
        }

        ensureMesh(gl, gizmo);

        SimpleBody body = gizmo.getBody();
        SimpleMaterial material = body.getMaterial();
        if ( material == null ) {
            material = defaultMaterial();
        }

        Matrix4x4d modelMatrix = body.getTransformationMatrix();
        Matrix4x4d projection = Jogl4CameraRenderer.activate(gl, camera);
        Matrix4x4d modelViewProjection = projection.multiply(modelMatrix);
        Matrix4x4d modelIt = modelMatrix.invert().transpose();

        RendererConfiguration quality = new RendererConfiguration();
        quality.setSurfaces(true);
        quality.setWires(false);
        quality.setPoints(false);
        quality.setTexture(false);
        quality.setBumpMap(false);

        int program = Jogl4RendererConfigurationShaderSelector.selectSurfaceShaderProgram(
                gl, quality, false, false);

        configureProgram(gl, program, modelViewProjection, modelMatrix, modelIt,
                camera, lights, material, quality);

        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glDepthMask(true);
        gl.glDepthFunc(GL4.GL_LESS);
        gl.glEnable(GL4.GL_CULL_FACE);
        gl.glCullFace(GL4.GL_BACK);
        gl.glEnable(GL4.GL_POLYGON_OFFSET_FILL);
        gl.glPolygonOffset(SURFACE_POLYGON_OFFSET_FACTOR, SURFACE_POLYGON_OFFSET_UNITS);
        gl.glPolygonMode(GL4.GL_FRONT_AND_BACK, GL4.GL_FILL);

        gl.glBindVertexArray(vaoId);
        gl.glDrawArrays(GL4.GL_TRIANGLES, 0, vertexCount);
        gl.glBindVertexArray(0);

        gl.glDisable(GL4.GL_POLYGON_OFFSET_FILL);
        gl.glDisable(GL4.GL_CULL_FACE);

        Jogl4RendererConfigurationShaderSelector.deactivateShader(gl);

        gl.glDepthMask(true);
        gl.glDepthFunc(GL4.GL_LESS);
    }

    public static void dispose(GL4 gl) {
        int[] tmp = new int[1];

        if ( positionVboId != 0 ) {
            tmp[0] = positionVboId;
            gl.glDeleteBuffers(1, tmp, 0);
            positionVboId = 0;
        }
        if ( normalVboId != 0 ) {
            tmp[0] = normalVboId;
            gl.glDeleteBuffers(1, tmp, 0);
            normalVboId = 0;
        }
        if ( uvVboId != 0 ) {
            tmp[0] = uvVboId;
            gl.glDeleteBuffers(1, tmp, 0);
            uvVboId = 0;
        }
        if ( vaoId != 0 ) {
            tmp[0] = vaoId;
            gl.glDeleteVertexArrays(1, tmp, 0);
            vaoId = 0;
        }

        vertexCount = 0;
        initialized = false;
    }

    // -----------------------------------------------------------------------

    private static void ensureMesh(GL4 gl, RayGizmo gizmo) {
        if ( initialized ) {
            return;
        }

        SimpleBody body = gizmo.getBody();
        vsdk.toolkit.environment.geometry.Geometry geom = body.getGeometry();
        if ( !(geom instanceof vsdk.toolkit.environment.geometry.volume.Arrow arrow) ) {
            return;
        }

        ArrowMesh mesh = buildArrowMesh(
                arrow.getBaseRadius(),
                arrow.getHeadRadius(),
                arrow.getBaseLength(),
                arrow.getHeadLength(),
                SLICES);

        uploadMesh(gl, mesh);
        initialized = true;
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
        for ( int i = 0; i < lights.size(); i++ ) {
            Light light = lights.get(i);
            if ( light == null ) {
                continue;
            }
            setVector3(gl, programId, "lightPositionsGlobal[" + lightCount + "]",
                    light.getPosition());
            setVector3(gl, programId, "lightColorsGlobal[" + lightCount + "]",
                    light.getSpecular());
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

    // -----------------------------------------------------------------------
    // Procedural arrow mesh (cylinder shaft + cone head), aligned along +Z.
    // -----------------------------------------------------------------------

    private static ArrowMesh buildArrowMesh(
        double baseRadius,
        double headRadius,
        double baseLength,
        double headLength,
        int slices)
    {
        // Faces: shaft side + shaft bottom cap + cone side (each as 2 tris per slice)
        // shaft side: slices * 2 triangles
        // shaft bottom: slices * 1 triangle (fan)
        // shaft top cap (ring from baseRadius to headRadius): slices * 2 triangles
        // cone side: slices * 1 triangle (fan to apex)
        int triangleCount = slices * 6;
        int verticesNeeded = triangleCount * 3;

        float[] positions = new float[verticesNeeded * 3];
        float[] normals   = new float[verticesNeeded * 3];
        float[] uvs       = new float[verticesNeeded * 2];

        int pi = 0;
        int ni = 0;
        int ti = 0;

        // ---- Shaft side ----
        for ( int i = 0; i < slices; i++ ) {
            double a0 = 2.0 * Math.PI * i / slices;
            double a1 = 2.0 * Math.PI * (i + 1) / slices;

            float x0 = (float)(Math.cos(a0) * baseRadius);
            float y0 = (float)(Math.sin(a0) * baseRadius);
            float x1 = (float)(Math.cos(a1) * baseRadius);
            float y1 = (float)(Math.sin(a1) * baseRadius);

            float nx0 = (float)Math.cos(a0);
            float ny0 = (float)Math.sin(a0);
            float nx1 = (float)Math.cos(a1);
            float ny1 = (float)Math.sin(a1);

            // Triangle 1: bottom-left, bottom-right, top-right
            pi = addPos(positions, pi, x0, y0, 0);
            ni = addNorm(normals, ni, nx0, ny0, 0);
            ti = addUv(uvs, ti, 0, 0);

            pi = addPos(positions, pi, x1, y1, 0);
            ni = addNorm(normals, ni, nx1, ny1, 0);
            ti = addUv(uvs, ti, 1, 0);

            pi = addPos(positions, pi, x1, y1, (float)baseLength);
            ni = addNorm(normals, ni, nx1, ny1, 0);
            ti = addUv(uvs, ti, 1, 1);

            // Triangle 2: bottom-left, top-right, top-left
            pi = addPos(positions, pi, x0, y0, 0);
            ni = addNorm(normals, ni, nx0, ny0, 0);
            ti = addUv(uvs, ti, 0, 0);

            pi = addPos(positions, pi, x1, y1, (float)baseLength);
            ni = addNorm(normals, ni, nx1, ny1, 0);
            ti = addUv(uvs, ti, 1, 1);

            pi = addPos(positions, pi, x0, y0, (float)baseLength);
            ni = addNorm(normals, ni, nx0, ny0, 0);
            ti = addUv(uvs, ti, 0, 1);
        }

        // ---- Shaft bottom cap (z=0, facing -Z) ----
        for ( int i = 0; i < slices; i++ ) {
            double a0 = 2.0 * Math.PI * i / slices;
            double a1 = 2.0 * Math.PI * (i + 1) / slices;

            float x0 = (float)(Math.cos(a0) * baseRadius);
            float y0 = (float)(Math.sin(a0) * baseRadius);
            float x1 = (float)(Math.cos(a1) * baseRadius);
            float y1 = (float)(Math.sin(a1) * baseRadius);

            pi = addPos(positions, pi, 0, 0, 0);
            ni = addNorm(normals, ni, 0, 0, -1);
            ti = addUv(uvs, ti, 0.5f, 0.5f);

            pi = addPos(positions, pi, x1, y1, 0);
            ni = addNorm(normals, ni, 0, 0, -1);
            ti = addUv(uvs, ti, 0, 0);

            pi = addPos(positions, pi, x0, y0, 0);
            ni = addNorm(normals, ni, 0, 0, -1);
            ti = addUv(uvs, ti, 1, 0);
        }

        // ---- Head annular cap (flat ring from baseRadius to headRadius at z=baseLength, facing -Z) ----
        float coneBase = (float)baseLength;
        for ( int i = 0; i < slices; i++ ) {
            double a0 = 2.0 * Math.PI * i / slices;
            double a1 = 2.0 * Math.PI * (i + 1) / slices;

            float ox0 = (float)(Math.cos(a0) * baseRadius);
            float oy0 = (float)(Math.sin(a0) * baseRadius);
            float ox1 = (float)(Math.cos(a1) * baseRadius);
            float oy1 = (float)(Math.sin(a1) * baseRadius);

            float ix0 = (float)(Math.cos(a0) * headRadius);
            float iy0 = (float)(Math.sin(a0) * headRadius);
            float ix1 = (float)(Math.cos(a1) * headRadius);
            float iy1 = (float)(Math.sin(a1) * headRadius);

            // Triangle 1
            pi = addPos(positions, pi, ox0, oy0, coneBase);
            ni = addNorm(normals, ni, 0, 0, -1);
            ti = addUv(uvs, ti, 0, 0);

            pi = addPos(positions, pi, ox1, oy1, coneBase);
            ni = addNorm(normals, ni, 0, 0, -1);
            ti = addUv(uvs, ti, 1, 0);

            pi = addPos(positions, pi, ix1, iy1, coneBase);
            ni = addNorm(normals, ni, 0, 0, -1);
            ti = addUv(uvs, ti, 1, 1);

            // Triangle 2
            pi = addPos(positions, pi, ox0, oy0, coneBase);
            ni = addNorm(normals, ni, 0, 0, -1);
            ti = addUv(uvs, ti, 0, 0);

            pi = addPos(positions, pi, ix1, iy1, coneBase);
            ni = addNorm(normals, ni, 0, 0, -1);
            ti = addUv(uvs, ti, 1, 1);

            pi = addPos(positions, pi, ix0, iy0, coneBase);
            ni = addNorm(normals, ni, 0, 0, -1);
            ti = addUv(uvs, ti, 0, 1);
        }

        // ---- Cone side ----
        float apex = (float)(baseLength + headLength);
        // Cone slant normal: the outward normal of a cone side face has
        // components (cos(a)*sinAlpha, sin(a)*sinAlpha, cosAlpha) where
        // sinAlpha = headRadius/slantLength, cosAlpha = headLength/slantLength.
        float slantLength = (float)Math.sqrt(headRadius * headRadius + headLength * headLength);
        float cosAlpha = (slantLength > 1e-12f) ? (float)(headLength / slantLength) : 1.0f;
        float sinAlpha = (slantLength > 1e-12f) ? (float)(headRadius / slantLength) : 0.0f;

        for ( int i = 0; i < slices; i++ ) {
            double a0 = 2.0 * Math.PI * i / slices;
            double a1 = 2.0 * Math.PI * (i + 1) / slices;
            double aMid = (a0 + a1) * 0.5;

            float x0 = (float)(Math.cos(a0) * headRadius);
            float y0 = (float)(Math.sin(a0) * headRadius);
            float x1 = (float)(Math.cos(a1) * headRadius);
            float y1 = (float)(Math.sin(a1) * headRadius);

            float nx0 = (float)(Math.cos(a0) * sinAlpha);
            float ny0 = (float)(Math.sin(a0) * sinAlpha);
            float nx1 = (float)(Math.cos(a1) * sinAlpha);
            float ny1 = (float)(Math.sin(a1) * sinAlpha);
            float nxMid = (float)(Math.cos(aMid) * sinAlpha);
            float nyMid = (float)(Math.sin(aMid) * sinAlpha);

            pi = addPos(positions, pi, x0, y0, coneBase);
            ni = addNorm(normals, ni, nx0, ny0, cosAlpha);
            ti = addUv(uvs, ti, 0, 1);

            pi = addPos(positions, pi, x1, y1, coneBase);
            ni = addNorm(normals, ni, nx1, ny1, cosAlpha);
            ti = addUv(uvs, ti, 1, 1);

            pi = addPos(positions, pi, 0, 0, apex);
            ni = addNorm(normals, ni, nxMid, nyMid, cosAlpha);
            ti = addUv(uvs, ti, 0.5f, 0);
        }

        ArrowMesh mesh = new ArrowMesh();
        mesh.positions = positions;
        mesh.normals = normals;
        mesh.uvs = uvs;
        mesh.vertexCount = verticesNeeded;
        return mesh;
    }

    private static int addPos(float[] buf, int idx, float x, float y, float z) {
        buf[idx++] = x;
        buf[idx++] = y;
        buf[idx++] = z;
        return idx;
    }

    private static int addNorm(float[] buf, int idx, float x, float y, float z) {
        float len = (float)Math.sqrt(x * x + y * y + z * z);
        if ( len > 1e-12f ) {
            buf[idx++] = x / len;
            buf[idx++] = y / len;
            buf[idx++] = z / len;
        }
        else {
            buf[idx++] = 0;
            buf[idx++] = 0;
            buf[idx++] = 1;
        }
        return idx;
    }

    private static int addUv(float[] buf, int idx, float u, float v) {
        buf[idx++] = u;
        buf[idx++] = v;
        return idx;
    }

    private static void uploadMesh(GL4 gl, ArrowMesh mesh) {
        int[] tmp = new int[1];

        gl.glGenVertexArrays(1, tmp, 0);
        vaoId = tmp[0];

        gl.glGenBuffers(1, tmp, 0);
        positionVboId = tmp[0];

        gl.glGenBuffers(1, tmp, 0);
        normalVboId = tmp[0];

        gl.glGenBuffers(1, tmp, 0);
        uvVboId = tmp[0];

        gl.glBindVertexArray(vaoId);

        uploadBuffer(gl, positionVboId, 0, 3, mesh.positions);
        uploadBuffer(gl, normalVboId, 1, 3, mesh.normals);
        uploadBuffer(gl, uvVboId, 2, 2, mesh.uvs);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
        gl.glBindVertexArray(0);

        vertexCount = mesh.vertexCount;
    }

    private static void uploadBuffer(GL4 gl, int bufferId, int attrib, int size, float[] data) {
        FloatBuffer buf = Buffers.newDirectFloatBuffer(data);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, bufferId);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long)data.length * Float.BYTES, buf, GL4.GL_STATIC_DRAW);
        gl.glEnableVertexAttribArray(attrib);
        gl.glVertexAttribPointer(attrib, size, GL4.GL_FLOAT, false, 0, 0L);
    }

    private static SimpleMaterial defaultMaterial() {
        SimpleMaterial m = new SimpleMaterial();
        m = m.withAmbient(new ColorRgb(0.1, 0.0, 0.0));
        m = m.withDiffuse(new ColorRgb(0.9, 0.2, 0.1));
        m = m.withSpecular(new ColorRgb(1.0, 1.0, 1.0));
        m = m.withPhongExponent(32.0);
        return m;
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

    private static final class ArrowMesh {
        float[] positions;
        float[] normals;
        float[] uvs;
        int vertexCount;
    }
}
