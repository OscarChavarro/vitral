import { RendererConfiguration } from '@vitral/base';
import type { WebKeyEvent } from '@vitral/webgl';

/**
 * Keyboard handling of the renderer quality flags, shared by the WebGL example
 * modules.
 *
 * This is `vsdk.toolkit.gui.RendererConfigurationController`: the F1..F9
 * bindings, the shading-type cycle and the `processKeyPressedEvent` /
 * `processKeyReleasedEvent` contract are the Java ones, unchanged.
 *
 * It lives beside `ExampleCameraInteraction` rather than in `@vitral/base` for
 * the same reason: the Java class extends `vsdk.toolkit.gui.Controller` and
 * takes a `vsdk.toolkit.gui.KeyEvent`, and that family lives in `61_gui.txt`,
 * the source group of the plan's decision-gated Phase 38, which is not ported
 * yet. Until it is, the event that reaches this adapter is the browser
 * `WebKeyEvent`, whose `keycode` carries the same `KEY_F1`..`KEY_F9` names the
 * Java constants have, so adopting the ported controller later is a change of
 * construction, not of the example modules.
 */
export class ExampleRendererConfigurationInteraction {
  constructor(private qualitySelection: RendererConfiguration) {}

  setRendererConfiguration(q: RendererConfiguration): void {
    this.qualitySelection = q;
  }

  processKeyPressedEvent(keyEvent: WebKeyEvent): boolean {
    let updated = false;
    let st: number;

    switch (keyEvent.keycode) {
      case 'KEY_F1':
        this.qualitySelection.changePoints();
        updated = true;
        break;
      case 'KEY_F2':
        this.qualitySelection.changeWires();
        updated = true;
        break;
      case 'KEY_F3':
        this.qualitySelection.changeSurfaces();
        updated = true;
        break;
      case 'KEY_F4':
        this.qualitySelection.changeBoundingVolume();
        updated = true;
        break;
      case 'KEY_F5':
        this.qualitySelection.changeNormals();
        updated = true;
        break;
      case 'KEY_F6':
        this.qualitySelection.changeTrianglesNormals();
        updated = true;
        break;
      case 'KEY_F7':
        st = this.qualitySelection.getShadingType();
        if (st === RendererConfiguration.SHADING_TYPE_NOLIGHT) {
          st = RendererConfiguration.SHADING_TYPE_FLAT;
        } else if (st === RendererConfiguration.SHADING_TYPE_FLAT) {
          st = RendererConfiguration.SHADING_TYPE_GOURAUD;
        } else if (st === RendererConfiguration.SHADING_TYPE_GOURAUD) {
          st = RendererConfiguration.SHADING_TYPE_PHONG;
        } else if (st === RendererConfiguration.SHADING_TYPE_PHONG) {
          st = RendererConfiguration.SHADING_TYPE_COOK_TERRANCE;
        } else {
          st = RendererConfiguration.SHADING_TYPE_NOLIGHT;
        }
        this.qualitySelection.setShadingType(st);
        updated = true;
        break;
      case 'KEY_F8':
        this.qualitySelection.changeTexture();
        updated = true;
        break;
      case 'KEY_F9':
        this.qualitySelection.changeBumpMap();
        updated = true;
        break;
    }
    return updated;
  }

  processKeyReleasedEvent(_keyEvent: WebKeyEvent): boolean {
    return false;
  }
}
