#ifndef __VSDK_TOOLKIT_ENVIRONMENT_MATERIAL_SIMPLEMATERIAL_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_MATERIAL_SIMPLEMATERIAL_H__

#include "vsdk/toolkit/common/color/ColorRgb.h"

#include <string>

class SimpleMaterial {
private:
    ColorRgb ambient;
    ColorRgb diffuse;
    ColorRgb specular;
    bool doubleSided;
    double reflectionCoefficient;
    double refractionCoefficient;
    std::string name;
    double opacity;
    double phongExponent;

public:
    SimpleMaterial();
    SimpleMaterial(const SimpleMaterial& m);
    SimpleMaterial(
        const std::string& name,
        const ColorRgb& ambient,
        const ColorRgb& diffuse,
        const ColorRgb& specular,
        bool doubleSided,
        double reflectionCoefficient,
        double refractionCoefficient,
        double opacity,
        double phongExponent);
    virtual ~SimpleMaterial() {}

    const std::string& getName() const;

    SimpleMaterial withName(const std::string& n) const;
    SimpleMaterial withAmbient(const ColorRgb& a) const;
    SimpleMaterial withDiffuse(const ColorRgb& d) const;
    SimpleMaterial withSpecular(const ColorRgb& s) const;
    SimpleMaterial withPhongExponent(double p) const;
    SimpleMaterial withReflectionCoefficient(double kr) const;
    SimpleMaterial withRefractionCoefficient(double kr) const;
    SimpleMaterial withOpacity(double a) const;
    SimpleMaterial withDoubleSided(bool b) const;

    bool isDoubleSided() const;

    ColorRgb getAmbient() const;
    const ColorRgb& getAmbientReference() const;
    ColorRgb getDiffuse() const;
    const ColorRgb& getDiffuseReference() const;
    ColorRgb getSpecular() const;
    const ColorRgb& getSpecularReference() const;

    double getPhongExponent() const;
    double getReflectionCoefficient() const;
    double getRefractionCoefficient() const;
    double getOpacity() const;

    std::string toString() const;
};

#endif
