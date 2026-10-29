#ifndef __PROTECTED_286_VGA_TEXT__
#define __PROTECTED_286_VGA_TEXT__

#include "Protected286Types.h"

class Protected286VgaText {
private:
    static Protected286Word x;
    static Protected286Word y;
    static Protected286Word consoleRows;
    static Protected286Byte attribute;
    static void scroll();
    static void updateCursor();
    static void putCell(Protected286Word x, Protected286Word y, char character);

public:
    static void initialize();
    static void clear(Protected286Byte newAttribute);
    static void putCharacter(char character);
    static void write(const char* text);
    static void writeAt(Protected286Word column, Protected286Word row,
                        const char* text);
    static void setConsoleRows(Protected286Word rows);
    static void writeStatusAt(Protected286Word column, Protected286Word row,
                              const char* text);
    static void drawProgressBar(Protected286Word column, Protected286Word row,
                                Protected286Word width,
                                Protected286Word percent);
};

#endif
