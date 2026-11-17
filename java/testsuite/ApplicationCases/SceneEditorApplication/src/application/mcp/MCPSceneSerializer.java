package application.mcp;

import java.util.ArrayList;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.curve.ParametricCurve;
import vsdk.toolkit.environment.geometry.surface.FunctionalExplicitSurface;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.surface.ParametricBiCubicPatch;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.geometry.surface.TriangleMeshGroup;
import vsdk.toolkit.environment.geometry.volume.Arrow;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.geometry.volume.Torus;
import vsdk.toolkit.environment.geometry.volume.VoxelVolume;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.light.SpotLight;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.scene.SimpleBody;

import model.Scene;

/**
Presents the bodies and lights of the scene as JSON, for the tools
`scene.describe` and `scene.serialize` of the automation service (see
`MCP.md`).

Each body reports its placement (position, scale and rotation) and the
parameters of its geometry, the ones its constructor takes, so it can be
created again: i.e. the radii and height of a `Cone`, or the radii of a
`Torus`. Geometries defined by data rather than by a few numbers (boundary
representations, meshes, curves, patches) report only a summary of it in the
description, and the full data when serialized.
*/
class MCPSceneSerializer
{
    private final Scene scene;
    private final boolean full;

    /**
    @param scene scene to present
    @param full false for a description (parameters, and summaries of the
    data of the geometries defined by data), true for a serialization (also
    the materials, and the full data of every geometry)
    */
    MCPSceneSerializer(Scene scene, boolean full)
    {
        this.scene = scene;
        this.full = full;
    }

    /**
    @param index index of the only body to present, or a negative value for
    all of them (lights are presented only in that case)
    @return the bodies and lights as JSON
    */
    String toJson(int index)
    {
        ArrayList<SimpleBody> bodies = scene.scene.getSimpleBodies();
        StringBuilder sb = new StringBuilder();

        if ( index >= bodies.size() ) {
            throw new IllegalArgumentException("Body index out of range: " + index);
        }
        sb.append("{\"bodies\":[");
        boolean first = true;
        for ( int i = 0; i < bodies.size(); i++ ) {
            if ( index >= 0 && i != index ) {
                continue;
            }
            if ( !first ) {
                sb.append(',');
            }
            first = false;
            appendBody(sb, i, bodies.get(i));
        }
        sb.append(']');
        if ( index < 0 ) {
            sb.append(",\"lights\":[");
            ArrayList<Light> lights = scene.scene.getLights();
            for ( int i = 0; i < lights.size(); i++ ) {
                if ( i > 0 ) {
                    sb.append(',');
                }
                appendLight(sb, i, lights.get(i));
            }
            sb.append(']');
        }
        sb.append('}');
        return sb.toString();
    }

    //= Bodies ==============================================================

    private void appendBody(StringBuilder sb, int index, SimpleBody body)
    {
        Geometry geometry = body.getGeometry();

        sb.append("{\"index\":").append(index)
            .append(",\"name\":\"").append(MCPJson.escape(body.getName())).append('"')
            .append(",\"geometry\":\"")
            .append(geometry == null ? "null" : geometry.getClass().getSimpleName())
            .append('"')
            .append(",\"hidden\":").append(scene.isHidden(body))
            .append(",\"position\":").append(MCPJson.vector(body.getPosition()))
            .append(",\"scale\":").append(MCPJson.vector(body.getScale()))
            .append(",\"rotation\":");
        appendRotation(sb, body.getRotation());
        // Kept for the clients of the first version of scene.describe
        if ( geometry instanceof Sphere ) {
            sb.append(",\"radius\":").append(((Sphere)geometry).getRadius());
        }
        sb.append(",\"parameters\":");
        appendParameters(sb, geometry);
        if ( full ) {
            sb.append(",\"material\":");
            appendMaterial(sb, body.getMaterial());
        }
        sb.append('}');
    }

    /**
    The rotation is given as its 3x3 matrix, by rows (exact), and as the
    yaw, pitch and roll angles, in degrees, that `Matrix4x4d` obtains from it
    (and `Matrix4x4d.eulerAnglesRotation` takes, in radians).
    */
    private static void appendRotation(StringBuilder sb, Matrix4x4d rotation)
    {
        sb.append("{\"matrix\":[");
        for ( int row = 0; row < 3; row++ ) {
            if ( row > 0 ) {
                sb.append(',');
            }
            sb.append('[');
            for ( int column = 0; column < 3; column++ ) {
                if ( column > 0 ) {
                    sb.append(',');
                }
                sb.append(rotation.get(row, column));
            }
            sb.append(']');
        }
        sb.append("],\"yaw\":").append(degrees(rotation.obtainEulerYawAngle()))
            .append(",\"pitch\":").append(degrees(rotation.obtainEulerPitchAngle()))
            .append(",\"roll\":").append(degrees(rotation.obtainEulerRollAngle()))
            .append('}');
    }

    /**
    @return the angle in degrees, or null if it could not be obtained (JSON
    has no NaN)
    */
    private static String degrees(double radians)
    {
        double value = Math.toDegrees(radians);
        return Double.isFinite(value) ? Double.toString(value) : "null";
    }

    /**
    Parameters of the geometry, named as in its constructor or setters.
    */
    private void appendParameters(StringBuilder sb, Geometry geometry)
    {
        if ( geometry instanceof Sphere sphere ) {
            sb.append("{\"radius\":").append(sphere.getRadius()).append('}');
        }
        else if ( geometry instanceof Cone cone ) {
            sb.append("{\"bottomRadius\":").append(cone.getBottomRadius())
                .append(",\"topRadius\":").append(cone.getTopRadius())
                .append(",\"height\":").append(cone.getHeight())
                .append('}');
        }
        else if ( geometry instanceof Box box ) {
            Vector3Dd[] bounds = box.getRawBounds();
            sb.append("{\"size\":").append(MCPJson.vector(box.getSize()))
                .append(",\"minimum\":").append(MCPJson.vector(bounds[0]))
                .append(",\"maximum\":").append(MCPJson.vector(bounds[1]))
                .append('}');
        }
        else if ( geometry instanceof Torus torus ) {
            sb.append("{\"majorRadius\":").append(torus.getMajorRadius())
                .append(",\"minorRadius\":").append(torus.getMinorRadius())
                .append('}');
        }
        else if ( geometry instanceof Arrow arrow ) {
            sb.append("{\"baseLength\":").append(arrow.getBaseLength())
                .append(",\"headLength\":").append(arrow.getHeadLength())
                .append(",\"baseRadius\":").append(arrow.getBaseRadius())
                .append(",\"headRadius\":").append(arrow.getHeadRadius())
                .append('}');
        }
        else if ( geometry instanceof InfinitePlane plane ) {
            // Plane a*x + b*y + c*z + d = 0
            sb.append("{\"a\":").append(plane.getA())
                .append(",\"b\":").append(plane.getB())
                .append(",\"c\":").append(plane.getC())
                .append(",\"d\":").append(plane.getD())
                .append('}');
        }
        else if ( geometry instanceof FunctionalExplicitSurface surface ) {
            sb.append("{\"functionExpression\":\"")
                .append(MCPJson.escape(surface.getFunctionExpression())).append('"')
                .append(",\"minimum\":").append(MCPJson.vector(new Vector3Dd(
                    surface.getMinXBound(), surface.getMinYBound(), surface.getMinZBound())))
                .append(",\"maximum\":").append(MCPJson.vector(new Vector3Dd(
                    surface.getMaxXBound(), surface.getMaxYBound(), surface.getMaxZBound())))
                .append(",\"tesselationHintX\":").append(surface.getTesselationHintX())
                .append(",\"tesselationHintY\":").append(surface.getTesselationHintY())
                .append('}');
        }
        else if ( geometry instanceof VoxelVolume volume ) {
            // The voxels are not presented: they come from the voxelization
            // of other body (Create > Volume)
            sb.append("{\"xSize\":").append(volume.getXSize())
                .append(",\"ySize\":").append(volume.getYSize())
                .append(",\"zSize\":").append(volume.getZSize())
                .append(",\"threshold\":").append(volume.getThreshold())
                .append('}');
        }
        else if ( geometry instanceof ParametricCurve curve ) {
            appendCurve(sb, curve);
        }
        else if ( geometry instanceof ParametricBiCubicPatch patch ) {
            appendPatch(sb, patch);
        }
        else if ( geometry instanceof PolyhedralBoundedSolid solid ) {
            appendSolid(sb, solid);
        }
        else if ( geometry instanceof TriangleMeshGroup group ) {
            sb.append("{\"meshes\":[");
            for ( int i = 0; i < group.getMeshes().size(); i++ ) {
                if ( i > 0 ) {
                    sb.append(',');
                }
                appendMesh(sb, group.getMeshes().get(i));
            }
            sb.append("]}");
        }
        else if ( geometry instanceof TriangleMesh mesh ) {
            appendMesh(sb, mesh);
        }
        else {
            sb.append("null");
        }
    }

    private void appendCurve(StringBuilder sb, ParametricCurve curve)
    {
        sb.append("{\"approximationSteps\":").append(curve.getApproximationSteps())
            .append(",\"numberOfPoints\":").append(curve.getPointSize());
        if ( full ) {
            // Each point is its position followed by its tangent control
            // points, as given to ParametricCurve.addPoint
            sb.append(",\"points\":[");
            for ( int i = 0; i < curve.getPointSize(); i++ ) {
                if ( i > 0 ) {
                    sb.append(',');
                }
                sb.append("{\"type\":\"").append(curveTypeName(curve.types.get(i)))
                    .append("\",\"vectors\":[");
                Vector3Dd[] point = curve.getPoint(i);
                for ( int j = 0; j < point.length; j++ ) {
                    if ( j > 0 ) {
                        sb.append(',');
                    }
                    sb.append(MCPJson.vector(point[j]));
                }
                sb.append("]}");
            }
            sb.append(']');
        }
        sb.append('}');
    }

    private static String curveTypeName(int type)
    {
        return switch ( type ) {
            case ParametricCurve.BREAK -> "BREAK";
            case ParametricCurve.CORNER -> "CORNER";
            case ParametricCurve.QUAD -> "QUAD";
            case ParametricCurve.HERMITE -> "HERMITE";
            case ParametricCurve.BEZIER -> "BEZIER";
            case ParametricCurve.UNRBSPLINE -> "UNRBSPLINE";
            case ParametricCurve.NUNRBSPLINE -> "NUNRBSPLINE";
            case ParametricCurve.CATMULLROM -> "CATMULLROM";
            default -> Integer.toString(type);
        };
    }

    private void appendPatch(StringBuilder sb, ParametricBiCubicPatch patch)
    {
        boolean ferguson = patch.getType() == ParametricBiCubicPatch.FERGUSON;

        sb.append("{\"type\":\"")
            .append(ferguson ? "FERGUSON" : "BEZIER").append('"')
            .append(",\"approximationSteps\":").append(patch.getApproximationSteps());
        if ( full ) {
            if ( ferguson && patch.contourCurve != null ) {
                // ParametricBiCubicPatch.buildFergusonPatch(contourCurve)
                sb.append(",\"contourCurve\":");
                appendCurve(sb, patch.contourCurve);
            }
            else {
                // ParametricBiCubicPatch.buildBezierPatch(controlMeshPoints)
                sb.append(",\"controlMeshPoints\":[");
                for ( int i = 0; i < 4; i++ ) {
                    if ( i > 0 ) {
                        sb.append(',');
                    }
                    sb.append('[');
                    for ( int j = 0; j < 4; j++ ) {
                        if ( j > 0 ) {
                            sb.append(',');
                        }
                        Vector3Dd p = patch.getBezierControlPoint(i, j);
                        sb.append(p == null ? "null" : MCPJson.vector(p));
                    }
                    sb.append(']');
                }
                sb.append(']');
            }
        }
        sb.append('}');
    }

    /**
    A boundary representation is serialized as its vertices (id and
    position) and faces (id and loops, each one the ids of the starting
    vertices of its half edges, in order; the first loop is the outer one).
    */
    private void appendSolid(StringBuilder sb, PolyhedralBoundedSolid solid)
    {
        sb.append("{\"numberOfVertices\":").append(solid.getVerticesList().size())
            .append(",\"numberOfEdges\":").append(solid.getEdgesList().size())
            .append(",\"numberOfFaces\":").append(solid.getPolygonsList().size());
        if ( full ) {
            sb.append(",\"vertices\":[");
            for ( int i = 0; i < solid.getVerticesList().size(); i++ ) {
                _PolyhedralBoundedSolidVertex vertex = solid.getVerticesList().get(i);
                if ( i > 0 ) {
                    sb.append(',');
                }
                sb.append("{\"id\":").append(vertex.id)
                    .append(",\"position\":").append(MCPJson.vector(vertex.position))
                    .append('}');
            }
            sb.append("],\"faces\":[");
            for ( int i = 0; i < solid.getPolygonsList().size(); i++ ) {
                _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
                if ( i > 0 ) {
                    sb.append(',');
                }
                sb.append("{\"id\":").append(face.id).append(",\"loops\":[");
                for ( int j = 0; j < face.boundariesList.size(); j++ ) {
                    _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(j);
                    if ( j > 0 ) {
                        sb.append(',');
                    }
                    sb.append('[');
                    _PolyhedralBoundedSolidHalfEdge start = loop.boundaryStartHalfEdge;
                    _PolyhedralBoundedSolidHalfEdge he = start;
                    boolean firstVertex = true;
                    while ( he != null ) {
                        if ( !firstVertex ) {
                            sb.append(',');
                        }
                        firstVertex = false;
                        sb.append(he.startingVertex.id);
                        he = he.next();
                        if ( he == start ) {
                            break;
                        }
                    }
                    sb.append(']');
                }
                sb.append("]}");
            }
            sb.append(']');
        }
        sb.append('}');
    }

    /**
    A mesh is serialized as its vertex positions (x, y, z of each vertex,
    one after the other) and the vertex indexes of its triangles (three per
    triangle).
    */
    private void appendMesh(StringBuilder sb, TriangleMesh mesh)
    {
        sb.append("{\"numberOfVertices\":").append(mesh.getNumVertices())
            .append(",\"numberOfTriangles\":").append(mesh.getNumTriangles());
        if ( full ) {
            double[] positions = mesh.getVertexPositions();
            int[] triangles = mesh.getTriangleIndexes();
            sb.append(",\"vertexPositions\":[");
            for ( int i = 0; positions != null && i < 3*mesh.getNumVertices(); i++ ) {
                if ( i > 0 ) {
                    sb.append(',');
                }
                sb.append(positions[i]);
            }
            sb.append("],\"triangleIndexes\":[");
            for ( int i = 0; triangles != null && i < 3*mesh.getNumTriangles(); i++ ) {
                if ( i > 0 ) {
                    sb.append(',');
                }
                sb.append(triangles[i]);
            }
            sb.append(']');
        }
        sb.append('}');
    }

    private static void appendMaterial(StringBuilder sb, SimpleMaterial material)
    {
        if ( material == null ) {
            sb.append("null");
            return;
        }
        sb.append("{\"name\":\"").append(MCPJson.escape(material.getName())).append('"')
            .append(",\"ambient\":").append(MCPJson.color(material.getAmbient()))
            .append(",\"diffuse\":").append(MCPJson.color(material.getDiffuse()))
            .append(",\"specular\":").append(MCPJson.color(material.getSpecular()))
            .append(",\"emission\":").append(MCPJson.color(material.getEmission()))
            .append(",\"transmittance\":").append(MCPJson.color(material.getTransmittance()))
            .append(",\"phongExponent\":").append(material.getPhongExponent())
            .append(",\"reflectionCoefficient\":").append(material.getReflectionCoefficient())
            .append(",\"refractionCoefficient\":").append(material.getRefractionCoefficient())
            .append(",\"indexOfRefraction\":").append(material.getIndexOfRefraction())
            .append(",\"opacity\":").append(material.getOpacity())
            .append(",\"doubleSided\":").append(material.isDoubleSided())
            .append('}');
    }

    //= Lights ==============================================================

    private void appendLight(StringBuilder sb, int index, Light light)
    {
        sb.append("{\"index\":").append(index)
            .append(",\"type\":\"").append(light.getClass().getSimpleName()).append('"')
            .append(",\"position\":").append(MCPJson.vector(light.getPosition()))
            .append(",\"emission\":").append(MCPJson.color(light.getEmission()));
        if ( full && light instanceof SpotLight spot ) {
            sb.append(",\"pointsAt\":").append(MCPJson.vector(spot.getPointsAt()))
                .append(",\"coefficient\":").append(spot.getCoefficient())
                .append(",\"radius\":").append(spot.getRadius())
                .append(",\"falloff\":").append(spot.getFalloff());
        }
        sb.append('}');
    }
}
