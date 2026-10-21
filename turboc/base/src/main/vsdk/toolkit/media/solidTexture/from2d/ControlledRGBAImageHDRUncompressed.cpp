#include "vsdk/toolkit/media/solidTexture/from2d/ControlledRGBAImageHDRUncompressed.h"

ControlledRGBAImageHDRUncompressed::ControlledRGBAImageHDRUncompressed() :
    mapType(PLANAR_MAP),
    interpolationType(NO_INTERPOLATION),
    onceFlag(false),
    useColorFlag(true),
    imageGradient(),
    indexedData(NULL)
{
}

ControlledRGBAImageHDRUncompressed::~ControlledRGBAImageHDRUncompressed()
{
    delete indexedData;
}

int
ControlledRGBAImageHDRUncompressed::getMapType() const
{
    return mapType;
}

void
ControlledRGBAImageHDRUncompressed::setMapType(ImageToSolidTextureProjectionMethods v)
{
    mapType = v;
}

void
ControlledRGBAImageHDRUncompressed::setMapType(int v)
{
    switch (v) {
      case 1:
        mapType = SPHERICAL_MAP;
        break;
      case 2:
        mapType = CYLINDRICAL_MAP;
        break;
      case 5:
        mapType = TORUS_MAP;
        break;
      default:
        mapType = PLANAR_MAP;
        break;
    }
}

int
ControlledRGBAImageHDRUncompressed::getInterpolationType() const
{
    return interpolationType;
}

void
ControlledRGBAImageHDRUncompressed::setInterpolationType(ImageToSolidTextureInterpolationTypes v)
{
    interpolationType = v;
}

void
ControlledRGBAImageHDRUncompressed::setInterpolationType(int v)
{
    switch (v) {
      case 1:
        interpolationType = NEAREST_NEIGHBOR;
        break;
      case 2:
        interpolationType = BI_LINEAR;
        break;
      case 3:
        interpolationType = CUBIC_SPLINE;
        break;
      case 4:
        interpolationType = NORMALIZED_DIST;
        break;
      default:
        interpolationType = NO_INTERPOLATION;
        break;
    }
}

bool
ControlledRGBAImageHDRUncompressed::getOnceFlag() const
{
    return onceFlag;
}

void
ControlledRGBAImageHDRUncompressed::setOnceFlag(bool v)
{
    onceFlag = v;
}

bool
ControlledRGBAImageHDRUncompressed::getUseColorFlag() const
{
    return useColorFlag;
}

void
ControlledRGBAImageHDRUncompressed::setUseColorFlag(bool v)
{
    useColorFlag = v;
}

const Vector3Dd&
ControlledRGBAImageHDRUncompressed::getImageGradient() const
{
    return imageGradient;
}

void
ControlledRGBAImageHDRUncompressed::setImageGradient(const Vector3Dd& v)
{
    imageGradient = v;
}

IndexedColorImageHDRUncompressed*
ControlledRGBAImageHDRUncompressed::getIndexedData() const
{
    return indexedData;
}

void
ControlledRGBAImageHDRUncompressed::setIndexedData(IndexedColorImageHDRUncompressed* v)
{
    indexedData = v;
}
