package vsdk.toolkit.render.jogl;

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
import vsdk.toolkit.render.hiddenLine.HiddenLineRenderer;

public class Jogl4PolyhedralBoundedSolidRenderer extends Jogl4Renderer
{
    private static final float SURFACE_POLYGON_OFFSET_FACTOR = 2.0f;
    private static final float SURFACE_POLYGON_OFFSET_UNITS = 2.0f;
    private static final float NORMAL_LINE_LENGTH = 0.2f;
    private static final float WIREFRAME_DEPTH_BIAS = -1.0e-4f;
    private static final float POINTS_DEPTH_BIAS = -2.0e-4f;
    private static final float HIGHLIGHT_DEPTH_BIAS = -3.0e-4f;
    private static final double EDGE_ARROW_SIZE = 0.5;
    private static final double EDGE_ARROW_CURVE_OFFSET = 0.1;
    private static final int CURVED_ARROW_SEGMENTS = 10;
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
        if ( gl == null || solid == null || modelViewProjection == null ) {
            return;
        }
        DebugLines lines = buildFaceBoundaryLines(solid, faceIndex);
        if ( lines.positions.length == 0 ) {
            return;
        }
        Jogl4LineRenderer.drawLines(gl, modelViewProjection, lines.positions,
            lines.colors, 2.0f, HIGHLIGHT_DEPTH_BIAS);
    }

    public static void drawDebugFace(
        GL4 gl,
        PolyhedralBoundedSolid solid,
        int faceIndex,
        Matrix4x4d modelMatrix,
        Matrix4x4d modelViewProjection,
        Camera camera)
    {
        if ( gl == null || solid == null || faceIndex < 0 ||
             modelMatrix == null || modelViewProjection == null ) {
            return;
        }

        ensureInitialized(gl);

        Matrix4x4d modelViewITLocal = modelMatrix.invert().transpose();
        SimpleMaterial material = Jogl4SimpleMaterialRenderer.getActiveMaterial()
            .withDiffuse(new ColorRgb(1.0, 0.0, 0.0))
            .withAmbient(new ColorRgb(1.0, 0.0, 0.0))
            .withSpecular(new ColorRgb(0.0, 0.0, 0.0));

        MeshData mesh = buildFaceMesh(solid, faceIndex);
        if ( mesh.vertexCount == 0 ) {
            return;
        }

        RendererConfiguration quality = new RendererConfiguration();
        quality.setTexture(false);
        quality.setUseVertexColors(false);
        quality.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);

        int programId = Jogl4RendererConfigurationShaderSelector
            .selectSurfaceShaderProgram(gl, quality, false, false);
        configureSurfaceProgram(gl, programId, modelViewProjection,
            modelMatrix, modelViewITLocal, material, null, quality,
            camera != null ? camera.getPosition() : new Vector3Dd(0, 0, 0));
        renderMesh(gl, mesh, GL4.GL_TRIANGLES);
        Jogl4RendererConfigurationShaderSelector.deactivateShader(gl);
    }

    public static void drawDebugEdges(
        GL4 gl,
        PolyhedralBoundedSolid solid,
        Camera camera,
        int edgeIndex,
        Matrix4x4d modelViewProjection)
    {
        if ( gl == null || solid == null || modelViewProjection == null ) {
            return;
        }

        DebugLines lines = buildDebugEdgeLines(solid, camera, edgeIndex);
        if ( lines.positions.length > 0 ) {
            Jogl4LineRenderer.drawLines(gl, modelViewProjection, lines.positions,
                lines.colors, 2.0f, HIGHLIGHT_DEPTH_BIAS);
        }
        if ( lines.pointPositions.length > 0 ) {
            drawColoredPrimitives(gl, modelViewProjection, lines.pointPositions,
                lines.pointColors, GL4.GL_POINTS, 4.0f, HIGHLIGHT_DEPTH_BIAS);
        }
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
                DebugLines flatSurfaces = buildFlatShadedSurfaceTriangles(mesh,
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

        if ( quality.isWiresSet() ) {
            DebugLines lines = buildSolidEdgeLines(solid);
            if ( lines.positions.length > 0 ) {
                Jogl4LineRenderer.drawLines(gl, modelViewProjection,
                    lines.positions, lines.colors, 1.0f, WIREFRAME_DEPTH_BIAS);
            }
        }

        if ( quality.isPointsSet() ) {
            DebugLines points = buildSolidPointCloud(solid);
            if ( points.pointPositions.length > 0 ) {
                drawColoredPrimitives(gl, modelViewProjection,
                    points.pointPositions, points.pointColors, GL4.GL_POINTS,
                    6.0f, POINTS_DEPTH_BIAS);
            }
        }

        if ( quality.isNormalsSet() ) {
            DebugLines normals = buildVertexNormalLines(solid);
            if ( normals.positions.length > 0 ) {
                Jogl4LineRenderer.drawLines(gl, modelViewProjection,
                    normals.positions, normals.colors, 1.0f,
                    HIGHLIGHT_DEPTH_BIAS);
            }
        }

        if ( quality.isBoundingVolumeSet() ) {
            DebugLines bounds = buildBoundingVolumeLines(solid, quality);
            if ( bounds.positions.length > 0 ) {
                Jogl4LineRenderer.drawLines(gl, modelViewProjection,
                    bounds.positions, bounds.colors, 1.0f, HIGHLIGHT_DEPTH_BIAS);
            }
        }

        if ( quality.isSelectionCornersSet() ) {
            DebugLines corners = buildSelectionCornerLines(solid);
            if ( corners.positions.length > 0 ) {
                Jogl4LineRenderer.drawLines(gl, modelViewProjection,
                    corners.positions, corners.colors, 1.0f, HIGHLIGHT_DEPTH_BIAS);
            }
        }

        DebugLines nonPlanar = buildNonPlanarFaceHighlights(solid);
        if ( nonPlanar.positions.length > 0 ) {
            Jogl4LineRenderer.drawLines(gl, modelViewProjection,
                nonPlanar.positions, nonPlanar.colors, 4.0f, HIGHLIGHT_DEPTH_BIAS);
        }
    }

    private static void ensureInitialized(GL4 gl)
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

    private static void configureSurfaceProgram(
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
                    light.getSpecular());
            }
        }
        setInt(gl, programId, "numberOfLights", lightCount);
    }

    private static void renderMesh(GL4 gl, MeshData mesh, int mode)
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

    private static void drawColoredPrimitives(
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

    private static MeshData buildFaceMesh(PolyhedralBoundedSolid solid,
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
        if ( face == null || shouldDrawFaceAsBoundaryOnly(face) ) {
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
                 shouldDrawFaceAsBoundaryOnly(face) ) {
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

    private static DebugLines buildSolidEdgeLines(PolyhedralBoundedSolid solid)
    {
        ArrayList<Float> positions = new ArrayList<Float>();
        ArrayList<Float> colors = new ArrayList<Float>();

        for ( int i = 0; i < solid.getEdgesList().size(); i++ ) {
            _PolyhedralBoundedSolidEdge edge = solid.getEdgesList().get(i);
            if ( edge.rightHalf == null || edge.leftHalf == null ) {
                continue;
            }
            Vector3Dd start = edge.rightHalf.startingVertex.position;
            Vector3Dd end = edge.leftHalf.startingVertex.position;
            appendLine(positions, colors, start, end, edge.debugColor);
        }
        return new DebugLines(toArray(positions), toArray(colors));
    }

    private static DebugLines buildFlatShadedSurfaceTriangles(
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
            return new DebugLines(toArray(positions), toArray(colors));
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

        return new DebugLines(toArray(positions), toArray(colors));
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

                ColorRgb lightColor = light.getSpecular();
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

    private static DebugLines buildSolidPointCloud(PolyhedralBoundedSolid solid)
    {
        ArrayList<Float> positions = new ArrayList<Float>();
        ArrayList<Float> colors = new ArrayList<Float>();
        for ( int i = 0; i < solid.getVerticesList().size(); i++ ) {
            _PolyhedralBoundedSolidVertex vertex = solid.getVerticesList().get(i);
            appendPoint(positions, colors, vertex.position, vertex.debugColor);
        }
        return new DebugLines(new float[0], new float[0], toArray(positions),
            toArray(colors));
    }

    private static DebugLines buildVertexNormalLines(PolyhedralBoundedSolid solid)
    {
        ArrayList<Float> positions = new ArrayList<Float>();
        ArrayList<Float> colors = new ArrayList<Float>();
        ColorRgb yellow = new ColorRgb(1, 1, 0);

        for ( int i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            InfinitePlane plane = face.getContainingPlane();
            if ( plane == null ) {
                continue;
            }
            Vector3Dd normal = plane.getNormal().normalized();

            for ( int j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(j);
                if ( loop == null || loop.boundaryStartHalfEdge == null ) {
                    continue;
                }
                _PolyhedralBoundedSolidHalfEdge he = loop.boundaryStartHalfEdge;
                _PolyhedralBoundedSolidHalfEdge start = he;
                do {
                    he = he.next();
                    if ( he == null ) {
                        break;
                    }
                    Vector3Dd p = he.startingVertex.position;
                    appendLine(positions, colors,
                        p.add(normal.multiply(NORMAL_LINE_LENGTH / 100.0)),
                        p.add(normal.multiply(NORMAL_LINE_LENGTH)), yellow);
                } while ( he != start );
            }
        }
        return new DebugLines(toArray(positions), toArray(colors));
    }

    private static DebugLines buildBoundingVolumeLines(
        PolyhedralBoundedSolid solid,
        RendererConfiguration quality)
    {
        double[] minmax = solid.getMinMax();
        ColorRgb c = quality.getBoundingVolumeColor();
        return new DebugLines(buildBoxLinePositions(minmax),
            buildUniformColors(c, 24));
    }

    private static DebugLines buildSelectionCornerLines(
        PolyhedralBoundedSolid solid)
    {
        double[] minmax = solid.getMinMax();
        Vector3Dd min = new Vector3Dd(minmax[0], minmax[1], minmax[2]);
        Vector3Dd max = new Vector3Dd(minmax[3], minmax[4], minmax[5]);
        Vector3Dd delta = max.subtract(min);
        min = min.subtract(delta.multiply(0.01));
        max = max.add(delta.multiply(0.01));
        delta = delta.multiply(0.25);

        ArrayList<Float> positions = new ArrayList<Float>();
        ArrayList<Float> colors = new ArrayList<Float>();
        ColorRgb white = new ColorRgb(1, 1, 1);

        appendCorner(positions, colors, min, new Vector3Dd(delta.x(), 0, 0),
            new Vector3Dd(0, delta.y(), 0), new Vector3Dd(0, 0, delta.z()),
            white);
        appendCorner(positions, colors,
            new Vector3Dd(max.x(), min.y(), min.z()),
            new Vector3Dd(-delta.x(), 0, 0), new Vector3Dd(0, delta.y(), 0),
            new Vector3Dd(0, 0, delta.z()), white);
        appendCorner(positions, colors,
            new Vector3Dd(min.x(), max.y(), min.z()),
            new Vector3Dd(delta.x(), 0, 0), new Vector3Dd(0, -delta.y(), 0),
            new Vector3Dd(0, 0, delta.z()), white);
        appendCorner(positions, colors,
            new Vector3Dd(min.x(), min.y(), max.z()),
            new Vector3Dd(delta.x(), 0, 0), new Vector3Dd(0, delta.y(), 0),
            new Vector3Dd(0, 0, -delta.z()), white);
        appendCorner(positions, colors,
            new Vector3Dd(max.x(), max.y(), min.z()),
            new Vector3Dd(-delta.x(), 0, 0), new Vector3Dd(0, -delta.y(), 0),
            new Vector3Dd(0, 0, delta.z()), white);
        appendCorner(positions, colors,
            new Vector3Dd(max.x(), min.y(), max.z()),
            new Vector3Dd(-delta.x(), 0, 0), new Vector3Dd(0, delta.y(), 0),
            new Vector3Dd(0, 0, -delta.z()), white);
        appendCorner(positions, colors,
            new Vector3Dd(min.x(), max.y(), max.z()),
            new Vector3Dd(delta.x(), 0, 0), new Vector3Dd(0, -delta.y(), 0),
            new Vector3Dd(0, 0, -delta.z()), white);
        appendCorner(positions, colors, max,
            new Vector3Dd(-delta.x(), 0, 0), new Vector3Dd(0, -delta.y(), 0),
            new Vector3Dd(0, 0, -delta.z()), white);

        return new DebugLines(toArray(positions), toArray(colors));
    }

    private static DebugLines buildNonPlanarFaceHighlights(
        PolyhedralBoundedSolid solid)
    {
        ArrayList<Float> positions = new ArrayList<Float>();
        ArrayList<Float> colors = new ArrayList<Float>();
        ColorRgb yellow = new ColorRgb(1, 1, 0);

        for ( int i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            if ( shouldDrawFaceAsBoundaryOnly(face) ) {
                appendFaceBoundaryLines(face, positions, colors, yellow);
            }
        }
        return new DebugLines(toArray(positions), toArray(colors));
    }

    private static DebugLines buildFaceBoundaryLines(
        PolyhedralBoundedSolid solid,
        int faceIndex)
    {
        ArrayList<Float> positions = new ArrayList<Float>();
        ArrayList<Float> colors = new ArrayList<Float>();

        if ( solid == null || solid.getPolygonsList() == null || faceIndex < -1 ) {
            return new DebugLines(toArray(positions), toArray(colors));
        }

        for ( int i = 0; i < solid.getPolygonsList().size(); i++ ) {
            if ( faceIndex > -1 && i != faceIndex ) {
                continue;
            }
            appendFaceBoundaryLines(solid.getPolygonsList().get(i), positions,
                colors, colorForIndex(i));
        }
        return new DebugLines(toArray(positions), toArray(colors));
    }

    private static DebugLines buildDebugEdgeLines(
        PolyhedralBoundedSolid solid,
        Camera camera,
        int edgeIndex)
    {
        ArrayList<Float> positions = new ArrayList<Float>();
        ArrayList<Float> colors = new ArrayList<Float>();
        ArrayList<Float> pointPositions = new ArrayList<Float>();
        ArrayList<Float> pointColors = new ArrayList<Float>();

        for ( int i = 0; edgeIndex >= -1 && i < solid.getEdgesList().size(); i++ ) {
            if ( i != edgeIndex && edgeIndex > -1 ) {
                continue;
            }

            _PolyhedralBoundedSolidEdge edge = solid.getEdgesList().get(i);
            if ( edge.leftHalf == null || edge.rightHalf == null ) {
                continue;
            }
            Vector3Dd start = edge.rightHalf.startingVertex.position;
            Vector3Dd end = edge.leftHalf.startingVertex.position;
            _PolyhedralBoundedSolidFace face1 = edge.leftHalf.parentLoop.parentFace;
            _PolyhedralBoundedSolidFace face2 = edge.rightHalf.parentLoop.parentFace;

            ColorRgb color = new ColorRgb(0.8, 0, 0);
            if ( camera != null ) {
                boolean f1 = HiddenLineRenderer.isFaceVisibleFromCamera(face1,
                    camera) >= 0;
                boolean f2 = HiddenLineRenderer.isFaceVisibleFromCamera(face2,
                    camera) >= 0;
                if ( !f1 && !f2 ) {
                    color = new ColorRgb(0, 0, 0);
                }
                else if ( f1 != f2 ) {
                    color = new ColorRgb(1, 0, 0);
                }
            }
            appendLine(positions, colors, start, end, color);

            if ( edgeIndex > -1 && camera != null ) {
                Vector3Dd middle = start.add(end).multiply(0.5);
                Vector3Dd n1 = face1.getContainingPlane().getNormal();
                Vector3Dd n2 = face2.getContainingPlane().getNormal();
                appendLine(positions, colors, middle,
                    middle.add(n1.multiply(0.1)), new ColorRgb(1, 1, 0));
                appendLine(positions, colors, middle,
                    middle.add(n2.multiply(0.1)), new ColorRgb(0, 1, 1));

                Vector3Dd d = end.subtract(start);
                double length = d.length();
                if ( length > VSDK.EPSILON ) {
                    d = d.normalized();
                    for ( double t = VSDK.EPSILON; t < length;
                          t += (length / 20.0) ) {
                        Vector3Dd p = start.add(d.multiply(t));
                        int qi = solid.computeQuantitativeInvisibility(
                            camera.getPosition(), p);
                        appendPoint(pointPositions, pointColors, p,
                            qi == 0 ? new ColorRgb(0, 1, 0)
                                    : new ColorRgb(0, 0, 1));
                    }
                }
            }
        }

        return new DebugLines(toArray(positions), toArray(colors),
            toArray(pointPositions), toArray(pointColors));
    }

    private static void appendFaceBoundaryLines(
        _PolyhedralBoundedSolidFace face,
        List<Float> positions,
        List<Float> colors,
        ColorRgb color)
    {
        if ( face == null ) {
            return;
        }
        for ( int j = 0; j < face.boundariesList.size(); j++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(j);
            if ( loop == null || loop.boundaryStartHalfEdge == null ) {
                continue;
            }
            _PolyhedralBoundedSolidHalfEdge he = loop.boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge start = he;
            do {
                _PolyhedralBoundedSolidHalfEdge next = he.next();
                if ( next == null ) {
                    break;
                }
                appendFaceBoundaryArrow(positions, colors, he, next,
                    j == 0 ? 1.0 : -1.0, color);
                he = next;
            } while ( he != start );
        }
    }

    private static void appendFaceBoundaryArrow(
        List<Float> positions,
        List<Float> colors,
        _PolyhedralBoundedSolidHalfEdge he,
        _PolyhedralBoundedSolidHalfEdge next,
        double invert,
        ColorRgb color)
    {
        if ( he == null || next == null || he.startingVertex == null ||
             next.startingVertex == null ) {
            return;
        }

        _PolyhedralBoundedSolidHalfEdge nextNext = next.next();
        if ( nextNext == null || nextNext.startingVertex == null ) {
            appendLine(positions, colors, he.startingVertex.position,
                next.startingVertex.position, color);
            return;
        }

        Vector3Dd startPoint = he.startingVertex.position;
        Vector3Dd endPoint = next.startingVertex.position;
        Vector3Dd thirdPoint = nextNext.startingVertex.position;
        Vector3Dd a = endPoint.subtract(startPoint);
        Vector3Dd b = thirdPoint.subtract(startPoint);
        if ( a.length() <= VSDK.EPSILON || b.length() <= VSDK.EPSILON ) {
            appendLine(positions, colors, startPoint, endPoint, color);
            return;
        }

        Vector3Dd planeNormal = a.normalized().crossProduct(b.normalized());
        if ( planeNormal.length() <= VSDK.EPSILON ) {
            InfinitePlane containingPlane = he.parentLoop != null &&
                he.parentLoop.parentFace != null
                ? he.parentLoop.parentFace.getContainingPlane()
                : null;
            if ( containingPlane == null ||
                 containingPlane.getNormal().length() <= VSDK.EPSILON ) {
                appendLine(positions, colors, startPoint, endPoint, color);
                return;
            }
            planeNormal = containingPlane.getNormal();
        }

        appendCurvedArrowOverPlane(positions, colors, startPoint, endPoint,
            planeNormal.normalized(), invert, EDGE_ARROW_SIZE,
            EDGE_ARROW_CURVE_OFFSET, color);
    }

    private static void appendCurvedArrowOverPlane(
        List<Float> positions,
        List<Float> colors,
        Vector3Dd startPoint,
        Vector3Dd endPoint,
        Vector3Dd planeNormal,
        double invert,
        double sizePercent,
        double curveOffsetPercent,
        ColorRgb color)
    {
        Vector3Dd axis = endPoint.subtract(startPoint);
        double fullLength = axis.length();
        if ( fullLength <= 1e-12 ) {
            return;
        }

        sizePercent = Math.min(1.0, Math.max(VSDK.EPSILON, sizePercent));
        curveOffsetPercent = Math.min(1.0, Math.max(0.0, curveOffsetPercent));

        double factor = fullLength * sizePercent;
        double delta = factor / CURVED_ARROW_SEGMENTS;
        Vector3Dd tangentAxis = axis.normalized();
        Vector3Dd curveAxis = tangentAxis.crossProduct(planeNormal);
        if ( curveAxis.length() <= 1e-12 ) {
            appendLine(positions, colors, startPoint, endPoint, color);
            return;
        }
        curveAxis = curveAxis.normalized();

        for ( int i = 0; i < CURVED_ARROW_SEGMENTS; i++ ) {
            double t0 = i * delta;
            double t1 = t0 + delta;
            Vector3Dd p0 = curvedArrowPoint(startPoint, tangentAxis, curveAxis,
                invert, t0, fullLength, curveOffsetPercent);
            Vector3Dd p1 = curvedArrowPoint(startPoint, tangentAxis, curveAxis,
                invert, t1, fullLength, curveOffsetPercent);
            appendLine(positions, colors, p0, p1, color);
        }

        Vector3Dd tip = curvedArrowPoint(startPoint, tangentAxis, curveAxis,
            invert, factor, fullLength, curveOffsetPercent);
        Vector3Dd tangent = tangentAxis.add(curveAxis.multiply(invert *
            curveSlope(factor, fullLength, curveOffsetPercent)));
        if ( tangent.length() <= 1e-12 ) {
            tangent = tangentAxis;
        }
        tangent = tangent.normalized();

        Vector3Dd headSide = tangent.crossProduct(planeNormal);
        if ( headSide.length() <= 1e-12 ) {
            headSide = curveAxis;
        }
        headSide = headSide.normalized();

        double headLength = factor * 0.1;
        double headHalfWidth = fullLength * curveOffsetPercent * 0.5;
        Vector3Dd headBase = tip.subtract(tangent.multiply(headLength));

        appendLine(positions, colors, tip,
            headBase.add(headSide.multiply(headHalfWidth)), color);
        appendLine(positions, colors, tip,
            headBase.add(headSide.multiply(-headHalfWidth)), color);
    }

    private static Vector3Dd curvedArrowPoint(
        Vector3Dd startPoint,
        Vector3Dd tangentAxis,
        Vector3Dd curveAxis,
        double invert,
        double axisDistance,
        double fullLength,
        double curveOffsetPercent)
    {
        return startPoint.add(tangentAxis.multiply(axisDistance).add(
            curveAxis.multiply(invert *
                curveFactor(axisDistance, fullLength, curveOffsetPercent))));
    }

    private static double curveFactor(
        double axisDistance,
        double fullLength,
        double curveOffsetPercent)
    {
        double percent = axisDistance / fullLength;
        return curveOffsetPercent * fullLength * Math.sin(percent * Math.PI);
    }

    private static double curveSlope(
        double axisDistance,
        double fullLength,
        double curveOffsetPercent)
    {
        double percent = axisDistance / fullLength;
        return curveOffsetPercent * Math.PI * Math.cos(percent * Math.PI);
    }

    private static void appendLine(List<Float> positions, List<Float> colors,
                                   Vector3Dd a, Vector3Dd b, ColorRgb color)
    {
        positions.add((float)a.x());
        positions.add((float)a.y());
        positions.add((float)a.z());
        positions.add((float)b.x());
        positions.add((float)b.y());
        positions.add((float)b.z());
        appendColor(colors, color);
        appendColor(colors, color);
    }

    private static void appendPoint(List<Float> positions, List<Float> colors,
                                    Vector3Dd p, ColorRgb color)
    {
        positions.add((float)p.x());
        positions.add((float)p.y());
        positions.add((float)p.z());
        appendColor(colors, color);
    }

    private static void appendColor(List<Float> colors, ColorRgb color)
    {
        colors.add((float)color.r());
        colors.add((float)color.g());
        colors.add((float)color.b());
    }

    private static float[] buildBoxLinePositions(double[] minmax)
    {
        return new float[] {
            (float)minmax[0], (float)minmax[1], (float)minmax[2],
            (float)minmax[3], (float)minmax[1], (float)minmax[2],
            (float)minmax[3], (float)minmax[1], (float)minmax[2],
            (float)minmax[3], (float)minmax[4], (float)minmax[2],
            (float)minmax[3], (float)minmax[4], (float)minmax[2],
            (float)minmax[0], (float)minmax[4], (float)minmax[2],
            (float)minmax[0], (float)minmax[4], (float)minmax[2],
            (float)minmax[0], (float)minmax[1], (float)minmax[2],

            (float)minmax[0], (float)minmax[1], (float)minmax[5],
            (float)minmax[3], (float)minmax[1], (float)minmax[5],
            (float)minmax[3], (float)minmax[1], (float)minmax[5],
            (float)minmax[3], (float)minmax[4], (float)minmax[5],
            (float)minmax[3], (float)minmax[4], (float)minmax[5],
            (float)minmax[0], (float)minmax[4], (float)minmax[5],
            (float)minmax[0], (float)minmax[4], (float)minmax[5],
            (float)minmax[0], (float)minmax[1], (float)minmax[5],

            (float)minmax[0], (float)minmax[1], (float)minmax[2],
            (float)minmax[0], (float)minmax[1], (float)minmax[5],
            (float)minmax[3], (float)minmax[1], (float)minmax[2],
            (float)minmax[3], (float)minmax[1], (float)minmax[5],
            (float)minmax[3], (float)minmax[4], (float)minmax[2],
            (float)minmax[3], (float)minmax[4], (float)minmax[5],
            (float)minmax[0], (float)minmax[4], (float)minmax[2],
            (float)minmax[0], (float)minmax[4], (float)minmax[5]
        };
    }

    private static void appendCorner(
        List<Float> positions,
        List<Float> colors,
        Vector3Dd origin,
        Vector3Dd dx,
        Vector3Dd dy,
        Vector3Dd dz,
        ColorRgb color)
    {
        appendLine(positions, colors, origin, origin.add(dx), color);
        appendLine(positions, colors, origin, origin.add(dy), color);
        appendLine(positions, colors, origin, origin.add(dz), color);
    }

    private static float[] buildUniformColors(ColorRgb color, int vertexCount)
    {
        float[] colors = new float[vertexCount * 3];
        for ( int i = 0; i < vertexCount; i++ ) {
            int base = i * 3;
            colors[base] = (float)color.r();
            colors[base + 1] = (float)color.g();
            colors[base + 2] = (float)color.b();
        }
        return colors;
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

    private static ColorRgb colorForIndex(int index)
    {
        switch ( index % 8 ) {
          case 0: return new ColorRgb(1, 0, 0);
          case 1: return new ColorRgb(0, 1, 0);
          case 2: return new ColorRgb(0, 0, 1);
          case 3: return new ColorRgb(0, 1, 1);
          case 4: return new ColorRgb(1, 0, 1);
          case 5: return new ColorRgb(0.5, 0, 0);
          case 6: return new ColorRgb(0, 0.5, 0);
          default: return new ColorRgb(0.6, 0.5, 0.4);
        }
    }

    private static boolean shouldDrawFaceAsBoundaryOnly(
        _PolyhedralBoundedSolidFace face)
    {
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forFace(face);
        ArrayList<Vector3Dd> points =
            PolyhedralBoundedSolidGeometricValidator.extractPointsFromFace(face);
        if ( points == null ||
             !PolyhedralBoundedSolidGeometricValidator
                 .validateFacePointsAreCoplanar(points, numericContext) ) {
            return true;
        }
        if ( faceArea(face) <= numericContext.bigEpsilon() *
             numericContext.bigEpsilon() ) {
            return true;
        }
        return hasCloseNonAdjacentEdges(face, numericContext);
    }

    private static double faceArea(_PolyhedralBoundedSolidFace face)
    {
        double area = 0.0;
        for ( int i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            _PolyhedralBoundedSolidHalfEdge he = loop.boundaryStartHalfEdge;
            if ( he == null ) {
                continue;
            }
            _PolyhedralBoundedSolidHalfEdge start = he;
            Vector3Dd vectorArea = new Vector3Dd();
            do {
                _PolyhedralBoundedSolidHalfEdge next = he.next();
                if ( next == null ) {
                    break;
                }
                vectorArea = vectorArea.add(
                    he.startingVertex.position.crossProduct(
                        next.startingVertex.position));
                he = next;
            } while ( he != start );
            area += 0.5 * vectorArea.length();
        }
        return area;
    }

    private static boolean hasCloseNonAdjacentEdges(
        _PolyhedralBoundedSolidFace face,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext)
    {
        ArrayList<FaceSegment> segments = collectFaceSegments(face);
        double tolerance = Math.max(numericContext.bigEpsilon() * 10.0,
            numericContext.modelScale() * 1.0e-5);
        for ( int i = 0; i < segments.size(); i++ ) {
            for ( int j = i + 1; j < segments.size(); j++ ) {
                FaceSegment a = segments.get(i);
                FaceSegment b = segments.get(j);
                if ( a.sharesEndpointWith(b, numericContext.bigEpsilon()) ) {
                    continue;
                }
                if ( segmentDistance(a.start, a.end, b.start, b.end) <=
                     tolerance ) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ArrayList<FaceSegment> collectFaceSegments(
        _PolyhedralBoundedSolidFace face)
    {
        ArrayList<FaceSegment> segments = new ArrayList<FaceSegment>();
        for ( int i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            _PolyhedralBoundedSolidHalfEdge he = loop.boundaryStartHalfEdge;
            if ( he == null ) {
                continue;
            }
            _PolyhedralBoundedSolidHalfEdge start = he;
            do {
                _PolyhedralBoundedSolidHalfEdge next = he.next();
                if ( next == null ) {
                    break;
                }
                segments.add(new FaceSegment(he.startingVertex.position,
                    next.startingVertex.position));
                he = next;
            } while ( he != start );
        }
        return segments;
    }

    private static double segmentDistance(Vector3Dd p1, Vector3Dd q1,
                                          Vector3Dd p2, Vector3Dd q2)
    {
        Vector3Dd d1 = q1.subtract(p1);
        Vector3Dd d2 = q2.subtract(p2);
        Vector3Dd r = p1.subtract(p2);
        double a = d1.dotProduct(d1);
        double e = d2.dotProduct(d2);
        double f = d2.dotProduct(r);
        double s;
        double t;

        if ( a <= VSDK.EPSILON && e <= VSDK.EPSILON ) {
            return p1.subtract(p2).length();
        }
        if ( a <= VSDK.EPSILON ) {
            s = 0.0;
            t = clamp(f / e, 0.0, 1.0);
        }
        else {
            double c = d1.dotProduct(r);
            if ( e <= VSDK.EPSILON ) {
                t = 0.0;
                s = clamp(-c / a, 0.0, 1.0);
            }
            else {
                double b = d1.dotProduct(d2);
                double denom = a * e - b * b;
                if ( denom != 0.0 ) {
                    s = clamp((b * f - c * e) / denom, 0.0, 1.0);
                }
                else {
                    s = 0.0;
                }
                t = (b * s + f) / e;
                if ( t < 0.0 ) {
                    t = 0.0;
                    s = clamp(-c / a, 0.0, 1.0);
                }
                else if ( t > 1.0 ) {
                    t = 1.0;
                    s = clamp((b - c) / a, 0.0, 1.0);
                }
            }
        }

        return p1.add(d1.multiply(s)).subtract(p2.add(d2.multiply(t))).length();
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

    private static final class MeshData
    {
        private final float[] positions;
        private final float[] normals;
        private final float[] uvs;
        private final int vertexCount;

        private MeshData(float[] positions, float[] normals, float[] uvs)
        {
            this.positions = positions;
            this.normals = normals;
            this.uvs = uvs;
            this.vertexCount = positions.length / 4;
        }
    }

    private static final class DebugLines
    {
        private final float[] positions;
        private final float[] colors;
        private final float[] pointPositions;
        private final float[] pointColors;

        private DebugLines(float[] positions, float[] colors)
        {
            this(positions, colors, new float[0], new float[0]);
        }

        private DebugLines(float[] positions, float[] colors,
                           float[] pointPositions, float[] pointColors)
        {
            this.positions = positions;
            this.colors = colors;
            this.pointPositions = pointPositions;
            this.pointColors = pointColors;
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

    private static final class FaceSegment
    {
        private final Vector3Dd start;
        private final Vector3Dd end;

        private FaceSegment(Vector3Dd start, Vector3Dd end)
        {
            this.start = start;
            this.end = end;
        }

        private boolean sharesEndpointWith(FaceSegment other, double tolerance)
        {
            return start.subtract(other.start).length() <= tolerance ||
                start.subtract(other.end).length() <= tolerance ||
                end.subtract(other.start).length() <= tolerance ||
                end.subtract(other.end).length() <= tolerance;
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
