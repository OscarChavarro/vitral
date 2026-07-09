#ifndef __KEYBOARD_INTERACTION_TECHNIQUES__
#define __KEYBOARD_INTERACTION_TECHNIQUES__

class MarkersModel;

class KeyboardInteractionTechniques {
public:
    enum KeyAction {
        NONE,
        EXIT
    };

    explicit KeyboardInteractionTechniques(MarkersModel* model);

    KeyAction processKey(int key);

private:
    MarkersModel* model_;
};

#endif
