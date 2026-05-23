#ifndef __VSDK_TOOLKIT_MEDIA_SHAPEDESCRIPTOR_H__
#define __VSDK_TOOLKIT_MEDIA_SHAPEDESCRIPTOR_H__

#include "MediaEntity.h"
#include "java/lang/String.h"

class ShapeDescriptor : public MediaEntity {

protected:
    java::String* label;

public:
    ShapeDescriptor(const java::String* label_);
    virtual ~ShapeDescriptor();

    java::String* getLabel() const;
    void setLabel(const java::String* label_);

    virtual double* getFeatureVector() const {
        return nullptr;
    }

    virtual void setFeatureVector(double* vector) {
    }
};

#endif // __VSDK_TOOLKIT_MEDIA_SHAPEDESCRIPTOR_H__
