#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>

#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/material/MicroFacetedMaterial.h"
struct MicrofacetConfig {
    String name;
    ColorRgb ambient;
    ColorRgb diffuse;
    ColorRgb specular;
    bool doubleSided;
    double reflectionCoefficient;
    double refractionCoefficient;
    double opacity;
    double phongExponent;
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
};

static String trim(const String& s)
{
    size_t a = 0;
    while ( a < (size_t)s.size() && isspace((unsigned char)s[a]) ) a++;
    size_t b = s.size();
    while ( b > a && isspace((unsigned char)s[b - 1]) ) b--;
    return s.substr(a, b - a);
}

static String lower(const String& s)
{
    String out = s;
    for ( size_t i = 0; i < (size_t)out.size(); i++ ) out[i] = (char)tolower((unsigned char)out[i]);
    return out;
}

static double clamp01(double v)
{
    if ( v < 0.0 ) return 0.0;
    if ( v > 1.0 ) return 1.0;
    return v;
}

static double parseDoubleOr(const String& s, double fallback)
{
    const char* str = trim(s).toCString();
    char* endptr = NULL;
    double val = strtod(str, &endptr);
    if (endptr == str) return fallback;
    return val;
}

static int parseIntOr(const String& s, int fallback)
{
    const char* str = trim(s).toCString();
    char* endptr = NULL;
    long val = strtol(str, &endptr, 10);
    if (endptr == str) return fallback;
    return (int)val;
}

static void splitCsv(const String& line, ArrayList<String>& cols)
{
    int prev = 0;
    for (int i = 0; i <= (int)line.size(); i++) {
        if (i == (int)line.size() || line[i] == ',') {
            cols.add(line.substr(prev, i - prev));
            prev = i + 1;
        }
    }
}

static int columnIndex(const ArrayList<String>& header, const String& n)
{
    const String needle = lower(n);
    for ( long int i = 0; i < header.size(); i++ ) {
        if ( lower(trim(header[i])) == needle ) return (int)i;
    }
    return -1;
}

static MicrofacetConfig defaultConfig()
{
    MicrofacetConfig c;
    c.name = "Copper";
    c.ambient = ColorRgb(0.1, 0.1, 0.1);
    c.diffuse = ColorRgb(0.955008, 0.637427, 0.538163);
    c.specular = ColorRgb(0.955008, 0.637427, 0.538163);
    c.doubleSided = true;
    c.reflectionCoefficient = 0.0;
    c.refractionCoefficient = 0.0;
    c.opacity = 1.0;
    c.phongExponent = 40.0;
    c.roughness = 0.35;
    c.alpha = c.roughness * c.roughness;
    c.fresnelF0 = c.specular;
    c.eta = ColorRgb(0.2, 0.9, 1.1);
    c.kappa = ColorRgb(3.9, 2.5, 2.4);
    c.kd = 1.0;
    c.ks = 1.0;
    c.fresnelModel = MicroFacetedMaterial::FRESNEL_MODEL_SCHLICK;
    c.ndfModel = MicroFacetedMaterial::NDF_MODEL_BECKMANN;
    c.geometryModel = MicroFacetedMaterial::GEOMETRY_MODEL_SMITH;
    return c;
}

static MicrofacetConfig loadFromCsv(const String& csvFileName, const String& materialName)
{
    MicrofacetConfig d = defaultConfig();
    if ( csvFileName.empty() || materialName.empty() ) return d;

    FILE* in = fopen(csvFileName.c_str(), "r");
    if ( !in ) return d;

    char headerLineStr[4096];
    if ( !fgets(headerLineStr, sizeof(headerLineStr), in) ) { fclose(in); return d; }
    String headerLine(headerLineStr);
    ArrayList<String> header;
    splitCsv(headerLine, header);


    const int iName = columnIndex(header, "material_name");
    const int iRoughness = columnIndex(header, "roughness");
    const int iAlpha = columnIndex(header, "alpha");
    const int iKd = columnIndex(header, "kd");
    const int iKs = columnIndex(header, "ks");
    const int iDr = columnIndex(header, "diffuse_r");
    const int iDg = columnIndex(header, "diffuse_g");
    const int iDb = columnIndex(header, "diffuse_b");
    const int iF0r = columnIndex(header, "f0_r");
    const int iF0g = columnIndex(header, "f0_g");
    const int iF0b = columnIndex(header, "f0_b");
    const int iEtaR = columnIndex(header, "eta_r");
    const int iEtaG = columnIndex(header, "eta_g");
    const int iEtaB = columnIndex(header, "eta_b");
    const int iKappaR = columnIndex(header, "kappa_r");
    const int iKappaG = columnIndex(header, "kappa_g");
    const int iKappaB = columnIndex(header, "kappa_b");
    const int iFModel = columnIndex(header, "fresnel_model");
    const int iNdf = columnIndex(header, "ndf_model");
    const int iGeo = columnIndex(header, "geometry_model");

    const String target = lower(trim(materialName));
    char lineStr[4096];
    while ( fgets(lineStr, sizeof(lineStr), in) ) {
        String line(lineStr);
        if ( trim(line).empty() ) continue;
        ArrayList<String> cols;
        splitCsv(line, cols);
        if ( iName < 0 || iName >= (int)cols.size() ) continue;
        const String rowName = trim(cols[(long int)iName]);
        if ( lower(rowName) != target ) continue;

        d.name = rowName;
        if ( iRoughness >= 0 && iRoughness < (int)cols.size() ) d.roughness = clamp01(parseDoubleOr(cols[(long int)iRoughness], d.roughness));
        if ( iAlpha >= 0 && iAlpha < (int)cols.size() ) d.alpha = clamp01(parseDoubleOr(cols[(long int)iAlpha], d.roughness * d.roughness));
        if ( iKd >= 0 && iKd < (int)cols.size() ) d.kd = clamp01(parseDoubleOr(cols[(long int)iKd], d.kd));
        if ( iKs >= 0 && iKs < (int)cols.size() ) d.ks = clamp01(parseDoubleOr(cols[(long int)iKs], d.ks));
        d.diffuse = ColorRgb(
            (iDr >= 0 && iDr < (int)cols.size()) ? parseDoubleOr(cols[(long int)iDr], d.diffuse.r()) : d.diffuse.r(),
            (iDg >= 0 && iDg < (int)cols.size()) ? parseDoubleOr(cols[(long int)iDg], d.diffuse.g()) : d.diffuse.g(),
            (iDb >= 0 && iDb < (int)cols.size()) ? parseDoubleOr(cols[(long int)iDb], d.diffuse.b()) : d.diffuse.b());
        d.fresnelF0 = ColorRgb(
            (iF0r >= 0 && iF0r < (int)cols.size()) ? parseDoubleOr(cols[(long int)iF0r], d.fresnelF0.r()) : d.fresnelF0.r(),
            (iF0g >= 0 && iF0g < (int)cols.size()) ? parseDoubleOr(cols[(long int)iF0g], d.fresnelF0.g()) : d.fresnelF0.g(),
            (iF0b >= 0 && iF0b < (int)cols.size()) ? parseDoubleOr(cols[(long int)iF0b], d.fresnelF0.b()) : d.fresnelF0.b());
        d.specular = d.fresnelF0;
        d.eta = ColorRgb(
            (iEtaR >= 0 && iEtaR < (int)cols.size()) ? parseDoubleOr(cols[(long int)iEtaR], d.eta.r()) : d.eta.r(),
            (iEtaG >= 0 && iEtaG < (int)cols.size()) ? parseDoubleOr(cols[(long int)iEtaG], d.eta.g()) : d.eta.g(),
            (iEtaB >= 0 && iEtaB < (int)cols.size()) ? parseDoubleOr(cols[(long int)iEtaB], d.eta.b()) : d.eta.b());
        d.kappa = ColorRgb(
            (iKappaR >= 0 && iKappaR < (int)cols.size()) ? parseDoubleOr(cols[(long int)iKappaR], d.kappa.r()) : d.kappa.r(),
            (iKappaG >= 0 && iKappaG < (int)cols.size()) ? parseDoubleOr(cols[(long int)iKappaG], d.kappa.g()) : d.kappa.g(),
            (iKappaB >= 0 && iKappaB < (int)cols.size()) ? parseDoubleOr(cols[(long int)iKappaB], d.kappa.b()) : d.kappa.b());
        if ( iFModel >= 0 && iFModel < (int)cols.size() ) d.fresnelModel = parseIntOr(cols[(long int)iFModel], d.fresnelModel);
        if ( iNdf >= 0 && iNdf < (int)cols.size() ) d.ndfModel = parseIntOr(cols[(long int)iNdf], d.ndfModel);
        if ( iGeo >= 0 && iGeo < (int)cols.size() ) d.geometryModel = parseIntOr(cols[(long int)iGeo], d.geometryModel);
        fclose(in);
        return d;
    }
    fclose(in);
    return d;
}
MicroFacetedMaterial::MicroFacetedMaterial()
    : SimpleMaterial(
        defaultConfig().name,
        defaultConfig().ambient,
        defaultConfig().diffuse,
        defaultConfig().specular,
        defaultConfig().doubleSided,
        defaultConfig().reflectionCoefficient,
        defaultConfig().refractionCoefficient,
        defaultConfig().opacity,
        defaultConfig().phongExponent),
      roughness(defaultConfig().roughness),
      alpha(defaultConfig().alpha),
      fresnelF0(defaultConfig().fresnelF0),
      eta(defaultConfig().eta),
      kappa(defaultConfig().kappa),
      kd(defaultConfig().kd),
      ks(defaultConfig().ks),
      fresnelModel(defaultConfig().fresnelModel),
      ndfModel(defaultConfig().ndfModel),
      geometryModel(defaultConfig().geometryModel)
{
}

MicroFacetedMaterial::MicroFacetedMaterial(const MicroFacetedMaterial& other)
    : SimpleMaterial(other),
      roughness(other.roughness),
      alpha(other.alpha),
      fresnelF0(other.fresnelF0),
      eta(other.eta),
      kappa(other.kappa),
      kd(other.kd),
      ks(other.ks),
      fresnelModel(other.fresnelModel),
      ndfModel(other.ndfModel),
      geometryModel(other.geometryModel)
{
}

MicroFacetedMaterial::MicroFacetedMaterial(const String& csvFileName, const String& materialName)
    : SimpleMaterial(),
      roughness(0.35),
      alpha(0.35 * 0.35),
      fresnelF0(1, 1, 1),
      eta(0.2, 0.9, 1.1),
      kappa(3.9, 2.5, 2.4),
      kd(1.0),
      ks(1.0),
      fresnelModel(FRESNEL_MODEL_SCHLICK),
      ndfModel(NDF_MODEL_BECKMANN),
      geometryModel(GEOMETRY_MODEL_SMITH)
{
    MicrofacetConfig c = loadFromCsv(csvFileName, materialName);
    *this = MicroFacetedMaterial();
    SimpleMaterial tmp(
        c.name, c.ambient, c.diffuse, c.specular, c.doubleSided,
        c.reflectionCoefficient, c.refractionCoefficient, c.opacity, c.phongExponent);
    *((SimpleMaterial*)this) = tmp;
    roughness = c.roughness;
    alpha = c.alpha;
    fresnelF0 = c.fresnelF0;
    eta = c.eta;
    kappa = c.kappa;
    kd = c.kd;
    ks = c.ks;
    fresnelModel = c.fresnelModel;
    ndfModel = c.ndfModel;
    geometryModel = c.geometryModel;
}

double MicroFacetedMaterial::getRoughness() const { return roughness; }
double MicroFacetedMaterial::getAlpha() const { return alpha; }
ColorRgb MicroFacetedMaterial::getFresnelF0() const { return ColorRgb(fresnelF0); }
ColorRgb MicroFacetedMaterial::getEta() const { return ColorRgb(eta); }
ColorRgb MicroFacetedMaterial::getKappa() const { return ColorRgb(kappa); }
double MicroFacetedMaterial::getKd() const { return kd; }
double MicroFacetedMaterial::getKs() const { return ks; }
int MicroFacetedMaterial::getFresnelModel() const { return fresnelModel; }
int MicroFacetedMaterial::getNdfModel() const { return ndfModel; }
int MicroFacetedMaterial::getGeometryModel() const { return geometryModel; }

// Integral constants bound to references (i.e. by `?:`) need a
// definition out of the class in C++98
const int MicroFacetedMaterial::FRESNEL_MODEL_CONDUCTOR;
const int MicroFacetedMaterial::NDF_MODEL_GGX;
const int MicroFacetedMaterial::GEOMETRY_MODEL_IMPLICIT;

// Integral constants bound to references (i.e. by `?:`) need a
// definition out of the class in C++98
const int MicroFacetedMaterial::FRESNEL_MODEL_SCHLICK;
const int MicroFacetedMaterial::NDF_MODEL_BECKMANN;
const int MicroFacetedMaterial::GEOMETRY_MODEL_SMITH;
