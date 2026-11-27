#include <math.h>
#include <stddef.h>
#include <string.h>
#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Float.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/environment/material/PhongMaterial.h"
#include "vsdk/toolkit/environment/material/ScatteringFrame.h"

static BOOLEAN
isFiniteColor(const ColorRgb &color) {
    return Float::isFinite((double)((color.getR() + color.getG() + color.getB()) / 3.0));
}

static const PhongLobe *
copyLobe(const PhongLobe *lobe) {
    return lobe != NULL ? new PhongLobe(*lobe) : (const PhongLobe *)NULL;
}

PhongMaterial::PhongMaterial(
    const char *inName,
    const PhongLobe *inReflection,
    const PhongLobe *inTransmission,
    const PhongLobe *inEmission,
    const Texture *inTexture,
    const BOOLEAN inSided):
    name(copyName(inName)),
    sided(inSided),
    reflection(inReflection),
    transmission(inTransmission),
    emission(inEmission),
    texture(inTexture),
    storedRefractionIndexReal(1.0),
    storedRefractionIndexImaginary(0.0)
{
}

PhongMaterial::PhongMaterial(
    const char *inName,
    const PhongLobe *inReflection,
    const PhongLobe *inTransmission,
    const PhongLobe *inEmission,
    const Texture *inTexture,
    const BOOLEAN inSided,
    const double inRefractionIndexReal,
    const double inRefractionIndexImaginary):
    name(copyName(inName)),
    sided(inSided),
    reflection(inReflection),
    transmission(inTransmission),
    emission(inEmission),
    texture(inTexture),
    storedRefractionIndexReal(inRefractionIndexReal),
    storedRefractionIndexImaginary(inRefractionIndexImaginary)
{
}

char *
PhongMaterial::copyName(const char *inName) {
    const char *source = inName != NULL ? inName : "";
    char *copy = new char[strlen(source) + 1];
    strcpy(copy, source);
    return copy;
}

PhongMaterial::~PhongMaterial() {
    delete[] name;
    delete reflection;
    delete transmission;
    delete emission;
}

ColorRgb
PhongMaterial::scatteredPower(int components, const Vector3Dd *textureCoordinate) const {
    double r = 0.0;
    double g = 0.0;
    double b = 0.0;

    if ( texture != NULL && (components & TEXTURED_COMPONENT) ) {
        if ( textureCoordinate == NULL ) {
            Logger::reportMessage("PhongMaterial", Logger::WARNING, "scatteredPower", "Couldn't get texture coordinates");
        } else {
            const ColorRgb textureColor = texture->evaluateColor(textureCoordinate->x(), textureCoordinate->y());
            r += textureColor.getR();
            g += textureColor.getG();
            b += textureColor.getB();
        }
        components &= ~TEXTURED_COMPONENT;
    }

    if ( reflection != NULL ) {
        const ColorRgb reflectance = reflection->integrated(ScatteringComponent::reflectionComponents(components));
        if ( !isFiniteColor(reflectance) ) {
            Logger::reportMessage("PhongMaterial", Logger::FATAL_ERROR, "scatteredPower", "Reflectance is not finite");
        }
        r += reflectance.getR();
        g += reflectance.getG();
        b += reflectance.getB();
    }

    if ( transmission != NULL ) {
        const ColorRgb transmittance = transmission->integrated(ScatteringComponent::transmissionComponents(components));
        if ( !isFiniteColor(transmittance) ) {
            Logger::reportMessage("PhongMaterial", Logger::FATAL_ERROR, "scatteredPower", "Transmittance is not finite");
        }
        r += transmittance.getR();
        g += transmittance.getG();
        b += transmittance.getB();
    }

    return ColorRgb(r, g, b);
}

ColorRgb
PhongMaterial::emittance(const int components) const {
    if ( emission == NULL ) {
        return ColorRgb(0.0, 0.0, 0.0);
    }
    return emission->integrated(components);
}

Material *
PhongMaterial::copy() const {
    return new PhongMaterial(name, copyLobe(reflection), copyLobe(transmission), copyLobe(emission), texture, sided,
        storedRefractionIndexReal, storedRefractionIndexImaginary);
}

Material *
PhongMaterial::translate(Vector3Dd * /*vector*/) {
    return this;
}

Material *
PhongMaterial::rotate(Vector3Dd * /*vector*/) {
    return this;
}

Material *
PhongMaterial::scale(Vector3Dd * /*vector*/) {
    return this;
}

const Class PhongMaterial::CLASS = { "PhongMaterial", NULL };

const Class *
PhongMaterial::getClass() const
{
    return &CLASS;
}

// C++ port note: file statics instead of an unnamed namespace

static double
averageOf(const ColorRgb &color) {
    return (color.getR() + color.getG() + color.getB()) / 3.0;
}

// Scattering below which nothing is sampled (RenderPark's EPSILON)
static const double NO_SCATTERING = 1e-6;

/** Refraction index of a medium in evaluation and sampling: 0 when unknown */
static double
sampledIndex(const PhongMaterial *medium) {
    return medium != NULL ? medium->getRefractionIndex() : 0.0;
}

/** Refraction index of a medium in densityOf: vacuum when unknown */
static double
densityIndex(const PhongMaterial *medium) {
    return medium != NULL ? medium->getRefractionIndex() : 1.0;
}

/** The part of the scattering a first variate chose */
enum SampledPart { TEXTURE_PART, REFLECTION_PART, TRANSMISSION_PART, ABSORPTION };

/** Direction sampled within the chosen part, and its density there (0 when none) */
static Vector3Dd
sampleWithinPart(
    const SampledPart part,
    const PhongLobe *reflection,
    const PhongLobe *transmission,
    const Vector3Dd &normal,
    const Vector3Dd &in,
    const double inIndex,
    const double outIndex,
    const int reflectionComponents,
    const int transmissionComponents,
    const double x1,
    const double x2,
    double *density)
{
    switch ( part ) {
        case TEXTURE_PART:
            return ScatteringFrame(normal).sampleCosine(x1, x2, density);
        case REFLECTION_PART:
            if ( reflection != NULL ) {
                return reflection->reflectionSample(in, normal, false, reflectionComponents, x1, x2, density);
            }
            break;
        case TRANSMISSION_PART:
            if ( transmission != NULL ) {
                return transmission->transmissionSample(
                    inIndex, outIndex, in, normal, false, transmissionComponents, x1, x2, density);
            }
            break;
        case ABSORPTION:
            break;
    }
    *density = 0.0;
    return Vector3Dd(0.0, 0.0, 0.0);
}

/** Density of the texture's diffuse scattering: cos / pi */
static double
textureDensity(const Vector3Dd &out, const Vector3Dd &normal) {
    return scatteringArithmetic::dot(normal, out) / M_PI;
}

/** Texture color at the point; black without texture or texture coordinate (reported) */
ColorRgb
PhongMaterial::textureColor(const SurfaceShadingPoint &point) const {
    if ( texture == NULL ) {
        return ColorRgb(0.0, 0.0, 0.0);
    }
    if ( !point.hasTextureCoordinate() ) {
        Logger::reportMessage("PhongMaterial", Logger::WARNING, "textureColor", "Couldn't get texture coordinates");
        return ColorRgb(0.0, 0.0, 0.0);
    }
    return texture->evaluateColor(point.getTextureCoordinate().x(), point.getTextureCoordinate().y());
}

/**
Average albedos of the texture, of the reflection without the textured
component, and of the transmission, and the components left to each lobe
*/
void
PhongMaterial::samplingProbabilities(
    const SurfaceShadingPoint &point,
    int components,
    double *texturePart,
    double *reflectionPart,
    double *transmissionPart,
    int *reflectionComponents,
    int *transmissionComponents) const
{
    *texturePart = 0.0;
    if ( texture != NULL && (components & TEXTURED_COMPONENT) ) {
        *texturePart = averageOf(textureColor(point));
        components &= ~TEXTURED_COMPONENT;
    }
    *reflectionComponents = ScatteringComponent::reflectionComponents(components);
    *transmissionComponents = ScatteringComponent::transmissionComponents(components);
    // C++ port note: if / else instead of conditional expressions, as
    // Turbo C++ 3.0 rejects ColorRgb temporaries (with a destructor) in them
    *reflectionPart = 0.0;
    if ( reflection != NULL ) {
        *reflectionPart = averageOf(reflection->integrated(*reflectionComponents));
    }
    *transmissionPart = 0.0;
    if ( transmission != NULL ) {
        *transmissionPart = averageOf(transmission->integrated(*transmissionComponents));
    }
}

ColorRgb
PhongMaterial::scatteringEvaluate(
    const SurfaceShadingPoint &point,
    const PhongMaterial *inMedium,
    const PhongMaterial *outMedium,
    const Vector3Dd &in,
    const Vector3Dd &out,
    int components) const
{
    double r = 0.0;
    double g = 0.0;
    double b = 0.0;
    if ( !point.hasShadingNormal() ) {
        Logger::reportMessage("PhongMaterial", Logger::WARNING, "scatteringEvaluate", "Couldn't determine shading normal");
        return ColorRgb(r, g, b);
    }
    const Vector3Dd &normal = point.getShadingNormal();

    if ( texture != NULL && (components & TEXTURED_COMPONENT) ) {
        const double textureValue = 1.0 / M_PI;
        const ColorRgb color = textureColor(point);
        r += textureValue * color.getR();
        g += textureValue * color.getG();
        b += textureValue * color.getB();
        components &= ~TEXTURED_COMPONENT;
    }

    if ( reflection != NULL ) {
        const ColorRgb reflected = reflection->reflectionEvaluate(
            in, out, normal, ScatteringComponent::reflectionComponents(components));
        r += reflected.getR();
        g += reflected.getG();
        b += reflected.getB();
        if ( transmission != NULL ) {
            const ColorRgb transmitted = transmission->transmissionEvaluate(
                sampledIndex(inMedium), sampledIndex(outMedium), in, out, normal,
                ScatteringComponent::transmissionComponents(components));
            r += transmitted.getR();
            g += transmitted.getG();
            b += transmitted.getB();
        } else {
            r += 0.0;
            g += 0.0;
            b += 0.0;
        }
    }
    return ColorRgb(r, g, b);
}

Vector3Dd
PhongMaterial::scatteringSample(
    const SurfaceShadingPoint &point,
    const PhongMaterial *inMedium,
    const PhongMaterial *outMedium,
    const Vector3Dd &in,
    const BOOLEAN russianRoulette,
    const int components,
    double x1,
    const double x2,
    double *density) const
{
    *density = 0;
    if ( !point.hasShadingNormal() ) {
        Logger::reportMessage("PhongMaterial", Logger::WARNING, "scatteringSample", "Couldn't determine shading normal");
        return Vector3Dd(0.0, 0.0, 1.0);
    }
    const Vector3Dd &normal = point.getShadingNormal();

    double texturePart;
    double reflectionPart;
    double transmissionPart;
    int reflectionComponents;
    int transmissionComponents;
    samplingProbabilities(point, components, &texturePart, &reflectionPart, &transmissionPart,
        &reflectionComponents, &transmissionComponents);
    const double scattering = texturePart + reflectionPart + transmissionPart;
    if ( scattering < NO_SCATTERING ) {
        return Vector3Dd(0.0, 0.0, 0.0);
    }

    if ( !russianRoulette ) {
        // No absorption: the parts are normalized
        texturePart /= scattering;
        reflectionPart /= scattering;
        transmissionPart /= scattering;
    }

    // The first variate chooses the part, then is rescaled into [0, 1)
    SampledPart mode = ABSORPTION;
    if ( x1 < texturePart ) {
        mode = TEXTURE_PART;
        x1 /= texturePart;
    } else {
        x1 -= texturePart;
        if ( x1 < reflectionPart ) {
            mode = REFLECTION_PART;
            x1 /= reflectionPart;
        } else {
            x1 -= reflectionPart;
            if ( x1 < transmissionPart ) {
                mode = TRANSMISSION_PART;
                x1 /= transmissionPart;
            }
        }
    }

    const double inIndex = sampledIndex(inMedium);
    const double outIndex = sampledIndex(outMedium);

    double p;
    const Vector3Dd out = sampleWithinPart(
        mode, reflection, transmission, normal, in, inIndex, outIndex,
        reflectionComponents, transmissionComponents, x1, x2, &p);
    if ( mode == ABSORPTION ) {
        *density = 0;
        return out;
    }
    if ( p < NO_SCATTERING ) {
        return out;
    }
    *density = (mode == TEXTURE_PART ? texturePart : mode == REFLECTION_PART ? reflectionPart : transmissionPart) * p;

    // Density of the same direction through the parts not chosen
    if ( mode != TEXTURE_PART ) {
        p = textureDensity(out, normal);
        *density += texturePart * p;
    }
    double survival;
    if ( mode != REFLECTION_PART ) {
        if ( reflection == NULL ) {
            p = 0.0;
        } else {
            reflection->reflectionDensity(in, out, normal, reflectionComponents, &p, &survival);
        }
        *density += reflectionPart * p;
    }
    if ( mode != TRANSMISSION_PART ) {
        if ( transmission == NULL ) {
            p = 0.0;
        } else {
            transmission->transmissionDensity(inIndex, outIndex, in, out, normal, transmissionComponents, &p, &survival);
        }
        *density += transmissionPart * p;
    }
    return out;
}

void
PhongMaterial::scatteringDensity(
    const SurfaceShadingPoint &point,
    const PhongMaterial *inMedium,
    const PhongMaterial *outMedium,
    const Vector3Dd &in,
    const Vector3Dd &out,
    const int components,
    double *density,
    double *survival) const
{
    *density = *survival = 0.0;
    if ( !point.hasShadingNormal() ) {
        Logger::reportMessage("PhongMaterial", Logger::WARNING, "scatteringDensity", "Couldn't determine shading normal");
        return;
    }
    const Vector3Dd &normal = point.getShadingNormal();

    double texturePart;
    double reflectionPart;
    double transmissionPart;
    int reflectionComponents;
    int transmissionComponents;
    samplingProbabilities(point, components, &texturePart, &reflectionPart, &transmissionPart,
        &reflectionComponents, &transmissionComponents);
    const double scattering = texturePart + reflectionPart + transmissionPart;
    if ( scattering < NO_SCATTERING ) {
        return;
    }
    *survival = scattering;

    const double inIndex = densityIndex(inMedium);
    const double outIndex = densityIndex(outMedium);
    double p = textureDensity(out, normal);
    double pSurvival;
    *density = texturePart * p;
    if ( reflection == NULL ) {
        p = 0.0;
    } else {
        reflection->reflectionDensity(in, out, normal, reflectionComponents, &p, &pSurvival);
    }
    *density += reflectionPart * p;
    if ( transmission == NULL ) {
        p = 0.0;
    } else {
        transmission->transmissionDensity(inIndex, outIndex, in, out, normal, transmissionComponents, &p, &pSurvival);
    }
    *density += transmissionPart * p;
    *density /= scattering;
}

ColorRgb
PhongMaterial::scatteringEvaluateComponents(
    const SurfaceShadingPoint &point,
    const PhongMaterial *inMedium,
    const PhongMaterial *outMedium,
    const Vector3Dd &in,
    const Vector3Dd &out,
    const int components,
    ColorRgb *componentValues) const
{
    double r = 0.0;
    double g = 0.0;
    double b = 0.0;
    for ( int i = 0; i < 2 * ScatteringComponent::COMPONENTS_PER_LOBE; i++ ) {
        const int component = 1 << i;
        if ( components & component ) {
            componentValues[i] = scatteringEvaluate(point, inMedium, outMedium, in, out, component);
            r += componentValues[i].getR();
            g += componentValues[i].getG();
            b += componentValues[i].getB();
        } else {
            componentValues[i] = ColorRgb(0.0, 0.0, 0.0);
        }
    }
    return ColorRgb(r, g, b);
}

ColorRgb
PhongMaterial::emissionEvaluate(
    const SurfaceShadingPoint *point,
    const Vector3Dd &out,
    const int components,
    double *density) const
{
    if ( density != NULL ) {
        *density = 0.0;
    }
    if ( point == NULL || !point->hasShadingNormal() ) {
        Logger::reportMessage("PhongMaterial", Logger::WARNING, "emissionEvaluate", "Couldn't determine shading normal");
        return ColorRgb(0.0, 0.0, 0.0);
    }
    const double cosL = scatteringArithmetic::dot(out, point->getShadingNormal());
    if ( cosL < 0.0 || emission == NULL ) {
        return ColorRgb(0.0, 0.0, 0.0);
    }
    if ( components & ScatteringComponent::DIFFUSE ) {
        // kd = Kd / pi, with 1 / pi in single precision as RenderPark stores it
        const double toRadiance = 1.00 / (double)M_PI;
        if ( density != NULL ) {
            *density = cosL / M_PI;
        }
        return ColorRgb(
            0.0 + toRadiance * emission->getDiffuse().getR(),
            0.0 + toRadiance * emission->getDiffuse().getG(),
            0.0 + toRadiance * emission->getDiffuse().getB());
    }
    return ColorRgb(0.0, 0.0, 0.0);
}

Vector3Dd
PhongMaterial::emissionSample(
    const SurfaceShadingPoint *point,
    const int components,
    const double xi1,
    const double xi2,
    ColorRgb *radiance,
    double *density) const
{
    if ( radiance != NULL ) {
        *radiance = ColorRgb(0.0, 0.0, 0.0);
    }
    if ( density != NULL ) {
        *density = 0.0;
    }
    if ( !(components & ScatteringComponent::DIFFUSE) ) {
        return Vector3Dd(0.0, 0.0, 1.0);
    }
    if ( point == NULL || !point->hasShadingNormal() ) {
        Logger::reportMessage("PhongMaterial", Logger::WARNING, "emissionSample", "Couldn't determine shading normal");
        return Vector3Dd(0.0, 0.0, 1.0);
    }
    double sampleDensity;
    const Vector3Dd direction = ScatteringFrame(point->getShadingNormal()).sampleCosine(xi1, xi2, &sampleDensity);
    if ( density != NULL ) {
        *density = sampleDensity;
    }
    if ( radiance != NULL && emission != NULL ) {
        const double toRadiance = 1.0 / (double)M_PI;
        *radiance = ColorRgb(
            toRadiance * emission->getDiffuse().getR(),
            toRadiance * emission->getDiffuse().getG(),
            toRadiance * emission->getDiffuse().getB());
    }
    return direction;
}
