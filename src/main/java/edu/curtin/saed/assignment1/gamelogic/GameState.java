package edu.curtin.saed.assignment1.gamelogic;

import edu.curtin.saed.assignment1.worldobjects.Citadel;
import edu.curtin.saed.assignment1.worldobjects.Robot;
import edu.curtin.saed.assignment1.worldobjects.Wall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class GameState {

    private final Citadel citadel;
    private final List<Robot> robots;
    private final BlockingQueue<Runnable> robotQueue;
    private final List<Wall> walls;
    private final BlockingQueue<Runnable> wallQueue;
    private int score;
    private Thread thread;
    private final ScheduledExecutorService scoreUpdater;
    private final EventLogger eventLogger;
    private volatile boolean running = true;
    private final Runnable poison = () -> {
    };
    private final Object mutexScore = new Object();
    private final Object mutexWallQueue = new Object();
    private final Object mutexGameOver = new Object();
    private boolean isGameOver;

    public GameState(EventLogger eventLogger) {
        this.eventLogger = eventLogger;
        this.thread = null;
        double citadelX = (9 - 1) / 2.0; // 9 - 1 = grid size
        double citadelY = (9 - 1) / 2.0; // 9 - 1 = grid size
        this.citadel = new Citadel(citadelX, citadelY);
        this.isGameOver = false;
        this.robots = new ArrayList<>();
        this.robotQueue = new ArrayBlockingQueue<>(30);
        this.walls = new ArrayList<>();
        this.wallQueue = new ArrayBlockingQueue<>(30);
        this.score = 0;
        this.scoreUpdater = Executors.newScheduledThreadPool(1);
        startScoreUpdater();
    }

    /**
     * Starts the GameState thread, which manages the game's state by processing tasks
     * related to robots and walls as long as the 'running' flag is true.
     * This method should be called to initialize and begin the game state processing.
     */
    public void start() {
        Runnable task = () -> {
            while (running) {
                try {
                    if (!robotQueue.isEmpty()) {
                        robotQueue.take().run();
                    }
                    if (!wallQueue.isEmpty()) {
                        wallQueue.take().run();
                    }

                } catch (InterruptedException e) {
                    System.out.println(Thread.currentThread().getName() + "" + ": SHUTDOWN");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        };

        this.thread = new Thread(task, "game-state-thread");
        thread.start();
    }

    /**
     * Stops the GameState thread gracefully. It sets the 'running' flag to false to signal
     * the thread for shutdown, then interrupts the thread to initiate the shutdown process.
     * It also enqueues poison pills in the robotQueue and wallQueue to ensure thread termination.
     * Finally, it waits for the thread to complete using join() and shuts down the score updater
     * thread pool.
     * This method should be called when ending the game state processing.
     */
    public void stop() {
        running = false;
        thread.interrupt(); // Signal the thread to stop gracefully
        try {
            robotQueue.put(poison);
            wallQueue.put(poison);
            // Wait for the thread to complete using join()
            thread.join();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
        // Also shut down the score updater thread pool
        scoreUpdater.shutdown();
    }

    /**
     * Gets the X position of the citadel on the grid.
     *
     * @return The X position of the citadel.
     */
    public double getCitadelPositionX() {
        return this.citadel.getPositionX();
    }

    /**
     * Gets the Y position of the citadel on the grid.
     *
     * @return The Y position of the citadel.
     */
    public double getCitadelPositionY() {
        return this.citadel.getPositionY();
    }

    /**
     * Sets the game over status to indicate that the game has ended.
     */
    public void setGameOverStatus() {
        synchronized (mutexGameOver) {
            this.isGameOver = true;
        }
    }

    /**
     * Retrieves the game over status to check if the game has ended.
     *
     * @return True if the game is over, false otherwise.
     */
    public boolean getIsGameOverStatus() {
        synchronized (mutexGameOver) {
            return this.isGameOver;
        }
    }

    /**
     * Adds a robot to the game. This method enqueues the task for adding the robot.
     *
     * @param robot The robot to be added to the game.
     * @throws InterruptedException If the operation is interrupted.
     */
    public void addRobot(Robot robot) throws InterruptedException {
        robotQueue.put(() -> {
            String logMessage = String.format("Robot spawned at (%d,%d) with ID %d", Math.round(robot.getCurrX()), Math.round(robot.getCurrY()), robot.getId());
            eventLogger.enqueueLogMessage(logMessage);
            robots.add(robot);
        });
    }

    /**
     * Asynchronously retrieves the list of robots and provides it to the specified RobotCallback.
     * The provided list is unmodifiable to ensure data integrity.
     *
     * @param robotCallback The callback interface to provide the list of robots to.
     * @throws InterruptedException If the operation is interrupted.
     */
    public void asyncGetRobots(RobotCallback robotCallback) throws InterruptedException {
        robotQueue.put(() -> robotCallback.provide(Collections.unmodifiableList(robots)));
    }

    /**
     * Deletes a robot from the game. The robot is destroyed, and its details are logged.
     * This method enqueues the task for deleting the robot and updating the score.
     *
     * @param robot The robot to be deleted from the game.
     * @throws InterruptedException If the operation is interrupted.
     */
    public void deleteRobot(Robot robot) throws InterruptedException {
        robotQueue.put(() -> {
            if (robot != null) {
                String logMessage = String.format("Robot with %d destroyed at (%d,%d)", robot.getId(), Math.round(robot.getCurrX()), Math.round(robot.getCurrY()));
                eventLogger.enqueueLogMessage(logMessage);
                robots.remove(robot);
                this.score += 100; // Add 100 points to the score each time a robot is destroyed
            }
        });
    }

    /**
     * Adds a wall to the game grid if the maximum limit of 10 walls has not been reached yet.
     * The wall's details are logged. This method enqueues the task for adding the wall.
     *
     * @param wall The wall to be added to the game grid.
     * @throws InterruptedException If the operation is interrupted.
     */
    public void addWalls(Wall wall) throws InterruptedException {
        // Max walls that can be on the grid at a given point in time is 10
        if (walls.size() != 10) {
            wallQueue.put(() -> {
                String logMessage = String.format("Player built a wall at (%d,%d)", Math.round(wall.getPositionX()), Math.round(wall.getPositionY()));
                eventLogger.enqueueLogMessage(logMessage);
                walls.add(wall);
            });
        }
    }

    /**
     * Asynchronously retrieves the list of walls and provides it to the specified WallCallback.
     * The provided list is unmodifiable to ensure data integrity.
     *
     * @param wallCallback The callback interface to provide the list of walls to.
     * @throws InterruptedException If the operation is interrupted.
     */
    public void asyncGetWalls(WallCallback wallCallback) throws InterruptedException {
        wallQueue.put(() -> wallCallback.provide(Collections.unmodifiableList(walls)));
    }

    /**
     * Updates the status of a wall in the game grid. The specified wall is marked as damaged,
     * and its details are logged. This method enqueues the task for updating the wall.
     *
     * @param wallToUpdate The wall to be updated.
     * @throws InterruptedException If the operation is interrupted.
     */
    public void updateWall(Wall wallToUpdate) throws InterruptedException {
        wallQueue.put(() -> {
            for (Wall wall : walls) {
                boolean isWallToUpdate = wallToUpdate.getPositionX() == wall.getPositionX() && wallToUpdate.getPositionY() == wall.getPositionY();
                if (isWallToUpdate) {
                    String logMessage = String.format("Wall at (%d,%d) damaged", Math.round(wall.getPositionX()), Math.round(wall.getPositionY()));
                    eventLogger.enqueueLogMessage(logMessage);
                    wall.setDamaged();
                    break;
                }
            }
        });
    }

    /**
     * Deletes a wall from the game grid. This method enqueues the task for deleting the wall.
     *
     * @param wall The wall to be deleted.
     * @throws InterruptedException If the operation is interrupted.
     */
    public void deleteWall(Wall wall) throws InterruptedException {
        wallQueue.put(() -> {
            String logMessage = String.format("Wall at (%d,%d) destroyed", Math.round(wall.getPositionX()), Math.round(wall.getPositionY()));
            eventLogger.enqueueLogMessage(logMessage);
            walls.remove(wall);
        });
    }

    /**
     * Starts a scheduled task to update the game score at a fixed rate.
     */
    private void startScoreUpdater() {
        scoreUpdater.scheduleAtFixedRate(() -> {
            // Increment the score by 10 every second
            score += 10;
        }, 1, 1, TimeUnit.SECONDS); // Run every 1 second
    }

    /**
     * Gets the total number of walls built in the game.
     *
     * @return The total number of walls built.
     */
    public int getTotalWallsBuilt() {
        synchronized (mutexWallQueue) {
            return this.walls.size();
        }
    }

    /**
     * Gets the current score of the game.
     *
     * @return The current score.
     */
    public int getScore() {
        synchronized (mutexScore) {
            return this.score;
        }
    }
}
