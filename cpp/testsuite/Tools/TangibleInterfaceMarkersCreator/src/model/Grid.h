#ifndef __GRID_H__
#define __GRID_H__

class Grid {
  public:
    static constexpr int SIZE = 6;
    Grid();
    ~Grid();

    bool get(int row, int col) const;
    void set(int row, int col, bool value);

  private:
    bool data[SIZE][SIZE];
};

#endif
