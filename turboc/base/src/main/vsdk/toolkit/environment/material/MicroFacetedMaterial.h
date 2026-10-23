#ifndef __MICRO_FACETED_MATERIAL__
#define __MICRO_FACETED_MATERIAL__

#include "java/lang/String.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
class MicroFacetedMaterial : public SimpleMaterial {
public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    enum {
        FRESNEL_MODEL_SCHLICK = 0,
        FRESNEL_MODEL_CONDUCTOR = 1,
        NDF_MODEL_BECKMANN = 0,
        NDF_MODEL_GGX = 1,
        GEOMETRY_MODEL_SMITH = 0,
        GEOMETRY_MODEL_IMPLICIT = 1
    };

private:
    double roughness;
    double alpha;
    ColorRgb fresnelF0;
    ColorRgb eta;
    ColorRgb kappa;
    double kd;
    double ks;
    int fresnelModel;
    int ndfModel;
    int geometryModel;

public:
    MicroFacetedMaterial();
    MicroFacetedMaterial(const MicroFacetedMaterial& other);
    MicroFacetedMaterial(const String& csvFileName, const String& materialName);

    double getRoughness() const;
    double getAlpha() const;
    ColorRgb getFresnelF0() const;
    ColorRgb getEta() const;
    ColorRgb getKappa() const;
    double getKd() const;
    double getKs() const;
    int getFresnelModel() const;
    int getNdfModel() const;
    int getGeometryModel() const;
};

#endif
