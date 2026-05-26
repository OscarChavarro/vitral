#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "java/lang/String.h"

#include <sstream>
#include "java/lang/String.h"

SimpleMaterial::SimpleMaterial()
    : SimpleMaterial(
        "VSDK_default_material",
        ColorRgb(0.1, 0.1, 0.1),
        ColorRgb(0.9, 0.5, 0.5),
        ColorRgb(1.0, 1.0, 1.0),
        ColorRgb(0.0, 0.0, 0.0),
        ColorRgb(0.0, 0.0, 0.0),
        true,
        0.0,
        0.0,
        1.0,
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
        m.emission,
        m.transmittance,
        m.doubleSided,
        m.reflectionCoefficient,
        m.refractionCoefficient,
        m.indexOfRefraction,
        m.opacity,
        m.phongExponent)
{
}

SimpleMaterial::SimpleMaterial(
    const java::String& inName,
    const ColorRgb& inAmbient,
    const ColorRgb& inDiffuse,
    const ColorRgb& inSpecular,
    const ColorRgb& inEmission,
    const ColorRgb& inTransmittance,
    bool inDoubleSided,
    double inReflectionCoefficient,
    double inRefractionCoefficient,
    double inIndexOfRefraction,
    double inOpacity,
    double inPhongExponent)
    : ambient(inAmbient),
      diffuse(inDiffuse),
      specular(inSpecular),
      emission(inEmission),
      transmittance(inTransmittance),
      doubleSided(inDoubleSided),
      reflectionCoefficient(inReflectionCoefficient),
      refractionCoefficient(inRefractionCoefficient),
      indexOfRefraction(inIndexOfRefraction),
      name(inName),
      opacity(inOpacity),
      phongExponent(inPhongExponent)
{
}

SimpleMaterial::SimpleMaterial(
    const java::String& inName,
    const ColorRgb& inAmbient,
    const ColorRgb& inDiffuse,
    const ColorRgb& inSpecular,
    bool inDoubleSided,
    double inReflectionCoefficient,
    double inRefractionCoefficient,
    double inOpacity,
    double inPhongExponent)
    : SimpleMaterial(
          inName,
          inAmbient,
          inDiffuse,
          inSpecular,
          ColorRgb(0.0, 0.0, 0.0),
          ColorRgb(0.0, 0.0, 0.0),
          inDoubleSided,
          inReflectionCoefficient,
          inRefractionCoefficient,
          1.0,
          inOpacity,
          inPhongExponent)
{
}

const java::String& SimpleMaterial::getName() const { return name; }

SimpleMaterial SimpleMaterial::withName(const java::String& n) const { return SimpleMaterial(n, ambient, diffuse, specular, emission, transmittance, doubleSided, reflectionCoefficient, refractionCoefficient, indexOfRefraction, opacity, phongExponent); }
SimpleMaterial SimpleMaterial::withAmbient(const ColorRgb& a) const { return SimpleMaterial(name, a, diffuse, specular, emission, transmittance, doubleSided, reflectionCoefficient, refractionCoefficient, indexOfRefraction, opacity, phongExponent); }
SimpleMaterial SimpleMaterial::withDiffuse(const ColorRgb& d) const { return SimpleMaterial(name, ambient, d, specular, emission, transmittance, doubleSided, reflectionCoefficient, refractionCoefficient, indexOfRefraction, opacity, phongExponent); }
SimpleMaterial SimpleMaterial::withSpecular(const ColorRgb& s) const { return SimpleMaterial(name, ambient, diffuse, s, emission, transmittance, doubleSided, reflectionCoefficient, refractionCoefficient, indexOfRefraction, opacity, phongExponent); }
SimpleMaterial SimpleMaterial::withEmission(const ColorRgb& e) const { return SimpleMaterial(name, ambient, diffuse, specular, e, transmittance, doubleSided, reflectionCoefficient, refractionCoefficient, indexOfRefraction, opacity, phongExponent); }
SimpleMaterial SimpleMaterial::withTransmittance(const ColorRgb& t) const { return SimpleMaterial(name, ambient, diffuse, specular, emission, t, doubleSided, reflectionCoefficient, refractionCoefficient, indexOfRefraction, opacity, phongExponent); }
SimpleMaterial SimpleMaterial::withPhongExponent(double p) const { return SimpleMaterial(name, ambient, diffuse, specular, emission, transmittance, doubleSided, reflectionCoefficient, refractionCoefficient, indexOfRefraction, opacity, p); }
SimpleMaterial SimpleMaterial::withReflectionCoefficient(double kr) const { return SimpleMaterial(name, ambient, diffuse, specular, emission, transmittance, doubleSided, kr, refractionCoefficient, indexOfRefraction, opacity, phongExponent); }
SimpleMaterial SimpleMaterial::withRefractionCoefficient(double kr) const { return SimpleMaterial(name, ambient, diffuse, specular, emission, transmittance, doubleSided, reflectionCoefficient, kr, indexOfRefraction, opacity, phongExponent); }
SimpleMaterial SimpleMaterial::withIndexOfRefraction(double ior) const { return SimpleMaterial(name, ambient, diffuse, specular, emission, transmittance, doubleSided, reflectionCoefficient, refractionCoefficient, ior, opacity, phongExponent); }
SimpleMaterial SimpleMaterial::withOpacity(double a) const { return SimpleMaterial(name, ambient, diffuse, specular, emission, transmittance, doubleSided, reflectionCoefficient, refractionCoefficient, indexOfRefraction, a, phongExponent); }
SimpleMaterial SimpleMaterial::withDoubleSided(bool b) const { return SimpleMaterial(name, ambient, diffuse, specular, emission, transmittance, b, reflectionCoefficient, refractionCoefficient, indexOfRefraction, opacity, phongExponent); }

bool SimpleMaterial::isDoubleSided() const { return doubleSided; }

ColorRgb SimpleMaterial::getAmbient() const { return ColorRgb(ambient); }
const ColorRgb& SimpleMaterial::getAmbientReference() const { return ambient; }
ColorRgb SimpleMaterial::getDiffuse() const { return ColorRgb(diffuse); }
const ColorRgb& SimpleMaterial::getDiffuseReference() const { return diffuse; }
ColorRgb SimpleMaterial::getSpecular() const { return ColorRgb(specular); }
const ColorRgb& SimpleMaterial::getSpecularReference() const { return specular; }
ColorRgb SimpleMaterial::getEmission() const { return ColorRgb(emission); }
const ColorRgb& SimpleMaterial::getEmissionReference() const { return emission; }
ColorRgb SimpleMaterial::getTransmittance() const { return ColorRgb(transmittance); }
const ColorRgb& SimpleMaterial::getTransmittanceReference() const { return transmittance; }

double SimpleMaterial::getPhongExponent() const { return phongExponent; }
double SimpleMaterial::getReflectionCoefficient() const { return reflectionCoefficient; }
double SimpleMaterial::getRefractionCoefficient() const { return refractionCoefficient; }
double SimpleMaterial::getIndexOfRefraction() const { return indexOfRefraction; }
double SimpleMaterial::getOpacity() const { return opacity; }

java::String SimpleMaterial::toString() const
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
