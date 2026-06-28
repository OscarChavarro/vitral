package render;

import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import model.OperationMode;
import model.SolidTextureModel;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.geometry.surface.TriangleMeshGroup;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.gui.gizmo.LightGizmoStyle;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.render.jogl.Jogl4CameraRenderer;
import vsdk.toolkit.render.jogl.Jogl4ImageRenderer;
import vsdk.toolkit.render.jogl.Jogl4InfinitePlaneGizmoRenderer;
import vsdk.toolkit.render.jogl.Jogl4LightRenderer;
import vsdk.toolkit.render.jogl.Jogl4MatrixRenderer;
import vsdk.toolkit.render.jogl.Jogl4RayGizmoRenderer;
import vsdk.toolkit.render.jogl.Jogl4RendererConfigurationShaderSelector;
import vsdk.toolkit.render.jogl.Jogl4SolidTextureRenderer;

public class Jogl4DebuggerRenderer implements GLEventListener {
    private static final float SURFACE_POLYGON_OFFSET_FACTOR = 1.0f;
    private static final float SURFACE_POLYGON_OFFSET_UNITS = 1.0f;
    private static final float LINE_POLYGON_OFFSET_FACTOR = -1.0f;
    private static final float LINE_POLYGON_OFFSET_UNITS = -1.0f;

    private final SolidTextureModel model;

    private int vaoId;
    private int positionVboId;
    private int normalVboId;
    private int uvVboId;
    private int vertexCount;
    private Jogl4SolidTextureHudRenderer hudRenderer;
    private Jogl4SolidTexturePlanesRenderer planesRenderer;
    private Jogl4SolidTextureRenderer solidTextureRenderer;

    public Jogl4DebuggerRenderer(SolidTextureModel model, Path shaderDirectory) {
        this.model = model;
        hudRenderer = new Jogl4SolidTextureHudRenderer(model);
        planesRenderer = new Jogl4SolidTexturePlanesRenderer();
        solidTextureRenderer = new Jogl4SolidTextureRenderer(shaderDirectory);
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();
        int[] arrays = new int[1];
        int[] buffers = new int[3];
        gl.glGenVertexArrays(1, arrays, 0);
        vaoId = arrays[0];
        gl.glGenBuffers(3, buffers, 0);
        positionVboId = buffers[0];
        normalVboId = buffers[1];
        uvVboId = buffers[2];
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();

        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glDepthMask(true);
        gl.glDepthFunc(GL4.GL_LESS);
        gl.glDisable(GL4.GL_CULL_FACE);

        gl.glClearColor(0.5f, 0.5f, 0.9f, 1.0f);
        gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);

        acquireGizmoSnapshots();

        if ( model.getOperationMode() == OperationMode.MESH_MODEL ) {
            drawMeshModel(gl);
        }
        else if ( model.getOperationMode() == OperationMode.TEXTURE_2D_STACK ) {
            planesRenderer.draw(gl, model.getTexture2DStack(), model.getCamera(),
                activeClippingPlane());
        }
        drawGizmos(gl);
        if ( model.isHudVisible() ) {
            hudRenderer.draw(gl);
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL4 gl = drawable.getGL().getGL4();
        gl.glViewport(0, 0, width, height);
        model.getCamera().updateViewportResize(width, height);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();
        int[] ids = new int[1];

        Jogl4RendererConfigurationShaderSelector.dispose(gl);
        Jogl4CameraRenderer.dispose(gl);
        Jogl4RayGizmoRenderer.dispose(gl);
        Jogl4InfinitePlaneGizmoRenderer.dispose(gl);
        hudRenderer.dispose(gl);
        planesRenderer.dispose(gl);
        solidTextureRenderer.dispose(gl);

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

    private void drawMeshModel(GL4 gl)
    {
        List<Light> activeLights = model.getLights();
        if ( activeLights == null || activeLights.isEmpty() ) {
            return;
        }

        solidTextureRenderer.draw(
            gl,
            model.getScene(),
            model.getCamera(),
            activeLights,
            model.getSolidTextureVolumeRgb8(),
            model.getSolidTextureSize(),
            model.getSolidTextureRevision(),
            activeClippingPlane());

        for ( Light light : activeLights ) {
            if ( light != null ) {
                Jogl4LightRenderer.draw(gl, light, model.getCamera(), LightGizmoStyle.OMNI_BILLBOARD);
            }
        }

    }

    private void drawGizmos(GL4 gl)
    {
        List<Light> activeLights = model.getLights();

        Jogl4RayGizmoRenderer.draw(gl, model.getRayGizmo(), model.getCamera(), activeLights);

        Jogl4InfinitePlaneGizmoRenderer.draw(gl, model.getInfinitePlaneGizmo(), model.getCamera());
    }

    private void acquireGizmoSnapshots()
    {
        model.getRayGizmo().acquireSnapshot();
        model.getInfinitePlaneGizmo().acquireSnapshot();
    }

    private InfinitePlane activeClippingPlane()
    {
        if ( !model.getInfinitePlaneGizmo().isVisible() ) {
            return null;
        }
        return model.getInfinitePlaneGizmo().getPlane();
    }

    private void drawSimpleBody(
        GL4 gl,
        SimpleBody body,
        Camera camera,
        List<Light> lights,
        RendererConfiguration quality)
    {
        Geometry geometry = body.getGeometry();
        if ( geometry == null ) {
            return;
        }

        List<TriangleMesh> meshes = new ArrayList<>();
        if ( geometry instanceof TriangleMesh triangleMesh ) {
            meshes.add(triangleMesh);
        }
        else if ( geometry instanceof TriangleMeshGroup triangleMeshGroup ) {
            meshes.addAll(triangleMeshGroup.getMeshes());
        }
        else {
            return;
        }

        Matrix4x4d modelMatrix = body.getTransformationMatrix();
        Matrix4x4d projection = Jogl4CameraRenderer.activate(gl, camera);
        Matrix4x4d modelViewProjection = projection.multiply(modelMatrix);
        Matrix4x4d modelIt = modelMatrix.invert().transpose();

        SimpleMaterial material = body.getMaterial();
        if ( material == null ) {
            material = defaultMaterial();
        }

        for ( TriangleMesh mesh : meshes ) {
            MeshFrame frame = buildFrame(mesh, modelMatrix);
            if ( frame == null || frame.positions.length == 0 ) {
                continue;
            }

            uploadFrame(gl, frame);

            Image texture = body.getTexture();
            if ( texture == null ) {
                Image[] textures = mesh.getTextures();
                if ( textures != null && textures.length > 0 ) {
                    texture = textures[0];
                }
            }

            int textureId = 0;
            boolean withTexture = false;
            if ( texture != null && quality.isTextureSet() ) {
                textureId = Jogl4ImageRenderer.activate(gl, texture);
                withTexture = textureId > 0;
            }

            if ( quality.isSurfacesSet() ) {
                int program = Jogl4RendererConfigurationShaderSelector.selectSurfaceShaderProgram(
                    gl, quality, withTexture, false);
                configureProgram(
                    gl, program, modelViewProjection, modelMatrix, modelIt, camera, lights,
                    material, quality, withTexture, textureId);

                gl.glEnable(GL4.GL_POLYGON_OFFSET_FILL);
                gl.glPolygonOffset(SURFACE_POLYGON_OFFSET_FACTOR, SURFACE_POLYGON_OFFSET_UNITS);
                gl.glPolygonMode(GL4.GL_FRONT_AND_BACK, GL4.GL_FILL);
                gl.glDepthMask(true);
                gl.glDepthFunc(GL4.GL_LESS);
                gl.glBindVertexArray(vaoId);
                gl.glDrawArrays(GL4.GL_TRIANGLES, 0, vertexCount);
                gl.glBindVertexArray(0);
                gl.glDisable(GL4.GL_POLYGON_OFFSET_FILL);
                Jogl4RendererConfigurationShaderSelector.deactivateShader(gl);
            }

            if ( quality.isWiresSet() ) {
                RendererConfiguration wireQuality = new RendererConfiguration();
                wireQuality.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
                wireQuality.setTexture(false);
                wireQuality.setBumpMap(false);
                int program = Jogl4RendererConfigurationShaderSelector.selectSurfaceShaderProgram(
                    gl, wireQuality, false, false);
                configureProgram(
                    gl, program, modelViewProjection, modelMatrix, modelIt, camera, lights,
                    whiteWireMaterial(), wireQuality, false, 0);

                gl.glEnable(GL4.GL_POLYGON_OFFSET_LINE);
                gl.glPolygonOffset(LINE_POLYGON_OFFSET_FACTOR, LINE_POLYGON_OFFSET_UNITS);
                gl.glPolygonMode(GL4.GL_FRONT_AND_BACK, GL4.GL_LINE);
                gl.glLineWidth(1.0f);
                gl.glDepthMask(false);
                gl.glDepthFunc(GL4.GL_LEQUAL);
                gl.glBindVertexArray(vaoId);
                gl.glDrawArrays(GL4.GL_TRIANGLES, 0, vertexCount);
                gl.glBindVertexArray(0);
                gl.glDisable(GL4.GL_POLYGON_OFFSET_LINE);
                Jogl4RendererConfigurationShaderSelector.deactivateShader(gl);
            }

            if ( quality.isPointsSet() ) {
                RendererConfiguration pointQuality = new RendererConfiguration();
                pointQuality.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
                pointQuality.setTexture(false);
                pointQuality.setBumpMap(false);
                int program = Jogl4RendererConfigurationShaderSelector.selectSurfaceShaderProgram(
                    gl, pointQuality, false, false);
                configureProgram(
                    gl, program, modelViewProjection, modelMatrix, modelIt, camera, lights,
                    redPointMaterial(), pointQuality, false, 0);

                gl.glPolygonMode(GL4.GL_FRONT_AND_BACK, GL4.GL_FILL);
                gl.glPointSize(4.0f);
                gl.glDepthMask(false);
                gl.glDepthFunc(GL4.GL_LEQUAL);
                gl.glBindVertexArray(vaoId);
                gl.glDrawArrays(GL4.GL_POINTS, 0, vertexCount);
                gl.glBindVertexArray(0);
                Jogl4RendererConfigurationShaderSelector.deactivateShader(gl);
            }

            gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);
            gl.glDepthMask(true);
            gl.glDepthFunc(GL4.GL_LESS);
        }
    }

    private void configureProgram(
        GL4 gl,
        int programId,
        Matrix4x4d modelViewProjection,
        Matrix4x4d model,
        Matrix4x4d modelIt,
        Camera camera,
        List<Light> lights,
        SimpleMaterial material,
        RendererConfiguration quality,
        boolean withTexture,
        int textureId)
    {
        ColorRgb kd = material.getDiffuse();
        Jogl4RendererConfigurationShaderSelector.activateShader(
            gl,
            programId,
            modelViewProjection,
            quality,
            (float)kd.r(),
            (float)kd.g(),
            (float)kd.b());

        setMatrix(gl, programId, "modelViewLocal", model);
        setMatrix(gl, programId, "modelViewITLocal", modelIt);
        setVector3(gl, programId, "cameraPositionGlobal", camera.getPosition());
        int lightCount = 0;
        for ( int i = 0; i < lights.size(); i++ ) {
            Light light = lights.get(i);
            if ( light == null ) {
                continue;
            }
            setVector3(gl, programId, "lightPositionsGlobal[" + lightCount + "]", light.getPosition());
            setVector3(gl, programId, "lightColorsGlobal[" + lightCount + "]", light.getSpecular());
            lightCount++;
        }
        setInt(gl, programId, "numberOfLights", lightCount);
        setVector3(gl, programId, "ambientColor", material.getAmbient());
        setVector3(gl, programId, "diffuseColor", material.getDiffuse());
        setVector3(gl, programId, "specularColor", material.getSpecular());
        setFloat(gl, programId, "phongExponent", (float)material.getPhongExponent());
        setInt(gl, programId, "withTexture", withTexture ? 1 : 0);
        setInt(gl, programId, "withBumpMap", 0);

        if ( withTexture ) {
            gl.glActiveTexture(GL4.GL_TEXTURE0);
            gl.glBindTexture(GL4.GL_TEXTURE_2D, textureId);
        }
    }

    private MeshFrame buildFrame(TriangleMesh mesh, Matrix4x4d modelMatrix)
    {
        int[] indices = mesh.getTriangleIndexes();
        double[] vertices = mesh.getVertexPositions();
        if ( indices == null || vertices == null || indices.length == 0 || vertices.length == 0 ) {
            return null;
        }

        double[] normals = mesh.getVertexNormals();
        double[] uvs = mesh.getVertexUvs();
        boolean hasNormals = normals != null && normals.length >= vertices.length;
        boolean hasUvs = uvs != null && (uvs.length / 2) >= (vertices.length / 3);

        float[] outPositions = new float[indices.length * 3];
        float[] outNormals = new float[indices.length * 3];
        float[] outUvs = new float[indices.length * 2];

        int p = 0;
        int n = 0;
        int t = 0;
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

            if ( hasUvs ) {
                int uv = idx * 2;
                outUvs[t++] = (float)uvs[uv];
                outUvs[t++] = (float)uvs[uv + 1];
            }
            else {
                outUvs[t++] = 0.0f;
                outUvs[t++] = 0.0f;
            }
        }

        return new MeshFrame(outPositions, outNormals, outUvs);
    }

    private static SimpleMaterial defaultMaterial()
    {
        SimpleMaterial m = new SimpleMaterial();
        m = m.withAmbient(new ColorRgb(0.2, 0.2, 0.2));
        m = m.withDiffuse(new ColorRgb(0.8, 0.8, 0.8));
        m = m.withSpecular(new ColorRgb(1.0, 1.0, 1.0));
        m = m.withPhongExponent(32.0);
        return m;
    }

    private static SimpleMaterial whiteWireMaterial()
    {
        SimpleMaterial m = new SimpleMaterial();
        m = m.withAmbient(new ColorRgb(0.0, 0.0, 0.0));
        m = m.withDiffuse(new ColorRgb(1.0, 1.0, 1.0));
        m = m.withSpecular(new ColorRgb(0.0, 0.0, 0.0));
        m = m.withPhongExponent(1.0);
        return m;
    }

    private static SimpleMaterial redPointMaterial()
    {
        SimpleMaterial m = new SimpleMaterial();
        m = m.withAmbient(new ColorRgb(0.0, 0.0, 0.0));
        m = m.withDiffuse(new ColorRgb(1.0, 0.0, 0.0));
        m = m.withSpecular(new ColorRgb(0.0, 0.0, 0.0));
        m = m.withPhongExponent(1.0);
        return m;
    }

    private void uploadFrame(GL4 gl, MeshFrame frame)
    {
        vertexCount = frame.positions.length / 3;

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

    private static void setVector3(GL4 gl, int programId, String name, ColorRgb value)
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

    private static final class MeshFrame {
        private final float[] positions;
        private final float[] normals;
        private final float[] uvs;

        private MeshFrame(float[] positions, float[] normals, float[] uvs) {
            this.positions = positions;
            this.normals = normals;
            this.uvs = uvs;
        }
    }
}
