#ifndef __LIGHT_TRANSFORM_STATE__
#define __LIGHT_TRANSFORM_STATE__

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "model/history/EntityTransformState.h"

class Light;

/**
Position of a light (the placement the editor can change).
*/
class LightTransformState : public EntityTransformState {
private:
    Light* light;
    Vector3Dd position;

    explicit LightTransformState(Light* light);

public:
    /**
    @param light light whose placement is captured
    @return the current placement of the light, owned by the caller
    */
    static LightTransformState* capture(Light* light);

    virtual Entity* getEntity() const override;
    virtual void restore() const override;
    virtual bool isSameState(const EntityTransformState* other) const override;
    virtual EntityTransformState* clone() const override;
};

#endif
