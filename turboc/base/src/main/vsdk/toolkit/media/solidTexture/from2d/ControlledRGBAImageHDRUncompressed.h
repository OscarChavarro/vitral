#ifndef __CONTROLLED_RGBA_IMAGE_HDR_UNCOMPRESSED__
#define __CONTROLLED_RGBA_IMAGE_HDR_UNCOMPRESSED__

#include <stddef.h>
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/media/IndexedColorImageHDRUncompressed.h"
#include "vsdk/toolkit/media/RGBAImageHDRUncompressed.h"
#include "vsdk/toolkit/media/solidTexture/from2d/ImageToSolidTextureInterpolationTypes.h"
#include "vsdk/toolkit/media/solidTexture/from2d/ImageToSolidTextureProjectionMethods.h"
class ControlledRGBAImageHDRUncompressed : public RGBAImageHDRUncompressed {
  private:
    ImageToSolidTextureProjectionMethods mapType;
    ImageToSolidTextureInterpolationTypes interpolationType;
    BOOLEAN onceFlag;
    BOOLEAN useColorFlag;
    Vector3Dd imageGradient;
    IndexedColorImageHDRUncompressed *indexedData;

  public:
    ControlledRGBAImageHDRUncompressed();
    ~ControlledRGBAImageHDRUncompressed();
    int getMapType() const;
    void setMapType(ImageToSolidTextureProjectionMethods v);
    void setMapType(int v);
    int getInterpolationType() const;
    void setInterpolationType(ImageToSolidTextureInterpolationTypes v);
    void setInterpolationType(int v);
    BOOLEAN getOnceFlag() const;
    void setOnceFlag(BOOLEAN v);
    BOOLEAN getUseColorFlag() const;
    void setUseColorFlag(BOOLEAN v);
    const Vector3Dd& getImageGradient() const;
    void setImageGradient(const Vector3Dd& v);
    IndexedColorImageHDRUncompressed* getIndexedData() const;
    void setIndexedData(IndexedColorImageHDRUncompressed* v);
};

#endif
