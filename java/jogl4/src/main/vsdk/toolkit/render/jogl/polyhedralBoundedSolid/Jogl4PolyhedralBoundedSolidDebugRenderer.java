package vsdk.toolkit.render.jogl.polyhedralBoundedSolid;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidFaceValidator;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.render.hiddenLine.HiddenLineRenderer;
import vsdk.toolkit.render.jogl.Jogl4LineRenderer;
import vsdk.toolkit.render.jogl.Jogl4RendererConfigurationShaderSelector;
import vsdk.toolkit.render.jogl.Jogl4SimpleMaterialRenderer;

public class Jogl4PolyhedralBoundedSolidDebugRenderer
{
    private static final float NORMAL_LINE_LENGTH = 0.2f;
    private static final float WIREFRAME_DEPTH_BIAS = -1.0e-4f;
    private static final float POINTS_DEPTH_BIAS = -2.0e-4f;
    private static final float HIGHLIGHT_DEPTH_BIAS = -3.0e-4f;
    private static final double EDGE_ARROW_SIZE = 0.5;
    private static final double EDGE_ARROW_CURVE_OFFSET = 0.1;
    private static final int CURVED_ARROW_SEGMENTS = 10;

    public static void drawDebugOverlays(
        GL4 gl,
        PolyhedralBoundedSolid solid,
        Camera camera,
        RendererConfiguration quality,
        Matrix4x4d modelViewProjection)
    {
        if ( gl == null || solid == null || quality == null ||
             modelViewProjection == null ) {
            return;
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
                Jogl4PolyhedralBoundedSolidRenderer.drawColoredPrimitives(gl,
                    modelViewProjection, points.pointPositions,
                    points.pointColors, GL4.GL_POINTS, 6.0f,
                    POINTS_DEPTH_BIAS);
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
                    bounds.positions, bounds.colors, 1.0f,
                    HIGHLIGHT_DEPTH_BIAS);
            }
        }

        if ( quality.isSelectionCornersSet() ) {
            DebugLines corners = buildSelectionCornerLines(solid);
            if ( corners.positions.length > 0 ) {
                Jogl4LineRenderer.drawLines(gl, modelViewProjection,
                    corners.positions, corners.colors, 1.0f,
                    HIGHLIGHT_DEPTH_BIAS);
            }
        }

        DebugLines nonPlanar = buildNonPlanarFaceHighlights(solid);
        if ( nonPlanar.positions.length > 0 ) {
            Jogl4LineRenderer.drawLines(gl, modelViewProjection,
                nonPlanar.positions, nonPlanar.colors, 4.0f,
                HIGHLIGHT_DEPTH_BIAS);
        }
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

        Jogl4PolyhedralBoundedSolidRenderer.ensureInitialized(gl);

        Matrix4x4d modelViewITLocal = modelMatrix.invert().transpose();
        SimpleMaterial material = Jogl4SimpleMaterialRenderer.getActiveMaterial()
            .withDiffuse(new ColorRgb(1.0, 0.0, 0.0))
            .withAmbient(new ColorRgb(1.0, 0.0, 0.0))
            .withSpecular(new ColorRgb(0.0, 0.0, 0.0));

        Jogl4PolyhedralBoundedSolidRenderer.MeshData mesh =
            Jogl4PolyhedralBoundedSolidRenderer.buildFaceMesh(solid, faceIndex);
        if ( mesh.vertexCount == 0 ) {
            return;
        }

        RendererConfiguration quality = new RendererConfiguration();
        quality.setTexture(false);
        quality.setUseVertexColors(false);
        quality.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);

        int programId = Jogl4RendererConfigurationShaderSelector
            .selectSurfaceShaderProgram(gl, quality, false, false);
        Jogl4PolyhedralBoundedSolidRenderer.configureSurfaceProgram(gl,
            programId, modelViewProjection, modelMatrix, modelViewITLocal,
            material, null, quality,
            camera != null ? camera.getPosition() : new Vector3Dd(0, 0, 0));
        Jogl4PolyhedralBoundedSolidRenderer.renderMesh(gl, mesh,
            GL4.GL_TRIANGLES);
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
            Jogl4PolyhedralBoundedSolidRenderer.drawColoredPrimitives(gl,
                modelViewProjection, lines.pointPositions, lines.pointColors,
                GL4.GL_POINTS, 4.0f, HIGHLIGHT_DEPTH_BIAS);
        }
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
            if ( _PolyhedralBoundedSolidFaceValidator
                     .isSurfaceDegenerate(face) ) {
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

    private static float[] toArray(List<Float> values)
    {
        float[] out = new float[values.size()];
        for ( int i = 0; i < values.size(); i++ ) {
            out[i] = values.get(i);
        }
        return out;
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
}
