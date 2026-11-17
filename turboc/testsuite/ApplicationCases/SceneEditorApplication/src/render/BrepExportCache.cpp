#include <stdio.h>
#include <string.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/volume/BlobComponent.h"
#include "vsdk/toolkit/environment/geometry/volume/BlobSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/HeightFieldSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/PolynomialSurface.h"
#include "vsdk/toolkit/environment/geometry/volume/QuadricSurface.h"
#include "vsdk/toolkit/environment/geometry/volume/Volume.h"
#include "vsdk/toolkit/environment/geometry/volume/VoxelVolume.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "render/BrepExportCache.h"

/**
As Java `Double.hashCode`: the two halves of the bits of the number, xored.
C++ port note: computed from the bytes (little endian, as every target of
the port), without 64 bit integers.
*/
static unsigned long doubleHashCode(double value)
{
    unsigned char bytes[8];
    unsigned long low;
    unsigned long high;

    memcpy(bytes, &value, 8);
    low = ((unsigned long)bytes[0]) | (((unsigned long)bytes[1]) << 8) |
        (((unsigned long)bytes[2]) << 16) | (((unsigned long)bytes[3]) << 24);
    high = ((unsigned long)bytes[4]) | (((unsigned long)bytes[5]) << 8) |
        (((unsigned long)bytes[6]) << 16) | (((unsigned long)bytes[7]) << 24);
    return (low ^ high) & 0xFFFFFFFFUL;
}

BrepExportCache::BrepExportCache() : entries(), keys()
{
}

BrepExportCache::~BrepExportCache()
{
    long i;

    for ( i = 0; i < keys.size(); i++ ) {
        _Entry* entry = entries.get(keys[i]);
        if ( entry != NULL ) {
            delete entry->brep;
        }
    }
}

PolyhedralBoundedSolid* BrepExportCache::get(Geometry* geometry,
    BOOLEAN& outCallerOwns)
{
    outCallerOwns = FALSE;
    if ( geometry->instanceOf(PolyhedralBoundedSolid::CLASS) ) {
        return (PolyhedralBoundedSolid*)geometry;
    }
    if ( !geometry->instanceOf(Volume::CLASS) ) {
        return NULL;
    }
    String signatureText = signature(geometry);
    if ( signatureText.isEmpty() ) {
        outCallerOwns = TRUE;
        return ((Volume*)geometry)->exportToPolyhedralBoundedSolid();
    }
    _Entry* entry = entries.get(geometry);
    if ( entry == NULL || !entry->signature.equals(signatureText) ) {
        _Entry fresh;
        fresh.signature = signatureText;
        fresh.brep = ((Volume*)geometry)->exportToPolyhedralBoundedSolid();
        if ( entry == NULL ) {
            keys.add(geometry);
        }
        else {
            delete entry->brep;
        }
        entries.put(geometry, fresh);
        entry = entries.get(geometry);
    }
    return entry->brep;
}

/**
C++ port note: an empty signature stands for the Java null one.
*/
String BrepExportCache::signature(Geometry* geometry)
{
    char buffer[96];
    String sb;
    long i;

    if ( geometry->instanceOf(BlobSolid::CLASS) ) {
        BlobSolid* blob = (BlobSolid*)geometry;
        sprintf(buffer, "blob %.17g", blob->getThreshold());
        sb = buffer;
        for ( i = 0; i < blob->getComponentCount(); i++ ) {
            const BlobComponent& c = blob->getComponent(i);
            sprintf(buffer, " %.17g %.17g", c.getRadius(), c.getStrength());
            sb = sb + " " + vector(c.getCenter()) + buffer;
        }
    }
    else if ( geometry->instanceOf(PolynomialSurface::CLASS) ) {
        PolynomialSurface* polynomial = (PolynomialSurface*)geometry;
        int terms = PolynomialSurface::getTermCount(polynomial->getOrder());
        sprintf(buffer, "polynomial %d[", polynomial->getOrder());
        sb = buffer;
        for ( i = 0; i < terms; i++ ) {
            sprintf(buffer, "%s%.17g", i > 0 ? ", " : "",
                polynomial->getCoefficients()[i]);
            sb = sb + buffer;
        }
        sb = sb + "]";
    }
    else if ( geometry->instanceOf(QuadricSurface::CLASS) ) {
        QuadricSurface* quadric = (QuadricSurface*)geometry;
        sprintf(buffer, "%.17g", quadric->constantCoefficient());
        sb = String("quadric ") + vector(quadric->squareCoefficients()) +
            vector(quadric->mixedCoefficients()) +
            vector(quadric->linearCoefficients()) + buffer;
    }
    else if ( geometry->instanceOf(HeightFieldSolid::CLASS) ) {
        HeightFieldSolid* field = (HeightFieldSolid*)geometry;
        unsigned long hash = 1;
        long x;
        long z;
        for ( z = 0; z < field->getRowCount(); z++ ) {
            for ( x = 0; x < field->getColumnCount(); x++ ) {
                hash = (31 * hash + doubleHashCode(field->getHeight(x, z))) &
                    0xFFFFFFFFUL;
            }
        }
        sprintf(buffer, "heightField %ldx%ld ", field->getColumnCount(),
            field->getRowCount());
        sb = String(buffer) + vector(field->getMinimum()) +
            vector(field->getMaximum());
        sprintf(buffer, " %lu", hash);
        sb = sb + buffer;
    }
    else if ( geometry->instanceOf(VoxelVolume::CLASS) ) {
        VoxelVolume* voxels = (VoxelVolume*)geometry;
        unsigned long hash = 1;
        int x;
        int y;
        int z;
        for ( z = 0; z < voxels->getZSize(); z++ ) {
            for ( y = 0; y < voxels->getYSize(); y++ ) {
                for ( x = 0; x < voxels->getXSize(); x++ ) {
                    hash = (31 * hash + voxels->getVoxel(x, y, z)) &
                        0xFFFFFFFFUL;
                }
            }
        }
        sprintf(buffer, "voxels %dx%dx%d %d %lu", voxels->getXSize(),
            voxels->getYSize(), voxels->getZSize(), voxels->getThreshold(),
            hash);
        sb = buffer;
    }
    return sb;
}

String BrepExportCache::vector(const Vector3Dd& v)
{
    char buffer[80];
    sprintf(buffer, "(%.17g,%.17g,%.17g)", v.x(), v.y(), v.z());
    return String(buffer);
}
