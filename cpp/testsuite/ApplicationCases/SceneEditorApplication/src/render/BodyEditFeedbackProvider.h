#ifndef __BODY_EDIT_FEEDBACK_PROVIDER__
#define __BODY_EDIT_FEEDBACK_PROVIDER__

#include "java/util/ArrayList.h"
#include "render/RenderPrimitive.h"

class SimpleBody;

/**
A body editor (i.e. the modify panel of the GUI) as seen by the renderers:
it tells which body is under edition and which feedback geometry must be
presented over it (handles, bounds, control points...), described as
technology independent `RenderPrimitive`s.

This is the abstraction side of a bridge: editors describe what to present
and each rendering technology (OpenGL 4, Vulkan...) implements how to draw
it, so editors do not depend on any rendering technology and new
technologies do not need code for each editor.
*/
class BodyEditFeedbackProvider {
public:
    virtual ~BodyEditFeedbackProvider() {}

    /**
    @return the body under edition, or null if there is none
    */
    virtual SimpleBody* getTarget() = 0;

    /**
    @return the feedback geometry to present over the body under edition, in
    world coordinates (empty if the editor has none)
    */
    virtual java::ArrayList<RenderPrimitive> buildEditFeedback() = 0;
};

#endif
