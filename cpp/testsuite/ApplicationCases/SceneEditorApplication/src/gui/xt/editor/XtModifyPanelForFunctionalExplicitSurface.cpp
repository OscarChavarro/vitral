#include <X11/StringDefs.h>
#include <X11/Xaw/MenuButton.h>
#include <X11/Xaw/SmeBSB.h>

#include "java/lang/NumberFormatException.h"
#include "java/util/ArrayList.txx"
#include "gui/xt/XtModifyPanelHost.h"
#include "gui/xt/XtPanelWidgets.h"
#include "gui/xt/editor/XtModifyPanelForFunctionalExplicitSurface.h"
#include "vsdk/toolkit/common/logging/Logger.h"

namespace {
const int MARGIN = 8;
const int ROW_HEIGHT = 24;
const int ROW_SPACING = 4;
const int LABEL_WIDTH = 96;
const char* const PRESETS_MENU = "functionalExplicitSurfacePresets";
}

XtModifyPanelForFunctionalExplicitSurface::XtModifyPanelForFunctionalExplicitSurface(
    XtModifyPanelHost* parent)
    : XtModifyPanel(parent, nullptr, 0), editor(nullptr),
      presetsButton(nullptr)
{
    for (int i = 0; i < FunctionalExplicitSurfaceEditor::PARAMETER_COUNT; ++i)
        fields[i] = nullptr;
}

XtModifyPanelForFunctionalExplicitSurface::~XtModifyPanelForFunctionalExplicitSurface()
{
    delete editor;
}

void XtModifyPanelForFunctionalExplicitSurface::notifyTargetBeginEdit(
    SimpleBody* target, Widget parentPanel, int panelWidth)
{
    XFontSet fontSet = parent->getPanelFontSet();
    const int fullWidth = panelWidth - 2 * MARGIN;
    int y = MARGIN;

    this->target = target;
    delete editor;
    editor = new FunctionalExplicitSurfaceEditor(target);

    //-----------------------------------------------------------------
    XtPanelWidgets::createLabel(parentPanel,
                                FunctionalExplicitSurfaceEditor::TITLE,
                                fontSet, XtPanelWidgets::CENTER,
                                MARGIN, y, fullWidth, ROW_HEIGHT);
    y += ROW_HEIGHT + ROW_SPACING;

    //-----------------------------------------------------------------
    XtPanelWidgets::createLabel(parentPanel,
                                FunctionalExplicitSurfaceEditor::PRESETS_LABEL,
                                fontSet, XtPanelWidgets::LEFT,
                                MARGIN, y, fullWidth, ROW_HEIGHT);
    y += ROW_HEIGHT + ROW_SPACING;

    Arg args[9]; Cardinal n = 0;
    XtSetArg(args[n], XtNlabel, FunctionalExplicitSurfaceEditor::NO_PRESET); ++n;
    XtSetArg(args[n], XtNmenuName, PRESETS_MENU); ++n;
    XtSetArg(args[n], XtNjustify, XtJustifyLeft); ++n;
    XtSetArg(args[n], XtNinternational, True); ++n;
    XtSetArg(args[n], XtNfontSet, fontSet); ++n;
    XtSetArg(args[n], XtNx, MARGIN); ++n;
    XtSetArg(args[n], XtNy, y); ++n;
    XtSetArg(args[n], XtNwidth, fullWidth); ++n;
    XtSetArg(args[n], XtNheight, ROW_HEIGHT); ++n;
    presetsButton = XtCreateManagedWidget("presetsButton",
        menuButtonWidgetClass, parentPanel, args, n);
    y += ROW_HEIGHT + ROW_SPACING;

    // The menu is a popup child of its button, destroyed with it
    Widget menu = parent->createPopupMenu(presetsButton, PRESETS_MENU);
    java::ArrayList<java::String> presets =
        FunctionalExplicitSurfaceEditor::getPresets();
    presetBindings.clear();
    presetBindings.resize(presets.size());
    for (long i = 0; i < presets.size(); ++i) {
        presetBindings[i].panel = this;
        presetBindings[i].preset = presets.get(i).c_str();
        Arg itemArgs[3]; Cardinal itemN = 0;
        XtSetArg(itemArgs[itemN], XtNlabel, presetBindings[i].preset.c_str()); ++itemN;
        XtSetArg(itemArgs[itemN], XtNinternational, True); ++itemN;
        XtSetArg(itemArgs[itemN], XtNfontSet, fontSet); ++itemN;
        Widget item = XtCreateManagedWidget("presetItem", smeBSBObjectClass,
                                            menu, itemArgs, itemN);
        XtAddCallback(item, XtNcallback,
                      &XtModifyPanelForFunctionalExplicitSurface::presetCallback,
                      &presetBindings[i]);
    }

    //-----------------------------------------------------------------
    for (int i = 0; i < FunctionalExplicitSurfaceEditor::PARAMETER_COUNT; ++i) {
        FunctionalExplicitSurfaceEditor::Parameter parameter =
            static_cast<FunctionalExplicitSurfaceEditor::Parameter>(i);
        const std::string label =
            FunctionalExplicitSurfaceEditor::getLabel(parameter).c_str();
        const std::string value = editor->getValue(parameter).c_str();

        if (parameter == FunctionalExplicitSurfaceEditor::FUNCTION) {
            // The function is long: its label goes above it
            XtPanelWidgets::createLabel(parentPanel, label, fontSet,
                                        XtPanelWidgets::LEFT,
                                        MARGIN, y, fullWidth, ROW_HEIGHT);
            y += ROW_HEIGHT + ROW_SPACING;
            fields[i] = XtPanelWidgets::createTextField(
                parentPanel, value, fontSet, MARGIN, y, fullWidth,
                ROW_HEIGHT,
                &XtModifyPanelForFunctionalExplicitSurface::fieldCallback, this);
            y += ROW_HEIGHT + ROW_SPACING;
            continue;
        }
        XtPanelWidgets::createLabel(parentPanel, label, fontSet,
                                    XtPanelWidgets::RIGHT,
                                    MARGIN, y, LABEL_WIDTH, ROW_HEIGHT);
        fields[i] = XtPanelWidgets::createTextField(
            parentPanel, value, fontSet, MARGIN + LABEL_WIDTH, y,
            fullWidth - LABEL_WIDTH, ROW_HEIGHT,
            &XtModifyPanelForFunctionalExplicitSurface::fieldCallback, this);
        y += ROW_HEIGHT + ROW_SPACING;
    }
}

void XtModifyPanelForFunctionalExplicitSurface::refreshFields()
{
    for (int i = 0; i < FunctionalExplicitSurfaceEditor::PARAMETER_COUNT; ++i) {
        if (fields[i] == nullptr) continue;
        XtPanelWidgets::setText(fields[i], editor->getValue(
            static_cast<FunctionalExplicitSurfaceEditor::Parameter>(i)).c_str());
    }
}

void XtModifyPanelForFunctionalExplicitSurface::presetSelected(
    const std::string& preset)
{
    if (editor == nullptr) return;
    XtPanelWidgets::setLabel(presetsButton, preset);
    try {
        if (!editor->applyPreset(preset.c_str())) return;
    }
    catch (const java::NumberFormatException& e) {
        Logger::reportMessage("XtModifyPanelForFunctionalExplicitSurface",
                              Logger::WARNING, "presetSelected",
                              e.getMessage());
        return;
    }
    refreshFields();
    parent->repaintDrawingArea();
}

void XtModifyPanelForFunctionalExplicitSurface::fieldActivated(Widget field)
{
    if (editor == nullptr) return;
    for (int i = 0; i < FunctionalExplicitSurfaceEditor::PARAMETER_COUNT; ++i) {
        if (fields[i] != field) continue;
        try {
            editor->setValue(
                static_cast<FunctionalExplicitSurfaceEditor::Parameter>(i),
                XtPanelWidgets::getText(field).c_str());
        }
        catch (const java::NumberFormatException& e) {
            Logger::reportMessage("XtModifyPanelForFunctionalExplicitSurface",
                                  Logger::WARNING, "fieldActivated",
                                  e.getMessage());
            XtPanelWidgets::setInvalid(field, true);
            return;
        }
        XtPanelWidgets::setInvalid(field, false);
        parent->repaintDrawingArea();
        return;
    }
}

void XtModifyPanelForFunctionalExplicitSurface::presetCallback(
    Widget, XtPointer clientData, XtPointer)
{
    PresetBinding* binding = static_cast<PresetBinding*>(clientData);
    binding->panel->presetSelected(binding->preset);
}

void XtModifyPanelForFunctionalExplicitSurface::fieldCallback(
    Widget field, void* clientData)
{
    static_cast<XtModifyPanelForFunctionalExplicitSurface*>(clientData)
        ->fieldActivated(field);
}
