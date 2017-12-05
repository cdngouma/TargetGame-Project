package edu.cuny.brooklyn.cisc3120.project.game.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TargetGame {
    private static Logger LOGGER = LoggerFactory.getLogger(TargetGame.class);
    
    private final static int GAME_TARGET_AREA_WIDTH = 15;
    private final static int GAME_TARGET_AREA_HEIGHT = 15;
    private final static char TARGET_INDICATOR_ON_BOARD = 'X';
    
    private boolean gameStateChanged;
    
    private File theGameFile;
    private Target target;
    private GameBoard gameBoard;
    private Random rng;
    private Scanner in;
    
    private GameStatistics gameStatistics;

    public TargetGame() {
        gameStateChanged = false;
        gameBoard = new GameBoard(GAME_TARGET_AREA_HEIGHT, GAME_TARGET_AREA_WIDTH);
        rng = new Random();
        target = null;
        gameStatistics = new GameStatistics();
    }
    
    public boolean isGameStateChanged() {
        return gameStateChanged;
    }

    public void setGameStateChanged(boolean gameStateChanged) {
        this.gameStateChanged = gameStateChanged;
    }

    public void saveTheGame() throws FileNotFoundException, IOException {
    	try (PrintWriter printWriter = new PrintWriter(theGameFile)) {
            printWriter.format("%d,%d,%d,%d,%d",
            		gameStatistics.getNumOfTargetsMade(),
            		gameStatistics.getNumOfShotsFired(),
            		gameStatistics.getNumOfTargetsShot(),
            		gameStatistics.getNumOfRoundsPlayed(),
            		gameStatistics.getNumOfRoundsWon());
         }       
    }
    
    public void openSavedGame() {
    	try {
        	in = new Scanner(theGameFile);
        	in.useDelimiter("(\\p{javaWhitespace}|,)+");
        	
        	while(in.hasNextLine()) {
        		gameStatistics.setNumOfTargetsMade(Integer.parseInt(in.next()));
        		gameStatistics.setNumOfShotsFired(Integer.parseInt(in.next()));
        		gameStatistics.setNumOfTargetsShot(Integer.parseInt(in.next()));
        		gameStatistics.setNumOfRoundsPlayed(Integer.parseInt(in.next()));
        		gameStatistics.setNumOfRoundsWon(Integer.parseInt(in.next()));
        		gameStatistics.updateAccuracy();
        	}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
    public void setTheGameFile(File theGameFile) {
    	this.theGameFile = theGameFile;
    }
    
    public File getTheGameFile() {
        return theGameFile;
    }

    public GameBoard getGameBoard() {
        return gameBoard;
    }

    public Target getTarget() {
        return target;
    }
    
    public void setNewTarget() {
        target = getRandomTarget();
        addTargetToBoard(target);
    }

    private Target getRandomTarget() {
        int x = rng.nextInt(GAME_TARGET_AREA_WIDTH);
        int y = rng.nextInt(GAME_TARGET_AREA_HEIGHT);
   //     int x = 2;
   //     int y = 2; // For testing purpose
        Target target = new Target(x, y);
        LOGGER.debug("Target: " + x + "," + y);
        return target;
    }

    private void addTargetToBoard(Target target) {
        gameBoard.setCell(target.getX(), target.getY(), TARGET_INDICATOR_ON_BOARD);
    }

    public GameStatistics getGameStatistics() {
        return gameStatistics;
    }    
}
