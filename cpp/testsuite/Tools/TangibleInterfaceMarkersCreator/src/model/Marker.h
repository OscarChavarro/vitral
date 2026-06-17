#ifndef __MARKER_H__
#define __MARKER_H__

#include "model/Grid.h"
class Marker {
  private:
    int id;
    Grid* grid;

  public:
    Marker(int id, Grid* grid);
    ~Marker();

    int getId() const;
    Grid* getGrid() const;
};

#endif
