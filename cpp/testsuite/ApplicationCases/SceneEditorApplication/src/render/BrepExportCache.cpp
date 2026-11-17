#include <functional>
#include <sstream>

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

namespace {
void vector(std::ostringstream& out, const Vector3Dd& v)
{
    out << "(" << v.x() << "," << v.y() << "," << v.z() << ")";
}
}

BrepExportCache::~BrepExportCache()
{
    endFrame();
    for ( std::map<const Geometry*, Entry>::iterator it = entries.begin();
          it != entries.end(); ++it ) {
        delete it->second.brep;
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
    std::string text;
    if ( !signature(geometry, text) ) {
        PolyhedralBoundedSolid* brep = volume->exportToPolyhedralBoundedSolid();
        if ( brep != nullptr ) {
            transientBreps.add(brep);
        }
        return brep;
    }
    std::map<const Geometry*, Entry>::iterator it = entries.find(geometry);
    if ( it == entries.end() || it->second.signature != text ) {
        if ( it != entries.end() ) {
            delete it->second.brep;
            entries.erase(it);
        }
        Entry entry;
        entry.signature = text;
        entry.brep = volume->exportToPolyhedralBoundedSolid();
        entry.used = true;
        it = entries.insert(std::make_pair(
            static_cast<const Geometry*>(geometry), entry)).first;
    }
    it->second.used = true;
    return it->second.brep;
}

void BrepExportCache::endFrame()
{
    for ( long i = 0; i < transientBreps.size(); i++ ) {
        delete transientBreps.get(i);
    }
    transientBreps.clear();
    for ( std::map<const Geometry*, Entry>::iterator it = entries.begin();
          it != entries.end(); ) {
        if ( !it->second.used ) {
            delete it->second.brep;
            it = entries.erase(it);
        }
        else {
            it->second.used = false;
            ++it;
        }
    }
}

bool BrepExportCache::signature(Geometry* geometry, std::string& outText)
{
    std::ostringstream sb;
    sb.precision(17);

    if ( BlobSolid* blob = dynamic_cast<BlobSolid*>(geometry) ) {
        sb << "blob " << blob->getThreshold();
        for ( long i = 0; i < blob->getComponentCount(); i++ ) {
            const BlobComponent& c = blob->getComponent(i);
            sb << ' ';
            vector(sb, c.getCenter());
            sb << ' ' << c.getRadius() << ' ' << c.getStrength();
        }
    }
    else if ( PolynomialSurface* polynomial =
              dynamic_cast<PolynomialSurface*>(geometry) ) {
        int order = polynomial->getOrder();
        int count = PolynomialSurface::getTermCount(order);
        const double* coefficients = polynomial->getCoefficients();
        sb << "polynomial " << order << "[";
        for ( int i = 0; coefficients != nullptr && i < count; i++ ) {
            sb << (i > 0 ? ", " : "") << coefficients[i];
        }
        sb << "]";
    }
    else if ( QuadricSurface* quadric =
              dynamic_cast<QuadricSurface*>(geometry) ) {
        const QuadricSurface* q = quadric;
        sb << "quadric ";
        vector(sb, q->squareCoefficients());
        vector(sb, q->mixedCoefficients());
        vector(sb, q->linearCoefficients());
        sb << q->constantCoefficient();
    }
    else if ( HeightFieldSolid* field =
              dynamic_cast<HeightFieldSolid*>(geometry) ) {
        size_t hash = 1;
        for ( long z = 0; z < field->getRowCount(); z++ ) {
            for ( long x = 0; x < field->getColumnCount(); x++ ) {
                hash = 31 * hash +
                    std::hash<double>()(field->getHeight(x, z));
            }
        }
        sb << "heightField " << field->getColumnCount() << 'x'
           << field->getRowCount() << ' ';
        vector(sb, field->getMinimum());
        vector(sb, field->getMaximum());
        sb << ' ' << hash;
    }
    else if ( VoxelVolume* voxels = dynamic_cast<VoxelVolume*>(geometry) ) {
        size_t hash = 1;
        for ( int z = 0; z < voxels->getZSize(); z++ ) {
            for ( int y = 0; y < voxels->getYSize(); y++ ) {
                for ( int x = 0; x < voxels->getXSize(); x++ ) {
                    hash = 31 * hash +
                        static_cast<size_t>(voxels->getVoxel(x, y, z));
                }
            }
        }
        sb << "voxels " << voxels->getXSize() << 'x' << voxels->getYSize()
           << 'x' << voxels->getZSize() << ' ' << voxels->getThreshold()
           << ' ' << hash;
    }
    else {
        return false;
    }
    outText = sb.str();
    return true;
}
