#ifndef __IDLE__
#define __IDLE__

class Idle {
public:
    static bool canSleep();
    static const char *name();

    [[noreturn]] static void run();
    [[noreturn]] static void stop();

private:
    enum class Mode { MWait, Hlt, Nop };
    static Mode mode();
    [[noreturn]] static void loop(Mode m, bool interruptsOn);
};

#endif
