package edu.curtin.saed.assignment1.wall;

import edu.curtin.saed.assignment1.gamelogic.GameState;
import edu.curtin.saed.assignment1.worldobjects.Robot;
import edu.curtin.saed.assignment1.worldobjects.Wall;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class WallBuilder {

    private final BlockingQueue<Wall> wallQueue;
    private Thread thread;
    private final GameState gameState;

    private final Object mutexWallQueueCount = new Object();

    public WallBuilder(GameState gameState) {
        this.gameState = gameState;
        this.wallQueue = new ArrayBlockingQueue<>(10);
    }

    /**
     * Starts the Wall Builder thread, which continuously dequeues wall building requests from the wallQueue
     * and constructs walls on the game grid. It introduces a 2000-millisecond delay between consecutive wall constructions.
     */
    public void start() {
        Runnable wallBuilderTask = () -> {
            while (true) {
                try {
                    Wall wall = wallQueue.take();
                    buildWall(wall);
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    System.out.println(Thread.currentThread().getName() + "" + ": SHUTDOWN");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        };
        thread = new Thread(wallBuilderTask, "wall-builder-thread");
        thread.start();
    }

    /**
     * Stops the Wall Builder thread gracefully. It interrupts the thread and waits for it to complete using join().
     */
    public void stop() {
        // Set a flag to signal the thread to stop gracefully
        thread.interrupt();

        try {
            // Wait for the thread to complete using join()
            thread.join();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Attempts to build a wall at the specified grid coordinates (x, y) if the wall queue has remaining capacity and the
     * maximum number of walls (10) on the grid has not been reached. It checks for existing walls at the specified
     * coordinates to avoid duplicate construction.
     *
     * @param x The X-coordinate where the wall should be built.
     * @param y The Y-coordinate where the wall should be built.
     */
    public void buildWall(double x, double y) {
        try {
            if (wallQueue.remainingCapacity() != 0 && gameState.getTotalWallsBuilt() != 10) {
                gameState.asyncGetWalls(walls -> {
                    boolean build = true;
                    for (Wall wall : walls) {
                        if (wall.getPositionX() == x && wall.getPositionY() == y) {
                            build = false;
                            break;
                        }
                    }
                    if (build) {
                        Wall wall = new Wall(x, y);
                        try {
                            wallQueue.put(wall);
                        } catch (InterruptedException e) {
                            System.err.println(e.getMessage());
                        }
                    }
                });
            }
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Attempts to build a wall at the specified grid coordinates represented by the provided Wall object. It checks for
     * the presence of robots at the same coordinates to prevent wall placement where a robot is present.
     *
     * @param wall The Wall object representing the location where the wall should be built.
     */
    private void buildWall(Wall wall) {
        try {
            gameState.asyncGetRobots((robots -> {
                boolean placeWall = true;
                for (Robot robot : robots) {
                    if (Math.round(robot.getCurrX()) == wall.getPositionX() && Math.round(robot.getCurrY()) == wall.getPositionY()) {
                        placeWall = false;
                        break;
                    }
                }
                if (placeWall) {
                    try {
                        gameState.addWalls(wall);
                    } catch (InterruptedException e) {
                        System.err.println(e.getMessage());
                    }
                }
            }));
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Retrieves the current count of walls waiting to be built in the wall queue.
     *
     * @return The count of walls in the wall queue.
     */
    public int getQueuedWallCount() {
        synchronized (mutexWallQueueCount) {
            return wallQueue.size();
        }
    }
}
