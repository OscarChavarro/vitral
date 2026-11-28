import { RendererConfigurationController, type RendererConfiguration } from '@vitral/base';
import type { WebKeyEvent } from '@vitral/webgl';
import { toVitralKeyEvent } from './web-key-event-conversion';

/**
 * Feeds the examples' browser key events to Java's
 * `vsdk.toolkit.gui.RendererConfigurationController`, as the Java examples do
 * through `AwtSystem.awt2vsdkEvent`. The F1..F10 bindings and the
 * shading-type cycle are the ported controller's; this class only converts
 * the `WebKeyEvent` into a `KeyEvent`.
 */
export class ExampleRendererConfigurationInteraction {
  private readonly controller: RendererConfigurationController;

  constructor(qualitySelection: RendererConfiguration) {
    this.controller = new RendererConfigurationController(qualitySelection);
  }

  setRendererConfiguration(q: RendererConfiguration): void {
    this.controller.setRendererConfiguration(q);
  }

  processKeyPressedEvent(keyEvent: WebKeyEvent): boolean {
    return this.controller.processKeyPressedEvent(toVitralKeyEvent(keyEvent));
  }

  processKeyReleasedEvent(keyEvent: WebKeyEvent): boolean {
    return this.controller.processKeyReleasedEvent(toVitralKeyEvent(keyEvent));
  }
}
