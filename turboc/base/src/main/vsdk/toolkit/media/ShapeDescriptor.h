#ifndef __SHAPE_DESCRIPTOR__
#define __SHAPE_DESCRIPTOR__

#include <stddef.h>
#include "java/lang/String.h"
#include "vsdk/toolkit/media/MediaEntity.h"
class ShapeDescriptor : public MediaEntity {

protected:
    String* label;

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    ShapeDescriptor(const String* label_);
    virtual ~ShapeDescriptor();

    String* getLabel() const;
    void setLabel(const String* label_);

    virtual double* getFeatureVector() const {
        return NULL;
    }

    virtual void setFeatureVector(double* vector) {
    }
};

#endif
