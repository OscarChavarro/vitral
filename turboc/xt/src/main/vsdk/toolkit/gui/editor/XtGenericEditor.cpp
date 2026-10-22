#include <ctype.h>
#include "java/lang/String.h"

#include "vsdk/toolkit/gui/editor/XtGenericEditor.h"
#include "vsdk/toolkit/gui/XtPanelWidgets.h"
#include "vsdk/toolkit/gui/XtWidgetSupport.h"
#include "vsdk/toolkit/gui/editor/ControlSpecification.h"

static const int MARGIN = 8;
static const int ROW_HEIGHT = 24;
static const int ROW_SPACING = 4;
static const int FIELD_WIDTH = 120;
static const char* const MESSAGE_COLOR = "red";

XtGenericEditor::XtGenericEditor(XtPanelWidgets* widgets, Widget container,
                                 XFontSet fontSet, int width)
    : widgets(widgets), container(container), fontSet(fontSet),
      width(width), nextY(0), messageLabel(NULL)
{
}

void XtGenericEditor::beginBuild(const String& title)
{
    XtWidgetSupport::removeAll(container);
    fieldWidgets.clear();
    fieldSpecifications.clear();
    nextY = MARGIN;

    String upper(title.c_str());
    for ( long i = 0; i < upper.size(); ++i)
        upper[i] = ((char)(toupper(((unsigned char)(upper[i])))));
    widgets->createLabel(container, upper + " EDITOR", fontSet,
                         XtPanelWidgets::CENTER, MARGIN, nextY,
                         width - 2 * MARGIN, ROW_HEIGHT);
    nextY += ROW_HEIGHT + ROW_SPACING;
    messageLabel = NULL;
}

void XtGenericEditor::addControl(const ControlSpecification* specification,
                                 const String& value)
{
    String label(specification->getLabel().c_str());
    if (!specification->getIntervalText().isEmpty())
        label += String(" ") + specification->getIntervalText().c_str();

    const int fieldX = width - MARGIN - FIELD_WIDTH;
    widgets->createLabel(container, label + ": ", fontSet,
                         XtPanelWidgets::RIGHT, MARGIN, nextY,
                         fieldX - MARGIN, ROW_HEIGHT);
    Widget field = widgets->createTextField(
        container, value.c_str(), fontSet, fieldX, nextY, FIELD_WIDTH,
        ROW_HEIGHT,
        &XtGenericEditor::fieldActivated, this);
    fieldWidgets.add(field);
    fieldSpecifications.add(specification);
    nextY += ROW_HEIGHT + ROW_SPACING;
}

void XtGenericEditor::endBuild()
{
    messageLabel = widgets->createLabel(
        container, " ", fontSet, XtPanelWidgets::LEFT, MARGIN, nextY,
        width - 2 * MARGIN, ROW_HEIGHT);
    widgets->setForeground(messageLabel, MESSAGE_COLOR);
    nextY += ROW_HEIGHT + ROW_SPACING;
    // The message given while building was shown before the label existed
    showValidationMessage(pendingMessage.isEmpty() ? (const char*)NULL :
                          pendingMessage.c_str());
}

void XtGenericEditor::showValidationMessage(const char* message)
{
    pendingMessage = message != NULL ? message : "";
    if (messageLabel != NULL)
        widgets->setLabel(messageLabel, message != NULL ? message : " ");
}

void XtGenericEditor::setControlValue(
    const ControlSpecification* specification, const String& value)
{
    for ( long i = 0; i < fieldWidgets.size(); ++i) {
        if (fieldSpecifications[i] == specification)
            widgets->setText(fieldWidgets[i], value.c_str());
    }
}

void XtGenericEditor::clearControls(const String& message)
{
    XtWidgetSupport::removeAll(container);
    fieldWidgets.clear();
    fieldSpecifications.clear();
    messageLabel = NULL;
    widgets->createLabel(container, message.c_str(), fontSet,
                         XtPanelWidgets::CENTER, MARGIN, MARGIN,
                         width - 2 * MARGIN, ROW_HEIGHT);
}

void XtGenericEditor::fieldActivated(Widget field, void* clientData)
{
    XtGenericEditor* self = ((XtGenericEditor*)(clientData));
    for ( long i = 0; i < self->fieldWidgets.size(); ++i) {
        if (self->fieldWidgets[i] != field) continue;
        // On success the entity emits UPDATED and `setControlValue`
        // refreshes the field
        if (!self->updateValue(self->fieldSpecifications[i],
                               self->widgets->getText(field).c_str()))
            self->widgets->setInvalid(field, true);
        return;
    }
}
