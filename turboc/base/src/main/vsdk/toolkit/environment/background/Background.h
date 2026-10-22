#ifndef __BACKGROUND__
#define __BACKGROUND__

#include "vsdk/toolkit/common/Entity.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
class Background : public Entity {
public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    virtual ~Background() {}
    virtual ColorRgb colorInDireccion(const Vector3Dd& d) = 0;
};

#endif
