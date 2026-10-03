#include <glad/gl.h>
#include <algorithm>
#include <cmath>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/validation/_PolyhedralBoundedSolidFaceValidator.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/render/hiddenLine/HiddenLineRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4LineRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4RendererConfigurationShaderSelector.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4SimpleMaterialRenderer.h"
#include "vsdk/toolkit/render/opengl4/polyhedralBoundedSolid/OpenGL4PolyhedralBoundedSolidDebugRenderer.h"
#include "vsdk/toolkit/render/opengl4/polyhedralBoundedSolid/OpenGL4PolyhedralBoundedSolidRenderer.h"

namespace {

const float NORMAL_LINE_LENGTH = 0.2f;
const float WIREFRAME_DEPTH_BIAS = -1.0e-4f;
const float POINTS_DEPTH_BIAS = -2.0e-4f;
const float HIGHLIGHT_DEPTH_BIAS = -3.0e-4f;
const double EDGE_ARROW_SIZE = 0.5;
const double EDGE_ARROW_CURVE_OFFSET = 0.1;
const int CURVED_ARROW_SEGMENTS = 10;

/**
Lines (and points) of an overlay, as (x, y, z) positions and (r, g, b)
colors of their vertices.
*/
struct DebugLines {
    java::ArrayList<float> positions;
    java::ArrayList<float> colors;
    java::ArrayList<float> pointPositions;
    java::ArrayList<float> pointColors;
};

void appendColor(java::ArrayList<float>& colors, const ColorRgb& color)
{
    colors.add((float)color.r());
    colors.add((float)color.g());
    colors.add((float)color.b());
}

void appendLine(java::ArrayList<float>& positions,
                java::ArrayList<float>& colors,
                const Vector3Dd& a, const Vector3Dd& b, const ColorRgb& color)
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

void appendPoint(java::ArrayList<float>& positions,
                 java::ArrayList<float>& colors,
                 const Vector3Dd& p, const ColorRgb& color)
{
    positions.add((float)p.x());
    positions.add((float)p.y());
    positions.add((float)p.z());
    appendColor(colors, color);
}

void appendCorner(java::ArrayList<float>& positions,
                  java::ArrayList<float>& colors,
                  const Vector3Dd& origin, const Vector3Dd& dx,
                  const Vector3Dd& dy, const Vector3Dd& dz,
                  const ColorRgb& color)
{
    appendLine(positions, colors, origin, origin.add(dx), color);
    appendLine(positions, colors, origin, origin.add(dy), color);
    appendLine(positions, colors, origin, origin.add(dz), color);
}

void buildBoxLinePositions(const double* minmax,
                           java::ArrayList<float>& out)
{
    // Pairs of (x index, y index, z index) of the minmax array for the ends
    // of the 12 edges of the box
    const int ends[24][3] = {
        {0, 1, 2}, {3, 1, 2}, {3, 1, 2}, {3, 4, 2},
        {3, 4, 2}, {0, 4, 2}, {0, 4, 2}, {0, 1, 2},

        {0, 1, 5}, {3, 1, 5}, {3, 1, 5}, {3, 4, 5},
        {3, 4, 5}, {0, 4, 5}, {0, 4, 5}, {0, 1, 5},

        {0, 1, 2}, {0, 1, 5}, {3, 1, 2}, {3, 1, 5},
        {3, 4, 2}, {3, 4, 5}, {0, 4, 2}, {0, 4, 5}
    };
    for ( int i = 0; i < 24; i++ ) {
        out.add((float)minmax[ends[i][0]]);
        out.add((float)minmax[ends[i][1]]);
        out.add((float)minmax[ends[i][2]]);
    }
}

void buildUniformColors(const ColorRgb& color, int vertexCount,
                        java::ArrayList<float>& out)
{
    for ( int i = 0; i < vertexCount; i++ ) {
        appendColor(out, color);
    }
}

ColorRgb colorForIndex(int index)
{
    switch ( index % 8 ) {
      case 0: return ColorRgb(1, 0, 0);
      case 1: return ColorRgb(0, 1, 0);
      case 2: return ColorRgb(0, 0, 1);
      case 3: return ColorRgb(0, 1, 1);
      case 4: return ColorRgb(1, 0, 1);
      case 5: return ColorRgb(0.5, 0, 0);
      case 6: return ColorRgb(0, 0.5, 0);
      default: return ColorRgb(0.6, 0.5, 0.4);
    }
}

double curveFactor(double axisDistance, double fullLength,
                   double curveOffsetPercent)
{
    double percent = axisDistance / fullLength;
    return curveOffsetPercent * fullLength * std::sin(percent * M_PI);
}

double curveSlope(double axisDistance, double fullLength,
                  double curveOffsetPercent)
{
    double percent = axisDistance / fullLength;
    return curveOffsetPercent * M_PI * std::cos(percent * M_PI);
}

Vector3Dd curvedArrowPoint(const Vector3Dd& startPoint,
                           const Vector3Dd& tangentAxis,
                           const Vector3Dd& curveAxis,
                           double invert,
                           double axisDistance,
                           double fullLength,
                           double curveOffsetPercent)
{
    return startPoint.add(tangentAxis.multiply(axisDistance).add(
        curveAxis.multiply(invert *
            curveFactor(axisDistance, fullLength, curveOffsetPercent))));
}

void appendCurvedArrowOverPlane(
    java::ArrayList<float>& positions,
    java::ArrayList<float>& colors,
    const Vector3Dd& startPoint,
    const Vector3Dd& endPoint,
    const Vector3Dd& planeNormal,
    double invert,
    double sizePercent,
    double curveOffsetPercent,
    const ColorRgb& color)
{
    Vector3Dd axis = endPoint.subtract(startPoint);
    double fullLength = axis.length();
    if ( fullLength <= 1e-12 ) {
        return;
    }

    sizePercent = std::min(1.0, std::max(VSDK::EPSILON, sizePercent));
    curveOffsetPercent = std::min(1.0, std::max(0.0, curveOffsetPercent));

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

void appendFaceBoundaryArrow(
    java::ArrayList<float>& positions,
    java::ArrayList<float>& colors,
    _PolyhedralBoundedSolidHalfEdge* he,
    _PolyhedralBoundedSolidHalfEdge* next,
    double invert,
    const ColorRgb& color)
{
    if ( he == nullptr || next == nullptr || he->startingVertex == nullptr ||
         next->startingVertex == nullptr ) {
        return;
    }

    _PolyhedralBoundedSolidHalfEdge* nextNext = next->next();
    if ( nextNext == nullptr || nextNext->startingVertex == nullptr ) {
        appendLine(positions, colors, he->startingVertex->position,
            next->startingVertex->position, color);
        return;
    }

    Vector3Dd startPoint = he->startingVertex->position;
    Vector3Dd endPoint = next->startingVertex->position;
    Vector3Dd thirdPoint = nextNext->startingVertex->position;
    Vector3Dd a = endPoint.subtract(startPoint);
    Vector3Dd b = thirdPoint.subtract(startPoint);
    if ( a.length() <= VSDK::EPSILON || b.length() <= VSDK::EPSILON ) {
        appendLine(positions, colors, startPoint, endPoint, color);
        return;
    }

    Vector3Dd planeNormal = a.normalized().crossProduct(b.normalized());
    if ( planeNormal.length() <= VSDK::EPSILON ) {
        InfinitePlane* containingPlane = he->parentLoop != nullptr &&
            he->parentLoop->parentFace != nullptr
            ? he->parentLoop->parentFace->getContainingPlane()
            : nullptr;
        if ( containingPlane == nullptr ||
             containingPlane->getNormal().length() <= VSDK::EPSILON ) {
            appendLine(positions, colors, startPoint, endPoint, color);
            return;
        }
        planeNormal = containingPlane->getNormal();
    }

    appendCurvedArrowOverPlane(positions, colors, startPoint, endPoint,
        planeNormal.normalized(), invert, EDGE_ARROW_SIZE,
        EDGE_ARROW_CURVE_OFFSET, color);
}

void appendFaceBoundaryLines(_PolyhedralBoundedSolidFace* face,
                             java::ArrayList<float>& positions,
                             java::ArrayList<float>& colors,
                             const ColorRgb& color)
{
    if ( face == nullptr ) {
        return;
    }
    for ( long j = 0; j < face->boundariesList.size(); j++ ) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(j);
        if ( loop == nullptr || loop->boundaryStartHalfEdge == nullptr ) {
            continue;
        }
        _PolyhedralBoundedSolidHalfEdge* he = loop->boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge* start = he;
        do {
            _PolyhedralBoundedSolidHalfEdge* next = he->next();
            if ( next == nullptr ) {
                break;
            }
            appendFaceBoundaryArrow(positions, colors, he, next,
                j == 0 ? 1.0 : -1.0, color);
            he = next;
        } while ( he != start );
    }
}

void buildSolidEdgeLines(PolyhedralBoundedSolid* solid, DebugLines& out)
{
    for ( long i = 0; i < solid->getEdgesList().size(); i++ ) {
        _PolyhedralBoundedSolidEdge* edge = solid->getEdgesList().get(i);
        if ( edge->rightHalf == nullptr || edge->leftHalf == nullptr ) {
            continue;
        }
        const Vector3Dd& start = edge->rightHalf->startingVertex->position;
        const Vector3Dd& end = edge->leftHalf->startingVertex->position;
        appendLine(out.positions, out.colors, start, end, edge->debugColor);
    }
}

void buildSolidPointCloud(PolyhedralBoundedSolid* solid, DebugLines& out)
{
    for ( long i = 0; i < solid->getVerticesList().size(); i++ ) {
        _PolyhedralBoundedSolidVertex* vertex = solid->getVerticesList().get(i);
        appendPoint(out.pointPositions, out.pointColors, vertex->position,
            vertex->debugColor);
    }
}

void buildVertexNormalLines(PolyhedralBoundedSolid* solid, DebugLines& out)
{
    ColorRgb yellow(1, 1, 0);

    for ( long i = 0; i < solid->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(i);
        InfinitePlane* plane = face->getContainingPlane();
        if ( plane == nullptr ) {
            continue;
        }
        Vector3Dd normal = plane->getNormal().normalized();

        for ( long j = 0; j < face->boundariesList.size(); j++ ) {
            _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(j);
            if ( loop == nullptr || loop->boundaryStartHalfEdge == nullptr ) {
                continue;
            }
            _PolyhedralBoundedSolidHalfEdge* he = loop->boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge* start = he;
            do {
                he = he->next();
                if ( he == nullptr ) {
                    break;
                }
                const Vector3Dd& p = he->startingVertex->position;
                appendLine(out.positions, out.colors,
                    p.add(normal.multiply(NORMAL_LINE_LENGTH / 100.0)),
                    p.add(normal.multiply(NORMAL_LINE_LENGTH)), yellow);
            } while ( he != start );
        }
    }
}

void buildBoundingVolumeLines(PolyhedralBoundedSolid* solid,
                              const RendererConfiguration* quality,
                              DebugLines& out)
{
    double* minmax = solid->getMinMax();
    buildBoxLinePositions(minmax, out.positions);
    delete[] minmax;
    buildUniformColors(quality->getBoundingVolumeColor(), 24, out.colors);
}

void buildSelectionCornerLines(PolyhedralBoundedSolid* solid, DebugLines& out)
{
    double* minmax = solid->getMinMax();
    Vector3Dd min(minmax[0], minmax[1], minmax[2]);
    Vector3Dd max(minmax[3], minmax[4], minmax[5]);
    delete[] minmax;
    Vector3Dd delta = max.subtract(min);
    min = min.subtract(delta.multiply(0.01));
    max = max.add(delta.multiply(0.01));
    delta = delta.multiply(0.25);

    java::ArrayList<float>& positions = out.positions;
    java::ArrayList<float>& colors = out.colors;
    ColorRgb white(1, 1, 1);

    appendCorner(positions, colors, min, Vector3Dd(delta.x(), 0, 0),
        Vector3Dd(0, delta.y(), 0), Vector3Dd(0, 0, delta.z()), white);
    appendCorner(positions, colors, Vector3Dd(max.x(), min.y(), min.z()),
        Vector3Dd(-delta.x(), 0, 0), Vector3Dd(0, delta.y(), 0),
        Vector3Dd(0, 0, delta.z()), white);
    appendCorner(positions, colors, Vector3Dd(min.x(), max.y(), min.z()),
        Vector3Dd(delta.x(), 0, 0), Vector3Dd(0, -delta.y(), 0),
        Vector3Dd(0, 0, delta.z()), white);
    appendCorner(positions, colors, Vector3Dd(min.x(), min.y(), max.z()),
        Vector3Dd(delta.x(), 0, 0), Vector3Dd(0, delta.y(), 0),
        Vector3Dd(0, 0, -delta.z()), white);
    appendCorner(positions, colors, Vector3Dd(max.x(), max.y(), min.z()),
        Vector3Dd(-delta.x(), 0, 0), Vector3Dd(0, -delta.y(), 0),
        Vector3Dd(0, 0, delta.z()), white);
    appendCorner(positions, colors, Vector3Dd(max.x(), min.y(), max.z()),
        Vector3Dd(-delta.x(), 0, 0), Vector3Dd(0, delta.y(), 0),
        Vector3Dd(0, 0, -delta.z()), white);
    appendCorner(positions, colors, Vector3Dd(min.x(), max.y(), max.z()),
        Vector3Dd(delta.x(), 0, 0), Vector3Dd(0, -delta.y(), 0),
        Vector3Dd(0, 0, -delta.z()), white);
    appendCorner(positions, colors, max,
        Vector3Dd(-delta.x(), 0, 0), Vector3Dd(0, -delta.y(), 0),
        Vector3Dd(0, 0, -delta.z()), white);
}

void buildNonPlanarFaceHighlights(PolyhedralBoundedSolid* solid,
                                  DebugLines& out)
{
    ColorRgb yellow(1, 1, 0);

    for ( long i = 0; i < solid->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(i);
        if ( _PolyhedralBoundedSolidFaceValidator::isSurfaceDegenerate(face) ) {
            appendFaceBoundaryLines(face, out.positions, out.colors, yellow);
        }
    }
}

void buildFaceBoundaryLines(PolyhedralBoundedSolid* solid, int faceIndex,
                            DebugLines& out)
{
    if ( solid == nullptr || faceIndex < -1 ) {
        return;
    }

    for ( long i = 0; i < solid->getPolygonsList().size(); i++ ) {
        if ( faceIndex > -1 && i != faceIndex ) {
            continue;
        }
        appendFaceBoundaryLines(solid->getPolygonsList().get(i),
            out.positions, out.colors, colorForIndex((int)i));
    }
}

void buildDebugEdgeLines(PolyhedralBoundedSolid* solid, Camera* camera,
                         int edgeIndex, DebugLines& out)
{
    for ( long i = 0; edgeIndex >= -1 && i < solid->getEdgesList().size(); i++ ) {
        if ( i != edgeIndex && edgeIndex > -1 ) {
            continue;
        }

        _PolyhedralBoundedSolidEdge* edge = solid->getEdgesList().get(i);
        if ( edge->leftHalf == nullptr || edge->rightHalf == nullptr ) {
            continue;
        }
        Vector3Dd start = edge->rightHalf->startingVertex->position;
        Vector3Dd end = edge->leftHalf->startingVertex->position;
        _PolyhedralBoundedSolidFace* face1 = edge->leftHalf->parentLoop->parentFace;
        _PolyhedralBoundedSolidFace* face2 = edge->rightHalf->parentLoop->parentFace;

        ColorRgb color(0.8, 0, 0);
        if ( camera != nullptr ) {
            bool f1 = HiddenLineRenderer::isFaceVisibleFromCamera(face1,
                camera) >= 0;
            bool f2 = HiddenLineRenderer::isFaceVisibleFromCamera(face2,
                camera) >= 0;
            if ( !f1 && !f2 ) {
                color = ColorRgb(0, 0, 0);
            }
            else if ( f1 != f2 ) {
                color = ColorRgb(1, 0, 0);
            }
        }
        appendLine(out.positions, out.colors, start, end, color);

        if ( edgeIndex > -1 && camera != nullptr ) {
            Vector3Dd middle = start.add(end).multiply(0.5);
            Vector3Dd n1 = face1->getContainingPlane()->getNormal();
            Vector3Dd n2 = face2->getContainingPlane()->getNormal();
            appendLine(out.positions, out.colors, middle,
                middle.add(n1.multiply(0.1)), ColorRgb(1, 1, 0));
            appendLine(out.positions, out.colors, middle,
                middle.add(n2.multiply(0.1)), ColorRgb(0, 1, 1));

            Vector3Dd d = end.subtract(start);
            double length = d.length();
            if ( length > VSDK::EPSILON ) {
                d = d.normalized();
                for ( double t = VSDK::EPSILON; t < length;
                      t += (length / 20.0) ) {
                    Vector3Dd p = start.add(d.multiply(t));
                    int qi = solid->computeQuantitativeInvisibility(
                        camera->getPosition(), p);
                    appendPoint(out.pointPositions, out.pointColors, p,
                        qi == 0 ? ColorRgb(0, 1, 0) : ColorRgb(0, 0, 1));
                }
            }
        }
    }
}

}

void OpenGL4PolyhedralBoundedSolidDebugRenderer::drawDebugOverlays(
    PolyhedralBoundedSolid* solid, Camera* /*camera*/,
    const RendererConfiguration* quality,
    const Matrix4x4d& modelViewProjection)
{
    if ( solid == nullptr || quality == nullptr ) {
        return;
    }

    if ( quality->isWiresSet() ) {
        DebugLines lines;
        buildSolidEdgeLines(solid, lines);
        if ( lines.positions.size() > 0 ) {
            OpenGL4LineRenderer::drawLines(modelViewProjection,
                lines.positions, lines.colors, 1.0f, WIREFRAME_DEPTH_BIAS);
        }
    }

    if ( quality->isPointsSet() ) {
        DebugLines points;
        buildSolidPointCloud(solid, points);
        if ( points.pointPositions.size() > 0 ) {
            OpenGL4PolyhedralBoundedSolidRenderer::drawColoredPrimitives(
                modelViewProjection, points.pointPositions,
                points.pointColors, GL_POINTS, 6.0f, POINTS_DEPTH_BIAS);
        }
    }

    if ( quality->isNormalsSet() ) {
        DebugLines normals;
        buildVertexNormalLines(solid, normals);
        if ( normals.positions.size() > 0 ) {
            OpenGL4LineRenderer::drawLines(modelViewProjection,
                normals.positions, normals.colors, 1.0f,
                HIGHLIGHT_DEPTH_BIAS);
        }
    }

    if ( quality->isBoundingVolumeSet() ) {
        DebugLines bounds;
        buildBoundingVolumeLines(solid, quality, bounds);
        if ( bounds.positions.size() > 0 ) {
            OpenGL4LineRenderer::drawLines(modelViewProjection,
                bounds.positions, bounds.colors, 1.0f, HIGHLIGHT_DEPTH_BIAS);
        }
    }

    if ( quality->isSelectionCornersSet() ) {
        DebugLines corners;
        buildSelectionCornerLines(solid, corners);
        if ( corners.positions.size() > 0 ) {
            OpenGL4LineRenderer::drawLines(modelViewProjection,
                corners.positions, corners.colors, 1.0f,
                HIGHLIGHT_DEPTH_BIAS);
        }
    }

    DebugLines nonPlanar;
    buildNonPlanarFaceHighlights(solid, nonPlanar);
    if ( nonPlanar.positions.size() > 0 ) {
        OpenGL4LineRenderer::drawLines(modelViewProjection,
            nonPlanar.positions, nonPlanar.colors, 4.0f,
            HIGHLIGHT_DEPTH_BIAS);
    }
}

void OpenGL4PolyhedralBoundedSolidDebugRenderer::drawDebugFaceBoundary(
    PolyhedralBoundedSolid* solid, int faceIndex,
    const Matrix4x4d& modelViewProjection)
{
    if ( solid == nullptr ) {
        return;
    }
    DebugLines lines;
    buildFaceBoundaryLines(solid, faceIndex, lines);
    if ( lines.positions.size() == 0 ) {
        return;
    }
    OpenGL4LineRenderer::drawLines(modelViewProjection, lines.positions,
        lines.colors, 2.0f, HIGHLIGHT_DEPTH_BIAS);
}

void OpenGL4PolyhedralBoundedSolidDebugRenderer::drawDebugFace(
    PolyhedralBoundedSolid* solid, int faceIndex,
    const Matrix4x4d& modelMatrix, const Matrix4x4d& modelViewProjection,
    Camera* camera)
{
    if ( solid == nullptr || faceIndex < 0 ) {
        return;
    }

    OpenGL4PolyhedralBoundedSolidRenderer::ensureInitialized();

    Matrix4x4d modelViewITLocal = modelMatrix.invert().transpose();
    SimpleMaterial material = OpenGL4SimpleMaterialRenderer::getActiveMaterial()
        .withDiffuse(ColorRgb(1.0, 0.0, 0.0))
        .withAmbient(ColorRgb(1.0, 0.0, 0.0))
        .withSpecular(ColorRgb(0.0, 0.0, 0.0));

    OpenGL4PolyhedralBoundedSolidRenderer::MeshData mesh;
    OpenGL4PolyhedralBoundedSolidRenderer::buildFaceMesh(solid, faceIndex,
                                                         mesh);
    if ( mesh.vertexCount == 0 ) {
        return;
    }

    RendererConfiguration quality;
    quality.setTexture(false);
    quality.setUseVertexColors(false);
    quality.setShadingType(RendererConfiguration::SHADING_TYPE_NOLIGHT);

    unsigned int programId = OpenGL4RendererConfigurationShaderSelector
        ::selectSurfaceShaderProgram(&quality, false, false);
    OpenGL4PolyhedralBoundedSolidRenderer::configureSurfaceProgram(
        programId, modelViewProjection, modelMatrix, modelViewITLocal,
        material, nullptr, &quality,
        camera != nullptr ? camera->getPosition() : Vector3Dd(0, 0, 0));
    OpenGL4PolyhedralBoundedSolidRenderer::renderMesh(mesh, GL_TRIANGLES);
    OpenGL4RendererConfigurationShaderSelector::deactivateShader();
}

void OpenGL4PolyhedralBoundedSolidDebugRenderer::drawDebugEdges(
    PolyhedralBoundedSolid* solid, Camera* camera, int edgeIndex,
    const Matrix4x4d& modelViewProjection)
{
    if ( solid == nullptr ) {
        return;
    }

    DebugLines lines;
    buildDebugEdgeLines(solid, camera, edgeIndex, lines);
    if ( lines.positions.size() > 0 ) {
        OpenGL4LineRenderer::drawLines(modelViewProjection, lines.positions,
            lines.colors, 2.0f, HIGHLIGHT_DEPTH_BIAS);
    }
    if ( lines.pointPositions.size() > 0 ) {
        OpenGL4PolyhedralBoundedSolidRenderer::drawColoredPrimitives(
            modelViewProjection, lines.pointPositions, lines.pointColors,
            GL_POINTS, 4.0f, HIGHLIGHT_DEPTH_BIAS);
    }
}
