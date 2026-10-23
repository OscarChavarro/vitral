#include <stddef.h>
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "vsdk/toolkit/gui/viewport/ViewportSetInteractionListener.h"
#include "vsdk/toolkit/gui/viewport/ViewportSetInteractionTechniques.h"

ViewportSetInteractionTechniques::ViewportSetInteractionTechniques(
    ViewportSet* viewportSet)
    : viewportSet(viewportSet), listener(NULL), titlePressArmed(FALSE)
{
}

void ViewportSetInteractionTechniques::setListener(
    ViewportSetInteractionListener* listener)
{
    this->listener = listener;
}

ViewportSet* ViewportSetInteractionTechniques::getViewportSet() const
{
    return viewportSet;
}

BOOLEAN ViewportSetInteractionTechniques::processCommand(
    const String& command)
{
    return processCommand(command, viewportSet->getSelectedViewport());
}

BOOLEAN ViewportSetInteractionTechniques::processCommand(
    const String& command, Viewport* viewport)
{
    if ( viewport == NULL ) {
        return FALSE;
    }
    if ( viewport->selectProjectionLocation(command) ) return TRUE;
    int mode = Viewport::renderModeForCommand(command);
    if ( mode == 0 || !viewportSet->isRenderModeAvailable(mode) ) return FALSE;
    viewport->setRenderMode(mode);
    return TRUE;
}

BOOLEAN ViewportSetInteractionTechniques::processKeyPressedEvent(
    const KeyEvent& event)
{
    switch ( event.unicodeId ) {
      case ';':
        viewportSet->selectNextViewport();
        return TRUE;
      case ',':
        viewportSet->selectNextLayoutStyle();
        return TRUE;
      case 'w':
        if ( (event.modifierMask & KeyEvent::MASK_ALT) != 0 ) {
            viewportSet->toggleFullViewport();
            return TRUE;
        }
        return FALSE;
      default:
        return processSelectedViewportCommand(event);
    }
}

BOOLEAN ViewportSetInteractionTechniques::processSelectedViewportCommand(
    const KeyEvent& event)
{
    Viewport* viewport = viewportSet->getSelectedViewport();

    if ( viewport == NULL ) {
        return FALSE;
    }

    if ( event.keycode == KeyEvent::KEY_9 ) {
        viewport->toggleRenderMode();
        return TRUE;
    }
    if ( event.keycode == KeyEvent::KEY_NUM0 ) {
        viewport->cycleRequestedSize();
        return TRUE;
    }

    switch ( event.unicodeId ) {
      case '.':
        viewport->toggleRenderMode();
        return TRUE;
      case 'g':
        viewport->toggleGrid();
        return TRUE;
      case 't':
        viewport->setActiveCamera(viewport->getTopCamera());
        return TRUE;
      case 'l':
        viewport->setActiveCamera(viewport->getLeftCamera());
        return TRUE;
      case 'f':
        viewport->setActiveCamera(viewport->getFrontCamera());
        return TRUE;
      case 'b':
        viewport->setActiveCamera(viewport->getBottomCamera());
        return TRUE;
      case 'p':
        viewport->setActiveCamera(viewport->getPerspectiveCamera());
        return TRUE;
      case '0':
        viewport->cycleRequestedSize();
        return TRUE;
      default:
        return FALSE;
    }
}

Viewport* ViewportSetInteractionTechniques::findViewportAt(
    const MouseEvent& event) const
{
    return viewportSet->findViewportAt(event.getX(), event.getY());
}

BOOLEAN ViewportSetInteractionTechniques::isPointerOverTitle(
    const MouseEvent& event) const
{
    Viewport* viewport = findViewportAt(event);

    return viewport != NULL && isOverTitle(event, viewport);
}

BOOLEAN ViewportSetInteractionTechniques::processMousePressedEvent(
    const MouseEvent& event)
{
    Viewport* viewport = findViewportAt(event);

    // Must be evaluated before the selection changes
    titlePressArmed = viewport != NULL &&
        event.getButton() == MouseEvent::BUTTON1 &&
        viewportSet->isSelected(viewport) &&
        isOverTitle(event, viewport);

    return selectViewportUnderPointer(event);
}

BOOLEAN ViewportSetInteractionTechniques::processMouseReleasedEvent(
    const MouseEvent& event)
{
    BOOLEAN titleClick = titlePressArmed;
    Viewport* viewport = findViewportAt(event);

    titlePressArmed = FALSE;
    BOOLEAN selected = selectViewportUnderPointer(event);

    if ( titleClick && viewport != NULL && listener != NULL &&
         viewportSet->isSelected(viewport) && isOverTitle(event, viewport) ) {
        // Just below the title, aligned with it
        int x = viewport->getPixelStartX() + viewport->getTitleAreaStartX();
        int viewportTop = viewportSet->getSizeYInPixels() -
            (viewport->getPixelStartY() + viewport->getPixelSizeY());
        int y = viewportTop + viewport->getTitleAreaStartY() +
            viewport->getTitleAreaSizeY();
        listener->projectionLocationMenuRequested(viewport, x, y);
    }
    return selected;
}

BOOLEAN ViewportSetInteractionTechniques::processMouseClickedEvent(
    const MouseEvent& event)
{
    titlePressArmed = FALSE;
    return selectViewportUnderPointer(event);
}

BOOLEAN ViewportSetInteractionTechniques::processMouseDraggedEvent(
    const MouseEvent& event)
{
    return selectViewportUnderPointer(event);
}

BOOLEAN ViewportSetInteractionTechniques::selectViewportUnderPointer(
    const MouseEvent& event)
{
    Viewport* viewport = findViewportAt(event);

    if ( viewport == NULL ) {
        return FALSE;
    }
    return viewportSet->selectViewport(viewport);
}

BOOLEAN ViewportSetInteractionTechniques::isOverTitle(
    const MouseEvent& event, const Viewport* viewport) const
{
    return viewport->isOverTitle(
        viewportSet->toViewportX(viewport, event.getX()),
        viewportSet->toViewportY(viewport, event.getY()));
}

MouseEvent ViewportSetInteractionTechniques::toViewportEvent(
    const MouseEvent& event, const Viewport* viewport) const
{
    MouseEvent translated;

    translated.setX(viewportSet->toViewportX(viewport, event.getX()));
    translated.setY(viewportSet->toViewportY(viewport, event.getY()));
    translated.setButton(event.getButton());
    translated.setModifiers(event.getModifiers());
    translated.setClicks(event.getClicks());
    return translated;
}
