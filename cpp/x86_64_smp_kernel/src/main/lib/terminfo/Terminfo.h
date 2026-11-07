#ifndef __TERMINFO__
#define __TERMINFO__
#include <stddef.h>
#include <stdint.h>

// The terminal descriptions a program needs to drive a character terminal: what to send for each action (the string
// capabilities of terminfo(5), with their parameters in terminfo's % notation) and what each special key sends. The
// entries follow ncurses' terminfo.src for the Linux console ("linux", what the virtual consoles are), the DEC VT220
// ("vt220", the default of the serial port), the VT100 and xterm. find() falls back to "vt100", which every terminal
// emulator understands.
class Terminfo {
public:
    // String capabilities, by their terminfo names.
    enum class Cap : uint8_t {
        Clear,           // clear: clear the screen, cursor home
        ClearToEnd,      // el: clear to the end of the line
        ClearToBottom,   // ed: clear to the end of the screen
        CursorAddress,   // cup: move to row %p1, column %p2 (from 0)
        CursorHome,      // home
        Left,            // cub1
        Right,           // cuf1
        Up,              // cuu1
        Down,            // cud1
        LeftBy,          // cub: %p1 columns left
        RightBy,         // cuf: %p1 columns right
        UpBy,            // cuu: %p1 rows up
        DownBy,          // cud: %p1 rows down
        Normal,          // sgr0: all attributes off
        Bold,            // bold
        Reverse,         // rev
        StandoutOn,      // smso
        StandoutOff,     // rmso
        UnderlineOn,     // smul
        UnderlineOff,    // rmul
        CursorInvisible, // civis
        CursorNormal,    // cnorm
        DeleteChar,      // dch1
        COUNT
    };

    // Special keys, as decode() reports them.
    enum class Key : uint8_t {
        None,
        Up,
        Down,
        Left,
        Right,
        Home,
        End,
        Insert,
        Delete,
        PageUp,
        PageDown,
        F1,
        F2,
        F3,
        F4,
        F5,
        F6,
        F7,
        F8,
        F9,
        F10,
        F11,
        F12,
        COUNT
    };

    enum class Match : uint8_t { No, Partial, Full };

    static const Terminfo &find(const char *name);

    const char *name() const { return nameValue; }
    int columns() const { return columnsValue; }
    int lines() const { return linesValue; }
    // The capability, or "" when the terminal has none.
    const char *get(Cap cap) const;
    // The capability with its parameters expanded, as tparm() does (at most two parameters). Returns its length,
    // without the terminating 0 that out always gets.
    size_t format(Cap cap, char *out, size_t size, int p1 = 0, int p2 = 0) const;
    // What the key sends on this terminal (kcuu1, khome, kf1...), or "" when it has no such key.
    const char *keySequence(Key key) const;
    // Matches the start of count received bytes against the keys of this terminal, then against the sequences most
    // terminals send (so that a guessed terminal type still works): Full sets key and used, Partial asks for more
    // bytes.
    Match decode(const uint8_t *bytes, size_t count, Key &key, size_t &used) const;

    // tparm() over any capability string.
    static size_t expand(const char *capability, char *out, size_t size, int p1, int p2);

private:
    const char *nameValue;
    int columnsValue;
    int linesValue;
    const char *const *caps;
    const char *const *keys;

    constexpr Terminfo(const char *entryName, int entryColumns, int entryLines, const char *const *entryCaps,
                       const char *const *entryKeys)
        : nameValue(entryName), columnsValue(entryColumns), linesValue(entryLines), caps(entryCaps),
          keys(entryKeys) {}

    static const Terminfo entries[];
    static const unsigned entryCount;

    static Match matchTable(const char *const *table, const uint8_t *bytes, size_t count, Key &key, size_t &used);
};

#endif
