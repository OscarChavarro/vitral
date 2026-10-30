#ifndef __A20__
#define __A20__

class A20 {
public:
    static bool isEnabled();
    static bool enable();

private:
    static bool settled();
    static bool tryBios();
    static void tryFast();
    static void tryKeyboardController();
    static bool waitInputEmpty();
    static bool waitOutputFull();
};

#endif
