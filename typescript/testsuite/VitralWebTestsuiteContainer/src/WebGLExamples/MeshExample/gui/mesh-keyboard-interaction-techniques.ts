import { KeyEvent, RayGizmoInteractionTechniques } from '@vitral/base';
import type { WebKeyEvent } from '@vitral/webgl';
import type { ExampleCameraInteraction } from '../../_shared/example-camera-interaction';
import type { ExampleRendererConfigurationInteraction } from '../../_shared/example-renderer-configuration-interaction';
import type { MeshModel } from '../model/mesh-model';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/MeshExample/src/gui/MeshKeyboardInteractionTechniques.java`.
 *
 * Java answers `KEY_ESC` with `System.exit(0)`. A module inside the testsuite
 * container cannot end the process, so the escape key returns the container to
 * its explorer, which is what the other ported examples do; the component owns
 * that branch, and this class keeps the rest.
 *
 * The ray gizmo techniques of the library work over vitral `KeyEvent`s (as
 * Java's do), while the rest of the bindings of the ported examples read the
 * normalized `WebKeyEvent`; the component hands both views of the key press.
 */
export class MeshKeyboardInteractionTechniques {
  private readonly rayGizmoTechniques: RayGizmoInteractionTechniques;

  constructor(
    private readonly model: MeshModel,
    private readonly cameraController: ExampleCameraInteraction,
    private readonly qualityController: ExampleRendererConfigurationInteraction,
  ) {
    this.rayGizmoTechniques = new RayGizmoInteractionTechniques(
      model.getRayGizmo(),
      KeyEvent.KEY_r,
      KeyEvent.KEY_R,
    );
  }

  processKeyPressedEvent(event: WebKeyEvent, keyEvent: KeyEvent): boolean {
    if (event.keycode === 'KEY_I') {
      console.log(this.model.getQualitySelection().toString());
      return true;
    }
    if (this.rayGizmoTechniques.processKeyPressedEvent(keyEvent)) {
      return true;
    }
    if (this.cameraController.processKeyPressed(event)) {
      return true;
    }
    if (this.qualityController.processKeyPressedEvent(event)) {
      console.log(this.model.getQualitySelection().toString());
      return true;
    }

    return false;
  }

  processKeyReleasedEvent(event: WebKeyEvent): boolean {
    if (this.qualityController.processKeyReleasedEvent(event)) {
      return true;
    }
    return false;
  }
}
