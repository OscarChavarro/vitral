import type { WebKeyEvent } from '@vitral/webgl';
import type { ExampleCameraInteraction } from '../../_shared/example-camera-interaction';
import type { ExampleRendererConfigurationInteraction } from '../../_shared/example-renderer-configuration-interaction';
import type { SolidTextureModel } from '../model/solid-texture-model';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/SolidTextureExample/src/gui/SolidTextureKeyboardInteractionTechniques.java`.
 *
 * The program's own bindings live here: `[1]` rotates the operation mode,
 * `[2]` and `[3]` halve and double the solid-texture side, `[4]` and `[5]`
 * step through the texture list, `[r]` toggles the rotation animation, `[h]`
 * toggles the HUD, and `[I]` prints the renderer configuration. What remains
 * goes to the camera controller and then to the quality controller, in that
 * order, as in Java.
 *
 * Java answers `KEY_ESC` with `System.exit(0)`. A module inside the testsuite
 * container cannot end the process, so the escape key returns the container to
 * its explorer, which is what the other ported examples do; the component owns
 * that branch, and this class keeps the rest.
 */
export class SolidTextureKeyboardInteractionTechniques {
  constructor(
    private readonly model: SolidTextureModel,
    private readonly cameraController: ExampleCameraInteraction,
    private readonly qualityController: ExampleRendererConfigurationInteraction,
  ) {}

  processKeyPressedEvent(event: WebKeyEvent): boolean {
    if (event.keycode === 'KEY_1') {
      this.model.rotateOperationMode();
      return true;
    }
    if (event.keycode === 'KEY_2') {
      this.model.decreaseSolidTextureSize();
      return true;
    }
    if (event.keycode === 'KEY_3') {
      this.model.increaseSolidTextureSize();
      return true;
    }
    if (event.keycode === 'KEY_4') {
      this.model.selectPreviousSolidTexture();
      return true;
    }
    if (event.keycode === 'KEY_5') {
      this.model.selectNextSolidTexture();
      return true;
    }
    if (event.keycode === 'KEY_r' || event.keycode === 'KEY_R') {
      this.model.toggleAnimationEnabled();
      return true;
    }
    if (event.keycode === 'KEY_h' || event.keycode === 'KEY_H') {
      this.model.toggleHudVisible();
      return true;
    }
    if (event.keycode === 'KEY_I') {
      console.log(this.model.getQualitySelection().toString());
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
    return this.qualityController.processKeyReleasedEvent(event);
  }
}
