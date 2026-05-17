#include "SimpleMaterial.h"

#include <sstream>

SimpleMaterial::SimpleMaterial()
    : SimpleMaterial(
        "VSDK_default_material",
        ColorRgb(0.1, 0.1, 0.1),
        ColorRgb(0.9, 0.5, 0.5),
        ColorRgb(1.0, 1.0, 1.0),
        true,
        0.0,
        0.0,
        1.0,
        128.0)
{
}

SimpleMaterial::SimpleMaterial(const SimpleMaterial& m)
    : SimpleMaterial(
        m.name,
        m.ambient,
        m.diffuse,
        m.specular,
        m.doubleSided,
        m.reflectionCoefficient,
        m.refractionCoefficient,
        m.opacity,
        m.phongExponent)
{
}

SimpleMaterial::SimpleMaterial(
    const std::string& inName,
    const ColorRgb& inAmbient,
    const ColorRgb& inDiffuse,
    const ColorRgb& inSpecular,
    bool inDoubleSided,
    double inReflectionCoefficient,
    double inRefractionCoefficient,
    double inOpacity,
    double inPhongExponent)
    : ambient(inAmbient),
      diffuse(inDiffuse),
      specular(inSpecular),
      doubleSided(inDoubleSided),
      reflectionCoefficient(inReflectionCoefficient),
      refractionCoefficient(inRefractionCoefficient),
      name(inName),
      opacity(inOpacity),
      phongExponent(inPhongExponent)
{
}

const std::string& SimpleMaterial::getName() const { return name; }

SimpleMaterial SimpleMaterial::withName(const std::string& n) const { return SimpleMaterial(n, ambient, diffuse, specular, doubleSided, reflectionCoefficient, refractionCoefficient, opacity, phongExponent); }
SimpleMaterial SimpleMaterial::withAmbient(const ColorRgb& a) const { return SimpleMaterial(name, a, diffuse, specular, doubleSided, reflectionCoefficient, refractionCoefficient, opacity, phongExponent); }
SimpleMaterial SimpleMaterial::withDiffuse(const ColorRgb& d) const { return SimpleMaterial(name, ambient, d, specular, doubleSided, reflectionCoefficient, refractionCoefficient, opacity, phongExponent); }
SimpleMaterial SimpleMaterial::withSpecular(const ColorRgb& s) const { return SimpleMaterial(name, ambient, diffuse, s, doubleSided, reflectionCoefficient, refractionCoefficient, opacity, phongExponent); }
SimpleMaterial SimpleMaterial::withPhongExponent(double p) const { return SimpleMaterial(name, ambient, diffuse, specular, doubleSided, reflectionCoefficient, refractionCoefficient, opacity, p); }
SimpleMaterial SimpleMaterial::withReflectionCoefficient(double kr) const { return SimpleMaterial(name, ambient, diffuse, specular, doubleSided, kr, refractionCoefficient, opacity, phongExponent); }
SimpleMaterial SimpleMaterial::withRefractionCoefficient(double kr) const { return SimpleMaterial(name, ambient, diffuse, specular, doubleSided, reflectionCoefficient, kr, opacity, phongExponent); }
SimpleMaterial SimpleMaterial::withOpacity(double a) const { return SimpleMaterial(name, ambient, diffuse, specular, doubleSided, reflectionCoefficient, refractionCoefficient, a, phongExponent); }
SimpleMaterial SimpleMaterial::withDoubleSided(bool b) const { return SimpleMaterial(name, ambient, diffuse, specular, b, reflectionCoefficient, refractionCoefficient, opacity, phongExponent); }

bool SimpleMaterial::isDoubleSided() const { return doubleSided; }

ColorRgb SimpleMaterial::getAmbient() const { return ColorRgb(ambient); }
const ColorRgb& SimpleMaterial::getAmbientReference() const { return ambient; }
ColorRgb SimpleMaterial::getDiffuse() const { return ColorRgb(diffuse); }
const ColorRgb& SimpleMaterial::getDiffuseReference() const { return diffuse; }
ColorRgb SimpleMaterial::getSpecular() const { return ColorRgb(specular); }
const ColorRgb& SimpleMaterial::getSpecularReference() const { return specular; }

double SimpleMaterial::getPhongExponent() const { return phongExponent; }
double SimpleMaterial::getReflectionCoefficient() const { return reflectionCoefficient; }
double SimpleMaterial::getRefractionCoefficient() const { return refractionCoefficient; }
double SimpleMaterial::getOpacity() const { return opacity; }

std::string SimpleMaterial::toString() const
{
    std::ostringstream ss;
    ss << "SimpleMaterial [" << name << "]:\n"
       << "  - Specular (" << specular.r() << ", " << specular.g() << ", " << specular.b() << ")\n"
       << "  - Diffuse (" << diffuse.r() << ", " << diffuse.g() << ", " << diffuse.b() << ")\n"
       << "  - Ambient (" << ambient.r() << ", " << ambient.g() << ", " << ambient.b() << ")\n"
       << "  - Phong exponent: " << phongExponent << "\n"
       << (doubleSided ? "  - Double sided\n\n" : "  - Single sided\n\n");
    return ss.str();
}
