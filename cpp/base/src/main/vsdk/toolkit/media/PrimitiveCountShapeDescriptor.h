#ifndef __VSDK_TOOLKIT_MEDIA_PRIMITIVECOUNTSHAPEDESCRIPTOR_H__
#define __VSDK_TOOLKIT_MEDIA_PRIMITIVECOUNTSHAPEDESCRIPTOR_H__

#include "java/lang/String.h"
#include "vsdk/toolkit/media/ShapeDescriptor.h"
/**
Stores primitive counts as a feature vector. Based on the primitive types
defined in the VSDK utility class.
*/
class PrimitiveCountShapeDescriptor : public ShapeDescriptor {

private:
    double* featureVector;
    static const int numberOfElements = 16;

public:
    PrimitiveCountShapeDescriptor(const java::String* label);
    virtual ~PrimitiveCountShapeDescriptor();

    void setFeature(int primitiveType, long count);

    virtual double* getFeatureVector() const override;

    virtual void setFeatureVector(double* vector) override;

    java::String* toString() const;
};

#endif // __VSDK_TOOLKIT_MEDIA_PRIMITIVECOUNTSHAPEDESCRIPTOR_H__
