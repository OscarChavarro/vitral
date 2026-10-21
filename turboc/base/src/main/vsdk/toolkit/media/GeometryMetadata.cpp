#include <math.h>
#include <stdio.h>

#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include <limits>
#include "vsdk/toolkit/media/GeometryMetadata.h"
#include "vsdk/toolkit/media/ShapeDescriptor.h"
#include "vsdk/toolkit/common/Cpp98Support.h"
#include <float.h>
long GeometryMetadata::lastId = 0;

GeometryMetadata::GeometryMetadata() : filename(NULL) {
    lastId++;
    id = lastId;
}

GeometryMetadata::~GeometryMetadata() {
    for (long int i = 0; i < descriptors.size(); i++) {
        if (descriptors[i] != NULL) {
            delete descriptors[i];
            descriptors[i] = NULL;
        }
    }
    descriptors.clear();

    if (filename != NULL) {
        delete filename;
        filename = NULL;
    }
}

double GeometryMetadata::doMinskowskiDistance(
    const GeometryMetadata* other,
    double s,
    const String* subGroup) const {
    if (other == NULL || subGroup == NULL) {
        return DBL_MAX;
    }

    ShapeDescriptor* a = NULL;
    ShapeDescriptor* b = NULL;

    for (long int i = 0; i < descriptors.size(); i++) {
        ShapeDescriptor* aa = descriptors.get(i);
        if (aa != NULL) {
            String* label = aa->getLabel();
            if (label != NULL && label->equals(*subGroup)) {
                a = aa;
            }
            delete label;
        }
    }
    if (a == NULL) {
        return DBL_MAX;
    }

    for (long int i = 0; i < other->descriptors.size(); i++) {
        ShapeDescriptor* bb = other->descriptors.get(i);
        if (bb != NULL) {
            String* label = bb->getLabel();
            if (label != NULL && label->equals(*subGroup)) {
                b = bb;
            }
            delete label;
        }
    }
    if (b == NULL) {
        return DBL_MAX;
    }

    double* av = a->getFeatureVector();
    double* bv = b->getFeatureVector();

    if (av == NULL || bv == NULL) {
        if (av != NULL) delete[] av;
        if (bv != NULL) delete[] bv;
        return DBL_MAX;
    }

    int avLength = 0;
    int bvLength = 0;

    for (int i = 0; i < 1024; i++) {
        if (av[i] == 0 && i > 0) {
            avLength = i;
            break;
        }
    }

    for (int i = 0; i < 1024; i++) {
        if (bv[i] == 0 && i > 0) {
            bvLength = i;
            break;
        }
    }

    if (avLength != bvLength) {
        delete[] av;
        delete[] bv;
        return DBL_MAX;
    }

    double acum = 0.0;
    for (int j = 0; j < avLength; j++) {
        acum += pow(Cpp98Support::abs(av[j] - bv[j]), s);
    }

    delete[] av;
    delete[] bv;

    return pow(acum, 1.0 / s);
}

void GeometryMetadata::setId(long id_) {
    id = id_;
    if (lastId < id_) {
        lastId = id_;
    }
}

long GeometryMetadata::getId() const {
    return id;
}

void GeometryMetadata::setFilename(const String* filename) {
    if (filename != NULL && filename->length() > 0) {
        if (this->filename != NULL) {
            delete this->filename;
        }
        this->filename = new String(*filename);
    } else {
        if (this->filename != NULL) {
            delete this->filename;
            this->filename = NULL;
        }
    }
}

String* GeometryMetadata::getFilename() const {
    if (filename != NULL) {
        return new String(*filename);
    }
    return NULL;
}

ArrayList<ShapeDescriptor*>& GeometryMetadata::getDescriptors() {
    return descriptors;
}

const ArrayList<ShapeDescriptor*>& GeometryMetadata::getDescriptors() const {
    return descriptors;
}

ShapeDescriptor* GeometryMetadata::getDescriptorByName(const String* name) const {
    if (name == NULL) {
        return NULL;
    }

    for (long int i = 0; i < descriptors.size(); i++) {
        ShapeDescriptor* s = descriptors.get(i);
        if (s != NULL) {
            String* label = s->getLabel();
            if (label != NULL && label->equals(*name)) {
                delete label;
                return s;
            }
            delete label;
        }
    }
    return NULL;
}

void GeometryMetadata::addDescriptor(ShapeDescriptor* descriptor) {
    if (descriptor != NULL) {
        descriptors.add(descriptor);
    }
}

String* GeometryMetadata::toString() const {
    char fullBuffer[4096];
    int offset = 0;
    const char* filename_str = filename != NULL ? filename->toCString() : "NULL";

    offset += snprintf(fullBuffer + offset, sizeof(fullBuffer) - offset,
        "%s\n    . %ld shape descriptors\n",
        filename_str, (long)descriptors.size());

    for (long int i = 0; i < descriptors.size(); i++) {
        offset += snprintf(fullBuffer + offset, sizeof(fullBuffer) - offset, "        . descriptor_%ld\n", i);
    }

    String* msg = new String(fullBuffer);
    return msg;
}
