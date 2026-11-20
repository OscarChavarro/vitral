#ifndef __BREP_EXPORT_CACHE__
#define __BREP_EXPORT_CACHE__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"

class Geometry;
class PolyhedralBoundedSolid;
class Vector3Dd;

/**
Boundary representations of the geometries of the scene, for the views that
present them as lines (`ProjectedLinesViewBuilder`) or as visible polygons
(`ProjectedSurfacesViewBuilder`), which ask for them each time they are
drawn.

The boundary representations of the geometries given as a field or as
samples (blobs, polynomial and quadric surfaces, height fields, voxels) are
polygonizations that take from tens to hundreds of milliseconds, so they are
kept while the parameters of their geometry do not change (a signature of
them is compared each time). The other volumes export their boundary
representation each time, as it is fast.

C++ port: the cache owns every exported boundary representation. The ones
exported each time live until `endFrame`, which also forgets the cached
geometries not asked for since the previous `endFrame` (they may have been
deleted: the keys are compared by identity, as the weak keys of Java). It
only uses the `java::` layer, so the framebuffer variants (and the kernel)
can use it.
*/
class BrepExportCache {
private:
    struct Entry {
        const Geometry* geometry;
        java::String signature;
        PolyhedralBoundedSolid* brep;
        bool used;

        Entry() : geometry(nullptr), brep(nullptr), used(false) {}
    };

    java::ArrayList<Entry> entries;
    java::ArrayList<PolyhedralBoundedSolid*> transientBreps;

    BrepExportCache(const BrepExportCache& other);
    BrepExportCache& operator=(const BrepExportCache& other);

    /**
    @param out receives a text that changes when the parameters of an
    expensive geometry change
    @return false for the geometries that are not cached
    */
    static bool signature(Geometry* geometry, java::String& out);
    static java::String vector(const Vector3Dd& v);

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
