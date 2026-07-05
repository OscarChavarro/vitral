#ifndef __LIGHT__
#define __LIGHT__

#include "java/lang/String.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
class Light {
public:
    int tipo_de_luz;
    Vector3Dd lvec;

private:
    ColorRgb ambient;
    ColorRgb diffuse;
    ColorRgb specular;
    int id;
    java::String name;

public:
    Light(int type, const Vector3Dd& pos, const ColorRgb& emission);
    virtual ~Light() {}

    const java::String& getName() const;
    void setName(const java::String& n);

    int getId() const;
    void setId(int i);

    void setAmbient(const ColorRgb& a);
    void setDiffuse(const ColorRgb& d);
    void setSpecular(const ColorRgb& s);

    Vector3Dd getPosition() const;
    void setPosition(const Vector3Dd& pos);

    ColorRgb getAmbient() const;
    ColorRgb getDiffuse() const;
    ColorRgb getSpecular() const;
    const ColorRgb& getSpecularReference() const;

    int getLightType() const;
};

#endif
