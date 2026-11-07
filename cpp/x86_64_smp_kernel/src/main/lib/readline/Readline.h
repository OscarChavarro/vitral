#ifndef __READLINE__
#define __READLINE__
#include <stddef.h>
#include <stdint.h>
#include "drivers/tty/Tty.h"
#include "lib/terminfo/Terminfo.h"

// A line editor for any terminal: it reads keys from the terminal in raw mode, decodes them with the terminal's
// terminfo entry, and edits the line with the control sequences of that entry (CR, el, cub), so it works the same
// on a virtual console, a serial line or, later, a network connection. The line stays on one row: it can be as long
// as the row allows after the prompt.
//
// Keys: Left/Right (Ctrl-B/Ctrl-F), Home/End (Ctrl-A/Ctrl-E), Backspace, Delete, Up/Down (Ctrl-P/Ctrl-N) for the
// history, Ctrl-R to search it, Tab to complete the first word, Ctrl-U and Ctrl-K to kill before and after the cursor,
// Ctrl-L to clear the screen, Ctrl-C to drop the line and Ctrl-D on an empty line for end of file.
class Readline {
public:
    static constexpr unsigned MAX_LINE = 127;
    static constexpr unsigned HISTORY_SIZE = 50;

    enum class Result {
        Line,
        Eof,
        Escape
    };

    class Completions {
    public:
        virtual unsigned count() const = 0;
        virtual const char *at(unsigned i) const = 0;

    protected:
        ~Completions() = default;
    };

    explicit Readline(Tty &terminal)
        : terminal(terminal), info(&Terminfo::find(terminal.type())), escapeEnds(false), completions(nullptr),
          count(0), first(0) {}

    void setEscapeEnds(bool value) { escapeEnds = value; }
    void setCompletions(const Completions *source) { completions = source; }

    Result read(const char *prompt, char *out, unsigned capacity);

    void add(const char *line);
    unsigned historyCount() const { return count; }
    const char *historyAt(unsigned i) const;
    void clearHistory() { count = first = 0; }

private:
    enum class Step { Continue, Enter, Eof, Escape };
    static constexpr int KEY_NONE = -1;
    static constexpr int KEY_CTRL_A = 1;
    static constexpr int KEY_CTRL_B = 2;
    static constexpr int KEY_CTRL_C = 3;
    static constexpr int KEY_CTRL_D = 4;
    static constexpr int KEY_CTRL_E = 5;
    static constexpr int KEY_CTRL_F = 6;
    static constexpr int KEY_CTRL_G = 7;
    static constexpr int KEY_BACKSPACE = 8;
    static constexpr int KEY_TAB = 9;
    static constexpr int KEY_ENTER = 10;
    static constexpr int KEY_CTRL_K = 11;
    static constexpr int KEY_CTRL_L = 12;
    static constexpr int KEY_CTRL_N = 14;
    static constexpr int KEY_CTRL_P = 16;
    static constexpr int KEY_CTRL_R = 18;
    static constexpr int KEY_CTRL_U = 21;
    static constexpr int KEY_ESC = 27;
    // Special keys are KEY_SPECIAL plus their Terminfo::Key.
    static constexpr int KEY_SPECIAL = 0x100;
    static constexpr int ESCAPE_TIMEOUT_MS = 60;
    static constexpr unsigned SEQUENCE_MAX = 16;
    static constexpr unsigned OUTPUT_SIZE = 512;

    Tty &terminal;
    const Terminfo *info;
    bool escapeEnds;
    const Completions *completions;

    char history[HISTORY_SIZE][MAX_LINE + 1];
    unsigned count;
    unsigned first;

    const char *prompt;
    unsigned promptWidth;
    unsigned origin;
    unsigned columns;
    unsigned limit;
    char line[MAX_LINE + 1];
    unsigned length;
    unsigned cursor;
    unsigned position;
    char draft[MAX_LINE + 1];
    int lastKey;

    bool searching;
    char query[MAX_LINE + 1];
    unsigned queryLength;
    int match;
    int matchOffset;
    char before[MAX_LINE + 1];

    char output[OUTPUT_SIZE];
    size_t outputLength;

    static constexpr int special(Terminfo::Key key) { return KEY_SPECIAL + (int)key; }
    bool readByte(uint8_t &byte, int timeoutMs);
    int nextKey();
    void put(const char *text);
    void putCap(Terminfo::Cap cap, int p1 = 0);
    void flushOutput();
    void startOfLine();
    Step edit(int key);
    Step searchKey(int key);
    void redraw();
    void drawSearch();
    void load(const char *text);
    void beginSearch();
    void endSearch(bool accept);
    bool findMatch(int from);
    bool completes(const char *candidate) const;
    void complete(bool listMatches);
};

#endif
