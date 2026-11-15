#ifndef __SLD_TXTR_CRDNT_MPPR__
#define __SLD_TXTR_CRDNT_MPPR__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/media/solidTexture/from2d/ControlledRGBAImageHDRUncompressed.h"
class SolidTextureCoordinateMapper {
  private:
    BOOLEAN cylindricalImageMap(
        double x, double y, double z, const ControlledRGBAImageHDRUncompressed *image, double *u, double *v) const;
    BOOLEAN torusImageMap(
        double x, double y, double z, const ControlledRGBAImageHDRUncompressed *image, double *u, double *v) const;
    BOOLEAN sphericalImageMap(
        double x, double y, double z, const RGBAImageHDRUncompressed *image, double *u, double *v) const;
    BOOLEAN planarImageMap(
        double x, double y, double z, const ControlledRGBAImageHDRUncompressed *image, double *u, double *v) const;

  public:
    BOOLEAN map(
        double x, double y, double z,
        const ControlledRGBAImageHDRUncompressed *image,
        double *xCoordinate, double *yCoordinate, double smallTolerance) const;
};

#endif
