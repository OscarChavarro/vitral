import { Double } from "../../../../java/lang/Double.js";
import { ColorRgb } from "../../common/color/ColorRgb.js";
import { KeyEvent } from "../KeyEvent.js";
import { Gizmo } from "./Gizmo.js";
import { InputGizmoValueChangeRules } from "./InputGizmoValueChangeRules.js";

/**
Java's `BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP)
.stripTrailingZeros().toPlainString()`, over the decimal digits of
`Double.toString(value)` (which is what `BigDecimal.valueOf` reads).
*/
function roundHalfUpToPlainString(value: number, scale: number): string {
    const text: string = Double.toString(value);
    const negative: boolean = text.startsWith("-");
    const unsignedText: string = negative ? text.substring(1) : text;
    const exponentIndex: number = unsignedText.indexOf("E");
    const mantissa: string = exponentIndex >= 0 ? unsignedText.substring(0, exponentIndex) : unsignedText;
    const exponent: number = exponentIndex >= 0 ? Number(unsignedText.substring(exponentIndex + 1)) : 0;
    const point: number = mantissa.indexOf(".");
    let digits: string = point >= 0 ? mantissa.substring(0, point) + mantissa.substring(point + 1) : mantissa;
    // Position of the decimal point inside `digits`
    let pointPosition: number = (point >= 0 ? point : mantissa.length) + exponent;

    if (pointPosition <= 0) {
        digits = "0".repeat(1 - pointPosition) + digits;
        pointPosition = 1;
    }
    if (digits.length < pointPosition + scale) {
        digits = digits + "0".repeat(pointPosition + scale - digits.length);
    }

    // Round the digits after `pointPosition + scale`, half up (away from zero)
    const kept: string = digits.substring(0, pointPosition + scale);
    const firstDropped: string = digits.charAt(pointPosition + scale);
    let rounded: bigint = BigInt(kept === "" ? "0" : kept);
    if (firstDropped !== "" && firstDropped >= "5") {
        rounded = rounded + 1n;
    }

    let roundedText: string = rounded.toString().padStart(scale + 1, "0");
    let integerPart: string = roundedText.substring(0, roundedText.length - scale);
    let fractionPart: string = roundedText.substring(roundedText.length - scale);

    fractionPart = fractionPart.replace(/0+$/, "");
    integerPart = integerPart.replace(/^0+(?=\d)/, "");
    roundedText = fractionPart.length > 0 ? integerPart + "." + fractionPart : integerPart;
    if (roundedText === "0") {
        return "0";
    }
    return (negative ? "-" : "") + roundedText;
}

/**
Gizmo made of a row of numeric input boxes, designed to show (and let the user
type) the numbers that describe the state of another gizmo, i.e. the
coordinates of a `TranslateGizmo`, which uses it nested.

Each box shows a value with `getDecimals()` digits (`DECIMALS` by default),
unless the user is editing it: then it shows the text typed. Exactly one box is selected, and TAB (SHIFT+TAB)
cycles the selection. Digits, the decimal point and BACKSPACE edit the
selected box as an input: typing over a box that is not being edited replaces
its value, and `-` starts a negative number, or changes the sign of the number
being edited. The arrow keys and PAGEUP / PAGEDOWN step the value of the
selected box, and accept it right away (see below). ENTER accepts every box
edited (see `consumeCommit`), ESC discards the edition. The owner keeps the values updated (see `setValue`) and
must discard the edition (see `cancelEditing`) whenever it changes the state
the values describe by other means, so the boxes go back to show the real
values.

Each box has a color (see `setFieldColor`), that becomes the highlight color
when the owner highlights it (see `setFieldHighlighted`).

This class only keeps state and processes vitral events: painting is up to
renderers such as `Jogl4InputGizmoRenderer`, and using the numbers accepted is
up to the owner.
*/
export class InputGizmo extends Gizmo {
    public static readonly DECIMALS = 3;
    public static readonly HIGHLIGHT_COLOR = new ColorRgb(1, 1, 0);

    private static readonly MAX_EDIT_LENGTH = 12;
    private static readonly DEFAULT_FIELD_COLOR = new ColorRgb(0, 0, 0);

    private readonly decimals: number;
    private readonly integerDigits: number;
    private readonly limitTypedDecimals: boolean;
    private readonly values: number[];
    private readonly colors: ColorRgb[];
    private readonly highlighted: boolean[];
    private readonly editTexts: (string | null)[];
    private selectedField: number;
    private commitPending: boolean;
    private valueChangeRules: InputGizmoValueChangeRules;

    /**
    Java has three public constructors, taken here by the given arguments:

    - `(numberOfFields)`: a gizmo that shows `DECIMALS` decimals, and lets the
      user type as many as fit in a box. Its boxes step by
      `InputGizmoValueChangeRules.forTranslation()`.
    - `(numberOfFields, decimals, integerDigits)`: a gizmo whose boxes show,
      and accept typing, only the given number of decimals (i.e. 2 for angles
      in degrees). Its boxes step by `InputGizmoValueChangeRules.forTranslation()`.
    - `(numberOfFields, decimals, integerDigits, valueChangeRules)`: as the
      former, stepping as described by the given rules (i.e.
      `InputGizmoValueChangeRules.forRotation()` for angles in degrees).

    @param numberOfFields number of input boxes, at least 1
    @param decimals number of digits shown after the decimal point (not
    negative); the user can not type more
    @param integerDigits number of digits before the decimal point the boxes
    are wide enough for (at least 1); i.e. 3 for angles in degrees
    @param valueChangeRules rules that describe how the selected box steps
    with the keyboard
    */
    public constructor(
        numberOfFields: number,
        decimals?: number,
        integerDigits?: number,
        valueChangeRules?: InputGizmoValueChangeRules | null,
    ) {
        super();
        const count: number = Math.max(1, numberOfFields);
        const limitTypedDecimals: boolean = decimals !== undefined;

        this.decimals = Math.max(0, decimals !== undefined ? decimals : InputGizmo.DECIMALS);
        this.integerDigits = Math.max(1, integerDigits !== undefined ? integerDigits : 1);
        this.limitTypedDecimals = limitTypedDecimals;
        this.valueChangeRules =
            valueChangeRules !== undefined && valueChangeRules !== null
                ? valueChangeRules
                : InputGizmoValueChangeRules.forTranslation();
        this.values = new Array<number>(count).fill(0);
        this.colors = new Array<ColorRgb>(count).fill(InputGizmo.DEFAULT_FIELD_COLOR);
        this.highlighted = new Array<boolean>(count).fill(false);
        this.editTexts = new Array<string | null>(count).fill(null);
        this.selectedField = 0;
        this.commitPending = false;
    }

    /**
    @return the rules that describe how the selected box steps with the
    keyboard
    */
    public getValueChangeRules(): InputGizmoValueChangeRules {
        return this.valueChangeRules;
    }

    /**
    @param valueChangeRules rules that describe how the selected box steps
    with the keyboard; null is ignored
    */
    public setValueChangeRules(valueChangeRules: InputGizmoValueChangeRules | null): void {
        if (valueChangeRules !== null) {
            this.valueChangeRules = valueChangeRules;
        }
    }

    /**
    @param value number to show
    @param decimals number of digits shown after the decimal point (`DECIMALS`
    when not given)
    @return the text shown for a value that is not being edited
    */
    public static format(value: number, decimals: number = InputGizmo.DECIMALS): string {
        const text: string = value.toFixed(decimals);

        if (text.startsWith("-") && /^0*\.?0*$/.test(text.substring(1))) {
            // A tiny negative value must not show as a "negative zero"
            return text.substring(1);
        }
        return text;
    }

    /**
    @return the number of digits shown after the decimal point
    */
    public getDecimals(): number {
        return this.decimals;
    }

    /**
    @return the widest text a box is expected to show (i.e. `-0.000`), so
    presenters can give every box the same width
    */
    public getReferenceText(): string {
        return "-" + "0".repeat(this.integerDigits) + (this.decimals > 0 ? "." + "0".repeat(this.decimals) : "");
    }

    /**
    @return the number of input boxes
    */
    public getNumberOfFields(): number {
        return this.values.length;
    }

    //= Values and colors =================================================

    /**
    @param field index of a box
    @return the value the box shows when it is not being edited
    */
    public getValue(field: number): number {
        return this.values[InputGizmo.checkIndex(field, this.values.length)]!;
    }

    /**
    @param field index of a box
    @param value the value the box shows when it is not being edited
    */
    public setValue(field: number, value: number): void {
        this.values[InputGizmo.checkIndex(field, this.values.length)] = value;
    }

    /**
    @param field index of a box
    @return the normal color of the box
    */
    public getFieldColor(field: number): ColorRgb {
        return this.colors[InputGizmo.checkIndex(field, this.colors.length)]!;
    }

    /**
    @param field index of a box
    @param color the normal color of the box
    */
    public setFieldColor(field: number, color: ColorRgb): void {
        this.colors[InputGizmo.checkIndex(field, this.colors.length)] = color;
    }

    public isFieldHighlighted(field: number): boolean {
        return this.highlighted[InputGizmo.checkIndex(field, this.highlighted.length)]!;
    }

    /**
    @param field index of a box
    @param highlighted true to draw the box with `HIGHLIGHT_COLOR`
    */
    public setFieldHighlighted(field: number, highlighted: boolean): void {
        this.highlighted[InputGizmo.checkIndex(field, this.highlighted.length)] = highlighted;
    }

    /**
    @param field index of a box
    @return the color the box must be drawn with
    */
    public getFieldDisplayColor(field: number): ColorRgb {
        return this.isFieldHighlighted(field) ? InputGizmo.HIGHLIGHT_COLOR : this.getFieldColor(field);
    }

    //= Selection and edition state =======================================

    /**
    @return the index of the selected box
    */
    public getSelectedField(): number {
        return this.selectedField;
    }

    /**
    @param field index of the box to select; invalid indexes are ignored
    */
    public setSelectedField(field: number): void {
        if (field >= 0 && field < this.values.length) {
            this.selectedField = field;
        }
    }

    public selectNextField(): void {
        this.selectedField = (this.selectedField + 1) % this.values.length;
    }

    public selectPreviousField(): void {
        this.selectedField = (this.selectedField + this.values.length - 1) % this.values.length;
    }

    /**
    Java's two `isEditing` overloads: with a box index, true if the user has
    typed something in the box, and it has not been accepted or discarded yet;
    without it, true if any box is being edited.
    @param field index of a box, or nothing to ask about every box
    @return true if the box (or any box) is being edited
    */
    public isEditing(field?: number): boolean {
        if (field !== undefined) {
            return this.editTexts[InputGizmo.checkIndex(field, this.editTexts.length)] !== null;
        }
        for (const text of this.editTexts) {
            if (text !== null) {
                return true;
            }
        }
        return false;
    }

    /**
    @param field index of a box
    @return the text typed in the box, or null if it is not being edited
    */
    public getEditText(field: number): string | null {
        return (this.editTexts[InputGizmo.checkIndex(field, this.editTexts.length)] ?? null);
    }

    /**
    @param field index of a box
    @return the text the box must show: the one being typed or its value
    */
    public getDisplayText(field: number): string {
        const text: string | null = this.getEditText(field);

        if (text !== null) {
            return text;
        }
        return InputGizmo.format(this.values[field]!, this.decimals);
    }

    /**
    Discards the text typed in every box (and any pending commit), so they show
    their values again.
    */
    public cancelEditing(): void {
        for (let i = 0; i < this.editTexts.length; i++) {
            this.editTexts[i] = null;
        }
        this.commitPending = false;
    }

    //= Events ============================================================

    /**
    Tells if a key press belongs to the boxes, so the caller can keep it away
    from other commands that use the same keys (digits, `-`, TAB...). ENTER and
    ESC are only taken while some box is being edited.
    @param event key press
    @return true if `processKeyPressedEvent` would use the event
    */
    public consumesKey(event: KeyEvent): boolean {
        if ((event.modifierMask & (KeyEvent.MASK_CTRL | KeyEvent.MASK_ALT)) !== 0) {
            return false;
        }
        if (
            InputGizmo.digitOf(event) >= 0 ||
            InputGizmo.isDecimalPoint(event) ||
            InputGizmo.isMinus(event) ||
            this.stepOf(event) !== 0.0
        ) {
            return true;
        }
        switch (event.keycode) {
            case KeyEvent.KEY_TAB:
            case KeyEvent.KEY_BACKSPACE:
                return true;
            case KeyEvent.KEY_ENTER:
            case KeyEvent.KEY_NUMENTER:
            case KeyEvent.KEY_ESC:
                return this.isEditing();
            default:
                return false;
        }
    }

    /**
    Edits the boxes as requested by a key press.
    @param event key press
    @return true if the event was used (see `consumesKey`)
    */
    public processKeyPressedEvent(event: KeyEvent): boolean {
        if (!this.consumesKey(event)) {
            return false;
        }

        const digit: number = InputGizmo.digitOf(event);

        if (digit >= 0) {
            this.appendToSelected(String.fromCharCode(KeyEvent.charCode("0") + digit));
        } else if (InputGizmo.isDecimalPoint(event)) {
            this.appendDecimalPoint();
        } else if (InputGizmo.isMinus(event)) {
            this.toggleSignOfSelected();
        } else if (this.stepOf(event) !== 0.0) {
            this.stepSelected(this.stepOf(event));
        } else {
            switch (event.keycode) {
                case KeyEvent.KEY_TAB:
                    if ((event.modifierMask & KeyEvent.MASK_SHIFT) !== 0) {
                        this.selectPreviousField();
                    } else {
                        this.selectNextField();
                    }
                    break;
                case KeyEvent.KEY_BACKSPACE:
                    this.deleteLastCharacterOfSelected();
                    break;
                case KeyEvent.KEY_ESC:
                    this.cancelEditing();
                    break;
                case KeyEvent.KEY_ENTER:
                case KeyEvent.KEY_NUMENTER:
                    this.commitPending = true;
                    break;
                default:
                    break;
            }
        }
        return true;
    }

    /**
    @return true (once) if the user accepted the edition with ENTER (or stepped
    a value with the arrow keys) since the last call; the owner must then use the numbers (see `getValuesWithEdits`)
    and discard the edition
    */
    public consumeCommit(): boolean {
        const commit: boolean = this.commitPending;

        this.commitPending = false;
        return commit;
    }

    /**
    @return the values of the boxes, replacing the ones of the boxes edited by
    the numbers typed. Texts that are not a number (i.e. empty ones) are
    ignored.
    */
    public getValuesWithEdits(): number[] {
        const result: number[] = this.values.slice();

        for (let i = 0; i < result.length; i++) {
            const value: number | null = InputGizmo.parse((this.editTexts[i] ?? null));

            if (value !== null) {
                result[i] = value;
            }
        }
        return result;
    }

    /**
    @param text text typed in a box, or null
    @return the number it represents, or null if it is not a valid number
    */
    public static parse(text: string | null): number | null {
        if (text === null || text.length === 0) {
            return null;
        }
        try {
            const value: number = Double.parseDouble(text);

            if (Number.isNaN(value) || !Number.isFinite(value)) {
                return null;
            }
            return value;
        } catch {
            return null;
        }
    }

    //= Key classification and text edition ===============================

    /**
    @return 0..9 if the event is a digit key (main or numeric keypad), or -1
    */
    private static digitOf(event: KeyEvent): number {
        if (event.unicodeId >= KeyEvent.charCode("0") && event.unicodeId <= KeyEvent.charCode("9")) {
            return event.unicodeId - KeyEvent.charCode("0");
        }
        if (event.keycode >= KeyEvent.KEY_0 && event.keycode <= KeyEvent.KEY_9) {
            return event.keycode - KeyEvent.KEY_0;
        }
        if (event.keycode >= KeyEvent.KEY_NUM0 && event.keycode <= KeyEvent.KEY_NUM9) {
            return event.keycode - KeyEvent.KEY_NUM0;
        }
        return -1;
    }

    /**
    The comma is accepted too, as it is the decimal separator of many keyboard
    layouts.
    */
    private static isDecimalPoint(event: KeyEvent): boolean {
        return (
            event.unicodeId === KeyEvent.charCode(".") ||
            event.unicodeId === KeyEvent.charCode(",") ||
            event.keycode === KeyEvent.KEY_PERIOD ||
            event.keycode === KeyEvent.KEY_NUMPERIOD ||
            event.keycode === KeyEvent.KEY_COMMA
        );
    }

    private static isMinus(event: KeyEvent): boolean {
        return (
            event.unicodeId === KeyEvent.charCode("-") ||
            event.keycode === KeyEvent.KEY_MINUS ||
            event.keycode === KeyEvent.KEY_NUMMINUS
        );
    }

    /**
    @return the increment the key requests for the selected box, or 0 if it is
    not a stepping key
    */
    private stepOf(event: KeyEvent): number {
        switch (event.keycode) {
            case KeyEvent.KEY_RIGHT:
                return this.valueChangeRules.getLevelOneStep();
            case KeyEvent.KEY_LEFT:
                return -this.valueChangeRules.getLevelOneStep();
            case KeyEvent.KEY_UP:
                return this.valueChangeRules.getLevelTwoStep();
            case KeyEvent.KEY_DOWN:
                return -this.valueChangeRules.getLevelTwoStep();
            case KeyEvent.KEY_PAGEUP:
                return this.valueChangeRules.getLevelThreeStep();
            case KeyEvent.KEY_PAGEDOWN:
                return -this.valueChangeRules.getLevelThreeStep();
            default:
                return 0.0;
        }
    }

    /**
    Adds the step to the number the selected box shows (the one being typed, if
    it is valid, or its value), restricted as `getValueChangeRules()`
    describes (grid, circular range), and accepts it right away, as ENTER does
    (so any other box being edited is accepted too).
    */
    private stepSelected(step: number): void {
        const typed: number | null = InputGizmo.parse((this.editTexts[this.selectedField] ?? null));
        const current: number = typed !== null ? typed : this.values[this.selectedField]!;
        const next: number = this.valueChangeRules.applyStep(current, step);

        // Rounded, so repeated small steps do not accumulate binary noise
        this.editTexts[this.selectedField] = roundHalfUpToPlainString(next, 9);
        this.commitPending = true;
    }

    private appendToSelected(character: string): void {
        let text: string | null = (this.editTexts[this.selectedField] ?? null);

        if (text === null) {
            text = "";
        }
        if (text.length >= InputGizmo.MAX_EDIT_LENGTH) {
            return;
        }
        if (this.limitTypedDecimals) {
            const point: number = text.indexOf(".");

            if (point >= 0 && text.length - point - 1 >= this.decimals) {
                return;
            }
        }
        this.editTexts[this.selectedField] = text + character;
    }

    private appendDecimalPoint(): void {
        let text: string | null = (this.editTexts[this.selectedField] ?? null);

        if (text === null) {
            text = "";
        }
        if (text.indexOf(".") >= 0 || (this.limitTypedDecimals && this.decimals === 0)) {
            return;
        }
        if (text.length === 0 || text === "-") {
            text += "0";
        }
        if (text.length >= InputGizmo.MAX_EDIT_LENGTH) {
            return;
        }
        this.editTexts[this.selectedField] = text + ".";
    }

    /**
    Over a box that is not being edited, it starts a negative number.
    */
    private toggleSignOfSelected(): void {
        let text: string | null = (this.editTexts[this.selectedField] ?? null);

        if (text === null) {
            text = "";
        }
        if (text.startsWith("-")) {
            this.editTexts[this.selectedField] = text.substring(1);
        } else if (text.length < InputGizmo.MAX_EDIT_LENGTH) {
            this.editTexts[this.selectedField] = "-" + text;
        }
    }

    /**
    Over a box that is not being edited, the deletion starts from the text the
    box shows.
    */
    private deleteLastCharacterOfSelected(): void {
        let text: string = this.getDisplayText(this.selectedField);

        if (text.length > 0) {
            text = text.substring(0, text.length - 1);
        }
        this.editTexts[this.selectedField] = text;
    }

    /**
    Java arrays throw `ArrayIndexOutOfBoundsException` for a bad index; the
    same failure is raised here instead of reading `undefined`.
    */
    private static checkIndex(index: number, length: number): number {
        if (!Number.isInteger(index) || index < 0 || index >= length) {
            throw new RangeError("Index " + index + " out of bounds for length " + length);
        }
        return index;
    }
}
