#include "GeometryMetadata.h"
#include "ShapeDescriptor.h"
#include "vsdk/toolkit/java/lang/String.h"
#include <cmath>
#include <cstdio>
#include <limits>

long GeometryMetadata::lastId = 0;

GeometryMetadata::GeometryMetadata() : objectFilename(nullptr) {
    lastId++;
    id = lastId;
}

GeometryMetadata::~GeometryMetadata() {
    for (size_t i = 0; i < descriptorsList.size(); i++) {
        if (descriptorsList[i] != nullptr) {
            delete descriptorsList[i];
            descriptorsList[i] = nullptr;
        }
    }
    descriptorsList.clear();

    if (objectFilename != nullptr) {
        delete objectFilename;
        objectFilename = nullptr;
    }
}

double GeometryMetadata::doMinskowskiDistance(
    const GeometryMetadata* other,
    double s,
    const java::String* subGroup) const {
    if (other == nullptr || subGroup == nullptr) {
        return std::numeric_limits<double>::max();
    }

    ShapeDescriptor* a = nullptr;
    ShapeDescriptor* b = nullptr;

    for (size_t i = 0; i < descriptorsList.size(); i++) {
        ShapeDescriptor* aa = descriptorsList[i];
        if (aa != nullptr) {
            java::String* label = aa->getLabel();
            if (label != nullptr && label->equals(*subGroup)) {
                a = aa;
            }
            delete label;
        }
    }
    if (a == nullptr) {
        return std::numeric_limits<double>::max();
    }

    for (size_t i = 0; i < other->descriptorsList.size(); i++) {
        ShapeDescriptor* bb = other->descriptorsList[i];
        if (bb != nullptr) {
            java::String* label = bb->getLabel();
            if (label != nullptr && label->equals(*subGroup)) {
                b = bb;
            }
            delete label;
        }
    }
    if (b == nullptr) {
        return std::numeric_limits<double>::max();
    }

    double* av = a->getFeatureVector();
    double* bv = b->getFeatureVector();

    if (av == nullptr || bv == nullptr) {
        if (av != nullptr) delete[] av;
        if (bv != nullptr) delete[] bv;
        return std::numeric_limits<double>::max();
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
        return std::numeric_limits<double>::max();
    }

    double acum = 0.0;
    for (int j = 0; j < avLength; j++) {
        acum += std::pow(std::abs(av[j] - bv[j]), s);
    }

    delete[] av;
    delete[] bv;

    return std::pow(acum, 1.0 / s);
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

void GeometryMetadata::setFilename(const java::String* filename) {
    if (filename != nullptr && filename->length() > 0) {
        if (objectFilename != nullptr) {
            delete objectFilename;
        }
        objectFilename = new java::String(*filename);
    } else {
        if (objectFilename != nullptr) {
            delete objectFilename;
            objectFilename = nullptr;
        }
    }
}

java::String* GeometryMetadata::getFilename() const {
    if (objectFilename != nullptr) {
        return new java::String(*objectFilename);
    }
    return nullptr;
}

std::vector<ShapeDescriptor*>& GeometryMetadata::getDescriptors() {
    return descriptorsList;
}

const std::vector<ShapeDescriptor*>& GeometryMetadata::getDescriptors() const {
    return descriptorsList;
}

ShapeDescriptor* GeometryMetadata::getDescriptorByName(const java::String* name) const {
    if (name == nullptr) {
        return nullptr;
    }

    for (size_t i = 0; i < descriptorsList.size(); i++) {
        ShapeDescriptor* s = descriptorsList[i];
        if (s != nullptr) {
            java::String* label = s->getLabel();
            if (label != nullptr && label->equals(*name)) {
                delete label;
                return s;
            }
            delete label;
        }
    }
    return nullptr;
}

void GeometryMetadata::addDescriptor(ShapeDescriptor* descriptor) {
    if (descriptor != nullptr) {
        descriptorsList.push_back(descriptor);
    }
}

java::String* GeometryMetadata::toString() const {
    char fullBuffer[4096];
    int offset = 0;
    const char* filename_str = objectFilename != nullptr ? objectFilename->toCString() : "nullptr";

    offset += snprintf(fullBuffer + offset, sizeof(fullBuffer) - offset,
        "%s\n    . %zu shape descriptors\n",
        filename_str, descriptorsList.size());

    for (size_t i = 0; i < descriptorsList.size(); i++) {
        char line[256];
        offset += snprintf(fullBuffer + offset, sizeof(fullBuffer) - offset, "        . descriptor_%zu\n", i);
    }

    java::String* msg = new java::String(fullBuffer);
    return msg;
}
