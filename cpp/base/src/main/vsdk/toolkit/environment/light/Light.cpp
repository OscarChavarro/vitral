#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/light/LightType.h"

Light::Light(int type, const Vector3Dd& pos, const ColorRgb& emission)
    : tipo_de_luz(type), lvec(0, 0, 0), ambient(0, 0, 0), diffuse(1, 1, 1), specular(emission), id(0), name("")
{
    if ( type != LightType::AMBIENT ) {
        lvec = Vector3Dd::copyOf(pos);
        if ( type == LightType::DIRECTIONAL ) {
            lvec = lvec.normalized();
        }
    }
}

const std::string& Light::getName() const { return name; }
void Light::setName(const std::string& n) { name = n; }

int Light::getId() const { return id; }
void Light::setId(int i) { id = i; }

void Light::setAmbient(const ColorRgb& a) { ambient = ColorRgb(a); }
void Light::setDiffuse(const ColorRgb& d) { diffuse = ColorRgb(d); }
void Light::setSpecular(const ColorRgb& s) { specular = ColorRgb(s); }

Vector3Dd Light::getPosition() const { return lvec; }
void Light::setPosition(const Vector3Dd& pos) { lvec = Vector3Dd::copyOf(pos); }

ColorRgb Light::getAmbient() const { return ColorRgb(ambient); }
ColorRgb Light::getDiffuse() const { return ColorRgb(diffuse); }
ColorRgb Light::getSpecular() const { return ColorRgb(specular); }
const ColorRgb& Light::getSpecularReference() const { return specular; }

int Light::getLightType() const { return tipo_de_luz; }
