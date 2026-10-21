#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/gui/gizmo/GizmoVertexArrayBuilder.h"

void GizmoVertexArrayBuilder::putVertex(ArrayList<float>& positions,
                                        const Vector3Dd& point)
{
    positions.add((float)point.x());
    positions.add((float)point.y());
    positions.add((float)point.z());
}

void GizmoVertexArrayBuilder::putRgb(ArrayList<float>& colors,
                                     const ColorRgb& color)
{
    colors.add((float)color.r());
    colors.add((float)color.g());
    colors.add((float)color.b());
}

void GizmoVertexArrayBuilder::putRgba(ArrayList<float>& colors,
                                      const ColorRgb& color, double alpha)
{
    colors.add((float)color.r());
    colors.add((float)color.g());
    colors.add((float)color.b());
    colors.add((float)alpha);
}

ArrayList<float> GizmoVertexArrayBuilder::buildPositions(
    const ArrayList<Vector3Dd>& points)
{
    ArrayList<float> positions;

    for ( long i = 0; i < points.size(); i++ ) {
        putVertex(positions, points.get(i));
    }
    return positions;
}

ArrayList<float> GizmoVertexArrayBuilder::buildRgbColors(
    long numberOfVertexes, const ColorRgb& color)
{
    ArrayList<float> colors;

    for ( long i = 0; i < numberOfVertexes; i++ ) {
        putRgb(colors, color);
    }
    return colors;
}

ArrayList<float> GizmoVertexArrayBuilder::buildRgbaColors(
    long numberOfVertexes, const ColorRgb& color, double alpha)
{
    ArrayList<float> colors;

    for ( long i = 0; i < numberOfVertexes; i++ ) {
        putRgba(colors, color, alpha);
    }
    return colors;
}
