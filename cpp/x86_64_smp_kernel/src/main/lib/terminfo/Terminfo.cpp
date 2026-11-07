#include "lib/terminfo/Terminfo.h"
#include "lib/vitralKernel/String.h"

static_assert((int)Terminfo::Cap::COUNT == 23, "a capability table per entry");
static_assert((int)Terminfo::Key::COUNT == 23, "a key table per entry");

// In Cap order: clear el ed cup home cub1 cuf1 cuu1 cud1 cub cuf cuu cud sgr0 bold rev smso rmso smul rmul civis
// cnorm dch1.
static const char *const TERMINFO_LINUX_CAPS[] = {
    "\033[H\033[J", "\033[K", "\033[J", "\033[%i%p1%d;%p2%dH", "\033[H", "\b", "\033[C", "\033[A", "\n",
    "\033[%p1%dD", "\033[%p1%dC", "\033[%p1%dA", "\033[%p1%dB", "\033[m\017", "\033[1m", "\033[7m", "\033[7m",
    "\033[27m", "\033[4m", "\033[24m", "\033[?25l\033[?1c", "\033[?25h\033[?0c", "\033[P",
};
static const char *const TERMINFO_VT220_CAPS[] = {
    "\033[H\033[J", "\033[K", "\033[J", "\033[%i%p1%d;%p2%dH", "\033[H", "\b", "\033[C", "\033[A", "\n",
    "\033[%p1%dD", "\033[%p1%dC", "\033[%p1%dA", "\033[%p1%dB", "\033[m\033(B", "\033[1m", "\033[7m", "\033[7m",
    "\033[27m", "\033[4m", "\033[24m", "\033[?25l", "\033[?25h", "\033[P",
};
static const char *const TERMINFO_VT100_CAPS[] = {
    "\033[H\033[J", "\033[K", "\033[J", "\033[%i%p1%d;%p2%dH", "\033[H", "\b", "\033[C", "\033[A", "\n",
    "\033[%p1%dD", "\033[%p1%dC", "\033[%p1%dA", "\033[%p1%dB", "\033[m", "\033[1m", "\033[7m", "\033[7m",
    "\033[m", "\033[4m", "\033[m", "", "", "",
};
static const char *const TERMINFO_XTERM_CAPS[] = {
    "\033[H\033[2J", "\033[K", "\033[J", "\033[%i%p1%d;%p2%dH", "\033[H", "\b", "\033[C", "\033[A", "\n",
    "\033[%p1%dD", "\033[%p1%dC", "\033[%p1%dA", "\033[%p1%dB", "\033(B\033[m", "\033[1m", "\033[7m", "\033[7m",
    "\033[27m", "\033[4m", "\033[24m", "\033[?25l", "\033[?12l\033[?25h", "\033[P",
};

// In Key order, from Up: kcuu1 kcud1 kcub1 kcuf1 khome kend kich1 kdch1 kpp knp kf1 ... kf12.
static const char *const TERMINFO_LINUX_KEYS[] = {
    "",          "\033[A",    "\033[B",    "\033[D",    "\033[C",    "\033[1~",   "\033[4~",   "\033[2~",
    "\033[3~",   "\033[5~",   "\033[6~",   "\033[[A",   "\033[[B",   "\033[[C",   "\033[[D",   "\033[[E",
    "\033[17~",  "\033[18~",  "\033[19~",  "\033[20~",  "\033[21~",  "\033[23~",  "\033[24~",
};
static const char *const TERMINFO_VT220_KEYS[] = {
    "",          "\033[A",    "\033[B",    "\033[D",    "\033[C",    "\033[1~",   "\033[4~",   "\033[2~",
    "\033[3~",   "\033[5~",   "\033[6~",   "\033OP",    "\033OQ",    "\033OR",    "\033OS",    "",
    "\033[17~",  "\033[18~",  "\033[19~",  "\033[20~",  "\033[21~",  "\033[23~",  "\033[24~",
};
static const char *const TERMINFO_VT100_KEYS[] = {
    "",          "\033OA",    "\033OB",    "\033OD",    "\033OC",    "",          "",          "",
    "",          "",          "",          "\033OP",    "\033OQ",    "\033OR",    "\033OS",    "",
    "",          "",          "",          "",          "",          "",          "",
};
static const char *const TERMINFO_XTERM_KEYS[] = {
    "",          "\033OA",    "\033OB",    "\033OD",    "\033OC",    "\033OH",    "\033OF",    "\033[2~",
    "\033[3~",   "\033[5~",   "\033[6~",   "\033OP",    "\033OQ",    "\033OR",    "\033OS",    "\033[15~",
    "\033[17~",  "\033[18~",  "\033[19~",  "\033[20~",  "\033[21~",  "\033[23~",  "\033[24~",
};
// What most terminals send whatever their entry says, in normal or application cursor key mode.
static const char *const TERMINFO_COMMON_KEYS[] = {
    "",          "\033[A",    "\033[B",    "\033[D",    "\033[C",    "\033[H",    "\033[F",    "\033[2~",
    "\033[3~",   "\033[5~",   "\033[6~",   "\033OP",    "\033OQ",    "\033OR",    "\033OS",    "\033[15~",
    "\033[17~",  "\033[18~",  "\033[19~",  "\033[20~",  "\033[21~",  "\033[23~",  "\033[24~",
};
static const char *const TERMINFO_COMMON_ALTERNATES[] = {
    "",          "\033OA",    "\033OB",    "\033OD",    "\033OC",    "\033OH",    "\033OF",    "",
    "",          "",          "",          "",          "",          "",          "",          "",
    "",          "",          "",          "",          "",          "",          "",
};
static const char *const TERMINFO_COMMON_THIRD[] = {
    "",          "",          "",          "",          "",          "\033[7~",   "\033[8~",   "",
    "",          "",          "",          "\033[11~",  "\033[12~",  "\033[13~",  "\033[14~",  "",
    "",          "",          "",          "",          "",          "",          "",
};

const Terminfo Terminfo::entries[] = {
    Terminfo("linux", 80, 25, TERMINFO_LINUX_CAPS, TERMINFO_LINUX_KEYS),
    Terminfo("vt220", 80, 24, TERMINFO_VT220_CAPS, TERMINFO_VT220_KEYS),
    Terminfo("vt100", 80, 24, TERMINFO_VT100_CAPS, TERMINFO_VT100_KEYS),
    Terminfo("xterm", 80, 24, TERMINFO_XTERM_CAPS, TERMINFO_XTERM_KEYS),
};
const unsigned Terminfo::entryCount = sizeof(entries) / sizeof(entries[0]);

static bool
terminfoSameName(const char *name, const char *entry) {
    for (; *entry; name++, entry++) {
        if (*name != *entry) {
            return false;
        }
    }
    // "xterm-256color" and "vt220-8bit" use the entry of their family.
    return *name == 0 || *name == '-';
}

const Terminfo &
Terminfo::find(const char *name) {
    if (name) {
        for (unsigned i = 0; i < entryCount; i++) {
            if (terminfoSameName(name, entries[i].nameValue)) {
                return entries[i];
            }
        }
        if (terminfoSameName(name, "vt102") || terminfoSameName(name, "ansi")) {
            return entries[2];
        }
        if (terminfoSameName(name, "screen") || terminfoSameName(name, "tmux") || terminfoSameName(name, "rxvt")) {
            return entries[3];
        }
    }
    return entries[2];
}

const char *
Terminfo::get(Cap cap) const { return cap < Cap::COUNT ? caps[(unsigned)cap] : ""; }

const char *
Terminfo::keySequence(Key key) const { return key < Key::COUNT ? keys[(unsigned)key] : ""; }

size_t
Terminfo::format(Cap cap, char *out, size_t size, int p1, int p2) const {
    return expand(get(cap), out, size, p1, p2);
}

// The subset of tparm()'s language the entries use: %pN pushes a parameter, %d and %c pop one, %i adds one to the
// first two parameters, %{N} pushes a constant, %+ adds and %% is a %.
size_t
Terminfo::expand(const char *capability, char *out, size_t size, int p1, int p2) {
    if (!size) {
        return 0;
    }
    int params[2] = {p1, p2};
    int stack[4];
    unsigned depth = 0;
    size_t n = 0;
    auto put = [&](char c) {
        if (n + 1 < size) {
            out[n++] = c;
        }
    };
    auto pop = [&]() { return depth ? stack[--depth] : 0; };
    auto push = [&](int v) {
        if (depth < 4) {
            stack[depth++] = v;
        }
    };
    for (const char *p = capability; p && *p; p++) {
        if (*p != '%') {
            put(*p);
            continue;
        }
        p++;
        if (!*p) {
            break;
        }
        switch (*p) {
        case '%': put('%'); break;
        case 'i': params[0]++; params[1]++; break;
        case 'p':
            if (p[1] >= '1' && p[1] <= '9') {
                p++;
                push(*p == '1' ? params[0] : *p == '2' ? params[1] : 0);
            }
            break;
        case 'c': put((char)pop()); break;
        case '+': { int b = pop(); push(pop() + b); break; }
        case '{': {
            int v = 0;
            while (p[1] >= '0' && p[1] <= '9') {
                v = v * 10 + (*++p - '0');
            }
            if (p[1] == '}') {
                p++;
            }
            push(v);
            break;
        }
        case 'd': {
            int v = pop();
            char digits[12];
            unsigned count = 0;
            unsigned magnitude = v < 0 ? (unsigned)-v : (unsigned)v;
            do {
                digits[count++] = (char)('0' + magnitude % 10);
                magnitude /= 10;
            } while (magnitude);
            if (v < 0) {
                put('-');
            }
            while (count) {
                put(digits[--count]);
            }
            break;
        }
        default: break;
        }
    }
    out[n] = 0;
    return n;
}

Terminfo::Match
Terminfo::matchTable(const char *const *table, const uint8_t *bytes, size_t count, Key &key, size_t &used) {
    Match best = Match::No;
    for (unsigned k = 1; k < (unsigned)Key::COUNT; k++) {
        const char *sequence = table[k];
        size_t length = String::length(sequence);
        if (!length) {
            continue;
        }
        size_t common = 0;
        while (common < length && common < count && (uint8_t)sequence[common] == bytes[common]) {
            common++;
        }
        if (common == length) {
            key = (Key)k;
            used = length;
            return Match::Full;
        }
        if (common == count) {
            best = Match::Partial;
        }
    }
    return best;
}

Terminfo::Match
Terminfo::decode(const uint8_t *bytes, size_t count, Key &key, size_t &used) const {
    key = Key::None;
    used = 0;
    if (!count) {
        return Match::No;
    }
    const char *const *tables[] = {keys, TERMINFO_COMMON_KEYS, TERMINFO_COMMON_ALTERNATES, TERMINFO_COMMON_THIRD};
    Match result = Match::No;
    for (const char *const *table : tables) {
        Match m = matchTable(table, bytes, count, key, used);
        if (m == Match::Full) {
            return m;
        }
        if (m == Match::Partial) {
            result = m;
        }
    }
    return result;
}
