/*
Deep module specifiers rather than the `@vitral/base` barrel: the fixture is
also built inside the tile workers of `render/raytracer-tile.worker.ts` (see
the note there).
*/
import { ArrayList } from '@vitral/base/java/util/ArrayList';
import { Matrix4x4d } from '@vitral/base/vsdk/toolkit/common/linealAlgebra/Matrix4x4d';
import { Vector3Dd } from '@vitral/base/vsdk/toolkit/common/linealAlgebra/Vector3Dd';
import type { Geometry } from '@vitral/base/vsdk/toolkit/environment/geometry/Geometry';
import { GeometryBounds } from '@vitral/base/vsdk/toolkit/environment/geometry/element/GeometryBounds';
import { BezierPatchSubdivision } from '@vitral/base/vsdk/toolkit/environment/geometry/surface/BezierPatchSubdivision';
import { SubdividedBezierPatch } from '@vitral/base/vsdk/toolkit/environment/geometry/surface/SubdividedBezierPatch';
import type { TriangleMesh } from '@vitral/base/vsdk/toolkit/environment/geometry/surface/TriangleMesh';
import { BlobComponent } from '@vitral/base/vsdk/toolkit/environment/geometry/volume/BlobComponent';
import { BlobSolid } from '@vitral/base/vsdk/toolkit/environment/geometry/volume/BlobSolid';
import { Box } from '@vitral/base/vsdk/toolkit/environment/geometry/volume/Box';
import { HeightFieldSolid } from '@vitral/base/vsdk/toolkit/environment/geometry/volume/HeightFieldSolid';
import { PolynomialSurface } from '@vitral/base/vsdk/toolkit/environment/geometry/volume/PolynomialSurface';
import { QuadricSurface } from '@vitral/base/vsdk/toolkit/environment/geometry/volume/QuadricSurface';
import { Sphere } from '@vitral/base/vsdk/toolkit/environment/geometry/volume/Sphere';
import type { SimpleMaterial } from '@vitral/base/vsdk/toolkit/environment/material/SimpleMaterial';
import { SimpleBody } from '@vitral/base/vsdk/toolkit/environment/scene/SimpleBody';
import { CsgBody } from '@vitral/base/vsdk/toolkit/environment/scene/csg/CsgBody';
import { CsgBodyEvaluator } from '@vitral/base/vsdk/toolkit/environment/scene/csg/CsgBodyEvaluator';
import { CsgOperation } from '@vitral/base/vsdk/toolkit/environment/scene/csg/CsgOperation';
import { BezierPatchTessellator } from '@vitral/base/vsdk/toolkit/processing/BezierPatchTessellator';
import { SolidBoundaryTessellationReport } from '@vitral/base/vsdk/toolkit/processing/SolidBoundaryTessellationReport';
import { SolidBoundaryTessellator } from '@vitral/base/vsdk/toolkit/processing/SolidBoundaryTessellator';
import type { ShadersGeometry } from './shaders-geometry';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/ShadersExample/src/model/ShadersGeometryFixture.java`
 * (cpp `ShadersGeometryFixture`).
 *
 * One non-sphere geometry of the example, built once and shared by both
 * renderers: the software path ray traces a body that references its
 * geometries, and the OpenGL path draws the drawable mesh that VITRAL
 * tessellation derives from that same body. The mesh is an approximation of
 * declared resolution, never a second definition of the geometry.
 *
 * The software path traces in Web Workers, which share no heap: each worker
 * builds its own fixture of the same kind (the construction is deterministic)
 * and asks it for the frame's body, so both sides still trace the same
 * geometries.
 */
export class ShadersGeometryFixture {
  /** Cells along the longest side of the region of a tessellated solid */
  static readonly SOLID_RESOLUTION = 96;
  /** Parameter steps of the patch mesh along u and v */
  static readonly PATCH_STEPS = 48;

  private readonly kind: ShadersGeometry;
  private readonly geometries: ArrayList<Geometry> = new ArrayList<Geometry>();
  private region: GeometryBounds = new GeometryBounds();
  private drawableMesh: TriangleMesh | null;
  private readonly report: SolidBoundaryTessellationReport = new SolidBoundaryTessellationReport();

  /** PRE: kind is not SPHERE */
  constructor(inKind: ShadersGeometry) {
    this.kind = inKind;
    this.drawableMesh = null;
    switch (this.kind) {
      case 'QUARTIC':
        this.buildQuartic();
        break;
      case 'BLOB':
        this.buildBlob();
        break;
      case 'HEIGHT_FIELD':
        this.buildHeightField();
        break;
      case 'CSG':
        this.buildCsg();
        break;
      case 'PATCH':
        this.buildPatch();
        break;
      case 'SPHERE':
        break;
    }
  }

  getKind(): ShadersGeometry {
    return this.kind;
  }

  /** Resolution and accuracy of the solid meshes (empty for the patch) */
  getTessellationReport(): SolidBoundaryTessellationReport {
    return this.report;
  }

  /** Rounded cube x^4 + y^4 + z^4 - 0.4 < 0, a quartic PolynomialSurface */
  private buildQuartic(): void {
    const quartic = new PolynomialSurface(4, false);
    const c: number[] = quartic.getCoefficients();
    c[PolynomialSurface.getTermIndex(4, 4, 0, 0)] = 1.0;
    c[PolynomialSurface.getTermIndex(4, 0, 4, 0)] = 1.0;
    c[PolynomialSurface.getTermIndex(4, 0, 0, 4)] = 1.0;
    c[PolynomialSurface.getTermIndex(4, 0, 0, 0)] = -0.4;
    this.geometries.add(quartic);
    this.region.setFinite(new Vector3Dd(-0.85, -0.85, -0.85), new Vector3Dd(0.85, 0.85, 0.85));
  }

  /** Four merged blob components */
  private buildBlob(): void {
    const blob = new BlobSolid(0.5, false);
    blob.addComponent(new BlobComponent(new Vector3Dd(-0.45, 0, -0.3), 0.8, 1.0));
    blob.addComponent(new BlobComponent(new Vector3Dd(0.45, 0, -0.3), 0.8, 1.0));
    blob.addComponent(new BlobComponent(new Vector3Dd(0, 0, 0.45), 0.8, 1.0));
    blob.addComponent(new BlobComponent(new Vector3Dd(0, -0.35, 0), 0.6, 1.0));
    this.geometries.add(blob);
    const bounds = new GeometryBounds();
    blob.getBounds(bounds);
    this.region = bounds.inflated(0.05);
  }

  /**
   * Hills on a 33 x 33 grid, laid with its height along world z and scaled
   * into a 1.6 unit square by the body's placement.
   */
  private buildHeightField(): void {
    const size = 33;
    const field = new HeightFieldSolid(
      size,
      size,
      new Vector3Dd(1, 0, 1),
      new Vector3Dd(size - 2, 1, size - 2),
    );
    for (let x = 0; x < size; x++) {
      for (let z = 0; z < size; z++) {
        const u = (x - 16.0) / 16.0,
          v = (z - 16.0) / 16.0;
        const h =
          0.45 + 0.3 * Math.cos(3.0 * u) * Math.cos(2.5 * v) + 0.15 * Math.sin(5.0 * (u + v));
        field.setHeight(x, z, h);
      }
    }
    field.updateBlocks();
    this.geometries.add(field);
    this.region.setFinite(new Vector3Dd(-0.82, -0.82, -0.45), new Vector3Dd(0.82, 0.82, 0.35));
  }

  /**
   * The classic CSG sample: the intersection of a cube and a sphere, minus
   * three cylinders along the axes, evaluated with Roth's classification.
   */
  private buildCsg(): void {
    this.geometries.add(new Box(1.3, 1.3, 1.3));
    this.geometries.add(new Sphere(0.85));
    const r2 = 0.33 * 0.33;
    this.geometries.add(
      new QuadricSurface(
        new Vector3Dd(1, 1, 0),
        new Vector3Dd(0, 0, 0),
        new Vector3Dd(0, 0, 0),
        -r2,
      ),
    );
    this.geometries.add(
      new QuadricSurface(
        new Vector3Dd(0, 1, 1),
        new Vector3Dd(0, 0, 0),
        new Vector3Dd(0, 0, 0),
        -r2,
      ),
    );
    this.geometries.add(
      new QuadricSurface(
        new Vector3Dd(1, 0, 1),
        new Vector3Dd(0, 0, 0),
        new Vector3Dd(0, 0, 0),
        -r2,
      ),
    );
    this.region.setFinite(new Vector3Dd(-0.72, -0.72, -0.72), new Vector3Dd(0.72, 0.72, 0.72));
  }

  /** A wavy open Bezier patch facing the camera */
  private buildPatch(): void {
    const net: Vector3Dd[][] = [];
    for (let i = 0; i < 4; i++) {
      net.push([]);
      for (let j = 0; j < 4; j++) {
        const x = -0.9 + 0.6 * i,
          z = -0.9 + 0.6 * j;
        const y = ((i + j) % 2 === 0 ? -0.35 : 0.35) * (i === 0 || i === 3 ? 0.5 : 1.0);
        net[i]!.push(new Vector3Dd(x, y, z));
      }
    }
    this.geometries.add(
      new SubdividedBezierPatch(BezierPatchSubdivision.UniformGrid, 16, 16, 0.0, net),
    );
  }

  /**
   * A new body for one frame, referencing the fixture's geometries, owning
   * the given material (which may be null) and turned by the rotation.
   */
  createBody(material: SimpleMaterial | null, rotation: Matrix4x4d): SimpleBody {
    let body: SimpleBody;
    // cpp setGeometryReference (borrowed); the same under garbage collection.
    if (this.kind === 'CSG') {
      const csg = new CsgBody(CsgOperation.DIFFERENCE, CsgBodyEvaluator.ROTH);
      const core = csg.addNode(0, CsgOperation.INTERSECTION);
      for (let i = 0; i < this.geometries.size(); i++) {
        const leaf = new SimpleBody();
        // cpp leaves start without a material, so the CSG body's one shades
        // them; SimpleBody starts with a default one, as in Java.
        leaf.setMaterial(null);
        leaf.setGeometry(this.geometries.get(i));
        csg.addLeaf(i < 2 ? core : 0, leaf);
      }
      body = csg;
    } else {
      body = new SimpleBody();
      body.setGeometry(this.geometries.get(0));
    }
    if (material !== null) body.setMaterial(material);
    let forward = Matrix4x4d.identityMatrix();
    if (this.kind === 'HEIGHT_FIELD') {
      // Grid x to world x, grid z to world -y, height y to world z.
      const s = 1.6 / 30.0,
        hz = 0.7;
      const m: number[][] = [
        [s, 0, 0, -16.0 * s],
        [0, 0, -s, 16.0 * s],
        [0, hz, 0, -0.4],
        [0, 0, 0, 1],
      ];
      forward = new Matrix4x4d(m);
    }
    const placed = rotation.multiply(forward);
    body.setAffinePlacement(placed, placed.inverse());
    return body;
  }

  /** Mesh drawn by the OpenGL path, built on first use; owned by the fixture */
  getDrawableMesh(): TriangleMesh | null {
    if (this.drawableMesh !== null) return this.drawableMesh;
    if (this.kind === 'PATCH') {
      this.drawableMesh = BezierPatchTessellator.tessellate(
        this.geometries.get(0) as SubdividedBezierPatch,
        ShadersGeometryFixture.PATCH_STEPS,
        ShadersGeometryFixture.PATCH_STEPS,
      );
      return this.drawableMesh;
    }
    const body = this.createBody(null, Matrix4x4d.identityMatrix());
    this.drawableMesh = SolidBoundaryTessellator.tessellate(
      body,
      this.region,
      ShadersGeometryFixture.SOLID_RESOLUTION,
      this.report,
    );
    return this.drawableMesh;
  }

  static nameOf(geometry: ShadersGeometry): string {
    switch (geometry) {
      case 'SPHERE':
        return 'sphere';
      case 'QUARTIC':
        return 'quartic';
      case 'BLOB':
        return 'blob';
      case 'HEIGHT_FIELD':
        return 'heightfield';
      case 'CSG':
        return 'csg';
      case 'PATCH':
        return 'patch';
    }
    return 'sphere';
  }

  /** @return null for an unknown name (cpp: false, with the result unchanged) */
  static parse(name: string): ShadersGeometry | null {
    for (let g: ShadersGeometry = 'SPHERE'; ; g = ShadersGeometryFixture.next(g)) {
      if (name === ShadersGeometryFixture.nameOf(g)) {
        return g;
      }
      if (ShadersGeometryFixture.next(g) === 'SPHERE') return null;
    }
  }

  static next(geometry: ShadersGeometry): ShadersGeometry {
    switch (geometry) {
      case 'SPHERE':
        return 'QUARTIC';
      case 'QUARTIC':
        return 'BLOB';
      case 'BLOB':
        return 'HEIGHT_FIELD';
      case 'HEIGHT_FIELD':
        return 'CSG';
      case 'CSG':
        return 'PATCH';
      case 'PATCH':
        return 'SPHERE';
    }
    return 'SPHERE';
  }
}
