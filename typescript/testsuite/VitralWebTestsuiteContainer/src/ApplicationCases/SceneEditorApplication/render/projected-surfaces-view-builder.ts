//= References:                                                             =
//= [WEIL1977] Weiler, Kevin. Atherton, Peter. "Hidden surface removal      =
//=          using polygon area sorting". SIGGRAPH proceedings, 1977.       =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

import {
  ArrayList,
  Camera,
  ColorRgb,
  FlatShader,
  HiddenSurfaceRenderer,
  RayHit,
  SimpleBody,
  TraceWorkspace,
  Vector3Dd,
  type _PolyhedralBoundedSolidFace,
  type Geometry,
  type Light,
  type PolyhedralBoundedSolid,
  type SimpleMaterial,
  type Viewport,
} from '@vitral/base';
import { CameraState } from '../model/history/camera-state';
import type { Scene } from '../model/scene';
import { BrepExportCache } from './brep-export-cache';
import { ProjectedPolygon } from './projected-polygon';

/**
 * The polygons of a viewport, with what they were computed for (Java's
 * `ProjectedSurfacesViewBuilder.CachedView`).
 */
class CachedView {
  cameraState!: CameraState;
  width = 0;
  height = 0;
  generation = 0;
  withShadows = false;
  polygons: readonly ProjectedPolygon[] = [];
}

/**
 * Port of `render.ProjectedSurfacesViewBuilder`.
 *
 * Builds the drawing of a viewport as projected 2D polygons, computed entirely
 * in the processor: the visible surfaces of the solids, solved by the hidden
 * surface removal of Weiler and Atherton [WEIL1977] (`HiddenSurfaceRenderer`),
 * whose output is a set of polygons that do not overlap on the view. The
 * result is a list of `ProjectedPolygon`s, so this class does not depend on the
 * 2D technology that finally draws them (see `render/html`), as
 * `ProjectedLinesViewBuilder` does for lines.
 *
 * Only the bodies that are (or can be exported to) a `PolyhedralBoundedSolid`
 * are presented, as [WEIL1977] works on polygons. Each visible polygon is
 * filled with the color of its face (constant shading [FOLE1992].16.2.1, by
 * `FlatShader`) under the lights of the scene, evaluated at the center of the
 * face. With shadows (see `RendererConfiguration.isWithShadowsSet`, `F10`),
 * the visible polygons are divided by the shadows the solids cast, computed
 * by the same algorithm from each light [WEIL1977].218, and each part is
 * shaded only with the lights that reach it. As the polygons have no depth,
 * the annotations of the view (selection, lights, gizmos) are drawn over them,
 * without depth test, as over a raytraced image.
 *
 * The polygons of a viewport are kept, and given again, while its camera, its
 * size, the shadows option and the content of the scene (see
 * `DrawingArea.getContentGeneration`) do not change: the hidden surface
 * removal of a complex scene takes from tens to hundreds of milliseconds, and
 * the frame is drawn again when only overlays change.
 *
 * Java's `WeakHashMap` of the views is a `WeakMap`; its map of colors, keyed
 * by the list of the face, the body and the lights (compared by identity),
 * is keyed here by the identifiers given to those objects.
 */
export class ProjectedSurfacesViewBuilder {
  private static readonly NO_LIGHT_COLOR = new ColorRgb(0.8, 0.8, 0.8);

  private readonly brepCache: BrepExportCache = new BrepExportCache();
  private readonly cache: WeakMap<Viewport, CachedView> = new WeakMap<Viewport, CachedView>();
  private readonly shader: FlatShader = new FlatShader();
  private readonly workspace: TraceWorkspace = new TraceWorkspace();
  /// Identity of the objects in the keys of the colors
  private readonly identities: WeakMap<object, number> = new WeakMap<object, number>();
  private nextIdentity = 0;

  /**
   * Builds the visible polygons presenting the solids of the scene in a
   * viewport.
   * @param scene scene to present
   * @param viewport viewport whose active camera is used; its camera must
   * have its viewport size updated
   * @param width width of the viewport, in pixels
   * @param height height of the viewport, in pixels
   * @param generation content generation of the scene (see
   * `DrawingArea.getContentGeneration`)
   * @param withShadows true to cast the shadows of the solids
   * @return the polygons to fill, which do not overlap
   */
  buildView(
    scene: Scene,
    viewport: Viewport,
    width: number,
    height: number,
    generation: number,
    withShadows: boolean,
  ): readonly ProjectedPolygon[] {
    const camera: Camera = viewport.getActiveCamera();
    const cameraState: CameraState = CameraState.capture(camera);
    let cached: CachedView | undefined = this.cache.get(viewport);

    if (
      cached !== undefined &&
      cached.width === width &&
      cached.height === height &&
      cached.generation === generation &&
      cached.withShadows === withShadows &&
      cached.cameraState.isSameState(cameraState)
    ) {
      return cached.polygons;
    }

    const solids: ArrayList<SimpleBody> = new ArrayList<SimpleBody>();
    for (const body of scene.scene.getSimpleBodies()) {
      const geometry: Geometry | null = body.getGeometry();
      if (scene.isHidden(body) || geometry === null) {
        continue;
      }
      const brep: PolyhedralBoundedSolid | null = this.brepCache.get(geometry);
      if (brep !== null) {
        solids.add(ProjectedSurfacesViewBuilder.brepBody(body, brep));
      }
    }

    const polygons: ProjectedPolygon[] = [];
    const lights: ArrayList<Light> | null = scene.scene.getLights();
    const shadows: boolean = withShadows && lights !== null && !lights.isEmpty();
    if (!solids.isEmpty()) {
      const visible: ArrayList<HiddenSurfaceRenderer.VisiblePolygon> = shadows
        ? HiddenSurfaceRenderer.executeWeilerAthertonAlgorithm(solids, camera, lights!)
        : HiddenSurfaceRenderer.executeWeilerAthertonAlgorithm(solids, camera);
      // The color depends on the face and, with shadows, on the
      // lights reaching the polygon
      const colors: Map<string, ColorRgb> = new Map<string, ColorRgb>();
      for (const polygon of visible) {
        const key: object[] = [];
        key.push(polygon.face);
        key.push(polygon.body);
        if (polygon.lights !== null) {
          key.push(...polygon.lights);
        }
        const keyText: string = this.identityKey(key);
        let color: ColorRgb | undefined = colors.get(keyText);
        if (color === undefined) {
          color = this.shadeFace(
            polygon,
            scene,
            camera,
            polygon.lights !== null ? polygon.lights : lights,
          );
          colors.set(keyText, color);
        }
        polygons.push(new ProjectedPolygon(polygon.area, color));
      }
    }

    cached = new CachedView();
    cached.cameraState = cameraState;
    cached.width = width;
    cached.height = height;
    cached.generation = generation;
    cached.withShadows = withShadows;
    cached.polygons = Object.freeze(polygons);
    this.cache.set(viewport, cached);
    return cached.polygons;
  }

  /**
   * @return a text identifying a list of objects by their identities
   */
  private identityKey(objects: readonly object[]): string {
    const ids: number[] = [];
    for (const o of objects) {
      let id: number | undefined = this.identities.get(o);
      if (id === undefined) {
        id = this.nextIdentity++;
        this.identities.set(o, id);
      }
      ids.push(id);
    }
    return ids.join(',');
  }

  /**
   * @param lights lights reaching the polygon
   * @return the flat shaded color of the face of a visible polygon, under the
   * given lights (without lights in the scene, under a light at the eye)
   */
  private shadeFace(
    polygon: HiddenSurfaceRenderer.VisiblePolygon,
    scene: Scene,
    camera: Camera,
    lights: ArrayList<Light> | null,
  ): ColorRgb {
    let material: SimpleMaterial | null = polygon.body.getMaterial();
    const normal: Vector3Dd = polygon.worldPlane.getNormal();
    const center: Vector3Dd = ProjectedSurfacesViewBuilder.faceCenter(polygon.face, polygon.body);
    let toEye: Vector3Dd;

    if (material === null) {
      material = scene.defaultMaterial();
    }
    if (camera.getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL) {
      toEye = camera.getFront().multiply(-1.0).normalized();
    } else {
      toEye = camera.getPosition().subtract(center).normalized();
    }
    if (polygon.lights === null && (lights === null || lights.isEmpty())) {
      // Without lights, a light at the eye
      const factor: number = Math.abs(normal.dotProduct(toEye));
      const diffuse: ColorRgb = material.getDiffuse();
      const noLight: ColorRgb = ProjectedSurfacesViewBuilder.NO_LIGHT_COLOR;
      return new ColorRgb(
        noLight.r() * diffuse.r() * factor,
        noLight.g() * diffuse.g() * factor,
        noLight.b() * diffuse.b() * factor,
      );
    }

    const hit: RayHit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
    hit.point = center;
    hit.normal = normal;
    const color: ColorRgb = this.shader
      .shadeLocal(
        hit,
        toEye.x(),
        toEye.y(),
        toEye.z(),
        lights!,
        new ArrayList<SimpleBody>(),
        material,
        this.workspace,
      )
      .color();
    const clamp = ProjectedSurfacesViewBuilder.clamp;
    return new ColorRgb(clamp(color.r()), clamp(color.g()), clamp(color.b()));
  }

  /**
   * @return the average of the vertices of a face, in world coordinates
   */
  private static faceCenter(face: _PolyhedralBoundedSolidFace, body: SimpleBody): Vector3Dd {
    let x = 0.0;
    let y = 0.0;
    let z = 0.0;
    let n = 0;

    for (let i = 0; i < face.boundariesList.size(); i++) {
      const loop = face.boundariesList.get(i);
      if (loop === null || loop.boundaryStartHalfEdge === null) {
        continue;
      }
      let he = loop.boundaryStartHalfEdge;
      const heStart = he;
      do {
        if (he === null || he.startingVertex === null) {
          break;
        }
        x += he.startingVertex.position.x();
        y += he.startingVertex.position.y();
        z += he.startingVertex.position.z();
        n++;
        he = he.next();
      } while (he !== heStart);
    }
    if (n === 0) {
      return body.getPosition();
    }
    return body.getTransformationMatrix().multiply(new Vector3Dd(x / n, y / n, z / n));
  }

  private static clamp(value: number): number {
    return Math.max(0.0, Math.min(1.0, value));
  }

  /**
   * A body whose geometry is exported to a `PolyhedralBoundedSolid` (i.e. a
   * `Box` or a `Sphere`) is replaced, for the hidden surface algorithm, by a
   * body with the same transformation and material, and the exported
   * boundary representation as geometry.
   */
  private static brepBody(body: SimpleBody, brep: PolyhedralBoundedSolid): SimpleBody {
    const geometry: Geometry | null = body.getGeometry();
    if (geometry === brep) {
      return body;
    }
    const surrogate: SimpleBody = new SimpleBody();
    surrogate.setName(body.getName());
    surrogate.setGeometry(brep);
    surrogate.setPosition(body.getPosition());
    surrogate.setRotation(body.getRotation());
    surrogate.setScale(body.getScale());
    surrogate.setMaterialReference(body.getMaterial());
    return surrogate;
  }
}
