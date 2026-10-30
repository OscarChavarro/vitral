#ifndef __VGA_MODES__
#define __VGA_MODES__

#include <termios.h>

class VgaModes {
public:
    static constexpr int MAX_MODES = 256;
    static constexpr int MAX_TEXT = 40;
    static constexpr int ROW_WIDTH = 72;

    class Screen {
    public:
        int mode;
        int width;
        int height;
        int colors;
        int bitsPerPixel;
        bool text;
    };

    enum class Change { None, Done, Failed };
    enum class Selection { Chosen, Quit, Unknown };

    explicit VgaModes(bool withText = true);
    // Leaves the console as it was found if the program did not call leave() (any return path).
    ~VgaModes();
    VgaModes(const VgaModes &) = delete;
    VgaModes &operator=(const VgaModes &) = delete;

    int count() const { return total; }
    int at(int index) const { return modes[index]; }
    int indexOf(int mode) const;
    int highest() const { return total > 0 ? modes[total - 1] : -1; }

    static void header(char *out, int size);
    static void row(int mode, char *out, int size);
    void print(const char *title) const;

    static const char *name(int mode);
    static int bitsPerPixel(int mode);
    static const char *origin(int mode);
    static void describe(int mode, char *out, int size);

    static int lookup(const char *text);

    int prompt(char *error, int size) const;
    int ask() const;

    Selection select(const char *program, int count, char *const words[], int fallback, int *mode) const;

    bool enter(int mode);
    const Screen &screen() const { return shown; }
    int current() const { return shown.mode; }

    Change step(int step);
    Change nextDepth();

    // Keys one at a time and without echo in text mode: svgalib only sets that terminal mode for graphics modes, and
    // in TEXT it puts back the one it found at its first vga_setmode().
    void rawKeys();
    // Back to TEXT, with the terminal mode found when this object was built.
    void leave();

private:
    int modes[MAX_MODES];
    int total;
    bool withText;
    Screen shown;
    termios original;
    bool haveOriginal;
    bool changed;

    Change change(int next);
    static bool sameSize(int a, int b);
    static int width(int mode);
    static void catchFatalSignals();
    static void onFatalSignal(int number);
    static unsigned area(int mode);
};

#endif
