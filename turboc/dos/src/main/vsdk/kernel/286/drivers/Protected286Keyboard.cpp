#include "drivers/Protected286Keyboard.h"
#include "drivers/Protected286Ports.h"

volatile Protected286Byte Protected286Keyboard::queue[PROTECTED_286_KEY_QUEUE_SIZE];
volatile Protected286Byte Protected286Keyboard::head = 0;
volatile Protected286Byte Protected286Keyboard::tail = 0;
Protected286Byte Protected286Keyboard::shift = 0;

static const char protected286Plain[0x3A] = {
    0, 27, '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '-', '=', '\b', '\t',
    'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', '[', ']', '\n', 0, 'a', 's',
    'd', 'f', 'g', 'h', 'j', 'k', 'l', ';', '\'', '`', 0, '\\', 'z', 'x', 'c', 'v',
    'b', 'n', 'm', ',', '.', '/', 0, '*', 0, ' '
};
static const char protected286Shifted[0x3A] = {
    0, 27, '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '_', '+', '\b', '\t',
    'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', '{', '}', '\n', 0, 'A', 'S',
    'D', 'F', 'G', 'H', 'J', 'K', 'L', ':', '"', '~', 0, '|', 'Z', 'X', 'C', 'V',
    'B', 'N', 'M', '<', '>', '?', 0, '*', 0, ' '
};

void
Protected286Keyboard::initialize()
{
    head = 0;
    tail = 0;
    shift = 0;
    while (Protected286Ports::inByte(0x64) & 1) {
        Protected286Ports::inByte(0x60);
    }
}

void
Protected286Keyboard::push(Protected286Byte character)
{
    Protected286Byte next = (Protected286Byte)((head + 1) & (PROTECTED_286_KEY_QUEUE_SIZE - 1));
    if (next != tail) {
        queue[head] = character;
        head = next;
    }
}

void
Protected286Keyboard::onInterrupt()
{
    Protected286Byte scanCode = Protected286Ports::inByte(0x60);
    Protected286Byte released = scanCode & 0x80;
    scanCode &= 0x7F;
    if (scanCode == 0x2A || scanCode == 0x36) {
        shift = released == 0;
    }
    else if (!released && scanCode < sizeof(protected286Plain)) {
        Protected286Byte character = shift ? protected286Shifted[scanCode] : protected286Plain[scanCode];
        if (character != 0) {
            push(character);
        }
    }
}

int
Protected286Keyboard::getCharacter()
{
    Protected286Byte character;
    if (tail == head) {
        return -1;
    }
    character = queue[tail];
    tail = (Protected286Byte)((tail + 1) & (PROTECTED_286_KEY_QUEUE_SIZE - 1));
    return character;
}

int
Protected286Keyboard::waitCharacter()
{
    int character;
    do {
        character = getCharacter();
        if (character < 0 && (Protected286Ports::inByte(0x64) & 1)) {
            onInterrupt();
        }
    } while (character < 0);
    return character;
}
