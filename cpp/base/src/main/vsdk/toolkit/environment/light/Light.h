#ifndef __VSDK_TOOLKIT_ENVIRONMENT_LIGHT_LIGHT_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_LIGHT_LIGHT_H__

#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

#include <string>

class Light {
public:
    int tipo_de_luz;
    Vector3Dd lvec;

private:
    ColorRgb ambient;
    ColorRgb diffuse;
    ColorRgb specular;
    int id;
    std::string name;

public:
    Light(int type, const Vector3Dd& pos, const ColorRgb& emission);
    virtual ~Light() {}

    const std::string& getName() const;
    void setName(const std::string& n);

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
