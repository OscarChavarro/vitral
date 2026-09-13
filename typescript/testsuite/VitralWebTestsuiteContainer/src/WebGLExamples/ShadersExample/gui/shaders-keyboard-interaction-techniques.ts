import { RendererConfiguration, type Light, type Vector3Dd } from '@vitral/base';
import type { WebKeyEvent } from '@vitral/webgl';
import type { ExampleCameraInteraction } from '../../_shared/example-camera-interaction';
import type { ExampleRendererConfigurationInteraction } from '../../_shared/example-renderer-configuration-interaction';
import type { ShadersModel } from '../model/shaders-model';

/**
 * The actions `ShadersKeyboardInteractionTechniques.Actions` declares. Java's
 * `requestExit` closes the window and calls `System.exit(0)`; a module inside
 * the container returns to the explorer instead, which is what the component's
 * `deactivate` output does.
 */
export interface ShadersKeyboardActions {
  requestExit(): void;
  animationStateChanged(): void;
}

/**
 * Port of
 * `java/testsuite/Jogl4Examples/ShadersExample/src/gui/ShadersKeyboardInteractionTechniques.java`.
 *
 * Every binding is the Java one, in the Java order: the escape key exits
 * before anything else is consulted; the camera controller and then the
 * quality controller each get the event and each may ask for a repaint; and
 * the switch that follows moves the light along the three axes with
 * `l`/`k`/`j`/`u`/`9`/`0`, selects the shading model with `g`/`p`/`n`, toggles
 * texture and bump mapping with `t`/`b`, walks the Cook-Torrance material list
 * with `m`, changes the meridian and parallel counts with `q`/`Q`/`w`/`W`,
 * turns the sphere and light animations on and off with `r` and the space bar,
 * shows or hides the HUD with `h`, and rotates the rendering mode with `.`.
 *
 * Java reads a `vsdk.toolkit.gui.KeyEvent` whose `keycode` distinguishes
 * `KEY_q` from `KEY_Q`; the browser event carries the same two names, so the
 * case-sensitive bindings — which is what makes `q` remove a meridian and `Q`
 * add one — survive unchanged. The controllers are the container's stand-ins
 * for `CameraControllerOrbiter` and `RendererConfigurationController`, whose
 * Phase 38 group is not ported yet.
 */
export class ShadersKeyboardInteractionTechniques {
  constructor(
    private readonly model: ShadersModel,
    private readonly cameraController: ExampleCameraInteraction,
    private readonly qualityController: ExampleRendererConfigurationInteraction,
  ) {}

  processPressed(event: WebKeyEvent, actions: ShadersKeyboardActions): boolean {
    if (event.keycode === 'KEY_ESC') {
      actions.requestExit();
      return false;
    }

    const light: Light = this.model.getLight();
    const quality: RendererConfiguration = this.model.getQuality();

    let repaint = false;

    if (this.cameraController.processKeyPressed(event)) {
      repaint = true;
    }

    if (this.qualityController.processKeyPressedEvent(event)) {
      repaint = true;
    }

    const lp: Vector3Dd = light.getPosition();
    switch (event.keycode) {
      case 'KEY_h':
      case 'KEY_H':
        this.model.toggleShowHud();
        repaint = true;
        break;
      case 'KEY_l':
      case 'KEY_L':
        light.setPosition(lp.withX(lp.x() - 0.1));
        repaint = true;
        break;
      case 'KEY_k':
      case 'KEY_K':
        light.setPosition(lp.withX(lp.x() + 0.1));
        repaint = true;
        break;
      case 'KEY_j':
      case 'KEY_J':
        light.setPosition(lp.withZ(lp.z() - 0.1));
        repaint = true;
        break;
      case 'KEY_u':
      case 'KEY_U':
        light.setPosition(lp.withZ(lp.z() + 0.1));
        repaint = true;
        break;
      case 'KEY_9':
        light.setPosition(lp.withY(lp.y() - 0.1));
        repaint = true;
        break;
      case 'KEY_0':
        light.setPosition(lp.withY(lp.y() + 0.1));
        repaint = true;
        break;
      case 'KEY_g':
      case 'KEY_G':
        quality.setShadingType(RendererConfiguration.SHADING_TYPE_GOURAUD);
        repaint = true;
        break;
      case 'KEY_p':
      case 'KEY_P':
        quality.setShadingType(RendererConfiguration.SHADING_TYPE_PHONG);
        repaint = true;
        break;
      case 'KEY_n':
      case 'KEY_N':
        quality.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
        repaint = true;
        break;
      case 'KEY_t':
      case 'KEY_T':
        quality.changeTexture();
        repaint = true;
        break;
      case 'KEY_b':
      case 'KEY_B':
        quality.changeBumpMap();
        repaint = true;
        break;
      case 'KEY_m':
      case 'KEY_M':
        this.model.cycleCookTorranceMaterial();
        repaint = true;
        break;
      case 'KEY_q':
        this.model.changeSphereMeridians(-1);
        repaint = true;
        break;
      case 'KEY_Q':
        this.model.changeSphereMeridians(1);
        repaint = true;
        break;
      case 'KEY_w':
        this.model.changeSphereParallels(-1);
        repaint = true;
        break;
      case 'KEY_W':
        this.model.changeSphereParallels(1);
        repaint = true;
        break;
      case 'KEY_r':
      case 'KEY_R':
        this.model.toggleAnimationEnabled();
        actions.animationStateChanged();
        repaint = true;
        break;
      case 'KEY_SPACE':
        this.model.toggleLightAnimationEnabled();
        actions.animationStateChanged();
        repaint = true;
        break;
      case 'KEY_PERIOD':
        this.model.rotateRenderingMode();
        repaint = true;
        break;
      default:
        break;
    }

    return repaint;
  }

  /**
   * Java forwards a key release to `CameraControllerOrbiter`, which uses it to
   * end a keyboard-driven camera move. The container's stand-in has no
   * key-release state to end — it moves the camera on the press alone — so
   * this asks the quality controller instead, which is the other half of what
   * Java's controllers answer and the one that is ported. It answers `false`,
   * as Java's does for every key that ends no move.
   */
  processReleased(event: WebKeyEvent): boolean {
    return this.qualityController.processKeyReleasedEvent(event);
  }
}
