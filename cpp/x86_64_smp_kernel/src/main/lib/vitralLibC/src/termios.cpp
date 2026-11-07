#include <termios.h>
#include <errno.h>
#include <stdarg.h>
#include <stdint.h>
#include <sys/ioctl.h>
#include <unistd.h>
#include "drivers/tty/Tty.h"
#include "fs/Err.h"
#include "syscalls/Syscalls.h"

static_assert(NCCS == Tty::CONTROL_CHARS, "termios c_cc");
static_assert(VERASE == Tty::CC_ERASE && VKILL == Tty::CC_KILL && VEOF == Tty::CC_EOF, "termios c_cc");
static_assert(VTIME == Tty::CC_TIME && VMIN == Tty::CC_MIN && VSUSP == Tty::CC_SUSPEND, "termios c_cc");
static_assert(VEOL == Tty::CC_EOL && VEOL2 == Tty::CC_EOL2, "termios c_cc");
static_assert(IGNBRK == Tty::IGNORE_BREAK && BRKINT == Tty::BREAK_INTERRUPT && IGNPAR == Tty::IGNORE_PARITY,
              "termios c_iflag");
static_assert(PARMRK == Tty::MARK_PARITY && INPCK == Tty::PARITY_CHECK && INLCR == Tty::NL_TO_CR, "termios c_iflag");
static_assert(IGNCR == Tty::IGNORE_CR && ICRNL == Tty::CR_TO_NL && IUCLC == Tty::UPPER_TO_LOWER, "termios c_iflag");
static_assert(IXON == Tty::OUTPUT_FLOW_CONTROL && IXOFF == Tty::INPUT_FLOW_CONTROL, "termios c_iflag");
static_assert(OPOST == Tty::POST_PROCESS && ONLCR == Tty::NL_TO_CRNL && ONOCR == Tty::NO_CR_AT_COLUMN_0,
              "termios c_oflag");
static_assert(ISIG == Tty::SIGNALS && ICANON == Tty::CANONICAL && ECHO == Tty::ECHO_INPUT, "termios c_lflag");
static_assert(ECHOE == Tty::ECHO_ERASE && ECHOK == Tty::ECHO_KILL && ECHONL == Tty::ECHO_NL, "termios c_lflag");
static_assert(NOFLSH == Tty::NO_FLUSH && ECHOCTL == Tty::ECHO_CONTROL && ECHOKE == Tty::ECHO_KILL_ERASE,
              "termios c_lflag");
static_assert(IEXTEN == Tty::EXTENDED_INPUT, "termios c_lflag");

static_assert(TCGETS == Tty::GET_MODE && TCSETS == Tty::SET_MODE && TCSETSW == Tty::SET_MODE_DRAIN, "ioctl");
static_assert(TCSETSF == Tty::SET_MODE_FLUSH && TCFLSH == Tty::FLUSH, "ioctl");
static_assert(TIOCGWINSZ == Tty::GET_WINDOW_SIZE && TIOCSWINSZ == Tty::SET_WINDOW_SIZE, "ioctl");
static_assert(sizeof(winsize) == sizeof(Tty::WindowSize), "struct winsize");

static int
failWithTerminal(int64_t result) {
    errno = result == -Err::BADF ? EBADF : result == -Err::FAULT ? EFAULT : ENOTTY;
    return -1;
}

static void
toTermios(const Tty::Mode &m, struct termios *mode) {
    mode->c_iflag = m.inputFlags;
    mode->c_oflag = m.outputFlags;
    mode->c_cflag = m.controlFlags;
    mode->c_lflag = m.localFlags;
    mode->c_line = 0;
    for (int i = 0; i < NCCS; i++) {
        mode->c_cc[i] = m.controlChars[i];
    }
    mode->c_ispeed = 0;
    mode->c_ospeed = 0;
}

static void
fromTermios(const struct termios *mode, Tty::Mode &m) {
    m.inputFlags = mode->c_iflag;
    m.outputFlags = mode->c_oflag;
    m.controlFlags = mode->c_cflag;
    m.localFlags = mode->c_lflag;
    for (int i = 0; i < NCCS; i++) {
        m.controlChars[i] = mode->c_cc[i];
    }
}

int
tcgetattr(int descriptor, struct termios *mode) {
    Tty::Mode m;
    int64_t r = Syscalls::ioctl(descriptor, Tty::GET_MODE, &m);
    if (r < 0) {
        return failWithTerminal(r);
    }
    toTermios(m, mode);
    return 0;
}

int
tcsetattr(int descriptor, int action, const struct termios *mode) {
    if (action != TCSANOW && action != TCSADRAIN && action != TCSAFLUSH) {
        errno = EINVAL;
        return -1;
    }
    Tty::Mode m;
    fromTermios(mode, m);
    uint64_t request = action == TCSAFLUSH ? Tty::SET_MODE_FLUSH : action == TCSADRAIN ? Tty::SET_MODE_DRAIN
                                                                                       : Tty::SET_MODE;
    int64_t r = Syscalls::ioctl(descriptor, request, &m);
    return r < 0 ? failWithTerminal(r) : 0;
}

int
tcflush(int descriptor, int queue) {
    if (queue != TCIFLUSH && queue != TCOFLUSH && queue != TCIOFLUSH) {
        errno = EINVAL;
        return -1;
    }
    int64_t r = Syscalls::ioctl(descriptor, Tty::FLUSH, (void *)(uintptr_t)queue);
    return r < 0 ? failWithTerminal(r) : 0;
}

// The terminal requests, with struct termios translated to the kernel's Mode; any other request goes to the device.
int
ioctl(int descriptor, unsigned long request, ...) {
    va_list arguments;
    va_start(arguments, request);
    void *argument = va_arg(arguments, void *);
    va_end(arguments);
    switch (request) {
    case TCGETS:
        return tcgetattr(descriptor, (struct termios *)argument);
    case TCSETS:
        return tcsetattr(descriptor, TCSANOW, (const struct termios *)argument);
    case TCSETSW:
        return tcsetattr(descriptor, TCSADRAIN, (const struct termios *)argument);
    case TCSETSF:
        return tcsetattr(descriptor, TCSAFLUSH, (const struct termios *)argument);
    default: {
        int64_t r = Syscalls::ioctl(descriptor, request, argument);
        if (r < 0) {
            errno = (int)-r;
            return -1;
        }
        return (int)r;
    }
    }
}

void
cfmakeraw(struct termios *mode) {
    mode->c_iflag &= ~(IGNBRK | BRKINT | PARMRK | ISTRIP | INLCR | IGNCR | ICRNL | IXON);
    mode->c_oflag &= ~OPOST;
    mode->c_lflag &= ~(ECHO | ECHONL | ICANON | ISIG | IEXTEN);
    mode->c_cflag &= ~(CSIZE | PARENB);
    mode->c_cflag |= CS8;
    mode->c_cc[VMIN] = 1;
    mode->c_cc[VTIME] = 0;
}
