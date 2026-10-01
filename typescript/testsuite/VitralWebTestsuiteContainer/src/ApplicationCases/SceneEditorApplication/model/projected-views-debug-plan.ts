//= References:                                                             =
//= [FUNK2003], Funkhouser, Thomas.  Min, Patrick. Kazhdan, Michael. Chen,  =
//=     Joyce. Halderman, Alex. Dobkin, David. Jacobs, David. "A Search     =
//=     Engine for 3D Models", ACM Transactions on Graphics, Vol 22. No1.   =
//=     January 2003. Pp. 83-105                                            =

import { Camera, JavaMath, Matrix4x4d, Vector3Dd } from '@vitral/base';

/**
 * Position, scale and orientation of the box presenting one projected view
 * (Java's nested class `ProjectedViewsDebugPlan.ViewPlacement`).
 */
export class ViewPlacement {
  private readonly position: Vector3Dd;
  private readonly scale: Vector3Dd;
  private readonly rotation: Matrix4x4d;

  constructor(position: Vector3Dd, scale: Vector3Dd, rotation: Matrix4x4d) {
    this.position = position;
    this.scale = scale;
    this.rotation = rotation;
  }

  getPosition(): Vector3Dd {
    return this.position;
  }

  getScale(): Vector3Dd {
    return this.scale;
  }

  getRotation(): Matrix4x4d {
    return this.rotation;
  }
}

/**
 * Port of `model.ProjectedViewsDebugPlan`.
 *
 * Placement of the 13 boxes (3 axis aligned faces and 10 diagonal ones) used to
 * present the projected views of a body for debugging, and of the cameras that
 * take those views. Views are numbered from 1 to `VIEW_COUNT`.
 */
export class ProjectedViewsDebugPlan {
  static readonly VIEW_COUNT = 13;

  private constructor() {}

  private static rotation(degrees: number, x: number, y: number, z: number): Matrix4x4d {
    return new Matrix4x4d().axisRotation(JavaMath.toRadians(degrees), new Vector3Dd(x, y, z));
  }

  private static composed(first: Matrix4x4d, second: Matrix4x4d): Matrix4x4d {
    return second.multiply(first);
  }

  private static diagonalPosition(x: number, y: number, z: number): Vector3Dd {
    return new Vector3Dd(x, y, z).normalized().multiply(1.5);
  }

  /**
   * @param view number of the view, from 1 to `VIEW_COUNT`
   * @return the placement of the box for that view, or null if the number is
   * out of range
   */
  static getPlacement(view: number): ViewPlacement | null {
    const unit: Vector3Dd = new Vector3Dd(1, 1, 1);
    const half: Vector3Dd = new Vector3Dd(0.5, 0.5, 0.5);
    const r = ProjectedViewsDebugPlan.rotation;
    const c = ProjectedViewsDebugPlan.composed;
    const d = ProjectedViewsDebugPlan.diagonalPosition;

    switch (view) {
      case 1:
        return new ViewPlacement(new Vector3Dd(0, -2, 0), unit, r(90, 1, 0, 0));
      case 2:
        return new ViewPlacement(new Vector3Dd(-2, 0, 0), unit, c(r(90, 0, 0, -1), r(90, 0, -1, 0)));
      case 3:
        return new ViewPlacement(new Vector3Dd(0, 0, -2), unit, r(180, 0, 1, 0));
      case 4:
        return new ViewPlacement(d(-1, -1, 1), half, c(r(45, 0, 0, -1), r(35, 1, -1, 0)));
      case 5:
        return new ViewPlacement(d(1, -1, 1), half, c(r(45, 0, 0, 1), r(35, 1, 1, 0)));
      case 6:
        return new ViewPlacement(d(1, 1, 1), half, c(r(135, 0, 0, 1), r(35, -1, 1, 0)));
      case 7:
        return new ViewPlacement(d(-1, 1, 1), half, c(r(135, 0, 0, -1), r(35, -1, -1, 0)));
      case 8:
        return new ViewPlacement(d(0, 1, -1), half, c(r(180, 0, 0, 1), r(135, -1, 0, 0)));
      case 9:
        return new ViewPlacement(d(-1, 0, -1), half, c(r(90, 0, 0, -1), r(135, 0, -1, 0)));
      case 10:
        return new ViewPlacement(d(0, -1, -1), half, r(135, 1, 0, 0));
      case 11:
        return new ViewPlacement(d(1, 0, -1), half, c(r(90, 0, 0, 1), r(135, 0, 1, 0)));
      case 12:
        return new ViewPlacement(d(1, -1, 0), half, c(r(90, 1, 0, 0), r(45, 0, 0, 1)));
      case 13:
        return new ViewPlacement(d(1, 1, 0), half, c(r(90, 1, 0, 0), r(135, 0, 0, 1)));
      default:
        return null;
    }
  }

  /**
   * Creates the camera that takes one projected view of a body normalized
   * inside the unit cube, as decribed in [FUNK2003].5, and figure
   * [FUNK2003].8 (orthogonal projection, 10 units away from the origin).
   * Views are:
   *   - 1   Front side view (from -Y axis)
   *   - 2   Lateral side view (from -X axis)
   *   - 3   Top side view (from -Z axis)
   *   - 4   Corner view from -X -Y Z direction
   *   - 5   Corner view from  X -Y Z direction
   *   - 6   Corner view from  X  Y Z direction
   *   - 7   Corner view from -X  Y Z direction
   *   - 8   Tilt view edge +Y on plane -Z
   *   - 9   Tilt view edge -X on plane -Z
   *   - 10  Tilt view edge -Y on plane -Z
   *   - 11  Tilt view edge +X on plane -Z
   *   - 12  Tilt view edge +X on plane -Y
   *   - 13  Tilt view edge +X on plane Y
   * @param view number of the view, from 1 to `VIEW_COUNT`
   * @return the camera for that view (at the origin, if the number is out of
   * range)
   */
  static createCamera(view: number): Camera {
    const camera: Camera = new Camera();
    camera.setFov(90);
    camera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
    camera.setNearPlaneDistance(2);
    camera.setFarPlaneDistance(20);

    let position: Vector3Dd = new Vector3Dd(0, 0, 0);
    let R: Matrix4x4d = new Matrix4x4d();

    const down: Vector3Dd = new Vector3Dd(0, 0, -1);
    let cornerReference: Vector3Dd = new Vector3Dd(10, 10, -10);
    cornerReference = cornerReference.normalized();
    const cornerAngle: number = Math.PI / 2 - Math.acos(down.dotProduct(cornerReference));
    const toRadians = JavaMath.toRadians;
    switch (view) {
      case 1:
        position = new Vector3Dd(0, -10, 0);
        R = R.eulerAnglesRotation(toRadians(90), 0, 0);
        break;
      case 2:
        position = new Vector3Dd(-10, 0, 0);
        break;
      case 3:
        position = new Vector3Dd(0, 0, -10);
        R = R.eulerAnglesRotation(toRadians(-90), toRadians(90), 0);
        break;
      case 4:
        position = new Vector3Dd(-10, -10, 10).normalized().multiply(10);
        R = R.eulerAnglesRotation(toRadians(45), -cornerAngle, 0);
        break;
      case 5:
        position = new Vector3Dd(10, -10, 10).normalized().multiply(10);
        R = R.eulerAnglesRotation(toRadians(135), -cornerAngle, 0);
        break;
      case 6:
        position = new Vector3Dd(10, 10, 10).normalized().multiply(10);
        R = R.eulerAnglesRotation(toRadians(-135), -cornerAngle, 0);
        break;
      case 7:
        position = new Vector3Dd(-10, 10, 10).normalized().multiply(10);
        R = R.eulerAnglesRotation(toRadians(-45), -cornerAngle, 0);
        break;
      case 8:
        position = new Vector3Dd(0, 10, -10).normalized().multiply(10);
        R = R.eulerAnglesRotation(toRadians(-90), toRadians(45), 0);
        break;
      case 9:
        position = new Vector3Dd(-10, 0, -10).normalized().multiply(10);
        R = R.eulerAnglesRotation(toRadians(0), toRadians(45), 0);
        break;
      case 10:
        position = new Vector3Dd(0, -10, -10).normalized().multiply(10);
        R = R.eulerAnglesRotation(toRadians(90), toRadians(45), 0);
        break;
      case 11:
        position = new Vector3Dd(10, 0, -10).normalized().multiply(10);
        R = R.eulerAnglesRotation(toRadians(180), toRadians(45), 0);
        break;
      case 12:
        position = new Vector3Dd(10, -10, 0).normalized().multiply(10);
        R = R.eulerAnglesRotation(toRadians(135), 0, 0);
        break;
      case 13:
        position = new Vector3Dd(10, 10, 0).normalized().multiply(10);
        R = R.eulerAnglesRotation(toRadians(180 + 45), 0, 0);
        break;
      default:
        break;
    }
    camera.setPosition(position);
    camera.setRotation(R);
    return camera;
  }
}
