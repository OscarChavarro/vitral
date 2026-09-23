package vsdk.toolkit.render.jogl;

import com.jogamp.opengl.GL2GL3;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.logging.Logger;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.light.PointLight;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.material.MicroFacetedMaterial;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.media.RGBImageUncompressed;

/**
Renders any triangle mesh (given as non indexed arrays of positions, normals,
texture coordinates, tangents and binormals) honoring every bit of a
`RendererConfiguration` (surfaces, wires, points, normals, bounding volume,
texture, bump map and shading type), with the GLSL programs selected by
`Jogl4RendererConfigurationShaderSelector`. It is the common flow shared by the
renderers of each geometry (see `Jogl4SphereRenderer` and
`Jogl4GeometryRenderer`).

The OpenGL resources of a mesh are created the first time it is drawn, and
must be released with `release` while the context that created them is
current.
*/
public class Jogl4MeshRenderer extends Jogl4Renderer {
    private static final int DEFAULT_SLICES = 32;

    // Every Phong/Gouraud/Cook-Torrance shader declares a `sTexture` sampler
    // even when the material has no diffuse texture (`withTexture` is set to
    // 0, and the shader does not sample it then). Some GL drivers (i.e. the
    // one on MacOSX) still require a complete texture object bound to the
    // unit a sampler uniform points to, or they log a "texture unloadable"
    // diagnostic and substitute a zero texture (correct color, noisy log).
    // This 1x1 white texture is bound to unit 0 instead, whenever the
    // material has none, so nothing is ever unbound while such a shader is
    // active.
    private static int dummyTextureId;
    private static final int DEFAULT_STACKS = 16;
    private static final float SURFACE_POLYGON_OFFSET_FACTOR = 1.0f;
    private static final float SURFACE_POLYGON_OFFSET_UNITS = 1.0f;
    private static final float LINE_POLYGON_OFFSET_FACTOR = -1.0f;
    private static final float LINE_POLYGON_OFFSET_UNITS = -1.0f;
    private static final float VERTEX_NORMAL_SCALE = 0.10f;
    private static final float TRIANGLE_NORMAL_SCALE = 0.12f;
    private static final float NORMAL_START_EPSILON = 0.002f;
    private static final float NORMAL_LINE_DEPTH_BIAS_NDC = -1.0e-4f;
    // [BLIN1978b] bump scales used by both shader and raytracer examples.
    private static final Vector3Dd DEFAULT_BUMP_SCALE = new Vector3Dd(1.0, 1.0, 1.0);
    private static final float[] VERTEX_NORMAL_COLOR = new float[] { 1.0f, 1.0f, 0.0f };
    private static final float[] TRIANGLE_NORMAL_COLOR = new float[] { 0.0f, 1.0f, 1.0f };

    /// Size of the light arrays of the GLSL programs
    public static final int MAX_LIGHTS = 8;
    private static boolean tooManyLightsReported = false;

    private Jogl4MeshRenderer() {
    }

    private static void reportTooManyLights(int count)
    {
        if ( tooManyLightsReported ) {
            return;
        }
        tooManyLightsReported = true;
        Logger.reportMessage(null, VSDK.WARNING, "Jogl4MeshRenderer",
            "Scene has " + count + " lights, but shaders use only the first " + MAX_LIGHTS);
    }

    /**
    A triangle mesh and the OpenGL objects that hold it in the GPU.
    */
    public static final class Mesh {
        private float[] positions;
        private float[] normals;
        private float[] uvs;
        private float[] tangents;
        private float[] biNormals;
        private int vertexCount;
        /// Vertices of the sides the geometry defines; the rest (if any) are
        /// back sides added to see open surfaces from both sides
        private int frontVertexCount;
        private double characteristicSize;

        private int vaoId;
        private int positionVboId;
        private int normalVboId;
        private int uvVboId;
        private int tangentVboId;
        private int biNormalVboId;
        private boolean uploaded;

        private float[] vertexNormalLinePositions;
        private float[] vertexNormalLineColors;
        private float[] triangleNormalLinePositions;
        private float[] triangleNormalLineColors;

        /**
        @param positions x,y,z of each vertex; three consecutive vertices
        make a triangle
        @param normals x,y,z of the normal of each vertex
        @param uvs u,v of each vertex
        @param tangents x,y,z of the tangent of each vertex
        @param biNormals x,y,z of the binormal of each vertex
        @param characteristicSize approximate size of the mesh, used to scale
        the normal overlays
        */
        public Mesh(
            float[] positions,
            float[] normals,
            float[] uvs,
            float[] tangents,
            float[] biNormals,
            double characteristicSize)
        {
            this.positions = positions;
            this.normals = normals;
            this.uvs = uvs;
            this.tangents = tangents;
            this.biNormals = biNormals;
            this.vertexCount = positions.length / 3;
            this.frontVertexCount = vertexCount;
            this.characteristicSize = characteristicSize;
        }

        /**
        Marks the vertices after the first `count` ones as back sides of open
        surfaces: they are drawn as surfaces and wires, but their points and
        normals are not shown again.
        @param count number of vertices of the front sides
        */
        void setFrontVertexCount(int count)
        {
            frontVertexCount = Math.max(0, Math.min(count, vertexCount));
        }
    }

    /**
    Draws a mesh lit by a single light.
    See the overload taking a list of lights.

    @param gl OpenGL context
    @param mesh mesh to draw
    @param geometry geometry the mesh comes from, used for the bounding volume
    @param camera camera that views the mesh
    @param light light of the scene, or null to use a light at the camera
    @param material material of the surfaces
    @param quality bits of rendering configuration
    @param textureMap texture of the surfaces, or null
    @param normalMap normal (bump) map of the surfaces, or null
    @param localTransform transformation from the mesh space to world space
    */
    public static void draw(
        GL4 gl,
        Mesh mesh,
        Geometry geometry,
        Camera camera,
        Light light,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap,
        Matrix4x4d localTransform)
    {
        List<Light> lights = null;

        if ( light != null ) {
            lights = new ArrayList<Light>();
            lights.add(light);
        }
        draw(gl, mesh, geometry, camera, lights, material, quality, textureMap,
            normalMap, localTransform);
    }

    /**
    Draws a mesh, with the passes selected by the configuration.

    @param gl OpenGL context
    @param mesh mesh to draw
    @param geometry geometry the mesh comes from, used for the bounding volume
    @param camera camera that views the mesh
    @param lights lights of the scene (at most `MAX_LIGHTS` are used), or null
    or empty to use a light at the camera
    @param material material of the surfaces
    @param quality bits of rendering configuration
    @param textureMap texture of the surfaces, or null
    @param normalMap normal (bump) map of the surfaces, or null
    @param localTransform transformation from the mesh space to world space
    */
    public static void draw(
        GL4 gl,
        Mesh mesh,
        Geometry geometry,
        Camera camera,
        List<Light> lights,
        SimpleMaterial material,
        RendererConfiguration quality,
        RGBImageUncompressed textureMap,
        RGBImageUncompressed normalMap,
        Matrix4x4d localTransform)
    {
        if ( mesh == null || camera == null || material == null || quality == null ) {
            return;
        }
        if ( lights == null || lights.isEmpty() ) {
            lights = new ArrayList<Light>();
            lights.add(new PointLight(camera.getPosition(), new ColorRgb(1, 1, 1)));
        }

        upload(gl, mesh);

        boolean hasTexture = textureMap != null;
        boolean hasNormalMap = normalMap != null;
        int textureId = hasTexture ? Jogl4ImageRenderer.activate(gl, textureMap) : 0;
        int normalMapId = (hasNormalMap && quality.isBumpMapSet()) ? Jogl4ImageRenderer.activate(gl, normalMap) : 0;

        Matrix4x4d localTransformNotNull = (localTransform != null)
            ? localTransform
            : Matrix4x4d.identityMatrix();
        drawPasses(gl, mesh, geometry, camera, lights, material, quality,
            textureId, normalMapId, hasTexture, localTransformNotNull);
    }

    private static void drawPasses(
        GL4 gl,
        Mesh mesh,
        Geometry geometry,
        Camera camera,
        List<Light> lights,
        SimpleMaterial material,
        RendererConfiguration quality,
        int textureId,
        int normalMapId,
        boolean hasTexture,
        Matrix4x4d localTransform)
    {
        Matrix4x4d modelViewProjection = camera.calculateProjectionMatrix().multiply(localTransform);
        Matrix4x4d modelViewITLocal = localTransform.invert().transpose();

        if ( quality.isSurfacesSet() ) {
            int programId = Jogl4RendererConfigurationShaderSelector.selectSurfaceShaderProgram(
                gl,
                quality,
                hasTexture,
                normalMapId > 0);
            configureProgram(
                gl,
                programId,
                modelViewProjection,
                localTransform,
                modelViewITLocal,
                camera,
                lights,
                material,
                quality,
                textureId,
                normalMapId);

            // Pass 1: Surfaces. Push slightly backwards to avoid z-fighting
            // with overlay passes (wireframe/points).
            gl.glEnable(GL.GL_DEPTH_TEST);
            gl.glDepthMask(true);
            gl.glDepthFunc(GL.GL_LESS);
            gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset(SURFACE_POLYGON_OFFSET_FACTOR, SURFACE_POLYGON_OFFSET_UNITS);
            gl.glEnable(GL.GL_CULL_FACE);
            gl.glCullFace(GL.GL_BACK);
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);
            renderMesh(gl, mesh);
            gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);

            Jogl4RendererConfigurationShaderSelector.deactivateShader(gl);
        }

        if ( quality.isWiresSet() ) {
            RendererConfiguration wireQuality = new RendererConfiguration();
            wireQuality.setTexture(false);
            wireQuality.setUseVertexColors(false);
            wireQuality.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);

            int wireProgram = Jogl4RendererConfigurationShaderSelector.selectSurfaceShaderProgram(
                gl,
                wireQuality,
                false,
                false);

            SimpleMaterial wireMaterial = new SimpleMaterial(material);
            wireMaterial = wireMaterial.withDiffuse(new ColorRgb(1, 1, 1));
            wireMaterial = wireMaterial.withSpecular(new ColorRgb(0, 0, 0));
            wireMaterial = wireMaterial.withAmbient(new ColorRgb(0, 0, 0));

            configureProgram(
                gl,
                wireProgram,
                modelViewProjection,
                localTransform,
                modelViewITLocal,
                camera,
                lights,
                wireMaterial,
                wireQuality,
                0,
                0);

            // Pass 2: Wires. Keep depth test but bias in front of surfaces.
            gl.glEnable(GL.GL_DEPTH_TEST);
            gl.glDepthMask(false);
            gl.glDepthFunc(GL.GL_LEQUAL);
            gl.glEnable(GL2GL3.GL_POLYGON_OFFSET_LINE);
            gl.glPolygonOffset(LINE_POLYGON_OFFSET_FACTOR, LINE_POLYGON_OFFSET_UNITS);
            gl.glDisable(GL.GL_CULL_FACE);
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);
            gl.glLineWidth(1.0f);
            renderMesh(gl, mesh);
            gl.glDisable(GL2GL3.GL_POLYGON_OFFSET_LINE);

            Jogl4RendererConfigurationShaderSelector.deactivateShader(gl);
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);
        }

        if ( quality.isPointsSet() ) {
            RendererConfiguration pointQuality = new RendererConfiguration();
            pointQuality.setTexture(false);
            pointQuality.setUseVertexColors(false);
            pointQuality.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);

            int pointsProgram = Jogl4RendererConfigurationShaderSelector.selectSurfaceShaderProgram(
                gl,
                pointQuality,
                false,
                false);

            SimpleMaterial pointMaterial = new SimpleMaterial(material);
            pointMaterial = pointMaterial.withAmbient(new ColorRgb(0, 0, 0));
            pointMaterial = pointMaterial.withDiffuse(new ColorRgb(1, 0, 0)); // #ff0000
            pointMaterial = pointMaterial.withSpecular(new ColorRgb(0, 0, 0));

            configureProgram(
                gl,
                pointsProgram,
                modelViewProjection,
                localTransform,
                modelViewITLocal,
                camera,
                lights,
                pointMaterial,
                pointQuality,
                0,
                0);

            // Pass 3: Points. Draw last with depth test against surfaces
            // (and no depth writes), so points appear above wireframe.
            gl.glEnable(GL.GL_DEPTH_TEST);
            gl.glDepthMask(false);
            gl.glDepthFunc(GL.GL_LEQUAL);
            gl.glDisable(GL.GL_CULL_FACE);
            gl.glPointSize(4.0f);
            gl.glBindVertexArray(mesh.vaoId);
            gl.glDrawArrays(GL.GL_POINTS, 0, mesh.frontVertexCount);
            gl.glBindVertexArray(0);

            Jogl4RendererConfigurationShaderSelector.deactivateShader(gl);
        }

        if ( quality.isNormalsSet() || quality.isTrianglesNormalsSet() ) {
            drawNormalOverlays(gl, mesh, quality, modelViewProjection);
        }

        if ( quality.isBoundingVolumeSet() ) {
            Jogl4MinMaxRenderer.draw(gl, geometry, camera, localTransform);
        }

        if ( quality.isSelectionCornersSet() ) {
            Jogl4SelectionCornersRenderer.draw(gl, geometry, camera, localTransform);
        }

        gl.glDepthMask(true);
        gl.glDepthFunc(GL.GL_LESS);
        gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
        }

    /**
    Releases the OpenGL objects of a mesh. PRE: the context that created them
    is current.
    @param gl OpenGL context
    @param mesh mesh to release
    */
    public static void release(GL4 gl, Mesh mesh)
    {
        int[] tmp = new int[1];

        if ( mesh == null || !mesh.uploaded ) {
            return;
        }
        int[] buffers = new int[] {
            mesh.positionVboId, mesh.normalVboId, mesh.uvVboId,
            mesh.tangentVboId, mesh.biNormalVboId
        };
        gl.glDeleteBuffers(buffers.length, buffers, 0);
        tmp[0] = mesh.vaoId;
        gl.glDeleteVertexArrays(1, tmp, 0);
        mesh.uploaded = false;
        mesh.vaoId = 0;
    }

    /**
    Releases the resources shared by all the meshes.
    @param gl OpenGL context
    */
    public static void dispose(GL4 gl)
    {
        Jogl4MinMaxRenderer.dispose(gl);
        if ( dummyTextureId > 0 ) {
            gl.glDeleteTextures(1, new int[] {dummyTextureId}, 0);
            dummyTextureId = 0;
        }
    }

    /**
    @return the id of the 1x1 white texture, created the first time it is
    needed
    */
    private static int ensureDummyTexture(GL4 gl)
    {
        if ( dummyTextureId > 0 ) {
            return dummyTextureId;
        }

        int[] tmp = new int[1];

        gl.glGenTextures(1, tmp, 0);
        dummyTextureId = tmp[0];

        gl.glBindTexture(GL4.GL_TEXTURE_2D, dummyTextureId);
        gl.glTexImage2D(
            GL4.GL_TEXTURE_2D,
            0,
            GL4.GL_RGB8,
            1,
            1,
            0,
            GL4.GL_RGB,
            GL4.GL_UNSIGNED_BYTE,
            Buffers.newDirectByteBuffer(new byte[] {(byte)255, (byte)255, (byte)255}));
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_NEAREST);
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_NEAREST);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);

        return dummyTextureId;
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
        RendererConfiguration quality,
        int textureId,
        int normalMapId)
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

        setMatrix(gl, programId, "modelViewLocal", modelViewLocal);
        setMatrix(gl, programId, "modelViewITLocal", modelViewITLocal);

        setVector3(gl, programId, "cameraPositionGlobal", camera.getPosition());
        int lightCount = 0;
        for ( Light light : lights ) {
            if ( light == null ) {
                continue;
            }
            if ( lightCount >= MAX_LIGHTS ) {
                reportTooManyLights(lights.size());
                break;
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
        setVector3(gl, programId, "bumpScale", DEFAULT_BUMP_SCALE);
        setFloat(gl, programId, "phongExponent", (float)material.getPhongExponent());
        configureMicroFacetUniforms(gl, programId, material);
        setInt(gl, programId, "withTexture", (quality.isTextureSet() && textureId > 0) ? 1 : 0);
        setInt(
            gl,
            programId,
            "withBumpMap",
            (quality.isBumpMapSet() && normalMapId > 0) ? 1 : 0);

        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, textureId > 0 ? textureId : ensureDummyTexture(gl));

        if ( normalMapId > 0 ) {
            gl.glActiveTexture(GL.GL_TEXTURE1);
            gl.glBindTexture(GL.GL_TEXTURE_2D, normalMapId);
            gl.glActiveTexture(GL.GL_TEXTURE0);
        }
    }

    private static void setMatrix(GL4 gl, int programId, String name, Matrix4x4d matrix)
    {
        int loc = gl.glGetUniformLocation(programId, name);
        if ( loc >= 0 ) {
            gl.glUniformMatrix4fv(loc, 1, false, Jogl4MatrixRenderer.toColumnMajorFloatArray(matrix), 0);
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

    private static void configureMicroFacetUniforms(GL4 gl, int programId, SimpleMaterial material)
    {
        float roughness = 0.35f;
        float alpha = roughness * roughness;
        ColorRgb fresnelF0 = material.getSpecular();
        float kd = 1.0f;
        float ks = 1.0f;
        int fresnelModel = MicroFacetedMaterial.FRESNEL_MODEL_SCHLICK;
        int ndfModel = MicroFacetedMaterial.NDF_MODEL_BECKMANN;
        int geometryModel = MicroFacetedMaterial.GEOMETRY_MODEL_SMITH;
        ColorRgb eta = new ColorRgb(1.5, 1.5, 1.5);
        ColorRgb kappa = new ColorRgb(0.0, 0.0, 0.0);

        if ( material instanceof MicroFacetedMaterial microFacetedMaterial ) {
            roughness = (float)microFacetedMaterial.getRoughness();
            alpha = (float)microFacetedMaterial.getAlpha();
            fresnelF0 = microFacetedMaterial.getFresnelF0();
            kd = (float)microFacetedMaterial.getKd();
            ks = (float)microFacetedMaterial.getKs();
            fresnelModel = microFacetedMaterial.getFresnelModel();
            ndfModel = microFacetedMaterial.getNdfModel();
            geometryModel = microFacetedMaterial.getGeometryModel();
            eta = microFacetedMaterial.getEta();
            kappa = microFacetedMaterial.getKappa();
        }

        setFloat(gl, programId, "cookRoughness", roughness);
        setFloat(gl, programId, "cookAlpha", alpha);
        setFloat(gl, programId, "cookKd", kd);
        setFloat(gl, programId, "cookKs", ks);
        setVector3(gl, programId, "cookF0", fresnelF0);
        setVector3(gl, programId, "cookEta", eta);
        setVector3(gl, programId, "cookKappa", kappa);
        setInt(gl, programId, "cookFresnelModel", fresnelModel);
        setInt(gl, programId, "cookNdfModel", ndfModel);
        setInt(gl, programId, "cookGeometryModel", geometryModel);
    }

    private static void renderMesh(GL4 gl, Mesh mesh)
    {
        gl.glBindVertexArray(mesh.vaoId);
        gl.glDrawArrays(GL.GL_TRIANGLES, 0, mesh.vertexCount);
        gl.glBindVertexArray(0);
    }

    private static void upload(GL4 gl, Mesh mesh)
    {
        if ( mesh.uploaded ) {
            return;
        }
        int[] tmp = new int[1];

        gl.glGenVertexArrays(1, tmp, 0);
        mesh.vaoId = tmp[0];
        gl.glGenBuffers(1, tmp, 0);
        mesh.positionVboId = tmp[0];
        gl.glGenBuffers(1, tmp, 0);
        mesh.normalVboId = tmp[0];
        gl.glGenBuffers(1, tmp, 0);
        mesh.uvVboId = tmp[0];
        gl.glGenBuffers(1, tmp, 0);
        mesh.tangentVboId = tmp[0];
        gl.glGenBuffers(1, tmp, 0);
        mesh.biNormalVboId = tmp[0];

        gl.glBindVertexArray(mesh.vaoId);
        uploadFloatBuffer(gl, mesh.positionVboId, 0, 3, mesh.positions);
        uploadFloatBuffer(gl, mesh.normalVboId, 1, 3, mesh.normals);
        uploadFloatBuffer(gl, mesh.uvVboId, 2, 2, mesh.uvs);
        uploadFloatBuffer(gl, mesh.tangentVboId, 3, 3, mesh.tangents);
        uploadFloatBuffer(gl, mesh.biNormalVboId, 4, 3, mesh.biNormals);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        gl.glBindVertexArray(0);

        mesh.uploaded = true;
    }

    private static void uploadFloatBuffer(
        GL4 gl,
        int bufferId,
        int attribIndex,
        int coordinatesSize,
        float[] data)
    {
        FloatBuffer direct = Buffers.newDirectFloatBuffer(data);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId);
        gl.glBufferData(
                GL.GL_ARRAY_BUFFER,
            (long)data.length * Float.BYTES,
            direct,
                GL.GL_STATIC_DRAW);
        gl.glEnableVertexAttribArray(attribIndex);
        gl.glVertexAttribPointer(attribIndex, coordinatesSize, GL.GL_FLOAT, false, 0, 0L);
    }

    private static void drawNormalOverlays(
        GL4 gl,
        Mesh mesh,
        RendererConfiguration quality,
        Matrix4x4d modelViewProjection)
    {
        if ( mesh.positions == null || mesh.normals == null ) {
            return;
        }

        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthMask(false);
        gl.glDepthFunc(GL.GL_LEQUAL);
        gl.glDisable(GL.GL_CULL_FACE);

        if ( quality.isNormalsSet() ) {
            if ( mesh.vertexNormalLinePositions == null || mesh.vertexNormalLineColors == null ) {
                float length = (float)Math.max(1e-6, mesh.characteristicSize) * VERTEX_NORMAL_SCALE;
                float epsilon = (float)Math.max(1e-6, mesh.characteristicSize) * NORMAL_START_EPSILON;
                mesh.vertexNormalLinePositions = buildVertexNormalLinePositions(mesh, length, epsilon);
                mesh.vertexNormalLineColors = buildUniformColors(
                    mesh.vertexNormalLinePositions.length / 3,
                    VERTEX_NORMAL_COLOR);
            }
            Jogl4LineRenderer.drawLines(
                gl,
                modelViewProjection,
                mesh.vertexNormalLinePositions,
                mesh.vertexNormalLineColors,
                1.0f,
                NORMAL_LINE_DEPTH_BIAS_NDC);
        }

        if ( quality.isTrianglesNormalsSet() ) {
            if ( mesh.triangleNormalLinePositions == null || mesh.triangleNormalLineColors == null ) {
                float length = (float)Math.max(1e-6, mesh.characteristicSize) * TRIANGLE_NORMAL_SCALE;
                float epsilon = (float)Math.max(1e-6, mesh.characteristicSize) * NORMAL_START_EPSILON;
                mesh.triangleNormalLinePositions = buildTriangleNormalLinePositions(mesh, length, epsilon);
                mesh.triangleNormalLineColors = buildUniformColors(
                    mesh.triangleNormalLinePositions.length / 3,
                    TRIANGLE_NORMAL_COLOR);
            }
            Jogl4LineRenderer.drawLines(
                gl,
                modelViewProjection,
                mesh.triangleNormalLinePositions,
                mesh.triangleNormalLineColors,
                1.0f,
                NORMAL_LINE_DEPTH_BIAS_NDC);
        }
    }

    private static float[] buildVertexNormalLinePositions(Mesh mesh, float length, float epsilon)
    {
        int vertexCountLocal = mesh.frontVertexCount;
        float[] lines = new float[vertexCountLocal * 2 * 3];
        int out = 0;
        for ( int i = 0; i < vertexCountLocal; i++ ) {
            int base = i * 3;
            float px = mesh.positions[base];
            float py = mesh.positions[base + 1];
            float pz = mesh.positions[base + 2];
            float nx = mesh.normals[base];
            float ny = mesh.normals[base + 1];
            float nz = mesh.normals[base + 2];

            float sx = px + nx * epsilon;
            float sy = py + ny * epsilon;
            float sz = pz + nz * epsilon;
            float ex = sx + nx * length;
            float ey = sy + ny * length;
            float ez = sz + nz * length;

            lines[out++] = sx;
            lines[out++] = sy;
            lines[out++] = sz;
            lines[out++] = ex;
            lines[out++] = ey;
            lines[out++] = ez;
        }
        return lines;
    }

    private static float[] buildTriangleNormalLinePositions(Mesh mesh, float length, float epsilon)
    {
        int triangleCount = mesh.frontVertexCount / 3;
        float[] lines = new float[triangleCount * 2 * 3];
        int out = 0;

        for ( int tri = 0; tri < triangleCount; tri++ ) {
            int base = tri * 9;
            float p0x = mesh.positions[base];
            float p0y = mesh.positions[base + 1];
            float p0z = mesh.positions[base + 2];
            float p1x = mesh.positions[base + 3];
            float p1y = mesh.positions[base + 4];
            float p1z = mesh.positions[base + 5];
            float p2x = mesh.positions[base + 6];
            float p2y = mesh.positions[base + 7];
            float p2z = mesh.positions[base + 8];

            float cx = (p0x + p1x + p2x) / 3.0f;
            float cy = (p0y + p1y + p2y) / 3.0f;
            float cz = (p0z + p1z + p2z) / 3.0f;

            float ux = p1x - p0x;
            float uy = p1y - p0y;
            float uz = p1z - p0z;
            float vx = p2x - p0x;
            float vy = p2y - p0y;
            float vz = p2z - p0z;

            float nx = uy * vz - uz * vy;
            float ny = uz * vx - ux * vz;
            float nz = ux * vy - uy * vx;

            float norm = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
            if ( norm <= 1e-12f ) {
                nx = 0.0f;
                ny = 0.0f;
                nz = 1.0f;
            }
            else {
                nx /= norm;
                ny /= norm;
                nz /= norm;
            }

            float outward = nx * cx + ny * cy + nz * cz;
            if ( outward < 0.0f ) {
                nx = -nx;
                ny = -ny;
                nz = -nz;
            }

            float sx = cx + nx * epsilon;
            float sy = cy + ny * epsilon;
            float sz = cz + nz * epsilon;
            float ex = sx + nx * length;
            float ey = sy + ny * length;
            float ez = sz + nz * length;

            lines[out++] = sx;
            lines[out++] = sy;
            lines[out++] = sz;
            lines[out++] = ex;
            lines[out++] = ey;
            lines[out++] = ez;
        }

        return lines;
    }

    private static float[] buildUniformColors(int vertexCountLocal, float[] rgb)
    {
        float[] colors = new float[vertexCountLocal * 3];
        for ( int i = 0; i < vertexCountLocal; i++ ) {
            int base = i * 3;
            colors[base] = rgb[0];
            colors[base + 1] = rgb[1];
            colors[base + 2] = rgb[2];
        }
        return colors;
    }
}
