#include "lib/readline/Readline.h"
#include "lib/vitralKernel/Format.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/String.h"

static int
readlineIndexOf(const char *text, const char *needle) {
    size_t n = String::length(needle);
    for (int i = 0; text[i]; i++) {
        size_t j = 0;
        while (j < n && text[i + j] == needle[j]) {
            j++;
        }
        if (j == n) {
            return i;
        }
    }
    return -1;
}

const char *
Readline::historyAt(unsigned i) const {
    return i < count ? history[(first + i) % HISTORY_SIZE] : nullptr;
}

void
Readline::add(const char *text) {
    if (!*text || String::length(text) > MAX_LINE) {
        return;
    }
    if (count && String::equals(historyAt(count - 1), text)) {
        return;
    }
    if (count == HISTORY_SIZE) {
        first = (first + 1) % HISTORY_SIZE;
    } else {
        count++;
    }
    String::copy(history[(first + count - 1) % HISTORY_SIZE], text, MAX_LINE + 1);
}

void
Readline::put(const char *text) {
    for (; *text; text++) {
        if (outputLength == OUTPUT_SIZE) {
            flushOutput();
        }
        output[outputLength++] = *text;
    }
}

void
Readline::putCap(Terminfo::Cap cap, int p1) {
    char sequence[32];
    info->format(cap, sequence, sizeof(sequence), p1);
    put(sequence);
}

void
Readline::flushOutput() {
    if (outputLength) {
        terminal.write(output, outputLength);
        outputLength = 0;
    }
}

// To the column where the prompt starts.
void
Readline::startOfLine() {
    put("\r");
    if (origin) {
        putCap(Terminfo::Cap::RightBy, (int)origin);
    }
}

void
Readline::load(const char *text) {
    String::copy(line, text, limit + 1);
    length = cursor = (unsigned)String::length(line);
}

void
Readline::redraw() {
    startOfLine();
    put(prompt);
    put(line);
    putCap(Terminfo::Cap::ClearToEnd);
    if (cursor < length) {
        putCap(Terminfo::Cap::LeftBy, (int)(length - cursor));
    }
    flushOutput();
}

void
Readline::drawSearch() {
    char text[MAX_LINE * 2 + 40];
    const char *shown = match >= 0 ? historyAt((unsigned)match) : "";
    Format::print(text, sizeof(text), "(%sreverse-i-search)`%s': %s", match < 0 && queryLength ? "failed " : "",
                  query, shown);
    unsigned room = columns > origin + 1 ? columns - origin - 1 : 1;
    if (String::length(text) > room) {
        text[room] = 0;
    }
    startOfLine();
    put(text);
    putCap(Terminfo::Cap::ClearToEnd);
    flushOutput();
}

bool
Readline::findMatch(int from) {
    if (!queryLength) {
        match = -1;
        return false;
    }
    for (int i = from; i >= 0; i--) {
        int at = readlineIndexOf(historyAt((unsigned)i), query);
        if (at >= 0) {
            match = i;
            matchOffset = at;
            return true;
        }
    }
    return false;
}

void
Readline::beginSearch() {
    searching = true;
    String::copy(before, line, sizeof(before));
    query[0] = 0;
    queryLength = 0;
    match = -1;
    matchOffset = 0;
    drawSearch();
}

void
Readline::endSearch(bool accept) {
    searching = false;
    if (accept && match >= 0) {
        load(historyAt((unsigned)match));
        cursor = (unsigned)matchOffset < length ? (unsigned)matchOffset : length;
        position = count;
    } else {
        load(before);
    }
    redraw();
}

Readline::Step
Readline::searchKey(int key) {
    if (key == KEY_ESC || key == KEY_CTRL_G || key == KEY_CTRL_C) {
        endSearch(false);
        return Step::Continue;
    }
    if (key == KEY_ENTER) {
        endSearch(true);
        return Step::Enter;
    }
    if (key == KEY_CTRL_R) {
        if (match > 0) {
            findMatch(match - 1);
        } else if (match < 0) {
            findMatch((int)count - 1);
        }
    } else if (key == KEY_BACKSPACE) {
        if (queryLength) {
            query[--queryLength] = 0;
            match = -1;
            findMatch((int)count - 1);
        }
    } else if (key >= ' ' && key < 0x7F) {
        if (queryLength < limit) {
            query[queryLength++] = (char)key;
            query[queryLength] = 0;
            if (match < 0 || readlineIndexOf(historyAt((unsigned)match), query) < 0) {
                int from = match >= 0 ? match : (int)count - 1;
                match = -1;
                findMatch(from);
            } else {
                matchOffset = readlineIndexOf(historyAt((unsigned)match), query);
            }
        }
    } else {
        endSearch(true);
        return edit(key);
    }
    drawSearch();
    return Step::Continue;
}

bool
Readline::completes(const char *candidate) const {
    for (unsigned i = 0; i < cursor; i++) {
        if (candidate[i] != line[i]) {
            return false;
        }
    }
    return true;
}

// Completes the first word of the line, ending at the cursor, with the completions that start with it: a single match
// is inserted (plus a space at the end of the line), several matches are listed when listMatches is set.
void
Readline::complete(bool listMatches) {
    if (!completions) {
        return;
    }
    for (unsigned i = 0; i < cursor; i++) {
        if (line[i] == ' ') {
            return;
        }
    }
    if (cursor < length && line[cursor] != ' ') {
        return;
    }
    unsigned matches = 0;
    const char *found = nullptr;
    for (unsigned i = 0; i < completions->count(); i++) {
        if (completes(completions->at(i))) {
            matches++;
            found = completions->at(i);
        }
    }
    if (matches == 1) {
        unsigned extra = (unsigned)String::length(found) - cursor;
        bool space = cursor == length;
        if (length + extra + (space ? 1 : 0) > limit) {
            return;
        }
        Memory::move(line + cursor + extra, line + cursor, length - cursor + 1);
        Memory::copy(line + cursor, found + cursor, extra);
        cursor += extra;
        length += extra;
        if (space) {
            line[cursor++] = ' ';
            line[cursor] = 0;
            length++;
        }
        redraw();
        return;
    }
    if (matches > 1 && listMatches) {
        put("\n");
        for (unsigned i = 0; i < completions->count(); i++) {
            const char *candidate = completions->at(i);
            if (completes(candidate)) {
                put(candidate);
                put("\n");
            }
        }
        origin = 0;
        redraw();
    }
}

Readline::Step
Readline::edit(int key) {
    switch (key) {
    case KEY_NONE:
        return Step::Continue;
    case KEY_ENTER:
        return Step::Enter;
    case KEY_ESC:
        return escapeEnds ? Step::Escape : Step::Continue;
    case KEY_CTRL_R:
        beginSearch();
        return Step::Continue;
    case KEY_TAB:
        complete(lastKey == KEY_TAB);
        return Step::Continue;
    case KEY_CTRL_C:
        put("^C");
        length = cursor = 0;
        line[0] = 0;
        return Step::Enter;
    case KEY_CTRL_L:
        putCap(Terminfo::Cap::Clear);
        origin = 0;
        break;
    case KEY_CTRL_B:
    case special(Terminfo::Key::Left):
        if (cursor > 0) {
            cursor--;
        }
        break;
    case KEY_CTRL_F:
    case special(Terminfo::Key::Right):
        if (cursor < length) {
            cursor++;
        }
        break;
    case KEY_CTRL_A:
    case special(Terminfo::Key::Home):
        cursor = 0;
        break;
    case KEY_CTRL_E:
    case special(Terminfo::Key::End):
        cursor = length;
        break;
    case KEY_CTRL_P:
    case special(Terminfo::Key::Up):
        if (position > 0) {
            if (position == count) {
                String::copy(draft, line, sizeof(draft));
            }
            load(historyAt(--position));
        }
        break;
    case KEY_CTRL_N:
    case special(Terminfo::Key::Down):
        if (position < count) {
            position++;
            load(position == count ? draft : historyAt(position));
        }
        break;
    case KEY_CTRL_U:
        Memory::move(line, line + cursor, length - cursor + 1);
        length -= cursor;
        cursor = 0;
        break;
    case KEY_CTRL_K:
        line[cursor] = 0;
        length = cursor;
        break;
    case KEY_CTRL_D:
        if (length == 0) {
            return Step::Eof;
        }
        [[fallthrough]];
    case special(Terminfo::Key::Delete):
        if (cursor < length) {
            Memory::move(line + cursor, line + cursor + 1, length - cursor);
            length--;
        }
        break;
    case KEY_BACKSPACE:
        if (cursor == 0) {
            return Step::Continue;
        }
        Memory::move(line + cursor - 1, line + cursor, length - cursor + 1);
        cursor--;
        length--;
        // At the end of the line only the last character goes: less to send on a slow line.
        if (cursor == length) {
            putCap(Terminfo::Cap::Left);
            putCap(Terminfo::Cap::ClearToEnd);
            flushOutput();
            return Step::Continue;
        }
        break;
    default: {
        if (key < ' ' || key >= 0x7F || length >= limit) {
            return Step::Continue;
        }
        bool appending = cursor == length;
        Memory::move(line + cursor + 1, line + cursor, length - cursor + 1);
        line[cursor++] = (char)key;
        length++;
        if (appending) {
            char typed[2] = {(char)key, 0};
            put(typed);
            flushOutput();
            return Step::Continue;
        }
        break;
    }
    }
    redraw();
    return Step::Continue;
}

bool
Readline::readByte(uint8_t &byte, int timeoutMs) {
    for (;;) {
        if (!terminal.waitReadable(timeoutMs)) {
            return false;
        }
        int64_t got = terminal.read(&byte, 1);
        if (got == 1) {
            return true;
        }
        if (got == 0 && timeoutMs >= 0) {
            return false;
        }
    }
}

// The next key: a byte, or KEY_SPECIAL plus the key whose sequence came in. A lone ESC is told from the start of a
// sequence by the time the next byte takes to come; unknown sequences are read to their end and dropped.
int
Readline::nextKey() {
    uint8_t c;
    if (!readByte(c, -1)) {
        return KEY_NONE;
    }
    if (c == '\r' || c == '\n') {
        return KEY_ENTER;
    }
    if (c == 0x7F || c == '\b') {
        return KEY_BACKSPACE;
    }
    if (c != KEY_ESC) {
        return c;
    }
    uint8_t sequence[SEQUENCE_MAX];
    size_t n = 0;
    sequence[n++] = c;
    for (;;) {
        Terminfo::Key key;
        size_t used;
        Terminfo::Match m = info->decode(sequence, n, key, used);
        if (m == Terminfo::Match::Full) {
            return special(key);
        }
        if (m == Terminfo::Match::No) {
            bool control = n >= 2 && (sequence[1] == '[' || sequence[1] == 'O');
            uint8_t last = sequence[n - 1];
            bool ended = n > 2 && last >= 0x40 && last <= 0x7E;
            while (control && !ended && readByte(last, ESCAPE_TIMEOUT_MS)) {
                ended = last >= 0x40 && last <= 0x7E;
            }
            return KEY_NONE;
        }
        if (n == SEQUENCE_MAX || !readByte(sequence[n], ESCAPE_TIMEOUT_MS)) {
            return n == 1 ? KEY_ESC : KEY_NONE;
        }
        n++;
    }
}

Readline::Result
Readline::read(const char *promptText, char *out, unsigned capacity) {
    Tty::Mode saved;
    terminal.getMode(saved);
    Tty::Mode raw = saved;
    raw.localFlags &= ~(Tty::CANONICAL | Tty::ECHO_INPUT | Tty::ECHO_NL | Tty::EXTENDED_INPUT);
    raw.inputFlags &= ~(Tty::CR_TO_NL | Tty::NL_TO_CR | Tty::IGNORE_CR | Tty::OUTPUT_FLOW_CONTROL);
    raw.controlChars[Tty::CC_MIN] = 1;
    raw.controlChars[Tty::CC_TIME] = 0;
    terminal.setMode(raw, false);

    Tty::WindowSize size;
    terminal.windowSize(size);
    columns = size.columns ? size.columns : 80;
    outputLength = 0;
    prompt = promptText;
    promptWidth = (unsigned)String::length(promptText);
    origin = terminal.column();
    if (origin + promptWidth + 2 > columns) {
        put("\n");
        origin = 0;
    }
    limit = columns - origin - promptWidth - 1;
    if (limit > MAX_LINE) {
        limit = MAX_LINE;
    }
    if (limit > capacity - 1) {
        limit = capacity - 1;
    }
    line[0] = 0;
    length = cursor = 0;
    position = count;
    lastKey = 0;
    searching = false;
    putCap(Terminfo::Cap::CursorNormal);
    redraw();

    Step step;
    for (;;) {
        int key = nextKey();
        step = searching ? searchKey(key) : edit(key);
        lastKey = key;
        if (step != Step::Continue) {
            break;
        }
    }
    if (step == Step::Enter) {
        if (cursor < length) {
            putCap(Terminfo::Cap::RightBy, (int)(length - cursor));
        }
        String::copy(out, line, capacity);
    }
    put("\n");
    flushOutput();
    terminal.setMode(saved, false);
    return step == Step::Enter ? Result::Line : step == Step::Eof ? Result::Eof : Result::Escape;
}
