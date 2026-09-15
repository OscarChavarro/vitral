import {
  Camera,
  JavaMath,
  VSDK,
  Vector3Dd,
  type InfinitePlane,
  type PolyhedralBoundedSolid,
  type _PolyhedralBoundedSolidFace,
  type _PolyhedralBoundedSolidHalfEdge,
  type _PolyhedralBoundedSolidLoop,
  type _PolyhedralBoundedSolidVertex,
} from '@vitral/base';
import type { DebuggerModel } from '../models/debugger-model';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/PolyhedralBoundedSolidExample/src/gui/CameraFaceFocusInteraction.java`.
 *
 * `[c]` frames the selected face: the camera is placed on the side of the
 * face's plane it already looks from, at the distance that fits every vertex
 * of the face in both fields of view with a five percent margin, looking at
 * the centroid of the face's distinct vertices, with the up direction kept as
 * close to the current one as the new view allows. Every step is Java's.
 *
 * Java finally hands the centroid to the orbiter as its point of interest.
 * The container's stand-in for `CameraControllerOrbiter` keeps its orbit
 * centre as the camera's focused position, which `setFocusedPositionDirect`
 * has just set to that centroid, so `adoptCameraState()` is the same step.
 */
export class CameraFaceFocusInteraction {
  private static readonly SAFETY_MARGIN = 1.05;

  focusSelectedFace(model: DebuggerModel): boolean {
    const solid: PolyhedralBoundedSolid | null = model.getSolid();
    const faceIndex: number = model.getFaceIndex();
    const camera: Camera | null = model.getCamera();

    if (solid === null || solid.getPolygonsList() === null || camera === null) {
      return false;
    }
    if (faceIndex < 0 || faceIndex >= solid.getPolygonsList().size()) {
      return false;
    }

    const face: _PolyhedralBoundedSolidFace = solid.getPolygonsList().get(faceIndex)!;
    const faceVertices: _PolyhedralBoundedSolidVertex[] =
      CameraFaceFocusInteraction.collectFaceVertices(face);
    if (faceVertices.length === 0) {
      return false;
    }

    const center: Vector3Dd = CameraFaceFocusInteraction.computeCentroid(faceVertices);
    const plane: InfinitePlane | null = face.getContainingPlane();
    const planeNormal: Vector3Dd | null = plane !== null ? plane.getNormal() : null;
    if (planeNormal === null || planeNormal.length() < VSDK.EPSILON) {
      return false;
    }

    const front: Vector3Dd = CameraFaceFocusInteraction.chooseFacingFront(
      planeNormal,
      center,
      camera.getPosition(),
    );
    const up: Vector3Dd = CameraFaceFocusInteraction.chooseUpVector(camera.getUp(), front);
    const left: Vector3Dd = up.crossProduct(front).normalized();

    let distance: number =
      CameraFaceFocusInteraction.computeFramingDistance(
        camera,
        center,
        front,
        left,
        up,
        faceVertices,
      ) * CameraFaceFocusInteraction.SAFETY_MARGIN;
    if (distance < VSDK.EPSILON) {
      distance = Math.max(camera.getNearPlaneDistance() * 2.0, 1.0);
    }

    const eye: Vector3Dd = center.subtract(front.multiply(distance));
    camera.setPosition(eye);
    camera.setUpDirect(up);
    camera.setLeftDirect(left);
    camera.setFocusedPositionDirect(center);

    model.getCameraController().adoptCameraState();
    return true;
  }

  private static collectFaceVertices(
    face: _PolyhedralBoundedSolidFace,
  ): _PolyhedralBoundedSolidVertex[] {
    const vertices: _PolyhedralBoundedSolidVertex[] = [];
    // Java's LinkedHashSet<Integer>: only membership is observed.
    const visitedIds = new Set<number>();

    for (let i = 0; i < face.boundariesList.size(); i++) {
      const loop: _PolyhedralBoundedSolidLoop | null = face.boundariesList.get(i);
      if (loop === null || loop.boundaryStartHalfEdge === null) {
        continue;
      }
      const start: _PolyhedralBoundedSolidHalfEdge = loop.boundaryStartHalfEdge;
      let he: _PolyhedralBoundedSolidHalfEdge = start;
      do {
        const v: _PolyhedralBoundedSolidVertex | null = he.startingVertex;
        if (v !== null && v.position !== null && !visitedIds.has(v.id)) {
          visitedIds.add(v.id);
          vertices.push(v);
        }
        he = he.next()!;
      } while (he !== start);
    }
    return vertices;
  }

  private static computeCentroid(vertices: _PolyhedralBoundedSolidVertex[]): Vector3Dd {
    let sx = 0.0;
    let sy = 0.0;
    let sz = 0.0;

    for (let i = 0; i < vertices.length; i++) {
      const p: Vector3Dd = vertices[i]!.position;
      sx += p.x();
      sy += p.y();
      sz += p.z();
    }
    const invN: number = 1.0 / vertices.length;
    return new Vector3Dd(sx * invN, sy * invN, sz * invN);
  }

  private static chooseFacingFront(
    normal: Vector3Dd,
    center: Vector3Dd,
    cameraPosition: Vector3Dd,
  ): Vector3Dd {
    const toCamera: Vector3Dd = cameraPosition.subtract(center);
    if (normal.dotProduct(toCamera) >= 0.0) {
      return normal.multiply(-1).normalized();
    }
    return normal.normalized();
  }

  private static chooseUpVector(upHint: Vector3Dd, front: Vector3Dd): Vector3Dd {
    let projected: Vector3Dd = upHint.subtract(front.multiply(upHint.dotProduct(front)));
    if (projected.length() > VSDK.EPSILON) {
      return projected.normalized();
    }

    const alt1 = new Vector3Dd(0, 0, 1);
    projected = alt1.subtract(front.multiply(alt1.dotProduct(front)));
    if (projected.length() > VSDK.EPSILON) {
      return projected.normalized();
    }

    const alt2 = new Vector3Dd(0, 1, 0);
    projected = alt2.subtract(front.multiply(alt2.dotProduct(front)));
    return projected.normalized();
  }

  private static computeFramingDistance(
    camera: Camera,
    center: Vector3Dd,
    front: Vector3Dd,
    left: Vector3Dd,
    up: Vector3Dd,
    vertices: _PolyhedralBoundedSolidVertex[],
  ): number {
    if (camera.getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL) {
      return Math.max(
        camera.getPosition().subtract(center).length(),
        camera.getNearPlaneDistance() * 2.0,
      );
    }

    const viewportY: number = Math.max(camera.getViewportYSize(), 1e-9);
    const aspect: number = camera.getViewportXSize() / viewportY;
    const halfVerticalFov: number = JavaMath.toRadians(camera.getFov() * 0.5);
    const tanVertical: number = Math.tan(halfVerticalFov);
    const halfHorizontalFov: number = Math.atan(Math.max(aspect, 1e-9) * tanVertical);
    const tanHorizontal: number = Math.tan(halfHorizontalFov);

    const right: Vector3Dd = left.multiply(-1);
    let requiredDistance = 0.0;

    for (let i = 0; i < vertices.length; i++) {
      const rel: Vector3Dd = vertices[i]!.position.subtract(center);
      const x: number = rel.dotProduct(right);
      const y: number = rel.dotProduct(up);
      const z: number = rel.dotProduct(front);

      const byHorizontal: number = Math.abs(x) / Math.max(tanHorizontal, 1e-9) - z;
      const byVertical: number = Math.abs(y) / Math.max(tanVertical, 1e-9) - z;
      const byNear: number = camera.getNearPlaneDistance() - z + 1e-6;

      requiredDistance = Math.max(requiredDistance, byHorizontal);
      requiredDistance = Math.max(requiredDistance, byVertical);
      requiredDistance = Math.max(requiredDistance, byNear);
    }

    return Math.max(requiredDistance, camera.getNearPlaneDistance() * 2.0);
  }
}
