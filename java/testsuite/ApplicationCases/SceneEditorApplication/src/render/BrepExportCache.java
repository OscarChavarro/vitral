package render;

import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.BlobComponent;
import vsdk.toolkit.environment.geometry.volume.BlobSolid;
import vsdk.toolkit.environment.geometry.volume.HeightFieldSolid;
import vsdk.toolkit.environment.geometry.volume.PolynomialSurface;
import vsdk.toolkit.environment.geometry.volume.QuadricSurface;
import vsdk.toolkit.environment.geometry.volume.Volume;
import vsdk.toolkit.environment.geometry.volume.VoxelVolume;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

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
*/
class BrepExportCache
{
    private static final class Entry
    {
        final String signature;
        final PolyhedralBoundedSolid brep;

        Entry(String signature, PolyhedralBoundedSolid brep)
        {
            this.signature = signature;
            this.brep = brep;
        }
    }

    /// Keys are compared by identity: geometries do not redefine equals
    private final Map<Geometry, Entry> entries = new WeakHashMap<Geometry, Entry>();

    /**
    @param geometry
    @return the boundary representation of the geometry, or null if it is
    not a volume that can be exported to one
    */
    PolyhedralBoundedSolid get(Geometry geometry)
    {
        if ( geometry instanceof PolyhedralBoundedSolid ) {
            return (PolyhedralBoundedSolid)geometry;
        }
        if ( !(geometry instanceof Volume) ) {
            return null;
        }
        String signature = signature(geometry);
        if ( signature == null ) {
            return ((Volume)geometry).exportToPolyhedralBoundedSolid();
        }
        Entry entry = entries.get(geometry);
        if ( entry == null || !entry.signature.equals(signature) ) {
            entry = new Entry(signature,
                ((Volume)geometry).exportToPolyhedralBoundedSolid());
            entries.put(geometry, entry);
        }
        return entry.brep;
    }

    /**
    @return a text that changes when the parameters of an expensive
    geometry change, or null for the geometries that are not cached
    */
    private static String signature(Geometry geometry)
    {
        StringBuilder sb = new StringBuilder();

        if ( geometry instanceof BlobSolid blob ) {
            sb.append("blob ").append(blob.getThreshold());
            for ( long i = 0; i < blob.getComponentCount(); i++ ) {
                BlobComponent c = blob.getComponent(i);
                sb.append(' ').append(vector(c.getCenter())).append(' ')
                    .append(c.getRadius()).append(' ').append(c.getStrength());
            }
        }
        else if ( geometry instanceof PolynomialSurface polynomial ) {
            sb.append("polynomial ").append(polynomial.getOrder())
                .append(Arrays.toString(polynomial.getCoefficients()));
        }
        else if ( geometry instanceof QuadricSurface quadric ) {
            sb.append("quadric ").append(vector(quadric.squareCoefficients()))
                .append(vector(quadric.mixedCoefficients()))
                .append(vector(quadric.linearCoefficients()))
                .append(quadric.constantCoefficient());
        }
        else if ( geometry instanceof HeightFieldSolid field ) {
            int hash = 1;
            for ( long z = 0; z < field.getRowCount(); z++ ) {
                for ( long x = 0; x < field.getColumnCount(); x++ ) {
                    hash = 31 * hash + Double.hashCode(field.getHeight(x, z));
                }
            }
            sb.append("heightField ").append(field.getColumnCount()).append('x')
                .append(field.getRowCount()).append(' ')
                .append(vector(field.getMinimum())).append(vector(field.getMaximum()))
                .append(' ').append(hash);
        }
        else if ( geometry instanceof VoxelVolume voxels ) {
            int hash = 1;
            for ( int z = 0; z < voxels.getZSize(); z++ ) {
                for ( int y = 0; y < voxels.getYSize(); y++ ) {
                    for ( int x = 0; x < voxels.getXSize(); x++ ) {
                        hash = 31 * hash + voxels.getVoxel(x, y, z);
                    }
                }
            }
            sb.append("voxels ").append(voxels.getXSize()).append('x')
                .append(voxels.getYSize()).append('x').append(voxels.getZSize())
                .append(' ').append(voxels.getThreshold()).append(' ').append(hash);
        }
        else {
            return null;
        }
        return sb.toString();
    }

    private static String vector(Vector3Dd v)
    {
        return "(" + v.x() + "," + v.y() + "," + v.z() + ")";
    }
}
