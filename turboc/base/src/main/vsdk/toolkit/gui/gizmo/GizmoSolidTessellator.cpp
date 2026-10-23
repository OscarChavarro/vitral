#include <math.h>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/gui/gizmo/GizmoSolidTessellator.h"

/// The 8 corners of a box, indexed by (sx,sy,sz) sign combination as
/// ((sx+1)/2)*4 + ((sy+1)/2)*2 + (sz+1)/2; each row is a face, given as a
/// valid (non self-intersecting) strip of 4 corner indexes
static const int BOX_FACES[GizmoSolidTessellator::BOX_NUMBER_OF_FACES][4] = {
    {0, 1, 2, 3}, {4, 5, 6, 7}, {0, 1, 4, 5}, {2, 3, 6, 7}, {0, 2, 4, 6}, {1, 3, 5, 7}
};

static double sliceCos(int i)
{
    return cos(2*M_PI*i/GizmoSolidTessellator::SLICES);
}

static double sliceSin(int i)
{
    return sin(2*M_PI*i/GizmoSolidTessellator::SLICES);
}

Matrix4x4d GizmoSolidTessellator::localTransform(const SimpleBody* element)
{
    Vector3Dd position = element->getPosition();

    return Matrix4x4d().translation(position).multiply(element->getRotation());
}

ArrayList<Vector3Dd> GizmoSolidTessellator::buildShaftStrip(
    const Matrix4x4d& local, double baseRadius, double topRadius, double height)
{
    ArrayList<Vector3Dd> strip;

    for ( int i = 0; i <= SLICES; i++ ) {
        strip.add(local.multiply(
            Vector3Dd(baseRadius*sliceCos(i), baseRadius*sliceSin(i), 0)));
        strip.add(local.multiply(
            Vector3Dd(topRadius*sliceCos(i), topRadius*sliceSin(i), height)));
    }
    return strip;
}

ArrayList<Vector3Dd> GizmoSolidTessellator::buildConeSideFan(
    const Matrix4x4d& local, double radius, double height)
{
    ArrayList<Vector3Dd> fan;

    fan.add(local.multiply(Vector3Dd(0, 0, height)));
    for ( int i = 0; i <= SLICES; i++ ) {
        fan.add(local.multiply(
            Vector3Dd(radius*sliceCos(i), radius*sliceSin(i), 0)));
    }
    return fan;
}

ArrayList<Vector3Dd> GizmoSolidTessellator::buildConeBaseFan(
    const Matrix4x4d& local, double radius)
{
    ArrayList<Vector3Dd> fan;

    fan.add(local.multiply(Vector3Dd(0, 0, 0)));
    for ( int i = 0; i <= SLICES; i++ ) {
        int k = SLICES - i;

        fan.add(local.multiply(
            Vector3Dd(radius*sliceCos(k), radius*sliceSin(k), 0)));
    }
    return fan;
}

void GizmoSolidTessellator::buildBoxFaceStrips(const Matrix4x4d& local,
    const Vector3Dd& size, Vector3Dd outFaces[BOX_NUMBER_OF_FACES][4])
{
    double hx = size.x()/2;
    double hy = size.y()/2;
    double hz = size.z()/2;
    Vector3Dd corners[8];
    int index = 0;

    for ( int sx = -1; sx <= 1; sx += 2 ) {
        for ( int sy = -1; sy <= 1; sy += 2 ) {
            for ( int sz = -1; sz <= 1; sz += 2 ) {
                corners[index++] = local.multiply(Vector3Dd(sx*hx, sy*hy, sz*hz));
            }
        }
    }

    for ( int face = 0; face < BOX_NUMBER_OF_FACES; face++ ) {
        for ( int corner = 0; corner < 4; corner++ ) {
            outFaces[face][corner] = corners[BOX_FACES[face][corner]];
        }
    }
}

ArrayList<Vector3Dd> GizmoSolidTessellator::buildPlaneQuad(
    const Matrix4x4d& local, double sizeX, double sizeY)
{
    double hx = sizeX/2;
    double hy = sizeY/2;
    ArrayList<Vector3Dd> quad;

    quad.add(local.multiply(Vector3Dd(-hx, -hy, 0)));
    quad.add(local.multiply(Vector3Dd(hx, -hy, 0)));
    quad.add(local.multiply(Vector3Dd(-hx, hy, 0)));
    quad.add(local.multiply(Vector3Dd(hx, hy, 0)));
    return quad;
}

// Integral constants bound to references (i.e. by `?:`) need a
// definition out of the class in C++98
