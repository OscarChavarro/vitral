#include <cmath>

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/render/raytracing/SnellRefraction.h"

bool SnellRefraction::refract(const Vector3Dd& towardViewer, const Vector3Dd& normal,
                              double eta, Vector3Dd* outDirection)
{
    double cosI = towardViewer.dotProduct(normal);
    double k = 1.0 - eta*eta*(1.0 - cosI*cosI);
    if ( k < 0.0 ) {
        return false;
    }
    double c = eta*cosI - std::sqrt(k);
    *outDirection = Vector3Dd(
        -eta*towardViewer.x() + c*normal.x(),
        -eta*towardViewer.y() + c*normal.y(),
        -eta*towardViewer.z() + c*normal.z());
    return true;
}

Vector3Dd SnellRefraction::reflect(const Vector3Dd& towardViewer, const Vector3Dd& normal)
{
    double twoCos = 2.0*towardViewer.dotProduct(normal);
    return Vector3Dd(
        twoCos*normal.x() - towardViewer.x(),
        twoCos*normal.y() - towardViewer.y(),
        twoCos*normal.z() - towardViewer.z());
}
