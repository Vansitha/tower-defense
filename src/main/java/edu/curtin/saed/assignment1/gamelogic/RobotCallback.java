package edu.curtin.saed.assignment1.gamelogic;


import edu.curtin.saed.assignment1.worldobjects.Robot;

import java.util.List;

@FunctionalInterface
public interface RobotCallback {
    void provide(List<Robot> robots);
}