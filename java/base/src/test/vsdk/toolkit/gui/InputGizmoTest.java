package vsdk.toolkit.gui;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.gui.gizmo.InputGizmo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
Exercises the state and the keyboard editing of an input gizmo, which only
depend on vitral key events.
 */
class InputGizmoTest
{
    private static final double EPS = 1.0e-9;

    private static InputGizmo createGizmo()
    {
        InputGizmo gizmo = new InputGizmo(3);

        gizmo.setValue(0, 1.5);
        gizmo.setValue(1, -2.25);
        gizmo.setValue(2, 0.0004);
        return gizmo;
    }

    private static KeyEvent character(char unicode)
    {
        KeyEvent event = new KeyEvent();

        event.unicodeId = unicode;
        return event;
    }

    private static KeyEvent key(int keycode)
    {
        KeyEvent event = new KeyEvent();

        event.keycode = keycode;
        return event;
    }

    private static void type(InputGizmo gizmo, String text)
    {
        for ( char c : text.toCharArray() ) {
            gizmo.processKeyPressedEvent(character(c));
        }
    }

    @Test
    void given_noEdition_when_displayed_then_showsValuesWithThreeDecimals()
    {
        // Arrange
        InputGizmo gizmo = createGizmo();

        // Act & Assert
        assertThat(gizmo.getDisplayText(0)).isEqualTo("1.500");
        assertThat(gizmo.getDisplayText(1)).isEqualTo("-2.250");
        assertThat(gizmo.getDisplayText(2)).isEqualTo("0.000");
        assertThat(InputGizmo.format(-0.0001)).isEqualTo("0.000");
    }

    @Test
    void given_twoDecimals_when_displayed_then_showsValuesWithTwoDecimals()
    {
        // Arrange
        InputGizmo gizmo = new InputGizmo(3, 2, 3);

        gizmo.setValue(0, 45.678);
        gizmo.setValue(1, -179.999);
        gizmo.setValue(2, -0.004);

        // Act & Assert
        assertThat(gizmo.getDecimals()).isEqualTo(2);
        assertThat(gizmo.getDisplayText(0)).isEqualTo("45.68");
        assertThat(gizmo.getDisplayText(1)).isEqualTo("-180.00");
        assertThat(gizmo.getDisplayText(2)).isEqualTo("0.00");
        assertThat(InputGizmo.format(-0.004, 2)).isEqualTo("0.00");
        assertThat(InputGizmo.format(-0.006, 2)).isEqualTo("-0.01");
    }

    @Test
    void given_gizmoWidth_when_referenceTextAsked_then_hasTheDigitsOfItsBoxes()
    {
        // Act & Assert
        assertThat(new InputGizmo(3).getReferenceText()).isEqualTo("-0.000");
        assertThat(new InputGizmo(3, 2, 3).getReferenceText()).isEqualTo("-000.00");
        assertThat(new InputGizmo(1, 0, 2).getReferenceText()).isEqualTo("-00");
    }

    @Test
    void given_twoDecimalsGizmo_when_typingMoreDecimals_then_areNotAccepted()
    {
        // Arrange
        InputGizmo gizmo = new InputGizmo(2, 2, 3);

        // Act
        type(gizmo, "12.3456");

        // Assert
        assertThat(gizmo.getEditText(0)).isEqualTo("12.34");
        assertThat(gizmo.getValuesWithEdits()[0]).isCloseTo(12.34, offset(EPS));
    }

    @Test
    void given_twoDecimalsGizmo_when_typingNegativeNumberAndInteger_then_areAccepted()
    {
        // Arrange
        InputGizmo gizmo = new InputGizmo(2, 2, 3);

        // Act
        type(gizmo, "-170");
        gizmo.selectNextField();
        type(gizmo, ".5");

        // Assert
        assertThat(gizmo.getEditText(0)).isEqualTo("-170");
        assertThat(gizmo.getEditText(1)).isEqualTo("0.5");
    }

    @Test
    void given_noDecimalsGizmo_when_typingPoint_then_isNotAccepted()
    {
        // Arrange
        InputGizmo gizmo = new InputGizmo(1, 0, 3);

        // Act
        type(gizmo, "12.5");

        // Assert
        assertThat(gizmo.getEditText(0)).isEqualTo("125");
    }

    @Test
    void given_defaultGizmo_when_typingManyDecimals_then_keepsAcceptingThem()
    {
        // Arrange
        InputGizmo gizmo = new InputGizmo(3);

        // Act
        type(gizmo, "1.23456");

        // Assert
        assertThat(gizmo.getEditText(0)).isEqualTo("1.23456");
    }

    @Test
    void given_highlight_when_colorAsked_then_highlightedFieldsAreYellow()
    {
        // Arrange
        InputGizmo gizmo = createGizmo();
        ColorRgb red = new ColorRgb(1, 0, 0);

        gizmo.setFieldColor(0, red);

        // Act & Assert
        assertThat(gizmo.getFieldDisplayColor(0)).isEqualTo(red);
        gizmo.setFieldHighlighted(0, true);
        assertThat(gizmo.getFieldDisplayColor(0)).isEqualTo(InputGizmo.HIGHLIGHT_COLOR);
    }

    @Test
    void given_tabKey_when_pressed_then_selectionCyclesForwardAndBackward()
    {
        // Arrange
        InputGizmo gizmo = createGizmo();
        KeyEvent shiftTab = key(KeyEvent.KEY_TAB);

        shiftTab.modifierMask = KeyEvent.MASK_SHIFT;

        // Act & Assert
        assertThat(gizmo.getSelectedField()).isEqualTo(0);
        gizmo.processKeyPressedEvent(key(KeyEvent.KEY_TAB));
        assertThat(gizmo.getSelectedField()).isEqualTo(1);
        gizmo.processKeyPressedEvent(key(KeyEvent.KEY_TAB));
        gizmo.processKeyPressedEvent(key(KeyEvent.KEY_TAB));
        assertThat(gizmo.getSelectedField()).isEqualTo(0);
        gizmo.processKeyPressedEvent(shiftTab);
        assertThat(gizmo.getSelectedField()).isEqualTo(2);
    }

    @Test
    void given_digitsOverUneditedField_when_typed_then_replaceItsValue()
    {
        // Arrange
        InputGizmo gizmo = createGizmo();

        // Act
        type(gizmo, "12.5");

        // Assert
        assertThat(gizmo.isEditing(0)).isTrue();
        assertThat(gizmo.getDisplayText(0)).isEqualTo("12.5");
        assertThat(gizmo.getDisplayText(1)).isEqualTo("-2.250");
    }

    @Test
    void given_numericKeypadKeys_when_pressed_then_areTypedToo()
    {
        // Arrange
        InputGizmo gizmo = createGizmo();

        // Act: numeric keypad events only have a key code
        gizmo.processKeyPressedEvent(key(KeyEvent.KEY_NUM7));
        gizmo.processKeyPressedEvent(key(KeyEvent.KEY_NUMPERIOD));
        gizmo.processKeyPressedEvent(character(','));
        gizmo.processKeyPressedEvent(key(KeyEvent.KEY_NUM2));

        // Assert: the second point is ignored
        assertThat(gizmo.getEditText(0)).isEqualTo("7.2");
    }

    @Test
    void given_pointFirst_when_typed_then_startsWithZero()
    {
        // Arrange
        InputGizmo gizmo = createGizmo();

        // Act
        type(gizmo, ".5");

        // Assert
        assertThat(gizmo.getEditText(0)).isEqualTo("0.5");
    }

    @Test
    void given_minusFirst_when_typed_then_startsANegativeNumber()
    {
        // Arrange
        InputGizmo gizmo = createGizmo();

        // Act
        type(gizmo, "-3.5");

        // Assert
        assertThat(gizmo.getEditText(0)).isEqualTo("-3.5");
        assertThat(gizmo.getValuesWithEdits()[0]).isCloseTo(-3.5, offset(EPS));
    }

    @Test
    void given_minusKeyCodes_when_pressed_then_areConsumedAndStartNegativeNumbers()
    {
        // Arrange
        InputGizmo gizmo = createGizmo();

        // Act & Assert: `-` is consumed even when nothing is being edited
        assertThat(gizmo.consumesKey(key(KeyEvent.KEY_MINUS))).isTrue();
        gizmo.processKeyPressedEvent(key(KeyEvent.KEY_MINUS));
        gizmo.processKeyPressedEvent(key(KeyEvent.KEY_NUM4));
        assertThat(gizmo.getEditText(0)).isEqualTo("-4");
    }

    @Test
    void given_minusKey_when_pressedAgain_then_changesTheSign()
    {
        // Arrange
        InputGizmo gizmo = createGizmo();

        type(gizmo, "-3");

        // Act
        type(gizmo, "-");

        // Assert
        assertThat(gizmo.getEditText(0)).isEqualTo("3");
        type(gizmo, "-");
        assertThat(gizmo.getEditText(0)).isEqualTo("-3");
    }

    @Test
    void given_minusAlone_when_committed_then_isIgnored()
    {
        // Arrange
        InputGizmo gizmo = createGizmo();

        type(gizmo, "-");

        // Act
        double[] result = gizmo.getValuesWithEdits();

        // Assert
        assertThat(result[0]).isCloseTo(1.5, offset(EPS));
    }

    @Test
    void given_backspaceOverUneditedField_when_pressed_then_deletesLastCharacterShown()
    {
        // Arrange
        InputGizmo gizmo = createGizmo();

        // Act
        gizmo.processKeyPressedEvent(key(KeyEvent.KEY_BACKSPACE));
        gizmo.processKeyPressedEvent(key(KeyEvent.KEY_BACKSPACE));

        // Assert
        assertThat(gizmo.getEditText(0)).isEqualTo("1.5");
    }

    @Test
    void given_enterWithoutEdition_when_pressed_then_isNotConsumed()
    {
        // Arrange
        InputGizmo gizmo = createGizmo();

        // Act & Assert
        assertThat(gizmo.consumesKey(key(KeyEvent.KEY_ENTER))).isFalse();
        assertThat(gizmo.consumesKey(key(KeyEvent.KEY_ESC))).isFalse();
        type(gizmo, "1");
        assertThat(gizmo.consumesKey(key(KeyEvent.KEY_ENTER))).isTrue();
        assertThat(gizmo.consumesKey(key(KeyEvent.KEY_ESC))).isTrue();
    }

    @Test
    void given_editedFields_when_enterPressed_then_commitMergesOnlyValidEdits()
    {
        // Arrange
        InputGizmo gizmo = createGizmo();

        type(gizmo, "-7.125");
        gizmo.selectNextField();
        gizmo.selectNextField();
        type(gizmo, "9");
        gizmo.selectPreviousField();
        for ( int i = 0; i < 6; i++ ) {
            gizmo.processKeyPressedEvent(key(KeyEvent.KEY_BACKSPACE));
        }

        // Act
        boolean used = gizmo.processKeyPressedEvent(key(KeyEvent.KEY_ENTER));
        double[] result = gizmo.getValuesWithEdits();

        // Assert: Y was emptied, so it keeps its value
        assertThat(used).isTrue();
        assertThat(gizmo.consumeCommit()).isTrue();
        assertThat(gizmo.consumeCommit()).isFalse();
        assertThat(result[0]).isCloseTo(-7.125, offset(EPS));
        assertThat(result[1]).isCloseTo(-2.25, offset(EPS));
        assertThat(result[2]).isCloseTo(9.0, offset(EPS));
    }

    @Test
    void given_editedFields_when_escapePressed_then_editionIsDiscarded()
    {
        // Arrange
        InputGizmo gizmo = createGizmo();

        type(gizmo, "8");

        // Act
        gizmo.processKeyPressedEvent(key(KeyEvent.KEY_ESC));

        // Assert
        assertThat(gizmo.isEditing()).isFalse();
        assertThat(gizmo.getDisplayText(0)).isEqualTo("1.500");
    }

    @Test
    void given_keysWithControl_when_pressed_then_areNotConsumed()
    {
        // Arrange
        InputGizmo gizmo = createGizmo();
        KeyEvent ctrlOne = character('1');

        ctrlOne.modifierMask = KeyEvent.MASK_CTRL;

        // Act & Assert
        assertThat(gizmo.consumesKey(ctrlOne)).isFalse();
        assertThat(gizmo.consumesKey(character('x'))).isFalse();
    }

    @Test
    void given_veryLongText_when_typed_then_isLimited()
    {
        // Arrange
        InputGizmo gizmo = createGizmo();

        // Act
        type(gizmo, "1234567890123456789");

        // Assert
        assertThat(gizmo.getEditText(0)).hasSize(12);
    }

    private static double step(InputGizmo gizmo, int keycode)
    {
        gizmo.processKeyPressedEvent(key(keycode));
        double result = gizmo.getValuesWithEdits()[gizmo.getSelectedField()];

        assertThat(gizmo.consumeCommit()).isTrue();
        gizmo.cancelEditing();
        return result;
    }

    @Test
    void given_steppingKeys_when_pressed_then_selectedValueChangesByTheirStep()
    {
        // Arrange
        InputGizmo gizmo = new InputGizmo(3);

        // Act & Assert
        assertThat(step(gizmo, KeyEvent.KEY_RIGHT)).isCloseTo(0.1, offset(EPS));
        assertThat(step(gizmo, KeyEvent.KEY_LEFT)).isCloseTo(-0.1, offset(EPS));
        assertThat(step(gizmo, KeyEvent.KEY_UP)).isCloseTo(1.0, offset(EPS));
        assertThat(step(gizmo, KeyEvent.KEY_DOWN)).isCloseTo(-1.0, offset(EPS));
        assertThat(step(gizmo, KeyEvent.KEY_PAGEUP)).isCloseTo(5.0, offset(EPS));
        assertThat(step(gizmo, KeyEvent.KEY_PAGEDOWN)).isCloseTo(-5.0, offset(EPS));
    }

    @Test
    void given_steppingKey_when_pressedOverTypedNumber_then_stepsFromTheTypedNumber()
    {
        // Arrange
        InputGizmo gizmo = createGizmo();

        type(gizmo, "10");

        // Act
        double result = step(gizmo, KeyEvent.KEY_PAGEDOWN);

        // Assert
        assertThat(result).isCloseTo(5.0, offset(EPS));
    }

    @Test
    void given_repeatedSmallSteps_when_pressed_then_doNotAccumulateNoise()
    {
        // Arrange
        InputGizmo gizmo = new InputGizmo(1);

        // Act
        for ( int i = 0; i < 30; i++ ) {
            gizmo.processKeyPressedEvent(key(KeyEvent.KEY_RIGHT));
            gizmo.setValue(0, gizmo.getValuesWithEdits()[0]);
            gizmo.consumeCommit();
            gizmo.cancelEditing();
        }

        // Assert
        assertThat(InputGizmo.format(gizmo.getValue(0))).isEqualTo("3.000");
        assertThat(gizmo.getValue(0)).isEqualTo(3.0);
    }

    @Test
    void given_steppingKeyWithControl_when_pressed_then_isNotConsumed()
    {
        // Arrange
        InputGizmo gizmo = createGizmo();
        KeyEvent ctrlUp = key(KeyEvent.KEY_UP);

        ctrlUp.modifierMask = KeyEvent.MASK_CTRL;

        // Act & Assert
        assertThat(gizmo.consumesKey(ctrlUp)).isFalse();
        assertThat(gizmo.consumesKey(key(KeyEvent.KEY_PAGEUP))).isTrue();
    }
}
