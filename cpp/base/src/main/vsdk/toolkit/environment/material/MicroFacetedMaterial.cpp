#include "vsdk/toolkit/environment/material/MicroFacetedMaterial.h"

#include <algorithm>
#include <cctype>
#include <fstream>
#include <sstream>
#include <vector>

namespace {
struct MicrofacetConfig {
    std::string name;
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

static std::string trim(const std::string& s)
{
    size_t a = 0;
    while ( a < s.size() && std::isspace((unsigned char)s[a]) ) a++;
    size_t b = s.size();
    while ( b > a && std::isspace((unsigned char)s[b - 1]) ) b--;
    return s.substr(a, b - a);
}

static std::string lower(const std::string& s)
{
    std::string out = s;
    for ( size_t i = 0; i < out.size(); i++ ) out[i] = (char)std::tolower((unsigned char)out[i]);
    return out;
}

static double clamp01(double v)
{
    if ( v < 0.0 ) return 0.0;
    if ( v > 1.0 ) return 1.0;
    return v;
}

static double parseDoubleOr(const std::string& s, double fallback)
{
    try { return std::stod(trim(s)); } catch (...) { return fallback; }
}

static int parseIntOr(const std::string& s, int fallback)
{
    try { return std::stoi(trim(s)); } catch (...) { return fallback; }
}

static std::vector<std::string> splitCsv(const std::string& line)
{
    std::vector<std::string> cols;
    std::stringstream ss(line);
    std::string token;
    while ( std::getline(ss, token, ',') ) cols.push_back(token);
    return cols;
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

static MicrofacetConfig loadFromCsv(const std::string& csvFileName, const std::string& materialName)
{
    MicrofacetConfig d = defaultConfig();
    if ( csvFileName.empty() || materialName.empty() ) return d;

    std::ifstream in(csvFileName.c_str());
    if ( !in.good() ) return d;

    std::string headerLine;
    if ( !std::getline(in, headerLine) ) return d;
    std::vector<std::string> header = splitCsv(headerLine);

    auto idx = [&](const std::string& n) -> int {
        const std::string needle = lower(n);
        for ( size_t i = 0; i < header.size(); i++ ) {
            if ( lower(trim(header[i])) == needle ) return (int)i;
        }
        return -1;
    };

    const int iName = idx("material_name");
    const int iRoughness = idx("roughness");
    const int iAlpha = idx("alpha");
    const int iKd = idx("kd");
    const int iKs = idx("ks");
    const int iDr = idx("diffuse_r");
    const int iDg = idx("diffuse_g");
    const int iDb = idx("diffuse_b");
    const int iF0r = idx("f0_r");
    const int iF0g = idx("f0_g");
    const int iF0b = idx("f0_b");
    const int iEtaR = idx("eta_r");
    const int iEtaG = idx("eta_g");
    const int iEtaB = idx("eta_b");
    const int iKappaR = idx("kappa_r");
    const int iKappaG = idx("kappa_g");
    const int iKappaB = idx("kappa_b");
    const int iFModel = idx("fresnel_model");
    const int iNdf = idx("ndf_model");
    const int iGeo = idx("geometry_model");

    const std::string target = lower(trim(materialName));
    std::string line;
    while ( std::getline(in, line) ) {
        if ( trim(line).empty() ) continue;
        std::vector<std::string> cols = splitCsv(line);
        if ( iName < 0 || iName >= (int)cols.size() ) continue;
        const std::string rowName = trim(cols[(size_t)iName]);
        if ( lower(rowName) != target ) continue;

        d.name = rowName;
        if ( iRoughness >= 0 && iRoughness < (int)cols.size() ) d.roughness = clamp01(parseDoubleOr(cols[(size_t)iRoughness], d.roughness));
        if ( iAlpha >= 0 && iAlpha < (int)cols.size() ) d.alpha = clamp01(parseDoubleOr(cols[(size_t)iAlpha], d.roughness * d.roughness));
        if ( iKd >= 0 && iKd < (int)cols.size() ) d.kd = clamp01(parseDoubleOr(cols[(size_t)iKd], d.kd));
        if ( iKs >= 0 && iKs < (int)cols.size() ) d.ks = clamp01(parseDoubleOr(cols[(size_t)iKs], d.ks));
        d.diffuse = ColorRgb(
            (iDr >= 0 && iDr < (int)cols.size()) ? parseDoubleOr(cols[(size_t)iDr], d.diffuse.r()) : d.diffuse.r(),
            (iDg >= 0 && iDg < (int)cols.size()) ? parseDoubleOr(cols[(size_t)iDg], d.diffuse.g()) : d.diffuse.g(),
            (iDb >= 0 && iDb < (int)cols.size()) ? parseDoubleOr(cols[(size_t)iDb], d.diffuse.b()) : d.diffuse.b());
        d.fresnelF0 = ColorRgb(
            (iF0r >= 0 && iF0r < (int)cols.size()) ? parseDoubleOr(cols[(size_t)iF0r], d.fresnelF0.r()) : d.fresnelF0.r(),
            (iF0g >= 0 && iF0g < (int)cols.size()) ? parseDoubleOr(cols[(size_t)iF0g], d.fresnelF0.g()) : d.fresnelF0.g(),
            (iF0b >= 0 && iF0b < (int)cols.size()) ? parseDoubleOr(cols[(size_t)iF0b], d.fresnelF0.b()) : d.fresnelF0.b());
        d.specular = d.fresnelF0;
        d.eta = ColorRgb(
            (iEtaR >= 0 && iEtaR < (int)cols.size()) ? parseDoubleOr(cols[(size_t)iEtaR], d.eta.r()) : d.eta.r(),
            (iEtaG >= 0 && iEtaG < (int)cols.size()) ? parseDoubleOr(cols[(size_t)iEtaG], d.eta.g()) : d.eta.g(),
            (iEtaB >= 0 && iEtaB < (int)cols.size()) ? parseDoubleOr(cols[(size_t)iEtaB], d.eta.b()) : d.eta.b());
        d.kappa = ColorRgb(
            (iKappaR >= 0 && iKappaR < (int)cols.size()) ? parseDoubleOr(cols[(size_t)iKappaR], d.kappa.r()) : d.kappa.r(),
            (iKappaG >= 0 && iKappaG < (int)cols.size()) ? parseDoubleOr(cols[(size_t)iKappaG], d.kappa.g()) : d.kappa.g(),
            (iKappaB >= 0 && iKappaB < (int)cols.size()) ? parseDoubleOr(cols[(size_t)iKappaB], d.kappa.b()) : d.kappa.b());
        if ( iFModel >= 0 && iFModel < (int)cols.size() ) d.fresnelModel = parseIntOr(cols[(size_t)iFModel], d.fresnelModel);
        if ( iNdf >= 0 && iNdf < (int)cols.size() ) d.ndfModel = parseIntOr(cols[(size_t)iNdf], d.ndfModel);
        if ( iGeo >= 0 && iGeo < (int)cols.size() ) d.geometryModel = parseIntOr(cols[(size_t)iGeo], d.geometryModel);
        return d;
    }
    return d;
}
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

MicroFacetedMaterial::MicroFacetedMaterial(const std::string& csvFileName, const std::string& materialName)
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
