#ifndef __BREP_EXPORT_CACHE__
#define __BREP_EXPORT_CACHE__

#include <map>
#include <string>

#include "java/util/ArrayList.h"

class Geometry;
class PolyhedralBoundedSolid;

/**
Boundary representations of the geometries of the scene, for the views that
present them as lines (`ProjectedLinesViewBuilder`), which ask for them each
time they are drawn.

The boundary representations of the geometries given as a field or as
samples (blobs, polynomial and quadric surfaces, height fields, voxels) are
polygonizations that take from tens to hundreds of milliseconds, so they are
kept while the parameters of their geometry do not change (a signature of
them is compared each time). The other volumes export their boundary
representation each time, as it is fast.

C++ port: the cache owns every exported boundary representation. The ones
exported each time live until `endFrame`, which also forgets the cached
geometries not asked for since the previous `endFrame` (they may have been
deleted: the keys are compared by identity, as the weak keys of Java).
*/
class BrepExportCache {
private:
    struct Entry {
        std::string signature;
        PolyhedralBoundedSolid* brep;
        bool used;
    };

    std::map<const Geometry*, Entry> entries;
    java::ArrayList<PolyhedralBoundedSolid*> transientBreps;

    BrepExportCache(const BrepExportCache& other);
    BrepExportCache& operator=(const BrepExportCache& other);

    /**
    @param out receives a text that changes when the parameters of an
    expensive geometry change
    @return false for the geometries that are not cached
    */
    static bool signature(Geometry* geometry, std::string& out);

public:
    BrepExportCache() {}
    ~BrepExportCache();

    /**
    @return the boundary representation of the geometry (owned by the
    geometry itself if it is one, else by this cache), or null if it is not
    a volume that can be exported to one
    */
    PolyhedralBoundedSolid* get(Geometry* geometry);

    /**
    Frees the boundary representations exported for one frame and the
    cached ones not used in it.
    */
    void endFrame();
};

#endif
