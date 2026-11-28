import { KeyEvent } from '@vitral/base';
import type { WebKeyEvent } from '@vitral/webgl';

/**
 * The `vsdk.toolkit.gui.KeyEvent` a `WebKeyEvent` stands for. The examples'
 * keyboard techniques receive the `WebKeyEvent` form, whose key code is the
 * name of a `KeyEvent.KEY_*` constant and whose modifier mask uses the same
 * `MASK_*` bits as `KeyEvent`; the ported controllers take the `KeyEvent`.
 */
export function toVitralKeyEvent(webEvent: WebKeyEvent): KeyEvent {
  const keyEvent: KeyEvent = new KeyEvent();
  const code: unknown = (KeyEvent as unknown as Record<string, unknown>)[webEvent.keycode];
  keyEvent.keycode = typeof code === 'number' ? code : KeyEvent.KEY_NONE;
  keyEvent.unicodeId =
    webEvent.unicodeId.length > 0 ? webEvent.unicodeId.charCodeAt(0) : KeyEvent.KEY_NONE;
  keyEvent.modifierMask = webEvent.modifierMask;
  return keyEvent;
}
