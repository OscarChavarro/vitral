#include <cstdio>

#include "processing/MarkerGenerator.h"
#include <tag36h11.h>
MarkerGenerator::MarkerGenerator() {
    family = tag36h11_create();
    if (!family) {
        fprintf(stderr, "[MarkerGenerator] Failed to create tag36h11 family\n");
    }
}

MarkerGenerator::~MarkerGenerator() {
    if (family) {
        tag36h11_destroy(family);
    }
}

Marker MarkerGenerator::generate(int id) throw() {
    if (id < 0 || id > MAX_ID) {
        fprintf(stderr, "[MarkerGenerator] Invalid ID: %d (must be 0-586)\n", id);
        return Marker(-1, nullptr);
    }

    Grid* grid = new Grid();
    const int nbits = family->nbits;
    const uint64_t code = family->codes[id];

    // Fill grid with bit data (0-indexed, 6x6 interior)
    for (int i = 0; i < nbits; ++i) {
        const int bit = (code >> (nbits - 1 - i)) & 1ULL;
        const int bx = static_cast<int>(family->bit_x[i]);
        const int by = static_cast<int>(family->bit_y[i]);

        // Map tag bit coordinates (1-6) to the 6x6 interior grid (0-5),
        // inverting both axes as in the original generator.
        int gridX = 6 - bx;
        int gridY = 6 - by;

        if (gridX >= 0 && gridX < Grid::SIZE && gridY >= 0 && gridY < Grid::SIZE) {
            grid->set(gridX, gridY, (bit == 0));
        }
    }

    return Marker(id, grid);
}
