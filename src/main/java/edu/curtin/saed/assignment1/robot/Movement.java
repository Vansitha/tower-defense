package edu.curtin.saed.assignment1.robot;

import edu.curtin.saed.assignment1.gamelogic.GameState;
import edu.curtin.saed.assignment1.worldobjects.Robot;
import edu.curtin.saed.assignment1.worldobjects.Wall;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Movement implements Runnable {
    private Robot robot;
    private final GameState gameState;

    public Movement(Robot robot, GameState gameState) {
        this.robot = robot;
        this.gameState = gameState;
    }

    @Override
    public void run() {
        if (robot == null) {
            return;
        }
        move();
    }

    /**
     * Moves the robot to a new position based on certain conditions and animation-like steps.
     */
    private void move() {
        double nextX = robot.getCurrX();
        double nextY = robot.getCurrY();

        // Calculate the direction towards the citadel
        double citadelX = gameState.getCitadelPositionX();
        double citadelY = gameState.getCitadelPositionY();
        double directionX = citadelX - robot.getCurrX();
        double directionY = citadelY - robot.getCurrY();

        // Apply a weight to the direction towards the citadel
        double moveTowardsCitadelProbability = 0.7;

        // Calculate the next position based on weighted direction
        double randomValue = Math.random();
        if (randomValue < moveTowardsCitadelProbability) {
            // Move towards the citadel either horizontally or vertically
            if (Math.abs(directionX) > Math.abs(directionY)) {
                // Move horizontally
                nextX += (directionX > 0) ? 1 : -1;
                // Reset Y to prevent diagonal movement
                nextY = robot.getCurrY();
            } else {
                // Move vertically
                nextY += (directionY > 0) ? 1 : -1;
                // Reset X to prevent diagonal movement
                nextX = robot.getCurrX();
            }
        } else {
            // Move randomly
            double randomX = Math.random() * 2 - 1; // Random value between -1 and 1
            double randomY = Math.random() * 2 - 1; // Random value between -1 and 1

            // Adjust random values to ensure horizontal or vertical movement
            if (Math.abs(randomX) > Math.abs(randomY)) {
                // Random horizontal movement
                nextX += randomX;
                // Reset Y to prevent diagonal movement
                nextY = robot.getCurrY();
            } else {
                // Random vertical movement
                nextY += randomY;
                // Reset X to prevent diagonal movement
                nextX = robot.getCurrX();
            }
        }

        nextX = Math.round(nextX);
        nextY = Math.round(nextY);

        if (!isGridBoundaryCheck(nextX, nextY)) {

            boolean areRobotsColliding = false;
            try {
                areRobotsColliding = checkRobotCollision(nextX, nextY).get();
            } catch (InterruptedException | ExecutionException e) {
                System.err.println(e.getMessage());
            }

            if (areRobotsColliding) {
                return;
            }

            // Calculate the step size for each animation frame
            double stepX = (nextX - robot.getCurrX()) / 10.0;
            double stepY = (nextY - robot.getCurrY()) / 10.0;

            robot.setPrevX(Math.round(robot.getCurrX()));
            robot.setPrevY(Math.round(robot.getCurrY()));
            robot.setNextX(nextX);
            robot.setNextY(nextY);

            /* Updates the position of the robot in 40 ms intervals for 10 steps therefore it takes 400ms to move the
              robot from the current position to the new position. */
            for (int i = 0; i < 10; i++) {
                final double newX = robot.getCurrX() + stepX;
                final double newY = robot.getCurrY() + stepY;

                robot.setCurrX(newX);
                robot.setCurrY(newY);

                if (i == 9) {
                    robot.setCurrX(Math.round(newX));
                    robot.setCurrY(Math.round(newY));
                }

                try {
                    Thread.sleep(40);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            checkIsCitadelDestroyed(gameState, robot.getCurrX(), robot.getCurrY());
            // All checks collision happen after the animation period
            checkWallCollision(robot.getCurrX(), robot.getCurrY());
        }
    }

    /**
     * Checks if the robot's current position matches the position of the citadel and sets the game over status accordingly.
     *
     * @param gameState The GameState object containing game-related data.
     * @param robotX    The X-coordinate of the robot's current position.
     * @param robotY    The Y-coordinate of the robot's current position.
     */
    private void checkIsCitadelDestroyed(GameState gameState, double robotX, double robotY) {
        if (robotX == gameState.getCitadelPositionX() && robotY == gameState.getCitadelPositionY()) {
            gameState.setGameOverStatus();
        }
    }

    /**
     * Checks if the specified coordinates are within the boundaries of the game grid.
     *
     * @param nextX The X-coordinate to be checked.
     * @param nextY The Y-coordinate to be checked.
     * @return True if the coordinates are outside the grid boundaries, otherwise false.
     */
    private boolean isGridBoundaryCheck(double nextX, double nextY) {
        boolean isBoundary = false;
        int gridWidth = 9;
        if (nextX < 0 || nextX > gridWidth - 1) {
            isBoundary = true;
        }
        int gridHeight = 9;
        if (nextY < 0 || nextY > gridHeight - 1) {
            isBoundary = true;
        }
        return isBoundary;
    }

    /**
     * Checks for collisions between the robot and walls at the specified coordinates.
     * If a collision is detected, it interacts with the game state to update or delete the wall
     * and delete the robot accordingly.
     *
     * @param robotNewPosX The new X-coordinate of the robot.
     * @param robotNewPosY The new Y-coordinate of the robot.
     */
    private void checkWallCollision(double robotNewPosX, double robotNewPosY) {
        try {
            gameState.asyncGetWalls((walls) -> {
                for (Wall wall : walls) {
                    double wallX = wall.getPositionX();
                    double wallY = wall.getPositionY();

                    if (wallX == robotNewPosX && wallY == robotNewPosY) {
                        try {
                            if (wall.isMaxHealth()) {
                                gameState.updateWall(wall);
                            } else {
                                gameState.deleteWall(wall);
                            }
                            gameState.deleteRobot(robot);
                            robot = null;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Asynchronously checks for collisions between the robot and other robots at the specified coordinates.
     * This method queries the game state for the positions of other robots and determines if the robot's
     * new position results in a collision with any of them.
     *
     * @param robotNewPosX The new X-coordinate of the robot.
     * @param robotNewPosY The new Y-coordinate of the robot.
     * @return A CompletableFuture<Boolean> that represents whether a collision has occurred (true) or not (false).
     */
    private CompletableFuture<Boolean> checkRobotCollision(double robotNewPosX, double robotNewPosY) {
        CompletableFuture<Boolean> futureIsColliding = new CompletableFuture<>();
        try {
            gameState.asyncGetRobots((robots) -> {
                boolean isColliding = false;
                for (Robot otherRobot : robots) {
                    if (!robot.equals(otherRobot)) {
                        double otherRobotCurrX = Math.round(otherRobot.getCurrX());
                        double otherRobotCurrY = Math.round(otherRobot.getCurrY());
                        double otherRobotNextX = Math.round(otherRobot.getNextX());
                        double otherRobotNextY = Math.round(otherRobot.getNextY());

                        boolean isAnotherRobotInNewPosition = (robotNewPosX == otherRobotCurrX && robotNewPosY == otherRobotCurrY);
                        boolean areRobotsMovingToSameNextPosition = (robotNewPosX == otherRobotNextX && robotNewPosY == otherRobotNextY);

                        if (isAnotherRobotInNewPosition || areRobotsMovingToSameNextPosition) {
                            isColliding = true;
                            break; // No need to continue checking once a collision is found
                        }
                    }
                }
                futureIsColliding.complete(isColliding);
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            futureIsColliding.complete(false);
        }
        return futureIsColliding;
    }
}
