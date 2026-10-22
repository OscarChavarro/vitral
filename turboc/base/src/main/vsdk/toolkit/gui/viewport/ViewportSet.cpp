#include <stddef.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportElementScaler.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "vsdk/toolkit/gui/viewport/ViewportSetCommands.h"
#include "vsdk/toolkit/gui/widget/Widget.h"
#include "vsdk/toolkit/gui/widget/WidgetCommand.h"
#include "vsdk/toolkit/gui/widget/WidgetMenu.h"
#include "vsdk/toolkit/gui/widget/WidgetMenuItem.h"

const char* const ViewportSetCommands::POPUP_PROJECTION_LOCATION =
    "VIEWPORT_SET_PROJECTION_LOCATION";
const char* const ViewportSetCommands::IDV_PROJECTION_LOCATION_PERSPECTIVE =
    "IDV_VIEWPORT_SET_PROJECTION_LOCATION_PERSPECTIVE";
const char* const ViewportSetCommands::IDV_PROJECTION_LOCATION_TOP =
    "IDV_VIEWPORT_SET_PROJECTION_LOCATION_TOP";
const char* const ViewportSetCommands::IDV_PROJECTION_LOCATION_BOTTOM =
    "IDV_VIEWPORT_SET_PROJECTION_LOCATION_BOTTOM";
const char* const ViewportSetCommands::IDV_PROJECTION_LOCATION_LEFT =
    "IDV_VIEWPORT_SET_PROJECTION_LOCATION_LEFT";
const char* const ViewportSetCommands::IDV_PROJECTION_LOCATION_FRONT =
    "IDV_VIEWPORT_SET_PROJECTION_LOCATION_FRONT";
const char* const ViewportSetCommands::POPUP_RENDER_MODE =
    "VIEWPORT_SET_RENDER_MODE";
const char* const ViewportSetCommands::IDV_RENDER_MODE_GPU =
    "IDV_VIEWPORT_SET_RENDER_MODE_GPU";
const char* const ViewportSetCommands::IDV_RENDER_MODE_CPU =
    "IDV_VIEWPORT_SET_RENDER_MODE_CPU";
const char* const ViewportSetCommands::IDV_RENDER_MODE_CPU_HIDDEN_LINES =
    "IDV_VIEWPORT_SET_RENDER_MODE_CPU_HIDDEN_LINES";


static const double T1 = 1.0 / 3.0;
static const double T2 = 2.0 / 3.0;
static const double H = 0.5;

// Each layout has one {startX, startY, sizeX, sizeY} row per viewport
// (rows beyond the number of viewports are unused)
static const double LAYOUTS_2[][4][4] = {
    {{0, 0, H, 1}, {H, 0, H, 1}},
    {{0, H, 1, H}, {0, 0, 1, H}}
};

static const double LAYOUTS_3[][4][4] = {
    {{0, 0, H, H}, {0, H, H, H}, {H, 0, H, 1}},
    {{0, 0, H, 1}, {H, 0, H, H}, {H, H, H, H}},
    {{0, 0, H, H}, {H, 0, H, H}, {0, H, 1, H}},
    {{0, 0, 1, H}, {0, H, H, H}, {H, H, H, H}},
    {{0, 0, T1, 1}, {T1, 0, T1, 1}, {T2, 0, T1, 1}},
    {{0, 0, 1, T1}, {0, T1, 1, T1}, {0, T2, 1, T1}}
};

static const double LAYOUTS_4[][4][4] = {
    {{0, 0, T1, T1}, {T1, 0, T2, 1}, {0, T2, T1, T1}, {0, T1, T1, T1}},
    {{0, 0, H, H}, {H, 0, H, H}, {0, H, H, H}, {H, H, H, H}},
    {{0, 0, H, 1}, {H, 0, H, T1}, {H, T1, H, T1}, {H, T2, H, T1}},
    {{0, 0, T1, H}, {T1, 0, T1, H}, {T2, 0, T1, H}, {0, H, 1, H}},
    {{0, 0, 1, H}, {0, H, T1, H}, {T1, H, T1, H}, {T2, H, T1, H}}
};

static const int LAYOUTS_2_COUNT = 2;
static const int LAYOUTS_3_COUNT = 6;
static const int LAYOUTS_4_COUNT = 5;


ViewportSet::ViewportSet()
    : name("Viewport set"), selectedViewportIndex(0), layoutStyle(0),
      fullViewport(false), sizeXInPixels(0), sizeYInPixels(0),
      titleColor(1, 1, 1), selectedTitleColor(1, 1, 0),
      ownedElementScaler(new ViewportElementScaler()),
      elementScaler(NULL), i18nContext(NULL)
{
    elementScaler = ownedElementScaler;
    availableRenderModes.add(Viewport::RENDER_MODE_Z_BUFFER);
    availableRenderModes.add(Viewport::RENDER_MODE_RAYTRACING);
}

ViewportSet::~ViewportSet()
{
    long i;
    for ( i = 0; i < viewports.size(); i++ ) {
        delete viewports.get(i);
    }
    delete ownedElementScaler;
}

ViewportSet* ViewportSet::createStandardSet(const String& name)
{
    ViewportSet* set = new ViewportSet();
    int numViews = 4;
    int i;

    set->setName(name);
    for ( i = 0; i < numViews; i++ ) {
        Viewport* viewport = new Viewport();
        viewport->applyDefaultConfiguration(numViews, i);
        set->addViewport(viewport);
    }
    set->setSelectedViewportIndex(1);
    set->updateLayout();
    return set;
}

const String& ViewportSet::getName() const
{
    return name;
}

void ViewportSet::setName(const String& name)
{
    this->name = name;
}

const ArrayList<Viewport*>& ViewportSet::getViewports() const
{
    return viewports;
}

int ViewportSet::getViewportCount() const
{
    return (int)viewports.size();
}

Viewport* ViewportSet::getViewport(int index) const
{
    return viewports.get(index);
}

void ViewportSet::addViewport(Viewport* viewport)
{
    if ( viewport == NULL ) {
        return;
    }
    viewports.add(viewport);
    coerceRenderMode(viewport);
    updateLayout();
}

ArrayList<int> ViewportSet::getAvailableRenderModes() const { return availableRenderModes; }
void ViewportSet::setAvailableRenderModes(const ArrayList<int>& modes)
{ if (modes.size() > 0) { availableRenderModes = modes; enforceAvailableRenderModes(); } }
bool ViewportSet::isRenderModeAvailable(int mode) const
{ for (long i=0;i<availableRenderModes.size();i++) if (availableRenderModes.get(i)==mode) return true; return false; }
void ViewportSet::coerceRenderMode(Viewport* v)
{ if (v != NULL && availableRenderModes.size()>0 && !isRenderModeAvailable(v->getRenderMode())) v->setRenderMode(availableRenderModes.get(0)); }
void ViewportSet::enforceAvailableRenderModes()
{ for (long i=0;i<viewports.size();i++) coerceRenderMode(viewports.get(i)); }
void ViewportSet::cycleRenderMode(Viewport* v)
{ if (v == NULL || availableRenderModes.size()==0) return; int next=availableRenderModes.get(0); for(long i=0;i<availableRenderModes.size();i++) if(availableRenderModes.get(i)==v->getRenderMode()){next=availableRenderModes.get((i+1)%availableRenderModes.size());break;} v->setRenderMode(next); }

Viewport* ViewportSet::removeViewport(int index)
{
    if ( index < 0 || index >= viewports.size() ) {
        return NULL;
    }
    Viewport* removed = viewports.get(index);
    viewports.remove((long)index);
    updateLayout();
    return removed;
}

int ViewportSet::getSelectedViewportIndex() const
{
    return selectedViewportIndex;
}

void ViewportSet::setSelectedViewportIndex(int selectedViewportIndex)
{
    this->selectedViewportIndex = selectedViewportIndex;
}

Viewport* ViewportSet::getSelectedViewport()
{
    if ( viewports.size() == 0 ) {
        return NULL;
    }
    clampSelection();
    return viewports.get(selectedViewportIndex);
}

bool ViewportSet::isSelected(const Viewport* viewport)
{
    return viewport != NULL && viewport == getSelectedViewport();
}

bool ViewportSet::selectViewport(const Viewport* viewport)
{
    int i;
    for ( i = 0; i < viewports.size(); i++ ) {
        if ( viewports.get(i) == viewport ) {
            selectedViewportIndex = i;
            return true;
        }
    }
    return false;
}

void ViewportSet::selectNextViewport()
{
    if ( viewports.size() == 0 ) {
        return;
    }
    selectedViewportIndex = (selectedViewportIndex + 1) % viewports.size();
    updateLayout();
}

int ViewportSet::getLayoutStyle() const
{
    return layoutStyle;
}

void ViewportSet::setLayoutStyle(int layoutStyle)
{
    this->layoutStyle = layoutStyle > 0 ? layoutStyle : 0;
}

int ViewportSet::getLayoutStyleCount() const
{
    switch ( viewports.size() ) {
      case 2:
        return LAYOUTS_2_COUNT + 1;
      case 3:
        return LAYOUTS_3_COUNT + 1;
      case 4:
        return LAYOUTS_4_COUNT + 1;
      default:
        return 1;
    }
}

void ViewportSet::selectNextLayoutStyle()
{
    layoutStyle++;
    if ( layoutStyle < 0 ) {
        layoutStyle = 0;
    }
    updateLayout();
}

bool ViewportSet::isFullViewport() const
{
    return fullViewport;
}

void ViewportSet::setFullViewport(bool fullViewport)
{
    this->fullViewport = fullViewport;
}

void ViewportSet::toggleFullViewport()
{
    fullViewport = !fullViewport;
    updateLayout();
}

const ColorRgb& ViewportSet::getTitleColor() const
{
    return titleColor;
}

void ViewportSet::setTitleColor(const ColorRgb& titleColor)
{
    this->titleColor = titleColor;
}

const ColorRgb& ViewportSet::getSelectedTitleColor() const
{
    return selectedTitleColor;
}

void ViewportSet::setSelectedTitleColor(const ColorRgb& selectedTitleColor)
{
    this->selectedTitleColor = selectedTitleColor;
}

Widget* ViewportSet::getI18nContext() const
{
    return i18nContext;
}

void ViewportSet::setI18nContext(Widget* i18nContext)
{
    this->i18nContext = i18nContext;
}

String ViewportSet::getTitleFor(const Viewport* viewport) const
{
    String command = viewport->getProjectionLocationCommand();
    String name = findMenuItemName(
        ViewportSetCommands::POPUP_PROJECTION_LOCATION, command);

    if ( name.isEmpty() && i18nContext != NULL ) {
        WidgetCommand* widgetCommand = i18nContext->getCommandByName(command);
        if ( widgetCommand != NULL ) {
            name = widgetCommand->getName();
        }
    }
    if ( name.isEmpty() ) {
        return viewport->getTitle();
    }
    return name;
}

String ViewportSet::findMenuItemName(const String& popupName,
                                           const String& command) const
{
    if ( i18nContext == NULL ) {
        return String("");
    }

    WidgetMenu* popup = i18nContext->getPopup(popupName);
    if ( popup == NULL ) {
        return String("");
    }

    long i;
    for ( i = 0; i < popup->getChildren().size(); i++ ) {
        WidgetMenuElement* child = popup->getChildren().get(i);
        WidgetMenuItem* item = child != NULL &&
            child->instanceOf(WidgetMenuItem::CLASS) ?
            (WidgetMenuItem*)child : NULL;
        if ( item != NULL && !item->isSeparator() &&
             command.equals(item->getCommandName()) ) {
            return item->getName();
        }
    }
    return String("");
}

ViewportElementScaler* ViewportSet::getElementScaler() const
{
    return elementScaler;
}

void ViewportSet::setElementScaler(ViewportElementScaler* elementScaler)
{
    if ( elementScaler != NULL ) {
        this->elementScaler = elementScaler;
    }
}

const ColorRgb& ViewportSet::getTitleColorFor(const Viewport* viewport)
{
    if ( isSelected(viewport) ) {
        return selectedTitleColor;
    }
    return titleColor;
}

int ViewportSet::getSizeXInPixels() const
{
    return sizeXInPixels;
}

void ViewportSet::setSizeXInPixels(int sizeXInPixels)
{
    this->sizeXInPixels = sizeXInPixels;
}

int ViewportSet::getSizeYInPixels() const
{
    return sizeYInPixels;
}

void ViewportSet::setSizeYInPixels(int sizeYInPixels)
{
    this->sizeYInPixels = sizeYInPixels;
}

void ViewportSet::resize(int sizeXInPixels, int sizeYInPixels)
{
    this->sizeXInPixels = sizeXInPixels;
    this->sizeYInPixels = sizeYInPixels;
    updatePixelAreas();
}

void ViewportSet::updatePixelAreas()
{
    long i;
    for ( i = 0; i < viewports.size(); i++ ) {
        viewports.get(i)->updatePixelArea(sizeXInPixels, sizeYInPixels);
    }
}

int ViewportSet::countActiveViewports() const
{
    int n = 0;
    long i;

    for ( i = 0; i < viewports.size(); i++ ) {
        if ( viewports.get(i)->isActive() ) {
            n++;
        }
    }
    return n;
}

Viewport* ViewportSet::findViewportAt(int x, int y) const
{
    if ( sizeXInPixels <= 0 || sizeYInPixels <= 0 ) {
        return NULL;
    }

    double xPercent = ((double)x) / ((double)sizeXInPixels);
    double yPercent = 1 - ((double)y) / ((double)sizeYInPixels);
    Viewport* found = NULL;
    long i;

    for ( i = 0; i < viewports.size(); i++ ) {
        Viewport* viewport = viewports.get(i);
        if ( viewport->isActive() && viewport->contains(xPercent, yPercent) ) {
            found = viewport;
        }
    }
    return found;
}

int ViewportSet::toViewportX(const Viewport* viewport, int x) const
{
    return x - viewport->getPixelStartX();
}

int ViewportSet::toViewportY(const Viewport* viewport, int y) const
{
    return y + viewport->getPixelSizeY() -
        (sizeYInPixels - viewport->getPixelStartY());
}

int ViewportSet::toSetX(const Viewport* viewport, int x) const
{
    return x + viewport->getPixelStartX();
}

int ViewportSet::toSetY(const Viewport* viewport, int y) const
{
    return y - viewport->getPixelSizeY() +
        (sizeYInPixels - viewport->getPixelStartY());
}

void ViewportSet::updateLayout()
{
    int n = (int)viewports.size();
    int i;

    clampSelection();
    if ( n == 0 ) {
        return;
    }

    if ( fullViewport ) {
        showOnlySelectedViewport();
        return;
    }

    switch ( n ) {
      case 1:
        viewports.get(0)->setActive(true);
        viewports.get(0)->setPercentArea(0, 0, 1, 1);
        break;
      case 2:
        applyLayout(LAYOUTS_2, LAYOUTS_2_COUNT);
        break;
      case 3:
        applyLayout(LAYOUTS_3, LAYOUTS_3_COUNT);
        break;
      case 4:
        applyLayout(LAYOUTS_4, LAYOUTS_4_COUNT);
        break;
      default:
        // Not supported layout: first viewport is shown maximized
        selectedViewportIndex = 0;
        for ( i = 0; i < n; i++ ) {
            viewports.get(i)->setActive(i == 0);
        }
        viewports.get(0)->setPercentArea(0, 0, 1, 1);
        break;
    }
}

void ViewportSet::applyLayout(const double layouts[][4][4], int layoutsCount)
{
    // Applies the layout selected by `layoutStyle` among the given ones.
    // After the last defined layout there is one more style, where only the
    // selected viewport is visible.
    int style = layoutStyle % (layoutsCount + 1);

    if ( style == layoutsCount ) {
        showOnlySelectedViewport();
    }
    else {
        applyAreas(layouts[style], (int)viewports.size());
    }
}

void ViewportSet::showOnlySelectedViewport()
{
    int i;

    for ( i = 0; i < viewports.size(); i++ ) {
        Viewport* viewport = viewports.get(i);
        viewport->setActive(i == selectedViewportIndex);
        if ( i == selectedViewportIndex ) {
            viewport->setPercentArea(0, 0, 1, 1);
        }
    }
}

void ViewportSet::applyAreas(const double areas[][4], int n)
{
    int i;

    for ( i = 0; i < n; i++ ) {
        Viewport* viewport = viewports.get(i);
        viewport->setActive(true);
        viewport->setPercentArea(areas[i][0], areas[i][1], areas[i][2],
                                 areas[i][3]);
    }
}

void ViewportSet::clampSelection()
{
    if ( selectedViewportIndex < 0 ||
         selectedViewportIndex >= viewports.size() ) {
        selectedViewportIndex = 0;
    }
}
