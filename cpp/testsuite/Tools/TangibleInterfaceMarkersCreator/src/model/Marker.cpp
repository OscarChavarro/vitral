#include "model/Marker.h"

Marker::Marker(int id, Grid* grid) : id(id), grid(grid) {
}

Marker::~Marker() {
    if (grid) {
        delete grid;
        grid = nullptr;
    }
}

int Marker::getId() const {
    return id;
}

Grid* Marker::getGrid() const {
    return grid;
}
