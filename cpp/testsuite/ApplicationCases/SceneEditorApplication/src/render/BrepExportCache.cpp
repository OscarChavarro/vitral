#include <cstring>

#include "java/lang/StringBuilder.h"
#include "java/util/ArrayList.txx"
#include "render/BrepExportCache.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/volume/BlobComponent.h"
#include "vsdk/toolkit/environment/geometry/volume/BlobSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/HeightFieldSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/PolynomialSurface.h"
#include "vsdk/toolkit/environment/geometry/volume/QuadricSurface.h"
#include "vsdk/toolkit/environment/geometry/volume/Volume.h"
#include "vsdk/toolkit/environment/geometry/volume/VoxelVolume.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"

/**
Emulation of Java's `Double.hashCode`.
*/
static int
brepExportCacheDoubleHash(double value)
{
    unsigned long long bits;
    memcpy(&bits, &value, sizeof(bits));
    return (int)(unsigned int)(bits ^ (bits >> 32));
}

BrepExportCache::~BrepExportCache()
{
    endFrame();
    for ( long i = 0; i < entries.size(); i++ ) {
        delete entries[i].brep;
    }
    entries.clear();
}

PolyhedralBoundedSolid* BrepExportCache::get(Geometry* geometry)
{
    PolyhedralBoundedSolid* own =
        dynamic_cast<PolyhedralBoundedSolid*>(geometry);
    if ( own != nullptr ) {
        return own;
    }
    Volume* volume = dynamic_cast<Volume*>(geometry);
    if ( volume == nullptr ) {
        return nullptr;
    }
    java::String text;
    if ( !signature(geometry, text) ) {
        PolyhedralBoundedSolid* brep = volume->exportToPolyhedralBoundedSolid();
        if ( brep != nullptr ) {
            transientBreps.add(brep);
        }
        return brep;
    }
    long index = -1;
    for ( long i = 0; i < entries.size(); i++ ) {
        if ( entries[i].geometry == geometry ) {
            index = i;
            break;
        }
    }
    if ( index < 0 || !entries[index].signature.equals(text) ) {
        if ( index >= 0 ) {
            delete entries[index].brep;
            entries.remove(index);
        }
        Entry entry;
        entry.geometry = geometry;
        entry.signature = text;
        entry.brep = volume->exportToPolyhedralBoundedSolid();
        entries.add(entry);
        index = entries.size() - 1;
    }
    entries[index].used = true;
    return entries[index].brep;
}

void BrepExportCache::endFrame()
{
    for ( long i = 0; i < transientBreps.size(); i++ ) {
        delete transientBreps[i];
    }
    transientBreps.clear();
    for ( long i = 0; i < entries.size(); ) {
        if ( !entries[i].used ) {
            delete entries[i].brep;
            entries.remove(i);
        }
        else {
            entries[i].used = false;
            i++;
        }
    }
}

bool BrepExportCache::signature(Geometry* geometry, java::String& outText)
{
    java::StringBuilder sb;

    if ( BlobSolid* blob = dynamic_cast<BlobSolid*>(geometry) ) {
        sb.append("blob ").append(blob->getThreshold());
        for ( long i = 0; i < blob->getComponentCount(); i++ ) {
            const BlobComponent& c = blob->getComponent(i);
            sb.append(' ').append(vector(c.getCenter())).append(' ')
                .append(c.getRadius()).append(' ').append(c.getStrength());
        }
    }
    else if ( PolynomialSurface* polynomial =
              dynamic_cast<PolynomialSurface*>(geometry) ) {
        int order = polynomial->getOrder();
        int count = PolynomialSurface::getTermCount(order);
        const double* coefficients = polynomial->getCoefficients();
        sb.append("polynomial ").append(order).append("[");
        for ( int i = 0; coefficients != nullptr && i < count; i++ ) {
            sb.append(i > 0 ? ", " : "").append(coefficients[i]);
        }
        sb.append("]");
    }
    else if ( QuadricSurface* quadric =
              dynamic_cast<QuadricSurface*>(geometry) ) {
        const QuadricSurface* q = quadric;
        sb.append("quadric ").append(vector(q->squareCoefficients()))
            .append(vector(q->mixedCoefficients()))
            .append(vector(q->linearCoefficients()))
            .append(q->constantCoefficient());
    }
    else if ( HeightFieldSolid* field =
              dynamic_cast<HeightFieldSolid*>(geometry) ) {
        int hash = 1;
        for ( long z = 0; z < field->getRowCount(); z++ ) {
            for ( long x = 0; x < field->getColumnCount(); x++ ) {
                hash = (int)(31u * (unsigned int)hash +
                    (unsigned int)brepExportCacheDoubleHash(field->getHeight(x, z)));
            }
        }
        sb.append("heightField ").append(field->getColumnCount()).append('x')
            .append(field->getRowCount()).append(' ')
            .append(vector(field->getMinimum())).append(vector(field->getMaximum()))
            .append(' ').append(hash);
    }
    else if ( VoxelVolume* voxels = dynamic_cast<VoxelVolume*>(geometry) ) {
        int hash = 1;
        for ( int z = 0; z < voxels->getZSize(); z++ ) {
            for ( int y = 0; y < voxels->getYSize(); y++ ) {
                for ( int x = 0; x < voxels->getXSize(); x++ ) {
                    hash = (int)(31u * (unsigned int)hash +
                        (unsigned int)voxels->getVoxel(x, y, z));
                }
            }
        }
        sb.append("voxels ").append(voxels->getXSize()).append('x')
            .append(voxels->getYSize()).append('x').append(voxels->getZSize())
            .append(' ').append(voxels->getThreshold()).append(' ').append(hash);
    }
    else {
        return false;
    }
    outText = sb.toString();
    return true;
}

java::String BrepExportCache::vector(const Vector3Dd& v)
{
    java::StringBuilder sb;
    sb.append("(").append(v.x()).append(",").append(v.y()).append(",")
        .append(v.z()).append(")");
    return sb.toString();
}
