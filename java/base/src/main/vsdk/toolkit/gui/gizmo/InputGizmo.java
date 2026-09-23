package vsdk.toolkit.gui.gizmo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.gui.KeyEvent;

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
public class InputGizmo extends Gizmo {
    public static final int DECIMALS = 3;
    public static final ColorRgb HIGHLIGHT_COLOR = new ColorRgb(1, 1, 0);

    private static final int MAX_EDIT_LENGTH = 12;
    private static final ColorRgb DEFAULT_FIELD_COLOR = new ColorRgb(0, 0, 0);

    private final int decimals;
    private final int integerDigits;
    private final boolean limitTypedDecimals;
    private final double[] values;
    private final ColorRgb[] colors;
    private final boolean[] highlighted;
    private final String[] editTexts;
    private int selectedField;
    private boolean commitPending;
    private InputGizmoValueChangeRules valueChangeRules;

    /**
    Creates a gizmo that shows `DECIMALS` decimals, and lets the user type as
    many as fit in a box. Its boxes step by `InputGizmoValueChangeRules.forTranslation()`.
    @param numberOfFields number of input boxes, at least 1
    */
    public InputGizmo(int numberOfFields)
    {
        this(numberOfFields, DECIMALS, 1, false, InputGizmoValueChangeRules.forTranslation());
    }

    /**
    Creates a gizmo whose boxes show, and accept typing, only the given number
    of decimals (i.e. 2 for angles in degrees). Its boxes step by
    `InputGizmoValueChangeRules.forTranslation()`.
    @param numberOfFields number of input boxes, at least 1
    @param decimals number of digits shown after the decimal point (not
    negative); the user can not type more
    @param integerDigits number of digits before the decimal point the boxes
    are wide enough for (at least 1); i.e. 3 for angles in degrees
    */
    public InputGizmo(int numberOfFields, int decimals, int integerDigits)
    {
        this(numberOfFields, decimals, integerDigits, true, InputGizmoValueChangeRules.forTranslation());
    }

    /**
    Creates a gizmo whose boxes show, and accept typing, only the given number
    of decimals, and step as described by the given rules (i.e.
    `InputGizmoValueChangeRules.forRotation()` for angles in degrees).
    @param numberOfFields number of input boxes, at least 1
    @param decimals number of digits shown after the decimal point (not
    negative); the user can not type more
    @param integerDigits number of digits before the decimal point the boxes
    are wide enough for (at least 1); i.e. 3 for angles in degrees
    @param valueChangeRules rules that describe how the selected box steps
    with the keyboard; not null
    */
    public InputGizmo(int numberOfFields, int decimals, int integerDigits,
                      InputGizmoValueChangeRules valueChangeRules)
    {
        this(numberOfFields, decimals, integerDigits, true, valueChangeRules);
    }

    private InputGizmo(int numberOfFields,
                       int decimals,
                       int integerDigits,
                       boolean limitTypedDecimals,
                       InputGizmoValueChangeRules valueChangeRules)
    {
        int count = Math.max(1, numberOfFields);

        this.decimals = Math.max(0, decimals);
        this.integerDigits = Math.max(1, integerDigits);
        this.limitTypedDecimals = limitTypedDecimals;
        this.valueChangeRules = valueChangeRules != null ?
            valueChangeRules : InputGizmoValueChangeRules.forTranslation();
        values = new double[count];
        colors = new ColorRgb[count];
        highlighted = new boolean[count];
        editTexts = new String[count];
        for ( int i = 0; i < count; i++ ) {
            colors[i] = DEFAULT_FIELD_COLOR;
        }
        selectedField = 0;
        commitPending = false;
    }

    /**
    @return the rules that describe how the selected box steps with the
    keyboard
    */
    public InputGizmoValueChangeRules getValueChangeRules()
    {
        return valueChangeRules;
    }

    /**
    @param valueChangeRules rules that describe how the selected box steps
    with the keyboard; null is ignored
    */
    public void setValueChangeRules(InputGizmoValueChangeRules valueChangeRules)
    {
        if ( valueChangeRules != null ) {
            this.valueChangeRules = valueChangeRules;
        }
    }

    /**
    @param value number to show
    @return the text shown, with `DECIMALS` decimals, for a value that is not
    being edited
    */
    public static String format(double value)
    {
        return format(value, DECIMALS);
    }

    /**
    @param value number to show
    @param decimals number of digits shown after the decimal point
    @return the text shown for a value that is not being edited
    */
    public static String format(double value, int decimals)
    {
        String text = String.format(Locale.ROOT, "%." + decimals + "f", value);

        if ( text.startsWith("-") && text.substring(1).matches("0*\\.?0*") ) {
            // A tiny negative value must not show as a "negative zero"
            return text.substring(1);
        }
        return text;
    }

    /**
    @return the number of digits shown after the decimal point
    */
    public int getDecimals()
    {
        return decimals;
    }

    /**
    @return the widest text a box is expected to show (i.e. `-0.000`), so
    presenters can give every box the same width
    */
    public String getReferenceText()
    {
        return "-" + "0".repeat(integerDigits) + (decimals > 0 ? "." + "0".repeat(decimals) : "");
    }

    /**
    @return the number of input boxes
    */
    public int getNumberOfFields()
    {
        return values.length;
    }

    //= Values and colors =================================================

    /**
    @param field index of a box
    @return the value the box shows when it is not being edited
    */
    public double getValue(int field)
    {
        return values[field];
    }

    /**
    @param field index of a box
    @param value the value the box shows when it is not being edited
    */
    public void setValue(int field, double value)
    {
        values[field] = value;
    }

    /**
    @param field index of a box
    @return the normal color of the box
    */
    public ColorRgb getFieldColor(int field)
    {
        return colors[field];
    }

    /**
    @param field index of a box
    @param color the normal color of the box
    */
    public void setFieldColor(int field, ColorRgb color)
    {
        colors[field] = color;
    }

    public boolean isFieldHighlighted(int field)
    {
        return highlighted[field];
    }

    /**
    @param field index of a box
    @param highlighted true to draw the box with `HIGHLIGHT_COLOR`
    */
    public void setFieldHighlighted(int field, boolean highlighted)
    {
        this.highlighted[field] = highlighted;
    }

    /**
    @param field index of a box
    @return the color the box must be drawn with
    */
    public ColorRgb getFieldDisplayColor(int field)
    {
        return highlighted[field] ? HIGHLIGHT_COLOR : colors[field];
    }

    //= Selection and edition state =======================================

    /**
    @return the index of the selected box
    */
    public int getSelectedField()
    {
        return selectedField;
    }

    /**
    @param field index of the box to select; invalid indexes are ignored
    */
    public void setSelectedField(int field)
    {
        if ( field >= 0 && field < values.length ) {
            selectedField = field;
        }
    }

    public void selectNextField()
    {
        selectedField = (selectedField + 1) % values.length;
    }

    public void selectPreviousField()
    {
        selectedField = (selectedField + values.length - 1) % values.length;
    }

    /**
    @param field index of a box
    @return true if the user has typed something in the box, and it has not
    been accepted or discarded yet
    */
    public boolean isEditing(int field)
    {
        return editTexts[field] != null;
    }

    /**
    @return true if any box is being edited
    */
    public boolean isEditing()
    {
        for ( String text : editTexts ) {
            if ( text != null ) {
                return true;
            }
        }
        return false;
    }

    /**
    @param field index of a box
    @return the text typed in the box, or null if it is not being edited
    */
    public String getEditText(int field)
    {
        return editTexts[field];
    }

    /**
    @param field index of a box
    @return the text the box must show: the one being typed or its value
    */
    public String getDisplayText(int field)
    {
        if ( editTexts[field] != null ) {
            return editTexts[field];
        }
        return format(values[field], decimals);
    }

    /**
    Discards the text typed in every box (and any pending commit), so they show
    their values again.
    */
    public void cancelEditing()
    {
        for ( int i = 0; i < editTexts.length; i++ ) {
            editTexts[i] = null;
        }
        commitPending = false;
    }

    //= Events ============================================================

    /**
    Tells if a key press belongs to the boxes, so the caller can keep it away
    from other commands that use the same keys (digits, `-`, TAB...). ENTER and
    ESC are only taken while some box is being edited.
    @param event key press
    @return true if `processKeyPressedEvent` would use the event
    */
    public boolean consumesKey(KeyEvent event)
    {
        if ( (event.modifierMask & (KeyEvent.MASK_CTRL | KeyEvent.MASK_ALT)) != 0 ) {
            return false;
        }
        if ( digitOf(event) >= 0 || isDecimalPoint(event) || isMinus(event) ||
             stepOf(event) != 0.0 ) {
            return true;
        }
        switch ( event.keycode ) {
          case KeyEvent.KEY_TAB:
          case KeyEvent.KEY_BACKSPACE:
            return true;
          case KeyEvent.KEY_ENTER:
          case KeyEvent.KEY_NUMENTER:
          case KeyEvent.KEY_ESC:
            return isEditing();
          default:
            return false;
        }
    }

    /**
    Edits the boxes as requested by a key press.
    @param event key press
    @return true if the event was used (see `consumesKey`)
    */
    public boolean processKeyPressedEvent(KeyEvent event)
    {
        if ( !consumesKey(event) ) {
            return false;
        }

        int digit = digitOf(event);

        if ( digit >= 0 ) {
            appendToSelected((char)('0' + digit));
        }
        else if ( isDecimalPoint(event) ) {
            appendDecimalPoint();
        }
        else if ( isMinus(event) ) {
            toggleSignOfSelected();
        }
        else if ( stepOf(event) != 0.0 ) {
            stepSelected(stepOf(event));
        }
        else {
            switch ( event.keycode ) {
              case KeyEvent.KEY_TAB:
                if ( (event.modifierMask & KeyEvent.MASK_SHIFT) != 0 ) {
                    selectPreviousField();
                }
                else {
                    selectNextField();
                }
                break;
              case KeyEvent.KEY_BACKSPACE:
                deleteLastCharacterOfSelected();
                break;
              case KeyEvent.KEY_ESC:
                cancelEditing();
                break;
              case KeyEvent.KEY_ENTER:
              case KeyEvent.KEY_NUMENTER:
                commitPending = true;
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
    public boolean consumeCommit()
    {
        boolean commit = commitPending;

        commitPending = false;
        return commit;
    }

    /**
    @return the values of the boxes, replacing the ones of the boxes edited by
    the numbers typed. Texts that are not a number (i.e. empty ones) are
    ignored.
    */
    public double[] getValuesWithEdits()
    {
        double[] result = values.clone();

        for ( int i = 0; i < result.length; i++ ) {
            Double value = parse(editTexts[i]);

            if ( value != null ) {
                result[i] = value;
            }
        }
        return result;
    }

    /**
    @param text text typed in a box, or null
    @return the number it represents, or null if it is not a valid number
    */
    public static Double parse(String text)
    {
        if ( text == null || text.isEmpty() ) {
            return null;
        }
        try {
            double value = Double.parseDouble(text);

            if ( Double.isNaN(value) || Double.isInfinite(value) ) {
                return null;
            }
            return value;
        }
        catch ( NumberFormatException e ) {
            return null;
        }
    }

    //= Key classification and text edition ===============================

    /**
    @return 0..9 if the event is a digit key (main or numeric keypad), or -1
    */
    private static int digitOf(KeyEvent event)
    {
        if ( event.unicodeId >= '0' && event.unicodeId <= '9' ) {
            return event.unicodeId - '0';
        }
        if ( event.keycode >= KeyEvent.KEY_0 && event.keycode <= KeyEvent.KEY_9 ) {
            return event.keycode - KeyEvent.KEY_0;
        }
        if ( event.keycode >= KeyEvent.KEY_NUM0 && event.keycode <= KeyEvent.KEY_NUM9 ) {
            return event.keycode - KeyEvent.KEY_NUM0;
        }
        return -1;
    }

    /**
    The comma is accepted too, as it is the decimal separator of many keyboard
    layouts.
    */
    private static boolean isDecimalPoint(KeyEvent event)
    {
        return event.unicodeId == '.' || event.unicodeId == ',' ||
            event.keycode == KeyEvent.KEY_PERIOD ||
            event.keycode == KeyEvent.KEY_NUMPERIOD ||
            event.keycode == KeyEvent.KEY_COMMA;
    }

    private static boolean isMinus(KeyEvent event)
    {
        return event.unicodeId == '-' || event.keycode == KeyEvent.KEY_MINUS ||
            event.keycode == KeyEvent.KEY_NUMMINUS;
    }

    /**
    @return the increment the key requests for the selected box, or 0 if it is
    not a stepping key
    */
    private double stepOf(KeyEvent event)
    {
        return switch ( event.keycode ) {
            case KeyEvent.KEY_RIGHT -> valueChangeRules.getLevelOneStep();
            case KeyEvent.KEY_LEFT -> -valueChangeRules.getLevelOneStep();
            case KeyEvent.KEY_UP -> valueChangeRules.getLevelTwoStep();
            case KeyEvent.KEY_DOWN -> -valueChangeRules.getLevelTwoStep();
            case KeyEvent.KEY_PAGEUP -> valueChangeRules.getLevelThreeStep();
            case KeyEvent.KEY_PAGEDOWN -> -valueChangeRules.getLevelThreeStep();
            default -> 0.0;
        };
    }

    /**
    Adds the step to the number the selected box shows (the one being typed, if
    it is valid, or its value), restricted as `getValueChangeRules()`
    describes (grid, circular range), and accepts it right away, as ENTER does
    (so any other box being edited is accepted too).
    */
    private void stepSelected(double step)
    {
        Double typed = parse(editTexts[selectedField]);
        double current = typed != null ? typed : values[selectedField];
        double next = valueChangeRules.applyStep(current, step);
        // Rounded, so repeated small steps do not accumulate binary noise
        BigDecimal stepped = BigDecimal.valueOf(next)
            .setScale(9, RoundingMode.HALF_UP).stripTrailingZeros();

        editTexts[selectedField] = stepped.toPlainString();
        commitPending = true;
    }

    private void appendToSelected(char character)
    {
        String text = editTexts[selectedField];

        if ( text == null ) {
            text = "";
        }
        if ( text.length() >= MAX_EDIT_LENGTH ) {
            return;
        }
        if ( limitTypedDecimals ) {
            int point = text.indexOf('.');

            if ( point >= 0 && text.length() - point - 1 >= decimals ) {
                return;
            }
        }
        editTexts[selectedField] = text + character;
    }

    private void appendDecimalPoint()
    {
        String text = editTexts[selectedField];

        if ( text == null ) {
            text = "";
        }
        if ( text.indexOf('.') >= 0 || (limitTypedDecimals && decimals == 0) ) {
            return;
        }
        if ( text.isEmpty() || text.equals("-") ) {
            text += "0";
        }
        if ( text.length() >= MAX_EDIT_LENGTH ) {
            return;
        }
        editTexts[selectedField] = text + ".";
    }

    /**
    Over a box that is not being edited, it starts a negative number.
    */
    private void toggleSignOfSelected()
    {
        String text = editTexts[selectedField];

        if ( text == null ) {
            text = "";
        }
        if ( text.startsWith("-") ) {
            editTexts[selectedField] = text.substring(1);
        }
        else if ( text.length() < MAX_EDIT_LENGTH ) {
            editTexts[selectedField] = "-" + text;
        }
    }

    /**
    Over a box that is not being edited, the deletion starts from the text the
    box shows.
    */
    private void deleteLastCharacterOfSelected()
    {
        String text = getDisplayText(selectedField);

        if ( !text.isEmpty() ) {
            text = text.substring(0, text.length() - 1);
        }
        editTexts[selectedField] = text;
    }
}
