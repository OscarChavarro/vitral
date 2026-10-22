#include "java/lang/NumberFormatException.h"
#include "java/util/ArrayList.txx"
#include "gui/xt/XtModifyPanelHost.h"
#include "vsdk/toolkit/gui/XtPanelWidgets.h"
#include "gui/xt/editor/XtModifyPanelForFunctionalExplicitSurface.h"
#include "vsdk/toolkit/common/logging/Logger.h"

static const int MARGIN = 8;
static const int ROW_HEIGHT = 24;
static const int ROW_SPACING = 4;
static const int LABEL_WIDTH = 96;

XtModifyPanelForFunctionalExplicitSurface::XtModifyPanelForFunctionalExplicitSurface(
    XtModifyPanelHost* parent)
    : XtModifyPanel(parent, NULL, 0), editor(NULL)
{
    for (int i = 0; i < FunctionalExplicitSurfaceEditor::PARAMETER_COUNT; ++i)
        fields[i] = NULL;
}

XtModifyPanelForFunctionalExplicitSurface::~XtModifyPanelForFunctionalExplicitSurface()
{
    delete editor;
}

void XtModifyPanelForFunctionalExplicitSurface::notifyTargetBeginEdit(
    SimpleBody* target, Widget parentPanel, int panelWidth)
{
    XtPanelWidgets* widgets = parent->getPanelWidgets();
    XFontSet fontSet = parent->getPanelFontSet();
    const int fullWidth = panelWidth - 2 * MARGIN;
    int y = MARGIN;

    this->target = target;
    delete editor;
    editor = new FunctionalExplicitSurfaceEditor(target);

    //-----------------------------------------------------------------
    widgets->createLabel(parentPanel, FunctionalExplicitSurfaceEditor::TITLE,
                         fontSet, XtPanelWidgets::CENTER,
                         MARGIN, y, fullWidth, ROW_HEIGHT);
    y += ROW_HEIGHT + ROW_SPACING;

    //-----------------------------------------------------------------
    widgets->createLabel(parentPanel,
                         FunctionalExplicitSurfaceEditor::PRESETS_LABEL,
                         fontSet, XtPanelWidgets::LEFT,
                         MARGIN, y, fullWidth, ROW_HEIGHT);
    y += ROW_HEIGHT + ROW_SPACING;

    ArrayList<String> presetNames =
        FunctionalExplicitSurfaceEditor::getPresets();
    presets.clear();
    for (long i = 0; i < presetNames.size(); ++i)
        presets.add(presetNames.get(i).c_str());
    widgets->createOptionButton(
        parentPanel, FunctionalExplicitSurfaceEditor::NO_PRESET, presets,
        fontSet, MARGIN, y, fullWidth, ROW_HEIGHT,
        &XtModifyPanelForFunctionalExplicitSurface::presetCallback, this);
    y += ROW_HEIGHT + ROW_SPACING;

    //-----------------------------------------------------------------
    for (int i = 0; i < FunctionalExplicitSurfaceEditor::PARAMETER_COUNT; ++i) {
        FunctionalExplicitSurfaceEditor::Parameter parameter =
            ((FunctionalExplicitSurfaceEditor::Parameter)(i));
        const String label =
            FunctionalExplicitSurfaceEditor::getLabel(parameter).c_str();
        const String value = editor->getValue(parameter).c_str();

        if (parameter == FunctionalExplicitSurfaceEditor::FUNCTION) {
            // The function is long: its label goes above it
            widgets->createLabel(parentPanel, label, fontSet,
                                 XtPanelWidgets::LEFT,
                                 MARGIN, y, fullWidth, ROW_HEIGHT);
            y += ROW_HEIGHT + ROW_SPACING;
            fields[i] = widgets->createTextField(
                parentPanel, value, fontSet, MARGIN, y, fullWidth,
                ROW_HEIGHT,
                &XtModifyPanelForFunctionalExplicitSurface::fieldCallback, this);
            y += ROW_HEIGHT + ROW_SPACING;
            continue;
        }
        widgets->createLabel(parentPanel, label, fontSet,
                             XtPanelWidgets::RIGHT,
                             MARGIN, y, LABEL_WIDTH, ROW_HEIGHT);
        fields[i] = widgets->createTextField(
            parentPanel, value, fontSet, MARGIN + LABEL_WIDTH, y,
            fullWidth - LABEL_WIDTH, ROW_HEIGHT,
            &XtModifyPanelForFunctionalExplicitSurface::fieldCallback, this);
        y += ROW_HEIGHT + ROW_SPACING;
    }
}

void XtModifyPanelForFunctionalExplicitSurface::refreshFields()
{
    for (int i = 0; i < FunctionalExplicitSurfaceEditor::PARAMETER_COUNT; ++i) {
        if (fields[i] == NULL) continue;
        parent->getPanelWidgets()->setText(fields[i], editor->getValue(
            ((FunctionalExplicitSurfaceEditor::Parameter)(i))).c_str());
    }
}

void XtModifyPanelForFunctionalExplicitSurface::presetSelected(
    const String& preset)
{
    if (editor == NULL) return;
    try {
        if (!editor->applyPreset(preset.c_str())) return;
    }
    catch (const NumberFormatException& e) {
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
    if (editor == NULL) return;
    XtPanelWidgets* widgets = parent->getPanelWidgets();
    for (int i = 0; i < FunctionalExplicitSurfaceEditor::PARAMETER_COUNT; ++i) {
        if (fields[i] != field) continue;
        try {
            editor->setValue(
                ((FunctionalExplicitSurfaceEditor::Parameter)(i)),
                widgets->getText(field).c_str());
        }
        catch (const NumberFormatException& e) {
            Logger::reportMessage("XtModifyPanelForFunctionalExplicitSurface",
                                  Logger::WARNING, "fieldActivated",
                                  e.getMessage());
            widgets->setInvalid(field, true);
            return;
        }
        widgets->setInvalid(field, false);
        parent->repaintDrawingArea();
        return;
    }
}

void XtModifyPanelForFunctionalExplicitSurface::presetCallback(
    Widget, int index, void* clientData)
{
    XtModifyPanelForFunctionalExplicitSurface* self =
        ((XtModifyPanelForFunctionalExplicitSurface*)(clientData));
    if (index >= 0 && index < ((int)(self->presets.size())))
        self->presetSelected(self->presets[index]);
}

void XtModifyPanelForFunctionalExplicitSurface::fieldCallback(
    Widget field, void* clientData)
{
    ((XtModifyPanelForFunctionalExplicitSurface*)(clientData))
        ->fieldActivated(field);
}
