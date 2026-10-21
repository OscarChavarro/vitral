#ifndef __XT_MODIFY_PANEL_FOR_FUNCTIONAL_EXPLICIT_SURFACE__
#define __XT_MODIFY_PANEL_FOR_FUNCTIONAL_EXPLICIT_SURFACE__

#include <string>
#include <vector>

#include "gui/xt/XtModifyPanel.h"
#include "model/editor/FunctionalExplicitSurfaceEditor.h"

/**
Xt presentation of a `FunctionalExplicitSurfaceEditor`, as
`AwtModifyPanelForFunctionalExplicitSurface` is for Swing: an option button
of predefined configurations (the Swing combo box) and one text field per
parameter. What the user types (confirmed with Return) is passed
to the editor, which changes the body.
*/
class XtModifyPanelForFunctionalExplicitSurface : public XtModifyPanel {
private:
    FunctionalExplicitSurfaceEditor* editor;
    Widget fields[FunctionalExplicitSurfaceEditor::PARAMETER_COUNT];
    /// Names of the options of the presets button
    std::vector<std::string> presets;

    void refreshFields();
    void presetSelected(const std::string& preset);
    void fieldActivated(Widget field);

    static void presetCallback(Widget, int index, void* clientData);
    static void fieldCallback(Widget field, void* clientData);

public:
    explicit XtModifyPanelForFunctionalExplicitSurface(XtModifyPanelHost* parent);
    virtual ~XtModifyPanelForFunctionalExplicitSurface();

    /**
    Builds the editor for a body whose geometry is a
    `FunctionalExplicitSurface`.
    @param target body to edit
    @param parentPanel panel of the modify panel, where the widgets go
    @param panelWidth width of that Composite
    */
    void notifyTargetBeginEdit(SimpleBody* target, Widget parentPanel,
                               int panelWidth);
};

#endif
