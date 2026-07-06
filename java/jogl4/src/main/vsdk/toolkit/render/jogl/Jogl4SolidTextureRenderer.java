package vsdk.toolkit.render.jogl;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL2GL3;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector4Dd;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.geometry.surface.TriangleMeshGroup;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.environment.scene.SimpleBody;

public final class Jogl4SolidTextureRenderer {
    private int vaoId;
    private int positionVboId;
    private int normalVboId;
    private int vertexCount;
    private int programId;
    private int solidTextureId;
    private long uploadedTextureRevision = Long.MIN_VALUE;
    private int uploadedTextureSize;
    private ByteBuffer solidTextureUploadBuffer;
    private FloatBuffer positionUploadBuffer;
    private FloatBuffer normalUploadBuffer;
    private final Path shaderDirectory;

    public Jogl4SolidTextureRenderer(Path shaderDirectory)
    {
        this.shaderDirectory = shaderDirectory;
    }

    public void draw(
        GL4 gl,
        SimpleScene scene,
        Camera camera,
        List<Light> lights,
        byte[] solidTextureVolumeRgb8,
        int solidTextureSize,
        long solidTextureRevision)
    {
        draw(gl, scene, camera, lights, solidTextureVolumeRgb8, solidTextureSize,
            solidTextureRevision, null);
    }

    public void draw(
        GL4 gl,
        SimpleScene scene,
        Camera camera,
        List<Light> lights,
        byte[] solidTextureVolumeRgb8,
        int solidTextureSize,
        long solidTextureRevision,
        InfinitePlane clippingPlane)
    {
        if ( gl == null || scene == null || camera == null || lights == null ) {
            return;
        }
        ensureBuffers(gl);
        ensureProgram(gl);
        ensureSolidTexture(gl, solidTextureVolumeRgb8, solidTextureSize, solidTextureRevision);

        if ( solidTextureId <= 0 ) {
            return;
        }

        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthMask(true);
        gl.glDepthFunc(GL.GL_LESS);
        gl.glDisable(GL.GL_CULL_FACE);
        gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);

        gl.glUseProgram(programId);
        configureClippingPlane(gl, clippingPlane);
        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL2ES2.GL_TEXTURE_3D, solidTextureId);
        setInt(gl, programId, "sSolidTexture", 0);
        setInt(gl, programId, "numberOfLights", Math.min(lights.size(), 8));
        setFloat(gl, programId, "phongExponent", 24.0f);

        for ( int i = 0; i < lights.size() && i < 8; i++ ) {
            Light light = lights.get(i);
            if ( light != null ) {
                setVector3(gl, programId, "lightPositionsGlobal[" + i + "]", light.getPosition());
                setColor(gl, programId, "lightColorsGlobal[" + i + "]", light.getEmission());
            }
        }

        setVector3(gl, programId, "cameraPositionGlobal", camera.getPosition());

        for ( SimpleBody body : scene.getSimpleBodies() ) {
            drawBody(gl, body, camera);
        }

        gl.glBindVertexArray(0);
        gl.glBindTexture(GL2ES2.GL_TEXTURE_3D, 0);
        gl.glDisable(GL4.GL_CLIP_DISTANCE0);
        gl.glUseProgram(0);
    }

    public void dispose(GL4 gl)
    {
        if ( gl == null ) {
            return;
        }
        int[] ids = new int[1];
        if ( solidTextureId != 0 ) {
            ids[0] = solidTextureId;
            gl.glDeleteTextures(1, ids, 0);
            solidTextureId = 0;
        }
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
        if ( vaoId != 0 ) {
            ids[0] = vaoId;
            gl.glDeleteVertexArrays(1, ids, 0);
            vaoId = 0;
        }
        if ( programId != 0 ) {
            gl.glDeleteProgram(programId);
            programId = 0;
        }
        solidTextureUploadBuffer = null;
        positionUploadBuffer = null;
        normalUploadBuffer = null;
    }

    private void drawBody(GL4 gl, SimpleBody body, Camera camera)
    {
        Geometry geometry = body.getGeometry();
        if ( geometry == null ) {
            return;
        }

        double[] bounds = geometry.getMinMax();
        if ( bounds == null || bounds.length < 6 ) {
            return;
        }
        Vector3Dd min = new Vector3Dd(bounds[0], bounds[1], bounds[2]);
        Vector3Dd max = new Vector3Dd(bounds[3], bounds[4], bounds[5]);
        setVector3(gl, programId, "boundingBoxMinObject", min);
        setVector3(gl, programId, "boundingBoxMaxObject", max);

        Matrix4x4d modelMatrix = body.getTransformationMatrix();
        Matrix4x4d projection = Jogl4CameraRenderer.activate(gl, camera);
        Matrix4x4d modelViewProjection = projection.multiply(modelMatrix);
        Matrix4x4d modelIt = modelMatrix.invert().transpose();
        setMatrix(gl, programId, "modelViewProjectionLocal", modelViewProjection);
        setMatrix(gl, programId, "modelViewLocal", modelMatrix);
        setMatrix(gl, programId, "modelViewITLocal", modelIt);

        for ( TriangleMesh mesh : meshesOf(geometry) ) {
            MeshFrame frame = buildFrame(mesh);
            if ( frame == null || frame.positions.length == 0 ) {
                continue;
            }
            uploadFrame(gl, frame);
            gl.glBindVertexArray(vaoId);
            gl.glDrawArrays(GL.GL_TRIANGLES, 0, vertexCount);
        }
    }

    private List<TriangleMesh> meshesOf(Geometry geometry)
    {
        List<TriangleMesh> meshes = new ArrayList<>();
        if ( geometry instanceof TriangleMesh triangleMesh ) {
            meshes.add(triangleMesh);
        }
        else if ( geometry instanceof TriangleMeshGroup triangleMeshGroup ) {
            meshes.addAll(triangleMeshGroup.getMeshes());
        }
        return meshes;
    }

    private MeshFrame buildFrame(TriangleMesh mesh)
    {
        int[] indices = mesh.getTriangleIndexes();
        double[] vertices = mesh.getVertexPositions();
        if ( indices == null || vertices == null || indices.length == 0 || vertices.length == 0 ) {
            return null;
        }

        double[] normals = mesh.getVertexNormals();
        boolean hasNormals = normals != null && normals.length >= vertices.length;
        float[] outPositions = new float[indices.length * 3];
        float[] outNormals = new float[indices.length * 3];

        int p = 0;
        int n = 0;
        for ( int idx : indices ) {
            int vp = idx * 3;
            outPositions[p++] = (float)vertices[vp];
            outPositions[p++] = (float)vertices[vp + 1];
            outPositions[p++] = (float)vertices[vp + 2];

            if ( hasNormals ) {
                outNormals[n++] = (float)normals[vp];
                outNormals[n++] = (float)normals[vp + 1];
                outNormals[n++] = (float)normals[vp + 2];
            }
            else {
                outNormals[n++] = 0.0f;
                outNormals[n++] = 0.0f;
                outNormals[n++] = 1.0f;
            }
        }
        return new MeshFrame(outPositions, outNormals);
    }

    private void uploadFrame(GL4 gl, MeshFrame frame)
    {
        vertexCount = frame.positions.length / 3;
        gl.glBindVertexArray(vaoId);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, positionVboId);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, (long)frame.positions.length * Float.BYTES,
            getPositionUploadBuffer(frame.positions), GL2ES2.GL_STREAM_DRAW);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 0, 0L);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, normalVboId);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, (long)frame.normals.length * Float.BYTES,
            getNormalUploadBuffer(frame.normals), GL2ES2.GL_STREAM_DRAW);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(1, 3, GL.GL_FLOAT, false, 0, 0L);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        gl.glBindVertexArray(0);
    }

    private void ensureBuffers(GL4 gl)
    {
        if ( vaoId != 0 ) {
            return;
        }
        int[] arrays = new int[1];
        int[] buffers = new int[2];
        gl.glGenVertexArrays(1, arrays, 0);
        vaoId = arrays[0];
        gl.glGenBuffers(2, buffers, 0);
        positionVboId = buffers[0];
        normalVboId = buffers[1];
    }

    private void ensureProgram(GL4 gl)
    {
        if ( programId != 0 ) {
            return;
        }
        programId = Jogl4ShaderProgramUtil.createProgramFromPaths(
            gl,
            shaderDirectory.resolve("solidTextureVertexShader.glsl"),
            shaderDirectory.resolve("solidTexturePixelShader.glsl"));
    }

    private void ensureSolidTexture(
        GL4 gl,
        byte[] volume,
        int size,
        long revision)
    {
        if ( volume == null || volume.length == 0 || size <= 0 ) {
            return;
        }
        if ( solidTextureId != 0 &&
             uploadedTextureRevision == revision &&
             uploadedTextureSize == size ) {
            return;
        }

        int[] ids = new int[1];
        if ( solidTextureId == 0 ) {
            gl.glGenTextures(1, ids, 0);
            solidTextureId = ids[0];
        }

        ByteBuffer buffer = getSolidTextureUploadBuffer(volume.length);
        buffer.clear();
        buffer.put(volume);
        buffer.flip();

        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL2ES2.GL_TEXTURE_3D, solidTextureId);
        gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
        gl.glTexParameteri(GL2ES2.GL_TEXTURE_3D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL2ES2.GL_TEXTURE_3D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL2ES2.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL2ES2.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL2ES2.GL_TEXTURE_3D, GL2ES2.GL_TEXTURE_WRAP_R, GL.GL_CLAMP_TO_EDGE);
        gl.glTexImage3D(
            GL2ES2.GL_TEXTURE_3D, 0, GL.GL_RGB8, size, size, size, 0,
            GL.GL_RGB, GL.GL_UNSIGNED_BYTE, buffer);
        gl.glBindTexture(GL2ES2.GL_TEXTURE_3D, 0);

        uploadedTextureRevision = revision;
        uploadedTextureSize = size;
    }

    private ByteBuffer getSolidTextureUploadBuffer(int requiredCapacity)
    {
        if ( solidTextureUploadBuffer == null ||
             solidTextureUploadBuffer.capacity() < requiredCapacity ) {
            solidTextureUploadBuffer = Buffers.newDirectByteBuffer(requiredCapacity);
        }
        return solidTextureUploadBuffer;
    }

    private FloatBuffer getPositionUploadBuffer(float[] data)
    {
        positionUploadBuffer = fillUploadBuffer(positionUploadBuffer, data);
        return positionUploadBuffer;
    }

    private FloatBuffer getNormalUploadBuffer(float[] data)
    {
        normalUploadBuffer = fillUploadBuffer(normalUploadBuffer, data);
        return normalUploadBuffer;
    }

    private static FloatBuffer fillUploadBuffer(FloatBuffer buffer, float[] data)
    {
        if ( buffer == null || buffer.capacity() < data.length ) {
            buffer = Buffers.newDirectFloatBuffer(data.length);
        }
        buffer.clear();
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

    private static void setColor(GL4 gl, int programId, String name, ColorRgb value)
    {
        int loc = gl.glGetUniformLocation(programId, name);
        if ( loc >= 0 ) {
            gl.glUniform3f(loc, (float)value.r(), (float)value.g(), (float)value.b());
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

    private void configureClippingPlane(GL4 gl, InfinitePlane clippingPlane)
    {
        if ( clippingPlane == null ) {
            gl.glDisable(GL4.GL_CLIP_DISTANCE0);
            setInt(gl, programId, "clippingPlaneEnabled", 0);
            return;
        }

        gl.glEnable(GL4.GL_CLIP_DISTANCE0);
        setInt(gl, programId, "clippingPlaneEnabled", 1);
        setVector4(gl, programId, "clippingPlaneGlobal", new Vector4Dd(
            clippingPlane.getA(),
            clippingPlane.getB(),
            clippingPlane.getC(),
            clippingPlane.getD()));
    }

    private static void setVector4(GL4 gl, int programId, String name, Vector4Dd value)
    {
        int loc = gl.glGetUniformLocation(programId, name);
        if ( loc >= 0 ) {
            gl.glUniform4f(loc, (float)value.x(), (float)value.y(),
                (float)value.z(), (float)value.w());
        }
    }

    private record MeshFrame(float[] positions, float[] normals) {
    }
}
