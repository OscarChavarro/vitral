#ifndef __PRMTV_CNT_SHP_DSCRPTR__
#define __PRMTV_CNT_SHP_DSCRPTR__

#include "java/lang/String.h"
#include "vsdk/toolkit/media/ShapeDescriptor.h"
/**
Stores primitive counts as a feature vector. Based on the primitive types
defined in the VSDK utility class.
*/
class PrimitiveCountShapeDescriptor : public ShapeDescriptor {

private:
    double* featureVector;
    enum { numberOfElements = 16 };

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    PrimitiveCountShapeDescriptor(const String* label);
    virtual ~PrimitiveCountShapeDescriptor();

    void setFeature(int primitiveType, long count);

    virtual double* getFeatureVector() const;

    virtual void setFeatureVector(double* vector);

    String* toString() const;
};

#endif
