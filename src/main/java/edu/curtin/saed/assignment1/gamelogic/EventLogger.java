package edu.curtin.saed.assignment1.gamelogic;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class EventLogger {

    private final BlockingQueue<String> logEventQueue;
    private Thread eventLoggerThread;
    private final TextArea logger;

    public EventLogger(TextArea logger) {
        this.logger = logger;
        this.logEventQueue = new LinkedBlockingQueue<>();
    }

    /**
     * Starts the Event Logger thread, which continuously listens for log messages
     * in the logEventQueue and displays them using the displayLogMessage method.
     * This method should be called to initialize and begin the event logging process.
     */
    public void start() {
        Runnable eventLoggerTask = () -> {
            while (true) {
                try {
                    String message = logEventQueue.take(); // Block until an element is available
                    displayLogMessage(message);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println(Thread.currentThread().getName() + "" + ": SHUTDOWN");
                    break;
                }

            }
        };
        eventLoggerThread = new Thread(eventLoggerTask, "event-logger-thread");
        eventLoggerThread.start();
    }

    /**
     * Stops the Event Logger thread gracefully. It interrupts the thread to signal
     * it for shutdown and then waits for the thread to complete using join().
     * This method should be called when ending the event logging process.
     */
    public void stop() {
        eventLoggerThread.interrupt();
        try {
            // Wait for the thread to complete using join()
            eventLoggerThread.join();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Enqueues a log message to be processed by the Event Logger thread. The message
     * will be added to the logEventQueue for display.
     *
     * @param message The log message to be enqueued.
     */
    public void enqueueLogMessage(String message) {
        try {
            logEventQueue.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println(e.getMessage());
        }
    }

    /**
     * Displays a log message in the user interface. This method is designed to be called
     * on the JavaFX Application Thread to ensure proper UI updates.
     *
     * @param message The log message to be displayed.
     */
    private void displayLogMessage(String message) {
        Platform.runLater(() -> logger.appendText(message + "\n"));
    }
}
