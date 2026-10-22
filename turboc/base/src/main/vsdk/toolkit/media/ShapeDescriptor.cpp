#include <stddef.h>
#include "java/lang/String.h"
#include "vsdk/toolkit/media/ShapeDescriptor.h"
ShapeDescriptor::ShapeDescriptor(const String* label_) {
    if (label_ != NULL) {
        label = new String(*label_);
    } else {
        label = NULL;
    }
}

ShapeDescriptor::~ShapeDescriptor() {
    if (label != NULL) {
        delete label;
        label = NULL;
    }
}

String* ShapeDescriptor::getLabel() const {
    if (label != NULL) {
        return new String(*label);
    }
    return NULL;
}

void ShapeDescriptor::setLabel(const String* label_) {
    if (label != NULL) {
        delete label;
    }
    if (label_ != NULL) {
        label = new String(*label_);
    } else {
        label = NULL;
    }
}

const Class ShapeDescriptor::CLASS = { "ShapeDescriptor", &MediaEntity::CLASS };

const Class *
ShapeDescriptor::getClass() const
{
    return &CLASS;
}
