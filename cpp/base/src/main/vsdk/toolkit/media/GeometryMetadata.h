#ifndef __VSDK_TOOLKIT_MEDIA_GEOMETRYMETADATA_H__
#define __VSDK_TOOLKIT_MEDIA_GEOMETRYMETADATA_H__

#include "MediaEntity.h"
#include <vector>

class ShapeDescriptor;
namespace java { class String; }

/**
This class is a container for a set of different ShapeDescriptor's for an
associated Geometry. This class is designed to contain non-geometric data
associated with geometry, or "metadata". Such a metadata is useful for
indexing, querying and matching of 3D models.
*/
class GeometryMetadata : public MediaEntity {

private:
    static long lastId;

    long id;
    java::String* objectFilename;
    std::vector<ShapeDescriptor*> descriptorsList;

public:
    GeometryMetadata();
    virtual ~GeometryMetadata();

    /**
    Given a pair of GeometryMetadata elements, this method computes the
    MinskowskiDistance between feature vectors.

    Two sets of shape descriptors are "comparable" if they are of the same
    type, in the same order and with the same number of features.

    If given shape descriptors are not comparable, this method return infinite
    distance.
    @param other geometry metadata to be compared with current (`this`) set
    @param s Minkowski factor. If s == 1, this method returns the Manhattan
    distance in the Nth-dimensional space. If s == 2, this method returns
    the euclidean distance in the Nth-dimensional space.
    @param subGroup group label to search for
    */
    double doMinskowskiDistance(const GeometryMetadata* other, double s, const java::String* subGroup) const;

    void setId(long id);
    long getId() const;

    void setFilename(const java::String* filename);
    java::String* getFilename() const;

    std::vector<ShapeDescriptor*>& getDescriptors();
    const std::vector<ShapeDescriptor*>& getDescriptors() const;

    ShapeDescriptor* getDescriptorByName(const java::String* name) const;

    void addDescriptor(ShapeDescriptor* descriptor);

    java::String* toString() const;
};

#endif // __VSDK_TOOLKIT_MEDIA_GEOMETRYMETADATA_H__
