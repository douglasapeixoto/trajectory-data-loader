package traminer.loader.gui;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import traminer.io.log.LogWriter;

/**
 * Starts the GUI and binds the GUI Window with 
 * the FXML scene (JavaFX). GUI start point class.
 * 
 * @author uqdalves
 */
public class DataLoaderGUI extends Application {

	@Override
	public void start(Stage stage) {		
		try {
			Parent root = FXMLLoader.load(getClass().getResource("DataLoaderScene.fxml"));
			
			Scene mainScence  = new Scene(root);

	        stage.setTitle("TraMiner Data Loader");
	        stage.setScene(mainScence);
	        stage.setHeight(750.0);
	        stage.setWidth(830.0);
	        stage.setResizable(false);
	        stage.show();
		} catch (IOException e) {
			System.err.println("Error starting data loader GUI.");
			e.printStackTrace();
		}
	}

	/**
	 * Launch GUI.
	 * @param args
	 */
	public static void main(String[] args) {
		// Set up the log4j configuration
		LogWriter.startLog();
		// launch app GUI
		launch(args);
	}
}
