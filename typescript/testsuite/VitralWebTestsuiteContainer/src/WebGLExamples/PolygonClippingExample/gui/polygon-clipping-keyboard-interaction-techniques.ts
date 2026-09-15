import type { WebKeyEvent } from '@vitral/webgl';
import type { ExampleCameraInteraction } from '../../_shared/example-camera-interaction';
import type { ExampleRendererConfigurationInteraction } from '../../_shared/example-renderer-configuration-interaction';
import type { PolygonClippingDebuggerModel } from '../model/polygon-clipping-debugger-model';

/**
 * The actions `PolygonClippingKeyboardInteractionTechniques.Actions` declares.
 *
 * Java's `requestExit` closes the window and calls `System.exit(0)`; a module
 * inside the container returns to the explorer instead, which is what the
 * component's `deactivate` output does. `toggleFullscreen` swaps the `JFrame`
 * for an undecorated one on the default screen device; the component asks the
 * browser for its fullscreen element instead.
 */
export interface PolygonClippingKeyboardActions {
  requestExit(): void;
  rebuildScene(): void;
  toggleFullscreen(): void;
  requestSnapshot(): void;
}

/**
 * Port of
 * `java/testsuite/Jogl4Examples/PolygonClippingExample/src/gui/PolygonClippingKeyboardInteractionTechniques.java`.
 *
 * Every binding is the Java one, in the Java order and with the Java
 * bookkeeping: the escape key exits before the switch is entered and answers
 * `false`; the switch then steps the fixture with `[1]`/`[2]`, cycles the
 * boolean operation with `[3]`, shows or hides the reference frame with the
 * space bar, toggles the four polygons and the intersection points with
 * `C`/`S`/`I`/`O`/`P`, fills or unfills with `G`, takes a snapshot with `H`
 * and goes fullscreen with `F`; and `T` is the one case-sensitive binding,
 * lowercase toggling the fill as it did in earlier commits and uppercase
 * cycling the surface tessellation mode.
 *
 * `handledLetterShortcut` is what keeps those letters from also reaching the
 * camera controller, whose own bindings claim `S`, `P`, `F` and more; it is
 * kept exactly, which is why the camera sees the event only when no letter
 * shortcut consumed it while the quality controller always does.
 *
 * The controllers are the container's stand-ins for `CameraControllerOrbiter`
 * and `RendererConfigurationController`, whose Phase 38 group is not ported
 * yet; the event that reaches them carries the same `keycode` names the Java
 * `vsdk.toolkit.gui.KeyEvent` constants have, `KEY_t` and `KEY_T` included.
 */
export class PolygonClippingKeyboardInteractionTechniques {
  constructor(
    private readonly model: PolygonClippingDebuggerModel,
    private readonly cameraController: ExampleCameraInteraction,
    private readonly qualityController: ExampleRendererConfigurationInteraction,
  ) {}

  processPressed(event: WebKeyEvent, actions: PolygonClippingKeyboardActions): boolean {
    let repaint = false;
    let handled = false;
    let handledLetterShortcut = false;

    if (event.keycode === 'KEY_ESC') {
      actions.requestExit();
      return false;
    }

    switch (event.keycode) {
      case 'KEY_1':
        this.model.stepTest(-1);
        actions.rebuildScene();
        handled = true;
        break;
      case 'KEY_2':
        this.model.stepTest(1);
        actions.rebuildScene();
        handled = true;
        break;
      case 'KEY_3':
        this.model.cycleOperation();
        actions.rebuildScene();
        handled = true;
        break;
      case 'KEY_SPACE':
        this.model.setShowReferenceFrame(!this.model.isShowReferenceFrame());
        handled = true;
        break;
      case 'KEY_c':
      case 'KEY_C':
        this.model.setShowClipPolygon(!this.model.isShowClipPolygon());
        handled = true;
        handledLetterShortcut = true;
        break;
      case 'KEY_s':
      case 'KEY_S':
        this.model.setShowSubjectPolygon(!this.model.isShowSubjectPolygon());
        handled = true;
        handledLetterShortcut = true;
        break;
      case 'KEY_i':
      case 'KEY_I':
        this.model.setShowInnerPolygon(!this.model.isShowInnerPolygon());
        handled = true;
        handledLetterShortcut = true;
        break;
      case 'KEY_o':
      case 'KEY_O':
        this.model.setShowOuterPolygon(!this.model.isShowOuterPolygon());
        handled = true;
        handledLetterShortcut = true;
        break;
      case 'KEY_p':
      case 'KEY_P':
        this.model.setShowIntersections(!this.model.isShowIntersections());
        handled = true;
        handledLetterShortcut = true;
        break;
      case 'KEY_t':
        // Lowercase 't' keeps its original behaviour from prior commits:
        // toggling whether polygon surfaces are filled.
        this.model.setShowFilledPolygons(!this.model.isShowFilledPolygons());
        handled = true;
        handledLetterShortcut = true;
        break;
      case 'KEY_T':
        // Uppercase 'T' cycles the surface tessellation mode
        // (GLU vs monotone-decomposition triangulation).
        this.model.cyclePolygonSurfaceTessellationMode();
        handled = true;
        handledLetterShortcut = true;
        break;
      case 'KEY_g':
      case 'KEY_G':
        this.model.setShowFilledPolygons(!this.model.isShowFilledPolygons());
        handled = true;
        handledLetterShortcut = true;
        break;
      case 'KEY_h':
      case 'KEY_H':
        actions.requestSnapshot();
        handled = true;
        handledLetterShortcut = true;
        break;
      case 'KEY_f':
      case 'KEY_F':
        actions.toggleFullscreen();
        handled = true;
        handledLetterShortcut = true;
        break;
      default:
        break;
    }

    if (!handledLetterShortcut && this.cameraController.processKeyPressed(event)) {
      repaint = true;
    }
    if (this.qualityController.processKeyPressedEvent(event)) {
      repaint = true;
    }

    return repaint || handled;
  }

  /**
   * Java's `keyReleased` hands the event to the camera controller and then to
   * the quality controller, repainting if either asks for it. The camera
   * stand-in keeps no key-release state, so only the quality controller is
   * consulted here — the same call, on the same event.
   */
  processReleased(event: WebKeyEvent): boolean {
    return this.qualityController.processKeyReleasedEvent(event);
  }
}
