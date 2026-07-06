package vsdk.toolkit.render.jogl.polyhedralBoundedSolid;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;
import com.jogamp.opengl.glu.GLUtessellatorCallbackAdapter;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.linealAlgebra.Vector4Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidFaceValidator;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidGeometricValidator;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.render.jogl.Jogl4LightRenderer;
import vsdk.toolkit.render.jogl.Jogl4MatrixRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;
import vsdk.toolkit.render.jogl.Jogl4RendererConfigurationShaderSelector;
import vsdk.toolkit.render.jogl.Jogl4ShaderProgramUtil;
import vsdk.toolkit.render.jogl.Jogl4SimpleMaterialRenderer;

public class Jogl4PolyhedralBoundedSolidRenderer extends Jogl4Renderer
{
    private static final float SURFACE_POLYGON_OFFSET_FACTOR = 2.0f;
    private static final float SURFACE_POLYGON_OFFSET_UNITS = 2.0f;
    private static final double VERTEX_KEY_QUANTIZATION = 1.0e6;

    private static boolean initialized;
    private static int meshVaoId;
    private static int meshPositionVboId;
    private static int meshNormalVboId;
    private static int meshUvVboId;
    private static int colorVaoId;
    private static int colorPositionVboId;
    private static int colorDataVboId;
    private static int colorProgramId;

    public static void draw(
        GL4 gl,
        PolyhedralBoundedSolid solid,
        Camera camera,
        RendererConfiguration quality,
        Matrix4x4d modelMatrix)
    {
        if ( gl == null ) {
            return;
        }
        Matrix4x4d local = modelMatrix != null
            ? modelMatrix
            : Matrix4x4d.identityMatrix();
        Matrix4x4d mvp = camera.calculateProjectionMatrix().multiply(local);
        draw(gl, solid, camera, quality, local, mvp);
    }

    public static void drawDebugFaceBoundary(
        GL4 gl,
        PolyhedralBoundedSolid solid,
        int faceIndex,
        Matrix4x4d modelViewProjection)
    {
        Jogl4PolyhedralBoundedSolidDebugRenderer.drawDebugFaceBoundary(gl,
            solid, faceIndex, modelViewProjection);
    }

    public static void drawDebugFace(
        GL4 gl,
        PolyhedralBoundedSolid solid,
        int faceIndex,
        Matrix4x4d modelMatrix,
        Matrix4x4d modelViewProjection,
        Camera camera)
    {
        Jogl4PolyhedralBoundedSolidDebugRenderer.drawDebugFace(gl, solid,
            faceIndex, modelMatrix, modelViewProjection, camera);
    }

    public static void drawDebugEdges(
        GL4 gl,
        PolyhedralBoundedSolid solid,
        Camera camera,
        int edgeIndex,
        Matrix4x4d modelViewProjection)
    {
        Jogl4PolyhedralBoundedSolidDebugRenderer.drawDebugEdges(gl, solid,
            camera, edgeIndex, modelViewProjection);
    }

    private static void draw(
        GL4 gl,
        PolyhedralBoundedSolid solid,
        Camera camera,
        RendererConfiguration quality,
        Matrix4x4d modelMatrix,
        Matrix4x4d modelViewProjection)
    {
        if ( solid == null || quality == null || camera == null ||
             modelMatrix == null || modelViewProjection == null ) {
            return;
        }

        ensureInitialized(gl);

        Matrix4x4d modelViewITLocal = modelMatrix.invert().transpose();
        boolean smoothNormals =
            quality.getShadingType() != RendererConfiguration.SHADING_TYPE_FLAT &&
            quality.getShadingType() != RendererConfiguration.SHADING_TYPE_NOLIGHT;
        MeshData mesh = buildMesh(solid, smoothNormals, quality);
        SimpleMaterial material = Jogl4SimpleMaterialRenderer.getActiveMaterial();
        List<Light> activeLights = Jogl4LightRenderer.getActiveLights();
        Vector3Dd cameraPosition = camera.getPosition();

        if ( quality.isSurfacesSet() && mesh.vertexCount > 0 ) {
            gl.glEnable(GL4.GL_DEPTH_TEST);
            gl.glDepthMask(true);
            gl.glDepthFunc(GL4.GL_LESS);
            gl.glEnable(GL4.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset(SURFACE_POLYGON_OFFSET_FACTOR,
                SURFACE_POLYGON_OFFSET_UNITS);
            if ( material.isDoubleSided() ) {
                gl.glDisable(GL4.GL_CULL_FACE);
            }
            else {
                gl.glEnable(GL4.GL_CULL_FACE);
                gl.glCullFace(GL4.GL_BACK);
            }

            if ( quality.getShadingType() ==
                 RendererConfiguration.SHADING_TYPE_FLAT ) {
                FlatShadedMesh flatSurfaces = buildFlatShadedSurfaceTriangles(mesh,
                    modelMatrix, modelViewITLocal, material, activeLights,
                    cameraPosition);
                if ( flatSurfaces.positions.length > 0 ) {
                    drawColoredPrimitives(gl, modelViewProjection,
                        flatSurfaces.positions, flatSurfaces.colors,
                        GL4.GL_TRIANGLES, 1.0f, 0.0f);
                }
            }
            else {
                int programId = Jogl4RendererConfigurationShaderSelector
                    .selectSurfaceShaderProgram(gl, quality, false, false);
                configureSurfaceProgram(gl, programId, modelViewProjection,
                    modelMatrix, modelViewITLocal, material, activeLights,
                    quality, cameraPosition);
                renderMesh(gl, mesh, GL4.GL_TRIANGLES);
                Jogl4RendererConfigurationShaderSelector.deactivateShader(gl);
            }

            gl.glDisable(GL4.GL_POLYGON_OFFSET_FILL);
        }

        Jogl4PolyhedralBoundedSolidDebugRenderer.drawDebugOverlays(gl, solid,
            camera, quality, modelViewProjection);
    }

    static void ensureInitialized(GL4 gl)
    {
        if ( initialized ) {
            return;
        }

        colorProgramId = Jogl4ShaderProgramUtil.createProgramFromFiles(gl,
            "lineVertexShader.glsl", "linePixelShader.glsl");

        int[] ids = new int[1];

        gl.glGenVertexArrays(1, ids, 0);
        meshVaoId = ids[0];
        gl.glGenBuffers(1, ids, 0);
        meshPositionVboId = ids[0];
        gl.glGenBuffers(1, ids, 0);
        meshNormalVboId = ids[0];
        gl.glGenBuffers(1, ids, 0);
        meshUvVboId = ids[0];

        gl.glGenVertexArrays(1, ids, 0);
        colorVaoId = ids[0];
        gl.glGenBuffers(1, ids, 0);
        colorPositionVboId = ids[0];
        gl.glGenBuffers(1, ids, 0);
        colorDataVboId = ids[0];

        initialized = true;
    }

    public static void release(GL4 gl)
    {
        if ( !initialized ) {
            return;
        }

        int[] ids = new int[1];

        if ( meshPositionVboId != 0 ) {
            ids[0] = meshPositionVboId;
            gl.glDeleteBuffers(1, ids, 0);
            meshPositionVboId = 0;
        }
        if ( meshNormalVboId != 0 ) {
            ids[0] = meshNormalVboId;
            gl.glDeleteBuffers(1, ids, 0);
            meshNormalVboId = 0;
        }
        if ( meshUvVboId != 0 ) {
            ids[0] = meshUvVboId;
            gl.glDeleteBuffers(1, ids, 0);
            meshUvVboId = 0;
        }
        if ( colorPositionVboId != 0 ) {
            ids[0] = colorPositionVboId;
            gl.glDeleteBuffers(1, ids, 0);
            colorPositionVboId = 0;
        }
        if ( colorDataVboId != 0 ) {
            ids[0] = colorDataVboId;
            gl.glDeleteBuffers(1, ids, 0);
            colorDataVboId = 0;
        }
        if ( meshVaoId != 0 ) {
            ids[0] = meshVaoId;
            gl.glDeleteVertexArrays(1, ids, 0);
            meshVaoId = 0;
        }
        if ( colorVaoId != 0 ) {
            ids[0] = colorVaoId;
            gl.glDeleteVertexArrays(1, ids, 0);
            colorVaoId = 0;
        }
        if ( colorProgramId != 0 ) {
            gl.glDeleteProgram(colorProgramId);
            colorProgramId = 0;
        }

        initialized = false;
    }

    static void configureSurfaceProgram(
        GL4 gl,
        int programId,
        Matrix4x4d modelViewProjection,
        Matrix4x4d modelViewLocal,
        Matrix4x4d modelViewITLocal,
        SimpleMaterial material,
        List<Light> lights,
        RendererConfiguration quality,
        Vector3Dd cameraPosition)
    {
        ColorRgb kd = material.getDiffuse();
        Jogl4RendererConfigurationShaderSelector.activateShader(gl, programId,
            modelViewProjection, quality, (float)kd.r(), (float)kd.g(),
            (float)kd.b());

        setMatrix(gl, programId, "modelViewLocal", modelViewLocal);
        setMatrix(gl, programId, "modelViewITLocal", modelViewITLocal);
        setVector3(gl, programId, "cameraPositionGlobal", cameraPosition);
        setVector3(gl, programId, "ambientColor", material.getAmbient());
        setVector3(gl, programId, "diffuseColor", material.getDiffuse());
        setVector3(gl, programId, "specularColor", material.getSpecular());
        setFloat(gl, programId, "phongExponent",
            (float)material.getPhongExponent());
        setInt(gl, programId, "withTexture", 0);
        setInt(gl, programId, "withBumpMap", 0);

        int lightCount = 0;
        if ( lights != null ) {
            lightCount = Math.min(lights.size(), 8);
            for ( int i = 0; i < lightCount; i++ ) {
                Light light = lights.get(i);
                setVector3(gl, programId, "lightPositionsGlobal[" + i + "]",
                    light.getPosition());
                setVector3(gl, programId, "lightColorsGlobal[" + i + "]",
                    light.getEmission());
            }
        }
        setInt(gl, programId, "numberOfLights", lightCount);
    }

    static void renderMesh(GL4 gl, MeshData mesh, int mode)
    {
        gl.glBindVertexArray(meshVaoId);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, meshPositionVboId);
        upload(gl, mesh.positions);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 4, GL4.GL_FLOAT, false, 0, 0L);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, meshNormalVboId);
        upload(gl, mesh.normals);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 0, 0L);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, meshUvVboId);
        upload(gl, mesh.uvs);
        gl.glEnableVertexAttribArray(2);
        gl.glVertexAttribPointer(2, 2, GL4.GL_FLOAT, false, 0, 0L);

        gl.glDrawArrays(mode, 0, mesh.vertexCount);

        gl.glDisableVertexAttribArray(0);
        gl.glDisableVertexAttribArray(1);
        gl.glDisableVertexAttribArray(2);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
        gl.glBindVertexArray(0);
    }

    static void drawColoredPrimitives(
        GL4 gl,
        Matrix4x4d mvp,
        float[] positions,
        float[] colors,
        int mode,
        float size,
        float depthBiasNdc)
    {
        if ( positions.length == 0 || colors.length == 0 ) {
            return;
        }

        gl.glUseProgram(colorProgramId);
        int mvpLoc = gl.glGetUniformLocation(colorProgramId,
            "modelViewProjectionLocal");
        if ( mvpLoc >= 0 ) {
            gl.glUniformMatrix4fv(mvpLoc, 1, false,
                Jogl4MatrixRenderer.toColumnMajorFloatArray(mvp), 0);
        }
        int depthBiasLoc = gl.glGetUniformLocation(colorProgramId,
            "depthBiasNdc");
        if ( depthBiasLoc >= 0 ) {
            gl.glUniform1f(depthBiasLoc, depthBiasNdc);
        }

        gl.glBindVertexArray(colorVaoId);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, colorPositionVboId);
        upload(gl, positions);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0L);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, colorDataVboId);
        upload(gl, colors);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 0, 0L);

        if ( mode == GL4.GL_POINTS ) {
            gl.glPointSize(size);
        }
        else if ( mode == GL4.GL_LINES ) {
            gl.glLineWidth(size);
        }
        gl.glDrawArrays(mode, 0, positions.length / 3);

        gl.glDisableVertexAttribArray(0);
        gl.glDisableVertexAttribArray(1);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
        gl.glBindVertexArray(0);
        gl.glUseProgram(0);
    }

    private static void upload(GL4 gl, float[] data)
    {
        FloatBuffer buffer = Buffers.newDirectFloatBuffer(data);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long)data.length * Float.BYTES,
            buffer, GL4.GL_STREAM_DRAW);
    }

    private static MeshData buildMesh(PolyhedralBoundedSolid solid,
                                      boolean smoothNormals,
                                      RendererConfiguration quality)
    {
        ArrayList<Float> positions = new ArrayList<Float>();
        ArrayList<Float> normals = new ArrayList<Float>();
        ArrayList<Float> uvs = new ArrayList<Float>();
        Map<VertexCoordinateKey, List<Vector3Dd>> vertexNormals =
            smoothNormals ? buildSmoothedVertexNormals(solid) : null;

        for ( int i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            appendFaceMesh(face, positions, normals, uvs, vertexNormals,
                quality.getVertexNormalSmoothingThresholdDegrees());
        }
        return new MeshData(toArray(positions), toArray(normals), toArray(uvs));
    }

    static MeshData buildFaceMesh(PolyhedralBoundedSolid solid,
                                  int faceIndex)
    {
        ArrayList<Float> positions = new ArrayList<Float>();
        ArrayList<Float> normals = new ArrayList<Float>();
        ArrayList<Float> uvs = new ArrayList<Float>();

        if ( faceIndex >= 0 && faceIndex < solid.getPolygonsList().size() ) {
            appendFaceMesh(solid.getPolygonsList().get(faceIndex), positions,
                normals, uvs, null, 0.0);
        }
        return new MeshData(toArray(positions), toArray(normals), toArray(uvs));
    }

    private static void appendFaceMesh(
        _PolyhedralBoundedSolidFace face,
        List<Float> positions,
        List<Float> normals,
        List<Float> uvs,
        Map<VertexCoordinateKey, List<Vector3Dd>> vertexNormals,
        double smoothingThresholdDegrees)
    {
        if ( face == null ||
             _PolyhedralBoundedSolidFaceValidator
                 .isSurfaceDegenerate(face) ) {
            return;
        }

        InfinitePlane plane = face.getContainingPlane();
        if ( plane == null ) {
            return;
        }
        Vector3Dd normal = plane.getNormal().normalized();
        TessellatedFace faceTriangles = tessellateFace(face);
        for ( int i = 0; i + 3 < faceTriangles.positions.size(); i += 4 ) {
            float px = faceTriangles.positions.get(i);
            float py = faceTriangles.positions.get(i + 1);
            float pz = faceTriangles.positions.get(i + 2);
            Vector3Dd vertexNormal = resolveVertexNormal(vertexNormals,
                px, py, pz, normal, smoothingThresholdDegrees);
            positions.add(faceTriangles.positions.get(i));
            positions.add(faceTriangles.positions.get(i + 1));
            positions.add(faceTriangles.positions.get(i + 2));
            positions.add(1.0f);
            normals.add((float)vertexNormal.x());
            normals.add((float)vertexNormal.y());
            normals.add((float)vertexNormal.z());
            uvs.add(0.0f);
            uvs.add(0.0f);
        }
    }

    private static Map<VertexCoordinateKey, List<Vector3Dd>> buildSmoothedVertexNormals(
        PolyhedralBoundedSolid solid)
    {
        Map<VertexCoordinateKey, List<Vector3Dd>> incidentNormals =
            new HashMap<VertexCoordinateKey, List<Vector3Dd>>();

        if ( solid == null || solid.getPolygonsList() == null ) {
            return incidentNormals;
        }

        for ( int i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            if ( face == null || face.getContainingPlane() == null ||
                 _PolyhedralBoundedSolidFaceValidator
                     .isSurfaceDegenerate(face) ) {
                continue;
            }

            Vector3Dd faceNormal = face.getContainingPlane().getNormal();
            if ( faceNormal == null || faceNormal.length() <= VSDK.EPSILON ) {
                continue;
            }
            faceNormal = faceNormal.normalized();

            for ( int j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(j);
                if ( loop == null || loop.boundaryStartHalfEdge == null ) {
                    continue;
                }
                _PolyhedralBoundedSolidHalfEdge start = loop.boundaryStartHalfEdge;
                _PolyhedralBoundedSolidHalfEdge he = start;
                do {
                    if ( he.startingVertex != null &&
                        he.startingVertex.position != null ) {
                        VertexCoordinateKey key = VertexCoordinateKey.from(
                            he.startingVertex.position);
                        List<Vector3Dd> current = incidentNormals.get(key);
                        if ( current == null ) {
                            current = new ArrayList<Vector3Dd>();
                            incidentNormals.put(key, current);
                        }
                        current.add(faceNormal);
                    }
                    he = he.next();
                } while ( he != null && he != start );
            }
        }
        return incidentNormals;
    }

    private static Vector3Dd resolveVertexNormal(
        Map<VertexCoordinateKey, List<Vector3Dd>> vertexNormals,
        float px,
        float py,
        float pz,
        Vector3Dd fallback,
        double smoothingThresholdDegrees)
    {
        if ( vertexNormals == null || vertexNormals.isEmpty() ) {
            return fallback;
        }
        List<Vector3Dd> incidentNormals = vertexNormals.get(new VertexCoordinateKey(
            quantizeCoordinate(px),
            quantizeCoordinate(py),
            quantizeCoordinate(pz)));
        if ( incidentNormals == null || incidentNormals.isEmpty() ) {
            return fallback;
        }

        double clampedThreshold = Math.max(0.0,
            Math.min(180.0, smoothingThresholdDegrees));
        double cosineThreshold = Math.cos(Math.toRadians(clampedThreshold));
        Vector3Dd sum = new Vector3Dd(0, 0, 0);
        int count = 0;

        for ( int i = 0; i < incidentNormals.size(); i++ ) {
            Vector3Dd candidate = incidentNormals.get(i);
            if ( candidate == null || candidate.length() <= VSDK.EPSILON ) {
                continue;
            }
            Vector3Dd normalizedCandidate = candidate.normalized();
            if ( fallback.dotProduct(normalizedCandidate) >= cosineThreshold ) {
                sum = sum.add(normalizedCandidate);
                count++;
            }
        }

        if ( count == 0 || sum.length() <= VSDK.EPSILON ) {
            return fallback;
        }
        return sum.normalized();
    }

    private static long quantizeCoordinate(double value)
    {
        return Math.round(value * VERTEX_KEY_QUANTIZATION);
    }

    private static TessellatedFace tessellateFace(_PolyhedralBoundedSolidFace face)
    {
        ArrayList<Float> out = new ArrayList<Float>();
        if ( face == null ) {
            return new TessellatedFace(out);
        }

        GLUtessellator tess = GLU.gluNewTess();
        FaceTessellationCollector collector = new FaceTessellationCollector(out);

        GLU.gluTessCallback(tess, GLU.GLU_TESS_BEGIN, collector);
        GLU.gluTessCallback(tess, GLU.GLU_TESS_VERTEX, collector);
        GLU.gluTessCallback(tess, GLU.GLU_TESS_END, collector);
        GLU.gluTessCallback(tess, GLU.GLU_TESS_COMBINE, collector);
        GLU.gluTessCallback(tess, GLU.GLU_TESS_ERROR, collector);

        GLU.gluTessBeginPolygon(tess, null);
        for ( int i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            if ( loop == null || loop.boundaryStartHalfEdge == null ) {
                continue;
            }
            GLU.gluTessBeginContour(tess);
            _PolyhedralBoundedSolidHalfEdge he = loop.boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge start = he;
            do {
                he = he.next();
                if ( he == null ) {
                    break;
                }
                Vector3Dd p = he.startingVertex.position;
                double[] vertex = new double[] { p.x(), p.y(), p.z() };
                collector.keepReference(vertex);
                GLU.gluTessVertex(tess, vertex, 0, vertex);
            } while ( he != start );
            GLU.gluTessEndContour(tess);
        }
        GLU.gluTessEndPolygon(tess);
        GLU.gluDeleteTess(tess);
        return new TessellatedFace(out);
    }

    private static FlatShadedMesh buildFlatShadedSurfaceTriangles(
        MeshData mesh,
        Matrix4x4d modelMatrix,
        Matrix4x4d modelViewITLocal,
        SimpleMaterial material,
        List<Light> activeLights,
        Vector3Dd cameraPosition)
    {
        ArrayList<Float> positions = new ArrayList<Float>();
        ArrayList<Float> colors = new ArrayList<Float>();

        if ( mesh == null || modelMatrix == null || modelViewITLocal == null ||
             mesh.positions.length < 12 || mesh.normals.length < 9 ) {
            return new FlatShadedMesh(toArray(positions), toArray(colors));
        }

        for ( int pos = 0, normal = 0;
              pos + 11 < mesh.positions.length && normal + 8 < mesh.normals.length;
              pos += 12, normal += 9 ) {
            Vector3Dd p0 = new Vector3Dd(mesh.positions[pos],
                mesh.positions[pos + 1], mesh.positions[pos + 2]);
            Vector3Dd p1 = new Vector3Dd(mesh.positions[pos + 4],
                mesh.positions[pos + 5], mesh.positions[pos + 6]);
            Vector3Dd p2 = new Vector3Dd(mesh.positions[pos + 8],
                mesh.positions[pos + 9], mesh.positions[pos + 10]);

            Vector3Dd centroidLocal = p0.add(p1).add(p2).multiply(1.0 / 3.0);
            Vector3Dd centroidGlobal = modelMatrix.multiply(centroidLocal);
            Vector3Dd normalLocal = new Vector3Dd(mesh.normals[normal],
                mesh.normals[normal + 1], mesh.normals[normal + 2]).normalized();
            Vector4Dd normalGlobal4 = modelViewITLocal.multiply(
                new Vector4Dd(normalLocal.x(), normalLocal.y(), normalLocal.z(),
                    0.0));
            Vector3Dd normalGlobal = new Vector3Dd(normalGlobal4.x(),
                normalGlobal4.y(), normalGlobal4.z()).normalized();

            ColorRgb color = evaluateFlatColor(centroidGlobal, normalGlobal,
                material, activeLights, cameraPosition);

            appendTriangleVertex(positions, colors, p0, color);
            appendTriangleVertex(positions, colors, p1, color);
            appendTriangleVertex(positions, colors, p2, color);
        }

        return new FlatShadedMesh(toArray(positions), toArray(colors));
    }

    private static void appendTriangleVertex(
        List<Float> positions,
        List<Float> colors,
        Vector3Dd point,
        ColorRgb color)
    {
        positions.add((float)point.x());
        positions.add((float)point.y());
        positions.add((float)point.z());
        appendColor(colors, color);
    }

    private static void appendColor(List<Float> colors, ColorRgb color)
    {
        colors.add((float)color.r());
        colors.add((float)color.g());
        colors.add((float)color.b());
    }

    private static ColorRgb evaluateFlatColor(
        Vector3Dd pointGlobal,
        Vector3Dd normalGlobal,
        SimpleMaterial material,
        List<Light> activeLights,
        Vector3Dd cameraPosition)
    {
        Vector3Dd normal = normalGlobal;
        Vector3Dd viewDir = cameraPosition != null
            ? cameraPosition.subtract(pointGlobal)
            : new Vector3Dd(0, 0, 1);
        if ( viewDir.length() > VSDK.EPSILON ) {
            viewDir = viewDir.normalized();
            if ( normal.dotProduct(viewDir) < 0.0 ) {
                normal = normal.multiply(-1.0);
            }
        }

        ColorRgb ambient = material.getAmbient();
        ColorRgb diffuse = material.getDiffuse();
        ColorRgb specular = material.getSpecular();
        double r = ambient.r();
        double g = ambient.g();
        double b = ambient.b();

        if ( activeLights != null ) {
            for ( int i = 0; i < activeLights.size(); i++ ) {
                Light light = activeLights.get(i);
                Vector3Dd lightDir = light.getPosition().subtract(pointGlobal);
                if ( lightDir.length() <= VSDK.EPSILON ) {
                    continue;
                }
                lightDir = lightDir.normalized();
                double ndotl = Math.max(normal.dotProduct(lightDir), 0.0);
                Vector3Dd reflection = normal.multiply(2.0 * ndotl)
                    .subtract(lightDir);
                double spec = 0.0;
                if ( ndotl > 0.0 && viewDir.length() > VSDK.EPSILON &&
                     reflection.length() > VSDK.EPSILON ) {
                    spec = Math.pow(Math.max(reflection.normalized()
                        .dotProduct(viewDir), 0.0),
                        material.getPhongExponent());
                }

                ColorRgb lightColor = light.getEmission();
                r += lightColor.r() * diffuse.r() * ndotl +
                    lightColor.r() * specular.r() * spec;
                g += lightColor.g() * diffuse.g() * ndotl +
                    lightColor.g() * specular.g() * spec;
                b += lightColor.b() * diffuse.b() * ndotl +
                    lightColor.b() * specular.b() * spec;
            }
        }

        return new ColorRgb(clamp01(r), clamp01(g), clamp01(b));
    }

    private static double clamp01(double value)
    {
        if ( value < 0.0 ) {
            return 0.0;
        }
        if ( value > 1.0 ) {
            return 1.0;
        }
        return value;
    }

    private static void setMatrix(GL4 gl, int programId, String name,
                                  Matrix4x4d matrix)
    {
        int loc = gl.glGetUniformLocation(programId, name);
        if ( loc >= 0 ) {
            gl.glUniformMatrix4fv(loc, 1, false,
                Jogl4MatrixRenderer.toColumnMajorFloatArray(matrix), 0);
        }
    }

    private static void setVector3(GL4 gl, int programId, String name,
                                   Vector3Dd value)
    {
        int loc = gl.glGetUniformLocation(programId, name);
        if ( loc >= 0 ) {
            gl.glUniform3f(loc, (float)value.x(), (float)value.y(),
                (float)value.z());
        }
    }

    private static void setVector3(GL4 gl, int programId, String name,
                                   ColorRgb value)
    {
        int loc = gl.glGetUniformLocation(programId, name);
        if ( loc >= 0 ) {
            gl.glUniform3f(loc, (float)value.r(), (float)value.g(),
                (float)value.b());
        }
    }

    private static void setInt(GL4 gl, int programId, String name, int value)
    {
        int loc = gl.glGetUniformLocation(programId, name);
        if ( loc >= 0 ) {
            gl.glUniform1i(loc, value);
        }
    }

    private static void setFloat(GL4 gl, int programId, String name,
                                 float value)
    {
        int loc = gl.glGetUniformLocation(programId, name);
        if ( loc >= 0 ) {
            gl.glUniform1f(loc, value);
        }
    }

    private static double clamp(double value, double min, double max)
    {
        if ( value < min ) {
            return min;
        }
        if ( value > max ) {
            return max;
        }
        return value;
    }

    private static float[] toArray(List<Float> values)
    {
        float[] out = new float[values.size()];
        for ( int i = 0; i < values.size(); i++ ) {
            out[i] = values.get(i);
        }
        return out;
    }

    static final class MeshData
    {
        final float[] positions;
        final float[] normals;
        final float[] uvs;
        final int vertexCount;

        private MeshData(float[] positions, float[] normals, float[] uvs)
        {
            this.positions = positions;
            this.normals = normals;
            this.uvs = uvs;
            this.vertexCount = positions.length / 4;
        }
    }

    private static final class FlatShadedMesh
    {
        private final float[] positions;
        private final float[] colors;

        private FlatShadedMesh(float[] positions, float[] colors)
        {
            this.positions = positions;
            this.colors = colors;
        }
    }

    private static final class TessellatedFace
    {
        private final ArrayList<Float> positions;

        private TessellatedFace(ArrayList<Float> positions)
        {
            this.positions = positions;
        }
    }

    private static final class VertexCoordinateKey
    {
        private final long xBits;
        private final long yBits;
        private final long zBits;

        private VertexCoordinateKey(long xBits, long yBits, long zBits)
        {
            this.xBits = xBits;
            this.yBits = yBits;
            this.zBits = zBits;
        }

        private static VertexCoordinateKey from(Vector3Dd point)
        {
            return new VertexCoordinateKey(
                quantizeCoordinate(point.x()),
                quantizeCoordinate(point.y()),
                quantizeCoordinate(point.z()));
        }

        @Override
        public boolean equals(Object other)
        {
            if ( this == other ) {
                return true;
            }
            if ( !(other instanceof VertexCoordinateKey) ) {
                return false;
            }
            VertexCoordinateKey key = (VertexCoordinateKey)other;
            return xBits == key.xBits &&
                yBits == key.yBits &&
                zBits == key.zBits;
        }

        @Override
        public int hashCode()
        {
            return Long.hashCode(xBits) * 31 * 31 +
                Long.hashCode(yBits) * 31 +
                Long.hashCode(zBits);
        }
    }

    private static final class FaceTessellationCollector
        extends GLUtessellatorCallbackAdapter
    {
        private final ArrayList<double[]> kept = new ArrayList<double[]>();
        private final ArrayList<Float> out;
        private final ArrayList<double[]> pending = new ArrayList<double[]>();
        private int mode = -1;

        private FaceTessellationCollector(ArrayList<Float> out)
        {
            this.out = out;
        }

        private void keepReference(double[] value)
        {
            kept.add(value);
        }

        @Override
        public void begin(int type)
        {
            mode = type;
            pending.clear();
        }

        @Override
        public void vertex(Object vertexData)
        {
            if ( !(vertexData instanceof double[]) ) {
                return;
            }
            pending.add((double[])vertexData);
        }

        @Override
        public void end()
        {
            if ( mode == GL4.GL_TRIANGLES ) {
                for ( int i = 0; i + 2 < pending.size(); i += 3 ) {
                    emitTriangle(pending.get(i), pending.get(i + 1),
                        pending.get(i + 2));
                }
            }
            else if ( mode == GL4.GL_TRIANGLE_FAN ) {
                for ( int i = 1; i + 1 < pending.size(); i++ ) {
                    emitTriangle(pending.get(0), pending.get(i),
                        pending.get(i + 1));
                }
            }
            else if ( mode == GL4.GL_TRIANGLE_STRIP ) {
                for ( int i = 0; i + 2 < pending.size(); i++ ) {
                    if ( (i & 1) == 0 ) {
                        emitTriangle(pending.get(i), pending.get(i + 1),
                            pending.get(i + 2));
                    }
                    else {
                        emitTriangle(pending.get(i + 1), pending.get(i),
                            pending.get(i + 2));
                    }
                }
            }
            pending.clear();
        }

        @Override
        public void combine(double[] coords, Object[] data, float[] weight,
                            Object[] outData)
        {
            double[] combined = new double[] { coords[0], coords[1], coords[2] };
            kept.add(combined);
            outData[0] = combined;
        }

        @Override
        public void error(int errnum)
        {
            pending.clear();
        }

        private void emitTriangle(double[] a, double[] b, double[] c)
        {
            emitVertex(a);
            emitVertex(b);
            emitVertex(c);
        }

        private void emitVertex(double[] v)
        {
            out.add((float)v[0]);
            out.add((float)v[1]);
            out.add((float)v[2]);
            out.add(1.0f);
        }
    }
}
