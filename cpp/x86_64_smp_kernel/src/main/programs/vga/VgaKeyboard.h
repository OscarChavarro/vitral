#ifndef __VGA_KEYBOARD__
#define __VGA_KEYBOARD__

class VgaKeyboard {
  public:
    static int readKey();
    static int waitKey();
};

#endif
