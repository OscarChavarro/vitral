import {
  BlobSolid,
  HeightFieldSolid,
  PolyhedralBoundedSolid,
  PolynomialSurface,
  QuadricSurface,
  Volume,
  VoxelVolume,
  type BlobComponent,
  type Geometry,
  type Vector3Dd,
} from '@vitral/base';

/**
 * An exported boundary representation, with the signature of the parameters
 * of the geometry it was exported from (Java's `BrepExportCache.Entry`).
 */
class BrepExportCacheEntry {
  constructor(
    readonly signature: string,
    readonly brep: PolyhedralBoundedSolid | null,
  ) {}
}

/**
 * Port of `render.BrepExportCache` (package private in Java).
 *
 * Boundary representations of the geometries of the scene, for the views that
 * present them as lines (`ProjectedLinesViewBuilder`) or as polygons
 * (`ProjectedSurfacesViewBuilder`), which ask for them each time they are
 * drawn.
 *
 * The boundary representations of the geometries given as a field or as
 * samples (blobs, polynomial and quadric surfaces, height fields, voxels) are
 * polygonizations that take from tens to hundreds of milliseconds, so they are
 * kept while the parameters of their geometry do not change (a signature of
 * them is compared each time). The other volumes export their boundary
 * representation each time, as it is fast.
 *
 * Java's `WeakHashMap` is a `WeakMap` (keys are compared by identity in both).
 * The TypeScript `VoxelVolume` has no threshold yet, so its signature has none.
 */
export class BrepExportCache {
  private readonly entries: WeakMap<Geometry, BrepExportCacheEntry> = new WeakMap<
    Geometry,
    BrepExportCacheEntry
  >();

  /**
   * @param geometry
   * @return the boundary representation of the geometry, or null if it is
   * not a volume that can be exported to one
   */
  get(geometry: Geometry): PolyhedralBoundedSolid | null {
    if (geometry instanceof PolyhedralBoundedSolid) {
      return geometry;
    }
    if (!(geometry instanceof Volume)) {
      return null;
    }
    const signature: string | null = BrepExportCache.signature(geometry);
    if (signature === null) {
      return BrepExportCache.export(geometry);
    }
    let entry: BrepExportCacheEntry | undefined = this.entries.get(geometry);
    if (entry === undefined || entry.signature !== signature) {
      entry = new BrepExportCacheEntry(signature, BrepExportCache.export(geometry));
      this.entries.set(geometry, entry);
    }
    return entry.brep;
  }

  /**
   * `Volume.exportToPolyhedralBoundedSolid`, whose TypeScript declaration in
   * the base class returns an `object`
   */
  private static export(volume: Volume): PolyhedralBoundedSolid | null {
    const brep: object | null = volume.exportToPolyhedralBoundedSolid();
    return brep instanceof PolyhedralBoundedSolid ? brep : null;
  }

  /**
   * @return a text that changes when the parameters of an expensive
   * geometry change, or null for the geometries that are not cached
   */
  private static signature(geometry: Geometry): string | null {
    let sb = '';

    if (geometry instanceof BlobSolid) {
      const blob: BlobSolid = geometry;
      sb += 'blob ' + blob.getThreshold();
      for (let i = 0; i < blob.getComponentCount(); i++) {
        const c: BlobComponent = blob.getComponent(i);
        sb +=
          ' ' + BrepExportCache.vector(c.getCenter()) + ' ' + c.getRadius() + ' ' + c.getStrength();
      }
    } else if (geometry instanceof PolynomialSurface) {
      const polynomial: PolynomialSurface = geometry;
      sb +=
        'polynomial ' + polynomial.getOrder() + '[' + polynomial.getCoefficients().join(', ') + ']';
    } else if (geometry instanceof QuadricSurface) {
      const quadric: QuadricSurface = geometry;
      sb +=
        'quadric ' +
        BrepExportCache.vector(quadric.squareCoefficients()) +
        BrepExportCache.vector(quadric.mixedCoefficients()) +
        BrepExportCache.vector(quadric.linearCoefficients()) +
        quadric.constantCoefficient();
    } else if (geometry instanceof HeightFieldSolid) {
      const field: HeightFieldSolid = geometry;
      let hash = 1;
      for (let z = 0; z < field.getRowCount(); z++) {
        for (let x = 0; x < field.getColumnCount(); x++) {
          hash = (Math.imul(31, hash) + BrepExportCache.doubleHashCode(field.getHeight(x, z))) | 0;
        }
      }
      sb +=
        'heightField ' +
        field.getColumnCount() +
        'x' +
        field.getRowCount() +
        ' ' +
        BrepExportCache.vector(field.getMinimum()) +
        BrepExportCache.vector(field.getMaximum()) +
        ' ' +
        hash;
    } else if (geometry instanceof VoxelVolume) {
      const voxels: VoxelVolume = geometry;
      let hash = 1;
      for (let z = 0; z < voxels.getZSize(); z++) {
        for (let y = 0; y < voxels.getYSize(); y++) {
          for (let x = 0; x < voxels.getXSize(); x++) {
            hash = (Math.imul(31, hash) + voxels.getVoxel(x, y, z)) | 0;
          }
        }
      }
      sb +=
        'voxels ' +
        voxels.getXSize() +
        'x' +
        voxels.getYSize() +
        'x' +
        voxels.getZSize() +
        ' ' +
        hash;
    } else {
      return null;
    }
    return sb;
  }

  private static vector(v: Vector3Dd): string {
    return '(' + v.x() + ',' + v.y() + ',' + v.z() + ')';
  }

  /** Java's `Double.hashCode(value)` */
  private static doubleHashCode(value: number): number {
    const view: DataView = new DataView(new ArrayBuffer(8));
    view.setFloat64(0, Number.isNaN(value) ? NaN : value);
    return (view.getInt32(0) ^ view.getInt32(4)) | 0;
  }
}
