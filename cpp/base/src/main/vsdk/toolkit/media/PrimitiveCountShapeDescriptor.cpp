#include "PrimitiveCountShapeDescriptor.h"
#include "../java/String.h"
#include "../common/VSDK.h"
#include <cstdio>

PrimitiveCountShapeDescriptor::PrimitiveCountShapeDescriptor(const java::String* label) :
    ShapeDescriptor(label) {
    featureVector = new double[numberOfElements];
    for (int i = 0; i < numberOfElements; i++) {
        featureVector[i] = 0.0;
    }
}

PrimitiveCountShapeDescriptor::~PrimitiveCountShapeDescriptor() {
    if (featureVector != nullptr) {
        delete[] featureVector;
        featureVector = nullptr;
    }
}

void PrimitiveCountShapeDescriptor::setFeature(int primitiveType, long count) {
    if (primitiveType < 0 || primitiveType >= numberOfElements) {
        return;
    }
    featureVector[primitiveType] = (double)count;
}

double* PrimitiveCountShapeDescriptor::getFeatureVector() const {
    if (featureVector == nullptr) {
        return nullptr;
    }
    double* copy = new double[numberOfElements];
    for (int i = 0; i < numberOfElements; i++) {
        copy[i] = featureVector[i];
    }
    return copy;
}

void PrimitiveCountShapeDescriptor::setFeatureVector(double* vector) {
    if (vector == nullptr) {
        return;
    }
    for (int i = 0; i < numberOfElements; i++) {
        featureVector[i] = vector[i];
    }
}

java::String* PrimitiveCountShapeDescriptor::toString() const {
    char buffer[2048];
    snprintf(buffer, sizeof(buffer), "Primitive counts for %d types:\n", numberOfElements);

    java::String* result = new java::String(buffer);

    for (int i = 0; i < numberOfElements; i++) {
        char line[256];
        std::string formatted = VSDK::formatDouble(featureVector[i]);
        snprintf(line, sizeof(line), "  - %s\n", formatted.c_str());
        *result = *result + java::String(line);
    }

    return result;
}
