import { Vector3Dd, type Light } from '@vitral/base';
import type { WebKeyEvent } from '@vitral/webgl';
import type { ExampleCameraInteraction } from '../../_shared/example-camera-interaction';
import type { ExampleRendererConfigurationInteraction } from '../../_shared/example-renderer-configuration-interaction';
import type { DebuggerModel } from '../model/debugger-model';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/MD2Example/src/gui/KeyboardInteractionTechniques.java`.
 *
 * The program's own bindings: `[1]` and `[2]` step back and forward through
 * the mesh's animations, `[3]` and `[4]` cycle which object the XYZ keys move,
 * `[I]` prints the renderer configuration, and `x`/`X`/`y`/`Y`/`z`/`Z` move the
 * selected light when one is selected.
 *
 * Java answers `KEY_ESC` with `System.exit(0)`. A module inside the testsuite
 * container cannot end the process, so the escape key returns the container to
 * its explorer, which is what the other ported examples do; the component owns
 * that branch, and this class keeps the rest.
 *
 * Java's `Md2MeshExample.keyPressed` decides between this class, the camera
 * controller and the quality controller, and gives the camera controller the
 * event only when it is not a movement key aimed at a selected light. That
 * routing is kept here rather than in the component, so that the three
 * controllers stay in Java's order behind one call; the component then has the
 * same single `processKeyPressedEvent` the other example modules call.
 */
export class KeyboardInteractionTechniques {
  static readonly DELTA_MOVEMENT = 10.0;

  constructor(
    private readonly model: DebuggerModel,
    private readonly cameraController: ExampleCameraInteraction,
    private readonly qualityController: ExampleRendererConfigurationInteraction,
  ) {}

  processKeyPressedEvent(keyEvent: WebKeyEvent): boolean {
    let handled = false;
    const md2Mesh = this.model.getMd2Mesh();

    const animStartEnd: Int16Array = new Int16Array(2);
    if (keyEvent.keycode === 'KEY_1') {
      md2Mesh.returnStartEndAnim(md2Mesh.getCurrentAnimationInd(), animStartEnd);
      if (md2Mesh.getCurrentAnimationInd() === animStartEnd[0]) {
        md2Mesh.setCurrentAnimationInd(md2Mesh.getMaxAnimationInd());
      } else {
        md2Mesh.setCurrentAnimationInd(md2Mesh.getCurrentAnimationInd() - 1);
      }
      handled = true;
    } else if (keyEvent.keycode === 'KEY_2') {
      md2Mesh.setCurrentAnimationInd(md2Mesh.getCurrentAnimationInd() + 1);
      handled = true;
    } else if (keyEvent.keycode === 'KEY_3') {
      this.cycleSelectedObject(-1);
      handled = true;
    } else if (keyEvent.keycode === 'KEY_4') {
      this.cycleSelectedObject(1);
      handled = true;
    } else if (keyEvent.keycode === 'KEY_I') {
      console.log(this.model.getQualitySelection().toString());
      handled = true;
    } else if (
      KeyboardInteractionTechniques.isMovementKey(keyEvent) &&
      this.model.getSelectedObject() >= 0
    ) {
      this.moveSelectedLight(keyEvent);
      handled = true;
    }

    // Java's `keyPressed` continues into the camera controller unless the key
    // is a movement key aimed at a selected light, and then into the quality
    // controller, whatever the earlier handlers answered.
    const redirectToCamera = !(
      KeyboardInteractionTechniques.isMovementKey(keyEvent) && this.model.getSelectedObject() >= 0
    );
    if (redirectToCamera && this.cameraController.processKeyPressed(keyEvent)) {
      handled = true;
    }
    if (this.qualityController.processKeyPressedEvent(keyEvent)) {
      handled = true;
    }

    return handled;
  }

  processKeyReleasedEvent(keyEvent: WebKeyEvent): boolean {
    // Java's own `processKeyReleasedEvent` answers false, and `keyReleased`
    // then asks the camera and quality controllers.
    return this.qualityController.processKeyReleasedEvent(keyEvent);
  }

  static isMovementKey(keyEvent: WebKeyEvent | null): boolean {
    if (keyEvent === null) {
      return false;
    }
    return (
      keyEvent.keycode === 'KEY_x' ||
      keyEvent.keycode === 'KEY_X' ||
      keyEvent.keycode === 'KEY_y' ||
      keyEvent.keycode === 'KEY_Y' ||
      keyEvent.keycode === 'KEY_z' ||
      keyEvent.keycode === 'KEY_Z'
    );
  }

  private cycleSelectedObject(direction: number): void {
    const lightCount: number = this.model.getLights().length;
    const totalObjects: number = lightCount + 1; // camera + lights
    if (totalObjects <= 0) {
      this.model.setSelectedObject(-1);
      return;
    }

    const currentIndex: number = this.model.getSelectedObject() + 1;
    let next: number = (currentIndex + direction) % totalObjects;
    if (next < 0) {
      next += totalObjects;
    }
    this.model.setSelectedObject(next - 1);
  }

  private moveSelectedLight(keyEvent: WebKeyEvent): void {
    const lights: Light[] = this.model.getLights();
    const selectedObject: number = this.model.getSelectedObject();
    if (selectedObject < 0 || selectedObject >= lights.length) {
      return;
    }
    const selectedLight: Light = lights[selectedObject]!;

    let p: Vector3Dd = selectedLight.getPosition();
    switch (keyEvent.keycode) {
      case 'KEY_x':
        p = p.withX(p.x() - KeyboardInteractionTechniques.DELTA_MOVEMENT);
        break;
      case 'KEY_X':
        p = p.withX(p.x() + KeyboardInteractionTechniques.DELTA_MOVEMENT);
        break;
      case 'KEY_y':
        p = p.withY(p.y() - KeyboardInteractionTechniques.DELTA_MOVEMENT);
        break;
      case 'KEY_Y':
        p = p.withY(p.y() + KeyboardInteractionTechniques.DELTA_MOVEMENT);
        break;
      case 'KEY_z':
        p = p.withZ(p.z() - KeyboardInteractionTechniques.DELTA_MOVEMENT);
        break;
      case 'KEY_Z':
        p = p.withZ(p.z() + KeyboardInteractionTechniques.DELTA_MOVEMENT);
        break;
      default:
        return;
    }
    selectedLight.setPosition(p);
  }
}
