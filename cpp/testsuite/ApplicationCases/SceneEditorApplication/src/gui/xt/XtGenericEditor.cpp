#include <cctype>
#include <string>

#include "gui/xt/XtGenericEditor.h"
#include "gui/xt/XtPanelWidgets.h"
#include "vsdk/toolkit/gui/editor/ControlSpecification.h"

namespace {
const int MARGIN = 8;
const int ROW_HEIGHT = 24;
const int ROW_SPACING = 4;
const int FIELD_WIDTH = 120;
const char* const MESSAGE_COLOR = "red";
}

XtGenericEditor::XtGenericEditor(Widget container, XFontSet fontSet,
                                 int width)
    : container(container), fontSet(fontSet), width(width), nextY(0),
      messageLabel(nullptr)
{
}

void XtGenericEditor::beginBuild(const java::String& title)
{
    XtPanelWidgets::removeAll(container);
    fields.clear();
    nextY = MARGIN;

    std::string upper(title.c_str());
    for (size_t i = 0; i < upper.size(); ++i)
        upper[i] = static_cast<char>(std::toupper(static_cast<unsigned char>(upper[i])));
    XtPanelWidgets::createLabel(container, upper + " EDITOR", fontSet,
                                XtPanelWidgets::CENTER, MARGIN, nextY,
                                width - 2 * MARGIN, ROW_HEIGHT);
    nextY += ROW_HEIGHT + ROW_SPACING;
    messageLabel = nullptr;
}

void XtGenericEditor::addControl(const ControlSpecification* specification,
                                 const java::String& value)
{
    std::string label(specification->getLabel().c_str());
    if (!specification->getIntervalText().isEmpty())
        label += std::string(" ") + specification->getIntervalText().c_str();

    const int fieldX = width - MARGIN - FIELD_WIDTH;
    XtPanelWidgets::createLabel(container, label + ": ", fontSet,
                                XtPanelWidgets::RIGHT, MARGIN, nextY,
                                fieldX - MARGIN, ROW_HEIGHT);
    Widget field = XtPanelWidgets::createTextField(
        container, value.c_str(), fontSet, fieldX, nextY, FIELD_WIDTH,
        ROW_HEIGHT,
        &XtGenericEditor::fieldActivated, this);
    fields.push_back(std::make_pair(field, specification));
    nextY += ROW_HEIGHT + ROW_SPACING;
}

void XtGenericEditor::endBuild()
{
    messageLabel = XtPanelWidgets::createLabel(
        container, " ", fontSet, XtPanelWidgets::LEFT, MARGIN, nextY,
        width - 2 * MARGIN, ROW_HEIGHT);
    XtPanelWidgets::setForeground(messageLabel, MESSAGE_COLOR);
    nextY += ROW_HEIGHT + ROW_SPACING;
    // The message given while building was shown before the label existed
    showValidationMessage(pendingMessage.empty() ? nullptr :
                          pendingMessage.c_str());
}

void XtGenericEditor::showValidationMessage(const char* message)
{
    pendingMessage = message != nullptr ? message : "";
    if (messageLabel != nullptr)
        XtPanelWidgets::setLabel(messageLabel,
                                 message != nullptr ? message : " ");
}

void XtGenericEditor::setControlValue(
    const ControlSpecification* specification, const java::String& value)
{
    for (size_t i = 0; i < fields.size(); ++i) {
        if (fields[i].second == specification)
            XtPanelWidgets::setText(fields[i].first, value.c_str());
    }
}

void XtGenericEditor::clearControls(const java::String& message)
{
    XtPanelWidgets::removeAll(container);
    fields.clear();
    messageLabel = nullptr;
    XtPanelWidgets::createLabel(container, message.c_str(), fontSet,
                                XtPanelWidgets::CENTER, MARGIN, MARGIN,
                                width - 2 * MARGIN, ROW_HEIGHT);
}

void XtGenericEditor::fieldActivated(Widget field, void* clientData)
{
    XtGenericEditor* self = static_cast<XtGenericEditor*>(clientData);
    for (size_t i = 0; i < self->fields.size(); ++i) {
        if (self->fields[i].first != field) continue;
        // On success the entity emits UPDATED and `setControlValue`
        // refreshes the field
        if (!self->updateValue(self->fields[i].second,
                               XtPanelWidgets::getText(field).c_str()))
            XtPanelWidgets::setInvalid(field, true);
        return;
    }
}
