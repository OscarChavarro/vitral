#ifndef __BREP_EXPORT_CACHE__
#define __BREP_EXPORT_CACHE__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "java/util/HashMap.h"
class Geometry;
class PolyhedralBoundedSolid;
class Vector3Dd;

/**
Keeps the boundary representations exported from the volumes of the scene
whose export is expensive (sampled fields: blobs, polynomial and quadric
surfaces, height fields and voxel volumes), so they are not exported again
on each repaint of a drawing (calligraphic) view. Each one is kept with a
signature of the parameters of its volume, and exported again when the
volume is edited.

C++ port note: Java keeps the entries in a `WeakHashMap`, so they go away
with their volumes. Here they live until the cache is destroyed; as the
signature includes the class and all the parameters of the volume, a new
volume created at the address of a deleted one only reuses its solid when
it is equal to it.
*/
class BrepExportCache {
private:
    class _Entry {
    public:
        String signature;
        PolyhedralBoundedSolid* brep;

        _Entry() : signature(), brep(0) {}
    };

    /// Keys are compared by identity
    HashMap<Geometry*, _Entry> entries;
    /// C++ port note: the keys of `entries`, to delete the solids (`HashMap`
    /// has no iteration)
    ArrayList<Geometry*> keys;

    static String signature(Geometry* geometry);
    static String vector(const Vector3Dd& v);

    BrepExportCache(const BrepExportCache&);
    BrepExportCache& operator=(const BrepExportCache&);

public:
    BrepExportCache();
    ~BrepExportCache();

    /**
    @param geometry
    @param outCallerOwns receives true when the returned solid is a new one
    owned by the caller (volumes without signature are exported on each
    call), false when it is the geometry itself or is owned by the cache
    @return the boundary representation of the geometry, or null if it is
    not a volume or has none
    */
    PolyhedralBoundedSolid* get(Geometry* geometry, BOOLEAN& outCallerOwns);
};

#endif
