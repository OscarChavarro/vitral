#ifndef __XT_COLLAPSIBLE_SECTION__
#define __XT_COLLAPSIBLE_SECTION__

#include <string>

#include <X11/Intrinsic.h>

class XtPanelWidgets;

/**
Section of a side panel with a title that shows or hides its content when
clicked, as `AwtCollapsibleSection`. The section is a container placed at
an explicit position: its header is on its top and its content below it.

C++ port note: the Xt panels place their children at explicit positions
(there is no `BoxLayout`), so the sections do not move by themselves when
one of them changes its height: the owner restacks them from the layout
listener (see `stack`). The font set of the side panels has no tree marks,
so the header shows "[-]" (expanded) or "[+]" (collapsed) before the title.
*/
class XtCollapsibleSection {
public:
    /**
    Called after a section showed or hid its content, so its owner can
    restack the sections.
    @param section the section whose height changed
    @param clientData the data given with `setLayoutListener`
    */
    typedef void (*LayoutProc)(XtCollapsibleSection* section,
                               void* clientData);

    /**
    Height of the header of the sections.
    */
    static const int HEADER_HEIGHT = 24;

    /**
    @param widgets builder of the widgets of the widget set in use
    @param parent container of the section
    @param title text of the header of the section
    @param fontSet font set of the header
    @param y position of the section in its parent
    @param width width of the section
    @param expanded true if the section starts showing its content
    */
    XtCollapsibleSection(XtPanelWidgets* widgets, Widget parent,
                         const std::string& title, XFontSet fontSet,
                         int y, int width, bool expanded);

    /**
    The widgets of the section are destroyed with their parent.
    */
    ~XtCollapsibleSection();

    /**
    @return the container where the content of the section is built, at
    (0, 0): `setContent` places it below the header
    */
    Widget getContainer() const;

    /**
    @param content widget shown or hidden by the section, a child of the
    container of the section
    */
    void setContent(Widget content);

    /**
    @return true if the section shows its content
    */
    bool isExpanded() const;

    /**
    @param expanded true to show the content of the section, false to hide
    it
    */
    void setExpanded(bool expanded);

    /**
    @return height of the section: its header, and its content when it is
    expanded
    */
    int getHeight() const;

    /**
    @param y new position of the section in its parent
    */
    void moveTo(int y);

    /**
    @param listener called after the section shows or hides its content
    @param clientData data given to the listener
    */
    void setLayoutListener(LayoutProc listener, void* clientData);

    /**
    Places the sections one below the other from the given position, so
    the sections stacked in a panel keep together at its top when they are
    collapsed.
    @param sections sections to place, from top to bottom
    @param count number of sections
    @param y position of the first section
    */
    static void stack(XtCollapsibleSection* const* sections, int count,
                      int y);

private:
    XtPanelWidgets* widgets;
    Widget container;
    Widget header;
    Widget content;
    std::string title;
    int width;
    int contentHeight;
    bool expanded;
    bool handCursorDefined;
    LayoutProc layoutListener;
    void* layoutClientData;

    void updateHeader();
    void updateGeometry();

    static void headerEvent(Widget widget, XtPointer clientData,
                            XEvent* event, Boolean* continueDispatch);

    XtCollapsibleSection(const XtCollapsibleSection& other);
    XtCollapsibleSection& operator=(const XtCollapsibleSection& other);
};

#endif
