#ifndef __SURFACE_SHADING_POINT__
#define __SURFACE_SHADING_POINT__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Df.h"

/**
What the directional functions of PhongMaterial read about the surface point
being shaded: the shading normal and the texture coordinate, each with a flag
telling whether the caller could determine it. The caller finds the point,
its normal and its texture coordinate; the material only reads them.

Immutable value.
*/
class SurfaceShadingPoint {
  private:
    Vector3Df shadingNormal;
    Vector3Df textureCoordinate;
    BOOLEAN shadingNormalKnown;
    BOOLEAN textureCoordinateKnown;

  public:
    SurfaceShadingPoint(
        const Vector3Df &inShadingNormal,
        BOOLEAN inShadingNormalKnown,
        const Vector3Df &inTextureCoordinate,
        BOOLEAN inTextureCoordinateKnown) :
        shadingNormal(inShadingNormal),
        textureCoordinate(inTextureCoordinate),
        shadingNormalKnown(inShadingNormalKnown),
        textureCoordinateKnown(inTextureCoordinateKnown)
    {
    }

    /** Unit normal leaving the surface on the side the scattering is evaluated from */
    const Vector3Df &getShadingNormal() const { return shadingNormal; }
    BOOLEAN hasShadingNormal() const { return shadingNormalKnown; }

    /** Texture coordinate (x and y used) */
    const Vector3Df &getTextureCoordinate() const { return textureCoordinate; }
    BOOLEAN hasTextureCoordinate() const { return textureCoordinateKnown; }
};

#endif
