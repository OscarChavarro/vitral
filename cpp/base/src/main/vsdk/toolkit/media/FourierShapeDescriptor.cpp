#include "FourierShapeDescriptor.h"
#include "../java/String.h"
#include "../common/VSDK.h"
#include <cmath>
#include <cstdio>

FourierShapeDescriptor::FourierShapeDescriptor(const java::String* label) :
    ShapeDescriptor(label) {
    featureVector = new double[numberOfElements * numberOfHarmonics];
    for (int i = 0; i < numberOfElements * numberOfHarmonics; i++) {
        featureVector[i] = 0.0;
    }
}

FourierShapeDescriptor::~FourierShapeDescriptor() {
    if (featureVector != nullptr) {
        delete[] featureVector;
        featureVector = nullptr;
    }
}

void FourierShapeDescriptor::setFeature(int sphere, int harmonic, double r, double i) {
    if (sphere < 0 || sphere >= numberOfElements || harmonic < 0 || harmonic >= numberOfHarmonics) {
        return;
    }
    double harmonicAmplitude = std::sqrt(r * r + i * i);
    featureVector[sphere * numberOfHarmonics + harmonic] = harmonicAmplitude;
}

double* FourierShapeDescriptor::getFeatureVector() const {
    if (featureVector == nullptr) {
        return nullptr;
    }
    double* copy = new double[numberOfElements * numberOfHarmonics];
    for (int i = 0; i < numberOfElements * numberOfHarmonics; i++) {
        copy[i] = featureVector[i];
    }
    return copy;
}

void FourierShapeDescriptor::setFeatureVector(double* vector) {
    if (vector == nullptr) {
        return;
    }
    for (int i = 0; i < numberOfElements * numberOfHarmonics; i++) {
        featureVector[i] = vector[i];
    }
}

java::String* FourierShapeDescriptor::toString() const {
    char buffer[256];
    snprintf(buffer, sizeof(buffer), "SphericalHarmonics amplitudes for %d spheres and %d harmonics:\n",
        numberOfElements, numberOfHarmonics);

    java::String* result = new java::String(buffer);

    for (int i = 0; i < numberOfElements * numberOfHarmonics; i++) {
        char line[256];
        std::string formatted = VSDK::formatDouble(featureVector[i]);
        snprintf(line, sizeof(line), "  - %s\n", formatted.c_str());
        *result = *result + java::String(line);
    }

    return result;
}
