#include "model/Grid.h"

Grid::Grid() {
    for (int y = 0; y < SIZE; ++y) {
        for (int x = 0; x < SIZE; ++x) {
            data[y][x] = false;
        }
    }
}

Grid::~Grid() {
}

bool Grid::get(int row, int col) const {
    if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
        return false;
    }
    return data[row][col];
}

void Grid::set(int row, int col, bool value) {
    if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
        return;
    }
    data[row][col] = value;
}
