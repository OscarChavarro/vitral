//= References:                                                             =
//= [APPE1967] Appel, Arthur. "The notion of quantitative invisibility and  =
//=          the machine rendering of solids". Proceedings, ACM National    =
//=          Meeting 1967.                                                  =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

import {
  ArrayList,
  Calligraphic3DBuffer,
  Camera,
  ColorRgb,
  GeometryCalligraphicExporter,
  GeometryTriangulator,
  HiddenLineRenderer,
  InfinitePlane,
  JavaMath,
  Matrix4x4d,
  SelectionCorners,
  SimpleBody,
  TriangleMesh,
  Vector3Dd,
  type Calligraphic2DBuffer,
  type Geometry,
  type Light,
  type PolyhedralBoundedSolid,
  type RendererConfiguration,
  type SimpleMaterial,
  type TriangleMeshGroup,
  type Viewport,
} from '@vitral/base';
import type { Scene } from '../model/scene';
import type { BodyEditFeedbackProvider } from './body-edit-feedback-provider';
import { BrepExportCache } from './brep-export-cache';
import { ClipSpaceLineProjector } from './clip-space-line-projector';
import { ProjectedLineLayer, ProjectedLineLayerStyle } from './projected-line-layer';

/**
 * Lines in world coordinates that are not edges of the solids (the reference
 * grid, the curves), drawn with one color: with hidden line removal they are
 * hidden by the solids, but do not hide anything (Java's
 * `ProjectedLinesViewBuilder.FreeLineSet`).
 */
class FreeLineSet {
  readonly lines: Calligraphic3DBuffer = new Calligraphic3DBuffer();
  readonly color: ColorRgb;

  constructor(color: ColorRgb) {
    this.color = color;
  }
}

/**
 * Port of `render.ProjectedLinesViewBuilder`.
 *
 * Builds the drawing of a viewport as projected 2D lines, computed entirely in
 * the processor: the "calligraphic" approach of vector displays and plotters
 * [FOLE1992].1, where a 3D scene is presented without any 3D graphics API. The
 * result is a list of `ProjectedLineLayer`s, so this class does not depend on
 * the 2D technology that finally draws them (see `render/html`).
 *
 * The bodies follow the `RendererConfiguration` of the viewport:
 *   - With surfaces, the bodies that are (or can be exported to) a
 *     `PolyhedralBoundedSolid` are drawn with hidden line removal, by the
 *     quantitative invisibility algorithm of [APPE1967] (`HiddenLineRenderer`):
 *     contour lines are thicker than the other visible lines, and hidden lines
 *     are dashed if wires are also asked for. The occlusion is computed among
 *     all those solids.
 *   - Curves and surfaces are exported to 3D lines
 *     (`GeometryCalligraphicExporter`: the surfaces through a mesh, whose
 *     edges are the lines). With surfaces they are "free lines" of the hidden
 *     line algorithm: they are hidden by the solids (dashed if wires are asked
 *     for), but do not hide anything, not even themselves: by design, the
 *     hidden line removal is limited to the solids, as in [APPE1967]. The
 *     reference grid is also drawn as free lines. An infinite plane is
 *     presented as a square of it, centered at the point of the plane viewed
 *     by the camera (as a hint of its position).
 *   - Without surfaces, every body is a plain wireframe with no hidden line
 *     removal (whether wires are asked for or not): the edges of its boundary
 *     representation, or its lines as above. It is the simplest and fastest
 *     presentation without a 3D graphics API.
 *   - Bodies with none of them are represented by their bounding box.
 *   - Points, bounding volumes and the selection corners of selected bodies
 *     are added over them.
 *
 * The light gizmos, the visual debug entities and the feedback of the body
 * editor are also presented as lines, over the bodies.
 *
 * Java's `List`s of layers are arrays here.
 */
export class ProjectedLinesViewBuilder {
  private static readonly GRID_COLOR = new ColorRgb(0.37, 0.37, 0.37);
  private static readonly GRID_AXIS_COLOR = new ColorRgb(0, 0, 0);
  private static readonly VISIBLE_LINE_COLOR = new ColorRgb(0, 0, 0);
  private static readonly HIDDEN_LINE_COLOR = new ColorRgb(0.32, 0.32, 0.32);
  private static readonly BOUNDING_VOLUME_COLOR = new ColorRgb(0.6, 0.9, 1.0);
  private static readonly POINT_COLOR = new ColorRgb(1.0, 0.0, 0.0);
  private static readonly LIGHT_COLOR = new ColorRgb(1.0, 1.0, 1.0);
  private static readonly SELECTED_LIGHT_COLOR = new ColorRgb(1.0, 1.0, 0.0);
  private static readonly DEBUG_COLOR = new ColorRgb(0.2, 0.8, 1.0);
  private static readonly EDIT_FEEDBACK_COLOR = new ColorRgb(1.0, 0.55, 0.0);
  private static readonly LIGHT_GIZMO_SIZE = 0.15;
  /// Distance an orthogonal camera is moved back for the Appel lines of sight
  private static readonly ORTHOGONAL_EYE_DISTANCE = 1000.0;

  private readonly brepCache: BrepExportCache = new BrepExportCache();

  /**
   * Builds the lines presenting the scene in a viewport.
   * @param scene scene to present
   * @param viewport viewport whose active camera, rendering configuration and
   * grid are used; its camera must have its viewport size updated
   * @param editor editor of the selected body, whose feedback is presented, or
   * null if there is none
   * @param withBodies true to present the bodies and the grid (hidden lines
   * render mode), false to present only the annotations that go over an image
   * of the scene computed by other means (raytracing and hidden surfaces
   * render modes)
   * @return the layers, in drawing order
   */
  buildView(
    scene: Scene,
    viewport: Viewport,
    editor: BodyEditFeedbackProvider | null,
    withBodies: boolean,
  ): ProjectedLineLayer[] {
    const layers: ProjectedLineLayer[] = [];
    const camera: Camera = viewport.getActiveCamera();
    const quality: RendererConfiguration = viewport.getRendererConfiguration();
    const projector: ClipSpaceLineProjector = new ClipSpaceLineProjector(camera);

    if (withBodies) {
      const freeLines: FreeLineSet[] = [];
      if (viewport.isShowGrid()) {
        this.addGrid(freeLines, camera);
      }
      this.addBodies(layers, freeLines, projector, scene, camera, quality);
    }
    this.addAnnotations(layers, projector, camera, scene, quality, editor);
    this.addDebugEntities(layers, projector, camera, scene);
    this.addLights(layers, projector, scene);
    return layers;
  }

  //= Grid ==============================================================

  /**
   * Same reference grid as the WebGL presentation: 14x14 unit cells on the
   * plane z = 0, or on the vertical plane facing the camera for orthogonal
   * side views. Its lines are free lines (the solids hide them).
   */
  private addGrid(freeLines: FreeLineSet[], camera: Camera): void {
    let gridTransform: Matrix4x4d = new Matrix4x4d();
    const rotation: Matrix4x4d = camera.getRotation();
    const yaw: number = JavaMath.toDegrees(rotation.obtainEulerYawAngle());
    const pitch: number = JavaMath.toDegrees(rotation.obtainEulerPitchAngle());

    if (
      camera.getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL &&
      pitch > -45 &&
      pitch < 45
    ) {
      if ((yaw > 45 && yaw < 135) || (yaw < -45 && yaw > -135)) {
        gridTransform = gridTransform.axisRotation(JavaMath.toRadians(90), 1, 0, 0);
      } else {
        gridTransform = gridTransform.axisRotation(JavaMath.toRadians(90), 0, 1, 0);
      }
    }

    const grid: FreeLineSet = new FreeLineSet(ProjectedLinesViewBuilder.GRID_COLOR);
    const axes: FreeLineSet = new FreeLineSet(ProjectedLinesViewBuilder.GRID_AXIS_COLOR);
    const n = 14;
    const half: number = n / 2.0;
    let i: number;

    for (i = 0; i <= n; i++) {
      const c: number = -half + i;
      const target: FreeLineSet = i === Math.trunc(n / 2) ? axes : grid;
      target.lines.add3DLine(
        gridTransform.multiply(new Vector3Dd(c, -half, 0)),
        gridTransform.multiply(new Vector3Dd(c, half, 0)),
      );
      target.lines.add3DLine(
        gridTransform.multiply(new Vector3Dd(-half, c, 0)),
        gridTransform.multiply(new Vector3Dd(half, c, 0)),
      );
    }
    freeLines.push(grid);
    freeLines.push(axes);
  }

  //= Bodies ============================================================

  /**
   * @param freeLines the free lines already collected (i.e. the grid), to
   * which the curves are added; they are drawn here, with the solids
   */
  private addBodies(
    layers: ProjectedLineLayer[],
    freeLines: FreeLineSet[],
    projector: ClipSpaceLineProjector,
    scene: Scene,
    camera: Camera,
    quality: RendererConfiguration,
  ): void {
    const bodies: ArrayList<SimpleBody> = scene.scene.getSimpleBodies();
    const solids: ArrayList<SimpleBody> = new ArrayList<SimpleBody>();
    const firstBodyLayer: number = layers.length;
    const points: ProjectedLineLayer = new ProjectedLineLayer(
      ProjectedLinesViewBuilder.POINT_COLOR,
      3.0,
      ProjectedLineLayerStyle.POINTS,
    );

    for (const body of bodies) {
      if (scene.isHidden(body)) {
        continue;
      }
      const geometry: Geometry | null = body.getGeometry();
      if (geometry === null) {
        continue;
      }
      const brep: PolyhedralBoundedSolid | null = this.exportToBrep(geometry);
      let lines: Calligraphic3DBuffer | null = null;
      if (quality.isSurfacesSet() && brep === null) {
        lines = ProjectedLinesViewBuilder.exportToLines(
          geometry,
          body.getTransformationMatrix(),
          camera,
        );
      }
      if (quality.isSurfacesSet() && brep !== null) {
        // Drawn below, all at once: they occlude each other
        solids.add(ProjectedLinesViewBuilder.brepBody(body, brep));
      } else if (lines !== null) {
        // Curves and surfaces: hidden by the solids, hide nothing
        ProjectedLinesViewBuilder.addFreeLines(freeLines, lines, body);
      } else {
        // Without surfaces (or without a boundary representation to
        // hide with), a plain wireframe, whatever the wires flag is
        const wires: ProjectedLineLayer = new ProjectedLineLayer(
          ProjectedLinesViewBuilder.wireColor(body.getMaterial()),
          1.0,
          ProjectedLineLayerStyle.SOLID,
        );
        this.addWireframe(wires, projector, camera, geometry, brep, body.getTransformationMatrix());
        ProjectedLinesViewBuilder.addIfNotEmpty(layers, wires);
      }
      if (quality.isPointsSet()) {
        this.addPoints(points, projector, geometry, brep, body.getTransformationMatrix());
      }
    }

    if (!solids.isEmpty()) {
      this.addHiddenLineDrawing(layers, solids, freeLines, camera, quality.isWiresSet());
    } else {
      ProjectedLinesViewBuilder.addFreeLinesWithoutHiding(
        layers,
        firstBodyLayer,
        freeLines,
        projector,
      );
    }
    ProjectedLinesViewBuilder.addIfNotEmpty(layers, points);
  }

  /**
   * The lines of a curve or a surface, in world coordinates, with the wire
   * color of its body.
   * @param local the lines, in the coordinates of the body
   */
  private static addFreeLines(
    freeLines: FreeLineSet[],
    local: Calligraphic3DBuffer,
    body: SimpleBody,
  ): void {
    const transform: Matrix4x4d = body.getTransformationMatrix();
    const set: FreeLineSet = new FreeLineSet(
      ProjectedLinesViewBuilder.wireColor(body.getMaterial()),
    );
    let i: number;

    for (i = 0; i < local.getNumLines(); i++) {
      const ends: Vector3Dd[] = local.get3DLine(i);
      set.lines.add3DLine(transform.multiply(ends[0]!), transform.multiply(ends[1]!));
    }
    freeLines.push(set);
  }

  /**
   * Without solids to hide them, the free lines are simply projected, below
   * the wireframes of the bodies.
   * @param position index in `layers` where their layers are inserted
   */
  private static addFreeLinesWithoutHiding(
    layers: ProjectedLineLayer[],
    position: number,
    freeLines: FreeLineSet[],
    projector: ClipSpaceLineProjector,
  ): void {
    for (const set of freeLines) {
      const layer: ProjectedLineLayer = new ProjectedLineLayer(
        set.color,
        1.0,
        ProjectedLineLayerStyle.SOLID,
      );
      let i: number;
      for (i = 0; i < set.lines.getNumLines(); i++) {
        const ends: Vector3Dd[] = set.lines.get3DLine(i);
        projector.addSegment(layer.getLines(), ends[0]!, ends[1]!);
      }
      if (!layer.isEmpty()) {
        layers.splice(position, 0, layer);
        position++;
      }
    }
  }

  /**
   * Presents a set of solids with the hidden line removal of [APPE1967],
   * and the free lines hidden by them. The hidden parts of all the lines are
   * drawn first (dashed, only if asked for), then the visible free lines and
   * finally the visible lines of the solids.
   */
  private addHiddenLineDrawing(
    layers: ProjectedLineLayer[],
    solids: ArrayList<SimpleBody>,
    freeLines: FreeLineSet[],
    camera: Camera,
    withHiddenLines: boolean,
  ): void {
    const contours: ProjectedLineLayer = new ProjectedLineLayer(
      ProjectedLinesViewBuilder.VISIBLE_LINE_COLOR,
      2.0,
      ProjectedLineLayerStyle.SOLID,
    );
    const visible: ProjectedLineLayer = new ProjectedLineLayer(
      ProjectedLinesViewBuilder.VISIBLE_LINE_COLOR,
      1.0,
      ProjectedLineLayerStyle.SOLID,
    );
    const hidden: ProjectedLineLayer = new ProjectedLineLayer(
      ProjectedLinesViewBuilder.HIDDEN_LINE_COLOR,
      1.0,
      ProjectedLineLayerStyle.DASHED,
    );

    const freeInput: ArrayList<Calligraphic3DBuffer> = new ArrayList<Calligraphic3DBuffer>();
    const freeVisibleOutput: ArrayList<Calligraphic2DBuffer> =
      new ArrayList<Calligraphic2DBuffer>();
    const freeHiddenOutput: ArrayList<Calligraphic2DBuffer> = new ArrayList<Calligraphic2DBuffer>();
    const freeVisible: ProjectedLineLayer[] = [];
    const freeHidden: ProjectedLineLayer[] = [];

    for (const set of freeLines) {
      const setVisible: ProjectedLineLayer = new ProjectedLineLayer(
        set.color,
        1.0,
        ProjectedLineLayerStyle.SOLID,
      );
      const setHidden: ProjectedLineLayer = new ProjectedLineLayer(
        set.color,
        1.0,
        ProjectedLineLayerStyle.DASHED,
      );
      freeInput.add(set.lines);
      freeVisibleOutput.add(setVisible.getLines());
      freeHiddenOutput.add(setHidden.getLines());
      freeVisible.push(setVisible);
      freeHidden.push(setHidden);
    }

    HiddenLineRenderer.executeAppelAlgorithm(
      solids,
      ProjectedLinesViewBuilder.lineOfSightCamera(camera),
      contours.getLines(),
      visible.getLines(),
      hidden.getLines(),
      freeInput,
      freeVisibleOutput,
      freeHiddenOutput,
    );

    if (withHiddenLines) {
      for (const layer of freeHidden) {
        ProjectedLinesViewBuilder.addIfNotEmpty(layers, layer);
      }
      ProjectedLinesViewBuilder.addIfNotEmpty(layers, hidden);
    }
    for (const layer of freeVisible) {
      ProjectedLinesViewBuilder.addIfNotEmpty(layers, layer);
    }
    ProjectedLinesViewBuilder.addIfNotEmpty(layers, visible);
    ProjectedLinesViewBuilder.addIfNotEmpty(layers, contours);
  }

  /**
   * The quantitative invisibility of [APPE1967] is measured along the line of
   * sight from the eye to each point. For an orthogonal camera all the lines
   * of sight are parallel, so the eye is moved far back along the viewing
   * direction (with its far plane, so the projection does not change).
   * @param camera camera of the view
   * @return the camera to give to the hidden line algorithm
   */
  private static lineOfSightCamera(camera: Camera): Camera {
    if (camera.getProjectionMode() !== Camera.PROJECTION_MODE_ORTHOGONAL) {
      return camera;
    }
    const d: number = ProjectedLinesViewBuilder.ORTHOGONAL_EYE_DISTANCE;
    const eye: Camera = new Camera(camera);
    const front: Vector3Dd = camera.getFront().normalized();
    eye.setPosition(camera.getPosition().subtract(front.multiply(d)));
    eye.setNearPlaneDistance(camera.getNearPlaneDistance() + d);
    eye.setFarPlaneDistance(camera.getFarPlaneDistance() + d);
    return eye;
  }

  /**
   * The quantitative invisibility is answered by the geometry of each body,
   * and only a `PolyhedralBoundedSolid` answers it: a body whose geometry is
   * exported to one (i.e. a `Box` or a `Sphere`) is replaced, for the hidden
   * line algorithm, by a body with the same transformation and the exported
   * boundary representation as geometry.
   * @param body body of the scene
   * @param brep boundary representation of its geometry
   * @return the body to give to the hidden line algorithm
   */
  private static brepBody(body: SimpleBody, brep: PolyhedralBoundedSolid): SimpleBody {
    if (body.getGeometry() === brep) {
      return body;
    }
    const surrogate: SimpleBody = new SimpleBody();
    surrogate.setName(body.getName());
    surrogate.setGeometry(brep);
    surrogate.setPosition(body.getPosition());
    surrogate.setRotation(body.getRotation());
    surrogate.setScale(body.getScale());
    return surrogate;
  }

  /**
   * @param geometry
   * @return the boundary representation of the geometry, or null if it is
   * not a volume that can be exported to one (see `BrepExportCache`)
   */
  private exportToBrep(geometry: Geometry): PolyhedralBoundedSolid | null {
    return this.brepCache.get(geometry);
  }

  /**
   * @param geometry a geometry
   * @param transform transformation from the coordinates of the geometry to
   * world coordinates
   * @param camera camera of the view
   * @return the lines of a curve or of the mesh of a surface, in the
   * coordinates of the geometry (see `GeometryCalligraphicExporter`), or null
   * if it has none
   */
  private static exportToLines(
    geometry: Geometry,
    transform: Matrix4x4d,
    camera: Camera,
  ): Calligraphic3DBuffer | null {
    if (geometry instanceof InfinitePlane) {
      const plane: InfinitePlane = geometry;
      return GeometryCalligraphicExporter.exportToCalligraphic3DBuffer(
        plane,
        ProjectedLinesViewBuilder.viewedPoint(plane, transform, camera),
      );
    }
    return GeometryCalligraphicExporter.exportToCalligraphic3DBuffer(geometry);
  }

  /**
   * The point of a plane viewed by a camera: where the line of sight through
   * the center of the view meets the plane, or, if it does not meet it in
   * front of the camera and before its far plane, the point of the plane
   * closest to the camera.
   * @param transform transformation from the coordinates of the plane to
   * world coordinates
   * @return the point, in the coordinates of the plane
   */
  private static viewedPoint(
    plane: InfinitePlane,
    transform: Matrix4x4d,
    camera: Camera,
  ): Vector3Dd {
    const inverse: Matrix4x4d = transform.inverse();
    const eye: Vector3Dd = inverse.multiply(camera.getPosition());
    const direction: Vector3Dd = inverse
      .multiply(camera.getPosition().add(camera.getFront()))
      .subtract(eye);
    const towards: number = plane.getCoefficients().dotProduct(direction);
    const length: number = direction.length();

    if (towards !== 0 && length > 0) {
      const t: number = -plane.pointDistance(eye) / towards;
      if (t > 0 && t * length <= camera.getFarPlaneDistance()) {
        return eye.add(direction.multiply(t));
      }
    }
    return plane.projectPoint(eye);
  }

  /**
   * Adds the wireframe of a geometry: the edges of its boundary
   * representation, else the lines of a curve or of the mesh of a surface
   * (`GeometryCalligraphicExporter`), else its bounding box.
   * @param camera camera of the view (to center the square shown of an
   * infinite plane)
   * @param brep boundary representation of the geometry, or null
   */
  private addWireframe(
    layer: ProjectedLineLayer,
    projector: ClipSpaceLineProjector,
    camera: Camera,
    geometry: Geometry,
    brep: PolyhedralBoundedSolid | null,
    transform: Matrix4x4d,
  ): void {
    if (brep !== null) {
      let i: number;
      for (i = 0; i < brep.getEdgesList().size(); i++) {
        const edge = brep.getEdgesList().get(i)!;
        if (
          edge.getStartingVertexId() < 0 ||
          edge.getEndingVertexId() < 0 ||
          edge.rightHalf === null ||
          edge.leftHalf === null
        ) {
          continue;
        }
        const p0: Vector3Dd | null = edge.rightHalf.startingVertex.position;
        const p1: Vector3Dd | null = edge.leftHalf.startingVertex.position;
        if (p0 !== null && p1 !== null) {
          projector.addSegment(layer.getLines(), transform, p0, p1);
        }
      }
      return;
    }

    const lines: Calligraphic3DBuffer | null = ProjectedLinesViewBuilder.exportToLines(
      geometry,
      transform,
      camera,
    );
    if (lines !== null) {
      let i: number;
      for (i = 0; i < lines.getNumLines(); i++) {
        const ends: Vector3Dd[] = lines.get3DLine(i);
        projector.addSegment(layer.getLines(), transform, ends[0]!, ends[1]!);
      }
      return;
    }

    this.addBox(layer, projector, geometry.getMinMax(), transform);
  }

  private addPoints(
    layer: ProjectedLineLayer,
    projector: ClipSpaceLineProjector,
    geometry: Geometry,
    brep: PolyhedralBoundedSolid | null,
    transform: Matrix4x4d,
  ): void {
    if (brep !== null) {
      let i: number;
      for (i = 0; i < brep.getVerticesList().size(); i++) {
        const vertex = brep.getVerticesList().get(i)!;
        if (vertex.position !== null) {
          projector.addPoint(layer.getLines(), transform.multiply(vertex.position));
        }
      }
      return;
    }

    const meshes: TriangleMeshGroup | null =
      GeometryTriangulator.exportToTriangleMeshGroup(geometry);
    if (meshes === null) {
      return;
    }
    for (const element of meshes.getMeshes()) {
      // Java's `TriangleMeshGroup.getMeshes` gives `TriangleMesh`es
      if (!(element instanceof TriangleMesh)) {
        continue;
      }
      const mesh: TriangleMesh = element;
      const v: Float64Array | null = mesh.getVertexPositions();
      let i: number;
      for (i = 0; v !== null && i < mesh.getNumVertices(); i++) {
        projector.addPoint(
          layer.getLines(),
          transform.multiply(new Vector3Dd(v[3 * i]!, v[3 * i + 1]!, v[3 * i + 2]!)),
        );
      }
    }
  }

  private static wireColor(material: SimpleMaterial | null): ColorRgb {
    if (material === null) {
      return ProjectedLinesViewBuilder.VISIBLE_LINE_COLOR;
    }
    return material.getDiffuse();
  }

  //= Annotations =======================================================

  private addAnnotations(
    layers: ProjectedLineLayer[],
    projector: ClipSpaceLineProjector,
    camera: Camera,
    scene: Scene,
    quality: RendererConfiguration,
    editor: BodyEditFeedbackProvider | null,
  ): void {
    const bodies: ArrayList<SimpleBody> = scene.scene.getSimpleBodies();
    const boxes: ProjectedLineLayer = new ProjectedLineLayer(
      ProjectedLinesViewBuilder.BOUNDING_VOLUME_COLOR,
      1.0,
      ProjectedLineLayerStyle.SOLID,
    );
    const corners: ProjectedLineLayer = new ProjectedLineLayer(
      SelectionCorners.getDefaultColor(),
      1.0,
      ProjectedLineLayerStyle.SOLID,
    );
    let i: number;

    scene.selectedThings.sync();
    for (i = 0; i < bodies.size(); i++) {
      const body: SimpleBody = bodies.get(i);
      if (scene.isHidden(body)) {
        continue;
      }
      const geometry: Geometry | null = body.getGeometry();
      if (geometry === null) {
        continue;
      }
      if (quality.isBoundingVolumeSet()) {
        this.addBox(boxes, projector, geometry.getMinMax(), body.getTransformationMatrix());
      }
      if (scene.selectedThings.isSelected(i)) {
        this.addSelectionCorners(
          corners,
          projector,
          geometry.getMinMax(),
          body.getTransformationMatrix(),
        );
      }
    }
    ProjectedLinesViewBuilder.addIfNotEmpty(layers, boxes);
    ProjectedLinesViewBuilder.addIfNotEmpty(layers, corners);

    if (editor !== null && editor.getTarget() !== null) {
      const feedback: ProjectedLineLayer = new ProjectedLineLayer(
        ProjectedLinesViewBuilder.EDIT_FEEDBACK_COLOR,
        1.0,
        ProjectedLineLayerStyle.SOLID,
      );
      for (const primitive of editor.buildEditFeedback()) {
        const geometry: Geometry | null = primitive.getGeometry();
        if (geometry !== null) {
          this.addWireframe(
            feedback,
            projector,
            camera,
            geometry,
            this.exportToBrep(geometry),
            primitive.getTransform(),
          );
        }
      }
      ProjectedLinesViewBuilder.addIfNotEmpty(layers, feedback);
    }
  }

  private addSelectionCorners(
    layer: ProjectedLineLayer,
    projector: ClipSpaceLineProjector,
    minmax: ArrayLike<number>,
    transform: Matrix4x4d,
  ): void {
    const segments: Vector3Dd[] = SelectionCorners.buildSegments(minmax);
    let i: number;

    for (i = 0; i + 1 < segments.length; i += 2) {
      projector.addSegment(layer.getLines(), transform, segments[i]!, segments[i + 1]!);
    }
  }

  /**
   * Adds the twelve edges of an axis aligned box.
   * @param minmax minimum x, y, z followed by maximum x, y, z
   */
  private addBox(
    layer: ProjectedLineLayer,
    projector: ClipSpaceLineProjector,
    minmax: ArrayLike<number> | null,
    transform: Matrix4x4d,
  ): void {
    if (minmax === null || minmax.length < 6) {
      return;
    }
    for (let k = 0; k < minmax.length; k++) {
      const limit: number = minmax[k]!;
      // i.e. an infinite plane
      if (!Number.isFinite(limit)) {
        return;
      }
    }
    const c: Vector3Dd[] = [];
    let i: number;

    for (i = 0; i < 8; i++) {
      c.push(
        new Vector3Dd(
          (i & 1) !== 0 ? minmax[3]! : minmax[0]!,
          (i & 2) !== 0 ? minmax[4]! : minmax[1]!,
          (i & 4) !== 0 ? minmax[5]! : minmax[2]!,
        ),
      );
    }
    for (i = 0; i < 8; i++) {
      let bit: number;
      for (bit = 1; bit < 8; bit <<= 1) {
        if ((i & bit) === 0) {
          projector.addSegment(layer.getLines(), transform, c[i]!, c[i | bit]!);
        }
      }
    }
  }

  //= Debug entities and lights =========================================

  private addDebugEntities(
    layers: ProjectedLineLayer[],
    projector: ClipSpaceLineProjector,
    camera: Camera,
    scene: Scene,
  ): void {
    const debug: ProjectedLineLayer = new ProjectedLineLayer(
      ProjectedLinesViewBuilder.DEBUG_COLOR,
      1.0,
      ProjectedLineLayerStyle.SOLID,
    );

    for (const group of scene.debugThingGroups) {
      const groupTransform: Matrix4x4d = group.getTransformationMatrix();
      for (const body of group.getBodies()) {
        const geometry: Geometry | null = body.getGeometry();
        if (geometry !== null) {
          this.addWireframe(
            debug,
            projector,
            camera,
            geometry,
            this.exportToBrep(geometry),
            groupTransform.multiply(body.getTransformationMatrix()),
          );
        }
      }
    }
    ProjectedLinesViewBuilder.addIfNotEmpty(layers, debug);
  }

  /**
   * Each light is presented as a small star of three axis aligned segments
   * and four diagonals, centered at its position.
   */
  private addLights(
    layers: ProjectedLineLayer[],
    projector: ClipSpaceLineProjector,
    scene: Scene,
  ): void {
    const lights: ArrayList<Light> = scene.scene.getLights();
    const normal: ProjectedLineLayer = new ProjectedLineLayer(
      ProjectedLinesViewBuilder.LIGHT_COLOR,
      1.0,
      ProjectedLineLayerStyle.SOLID,
    );
    const selected: ProjectedLineLayer = new ProjectedLineLayer(
      ProjectedLinesViewBuilder.SELECTED_LIGHT_COLOR,
      2.0,
      ProjectedLineLayerStyle.SOLID,
    );
    const s: number = ProjectedLinesViewBuilder.LIGHT_GIZMO_SIZE * scene.getLightGizmoScale();
    const d: number = s / Math.sqrt(3.0);
    const directions: Vector3Dd[] = [
      new Vector3Dd(s, 0, 0),
      new Vector3Dd(0, s, 0),
      new Vector3Dd(0, 0, s),
      new Vector3Dd(d, d, d),
      new Vector3Dd(-d, d, d),
      new Vector3Dd(d, -d, d),
      new Vector3Dd(d, d, -d),
    ];
    let i: number;

    scene.selectedLights.sync();
    for (i = 0; i < lights.size(); i++) {
      const center: Vector3Dd | null = lights.get(i).getPosition();
      const target: ProjectedLineLayer = scene.selectedLights.isSelected(i) ? selected : normal;
      if (center === null) {
        continue;
      }
      for (const direction of directions) {
        projector.addSegment(target.getLines(), center.subtract(direction), center.add(direction));
      }
    }
    ProjectedLinesViewBuilder.addIfNotEmpty(layers, normal);
    ProjectedLinesViewBuilder.addIfNotEmpty(layers, selected);
  }

  private static addIfNotEmpty(layers: ProjectedLineLayer[], layer: ProjectedLineLayer): void {
    if (!layer.isEmpty()) {
      layers.push(layer);
    }
  }
}
