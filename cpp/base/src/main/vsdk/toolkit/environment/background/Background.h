#ifndef __VSDK_TOOLKIT_ENVIRONMENT_BACKGROUND_BACKGROUND_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_BACKGROUND_BACKGROUND_H__

#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
class Background {
public:
    virtual ~Background() {}
    virtual ColorRgb colorInDireccion(const Vector3Dd& d) = 0;
};

#endif
