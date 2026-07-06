package vsdk.toolkit.render.jogl;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.surface.Md2Mesh;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.media.Image;

public class Jogl4Md2MeshRenderer extends Jogl4Renderer
{
    private static final float SURFACE_POLYGON_OFFSET_FACTOR = 1.0f;
    private static final float SURFACE_POLYGON_OFFSET_UNITS = 1.0f;
    private static final float LINE_POLYGON_OFFSET_FACTOR = -1.0f;
    private static final float LINE_POLYGON_OFFSET_UNITS = -1.0f;

    private static int vaoId;
    private static int positionVboId;
    private static int normalVboId;
    private static int uvVboId;
    private static int vertexCount;
    private static int textureId;
    private static Image textureOwner;
    private static boolean initialized;

    public static void initGL(GL4 gl, Md2Mesh md2Mesh)
    {
        if ( initialized ) {
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
        initialized = true;
        textureId = 0;
        textureOwner = null;
        vertexCount = 0;
    }

    public static void draw(
        GL4 gl,
        Md2Mesh md2Mesh,
        Camera camera,
        Light light,
        RendererConfiguration quality,
        double xTranslation)
    {
        if ( md2Mesh == null || camera == null || light == null || quality == null ) {
            return;
        }
        initGL(gl, md2Mesh);

        InterpolatedFrame frame = buildFrame(md2Mesh);
        if ( frame == null || frame.positions.length == 0 ) {
            return;
        }
        uploadFrame(gl, frame);

        textureId = updateTexture(gl, md2Mesh);
        boolean hasTexture = textureId > 0;
        boolean useTexture = quality.isTextureSet() && hasTexture;

        Matrix4x4d model = new Matrix4x4d();
        model = model.translation(xTranslation, 0.0, 0.0);
        Matrix4x4d projection = camera.calculateProjectionMatrix();
        Matrix4x4d mvp = projection.multiply(model);
        Matrix4x4d modelIT = model.invert().transpose();

        SimpleMaterial material = new SimpleMaterial();
        material = material.withAmbient(new ColorRgb(0.2, 0.2, 0.2));
        material = material.withDiffuse(new ColorRgb(0.8, 0.8, 0.8));
        material = material.withSpecular(new ColorRgb(1.0, 1.0, 1.0));
        material = material.withPhongExponent(32.0);

        if ( quality.isSurfacesSet() ) {
            int program = Jogl4RendererConfigurationShaderSelector
                .selectSurfaceShaderProgram(gl, quality, useTexture, false);
            configureProgram(gl, program, mvp, model, modelIT, camera, light,
                material, quality, useTexture);
            gl.glEnable(GL4.GL_DEPTH_TEST);
            gl.glDepthMask(true);
            gl.glDepthFunc(GL4.GL_LESS);
            gl.glEnable(GL4.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset(SURFACE_POLYGON_OFFSET_FACTOR, SURFACE_POLYGON_OFFSET_UNITS);
            gl.glDisable(GL4.GL_CULL_FACE);
            gl.glPolygonMode(GL4.GL_FRONT_AND_BACK, GL4.GL_FILL);
            render(gl);
            gl.glDisable(GL4.GL_POLYGON_OFFSET_FILL);
            Jogl4RendererConfigurationShaderSelector.deactivateShader(gl);
        }

        if ( quality.isWiresSet() ) {
            RendererConfiguration wireQuality = new RendererConfiguration();
            wireQuality.setTexture(false);
            wireQuality.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
            int program = Jogl4RendererConfigurationShaderSelector
                .selectSurfaceShaderProgram(gl, wireQuality, false, false);
            SimpleMaterial wireMaterial = new SimpleMaterial(material);
            wireMaterial = wireMaterial.withAmbient(new ColorRgb(0, 0, 0));
            wireMaterial = wireMaterial.withDiffuse(new ColorRgb(1, 1, 1));
            wireMaterial = wireMaterial.withSpecular(new ColorRgb(0, 0, 0));
            configureProgram(gl, program, mvp, model, modelIT, camera, light,
                wireMaterial, wireQuality, false);
            gl.glEnable(GL4.GL_DEPTH_TEST);
            gl.glDepthMask(false);
            gl.glDepthFunc(GL4.GL_LEQUAL);
            gl.glEnable(GL4.GL_POLYGON_OFFSET_LINE);
            gl.glPolygonOffset(LINE_POLYGON_OFFSET_FACTOR, LINE_POLYGON_OFFSET_UNITS);
            gl.glDisable(GL4.GL_CULL_FACE);
            gl.glPolygonMode(GL4.GL_FRONT_AND_BACK, GL4.GL_LINE);
            gl.glLineWidth(1.0f);
            render(gl);
            gl.glDisable(GL4.GL_POLYGON_OFFSET_LINE);
            Jogl4RendererConfigurationShaderSelector.deactivateShader(gl);
            gl.glPolygonMode(GL4.GL_FRONT_AND_BACK, GL4.GL_FILL);
        }

        if ( quality.isPointsSet() ) {
            RendererConfiguration pointQuality = new RendererConfiguration();
            pointQuality.setTexture(false);
            pointQuality.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
            int program = Jogl4RendererConfigurationShaderSelector
                .selectSurfaceShaderProgram(gl, pointQuality, false, false);
            SimpleMaterial pointMaterial = new SimpleMaterial(material);
            pointMaterial = pointMaterial.withAmbient(new ColorRgb(0, 0, 0));
            pointMaterial = pointMaterial.withDiffuse(new ColorRgb(1, 0, 0));
            pointMaterial = pointMaterial.withSpecular(new ColorRgb(0, 0, 0));
            configureProgram(gl, program, mvp, model, modelIT, camera, light,
                pointMaterial, pointQuality, false);
            gl.glEnable(GL4.GL_DEPTH_TEST);
            gl.glDepthMask(false);
            gl.glDepthFunc(GL4.GL_LEQUAL);
            gl.glDisable(GL4.GL_CULL_FACE);
            gl.glPointSize(4.0f);
            gl.glBindVertexArray(vaoId);
            gl.glDrawArrays(GL4.GL_POINTS, 0, vertexCount);
            gl.glBindVertexArray(0);
            Jogl4RendererConfigurationShaderSelector.deactivateShader(gl);
        }

        gl.glDepthMask(true);
        gl.glDepthFunc(GL4.GL_LESS);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);
    }

    public static void dispose(GL4 gl)
    {
        if ( textureOwner != null ) {
            Jogl4ImageRenderer.unload(gl, textureOwner);
            textureOwner = null;
            textureId = 0;
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
        vertexCount = 0;
        initialized = false;
    }

    private static void configureProgram(
        GL4 gl,
        int program,
        Matrix4x4d mvp,
        Matrix4x4d model,
        Matrix4x4d modelIT,
        Camera camera,
        Light light,
        SimpleMaterial material,
        RendererConfiguration quality,
        boolean withTexture)
    {
        ColorRgb kd = material.getDiffuse();
        Jogl4RendererConfigurationShaderSelector.activateShader(
            gl,
            program,
            mvp,
            quality,
            (float)kd.r(),
            (float)kd.g(),
            (float)kd.b());

        setMatrix(gl, program, "modelViewLocal", model);
        setMatrix(gl, program, "modelViewITLocal", modelIT);
        setVector3(gl, program, "cameraPositionGlobal", camera.getPosition());
        setVector3(gl, program, "lightPositionsGlobal[0]", light.getPosition());
        setVector3(gl, program, "lightColorsGlobal[0]", light.getEmission());
        setInt(gl, program, "numberOfLights", 1);
        setVector3(gl, program, "ambientColor", material.getAmbient());
        setVector3(gl, program, "diffuseColor", material.getDiffuse());
        setVector3(gl, program, "specularColor", material.getSpecular());
        setFloat(gl, program, "phongExponent", (float)material.getPhongExponent());
        setInt(gl, program, "withTexture", withTexture ? 1 : 0);
        setInt(gl, program, "withBumpMap", 0);

        if ( withTexture ) {
            gl.glActiveTexture(GL4.GL_TEXTURE0);
            gl.glBindTexture(GL4.GL_TEXTURE_2D, textureId);
        }
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

    private static int updateTexture(GL4 gl, Md2Mesh md2Mesh)
    {
        if ( md2Mesh.skins == null || md2Mesh.skins.length == 0 || md2Mesh.skins[0] == null ) {
            textureOwner = null;
            textureId = 0;
            return 0;
        }
        Image current = md2Mesh.skins[0];
        if ( current != textureOwner ) {
            textureOwner = current;
            textureId = Jogl4ImageRenderer.activate(gl, current);
        }
        return textureId;
    }

    private static void uploadFrame(GL4 gl, InterpolatedFrame frame)
    {
        vertexCount = frame.positions.length / 3;
        gl.glBindVertexArray(vaoId);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, positionVboId);
        gl.glBufferData(
            GL4.GL_ARRAY_BUFFER,
            (long)frame.positions.length * Float.BYTES,
            toBuffer(frame.positions),
            GL4.GL_STREAM_DRAW);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0L);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, normalVboId);
        gl.glBufferData(
            GL4.GL_ARRAY_BUFFER,
            (long)frame.normals.length * Float.BYTES,
            toBuffer(frame.normals),
            GL4.GL_STREAM_DRAW);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 0, 0L);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, uvVboId);
        gl.glBufferData(
            GL4.GL_ARRAY_BUFFER,
            (long)frame.uvs.length * Float.BYTES,
            toBuffer(frame.uvs),
            GL4.GL_STREAM_DRAW);
        gl.glEnableVertexAttribArray(2);
        gl.glVertexAttribPointer(2, 2, GL4.GL_FLOAT, false, 0, 0L);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
        gl.glBindVertexArray(0);
    }

    private static FloatBuffer toBuffer(float[] src)
    {
        return Buffers.newDirectFloatBuffer(src);
    }

    private static void render(GL4 gl)
    {
        gl.glBindVertexArray(vaoId);
        gl.glDrawArrays(GL4.GL_TRIANGLES, 0, vertexCount);
        gl.glBindVertexArray(0);
    }

    private static InterpolatedFrame buildFrame(Md2Mesh md2Mesh)
    {
        if ( md2Mesh.frameVertices.isEmpty() || md2Mesh.frameNormalIndices.isEmpty() ) {
            return null;
        }

        float[][] normalsTable = Md2Mesh.anorms;
        short[] animStartEnd = new short[2];
        md2Mesh.returnStartEndAnim(md2Mesh.getCurrentAnimationInd(), animStartEnd);
        float frameTimeSeg = md2Mesh.getFrameTimeSeg();
        float elapsedTimeSeg = md2Mesh.getElapsedTimeSeg();
        float t = elapsedTimeSeg / frameTimeSeg;
        int length = (animStartEnd[1] - animStartEnd[0] + 1);
        if ( length <= 0 ) {
            length = 1;
        }
        int frame = ((int)t % length) + animStartEnd[0];
        t = t - (int)t;
        int nextFrame = (frame == animStartEnd[1]) ? animStartEnd[0] : frame + 1;

        float[] verts = md2Mesh.frameVertices.get(frame);
        float[] nextVerts = md2Mesh.frameVertices.get(nextFrame);
        short[] normalIdx = md2Mesh.frameNormalIndices.get(frame);
        short[] nextNormalIdx = md2Mesh.frameNormalIndices.get(nextFrame);

        ArrayList<Float> positionsList = new ArrayList<Float>();
        ArrayList<Float> normalsList = new ArrayList<Float>();
        ArrayList<Float> uvsList = new ArrayList<Float>();

        if ( !md2Mesh.glCmdVertIndexStrip.isEmpty() || !md2Mesh.glCmdVertIndexFan.isEmpty() ) {
            for ( int i = 0; i < md2Mesh.glCmdVertIndexStrip.size(); i++ ) {
                int[] strip = md2Mesh.glCmdVertIndexStrip.get(i);
                float[] stripUv = md2Mesh.glCmdTexCoordsStrip.get(i);
                for ( int j = 2; j < strip.length; j++ ) {
                    int ia;
                    int ib;
                    int ic = j;
                    if ( (j & 1) == 0 ) {
                        ia = j - 2;
                        ib = j - 1;
                    }
                    else {
                        ia = j - 1;
                        ib = j - 2;
                    }
                    appendVertex(positionsList, normalsList, uvsList,
                        strip[ia], stripUv[ia * 2], stripUv[ia * 2 + 1],
                        verts, nextVerts, normalIdx, nextNormalIdx, normalsTable, t);
                    appendVertex(positionsList, normalsList, uvsList,
                        strip[ib], stripUv[ib * 2], stripUv[ib * 2 + 1],
                        verts, nextVerts, normalIdx, nextNormalIdx, normalsTable, t);
                    appendVertex(positionsList, normalsList, uvsList,
                        strip[ic], stripUv[ic * 2], stripUv[ic * 2 + 1],
                        verts, nextVerts, normalIdx, nextNormalIdx, normalsTable, t);
                }
            }
            for ( int i = 0; i < md2Mesh.glCmdVertIndexFan.size(); i++ ) {
                int[] fan = md2Mesh.glCmdVertIndexFan.get(i);
                float[] fanUv = md2Mesh.glCmdTexCoordsFan.get(i);
                for ( int j = 2; j < fan.length; j++ ) {
                    appendVertex(positionsList, normalsList, uvsList,
                        fan[0], fanUv[0], fanUv[1],
                        verts, nextVerts, normalIdx, nextNormalIdx, normalsTable, t);
                    appendVertex(positionsList, normalsList, uvsList,
                        fan[j - 1], fanUv[(j - 1) * 2], fanUv[(j - 1) * 2 + 1],
                        verts, nextVerts, normalIdx, nextNormalIdx, normalsTable, t);
                    appendVertex(positionsList, normalsList, uvsList,
                        fan[j], fanUv[j * 2], fanUv[j * 2 + 1],
                        verts, nextVerts, normalIdx, nextNormalIdx, normalsTable, t);
                }
            }
        }
        else {
            int triCount = md2Mesh.numTriangles;
            for ( int i = 0; i < triCount; i++ ) {
                for ( int j = 0; j < 3; j++ ) {
                    int vIndex = md2Mesh.triangles[i * 2][j];
                    int tIndex = md2Mesh.triangles[i * 2 + 1][j];
                    int ti2 = tIndex * 2;
                    appendVertex(positionsList, normalsList, uvsList,
                        vIndex, md2Mesh.texCoords[ti2], md2Mesh.texCoords[ti2 + 1],
                        verts, nextVerts, normalIdx, nextNormalIdx, normalsTable, t);
                }
            }
        }

        float[] positions = toFloatArray(positionsList);
        float[] normals = toFloatArray(normalsList);
        float[] uvs = toFloatArray(uvsList);

        InterpolatedFrame out = new InterpolatedFrame();
        out.positions = positions;
        out.normals = normals;
        out.uvs = uvs;
        return out;
    }

    private static void appendVertex(
        ArrayList<Float> positionsList,
        ArrayList<Float> normalsList,
        ArrayList<Float> uvsList,
        int vIndex,
        float u,
        float v,
        float[] verts,
        float[] nextVerts,
        short[] normalIdx,
        short[] nextNormalIdx,
        float[][] normalsTable,
        float t)
    {
        int vi3 = vIndex * 3;
        positionsList.add(verts[vi3] + t * (nextVerts[vi3] - verts[vi3]));
        positionsList.add(verts[vi3 + 1] + t * (nextVerts[vi3 + 1] - verts[vi3 + 1]));
        positionsList.add(verts[vi3 + 2] + t * (nextVerts[vi3 + 2] - verts[vi3 + 2]));

        float[] normal = normalsTable[normalIdx[vIndex]];
        float[] normalN = normalsTable[nextNormalIdx[vIndex]];
        normalsList.add(normal[0] + t * (normalN[0] - normal[0]));
        normalsList.add(normal[1] + t * (normalN[1] - normal[1]));
        normalsList.add(normal[2] + t * (normalN[2] - normal[2]));

        uvsList.add(u);
        uvsList.add(1.0f - v);
    }

    private static float[] toFloatArray(ArrayList<Float> list)
    {
        float[] out = new float[list.size()];
        for ( int i = 0; i < list.size(); i++ ) {
            out[i] = list.get(i);
        }
        return out;
    }

    private static final class InterpolatedFrame {
        private float[] positions;
        private float[] normals;
        private float[] uvs;
    }
}
