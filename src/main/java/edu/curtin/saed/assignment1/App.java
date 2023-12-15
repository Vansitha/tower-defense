package edu.curtin.saed.assignment1;

import edu.curtin.saed.assignment1.gamelogic.EventLogger;
import edu.curtin.saed.assignment1.gamelogic.GameState;
import edu.curtin.saed.assignment1.robot.Spawner;
import edu.curtin.saed.assignment1.ui.JFXArena;
import edu.curtin.saed.assignment1.wall.WallBuilder;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

public class App extends Application {

    public static void main(String[] args) {
        launch();
    }

    /**
     * Starts the Tower Defense game application by initializing and configuring the game components
     * such as the game stage, logger, event handlers, and game threads.
     *
     * @param stage The JavaFX stage on which the game will be displayed.
     */
    @Override
    public void start(Stage stage) {
        stage.setTitle("Tower Defense: The Final Stand");

        TextArea logger = new TextArea();
        ToolBar toolbar = new ToolBar();

        EventLogger eventLogger = new EventLogger(logger);
        GameState gameState = new GameState(eventLogger);
        JFXArena arena = new JFXArena(gameState);

        Spawner spawner = new Spawner(gameState);
        WallBuilder wallBuilder = new WallBuilder(gameState);

        eventLogger.start();
        gameState.start();
        spawner.start();
        wallBuilder.start();

        arena.addListener(wallBuilder::buildWall);

        EventHandler<WindowEvent> closeRequestHandler = event -> {
            System.out.println("triggered");
            gameState.stop();
            eventLogger.stop();
            spawner.stop();
            wallBuilder.stop();
            Platform.exit();
        };
        stage.setOnCloseRequest(closeRequestHandler);

        initializeGameWindow(stage, toolbar, arena, logger);
        refreshToolBar(toolbar, gameState, wallBuilder);
        refreshGameWindow(arena, gameState, spawner, wallBuilder, eventLogger);
    }

    /**
     * Initializes and configures the game window by creating a split-pane layout with the game arena
     * and event logger, setting their initial sizes, and setting up the game scene.
     *
     * @param stage   The JavaFX stage on which the game window will be displayed.
     * @param toolbar The toolbar containing game controls.
     * @param arena   The game arena where gameplay occurs.
     * @param logger  The text area for displaying game logs and events.
     */
    private void initializeGameWindow(Stage stage, ToolBar toolbar, JFXArena arena, TextArea logger) {
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(arena, logger);
        arena.setMinWidth(300.0);
        arena.setPrefSize(500, 500); // Set an initial size for the arena

        BorderPane contentPane = new BorderPane();
        contentPane.setTop(toolbar);
        contentPane.setCenter(splitPane);

        Scene scene = new Scene(contentPane, 800, 600);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Refreshes the toolbar by updating the displayed game-related information, such as the current
     * score and the total number of walls queued for construction. This method sets up a periodic
     * task to update these values.
     *
     * @param toolbar     The toolbar to be refreshed with updated game information.
     * @param gameState   The game state object that provides access to the current game score.
     * @param wallBuilder The wall builder object that provides access to the queued wall count.
     */
    private void refreshToolBar(ToolBar toolbar, GameState gameState, WallBuilder wallBuilder) {
        Label scoreLabel = new Label();
        Label queuedWalls = new Label();
        Separator separator = new Separator(Orientation.VERTICAL);
        toolbar.getItems().addAll(scoreLabel, separator, queuedWalls);

        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(100), event -> {
            int currentScore = gameState.getScore();
            scoreLabel.setText("Score: " + currentScore);

            int wallsQueued = wallBuilder.getQueuedWallCount();
            queuedWalls.setText("Total Walls Queued: " + wallsQueued);
        }));

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    /**
     * Refreshes the game window by controlling the game loop and updating the game elements.
     * This method uses an AnimationTimer to manage frame updates and checks for game over conditions.
     *
     * @param arena       The JFXArena instance representing the game arena.
     * @param gameState   The game state object that manages the game's state.
     * @param spawner     The spawner responsible for robot spawning.
     * @param wallBuilder The wall builder responsible for wall construction.
     * @param eventLogger The event logger for recording game events.
     */
    private void refreshGameWindow(JFXArena arena, GameState gameState, Spawner spawner, WallBuilder wallBuilder, EventLogger eventLogger) {

        final int targetFps = 120;
        final long frameDurationNanos = 1_000_000_000L / targetFps;
        final long[] lastUpdateTime = {0};

        AnimationTimer animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Calculate the elapsed time since the last frame
                long elapsedNanos = now - lastUpdateTime[0];
                if (elapsedNanos >= frameDurationNanos) {
                    if (gameState.getIsGameOverStatus()) {
                        showGameOverPopup(gameState, spawner, wallBuilder, eventLogger);
                        stop();
                        return;
                    }
                    Platform.runLater(arena::requestLayout);
                    lastUpdateTime[0] = now;
                }
            }
        };
        animationTimer.start();
    }

    /**
     * Displays a game-over popup dialog to inform the player about the game outcome.
     * This method stops game-related threads and presents a dialog showing the player's final score.
     *
     * @param gameState   The game state object that manages the game's state.
     * @param spawner     The spawner responsible for robot spawning.
     * @param wallBuilder The wall builder responsible for wall construction.
     * @param eventLogger The event logger for recording game events.
     */
    private void showGameOverPopup(GameState gameState, Spawner spawner, WallBuilder wallBuilder, EventLogger eventLogger) {
        // Display a game-over popup
        Platform.runLater(() -> {
            gameState.stop();
            eventLogger.stop();
            spawner.stop();
            wallBuilder.stop();

            Alert gameOverAlert = new Alert(Alert.AlertType.INFORMATION);
            gameOverAlert.setTitle("Tower Defense: The Final Stand");
            gameOverAlert.setHeaderText("You failed to protect the Citadel!");
            gameOverAlert.setContentText("Your final score: " + gameState.getScore());

            // Add a button to exit the application
            ButtonType exitButton = new ButtonType("Exit", ButtonBar.ButtonData.CANCEL_CLOSE);
            gameOverAlert.getButtonTypes().setAll(exitButton);

            // Handle the exit action
            gameOverAlert.setOnCloseRequest(event -> {
                // Stop all threads and exit the application
                Platform.exit();
            });

            gameOverAlert.showAndWait();
        });
    }
}
