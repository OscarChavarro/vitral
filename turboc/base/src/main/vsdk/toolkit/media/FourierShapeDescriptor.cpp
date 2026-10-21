#include <math.h>
#include <stdio.h>

#include "java/lang/String.h"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/media/FourierShapeDescriptor.h"
FourierShapeDescriptor::FourierShapeDescriptor(const String* label) :
    ShapeDescriptor(label) {
    featureVector = new double[numberOfElements * numberOfHarmonics];
    for (int i = 0; i < numberOfElements * numberOfHarmonics; i++) {
        featureVector[i] = 0.0;
    }
}

FourierShapeDescriptor::~FourierShapeDescriptor() {
    if (featureVector != NULL) {
        delete[] featureVector;
        featureVector = NULL;
    }
}

void FourierShapeDescriptor::setFeature(int sphere, int harmonic, double r, double i) {
    if (sphere < 0 || sphere >= numberOfElements || harmonic < 0 || harmonic >= numberOfHarmonics) {
        return;
    }
    double harmonicAmplitude = sqrt(r * r + i * i);
    featureVector[sphere * numberOfHarmonics + harmonic] = harmonicAmplitude;
}

double* FourierShapeDescriptor::getFeatureVector() const {
    if (featureVector == NULL) {
        return NULL;
    }
    double* copy = new double[numberOfElements * numberOfHarmonics];
    for (int i = 0; i < numberOfElements * numberOfHarmonics; i++) {
        copy[i] = featureVector[i];
    }
    return copy;
}

void FourierShapeDescriptor::setFeatureVector(double* vector) {
    if (vector == NULL) {
        return;
    }
    for (int i = 0; i < numberOfElements * numberOfHarmonics; i++) {
        featureVector[i] = vector[i];
    }
}

String* FourierShapeDescriptor::toString() const {
    char fullBuffer[8192];
    int offset = 0;
    offset += snprintf(fullBuffer + offset, sizeof(fullBuffer) - offset, "SphericalHarmonics amplitudes for %d spheres and %d harmonics:\n",
        numberOfElements, numberOfHarmonics);

    for (int i = 0; i < numberOfElements * numberOfHarmonics; i++) {
        String formatted = VSDK::formatDouble(featureVector[i]);
        offset += snprintf(fullBuffer + offset, sizeof(fullBuffer) - offset, "  - %s\n", formatted.c_str());
    }

    String* result = new String(fullBuffer);
    return result;
}

// Integral constants bound to references (i.e. by `?:`) need a
// definition out of the class in C++98
const int FourierShapeDescriptor::numberOfElements;
const int FourierShapeDescriptor::numberOfHarmonics;
