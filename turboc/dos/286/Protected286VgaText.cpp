#include <dos.h>

#include "Protected286Ports.h"
#include "Protected286VgaText.h"

Protected286Word Protected286VgaText::x = 0;
Protected286Word Protected286VgaText::y = 0;
Protected286Word Protected286VgaText::consoleRows = 25;
Protected286Byte Protected286VgaText::attribute = 0x07;

void
Protected286VgaText::initialize()
{
    x = 0;
    y = 0;
    consoleRows = 25;
}

void
Protected286VgaText::putCell(Protected286Word column, Protected286Word row, char character)
{
    Protected286Word far* video = (Protected286Word far*)MK_FP(PROTECTED_286_VIDEO_SELECTOR, 0);
    video[row * 80 + column] = ((Protected286Word)attribute << 8) | (Protected286Byte)character;
}

void
Protected286VgaText::updateCursor()
{
    Protected286Word position = y * 80 + x;
    Protected286Ports::outByte(0x3D4, 0x0F);
    Protected286Ports::outByte(0x3D5, (Protected286Byte)position);
    Protected286Ports::outByte(0x3D4, 0x0E);
    Protected286Ports::outByte(0x3D5, (Protected286Byte)(position >> 8));
}

void
Protected286VgaText::scroll()
{
    Protected286Word row;
    Protected286Word column;
    Protected286Word far* video = (Protected286Word far*)MK_FP(PROTECTED_286_VIDEO_SELECTOR, 0);
    for (row = 1; row < consoleRows; row++) {
        for (column = 0; column < 80; column++) {
            video[(row - 1) * 80 + column] = video[row * 80 + column];
        }
    }
    for (column = 0; column < 80; column++) {
        video[(consoleRows - 1) * 80 + column] = ((Protected286Word)attribute << 8) | ' ';
    }
    if (y) --y;
}

void
Protected286VgaText::clear(Protected286Byte newAttribute)
{
    Protected286Word row;
    Protected286Word column;
    attribute = newAttribute;
    for (row = 0; row < 25; row++) {
        for (column = 0; column < 80; column++) {
            putCell(column, row, ' ');
        }
    }
    x = 0;
    y = 0;
    updateCursor();
}

void
Protected286VgaText::putCharacter(char character)
{
    if (character == '\n') {
        x = 0;
        y++;
    }
    else if (character == '\r') {
        x = 0;
    }
    else {
        putCell(x, y, character);
        x++;
    }
    if (x >= 80) {
        x = 0;
        y++;
    }
    if (y >= consoleRows) {
        scroll();
    }
    updateCursor();
}

void
Protected286VgaText::write(const char* text)
{
    if (text == 0) {
        return;
    }
    while (*text != 0) {
        putCharacter(*text++);
    }
}

void
Protected286VgaText::writeAt(Protected286Word column, Protected286Word row,
                             const char* text)
{
    x = column;
    y = row;
    write(text);
}

void
Protected286VgaText::setConsoleRows(Protected286Word rows)
{
    if (!rows || rows > 25) return;
    // Scroll using the old height first, preserving the most recent lines
    // before the progress monitor reserves the bottom rows.
    while (y >= rows) scroll();
    consoleRows = rows;
    // The reserved area may still contain lines from the boot trace.
    for (Protected286Word row = rows; row < 25; ++row)
        for (Protected286Word column = 0; column < 80; ++column)
            putCell(column, row, ' ');
    updateCursor();
}

void
Protected286VgaText::writeStatusAt(Protected286Word column, Protected286Word row,
                                  const char* text)
{
    if (!text || row >= 25) return;
    while (*text && column < 80) putCell(column++, row, *text++);
}

void
Protected286VgaText::drawProgressBar(Protected286Word column,
                                     Protected286Word row,
                                     Protected286Word width,
                                     Protected286Word percent)
{
    Protected286Word i;
    Protected286Word filled;
    char percentage[5];

    if (percent > 100) {
        percent = 100;
    }
    if (row >= 25 || width > 70 || column + width + 8 > 80) {
        return;
    }
    filled = (Protected286Word)((width * percent) / 100);
    putCell(column, row, '[');
    for (i = 0; i < width; i++) {
        putCell((Protected286Word)(column + i + 1), row,
                i < filled ? '#' : '.');
    }
    putCell((Protected286Word)(column + width + 1), row, ']');
    percentage[0] = (char)('0' + percent / 100);
    percentage[1] = (char)('0' + (percent / 10) % 10);
    percentage[2] = (char)('0' + percent % 10);
    percentage[3] = '%';
    percentage[4] = 0;
    writeStatusAt((Protected286Word)(column + width + 4), row, percentage);
}
