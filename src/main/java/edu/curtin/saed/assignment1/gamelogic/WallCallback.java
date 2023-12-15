package edu.curtin.saed.assignment1.gamelogic;

import edu.curtin.saed.assignment1.worldobjects.Wall;
import java.util.List;

@FunctionalInterface
public interface WallCallback {
    void provide(List<Wall> walls);
}
