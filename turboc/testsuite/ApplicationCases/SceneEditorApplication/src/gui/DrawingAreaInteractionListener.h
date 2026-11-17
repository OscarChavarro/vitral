#ifndef __DRWNG_AR_INTRCTN_LSTNR__
#define __DRWNG_AR_INTRCTN_LSTNR__

#include "java/lang/String.h"
#include "java/util/function/BooleanSupplier.h"
#include "gui/PointerCursor.h"

/**
Receives the requests that `DrawingAreaInteractionTechniques` derives from
user interaction, so the GUI technology in use can present them (pointer
shape, messages, dialogs...).
*/
class DrawingAreaInteractionListener {
public:
    virtual ~DrawingAreaInteractionListener() {}

    /**
    @param cursor the pointer shape to present over the drawing area
    */
    virtual void cursorRequested(PointerCursor::Value cursor) = 0;

    /**
    The pointer must be placed at a position (infinite drag of a gizmo).
    @param surfaceX horizontal position, in pixels of the viewport set area
    @param surfaceY vertical position, in pixels of the viewport set area
    */
    virtual void cursorWarpRequested(int surfaceX, int surfaceY) = 0;

    /**
    The drawing area content changed and must be drawn again.
    */
    virtual void repaintRequested() = 0;

    /**
    @param message text for the status message of the application
    */
    virtual void statusMessageRequested(const String& message) = 0;

    /**
    The selection of things changed: panels showing the selected target
    should be updated.
    */
    virtual void selectionChanged() = 0;

    /**
    A raytraced image of the scene was requested.
    */
    virtual void raytracingRequested() = 0;

    /**
    The object selector dialog was requested.
    */
    virtual void selectorDialogRequested() = 0;

    /**
    The user requested to close the application.
    */
    virtual void closeRequested() = 0;

    /**
    The user requested to toggle the full screen GUI mode.
    */
    virtual void fullScreenGuiToggleRequested() = 0;

    /**
    An animation started (i.e. the camera turning to a view chosen with the
    camera control gizmo): `frame` must be called periodically (some tens of
    times per second), drawing the drawing area again after each call, while
    it returns true. A new request replaces the one in course.
    @param frame advances the animation to the current time; returns false
    when it ended. C++ port note: not owned by the listener; it lives as long
    as the interaction techniques that request the animation.
    */
    virtual void animationRequested(BooleanSupplier* frame) = 0;
};

#endif
