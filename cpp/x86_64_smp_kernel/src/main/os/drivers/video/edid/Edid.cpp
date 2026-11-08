#include "drivers/video/edid/Edid.h"

// Established timings I and II (bytes 35 and 36) and the manufacturer's byte 37, most significant bit first.
class EdidEstablishedMode {
public:
    uint16_t width;
    uint16_t height;
    uint8_t refresh;
};

static const EdidEstablishedMode ESTABLISHED[24] = {
    {720, 400, 70}, {720, 400, 88}, {640, 480, 60}, {640, 480, 67},
    {640, 480, 72}, {640, 480, 75}, {800, 600, 56}, {800, 600, 60},
    {800, 600, 72}, {800, 600, 75}, {832, 624, 75}, {1024, 768, 87},
    {1024, 768, 60}, {1024, 768, 70}, {1024, 768, 75}, {1280, 1024, 75},
    {1152, 870, 75}, {0, 0, 0}, {0, 0, 0}, {0, 0, 0},
    {0, 0, 0}, {0, 0, 0}, {0, 0, 0}, {0, 0, 0},
};

static bool
edidClose(unsigned a, unsigned b) {
    return a + 1 >= b && b + 1 >= a;
}

bool
Edid::parse(const uint8_t *block) {
    static const uint8_t HEADER[8] = {0x00, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x00};
    isValid = false;
    uint8_t sum = 0;
    for (unsigned i = 0; i < BLOCK_BYTES; i++) {
        sum = (uint8_t)(sum + block[i]);
    }
    for (unsigned i = 0; i < 8; i++) {
        if (block[i] != HEADER[i]) {
            return false;
        }
    }
    if (sum != 0) {
        return false;
    }

    uint16_t id = (uint16_t)(block[8] << 8 | block[9]);
    vendorCode[0] = (char)('A' - 1 + (id >> 10 & 31));
    vendorCode[1] = (char)('A' - 1 + (id >> 5 & 31));
    vendorCode[2] = (char)('A' - 1 + (id & 31));
    vendorCode[3] = 0;
    productCode = (uint16_t)(block[10] | block[11] << 8);
    analog = !(block[20] & 0x80);
    monitorName[0] = 0;
    detailed = 0;
    hasRange = false;

    for (unsigned i = 0; i < 3; i++) {
        established[i] = block[35 + i];
    }
    standardCount = 0;
    for (unsigned i = 0; i < MAX_STANDARD; i++) {
        uint8_t first = block[38 + 2 * i];
        uint8_t second = block[39 + 2 * i];
        if ((first == 0x01 && second == 0x01) || first == 0) {
            continue;
        }
        unsigned width = (first + 31u) * 8;
        unsigned height;
        switch (second >> 6) {
        case 0:
            height = width * 10 / 16;
            break;
        case 1:
            height = width * 3 / 4;
            break;
        case 2:
            height = width * 4 / 5;
            break;
        default:
            height = width * 9 / 16;
            break;
        }
        standardModes[standardCount].width = (uint16_t)width;
        standardModes[standardCount].height = (uint16_t)height;
        standardModes[standardCount].refresh = (uint8_t)((second & 0x3F) + 60);
        standardCount++;
    }

    for (unsigned i = 0; i < 4; i++) {
        parseDescriptor(block + 54 + 18 * i);
    }
    isValid = true;
    return true;
}

void
Edid::parseDescriptor(const uint8_t *d) {
    uint32_t clock = (uint32_t)(d[0] | d[1] << 8) * 10;
    if (clock != 0) {
        if (detailed >= MAX_DETAILED || (d[17] & 0x80)) {
            return;
        }
        unsigned hActive = d[2] | (d[4] & 0xF0u) << 4;
        unsigned hBlank = d[3] | (d[4] & 0x0Fu) << 8;
        unsigned vActive = d[5] | (d[7] & 0xF0u) << 4;
        unsigned vBlank = d[6] | (d[7] & 0x0Fu) << 8;
        unsigned hSyncOffset = d[8] | (d[11] & 0xC0u) << 2;
        unsigned hSyncWidth = d[9] | (d[11] & 0x30u) << 4;
        unsigned vSyncOffset = (d[10] >> 4) | (d[11] & 0x0Cu) << 2;
        unsigned vSyncWidth = (d[10] & 0x0Fu) | (d[11] & 0x03u) << 4;
        DisplayTiming &t = timings[detailed];
        t.pixelClockKhz = clock;
        t.hDisplay = (uint16_t)hActive;
        t.hSyncStart = (uint16_t)(hActive + hSyncOffset);
        t.hSyncEnd = (uint16_t)(hActive + hSyncOffset + hSyncWidth);
        t.hTotal = (uint16_t)(hActive + hBlank);
        t.vDisplay = (uint16_t)vActive;
        t.vSyncStart = (uint16_t)(vActive + vSyncOffset);
        t.vSyncEnd = (uint16_t)(vActive + vSyncOffset + vSyncWidth);
        t.vTotal = (uint16_t)(vActive + vBlank);
        // Digital separate sync gives both polarities; analog and digital composite sync only the horizontal one.
        bool separate = (d[17] & 0x18) == 0x18;
        t.hSyncPositive = (d[17] & 0x02) != 0;
        t.vSyncPositive = separate ? (d[17] & 0x04) != 0 : false;
        if (t.sane()) {
            detailed++;
        }
        return;
    }
    if (d[3] == 0xFC) {
        unsigned n = 0;
        while (n < 13 && d[5 + n] != 0x0A && d[5 + n] != 0) {
            monitorName[n] = (char)d[5 + n];
            n++;
        }
        while (n > 0 && monitorName[n - 1] == ' ') {
            n--;
        }
        monitorName[n] = 0;
    } else if (d[3] == 0xFD) {
        hasRange = true;
        minVerticalHz = d[5] + ((d[4] & 0x01) ? 255u : 0u);
        maxVerticalHz = d[6] + ((d[4] & 0x02) ? 255u : 0u);
        minHorizontalKhz = d[7] + ((d[4] & 0x04) ? 255u : 0u);
        maxHorizontalKhz = d[8] + ((d[4] & 0x08) ? 255u : 0u);
        maxPixelClockKhz = (uint32_t)d[9] * 10000;
    }
}

const DisplayTiming *
Edid::detailedFor(unsigned width, unsigned height) const {
    for (unsigned i = 0; i < detailed; i++) {
        if (timings[i].hDisplay == width && timings[i].vDisplay == height) {
            return &timings[i];
        }
    }
    return nullptr;
}

bool
Edid::listed(unsigned width, unsigned height, unsigned refresh) const {
    for (unsigned i = 0; i < 24; i++) {
        const EdidEstablishedMode &m = ESTABLISHED[i];
        if (m.width != 0 && (established[i / 8] & (0x80 >> (i % 8))) && m.width == width && m.height == height &&
            edidClose(m.refresh, refresh)) {
            return true;
        }
    }
    for (unsigned i = 0; i < standardCount; i++) {
        const ListedMode &m = standardModes[i];
        if (m.width == width && m.height == height && edidClose(m.refresh, refresh)) {
            return true;
        }
    }
    return false;
}

bool
Edid::supports(const DisplayTiming &timing) const {
    if (!isValid) {
        return false;
    }
    unsigned refresh = timing.refreshHz();
    for (unsigned i = 0; i < detailed; i++) {
        if (timings[i].hDisplay == timing.hDisplay && timings[i].vDisplay == timing.vDisplay &&
            edidClose(timings[i].refreshHz(), refresh)) {
            return true;
        }
    }
    if (listed(timing.hDisplay, timing.vDisplay, refresh)) {
        return true;
    }
    if (!hasRange) {
        return false;
    }
    uint32_t lineKhz = (timing.lineHz() + 500) / 1000;
    return refresh >= minVerticalHz && refresh <= maxVerticalHz && lineKhz >= minHorizontalKhz &&
           lineKhz <= maxHorizontalKhz && (maxPixelClockKhz == 0 || timing.pixelClockKhz <= maxPixelClockKhz);
}
