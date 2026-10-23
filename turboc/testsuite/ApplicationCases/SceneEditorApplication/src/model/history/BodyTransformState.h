#ifndef __BODY_TRANSFORM_STATE__
#define __BODY_TRANSFORM_STATE__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "model/history/EntityTransformState.h"

class SimpleBody;

/**
Position, orientation and scale of a body. Vectors and matrices of vitral are
immutable values, so they are kept by copy.
*/
class BodyTransformState : public EntityTransformState {
private:
    SimpleBody* body;
    Vector3Dd position;
    Matrix4x4d rotation;
    Vector3Dd scale;

    explicit BodyTransformState(SimpleBody* body);

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    /**
    @param body body whose placement is captured
    @return the current placement of the body, owned by the caller
    */
    static BodyTransformState* capture(SimpleBody* body);

    virtual Entity* getEntity() const;
    virtual void restore() const;
    virtual BOOLEAN isSameState(const EntityTransformState* other) const;
    virtual EntityTransformState* clone() const;
};

#endif
