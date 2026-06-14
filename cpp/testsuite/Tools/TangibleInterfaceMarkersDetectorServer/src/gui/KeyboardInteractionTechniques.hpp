#ifndef KEYBOARD_INTERACTION_TECHNIQUES_HPP
#define KEYBOARD_INTERACTION_TECHNIQUES_HPP

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
