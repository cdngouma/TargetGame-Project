package edu.cuny.brooklyn.cisc3120.project.game.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.cuny.brooklyn.cisc3120.project.game.TargetGameApp;
import edu.cuny.brooklyn.cisc3120.project.game.model.DecisionWrapper;
import edu.cuny.brooklyn.cisc3120.project.game.model.TargetGame;
import edu.cuny.brooklyn.cisc3120.project.game.net.StatusBroadcaster;
import edu.cuny.brooklyn.cisc3120.project.game.utils.I18n;
import edu.cuny.brooklyn.cisc3120.project.game.model.Shot;
import edu.cuny.brooklyn.cisc3120.project.game.model.DecisionWrapper.UserDecision;
import edu.cuny.brooklyn.cisc3120.project.game.model.GameStatistics.StatNameValue;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Callback;
import javafx.util.StringConverter;

public class GameController {
    private final static Logger LOGGER = LoggerFactory.getLogger(GameController.class);
    
    private final static String APP_TITLE = "CISC 3120 Fall 2017: TargetGame";
    
    @FXML
    private Canvas targetCanvas;

    @FXML
    private TextField xAimedAtTextField;

    @FXML
    private TextField yAimedAtTextField;

    @FXML
    private Button fireWeaponButton;

    @FXML
    private ComboBox<Locale> lcComboBox;
    
    @FXML
    private TableView<StatNameValue> gameStatTableView;
    
    @FXML
    private TableColumn<StatNameValue, String> tableViewStatName;
    
    @FXML
    private TableColumn<StatNameValue, String> tableViewStatValue;
    
    @FXML
    private VBox playersOnLineVbox;
    
    @FXML
    MenuItem menuItemFileSaveGame;
    
    @FXML
    MenuItem menuItemFileOpenGame;
    
    private TargetGame targetGame = new TargetGame();
    
    private Stage stage;
    
    private StatusBroadcaster statusBroadCaster;
    
    private final int NUM_BULLET_IMPACT_TYPE = 4;
    
    private AudioClip shotSound = new AudioClip(Paths.get("src\\main\\resources\\9mm_Gun_shot.wav").toUri().toString());
    
    public void setStage(Stage stage) {
        this.stage = stage;
        this.stage.setOnCloseRequest(event -> {
            LOGGER.debug("User clicked the X button on the stage.");
            exitGame(event);
        });
    }
    
    @FXML
    void initialize() throws IOException, URISyntaxException {
        LOGGER.debug("Initializing GameController.");
        setWeaponDisable(true);
        initializeI18n();
        gameStatTableView.setVisible(false);
        playersOnLineVbox.setVisible(false);
        menuItemFileSaveGame.setDisable(true);
        statusBroadCaster = new StatusBroadcaster();
        statusBroadCaster.start();
    }
    
    @FXML
    void fireTheWeapon(ActionEvent event) {
        LOGGER.debug("Weapon fired!");
        shotSound.play();
        int shotX = Integer.parseInt(xAimedAtTextField.getText());
        int shotY = Integer.parseInt(yAimedAtTextField.getText());
        
        // Forces input to fit limit
        if(shotX >= targetGame.getGameBoard().getWidth())
        	shotX = (int) (targetGame.getGameBoard().getWidth() - 1);
        if(shotX < 0)
    		shotX = 0;
        if(shotY >= targetGame.getGameBoard().getHeight())
    		shotY = (int) (targetGame.getGameBoard().getHeight() - 1);
        if(shotY < 0)
    		shotY = 0;
        
        Shot shot = new Shot(shotX, shotY);
        processShotAction(targetGame, shot);
    }
    
    @FXML
    void exitGame(ActionEvent event) {
        LOGGER.debug("calling exitGame(ActionEvent event).");
        exitGame((Event)event);
    }

    @FXML
    void newGame(ActionEvent event) {
    	startNewGame();
    }
    
    private void startNewGame() {
    	clearTargetPane();
    	targetGame.getGameStatistics().resetStatistics();
        LOGGER.debug("started new game.");
        lcComboBox.setDisable(true); // don't allow users to change locale when a game is in session
        addTarget(targetGame, targetCanvas);
        setWeaponDisable(false);
        gameStatTableView.setVisible(true);
        gameStatTableView.setItems(targetGame.getGameStatistics().toObservableList());
        tableViewStatName.setCellValueFactory(new PropertyValueFactory<StatNameValue, String>(StatNameValue.COLUMN_NAME_TITLE));
        tableViewStatValue.setCellValueFactory(new PropertyValueFactory<StatNameValue, String>(StatNameValue.COLUMN_VALUE_TITLE));
        gameStatTableView.getColumns().set(0,  tableViewStatName);
        gameStatTableView.getColumns().set(1,  tableViewStatValue);
        playersOnLineVbox.setVisible(true);
        menuItemFileSaveGame.setDisable(false);
    }

    @FXML
    void saveTheGame(ActionEvent event) {
   // 	if(targetGame.isGameStateChanged()) {		// NOTE: this is commented out for testing purpose
    		try {
    			saveNewFile();
    			targetGame.saveTheGame();
    			LOGGER.debug(String.format("Created new file %s for editing.", 
    					targetGame.getTheGameFile().getPath()));
    		}
    		catch(FileNotFoundException e) {
                LOGGER.error(String.format("Cannot found the file %s while saving the file."
                        , targetGame.getTheGameFile().getPath()), e);
                NotificationHelper.showFileNotFound(targetGame.getTheGameFile().getPath());
            } catch (IOException e) {
                LOGGER.error(String.format("Cannot write to the file %s while saving the file."
                        , targetGame.getTheGameFile().getPath()), e);
                NotificationHelper.showWritingError(targetGame.getTheGameFile().getPath());
            }
    //	}		// NOTE: this is commented out for testing purpose
    }
    
    @FXML
    void openGame(ActionEvent event) {
        try {
   //         if (targetGame.isGameStateChanged()) { 		NOTE: this is commented out for tetsing purpose
            	UserDecision decision = NotificationHelper.askUserDecision(new DecisionWrapper(UserDecision.CancelPendingAction));
                switch(decision) {
                case CancelPendingAction:
                    break;
                case DiscardGame:
                    openFileFromFileSystem();
                    LOGGER.debug(String.format("Opened file %s for editing.", targetGame.getTheGameFile().getPath()));                                       
                    startNewGame();
                    break;
                case SaveGame:
                	saveNewFile();
                    targetGame.saveTheGame();
                    LOGGER.debug(String.format("Saved the file %s.", targetGame.getTheGameFile().getPath()));                    
                    openFileFromFileSystem();
                    LOGGER.debug(String.format("Opened file %s for editing.", targetGame.getTheGameFile().getPath()));                                                           
                    startNewGame();
                    break;
                default:
                    throw new IllegalArgumentException(String.format(
                            "User decision's value (%s) is unexpected", decision));
               }
    /*          openFileFromFileSystem();	
                LOGGER.debug(String.format("Opened file %s for editing.", editor.getTheFile().getPath()));                                                                               
               	startNewGame();
                }
            }			NOTE: Commented out for testing purpose
     */
        } catch (FileNotFoundException e) {
            LOGGER.error(String.format("Cannot found the file %s while opening the file."
                    , targetGame.getTheGameFile().getPath()), e);
            NotificationHelper.showFileNotFound(targetGame.getTheGameFile().getPath());
        } catch (IOException e) {
            LOGGER.error(String.format("Cannot load the file %s while opening the file."
                    , targetGame.getTheGameFile().getPath()), e);
            NotificationHelper.showWritingError(targetGame.getTheGameFile().getPath());
        }
    }
    
    private void exitGame(Event event) {
        LOGGER.debug("calling exitGame(Event event).");
        if (targetGame.isGameStateChanged()) {
            UserDecision decision = NotificationHelper.askUserDecision(new DecisionWrapper(UserDecision.CancelPendingAction));
            switch (decision) {
            case CancelPendingAction:
                event.consume();
                break;
            case DiscardGame:
                statusBroadCaster.close();
                Platform.exit();
                break;
            case SaveGame:
                try {
                	saveNewFile();
                    targetGame.saveTheGame();
                    LOGGER.debug(String.format("Saved the game at %s.", targetGame.getTheGameFile().getPath()));
                    statusBroadCaster.close();
                    Platform.exit();
                } catch (FileNotFoundException e) {
                    LOGGER.error(String.format("Cannot found the file %s while saving the game.",
                            targetGame.getTheGameFile().getPath()), e);
                    NotificationHelper.showFileNotFound(targetGame.getTheGameFile().getPath());
                } catch (IOException e) {
                    LOGGER.error(String.format("Cannot write to the file %s while saving the game.",
                            targetGame.getTheGameFile().getPath()), e);
                    NotificationHelper.showWritingError(targetGame.getTheGameFile().getPath());
                }
                break;
            default:
                throw new IllegalArgumentException(String.format("User decision's value (%s) is unexpected", decision));
            }
        } else {
            statusBroadCaster.close();
            Platform.exit();
        }       
    }
    
    private void addTarget(TargetGame game, Canvas canvas) {
        game.setNewTarget();
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        double cellWidth = width / game.getGameBoard().getWidth();
        double cellHeight = height / game.getGameBoard().getHeight();
        double xPos = cellWidth * game.getTarget().getX();
        double yPos = cellHeight * game.getTarget().getY();
        GraphicsContext gc = targetCanvas.getGraphicsContext2D();
        gc.setFill(game.getTarget().getColor());
        gc.fillRect(xPos, yPos, cellWidth, cellHeight);
   //     gc.drawImage(new Image("img/target.png"), xPos, yPos, cellWidth, cellHeight); If we want an image as target
        game.getGameStatistics().setNumOfTargetsMade();
        game.getGameStatistics().setNumOfRoundsPlayed();
        gameStatTableView.setItems(targetGame.getGameStatistics().toObservableList());
    }
    
    private void shoot(TargetGame game, Shot shot, Canvas canvas) {
    	Random rng = new Random();
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        double cellWidth = width / game.getGameBoard().getWidth();
        double cellHeight = height / game.getGameBoard().getHeight();
        double xPos = cellWidth * shot.getX();
        double yPos = cellHeight * shot.getY();
        GraphicsContext gc = targetCanvas.getGraphicsContext2D();
        gc.drawImage(new Image("img/impact" + (rng.nextInt(NUM_BULLET_IMPACT_TYPE) + 1) + ".png"),
        									xPos, yPos, cellWidth, cellHeight);	// display different impacts
    }
    
    private void processShotAction(TargetGame gameState, Shot shot) {
    	shoot(targetGame, shot, targetCanvas);
    	
        // Updates stats
    	targetGame.getGameStatistics().decreementNumAttempts();
        targetGame.getGameStatistics().setNumOfShotsFired();
    	gameStatTableView.setItems(targetGame.getGameStatistics().toObservableList()); // Updates statistics table (view)
    	
        if (gameState.getTarget().isTargetShot(shot)) {
        	targetGame.getGameStatistics().setNumOfTargetsShot();
        	targetGame.getGameStatistics().setNumOfRoundsWon();
            Alert alert = new Alert(AlertType.INFORMATION
                    , I18n.getBundle().getString("uShotTarget"), ButtonType.CLOSE);
            alert.setTitle(APP_TITLE + ":" + I18n.getBundle().getString("targetShot"));
            alert.setHeaderText(I18n.getBundle().getString("greatShot"));
            alert.showAndWait();
            clearTarget();
            clearTargetPane();
            targetGame.getGameStatistics().updateAccuracy();
            targetGame.getGameStatistics().resetNumAttempts();
            addTarget(gameState, targetCanvas);
        }
        else if (targetGame.getGameStatistics().getNumAttempts() == 0) {
        	Alert alert = new Alert(AlertType.INFORMATION
                    , I18n.getBundle().getString("maxAttemptsExceeded"), ButtonType.CLOSE);
            alert.setTitle(APP_TITLE + ":" + I18n.getBundle().getString("uMissedTarget"));
            alert.setHeaderText(I18n.getBundle().getString("gameOver"));
            alert.showAndWait();
            clearTargetPane();
            targetGame.getGameStatistics().resetNumAttempts();
            addTarget(gameState, targetCanvas);
        }
   }
    
    private void clearTarget() {
        double width = targetCanvas.getWidth();
        double height = targetCanvas.getHeight();
        double cellWidth = width / targetGame.getGameBoard().getWidth();
        double cellHeight = height / targetGame.getGameBoard().getHeight();
        double xPos = cellWidth * targetGame.getTarget().getX();
        double yPos = cellHeight * targetGame.getTarget().getY();
        
        GraphicsContext gc = targetCanvas.getGraphicsContext2D();
        gc.clearRect(xPos, yPos, cellWidth, cellHeight);        
    }
    
    private void clearTargetPane() {
    	GraphicsContext gc = targetCanvas.getGraphicsContext2D();
    	gc.clearRect(0, 0, targetCanvas.getWidth(), targetCanvas.getHeight());
    }
    
    private void setWeaponDisable(boolean disabled) {
        xAimedAtTextField.setDisable(disabled);
        yAimedAtTextField.setDisable(disabled);
        fireWeaponButton.setDisable(disabled);
    }
    
    private void initializeI18n() throws IOException, URISyntaxException {
        List<Locale> lcList = I18n.getSupportedLocale();
        lcComboBox.getItems().addAll(lcList);
        Callback<ListView<Locale>, ListCell<Locale>> lcCellFactory = 
                new Callback<ListView<Locale>, ListCell<Locale>>() {

            @Override
            public ListCell<Locale> call(ListView<Locale> lv) {
                return new ListCell<Locale>() {
                    @Override
                    protected void updateItem(Locale lc, boolean empty) {
                        super.updateItem(lc, empty);
                        if (lc == null || empty) {
                            setText("");
                        } else {
                            setText(I18n.getDisplayLC(lc));
                        }
                    }
                };
            }
        };
        lcComboBox.setValue(I18n.getSelectedLocale());
        lcComboBox.setConverter(new StringConverter<Locale>() {

            @Override
            public Locale fromString(String arg0) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String toString(Locale lc) {
                return I18n.getDisplayLC(lc);
            }
        });
        lcComboBox.setCellFactory(lcCellFactory);
        lcComboBox.valueProperty().addListener(
                (observedLocale, oldLocale, newLocale) -> {
                    LOGGER.debug(String.format("Change locale from %s to %s.", oldLocale, newLocale));
                    try {
                        LOGGER.debug("TODO: change language results to a new game. Need to handle it better.");
                        reLoadScene(stage, newLocale);
                    } catch (IOException e) {
                        LOGGER.error("failed to load locale specific scene.", e);
                    }
        });
    }
    
    
    private void reLoadScene(Stage stage, Locale locale) throws IOException {
        I18n.setSelectedLocale(locale);
        I18n.setBundle(ResourceBundle.getBundle(I18n.getBundleBaseName(), locale));
        FXMLLoader loader = new FXMLLoader(TargetGameApp.class.getResource(TargetGameApp.FXML_MAIN_SCENE)
                , I18n.getBundle());
        Parent pane = loader.load();
        
        StackPane viewHolder = (StackPane)stage.getScene().getRoot();

        viewHolder.getChildren().clear();
        viewHolder.getChildren().add(pane);
        
        GameController controller = loader.getController();
        controller.setStage(stage);
        stage.setTitle(I18n.getBundle().getString(TargetGameApp.APP_TITLE_KEY));
        
        LOGGER.debug(targetGame.getTarget() == null? "No target set yet.":targetGame.getTarget().toString());
    }
    
    private void saveNewFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18n.getBundle().getString("saveFile"));
        fileChooser.getExtensionFilters().addAll(
                new ExtensionFilter("Text Files", "*.txt"),
                new ExtensionFilter("All Files", "*.*"));
        File theFile = fileChooser.showSaveDialog(stage);
        if (theFile != null) {
            targetGame.setTheGameFile(theFile);
        }
    }
    
    private void openFileFromFileSystem() throws FileNotFoundException, IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open an Text File");
        fileChooser.getExtensionFilters().addAll(
                new ExtensionFilter("Text Files", "*.txt"),
                new ExtensionFilter("All Files", "*.*"));
        File theFile = fileChooser.showOpenDialog(stage);
        if (theFile != null) {
            targetGame.setTheGameFile(theFile);
            targetGame.openSavedGame();
        }
    }
}
