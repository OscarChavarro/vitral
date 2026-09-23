package gui.awt;

// AWT/Swing classes
import java.awt.event.InputEvent;

// VSDK classes
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.gui.KeyEvent;

/**
Maps AWT key events to vitral ones for the interaction techniques of the
editor (i.e. the undo/redo chords of `gui.history`).

`AwtSystem` identifies the key of a vitral event by its character, but the
character of a Ctrl chord is a control one (Ctrl+Z gives 0x1A), so the key
identity would be lost. This mapper completes it from the AWT key code:
Ctrl+letter chords get the letter in `KeyEvent.keycode` (`KEY_a`..`KEY_z`,
or `KEY_A`..`KEY_Z` with Shift), while the character stays the control one,
so the chord is not taken as the plain letter command.
*/
public final class AwtKeyEventMapper
{
    private AwtKeyEventMapper()
    {
    }

    /**
    @param awtEvent AWT key event
    @return the equivalent vitral key event
    */
    public static KeyEvent toVitralEvent(java.awt.event.KeyEvent awtEvent)
    {
        KeyEvent event = AwtSystem.awt2vsdkEvent(awtEvent);

        completeControlLetterChord(event, awtEvent);
        return event;
    }

    private static void completeControlLetterChord(KeyEvent event,
                                                   java.awt.event.KeyEvent awtEvent)
    {
        int modifiers = awtEvent.getModifiersEx();
        int keyCode = awtEvent.getKeyCode();
        boolean shift;

        if ( (modifiers & InputEvent.CTRL_DOWN_MASK) == 0 ||
             keyCode < java.awt.event.KeyEvent.VK_A ||
             keyCode > java.awt.event.KeyEvent.VK_Z ) {
            return;
        }
        shift = (modifiers & InputEvent.SHIFT_DOWN_MASK) != 0;
        event.keycode = (shift ? KeyEvent.KEY_A : KeyEvent.KEY_a) +
            (keyCode - java.awt.event.KeyEvent.VK_A);
    }
}
