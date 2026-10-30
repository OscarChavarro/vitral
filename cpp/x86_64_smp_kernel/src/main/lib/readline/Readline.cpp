#include "lib/readline/Readline.h"
#include "drivers/keyboard/Keyboard.h"
#include "drivers/video/virtualConsole/VirtualConsole.h"
#include "lib/vitralKernel/Format.h"
#include "lib/vitralKernel/Memory.h"
#include "syscalls/Syscalls.h"
#include "lib/vitralKernel/String.h"

static constexpr uint8_t NORMAL = 0x07;

static int
indexOf(const char *text, const char *needle) {
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
Readline::load(const char *text) {
    String::copy(line, text, limit + 1);
    length = cursor = (unsigned)String::length(line);
}

void
Readline::redraw() {
    VirtualConsole::fillRow(console, row, ' ', NORMAL);
    VirtualConsole::writeAt(console, 0, row, prompt, NORMAL);
    VirtualConsole::writeAt(console, (int)promptWidth, row, line, NORMAL);
    VirtualConsole::setCursor(console, (int)(promptWidth + cursor), row);
}

void
Readline::drawSearch() {
    char text[VirtualConsole::COLS + 1];
    const char *shown = match >= 0 ? historyAt((unsigned)match) : "";
    Format::print(text, sizeof(text), "(%sreverse-i-search)`%s': %s", match < 0 && queryLength ? "failed " : "",
                  query, shown);
    VirtualConsole::fillRow(console, row, ' ', NORMAL);
    VirtualConsole::writeAt(console, 0, row, text, NORMAL);
    int x = (int)String::length(text);
    VirtualConsole::setCursor(console, x < VirtualConsole::COLS ? x : VirtualConsole::COLS - 1, row);
}

bool
Readline::findMatch(int from) {
    if (!queryLength) {
        match = -1;
        return false;
    }
    for (int i = from; i >= 0; i--) {
        int at = indexOf(historyAt((unsigned)i), query);
        if (at >= 0) { match = i; matchOffset = at; return true; }
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
    if (key == Keyboard::KEY_ESC || key == KEY_CTRL_G) {
        endSearch(false);
        return Step::Continue;
    }
    if (key == Keyboard::KEY_ENTER) {
        endSearch(true);
        return Step::Enter;
    }
    if (key == KEY_CTRL_R) {
        if (match > 0) {
            findMatch(match - 1);
        } else if (match < 0) {
            findMatch((int)count - 1);
        }
    } else if (key == '\b') {
        if (queryLength) {
            query[--queryLength] = 0;
            match = -1;
            findMatch((int)count - 1);
        }
    } else if (key >= ' ' && key < 0x7F) {
        if (queryLength < limit) {
            query[queryLength++] = (char)key;
            query[queryLength] = 0;
            if (match < 0 || indexOf(historyAt((unsigned)match), query) < 0) {
                int from = match >= 0 ? match : (int)count - 1;
                match = -1;
                findMatch(from);
            } else {
                matchOffset = indexOf(historyAt((unsigned)match), query);
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
        VirtualConsole::setCursor(console, (int)(promptWidth + length), row);
        VirtualConsole::putc(console, '\n');
        for (unsigned i = 0; i < completions->count(); i++) {
            const char *candidate = completions->at(i);
            if (!completes(candidate)) {
                continue;
            }
            for (const char *p = candidate; *p; p++) {
                VirtualConsole::putc(console, *p);
            }
            VirtualConsole::putc(console, '\n');
        }
        row = VirtualConsole::cursorY(console);
        redraw();
    }
}

Readline::Step
Readline::edit(int key) {
    if (key == Keyboard::KEY_ENTER) {
        return Step::Enter;
    }
    if (key == Keyboard::KEY_ESC) {
        return escapeEnds ? Step::Escape : Step::Continue;
    }
    if (key == KEY_CTRL_R) {
        beginSearch();
        return Step::Continue;
    }
    if (key == KEY_TAB) {
        complete(lastKey == KEY_TAB);
        return Step::Continue;
    }
    if (key == Keyboard::KEY_LEFT) {
        if (cursor > 0) {
            cursor--;
        }
    } else if (key == Keyboard::KEY_RIGHT) {
        if (cursor < length) {
            cursor++;
        }
    } else if (key == KEY_CTRL_A) {
        cursor = 0;
    } else if (key == KEY_CTRL_E) {
        cursor = length;
    } else if (key == Keyboard::KEY_UP) {
        if (position > 0) {
            if (position == count) {
                String::copy(draft, line, sizeof(draft));
            }
            load(historyAt(--position));
        }
    } else if (key == Keyboard::KEY_DOWN) {
        if (position < count) {
            position++;
            load(position == count ? draft : historyAt(position));
        }
    } else if (key == KEY_CTRL_D) {
        if (length == 0) {
            return Step::Eof;
        }
        if (cursor < length) {
            Memory::move(line + cursor, line + cursor + 1, length - cursor);
            length--;
        }
    } else if (key == '\b') {
        if (cursor > 0) {
            Memory::move(line + cursor - 1, line + cursor, length - cursor + 1);
            cursor--;
            length--;
        }
    } else if (key >= ' ' && key < 0x7F && length < limit) {
        Memory::move(line + cursor + 1, line + cursor, length - cursor + 1);
        line[cursor++] = (char)key;
        length++;
    }
    redraw();
    return Step::Continue;
}

Readline::Result
Readline::read(const char *promptText, char *out, unsigned capacity) {
    prompt = promptText;
    promptWidth = (unsigned)String::length(promptText);
    limit = VirtualConsole::COLS - promptWidth - 1;
    if (limit > MAX_LINE) {
        limit = MAX_LINE;
    }
    if (limit > capacity - 1) {
        limit = capacity - 1;
    }
    row = VirtualConsole::cursorY(console);
    line[0] = 0;
    length = cursor = 0;
    position = count;
    lastKey = 0;
    searching = false;
    VirtualConsole::showCursor(console, true);
    redraw();

    Step step;
    for (;;) {
        int key = Keyboard::read();
        if (key < 0) { Syscalls::sleep(20); continue; }
        step = searching ? searchKey(key) : edit(key);
        lastKey = key;
        if (step != Step::Continue) {
            break;
        }
    }
    if (step == Step::Enter) {
        VirtualConsole::setCursor(console, (int)(promptWidth + length), row);
        String::copy(out, line, capacity);
    }
    VirtualConsole::putc(console, '\n');
    return step == Step::Enter ? Result::Line : step == Step::Eof ? Result::Eof : Result::Escape;
}
