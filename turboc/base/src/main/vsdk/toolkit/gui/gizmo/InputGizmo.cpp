#include <math.h>
#include <stdio.h>
#include <stdlib.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/gui/gizmo/InputGizmo.h"
#include "vsdk/toolkit/common/Cpp98Support.h"

const ColorRgb InputGizmo::HIGHLIGHT_COLOR(1, 1, 0);

static const int MAX_EDIT_LENGTH = 12;

static BOOLEAN isZeroText(const String& text)
{
    // Matches "0*\.?0*"
    int i = 0;
    int n = text.length();
    while ( i < n && text.charAt(i) == '0' ) {
        i++;
    }
    if ( i < n && text.charAt(i) == '.' ) {
        i++;
    }
    while ( i < n && text.charAt(i) == '0' ) {
        i++;
    }
    return i == n;
}

static String repeatZeros(int n)
{
    String text;
    int i;
    for ( i = 0; i < n; i++ ) {
        text += "0";
    }
    return text;
}

InputGizmo::InputGizmo(int numberOfFields)
    : decimals(DECIMALS), integerDigits(1), limitTypedDecimals(FALSE),
      valueChangeRules(InputGizmoValueChangeRules::forTranslation())
{
    init(numberOfFields);
}

InputGizmo::InputGizmo(int numberOfFields, int decimals, int integerDigits)
    : decimals(decimals > 0 ? decimals : 0),
      integerDigits(integerDigits > 1 ? integerDigits : 1),
      limitTypedDecimals(TRUE),
      valueChangeRules(InputGizmoValueChangeRules::forTranslation())
{
    init(numberOfFields);
}

InputGizmo::InputGizmo(int numberOfFields, int decimals, int integerDigits,
                       const InputGizmoValueChangeRules& valueChangeRules)
    : decimals(decimals > 0 ? decimals : 0),
      integerDigits(integerDigits > 1 ? integerDigits : 1),
      limitTypedDecimals(TRUE), valueChangeRules(valueChangeRules)
{
    init(numberOfFields);
}

void InputGizmo::init(int numberOfFields)
{
    int count = numberOfFields > 1 ? numberOfFields : 1;
    int i;

    for ( i = 0; i < count; i++ ) {
        values.add(0.0);
        colors.add(ColorRgb(0, 0, 0));
        highlighted.add(FALSE);
        editTexts.add(String(""));
        editing.add(FALSE);
    }
    selectedField = 0;
    commitPending = FALSE;
}

InputGizmoValueChangeRules& InputGizmo::getValueChangeRules()
{
    return valueChangeRules;
}

void InputGizmo::setValueChangeRules(
    const InputGizmoValueChangeRules& valueChangeRules)
{
    this->valueChangeRules = valueChangeRules;
}

String InputGizmo::format(double value)
{
    return format(value, DECIMALS);
}

String InputGizmo::format(double value, int decimals)
{
    char buffer[128];
    snprintf(buffer, sizeof(buffer), "%.*f", decimals, value);
    String text(buffer);

    if ( text.startsWith("-") && isZeroText(text.substring(1)) ) {
        // A tiny negative value must not show as a "negative zero"
        return text.substring(1);
    }
    return text;
}

int InputGizmo::getDecimals() const
{
    return decimals;
}

String InputGizmo::getReferenceText() const
{
    String text = String("-") + repeatZeros(integerDigits);
    if ( decimals > 0 ) {
        text += ".";
        text += repeatZeros(decimals);
    }
    return text;
}

int InputGizmo::getNumberOfFields() const
{
    return (int)values.size();
}

double InputGizmo::getValue(int field) const
{
    return values[field];
}

void InputGizmo::setValue(int field, double value)
{
    values[field] = value;
}

const ColorRgb& InputGizmo::getFieldColor(int field) const
{
    return colors[field];
}

void InputGizmo::setFieldColor(int field, const ColorRgb& color)
{
    colors[field] = color;
}

BOOLEAN InputGizmo::isFieldHighlighted(int field) const
{
    return highlighted[field];
}

void InputGizmo::setFieldHighlighted(int field, BOOLEAN highlighted)
{
    this->highlighted[field] = highlighted;
}

ColorRgb InputGizmo::getFieldDisplayColor(int field) const
{
    return highlighted[field] ? HIGHLIGHT_COLOR : colors[field];
}

int InputGizmo::getSelectedField() const
{
    return selectedField;
}

void InputGizmo::setSelectedField(int field)
{
    if ( field >= 0 && field < values.size() ) {
        selectedField = field;
    }
}

void InputGizmo::selectNextField()
{
    selectedField = (selectedField + 1) % values.size();
}

void InputGizmo::selectPreviousField()
{
    selectedField = (selectedField + values.size() - 1) % values.size();
}

BOOLEAN InputGizmo::isEditing(int field) const
{
    return editing[field];
}

BOOLEAN InputGizmo::isEditing() const
{
    long i;
    for ( i = 0; i < editing.size(); i++ ) {
        if ( editing[i] ) {
            return TRUE;
        }
    }
    return FALSE;
}

String InputGizmo::getEditText(int field) const
{
    return editing[field] ? editTexts[field] : String("");
}

String InputGizmo::getDisplayText(int field) const
{
    if ( editing[field] ) {
        return editTexts[field];
    }
    return format(values[field], decimals);
}

void InputGizmo::cancelEditing()
{
    long i;
    for ( i = 0; i < editTexts.size(); i++ ) {
        editTexts[i] = "";
        editing[i] = FALSE;
    }
    commitPending = FALSE;
}

BOOLEAN InputGizmo::consumesKey(const KeyEvent& event) const
{
    if ( (event.modifierMask & (KeyEvent::MASK_CTRL | KeyEvent::MASK_ALT)) != 0 ) {
        return FALSE;
    }
    if ( digitOf(event) >= 0 || isDecimalPoint(event) || isMinus(event) ||
         stepOf(event) != 0.0 ) {
        return TRUE;
    }
    switch ( event.keycode ) {
      case KeyEvent::KEY_TAB:
      case KeyEvent::KEY_BACKSPACE:
        return TRUE;
      case KeyEvent::KEY_ENTER:
      case KeyEvent::KEY_NUMENTER:
      case KeyEvent::KEY_ESC:
        return isEditing();
      default:
        return FALSE;
    }
}

BOOLEAN InputGizmo::processKeyPressedEvent(const KeyEvent& event)
{
    if ( !consumesKey(event) ) {
        return FALSE;
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
          case KeyEvent::KEY_TAB:
            if ( (event.modifierMask & KeyEvent::MASK_SHIFT) != 0 ) {
                selectPreviousField();
            }
            else {
                selectNextField();
            }
            break;
          case KeyEvent::KEY_BACKSPACE:
            deleteLastCharacterOfSelected();
            break;
          case KeyEvent::KEY_ESC:
            cancelEditing();
            break;
          case KeyEvent::KEY_ENTER:
          case KeyEvent::KEY_NUMENTER:
            commitPending = TRUE;
            break;
          default:
            break;
        }
    }
    return TRUE;
}

BOOLEAN InputGizmo::consumeCommit()
{
    BOOLEAN commit = commitPending;

    commitPending = FALSE;
    return commit;
}

ArrayList<double> InputGizmo::getValuesWithEdits() const
{
    ArrayList<double> result(values);
    long i;

    for ( i = 0; i < result.size(); i++ ) {
        double value;

        if ( editing[i] && parse(editTexts[i], &value) ) {
            result[i] = value;
        }
    }
    return result;
}

BOOLEAN InputGizmo::parse(const String& text, double* outValue)
{
    if ( text.isEmpty() ) {
        return FALSE;
    }
    const char* start = text.c_str();
    char* end = NULL;
    double value = strtod(start, &end);

    if ( end == start ) {
        return FALSE;
    }
    while ( *end == ' ' || *end == '\t' ) {
        end++;
    }
    if ( *end != '\0' || Cpp98Support::isNaN(value) || Cpp98Support::isInfinite(value) ) {
        return FALSE;
    }
    *outValue = value;
    return TRUE;
}

int InputGizmo::digitOf(const KeyEvent& event)
{
    if ( event.unicodeId >= '0' && event.unicodeId <= '9' ) {
        return event.unicodeId - '0';
    }
    if ( event.keycode >= KeyEvent::KEY_0 && event.keycode <= KeyEvent::KEY_9 ) {
        return event.keycode - KeyEvent::KEY_0;
    }
    if ( event.keycode >= KeyEvent::KEY_NUM0 &&
         event.keycode <= KeyEvent::KEY_NUM9 ) {
        return event.keycode - KeyEvent::KEY_NUM0;
    }
    return -1;
}

BOOLEAN InputGizmo::isDecimalPoint(const KeyEvent& event)
{
    // The comma is accepted too, as it is the decimal separator of many
    // keyboard layouts.
    return event.unicodeId == '.' || event.unicodeId == ',' ||
        event.keycode == KeyEvent::KEY_PERIOD ||
        event.keycode == KeyEvent::KEY_NUMPERIOD ||
        event.keycode == KeyEvent::KEY_COMMA;
}

BOOLEAN InputGizmo::isMinus(const KeyEvent& event)
{
    return event.unicodeId == '-' || event.keycode == KeyEvent::KEY_MINUS ||
        event.keycode == KeyEvent::KEY_NUMMINUS;
}

double InputGizmo::stepOf(const KeyEvent& event) const
{
    switch ( event.keycode ) {
      case KeyEvent::KEY_RIGHT: return valueChangeRules.getLevelOneStep();
      case KeyEvent::KEY_LEFT: return -valueChangeRules.getLevelOneStep();
      case KeyEvent::KEY_UP: return valueChangeRules.getLevelTwoStep();
      case KeyEvent::KEY_DOWN: return -valueChangeRules.getLevelTwoStep();
      case KeyEvent::KEY_PAGEUP: return valueChangeRules.getLevelThreeStep();
      case KeyEvent::KEY_PAGEDOWN: return -valueChangeRules.getLevelThreeStep();
      default: return 0.0;
    }
}

void InputGizmo::stepSelected(double step)
{
    double typed;
    double current = values[selectedField];
    if ( editing[selectedField] && parse(editTexts[selectedField], &typed) ) {
        current = typed;
    }
    double next = valueChangeRules.applyStep(current, step);

    // Rounded, so repeated small steps do not accumulate binary noise
    // (plain notation, 9 decimals at most, no trailing zeros)
    char buffer[128];
    snprintf(buffer, sizeof(buffer), "%.9f", next);
    String stepped(buffer);
    if ( stepped.indexOf('.') >= 0 ) {
        int end = stepped.length();
        while ( end > 0 && stepped.charAt(end - 1) == '0' ) {
            end--;
        }
        if ( end > 0 && stepped.charAt(end - 1) == '.' ) {
            end--;
        }
        stepped = stepped.substring(0, end);
    }
    if ( stepped.equals("-0") ) {
        stepped = "0";
    }

    setSelectedEditText(stepped);
    commitPending = TRUE;
}

String InputGizmo::selectedEditTextOrEmpty() const
{
    if ( editing[selectedField] ) {
        return editTexts[selectedField];
    }
    return String("");
}

void InputGizmo::setSelectedEditText(const String& text)
{
    editTexts[selectedField] = text;
    editing[selectedField] = TRUE;
}

void InputGizmo::appendToSelected(char character)
{
    String text = selectedEditTextOrEmpty();

    if ( text.length() >= MAX_EDIT_LENGTH ) {
        return;
    }
    if ( limitTypedDecimals ) {
        int point = text.indexOf('.');

        if ( point >= 0 && text.length() - point - 1 >= decimals ) {
            return;
        }
    }
    char buffer[2] = {character, '\0'};
    setSelectedEditText(text + buffer);
}

void InputGizmo::appendDecimalPoint()
{
    String text = selectedEditTextOrEmpty();

    if ( text.indexOf('.') >= 0 || (limitTypedDecimals && decimals == 0) ) {
        return;
    }
    if ( text.isEmpty() || text.equals("-") ) {
        text += "0";
    }
    if ( text.length() >= MAX_EDIT_LENGTH ) {
        return;
    }
    setSelectedEditText(text + ".");
}

void InputGizmo::toggleSignOfSelected()
{
    // Over a box that is not being edited, it starts a negative number.
    String text = selectedEditTextOrEmpty();

    if ( text.startsWith("-") ) {
        setSelectedEditText(text.substring(1));
    }
    else if ( text.length() < MAX_EDIT_LENGTH ) {
        setSelectedEditText(String("-") + text);
    }
}

void InputGizmo::deleteLastCharacterOfSelected()
{
    // Over a box that is not being edited, the deletion starts from the
    // text the box shows.
    String text = getDisplayText(selectedField);

    if ( !text.isEmpty() ) {
        text = text.substring(0, text.length() - 1);
    }
    setSelectedEditText(text);
}

const Class InputGizmo::CLASS = { "InputGizmo", &Gizmo::CLASS };

const Class *
InputGizmo::getClass() const
{
    return &CLASS;
}
