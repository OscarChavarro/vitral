#include <stdio.h>

#include "java/lang/String.h"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/media/PrimitiveCountShapeDescriptor.h"
PrimitiveCountShapeDescriptor::PrimitiveCountShapeDescriptor(const String* label) :
    ShapeDescriptor(label) {
    featureVector = new double[numberOfElements];
    for (int i = 0; i < numberOfElements; i++) {
        featureVector[i] = 0.0;
    }
}

PrimitiveCountShapeDescriptor::~PrimitiveCountShapeDescriptor() {
    if (featureVector != NULL) {
        delete[] featureVector;
        featureVector = NULL;
    }
}

void PrimitiveCountShapeDescriptor::setFeature(int primitiveType, long count) {
    if (primitiveType < 0 || primitiveType >= numberOfElements) {
        return;
    }
    featureVector[primitiveType] = (double)count;
}

double* PrimitiveCountShapeDescriptor::getFeatureVector() const {
    if (featureVector == NULL) {
        return NULL;
    }
    double* copy = new double[numberOfElements];
    for (int i = 0; i < numberOfElements; i++) {
        copy[i] = featureVector[i];
    }
    return copy;
}

void PrimitiveCountShapeDescriptor::setFeatureVector(double* vector) {
    if (vector == NULL) {
        return;
    }
    for (int i = 0; i < numberOfElements; i++) {
        featureVector[i] = vector[i];
    }
}

String* PrimitiveCountShapeDescriptor::toString() const {
    char fullBuffer[4096];
    int offset = 0;
    offset += snprintf(fullBuffer + offset, sizeof(fullBuffer) - offset, "Primitive counts for %d types:\n", numberOfElements);

    for (int i = 0; i < numberOfElements; i++) {
        String formatted = VSDK::formatDouble(featureVector[i]);
        offset += snprintf(fullBuffer + offset, sizeof(fullBuffer) - offset, "  - %s\n", formatted.c_str());
    }

    String* result = new String(fullBuffer);
    return result;
}

// Integral constants bound to references (i.e. by `?:`) need a
// definition out of the class in C++98
const int PrimitiveCountShapeDescriptor::numberOfElements;
