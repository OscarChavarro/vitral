#include <X11/IntrinsicP.h>
#include <X11/StringDefs.h>
#include <X11/cursorfont.h>

#include "gui/xt/XtCollapsibleSection.h"
#include "vsdk/toolkit/gui/XtPanelWidgets.h"

XtCollapsibleSection::XtCollapsibleSection(XtPanelWidgets* widgets,
                                           Widget parent,
                                           const std::string& title,
                                           XFontSet fontSet, int y,
                                           int width, bool expanded)
    : widgets(widgets)
    , container(nullptr)
    , header(nullptr)
    , content(nullptr)
    , title(title)
    , width(width)
    , contentHeight(0)
    , expanded(expanded)
    , handCursorDefined(false)
    , layoutListener(nullptr)
    , layoutClientData(nullptr)
{
    container = widgets->createPanel(parent, "collapsibleSection", 0, y,
                                     width, HEADER_HEIGHT, true);
    header = widgets->createLabel(container, "", fontSet,
                                  XtPanelWidgets::LEFT, 4, 0, width - 8,
                                  HEADER_HEIGHT);
    // Labels have no activation callback in every widget set: the clicks
    // are taken with an event handler of the Intrinsics
    XtAddEventHandler(header, ButtonReleaseMask | EnterWindowMask, False,
                      &XtCollapsibleSection::headerEvent,
                      reinterpret_cast<XtPointer>(this));
    updateHeader();
}

XtCollapsibleSection::~XtCollapsibleSection()
{
}

Widget XtCollapsibleSection::getContainer() const
{
    return container;
}

void XtCollapsibleSection::setContent(Widget content)
{
    Dimension height = 0;

    this->content = content;
    contentHeight = 0;
    if ( content != nullptr ) {
        XtVaGetValues(content, XtNheight, &height, nullptr);
        contentHeight = height;
        XtMoveWidget(content, 0, HEADER_HEIGHT);
    }
    updateGeometry();
}

bool XtCollapsibleSection::isExpanded() const
{
    return expanded;
}

void XtCollapsibleSection::setExpanded(bool expanded)
{
    this->expanded = expanded;
    updateHeader();
    updateGeometry();
    if ( layoutListener != nullptr ) {
        layoutListener(this, layoutClientData);
    }
}

int XtCollapsibleSection::getHeight() const
{
    return HEADER_HEIGHT + (expanded ? contentHeight : 0);
}

void XtCollapsibleSection::moveTo(int y)
{
    XtMoveWidget(container, 0, y);
}

void XtCollapsibleSection::setLayoutListener(LayoutProc listener,
                                             void* clientData)
{
    layoutListener = listener;
    layoutClientData = clientData;
}

void XtCollapsibleSection::stack(XtCollapsibleSection* const* sections,
                                 int count, int y)
{
    for ( int i = 0; i < count; i++ ) {
        if ( sections[i] == nullptr ) continue;
        sections[i]->moveTo(y);
        y += sections[i]->getHeight();
    }
}

void XtCollapsibleSection::updateHeader()
{
    widgets->setLabel(header, (expanded ? "[-] " : "[+] ") + title);
}

void XtCollapsibleSection::updateGeometry()
{
    // The panels place their children by themselves: the geometry changes
    // are applied directly, without asking the parent
    if ( content != nullptr ) {
        if ( expanded ) {
            XtManageChild(content);
        }
        else {
            XtUnmanageChild(content);
        }
    }
    XtResizeWidget(container, static_cast<Dimension>(width),
                   static_cast<Dimension>(getHeight()), 0);
}

void XtCollapsibleSection::headerEvent(Widget widget, XtPointer clientData,
                                       XEvent* event, Boolean*)
{
    XtCollapsibleSection* self =
        reinterpret_cast<XtCollapsibleSection*>(clientData);
    if ( self == nullptr || event == nullptr ) return;

    if ( event->type == EnterNotify ) {
        // The window of the header exists once it is realized: the hand
        // cursor is set the first time the pointer enters it
        if ( !self->handCursorDefined && XtIsRealized(widget) ) {
            Display* display = XtDisplay(widget);
            Cursor hand = XCreateFontCursor(display, XC_hand2);
            // The window keeps the cursor after it is freed
            XDefineCursor(display, XtWindow(widget), hand);
            XFreeCursor(display, hand);
            self->handCursorDefined = true;
        }
        return;
    }
    if ( event->type == ButtonRelease && event->xbutton.button == Button1 ) {
        self->setExpanded(!self->isExpanded());
    }
}
