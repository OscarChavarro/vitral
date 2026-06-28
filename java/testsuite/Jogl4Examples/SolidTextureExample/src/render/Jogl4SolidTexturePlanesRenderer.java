package render;

import java.nio.FloatBuffer;
import java.util.List;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.render.jogl.Jogl4CameraRenderer;
import vsdk.toolkit.render.jogl.Jogl4MatrixRenderer;
import vsdk.toolkit.render.jogl.Jogl4RendererConfigurationShaderSelector;

public class Jogl4SolidTexturePlanesRenderer {
    private int vaoId;
    private int positionVboId;
    private int normalVboId;
    private int uvVboId;

    public void draw(GL4 gl, List<Image> images, Camera camera)
    {
        if ( gl == null || images == null || images.isEmpty() || camera == null ) {
            return;
        }

        ensureBuffers(gl);

        int planeCount = images.size();
        PlaneFrame frame = buildPlaneFrame(planeCount);
        uploadFrame(gl, frame);

        RendererConfiguration quality = new RendererConfiguration();
        quality.setShadingType(RendererConfiguration.SHADING_TYPE_FLAT);
        quality.setTexture(false);
        quality.setBumpMap(false);

        int program = Jogl4RendererConfigurationShaderSelector.selectSurfaceShaderProgram(
            gl, quality, false, false);
        Matrix4x4d identity = Matrix4x4d.identityMatrix();
        Matrix4x4d modelViewProjection = Jogl4CameraRenderer.activate(gl, camera);
        Jogl4RendererConfigurationShaderSelector.activateShader(
            gl, program, modelViewProjection, quality, 1.0f, 1.0f, 1.0f);
        configureFlatWhiteProgram(gl, program, identity, camera);

        gl.glDisable(GL4.GL_CULL_FACE);
        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glDepthMask(true);
        gl.glDepthFunc(GL4.GL_LESS);
        gl.glPolygonMode(GL4.GL_FRONT_AND_BACK, GL4.GL_FILL);
        gl.glBindVertexArray(vaoId);
        for ( int i = 0; i < frame.quadCount(); i++ ) {
            gl.glDrawArrays(GL4.GL_TRIANGLE_FAN, i * 4, 4);
        }
        gl.glBindVertexArray(0);
        Jogl4RendererConfigurationShaderSelector.deactivateShader(gl);
    }

    public void dispose(GL4 gl)
    {
        if ( gl == null ) {
            return;
        }
        int[] ids = new int[1];
        if ( positionVboId != 0 ) {
            ids[0] = positionVboId;
            gl.glDeleteBuffers(1, ids, 0);
            positionVboId = 0;
        }
        if ( normalVboId != 0 ) {
            ids[0] = normalVboId;
            gl.glDeleteBuffers(1, ids, 0);
            normalVboId = 0;
        }
        if ( uvVboId != 0 ) {
            ids[0] = uvVboId;
            gl.glDeleteBuffers(1, ids, 0);
            uvVboId = 0;
        }
        if ( vaoId != 0 ) {
            ids[0] = vaoId;
            gl.glDeleteVertexArrays(1, ids, 0);
            vaoId = 0;
        }
    }

    private void ensureBuffers(GL4 gl)
    {
        if ( vaoId != 0 ) {
            return;
        }
        int[] arrays = new int[1];
        int[] buffers = new int[3];
        gl.glGenVertexArrays(1, arrays, 0);
        vaoId = arrays[0];
        gl.glGenBuffers(3, buffers, 0);
        positionVboId = buffers[0];
        normalVboId = buffers[1];
        uvVboId = buffers[2];
    }

    private PlaneFrame buildPlaneFrame(int planeCount)
    {
        int vertexCount = planeCount * 4;
        float[] positions = new float[vertexCount * 3];
        float[] normals = new float[vertexCount * 3];
        float[] uvs = new float[vertexCount * 2];

        int p = 0;
        int n = 0;
        int t = 0;
        for ( int i = 0; i < planeCount; i++ ) {
            float z = planeCount == 1
                ? -1.0f
                : -1.0f + (2.0f * i) / (float)(planeCount - 1);

            p = appendVertex(positions, p, -1.0f, -1.0f, z);
            p = appendVertex(positions, p, 1.0f, -1.0f, z);
            p = appendVertex(positions, p, 1.0f, 1.0f, z);
            p = appendVertex(positions, p, -1.0f, 1.0f, z);

            for ( int j = 0; j < 4; j++ ) {
                n = appendVertex(normals, n, 0.0f, 0.0f, 1.0f);
            }

            t = appendUv(uvs, t, 0.0f, 0.0f);
            t = appendUv(uvs, t, 1.0f, 0.0f);
            t = appendUv(uvs, t, 1.0f, 1.0f);
            t = appendUv(uvs, t, 0.0f, 1.0f);
        }

        return new PlaneFrame(positions, normals, uvs);
    }

    private void uploadFrame(GL4 gl, PlaneFrame frame)
    {
        gl.glBindVertexArray(vaoId);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, positionVboId);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long)frame.positions.length * Float.BYTES,
            toBuffer(frame.positions), GL4.GL_STREAM_DRAW);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0L);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, normalVboId);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long)frame.normals.length * Float.BYTES,
            toBuffer(frame.normals), GL4.GL_STREAM_DRAW);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 0, 0L);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, uvVboId);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long)frame.uvs.length * Float.BYTES,
            toBuffer(frame.uvs), GL4.GL_STREAM_DRAW);
        gl.glEnableVertexAttribArray(2);
        gl.glVertexAttribPointer(2, 2, GL4.GL_FLOAT, false, 0, 0L);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
        gl.glBindVertexArray(0);
    }

    private void configureFlatWhiteProgram(
        GL4 gl,
        int programId,
        Matrix4x4d identity,
        Camera camera)
    {
        setMatrix(gl, programId, "modelViewLocal", identity);
        setMatrix(gl, programId, "modelViewITLocal", identity);
        setVector3(gl, programId, "cameraPositionGlobal", camera.getPosition());
        setVector3(gl, programId, "lightPositionsGlobal[0]", new Vector3Dd(0.0, 0.0, 5.0));
        setVector3(gl, programId, "lightColorsGlobal[0]", new Vector3Dd(1.0, 1.0, 1.0));
        setVector3(gl, programId, "ambientColor", new Vector3Dd(1.0, 1.0, 1.0));
        setVector3(gl, programId, "diffuseColor", new Vector3Dd(1.0, 1.0, 1.0));
        setVector3(gl, programId, "specularColor", new Vector3Dd(0.0, 0.0, 0.0));
        setInt(gl, programId, "numberOfLights", 1);
        setInt(gl, programId, "withTexture", 0);
        setInt(gl, programId, "withBumpMap", 0);
        setFloat(gl, programId, "phongExponent", 1.0f);
    }

    private static int appendVertex(float[] values, int offset, float x, float y, float z)
    {
        values[offset++] = x;
        values[offset++] = y;
        values[offset++] = z;
        return offset;
    }

    private static int appendUv(float[] values, int offset, float u, float v)
    {
        values[offset++] = u;
        values[offset++] = v;
        return offset;
    }

    private static FloatBuffer toBuffer(float[] data)
    {
        FloatBuffer buffer = Buffers.newDirectFloatBuffer(data.length);
        buffer.put(data);
        buffer.flip();
        return buffer;
    }

    private static void setMatrix(GL4 gl, int programId, String name, Matrix4x4d matrix)
    {
        int loc = gl.glGetUniformLocation(programId, name);
        if ( loc >= 0 ) {
            gl.glUniformMatrix4fv(loc, 1, false,
                Jogl4MatrixRenderer.toColumnMajorFloatArray(matrix), 0);
        }
    }

    private static void setVector3(GL4 gl, int programId, String name, Vector3Dd value)
    {
        int loc = gl.glGetUniformLocation(programId, name);
        if ( loc >= 0 ) {
            gl.glUniform3f(loc, (float)value.x(), (float)value.y(), (float)value.z());
        }
    }

    private static void setInt(GL4 gl, int programId, String name, int value)
    {
        int loc = gl.glGetUniformLocation(programId, name);
        if ( loc >= 0 ) {
            gl.glUniform1i(loc, value);
        }
    }

    private static void setFloat(GL4 gl, int programId, String name, float value)
    {
        int loc = gl.glGetUniformLocation(programId, name);
        if ( loc >= 0 ) {
            gl.glUniform1f(loc, value);
        }
    }

    private record PlaneFrame(float[] positions, float[] normals, float[] uvs) {
        private int quadCount()
        {
            return positions.length / 12;
        }
    }
}
