import { describe, expect, it } from "vitest";
import { ColorRgb } from "vsdk/toolkit/common/color/ColorRgb.js";
import { InputGizmo } from "vsdk/toolkit/gui/gizmo/InputGizmo.js";
import { KeyEvent } from "vsdk/toolkit/gui/KeyEvent.js";

/**
Exercises the state and the keyboard editing of an input gizmo, which only
depend on vitral key events.
 */
describe("InputGizmoTest", () => {
    function closeTo(actual: number, expected: number, offset: number): void {
        expect(Math.abs(actual - expected)).toBeLessThanOrEqual(offset);
    }

    /** AssertJ `isEqualTo`: Java `equals` when the value defines it. */
    function isEqualTo(actual: unknown, expected: unknown): void {
        const a = actual as { equals?: (other: unknown) => boolean } | null;
        if (a !== null && typeof a === "object" && typeof a.equals === "function") {
            expect(a.equals(expected)).toBe(true);
        } else {
            expect(actual).toEqual(expected);
        }
    }

    function sizeOf(collection: string | { length: number } | { size(): number }): number {
        return typeof collection === "string" || "length" in collection ? collection.length : collection.size();
    }

    const EPS = 1.0e-9;

    function createGizmo(): InputGizmo {
        let gizmo = new InputGizmo(3);

        gizmo.setValue(0, 1.5);
        gizmo.setValue(1, -2.25);
        gizmo.setValue(2, 0.0004);
        return gizmo;
    }

    function character(unicode: string): KeyEvent {
        let event = new KeyEvent();

        event.unicodeId = unicode.charCodeAt(0);
        return event;
    }

    function key(keycode: number): KeyEvent {
        let event = new KeyEvent();

        event.keycode = keycode;
        return event;
    }

    function type(gizmo: InputGizmo, text: string): void {
        for (const c of text) {
            gizmo.processKeyPressedEvent(character(c));
        }
    }

    it("given_noEdition_when_displayed_then_showsValuesWithThreeDecimals", () => {
        // Arrange
        let gizmo = createGizmo();

        // Act & Assert
        isEqualTo(gizmo.getDisplayText(0), "1.500");
        isEqualTo(gizmo.getDisplayText(1), "-2.250");
        isEqualTo(gizmo.getDisplayText(2), "0.000");
        isEqualTo(InputGizmo.format(-0.0001), "0.000");
    });

    it("given_twoDecimals_when_displayed_then_showsValuesWithTwoDecimals", () => {
        // Arrange
        let gizmo = new InputGizmo(3, 2, 3);

        gizmo.setValue(0, 45.678);
        gizmo.setValue(1, -179.999);
        gizmo.setValue(2, -0.004);

        // Act & Assert
        isEqualTo(gizmo.getDecimals(), 2);
        isEqualTo(gizmo.getDisplayText(0), "45.68");
        isEqualTo(gizmo.getDisplayText(1), "-180.00");
        isEqualTo(gizmo.getDisplayText(2), "0.00");
        isEqualTo(InputGizmo.format(-0.004, 2), "0.00");
        isEqualTo(InputGizmo.format(-0.006, 2), "-0.01");
    });

    it("given_gizmoWidth_when_referenceTextAsked_then_hasTheDigitsOfItsBoxes", () => {
        // Act & Assert
        isEqualTo(new InputGizmo(3).getReferenceText(), "-0.000");
        isEqualTo(new InputGizmo(3, 2, 3).getReferenceText(), "-000.00");
        isEqualTo(new InputGizmo(1, 0, 2).getReferenceText(), "-00");
    });

    it("given_twoDecimalsGizmo_when_typingMoreDecimals_then_areNotAccepted", () => {
        // Arrange
        let gizmo = new InputGizmo(2, 2, 3);

        // Act
        type(gizmo, "12.3456");

        // Assert
        isEqualTo(gizmo.getEditText(0), "12.34");
        closeTo(gizmo.getValuesWithEdits()[0], 12.34, EPS);
    });

    it("given_twoDecimalsGizmo_when_typingNegativeNumberAndInteger_then_areAccepted", () => {
        // Arrange
        let gizmo = new InputGizmo(2, 2, 3);

        // Act
        type(gizmo, "-170");
        gizmo.selectNextField();
        type(gizmo, ".5");

        // Assert
        isEqualTo(gizmo.getEditText(0), "-170");
        isEqualTo(gizmo.getEditText(1), "0.5");
    });

    it("given_noDecimalsGizmo_when_typingPoint_then_isNotAccepted", () => {
        // Arrange
        let gizmo = new InputGizmo(1, 0, 3);

        // Act
        type(gizmo, "12.5");

        // Assert
        isEqualTo(gizmo.getEditText(0), "125");
    });

    it("given_defaultGizmo_when_typingManyDecimals_then_keepsAcceptingThem", () => {
        // Arrange
        let gizmo = new InputGizmo(3);

        // Act
        type(gizmo, "1.23456");

        // Assert
        isEqualTo(gizmo.getEditText(0), "1.23456");
    });

    it("given_highlight_when_colorAsked_then_highlightedFieldsAreYellow", () => {
        // Arrange
        let gizmo = createGizmo();
        let red = new ColorRgb(1, 0, 0);

        gizmo.setFieldColor(0, red);

        // Act & Assert
        isEqualTo(gizmo.getFieldDisplayColor(0), red);
        gizmo.setFieldHighlighted(0, true);
        isEqualTo(gizmo.getFieldDisplayColor(0), InputGizmo.HIGHLIGHT_COLOR);
    });

    it("given_tabKey_when_pressed_then_selectionCyclesForwardAndBackward", () => {
        // Arrange
        let gizmo = createGizmo();
        let shiftTab = key(KeyEvent.KEY_TAB);

        shiftTab.modifierMask = KeyEvent.MASK_SHIFT;

        // Act & Assert
        isEqualTo(gizmo.getSelectedField(), 0);
        gizmo.processKeyPressedEvent(key(KeyEvent.KEY_TAB));
        isEqualTo(gizmo.getSelectedField(), 1);
        gizmo.processKeyPressedEvent(key(KeyEvent.KEY_TAB));
        gizmo.processKeyPressedEvent(key(KeyEvent.KEY_TAB));
        isEqualTo(gizmo.getSelectedField(), 0);
        gizmo.processKeyPressedEvent(shiftTab);
        isEqualTo(gizmo.getSelectedField(), 2);
    });

    it("given_digitsOverUneditedField_when_typed_then_replaceItsValue", () => {
        // Arrange
        let gizmo = createGizmo();

        // Act
        type(gizmo, "12.5");

        // Assert
        expect(gizmo.isEditing(0)).toBe(true);
        isEqualTo(gizmo.getDisplayText(0), "12.5");
        isEqualTo(gizmo.getDisplayText(1), "-2.250");
    });

    it("given_numericKeypadKeys_when_pressed_then_areTypedToo", () => {
        // Arrange
        let gizmo = createGizmo();

        // Act: numeric keypad events only have a key code
        gizmo.processKeyPressedEvent(key(KeyEvent.KEY_NUM7));
        gizmo.processKeyPressedEvent(key(KeyEvent.KEY_NUMPERIOD));
        gizmo.processKeyPressedEvent(character(","));
        gizmo.processKeyPressedEvent(key(KeyEvent.KEY_NUM2));

        // Assert: the second point is ignored
        isEqualTo(gizmo.getEditText(0), "7.2");
    });

    it("given_pointFirst_when_typed_then_startsWithZero", () => {
        // Arrange
        let gizmo = createGizmo();

        // Act
        type(gizmo, ".5");

        // Assert
        isEqualTo(gizmo.getEditText(0), "0.5");
    });

    it("given_minusFirst_when_typed_then_startsANegativeNumber", () => {
        // Arrange
        let gizmo = createGizmo();

        // Act
        type(gizmo, "-3.5");

        // Assert
        isEqualTo(gizmo.getEditText(0), "-3.5");
        closeTo(gizmo.getValuesWithEdits()[0], -3.5, EPS);
    });

    it("given_minusKeyCodes_when_pressed_then_areConsumedAndStartNegativeNumbers", () => {
        // Arrange
        let gizmo = createGizmo();

        // Act & Assert: `-` is consumed even when nothing is being edited
        expect(gizmo.consumesKey(key(KeyEvent.KEY_MINUS))).toBe(true);
        gizmo.processKeyPressedEvent(key(KeyEvent.KEY_MINUS));
        gizmo.processKeyPressedEvent(key(KeyEvent.KEY_NUM4));
        isEqualTo(gizmo.getEditText(0), "-4");
    });

    it("given_minusKey_when_pressedAgain_then_changesTheSign", () => {
        // Arrange
        let gizmo = createGizmo();

        type(gizmo, "-3");

        // Act
        type(gizmo, "-");

        // Assert
        isEqualTo(gizmo.getEditText(0), "3");
        type(gizmo, "-");
        isEqualTo(gizmo.getEditText(0), "-3");
    });

    it("given_minusAlone_when_committed_then_isIgnored", () => {
        // Arrange
        let gizmo = createGizmo();

        type(gizmo, "-");

        // Act
        let result = gizmo.getValuesWithEdits();

        // Assert
        closeTo(result[0], 1.5, EPS);
    });

    it("given_backspaceOverUneditedField_when_pressed_then_deletesLastCharacterShown", () => {
        // Arrange
        let gizmo = createGizmo();

        // Act
        gizmo.processKeyPressedEvent(key(KeyEvent.KEY_BACKSPACE));
        gizmo.processKeyPressedEvent(key(KeyEvent.KEY_BACKSPACE));

        // Assert
        isEqualTo(gizmo.getEditText(0), "1.5");
    });

    it("given_enterWithoutEdition_when_pressed_then_isNotConsumed", () => {
        // Arrange
        let gizmo = createGizmo();

        // Act & Assert
        expect(gizmo.consumesKey(key(KeyEvent.KEY_ENTER))).toBe(false);
        expect(gizmo.consumesKey(key(KeyEvent.KEY_ESC))).toBe(false);
        type(gizmo, "1");
        expect(gizmo.consumesKey(key(KeyEvent.KEY_ENTER))).toBe(true);
        expect(gizmo.consumesKey(key(KeyEvent.KEY_ESC))).toBe(true);
    });

    it("given_editedFields_when_enterPressed_then_commitMergesOnlyValidEdits", () => {
        // Arrange
        let gizmo = createGizmo();

        type(gizmo, "-7.125");
        gizmo.selectNextField();
        gizmo.selectNextField();
        type(gizmo, "9");
        gizmo.selectPreviousField();
        for (let i = 0; i < 6; i++) {
            gizmo.processKeyPressedEvent(key(KeyEvent.KEY_BACKSPACE));
        }

        // Act
        let used = gizmo.processKeyPressedEvent(key(KeyEvent.KEY_ENTER));
        let result = gizmo.getValuesWithEdits();

        // Assert: Y was emptied, so it keeps its value
        expect(used).toBe(true);
        expect(gizmo.consumeCommit()).toBe(true);
        expect(gizmo.consumeCommit()).toBe(false);
        closeTo(result[0], -7.125, EPS);
        closeTo(result[1], -2.25, EPS);
        closeTo(result[2], 9.0, EPS);
    });

    it("given_editedFields_when_escapePressed_then_editionIsDiscarded", () => {
        // Arrange
        let gizmo = createGizmo();

        type(gizmo, "8");

        // Act
        gizmo.processKeyPressedEvent(key(KeyEvent.KEY_ESC));

        // Assert
        expect(gizmo.isEditing()).toBe(false);
        isEqualTo(gizmo.getDisplayText(0), "1.500");
    });

    it("given_keysWithControl_when_pressed_then_areNotConsumed", () => {
        // Arrange
        let gizmo = createGizmo();
        let ctrlOne = character("1");

        ctrlOne.modifierMask = KeyEvent.MASK_CTRL;

        // Act & Assert
        expect(gizmo.consumesKey(ctrlOne)).toBe(false);
        expect(gizmo.consumesKey(character("x"))).toBe(false);
    });

    it("given_veryLongText_when_typed_then_isLimited", () => {
        // Arrange
        let gizmo = createGizmo();

        // Act
        type(gizmo, "1234567890123456789");

        // Assert
        expect(sizeOf(gizmo.getEditText(0))).toBe(12);
    });

    function step(gizmo: InputGizmo, keycode: number): number {
        gizmo.processKeyPressedEvent(key(keycode));
        let result = gizmo.getValuesWithEdits()[gizmo.getSelectedField()];

        expect(gizmo.consumeCommit()).toBe(true);
        gizmo.cancelEditing();
        return result;
    }

    it("given_steppingKeys_when_pressed_then_selectedValueChangesByTheirStep", () => {
        // Arrange
        let gizmo = new InputGizmo(3);

        // Act & Assert
        closeTo(step(gizmo, KeyEvent.KEY_RIGHT), 0.1, EPS);
        closeTo(step(gizmo, KeyEvent.KEY_LEFT), -0.1, EPS);
        closeTo(step(gizmo, KeyEvent.KEY_UP), 1.0, EPS);
        closeTo(step(gizmo, KeyEvent.KEY_DOWN), -1.0, EPS);
        closeTo(step(gizmo, KeyEvent.KEY_PAGEUP), 5.0, EPS);
        closeTo(step(gizmo, KeyEvent.KEY_PAGEDOWN), -5.0, EPS);
    });

    it("given_steppingKey_when_pressedOverTypedNumber_then_stepsFromTheTypedNumber", () => {
        // Arrange
        let gizmo = createGizmo();

        type(gizmo, "10");

        // Act
        let result = step(gizmo, KeyEvent.KEY_PAGEDOWN);

        // Assert
        closeTo(result, 5.0, EPS);
    });

    it("given_repeatedSmallSteps_when_pressed_then_doNotAccumulateNoise", () => {
        // Arrange
        let gizmo = new InputGizmo(1);

        // Act
        for (let i = 0; i < 30; i++) {
            gizmo.processKeyPressedEvent(key(KeyEvent.KEY_RIGHT));
            gizmo.setValue(0, gizmo.getValuesWithEdits()[0]);
            gizmo.consumeCommit();
            gizmo.cancelEditing();
        }

        // Assert
        isEqualTo(InputGizmo.format(gizmo.getValue(0)), "3.000");
        isEqualTo(gizmo.getValue(0), 3.0);
    });

    it("given_steppingKeyWithControl_when_pressed_then_isNotConsumed", () => {
        // Arrange
        let gizmo = createGizmo();
        let ctrlUp = key(KeyEvent.KEY_UP);

        ctrlUp.modifierMask = KeyEvent.MASK_CTRL;

        // Act & Assert
        expect(gizmo.consumesKey(ctrlUp)).toBe(false);
        expect(gizmo.consumesKey(key(KeyEvent.KEY_PAGEUP))).toBe(true);
    });
});
