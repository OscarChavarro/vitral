#ifndef __SELECTED_BODY_MAP_TOGGLES__
#define __SELECTED_BODY_MAP_TOGGLES__

class SimpleBody;

/**
Debugging toggles of the maps of a body: a sample texture and a sample bump
map (normal map), loaded from the etc folder of the project.
*/
class SelectedBodyMapToggles {
public:
    /**
    Sets the sample texture to a body without texture, or removes (and
    deletes) its texture.
    @param body the body to change
    */
    void toggleTexture(SimpleBody* body);

    /**
    Sets the sample bump map to a body without normal map, or removes (and
    deletes) its normal map.
    @param body the body to change
    */
    void toggleNormalMap(SimpleBody* body);
};

#endif
