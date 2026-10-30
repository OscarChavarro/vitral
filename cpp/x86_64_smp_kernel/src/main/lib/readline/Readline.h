#ifndef __READLINE__
#define __READLINE__
#include <stdint.h>

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

    explicit Readline(unsigned console)
        : console(console), escapeEnds(false), completions(nullptr), count(0), first(0) {}

    void setEscapeEnds(bool value) { escapeEnds = value; }
    void setCompletions(const Completions *source) { completions = source; }

    Result read(const char *prompt, char *out, unsigned capacity);

    void add(const char *line);
    unsigned historyCount() const { return count; }
    const char *historyAt(unsigned i) const;
    void clearHistory() { count = first = 0; }

private:
    enum class Step { Continue, Enter, Eof, Escape };
    static constexpr int KEY_CTRL_A = 1;
    static constexpr int KEY_CTRL_D = 4;
    static constexpr int KEY_CTRL_E = 5;
    static constexpr int KEY_TAB = '\t';
    static constexpr int KEY_CTRL_G = 7;
    static constexpr int KEY_CTRL_R = 18;

    unsigned console;
    bool escapeEnds;
    const Completions *completions;

    char history[HISTORY_SIZE][MAX_LINE + 1];
    unsigned count;
    unsigned first;

    const char *prompt;
    unsigned promptWidth;
    unsigned limit;
    int row;
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
