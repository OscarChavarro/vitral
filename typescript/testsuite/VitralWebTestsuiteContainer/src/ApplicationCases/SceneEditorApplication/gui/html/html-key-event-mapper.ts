import { KeyEvent } from '@vitral/base';
import { WebSystem } from '@vitral/webgl';

/**
 * Port of `gui.awt.AwtKeyEventMapper`.
 *
 * Maps DOM key events to vitral ones for the interaction techniques of the
 * editor (i.e. the undo/redo chords of `gui/history`).
 *
 * `WebSystem` identifies the key of a vitral event by its character, as
 * `AwtSystem` does, but the character of a Ctrl chord is a control one
 * (Ctrl+Z gives 0x1A), so the key identity would be lost. This mapper
 * completes it from the physical key of the event: Ctrl+letter chords get the
 * letter in `KeyEvent.keycode` (`KEY_a`..`KEY_z`, or `KEY_A`..`KEY_Z` with
 * Shift), while the character stays the control one, so the chord is not
 * taken as the plain letter command.
 */
export class HtmlKeyEventMapper {
  private constructor() {}

  /**
   * @param domEvent DOM key event
   * @return the equivalent vitral key event
   */
  static toVitralEvent(domEvent: KeyboardEvent): KeyEvent {
    const event: KeyEvent = WebSystem.toVitralKeyEvent(domEvent);

    HtmlKeyEventMapper.completeControlLetterChord(event, domEvent);
    return event;
  }

  private static completeControlLetterChord(event: KeyEvent, domEvent: KeyboardEvent): void {
    // AWT's VK_A..VK_Z: the physical letter key
    if (!domEvent.ctrlKey || !/^Key[A-Z]$/.test(domEvent.code)) {
      return;
    }
    const shift: boolean = domEvent.shiftKey;
    event.keycode = (shift ? KeyEvent.KEY_A : KeyEvent.KEY_a) + (domEvent.code.charCodeAt(3) - 65);
  }
}
