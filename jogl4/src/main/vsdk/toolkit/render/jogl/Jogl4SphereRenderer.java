package vsdk.toolkit.render.jogl;

import java.nio.FloatBuffer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.media.RGBImage;

public class Jogl4SphereRenderer extends Jogl4Renderer {
    private static final int DEFAULT_SLICES = 32;
    private static final int DEFAULT_STACKS = 16;
    private static final float SURFACE_POLYGON_OFFSET_FACTOR = 1.0f;
    private static final float SURFACE_POLYGON_OFFSET_UNITS = 1.0f;
    private static final float LINE_POLYGON_OFFSET_FACTOR = -1.0f;
    private static final float LINE_POLYGON_OFFSET_UNITS = -1.0f;
    private static final float VERTEX_NORMAL_SCALE = 0.10f;
    private static final float TRIANGLE_NORMAL_SCALE = 0.12f;
    private static final float NORMAL_START_EPSILON = 0.002f;
    private static final float NORMAL_LINE_DEPTH_BIAS_NDC = -1.0e-4f;
    private static final float[] VERTEX_NORMAL_COLOR = new float[] { 1.0f, 1.0f, 0.0f };
    private static final float[] TRIANGLE_NORMAL_COLOR = new float[] { 0.0f, 1.0f, 1.0f };

    private static int vaoId;
    private static int positionVboId;
    private static int normalVboId;
    private static int uvVboId;
    private static int tangentVboId;
    private static int binormalVboId;
    private static int vertexCount;
    private static boolean initialized;

    private static double meshRadius;
    private static int meshSlices;
    private static int meshStacks;
    private static float[] meshPositionsHost;
    private static float[] meshNormalsHost;
    private static float[] vertexNormalLinePositions;
    private static float[] vertexNormalLineColors;
    private static float[] triangleNormalLinePositions;
    private static float[] triangleNormalLineColors;

    public static void draw(
        GL4 gl,
        Sphere sphere,
        Camera camera,
        Light light,
        Material material,
        RendererConfiguration quality,
        RGBImage textureMap,
        RGBImage normalMap)
    {
        draw(
            gl,
            sphere,
            camera,
            light,
            material,
            quality,
            textureMap,
            normalMap,
            DEFAULT_SLICES,
            DEFAULT_STACKS);
    }

    public static void draw(
        GL4 gl,
        Sphere sphere,
        Camera camera,
        Light light,
        Material material,
        RendererConfiguration quality,
        RGBImage textureMap,
        RGBImage normalMap,
        int slices,
        int stacks)
    {
        draw(
            gl,
            sphere,
            camera,
            light,
            material,
            quality,
            textureMap,
            normalMap,
            Matrix4x4.identityMatrix(),
            slices,
            stacks);
    }

    public static void draw(
        GL4 gl,
        Sphere sphere,
        Camera camera,
        Light light,
        Material material,
        RendererConfiguration quality,
        RGBImage textureMap,
        RGBImage normalMap,
        Matrix4x4 modelViewLocal,
        int slices,
        int stacks)
    {
        if ( sphere == null || camera == null || light == null || material == null || quality == null ) {
            return;
        }

        ensureMesh(gl, sphere, slices, stacks);

        boolean hasTexture = textureMap != null;
        boolean hasNormalMap = normalMap != null;
        int textureId = hasTexture ? Jogl4ImageRenderer.activate(gl, textureMap) : 0;
        int normalMapId = (hasNormalMap && quality.isBumpMapSet()) ? Jogl4ImageRenderer.activate(gl, normalMap) : 0;

        Matrix4x4 localTransform = (modelViewLocal != null)
            ? modelViewLocal
            : Matrix4x4.identityMatrix();
        Matrix4x4 modelViewProjection = camera.calculateProjectionMatrix().multiply(localTransform);
        Matrix4x4 modelViewITLocal = localTransform.invert().transpose();

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
                light,
                material,
                quality,
                textureId,
                normalMapId);

            // Pass 1: Surfaces. Push slightly backwards to avoid z-fighting
            // with overlay passes (wireframe/points).
            gl.glEnable(GL4.GL_DEPTH_TEST);
            gl.glDepthMask(true);
            gl.glDepthFunc(GL4.GL_LESS);
            gl.glEnable(GL4.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset(SURFACE_POLYGON_OFFSET_FACTOR, SURFACE_POLYGON_OFFSET_UNITS);
            gl.glEnable(GL4.GL_CULL_FACE);
            gl.glCullFace(GL4.GL_BACK);
            gl.glPolygonMode(GL4.GL_FRONT_AND_BACK, GL4.GL_FILL);
            renderMesh(gl);
            gl.glDisable(GL4.GL_POLYGON_OFFSET_FILL);

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

            Material wireMaterial = new Material(material);
            wireMaterial.setDiffuse(new ColorRgb(1, 1, 1));
            wireMaterial.setSpecular(new ColorRgb(0, 0, 0));
            wireMaterial.setAmbient(new ColorRgb(0, 0, 0));

            configureProgram(
                gl,
                wireProgram,
                modelViewProjection,
                localTransform,
                modelViewITLocal,
                camera,
                light,
                wireMaterial,
                wireQuality,
                0,
                0);

            // Pass 2: Wires. Keep depth test but bias in front of surfaces.
            gl.glEnable(GL4.GL_DEPTH_TEST);
            gl.glDepthMask(false);
            gl.glDepthFunc(GL4.GL_LEQUAL);
            gl.glEnable(GL4.GL_POLYGON_OFFSET_LINE);
            gl.glPolygonOffset(LINE_POLYGON_OFFSET_FACTOR, LINE_POLYGON_OFFSET_UNITS);
            gl.glDisable(GL4.GL_CULL_FACE);
            gl.glPolygonMode(GL4.GL_FRONT_AND_BACK, GL4.GL_LINE);
            gl.glLineWidth(1.0f);
            renderMesh(gl);
            gl.glDisable(GL4.GL_POLYGON_OFFSET_LINE);

            Jogl4RendererConfigurationShaderSelector.deactivateShader(gl);
            gl.glPolygonMode(GL4.GL_FRONT_AND_BACK, GL4.GL_FILL);
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

            Material pointMaterial = new Material(material);
            pointMaterial.setAmbient(new ColorRgb(0, 0, 0));
            pointMaterial.setDiffuse(new ColorRgb(1, 0, 0)); // #ff0000
            pointMaterial.setSpecular(new ColorRgb(0, 0, 0));

            configureProgram(
                gl,
                pointsProgram,
                modelViewProjection,
                localTransform,
                modelViewITLocal,
                camera,
                light,
                pointMaterial,
                pointQuality,
                0,
                0);

            // Pass 3: Points. Draw last with depth test against surfaces
            // (and no depth writes), so points appear above wireframe.
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

        if ( quality.isNormalsSet() || quality.isTrianglesNormalsSet() ) {
            drawNormalOverlays(gl, quality, modelViewProjection);
        }

        if ( quality.isBoundingVolumeSet() ) {
            Jogl4MinMaxRenderer.draw(gl, sphere, camera, localTransform);
        }

        gl.glDepthMask(true);
        gl.glDepthFunc(GL4.GL_LESS);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);
    }

    public static void dispose(GL4 gl)
    {
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
        if ( tangentVboId != 0 ) {
            tmp[0] = tangentVboId;
            gl.glDeleteBuffers(1, tmp, 0);
            tangentVboId = 0;
        }
        if ( binormalVboId != 0 ) {
            tmp[0] = binormalVboId;
            gl.glDeleteBuffers(1, tmp, 0);
            binormalVboId = 0;
        }
        if ( vaoId != 0 ) {
            tmp[0] = vaoId;
            gl.glDeleteVertexArrays(1, tmp, 0);
            vaoId = 0;
        }

        vertexCount = 0;
        initialized = false;
        meshRadius = 0;
        meshSlices = 0;
        meshStacks = 0;
        meshPositionsHost = null;
        meshNormalsHost = null;
        vertexNormalLinePositions = null;
        vertexNormalLineColors = null;
        triangleNormalLinePositions = null;
        triangleNormalLineColors = null;

        Jogl4MinMaxRenderer.dispose(gl);
    }

    private static void configureProgram(
        GL4 gl,
        int programId,
        Matrix4x4 modelViewProjection,
        Matrix4x4 modelViewLocal,
        Matrix4x4 modelViewITLocal,
        Camera camera,
        Light light,
        Material material,
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
            (float)kd.r,
            (float)kd.g,
            (float)kd.b);

        setMatrix(gl, programId, "modelViewLocal", modelViewLocal);
        setMatrix(gl, programId, "modelViewITLocal", modelViewITLocal);

        setVector3(gl, programId, "cameraPositionGlobal", camera.getPosition());
        setVector3(gl, programId, "lightPositionsGlobal[0]", light.getPosition());

        ColorRgb lightColor = light.getSpecular();
        setVector3(gl, programId, "lightColorsGlobal[0]", lightColor);
        setInt(gl, programId, "numberOfLights", 1);

        setVector3(gl, programId, "ambientColor", material.getAmbient());
        setVector3(gl, programId, "diffuseColor", material.getDiffuse());
        setVector3(gl, programId, "specularColor", material.getSpecular());
        setFloat(gl, programId, "phongExponent", (float)material.getPhongExponent());
        setInt(gl, programId, "withTexture", (quality.isTextureSet() && textureId > 0) ? 1 : 0);

        if ( textureId > 0 ) {
            gl.glActiveTexture(GL4.GL_TEXTURE0);
            gl.glBindTexture(GL4.GL_TEXTURE_2D, textureId);
        }

        if ( normalMapId > 0 ) {
            gl.glActiveTexture(GL4.GL_TEXTURE1);
            gl.glBindTexture(GL4.GL_TEXTURE_2D, normalMapId);
            gl.glActiveTexture(GL4.GL_TEXTURE0);
        }
    }

    private static void setMatrix(GL4 gl, int programId, String name, Matrix4x4 matrix)
    {
        int loc = gl.glGetUniformLocation(programId, name);
        if ( loc >= 0 ) {
            gl.glUniformMatrix4fv(loc, 1, false, Jogl4MatrixRenderer.toColumnMajorFloatArray(matrix), 0);
        }
    }

    private static void setVector3(GL4 gl, int programId, String name, Vector3D value)
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
            gl.glUniform3f(loc, (float)value.r, (float)value.g, (float)value.b);
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

    private static void renderMesh(GL4 gl)
    {
        gl.glBindVertexArray(vaoId);
        gl.glDrawArrays(GL4.GL_TRIANGLES, 0, vertexCount);
        gl.glBindVertexArray(0);
    }

    private static void ensureMesh(GL4 gl, Sphere sphere, int requestedSlices, int requestedStacks)
    {
        int slices = Math.max(12, requestedSlices);
        int stacks = Math.max(8, requestedStacks);
        double radius = sphere.getRadius();

        boolean needsRebuild = !initialized ||
                               Math.abs(meshRadius - radius) > 1e-9 ||
                               meshSlices != slices ||
                               meshStacks != stacks;

        if ( !needsRebuild ) {
            return;
        }

        MeshData mesh = buildMesh(sphere, slices, stacks);
        uploadMesh(gl, mesh);
        meshPositionsHost = mesh.positions;
        meshNormalsHost = mesh.normals;
        vertexNormalLinePositions = null;
        vertexNormalLineColors = null;
        triangleNormalLinePositions = null;
        triangleNormalLineColors = null;

        initialized = true;
        meshRadius = radius;
        meshSlices = slices;
        meshStacks = stacks;
    }

    private static MeshData buildMesh(Sphere sphere, int slices, int stacks)
    {
        int triangles = (stacks - 1) * slices * 2;
        int vertices = triangles * 3;

        float[] positions = new float[vertices * 3];
        float[] normals = new float[vertices * 3];
        float[] uvs = new float[vertices * 2];
        float[] tangents = new float[vertices * 3];
        float[] binormals = new float[vertices * 3];

        int posIndex = 0;
        int uvIndex = 0;

        for ( int i = 0; i < stacks - 1; i++ ) {
            double t0 = i / (double)(stacks - 1);
            double t1 = (i + 1) / (double)(stacks - 1);
            double phi0 = Math.PI * t0 - Math.PI / 2;
            double phi1 = Math.PI * t1 - Math.PI / 2;

            for ( int j = 0; j < slices; j++ ) {
                double s0 = j / (double)slices;
                double s1 = (j + 1) / (double)slices;
                double theta0 = 2 * Math.PI * s0;
                double theta1 = 2 * Math.PI * s1;

                posIndex = addVertex(sphere, theta0, phi0, s0, t0, positions, normals, tangents, binormals, posIndex);
                uvIndex = addUv(s0, t0, uvs, uvIndex);

                posIndex = addVertex(sphere, theta0, phi1, s0, t1, positions, normals, tangents, binormals, posIndex);
                uvIndex = addUv(s0, t1, uvs, uvIndex);

                posIndex = addVertex(sphere, theta1, phi1, s1, t1, positions, normals, tangents, binormals, posIndex);
                uvIndex = addUv(s1, t1, uvs, uvIndex);

                posIndex = addVertex(sphere, theta0, phi0, s0, t0, positions, normals, tangents, binormals, posIndex);
                uvIndex = addUv(s0, t0, uvs, uvIndex);

                posIndex = addVertex(sphere, theta1, phi1, s1, t1, positions, normals, tangents, binormals, posIndex);
                uvIndex = addUv(s1, t1, uvs, uvIndex);

                posIndex = addVertex(sphere, theta1, phi0, s1, t0, positions, normals, tangents, binormals, posIndex);
                uvIndex = addUv(s1, t0, uvs, uvIndex);
            }
        }

        MeshData mesh = new MeshData();
        mesh.positions = positions;
        mesh.normals = normals;
        mesh.uvs = uvs;
        mesh.tangents = tangents;
        mesh.binormals = binormals;
        mesh.vertexCount = vertices;
        return mesh;
    }

    private static int addVertex(
        Sphere sphere,
        double theta,
        double phi,
        double s,
        double t,
        float[] positions,
        float[] normals,
        float[] tangents,
        float[] binormals,
        int index)
    {
        Vector3D p = sphere.spherePosition(theta, phi);
        Vector3D n = sphere.sphereNormal(theta, phi);
        Vector3D tangent = sphere.sphereTangent(theta, phi);
        Vector3D binormal = sphere.sphereBinormal(theta, phi);

        positions[index] = (float)p.x();
        normals[index] = (float)n.x();
        tangents[index] = (float)tangent.x();
        binormals[index] = (float)binormal.x();
        index++;

        positions[index] = (float)p.y();
        normals[index] = (float)n.y();
        tangents[index] = (float)tangent.y();
        binormals[index] = (float)binormal.y();
        index++;

        positions[index] = (float)p.z();
        normals[index] = (float)n.z();
        tangents[index] = (float)tangent.z();
        binormals[index] = (float)binormal.z();
        index++;

        return index;
    }

    private static int addUv(double s, double t, float[] uvs, int index)
    {
        uvs[index++] = (float)(1.0 - s);
        uvs[index++] = (float)t;
        return index;
    }

    private static void uploadMesh(GL4 gl, MeshData mesh)
    {
        if ( vaoId == 0 ) {
            int[] tmp = new int[1];

            gl.glGenVertexArrays(1, tmp, 0);
            vaoId = tmp[0];

            gl.glGenBuffers(1, tmp, 0);
            positionVboId = tmp[0];

            gl.glGenBuffers(1, tmp, 0);
            normalVboId = tmp[0];

            gl.glGenBuffers(1, tmp, 0);
            uvVboId = tmp[0];

            gl.glGenBuffers(1, tmp, 0);
            tangentVboId = tmp[0];

            gl.glGenBuffers(1, tmp, 0);
            binormalVboId = tmp[0];
        }

        gl.glBindVertexArray(vaoId);

        uploadFloatBuffer(gl, positionVboId, 0, 3, mesh.positions);
        uploadFloatBuffer(gl, normalVboId, 1, 3, mesh.normals);
        uploadFloatBuffer(gl, uvVboId, 2, 2, mesh.uvs);
        uploadFloatBuffer(gl, tangentVboId, 3, 3, mesh.tangents);
        uploadFloatBuffer(gl, binormalVboId, 4, 3, mesh.binormals);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
        gl.glBindVertexArray(0);

        vertexCount = mesh.vertexCount;
    }

    private static void uploadFloatBuffer(
        GL4 gl,
        int bufferId,
        int attribIndex,
        int coordSize,
        float[] data)
    {
        FloatBuffer direct = Buffers.newDirectFloatBuffer(data);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, bufferId);
        gl.glBufferData(
            GL4.GL_ARRAY_BUFFER,
            (long)data.length * Float.BYTES,
            direct,
            GL4.GL_STATIC_DRAW);
        gl.glEnableVertexAttribArray(attribIndex);
        gl.glVertexAttribPointer(attribIndex, coordSize, GL4.GL_FLOAT, false, 0, 0L);
    }

    private static void drawNormalOverlays(
        GL4 gl,
        RendererConfiguration quality,
        Matrix4x4 modelViewProjection)
    {
        if ( meshPositionsHost == null || meshNormalsHost == null ) {
            return;
        }

        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glDepthMask(false);
        gl.glDepthFunc(GL4.GL_LEQUAL);
        gl.glDisable(GL4.GL_CULL_FACE);

        if ( quality.isNormalsSet() ) {
            if ( vertexNormalLinePositions == null || vertexNormalLineColors == null ) {
                float length = (float)Math.max(1e-6, meshRadius) * VERTEX_NORMAL_SCALE;
                float epsilon = (float)Math.max(1e-6, meshRadius) * NORMAL_START_EPSILON;
                vertexNormalLinePositions = buildVertexNormalLinePositions(length, epsilon);
                vertexNormalLineColors = buildUniformColors(
                    vertexNormalLinePositions.length / 3,
                    VERTEX_NORMAL_COLOR);
            }
            Jogl4LineRenderer.drawLines(
                gl,
                modelViewProjection,
                vertexNormalLinePositions,
                vertexNormalLineColors,
                1.0f,
                NORMAL_LINE_DEPTH_BIAS_NDC);
        }

        if ( quality.isTrianglesNormalsSet() ) {
            if ( triangleNormalLinePositions == null || triangleNormalLineColors == null ) {
                float length = (float)Math.max(1e-6, meshRadius) * TRIANGLE_NORMAL_SCALE;
                float epsilon = (float)Math.max(1e-6, meshRadius) * NORMAL_START_EPSILON;
                triangleNormalLinePositions = buildTriangleNormalLinePositions(length, epsilon);
                triangleNormalLineColors = buildUniformColors(
                    triangleNormalLinePositions.length / 3,
                    TRIANGLE_NORMAL_COLOR);
            }
            Jogl4LineRenderer.drawLines(
                gl,
                modelViewProjection,
                triangleNormalLinePositions,
                triangleNormalLineColors,
                1.0f,
                NORMAL_LINE_DEPTH_BIAS_NDC);
        }
    }

    private static float[] buildVertexNormalLinePositions(float length, float epsilon)
    {
        int vertexCountLocal = meshPositionsHost.length / 3;
        float[] lines = new float[vertexCountLocal * 2 * 3];
        int out = 0;
        for ( int i = 0; i < vertexCountLocal; i++ ) {
            int base = i * 3;
            float px = meshPositionsHost[base];
            float py = meshPositionsHost[base + 1];
            float pz = meshPositionsHost[base + 2];
            float nx = meshNormalsHost[base];
            float ny = meshNormalsHost[base + 1];
            float nz = meshNormalsHost[base + 2];

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

    private static float[] buildTriangleNormalLinePositions(float length, float epsilon)
    {
        int triangleCount = meshPositionsHost.length / 9;
        float[] lines = new float[triangleCount * 2 * 3];
        int out = 0;

        for ( int tri = 0; tri < triangleCount; tri++ ) {
            int base = tri * 9;
            float p0x = meshPositionsHost[base];
            float p0y = meshPositionsHost[base + 1];
            float p0z = meshPositionsHost[base + 2];
            float p1x = meshPositionsHost[base + 3];
            float p1y = meshPositionsHost[base + 4];
            float p1z = meshPositionsHost[base + 5];
            float p2x = meshPositionsHost[base + 6];
            float p2y = meshPositionsHost[base + 7];
            float p2z = meshPositionsHost[base + 8];

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

    private static class MeshData {
        float[] positions;
        float[] normals;
        float[] uvs;
        float[] tangents;
        float[] binormals;
        int vertexCount;
    }
}
