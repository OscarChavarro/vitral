#include "vsdk/toolkit/common/VSDK.h"
#include <math.h>

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/render/raytracing/SnellRefraction.h"

BOOLEAN SnellRefraction::refract(const Vector3Dd& towardViewer, const Vector3Dd& normal,
                              double eta, Vector3Dd* outDirection)
{
    double cosI = towardViewer.dotProduct(normal);
    double k = 1.0 - eta*eta*(1.0 - cosI*cosI);
    if ( k < 0.0 ) {
        return FALSE;
    }
    double c = eta*cosI - sqrt(k);
    *outDirection = Vector3Dd(
        -eta*towardViewer.x() + c*normal.x(),
        -eta*towardViewer.y() + c*normal.y(),
        -eta*towardViewer.z() + c*normal.z());
    return TRUE;
}

Vector3Dd SnellRefraction::reflect(const Vector3Dd& towardViewer, const Vector3Dd& normal)
{
    double twoCos = 2.0*towardViewer.dotProduct(normal);
    return Vector3Dd(
        twoCos*normal.x() - towardViewer.x(),
        twoCos*normal.y() - towardViewer.y(),
        twoCos*normal.z() - towardViewer.z());
}
