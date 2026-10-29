#ifndef __PROTECTED_286_KEYBOARD__
#define __PROTECTED_286_KEYBOARD__

#include "Protected286Types.h"

#define PROTECTED_286_KEY_QUEUE_SIZE 32

class Protected286Keyboard {
private:
    static volatile Protected286Byte queue[PROTECTED_286_KEY_QUEUE_SIZE];
    static volatile Protected286Byte head;
    static volatile Protected286Byte tail;
    static Protected286Byte shift;
    static void push(Protected286Byte character);

public:
    static void initialize();
    static void onInterrupt();
    static int getCharacter();
    static int waitCharacter();
};

#endif
