#ifndef __CONTROLLED_RGBA_IMAGE_HDR_UNCOMPRESSED__
#define __CONTROLLED_RGBA_IMAGE_HDR_UNCOMPRESSED__

#include <stddef.h>
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/media/IndexedColorImageHDRUncompressed.h"
#include "vsdk/toolkit/media/RGBAImageHDRUncompressed.h"
#include "vsdk/toolkit/media/solidTexture/from2d/ImageToSolidTextureInterpolationTypes.h"
#include "vsdk/toolkit/media/solidTexture/from2d/ImageToSolidTextureProjectionMethods.h"
class ControlledRGBAImageHDRUncompressed : public RGBAImageHDRUncompressed {
  private:
    ImageToSolidTextureProjectionMethods mapType;
    ImageToSolidTextureInterpolationTypes interpolationType;
    bool onceFlag;
    bool useColorFlag;
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
    bool getOnceFlag() const;
    void setOnceFlag(bool v);
    bool getUseColorFlag() const;
    void setUseColorFlag(bool v);
    const Vector3Dd& getImageGradient() const;
    void setImageGradient(const Vector3Dd& v);
    IndexedColorImageHDRUncompressed* getIndexedData() const;
    void setIndexedData(IndexedColorImageHDRUncompressed* v);
};

#endif
