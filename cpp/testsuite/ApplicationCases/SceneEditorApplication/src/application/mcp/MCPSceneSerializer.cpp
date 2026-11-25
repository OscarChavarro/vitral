#include <cmath>

#include "java/lang/IllegalArgumentException.h"
#include "java/lang/String.h"
#include "java/lang/StringBuilder.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/curve/ParametricCurve.h"
#include "vsdk/toolkit/environment/geometry/surface/FunctionalExplicitSurface.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/surface/ParametricBiCubicPatch.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.h"
#include "vsdk/toolkit/environment/geometry/volume/Arrow.h"
#include "vsdk/toolkit/environment/geometry/volume/BlobComponent.h"
#include "vsdk/toolkit/environment/geometry/volume/BlobSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/geometry/volume/Cone.h"
#include "vsdk/toolkit/environment/geometry/volume/Tube.h"
#include "vsdk/toolkit/environment/geometry/volume/Prism.h"
#include "vsdk/toolkit/environment/geometry/volume/HeightFieldSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/PlanarHalfSpace.h"
#include "vsdk/toolkit/environment/geometry/volume/PolynomialSurface.h"
#include "vsdk/toolkit/environment/geometry/volume/QuadricSurface.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/geometry/volume/Torus.h"
#include "vsdk/toolkit/environment/geometry/volume/VoxelVolume.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/light/SpotLight.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "application/mcp/MCPJson.h"
#include "application/mcp/MCPSceneSerializer.h"
#include "model/Scene.h"

MCPSceneSerializer::MCPSceneSerializer(Scene* inScene, bool inFull)
    : scene(inScene), full(inFull)
{
}

java::String MCPSceneSerializer::toJson(int index)
{
    java::ArrayList<SimpleBody*>& bodies = scene->scene->getSimpleBodies();
    java::StringBuilder sb;

    if ( index >= bodies.size() ) {
        throw java::IllegalArgumentException(
            java::String("Body index out of range: ") + java::String::valueOf(index));
    }
    sb.append("{\"bodies\":[");
    bool first = true;
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
        java::ArrayList<Light*>& lights = scene->scene->getLights();
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

//= Bodies ==================================================================

void MCPSceneSerializer::appendBody(java::StringBuilder& sb, int index,
                                    SimpleBody* body)
{
    Geometry* geometry = body->getGeometry();

    sb.append("{\"index\":").append(index)
        .append(",\"name\":\"").append(MCPJson::escape(body->getName())).append('"')
        .append(",\"geometry\":\"")
        .append(geometry == nullptr ? java::String("null") : geometry->getClassSimpleName())
        .append('"')
        .append(",\"hidden\":").append(scene->isHidden(body))
        .append(",\"position\":").append(MCPJson::vector(body->getPosition()))
        .append(",\"scale\":").append(MCPJson::vector(body->getScale()))
        .append(",\"rotation\":");
    appendRotation(sb, body->getRotation());
    // Kept for the clients of the first version of scene.describe
    Sphere* sphere = dynamic_cast<Sphere*>(geometry);
    if ( sphere != nullptr ) {
        sb.append(",\"radius\":").append(sphere->getRadius());
    }
    sb.append(",\"parameters\":");
    appendParameters(sb, geometry);
    if ( full ) {
        sb.append(",\"material\":");
        appendMaterial(sb, body->getMaterial());
    }
    sb.append('}');
}

/**
The rotation is given as its 3x3 matrix, by rows (exact), and as the
yaw, pitch and roll angles, in degrees, that `Matrix4x4d` obtains from it
(and `Matrix4x4d::eulerAnglesRotation` takes, in radians).
*/
void MCPSceneSerializer::appendRotation(java::StringBuilder& sb,
                                        const Matrix4x4d& rotation)
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
java::String MCPSceneSerializer::degrees(double radians)
{
    double value = radians * 180.0 / M_PI;
    if ( !std::isfinite(value) ) {
        return "null";
    }
    java::StringBuilder text;
    text.append(value);
    return text.toString();
}

/**
Parameters of the geometry, named as in its constructor or setters.
*/
void MCPSceneSerializer::appendParameters(java::StringBuilder& sb,
                                          Geometry* geometry)
{
    if ( Sphere* sphere = dynamic_cast<Sphere*>(geometry) ) {
        sb.append("{\"radius\":").append(sphere->getRadius()).append('}');
    }
    else if ( Cone* cone = dynamic_cast<Cone*>(geometry) ) {
        sb.append("{\"bottomRadius\":").append(cone->getBottomRadius())
            .append(",\"topRadius\":").append(cone->getTopRadius())
            .append(",\"height\":").append(cone->getHeight())
            .append('}');
    }
    else if ( Tube* tube = dynamic_cast<Tube*>(geometry) ) {
        sb.append("{\"internalRadius\":").append(tube->getInternalRadius())
            .append(",\"externalRadius\":").append(tube->getExternalRadius())
            .append(",\"height\":").append(tube->getHeight())
            .append('}');
    }
    else if ( Box* box = dynamic_cast<Box*>(geometry) ) {
        const Vector3Dd* bounds = box->getRawBounds();
        sb.append("{\"size\":").append(MCPJson::vector(box->getSize()))
            .append(",\"minimum\":").append(MCPJson::vector(bounds[0]))
            .append(",\"maximum\":").append(MCPJson::vector(bounds[1]))
            .append('}');
    }
    else if ( Prism* prism = dynamic_cast<Prism*>(geometry) ) {
        sb.append("{\"numberOfSides\":").append(prism->getNumberOfSides())
            .append(",\"circumradius\":").append(prism->getCircumradius())
            .append(",\"height\":").append(prism->getHeight())
            .append(",\"base\":[");
        const java::ArrayList<Vector2Dd>& base = prism->getBase();
        for ( long int i = 0; i < base.size(); i++ ) {
            if ( i > 0 ) {
                sb.append(',');
            }
            sb.append('[').append(base[i].x).append(',').append(base[i].y)
                .append(']');
        }
        sb.append("]}");
    }
    else if ( Torus* torus = dynamic_cast<Torus*>(geometry) ) {
        sb.append("{\"majorRadius\":").append(torus->getMajorRadius())
            .append(",\"minorRadius\":").append(torus->getMinorRadius())
            .append('}');
    }
    else if ( Arrow* arrow = dynamic_cast<Arrow*>(geometry) ) {
        sb.append("{\"baseLength\":").append(arrow->getBaseLength())
            .append(",\"headLength\":").append(arrow->getHeadLength())
            .append(",\"baseRadius\":").append(arrow->getBaseRadius())
            .append(",\"headRadius\":").append(arrow->getHeadRadius())
            .append('}');
    }
    else if ( InfinitePlane* plane = dynamic_cast<InfinitePlane*>(geometry) ) {
        // Plane a*x + b*y + c*z + d = 0
        sb.append("{\"a\":").append(plane->getA())
            .append(",\"b\":").append(plane->getB())
            .append(",\"c\":").append(plane->getC())
            .append(",\"d\":").append(plane->getD())
            .append('}');
    }
    else if ( FunctionalExplicitSurface* surface =
                  dynamic_cast<FunctionalExplicitSurface*>(geometry) ) {
        sb.append("{\"functionExpression\":\"")
            .append(MCPJson::escape(surface->getFunctionExpression())).append('"')
            .append(",\"minimum\":").append(MCPJson::vector(Vector3Dd(
                surface->getMinXBound(), surface->getMinYBound(),
                surface->getMinZBound())))
            .append(",\"maximum\":").append(MCPJson::vector(Vector3Dd(
                surface->getMaxXBound(), surface->getMaxYBound(),
                surface->getMaxZBound())))
            .append(",\"tesselationHintX\":").append(surface->getTesselationHintX())
            .append(",\"tesselationHintY\":").append(surface->getTesselationHintY())
            .append('}');
    }
    else if ( BlobSolid* blob = dynamic_cast<BlobSolid*>(geometry) ) {
        // BlobSolid(threshold, sturm) and addComponent(BlobComponent(
        // center, radius, strength))
        sb.append("{\"threshold\":").append(blob->getThreshold())
            .append(",\"sturm\":").append(blob->usesSturmSequences())
            .append(",\"components\":[");
        for ( long i = 0; i < blob->getComponentCount(); i++ ) {
            const BlobComponent& component = blob->getComponent(i);
            if ( i > 0 ) {
                sb.append(',');
            }
            sb.append("{\"center\":").append(MCPJson::vector(component.getCenter()))
                .append(",\"radius\":").append(component.getRadius())
                .append(",\"strength\":").append(component.getStrength())
                .append('}');
        }
        sb.append("]}");
    }
    else if ( PolynomialSurface* polynomial =
                  dynamic_cast<PolynomialSurface*>(geometry) ) {
        // Coefficients in the order of PolynomialSurface::getTermIndex
        sb.append("{\"order\":").append(polynomial->getOrder())
            .append(",\"sturm\":").append(polynomial->usesSturmSequences())
            .append(",\"coefficients\":[");
        const double* coefficients = polynomial->getCoefficients();
        int count = PolynomialSurface::getTermCount(polynomial->getOrder());
        for ( int i = 0; i < count; i++ ) {
            if ( i > 0 ) {
                sb.append(',');
            }
            sb.append(coefficients[i]);
        }
        sb.append("]}");
    }
    else if ( QuadricSurface* quadric = dynamic_cast<QuadricSurface*>(geometry) ) {
        // QuadricSurface(square, mixed, linear, constant)
        const QuadricSurface& q = *quadric;
        sb.append("{\"square\":").append(MCPJson::vector(q.squareCoefficients()))
            .append(",\"mixed\":").append(MCPJson::vector(q.mixedCoefficients()))
            .append(",\"linear\":").append(MCPJson::vector(q.linearCoefficients()))
            .append(",\"constant\":").append(q.constantCoefficient())
            .append('}');
    }
    else if ( PlanarHalfSpace* halfSpace = dynamic_cast<PlanarHalfSpace*>(geometry) ) {
        // The solid a*x + b*y + c*z + d <= 0
        const InfinitePlane& plane = halfSpace->getBoundaryPlane();
        sb.append("{\"a\":").append(plane.getA())
            .append(",\"b\":").append(plane.getB())
            .append(",\"c\":").append(plane.getC())
            .append(",\"d\":").append(plane.getD())
            .append('}');
    }
    else if ( HeightFieldSolid* field = dynamic_cast<HeightFieldSolid*>(geometry) ) {
        sb.append("{\"columns\":").append(field->getColumnCount())
            .append(",\"rows\":").append(field->getRowCount())
            .append(",\"minimum\":").append(MCPJson::vector(field->getMinimum()))
            .append(",\"maximum\":").append(MCPJson::vector(field->getMaximum()));
        if ( full ) {
            // Height of point (x, z) at index x + columns*z
            sb.append(",\"heights\":[");
            for ( long z = 0; z < field->getRowCount(); z++ ) {
                for ( long x = 0; x < field->getColumnCount(); x++ ) {
                    if ( x > 0 || z > 0 ) {
                        sb.append(',');
                    }
                    sb.append(field->getHeight(x, z));
                }
            }
            sb.append(']');
        }
        sb.append('}');
    }
    else if ( VoxelVolume* volume = dynamic_cast<VoxelVolume*>(geometry) ) {
        // The voxels are not presented: they come from the voxelization
        // of other body (Create > Volume)
        sb.append("{\"xSize\":").append(volume->getXSize())
            .append(",\"ySize\":").append(volume->getYSize())
            .append(",\"zSize\":").append(volume->getZSize())
            .append(",\"threshold\":").append(volume->getThreshold())
            .append('}');
    }
    else if ( ParametricCurve* curve = dynamic_cast<ParametricCurve*>(geometry) ) {
        appendCurve(sb, *curve);
    }
    else if ( ParametricBiCubicPatch* patch =
                  dynamic_cast<ParametricBiCubicPatch*>(geometry) ) {
        appendPatch(sb, *patch);
    }
    else if ( PolyhedralBoundedSolid* solid =
                  dynamic_cast<PolyhedralBoundedSolid*>(geometry) ) {
        appendSolid(sb, *solid);
    }
    else if ( TriangleMeshGroup* group = dynamic_cast<TriangleMeshGroup*>(geometry) ) {
        sb.append("{\"meshes\":[");
        for ( int i = 0; i < group->getMeshes().size(); i++ ) {
            if ( i > 0 ) {
                sb.append(',');
            }
            appendMesh(sb, group->getMeshes()[i]);
        }
        sb.append("]}");
    }
    else if ( TriangleMesh* mesh = dynamic_cast<TriangleMesh*>(geometry) ) {
        appendMesh(sb, *mesh);
    }
    else {
        sb.append("null");
    }
}

void MCPSceneSerializer::appendCurve(java::StringBuilder& sb,
                                     const ParametricCurve& curve)
{
    sb.append("{\"approximationSteps\":").append(curve.getApproximationSteps())
        .append(",\"numberOfPoints\":").append(curve.getPointSize());
    if ( full ) {
        // Each point is its position followed by its tangent control
        // points, as given to ParametricCurve::addPoint
        sb.append(",\"points\":[");
        for ( int i = 0; i < curve.getPointSize(); i++ ) {
            if ( i > 0 ) {
                sb.append(',');
            }
            sb.append("{\"type\":\"").append(curveTypeName(curve.getPointType(i)))
                .append("\",\"vectors\":[");
            const java::ArrayList<Vector3Dd>& point = curve.getPointVector(i);
            for ( int j = 0; j < point.size(); j++ ) {
                if ( j > 0 ) {
                    sb.append(',');
                }
                sb.append(MCPJson::vector(point[j]));
            }
            sb.append("]}");
        }
        sb.append(']');
    }
    sb.append('}');
}

java::String MCPSceneSerializer::curveTypeName(int type)
{
    switch ( type ) {
      case ParametricCurve::BREAK: return "BREAK";
      case ParametricCurve::CORNER: return "CORNER";
      case ParametricCurve::QUAD: return "QUAD";
      case ParametricCurve::HERMITE: return "HERMITE";
      case ParametricCurve::BEZIER: return "BEZIER";
      case ParametricCurve::UNRBSPLINE: return "UNRBSPLINE";
      case ParametricCurve::NUNRBSPLINE: return "NUNRBSPLINE";
      case ParametricCurve::CATMULLROM: return "CATMULLROM";
      default: return java::String::valueOf(type);
    }
}

void MCPSceneSerializer::appendPatch(java::StringBuilder& sb,
                                     ParametricBiCubicPatch& patch)
{
    bool ferguson = patch.getType() == ParametricBiCubicPatch::FERGUSON;

    sb.append("{\"type\":\"")
        .append(ferguson ? "FERGUSON" : "BEZIER").append('"')
        .append(",\"approximationSteps\":").append(patch.getApproximationSteps());
    if ( full ) {
        if ( ferguson && patch.contourCurve != nullptr ) {
            // ParametricBiCubicPatch::buildFergusonPatch(contourCurve)
            sb.append(",\"contourCurve\":");
            appendCurve(sb, *patch.contourCurve);
        }
        else {
            // ParametricBiCubicPatch::buildBezierPatch(controlMeshPoints)
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
                    Vector3Dd p;
                    // C++ port note: false stands for Java's null point
                    if ( patch.getBezierControlPoint(i, j, p) ) {
                        sb.append(MCPJson::vector(p));
                    }
                    else {
                        sb.append("null");
                    }
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
void MCPSceneSerializer::appendSolid(java::StringBuilder& sb,
                                     PolyhedralBoundedSolid& solid)
{
    sb.append("{\"numberOfVertices\":").append(solid.getVerticesList().size())
        .append(",\"numberOfEdges\":").append(solid.getEdgesList().size())
        .append(",\"numberOfFaces\":").append(solid.getPolygonsList().size());
    if ( full ) {
        sb.append(",\"vertices\":[");
        for ( int i = 0; i < solid.getVerticesList().size(); i++ ) {
            _PolyhedralBoundedSolidVertex* vertex = solid.getVerticesList().get(i);
            if ( i > 0 ) {
                sb.append(',');
            }
            sb.append("{\"id\":").append(vertex->id)
                .append(",\"position\":").append(MCPJson::vector(vertex->position))
                .append('}');
        }
        sb.append("],\"faces\":[");
        for ( int i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace* face = solid.getPolygonsList().get(i);
            if ( i > 0 ) {
                sb.append(',');
            }
            sb.append("{\"id\":").append(face->id).append(",\"loops\":[");
            for ( int j = 0; j < face->boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(j);
                if ( j > 0 ) {
                    sb.append(',');
                }
                sb.append('[');
                _PolyhedralBoundedSolidHalfEdge* start = loop->boundaryStartHalfEdge;
                _PolyhedralBoundedSolidHalfEdge* he = start;
                bool firstVertex = true;
                while ( he != nullptr ) {
                    if ( !firstVertex ) {
                        sb.append(',');
                    }
                    firstVertex = false;
                    sb.append(he->startingVertex->id);
                    he = he->next();
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
void MCPSceneSerializer::appendMesh(java::StringBuilder& sb, TriangleMesh& mesh)
{
    sb.append("{\"numberOfVertices\":").append(mesh.getNumVertices())
        .append(",\"numberOfTriangles\":").append(mesh.getNumTriangles());
    if ( full ) {
        // C++ port note: empty lists stand for Java's null arrays
        java::ArrayList<double>& positions = mesh.getVertexPositions();
        java::ArrayList<int>& triangles = mesh.getTriangleIndexes();
        sb.append(",\"vertexPositions\":[");
        for ( long i = 0; i < positions.size() && i < 3L*mesh.getNumVertices(); i++ ) {
            if ( i > 0 ) {
                sb.append(',');
            }
            sb.append(positions[i]);
        }
        sb.append("],\"triangleIndexes\":[");
        for ( long i = 0; i < triangles.size() && i < 3L*mesh.getNumTriangles(); i++ ) {
            if ( i > 0 ) {
                sb.append(',');
            }
            sb.append(triangles[i]);
        }
        sb.append(']');
    }
    sb.append('}');
}

void MCPSceneSerializer::appendMaterial(java::StringBuilder& sb,
                                        const SimpleMaterial* material)
{
    if ( material == nullptr ) {
        sb.append("null");
        return;
    }
    sb.append("{\"name\":\"").append(MCPJson::escape(material->getName())).append('"')
        .append(",\"ambient\":").append(MCPJson::color(material->getAmbient()))
        .append(",\"diffuse\":").append(MCPJson::color(material->getDiffuse()))
        .append(",\"specular\":").append(MCPJson::color(material->getSpecular()))
        .append(",\"emission\":").append(MCPJson::color(material->getEmission()))
        .append(",\"transmittance\":").append(MCPJson::color(material->getTransmittance()))
        .append(",\"phongExponent\":").append(material->getPhongExponent())
        .append(",\"reflectionCoefficient\":").append(material->getReflectionCoefficient())
        .append(",\"refractionCoefficient\":").append(material->getRefractionCoefficient())
        .append(",\"indexOfRefraction\":").append(material->getIndexOfRefraction())
        .append(",\"opacity\":").append(material->getOpacity())
        .append(",\"doubleSided\":").append(material->isDoubleSided())
        .append('}');
}

//= Lights ==================================================================

void MCPSceneSerializer::appendLight(java::StringBuilder& sb, int index,
                                     Light* light)
{
    sb.append("{\"index\":").append(index)
        .append(",\"type\":\"").append(light->getClassSimpleName()).append('"')
        .append(",\"position\":").append(MCPJson::vector(light->getPosition()))
        .append(",\"emission\":").append(MCPJson::color(light->getEmission()));
    SpotLight* spot = dynamic_cast<SpotLight*>(light);
    if ( full && spot != nullptr ) {
        sb.append(",\"pointsAt\":").append(MCPJson::vector(spot->getPointsAt()))
            .append(",\"coefficient\":").append(spot->getCoefficient())
            .append(",\"radius\":").append(spot->getRadius())
            .append(",\"falloff\":").append(spot->getFalloff());
    }
    sb.append('}');
}
