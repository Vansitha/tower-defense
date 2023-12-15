package edu.curtin.saed.assignment1.robot;

import edu.curtin.saed.assignment1.gamelogic.GameState;
import edu.curtin.saed.assignment1.worldobjects.Robot;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Spawner {
    private final GameState gameState;
    private static int robotId = 0;

    private final ScheduledExecutorService executorService;
    private Thread thread;


    public Spawner(GameState gameState) {
        this.gameState = gameState;
        int coreCount = Runtime.getRuntime().availableProcessors();
        this.executorService = Executors.newScheduledThreadPool(coreCount);
    }

    /**
     * Starts the Spawner thread, responsible for periodically spawning robots at one of the four corners of the grid
     * every 1500 milliseconds, provided that the corner is unoccupied.
     */
    public void start() {
        Runnable spawnerTask = () -> {
            while (true) {
                try {
                    spawnRobot();
                    Thread.sleep(1500); // Sleep for 1500 milliseconds
                } catch (InterruptedException e) {
                    System.out.println(Thread.currentThread().getName() + "" + ": SHUTDOWN");
                    break;
                }
            }
        };
        thread = new Thread(spawnerTask, "spawner-thread");
        thread.start();

    }

    /**
     * Stops the Spawner thread gracefully by interrupting it and waiting for it to complete. Additionally, it stops
     * the associated thread pool used for moving robots.
     */
    public void stop() {
        // Set a flag to signal the thread to stop gracefully
        thread.interrupt();
        stopThreadPool();

        try {
            // Wait for the thread to complete using join()
            thread.join();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Stops the associated thread pool by initiating an orderly shutdown and logs a message indicating the shutdown.
     */
    private void stopThreadPool() {
        executorService.shutdown();
        System.out.println("Thread Pool: SHUTDOWN");
    }

    /**
     * Spawns a new robot in one of the four corners of the grid. The robot's initial position is determined randomly
     * among the corners. Each spawned robot is assigned a unique ID and a random movement delay. The robot is added to
     * the game state, and its movement is scheduled using the provided executor service.
     *
     * @throws InterruptedException if the operation is interrupted while adding the robot or scheduling its movement.
     */
    private void spawnRobot() throws InterruptedException {
        Random random = new Random();
        int corner = random.nextInt(4); // Randomly select one of the four corners

        double x, y;
        switch (corner) {
            // Top-left corner
            // Top-right corner
            case 1 -> {
                x = 8;
                y = 0;
            }
            // Bottom-left corner
            case 2 -> {
                x = 0;
                y = 8;
            }
            // Bottom-right corner
            case 3 -> {
                x = 8;
                y = 8;
            }
            default -> {
                x = 0;
                y = 0;
            }
        }


        robotId += 1;
        int delay = chooseDelay();

        // FOR EXECUTOR SERVER
        int initialDelay = new Random().nextInt(1500);
        int period = delay;

        Robot newRobot = new Robot(robotId, delay, x, y);

        try {
            gameState.asyncGetRobots((robots) -> {
                boolean spawn = true;

                for (Robot robot : robots) {
                    // A bot is already in this location, so don't spawn and wait
                    if (Math.round(robot.getCurrX()) == Math.round(newRobot.getCurrX()) && Math.round(robot.getCurrY()) == Math.round(newRobot.getCurrY())) {
                        spawn = false;
                        break;
                    }
                }

                if (spawn) {
                    try {
                        gameState.addRobot(newRobot);
                        executorService.scheduleAtFixedRate(new Movement(newRobot, gameState), initialDelay, period, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        System.err.println(e.getMessage());
                    }
                } else {
                    robotId -= 1;
                }
            });
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Chooses a random delay value for a newly spawned robot's movement. The delay value determines how frequently
     * the robot will move. It generates a random number between 500 and 2000 milliseconds (inclusive).
     *
     * @return the randomly chosen delay value in milliseconds.
     */
    private int chooseDelay() {
        Random random = new Random();
        // Generate a random number between 500 and 2000 (inclusive)
        return random.nextInt(1501) + 500; // 1501 to include 2000, and then add 500 to start from 500
    }
}