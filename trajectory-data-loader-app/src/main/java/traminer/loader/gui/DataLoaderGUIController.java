package traminer.loader.gui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import traminer.io.IOService;
import traminer.io.db.MongoDBService;
import traminer.io.log.LogWriter;
import traminer.io.params.HDFSParameters;
import traminer.io.params.LocalFSParameters;
import traminer.io.params.MongoDBParameters;
import traminer.io.params.VoltDBParameters;
import traminer.loader.map.OsmHandlerFactory;
import traminer.loader.map.OsmHandler;
import traminer.parser.ParserInterface;
import traminer.parser.TrajectoryParser;
import traminer.parser.analyzer.Keywords.OutputDatabase;
import traminer.parser.analyzer.Keywords.OutputFormat;
 
/**
 * GUI controller class (JavaFX). Handle the events and components 
 * of the GUI {@link DataLoaderScene.fxml}. Binds the GUI components 
 * with the Java code.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class DataLoaderGUIController implements Initializable, ParserInterface {
	// root panel
	@FXML
	private Pane rootPane;
	// loader tabs
	@FXML
	private Tab trajLoaderTab;
	@FXML
	private Tab mapLoaderTab;
	// Open input trajectory data
	@FXML
    private Button openDataBtn;
	@FXML
	private TextField inputDataPathTxt;
	// Open input map data
	@FXML
    private Button openMapBtn;
	@FXML
	private TextField inputMapPathTxt;
	// Open/Edit input data format
	@FXML
    private Button openInputFormatBtn;
	@FXML
    private Button saveInputFormatBtn;
	@FXML
    private Button loadInputFormatBtn;
	@FXML
	private TextArea inputDataFormatTxt;
	// Output format
	@FXML
	private RadioButton allBtn;
	@FXML
	private RadioButton spatialBtn;
	@FXML
	private RadioButton spatialTemporalBtn;
	// Output database
	@FXML
	private ChoiceBox<OutputDatabase> outputDatabaseChoice; 
	@FXML
	private Tab localTab;
	@FXML
	private Tab mongodbTab;
	@FXML
	private Tab hbaseTab;
	@FXML
	private Tab voltdbTab;
	@FXML
	private TabPane dbConfPane;
	// Open Local output database
	@FXML
    private Button openOuputFolderBtn;
	@FXML
	private TextField outputDataPathTxt;
	// Configure MongoDB output data
	@FXML
	private TextField mongodbHostTxt;
	@FXML
	private TextField mongodbPortTxt;
	@FXML
	private TextField mongodbDatabaseTxt;
	@FXML
	private Button mongodbDefaultBtn;
	@FXML
	private Button mongodbListBtn;
	@FXML
	private Button mongodbStartBtn;
	// Load and parse (main action buttons)
	@FXML
    private Button loadParseTrajBtn;
	@FXML
    private Button loadParseMapBtn;
	// Help buttons
	@FXML
    private Button trajHelpBtn;
	@FXML
    private Button mapHelpBtn;
	// Log field
	@FXML
	private VBox logTxt;        
	@FXML
	private ScrollPane logScrollPane = new ScrollPane();
	
	// fields to read  from the GUI
	private String inputDataPath = "";
	private String inputMapPath = "";
	private String dataFormatContent = "";
	private OutputFormat outputFormat = DEFAULT_OUT_FORMAT;                                                               
	private OutputDatabase outputDatabase = DEFAULT_OUT_DB;
	
	
	@Override
	public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
		// initialize the logic here: all @FXML variables will have been injected
		handleLog();
		handleOpenData();
		handleOpenMapData();
		handleOpenInputFormat();
		handleSaveAndLoadInputFormat();
		handleOutputFormatSelection();
		handleDatabaseChoice();
		handleMongoDBConfiguration();
		handleOpenOutputLocalDir();
		handleHelp();
		// main processes, run in a different thread 
		handleLoadAndParseTrajectoryData();
		handleLoadAndParseMapData();
	}
		
	/**
	 * Add log welcome message.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void handleLog() {
		// auto-scroll down the log panel  
	    logTxt.heightProperty().addListener(new ChangeListener() {
			@Override
			public void changed(ObservableValue observable, Object oldValue, Object newValue) {
				logScrollPane.setVvalue((double)newValue);
			}
		});
	    
		// log welcome message
		addLogInfo("Enter input data specifications, "
				+ "then click in 'Load and Parse' to run!");
	}
	
	/**
	 * Open directory containing the input trajectory data.
	 */
	private void handleOpenData() {
		openDataBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
            	final DirectoryChooser dirChooser = new DirectoryChooser();
            	dirChooser.setTitle("Open Input Data Folder");
                final File selectedDir = dirChooser.showDialog(
                		rootPane.getScene().getWindow());
                if (selectedDir != null) {
                	String dataPath = selectedDir.getAbsolutePath();
                	inputDataPathTxt.setText(dataPath);
                	inputDataPathTxt.home();
                	
                	addLogInfo("Input data path set as '" + dataPath + "'");
                }
            }
        });
	}

	/**
	 * Open file containing the input map data.
	 */
	private void handleOpenMapData() {
		openMapBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
            public void handle(ActionEvent event) {
            	final FileChooser fileChooser = new FileChooser();
            	fileChooser.setTitle("Open Input OSM Map File");
            	final File selectedFile = fileChooser.showOpenDialog(
                		rootPane.getScene().getWindow());
                if (selectedFile != null) {
                	String mapDataPath = selectedFile.getAbsolutePath();
                	inputMapPathTxt.setText(mapDataPath);
                	
                	addLogInfo("Input map file '" + mapDataPath + "' loaded");
                }
            }
        });
	}

	/**
	 * Open the file containing the input data format.
	 */
	private void handleOpenInputFormat() {
		openInputFormatBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
            public void handle(ActionEvent event) {
            	final FileChooser fileChooser = new FileChooser();
            	fileChooser.setTitle("Open Input Data Format");
            	final File selectedFile = fileChooser.showOpenDialog(
                		rootPane.getScene().getWindow());
                if (selectedFile != null) {
                	String dataFormatPath = selectedFile.getAbsolutePath();
                	String dataFormatName = selectedFile.getName();
                	// read data format file content
            		List<String> fileLines = IOService.readFile(selectedFile);
            		inputDataFormatTxt.setText("# Format file name '" + dataFormatName + "'");
                	for (String line : fileLines) {
                		inputDataFormatTxt.appendText("\n" + line);
                	}
                	dataFormatContent = inputDataFormatTxt.getText();
                	inputDataFormatTxt.home();
                	
                	addLogInfo("Input data format file '" + dataFormatPath + "' loaded");
                	addLogInfo("Input data format content saved");
                }
            }
        });
	}

	/**
	 * Save and load the input data format content.
	 */
	private void handleSaveAndLoadInputFormat() {
		// save the input data format content
		saveInputFormatBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (!inputDataFormatTxt.getText().isEmpty()) {
                	dataFormatContent = inputDataFormatTxt.getText();
                	
                	addLogInfo("Input data format content saved");
                } else{
                	addLogInfo("No input data format content to be saved");
                }
            }
        });
		
		// load a previously saved input data format content
		loadInputFormatBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (!dataFormatContent.isEmpty()) {
                	inputDataFormatTxt.clear();
                	inputDataFormatTxt.setText(dataFormatContent);
                	inputDataFormatTxt.home();
                	
                	addLogInfo("Input data format content loaded.");
                } else{
                	addLogInfo("No input data format content to be loaded.");
                }
            }
        });
	}

	/**
	 * Handle the actions of the radio buttons selection.
	 */
	private void handleOutputFormatSelection(){
		// output data format
		allBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				outputFormat = OutputFormat.ALL;
				addLogInfo("Output data format set as '" + 
						outputFormat.name() + "' attributes.");
			}
		});
		spatialBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				outputFormat = OutputFormat.SPATIAL;
				addLogInfo("Output data format set as '" + 
						outputFormat.name() + "' attributes only.");
			}
		});
		spatialTemporalBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				outputFormat = OutputFormat.SPATIAL_TEMPORAL;
				addLogInfo("Output data format set as '" + 
						outputFormat.name() + "' attributes only.");
			}
		});
	}
	
	/**
	 * Handle the actions of the output Database selection.
	 */
	private void handleDatabaseChoice() {
		ObservableList<OutputDatabase> databaseList = 
				FXCollections.observableArrayList(OutputDatabase.values());
		outputDatabaseChoice.setItems(databaseList);
		outputDatabaseChoice.setValue(outputDatabase);
				
		outputDatabaseChoice.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				outputDatabase = outputDatabaseChoice.getValue();
				
				localTab.setDisable(true);
				mongodbTab.setDisable(true);
				hbaseTab.setDisable(true);
				voltdbTab.setDisable(true);
				if (outputDatabase.equals(OutputDatabase.LOCAL)) {
					localTab.setDisable(false);
					dbConfPane.getSelectionModel().select(localTab);
				} else
				if (outputDatabase.equals(OutputDatabase.MONGODB)) {
					mongodbTab.setDisable(false);
					dbConfPane.getSelectionModel().select(mongodbTab);
				} else
				if (outputDatabase.equals(OutputDatabase.HBASE)) {
					hbaseTab.setDisable(false);
					dbConfPane.getSelectionModel().select(hbaseTab);
				} else
				if (outputDatabase.equals(OutputDatabase.VOLTDB)) {
					voltdbTab.setDisable(false);
					dbConfPane.getSelectionModel().select(voltdbTab);
				}
				
				addLogInfo("Output database set as '" + outputDatabase.name() + "'.");
			}
		});

	}

	/**
	 * Handle the MongoDB input configurations and actions (MongoDB tab).
	 */
	private void handleMongoDBConfiguration() {
	    // set default MongoDB parameters (gui start)
	    mongodbHostTxt.setText(MongoDBParameters.getDefaultHostName());
		mongodbPortTxt.setText(""+MongoDBParameters.getDefaultPort());
		mongodbDatabaseTxt.setText(DEFAULT_DB_NAME);
	    
		// set default MongoDB parameters (button)
		mongodbDefaultBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				mongodbHostTxt.setText(MongoDBParameters.getDefaultHostName());
				mongodbPortTxt.setText(""+MongoDBParameters.getDefaultPort());
				mongodbDatabaseTxt.setText(DEFAULT_DB_NAME);
			}
		});	
		
		// list the current databases in MongoDB
		mongodbListBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				String mongoHost = mongodbHostTxt.getText();
				String mongoPort = mongodbPortTxt.getText();

				MongoDBService mongodb = null;
				try {
					mongodb = new MongoDBService(mongoHost, Integer.parseInt(mongoPort));
					List<String> dbNamesList = mongodb.getDatabaseNames();
					
					addLogInfo("List of databases in MongoDB: "
							+ "'mongodb://" + mongoHost + ":" + mongoPort + "'");
					for (String db : dbNamesList) {
						addLogInfo(">> '" + db + "'");
					}
				} catch (IOException e) {
					addLogError(e.getMessage() + " Check if MongoDB is started, "
							+ "or if the access configurations are correct.");
				}
			}
		});
		
		// force the MongoDB port field to be numeric only
	    mongodbPortTxt.textProperty().addListener(new ChangeListener<String>() {
	        @Override
	        public void changed(ObservableValue<? extends String> observable, 
	        		String oldValue, String newValue) {
	            if (!newValue.matches("\\d*")) {
	            	mongodbPortTxt.setText(newValue.replaceAll("[^\\d]", ""));
	            }
	        }
	    });
	    
	    // try to start MongoDB service on the host machine
	    mongodbStartBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if(MongoDBService.startMongoDB()){
					addLogInfo("MongoDB process started! Server should be up..");
				} else {
					addLogError("Could not start MongoDB process. Please make sure 'MONGO_HOME' "
							+ "is correctly set in your system environment variables.");
				}
			}
		});
	}
	
	/**
	 * Choose location to output data to a local directory.
	 */
	private void handleOpenOutputLocalDir() {
		openOuputFolderBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
            	final DirectoryChooser dirChooser = new DirectoryChooser();
            	dirChooser.setTitle("Open Output Data Folder");
                final File selectedDir = dirChooser.showDialog(
                		rootPane.getScene().getWindow());
                if (selectedDir != null) {
                	String dataPath = selectedDir.getAbsolutePath();
                	outputDataPathTxt.setText(dataPath);
                	outputDataPathTxt.home();
                	
                	addLogInfo("Local output data path set as '" + dataPath + "'");
                }
            }
        });
	}
	
	/**
	 * Open the Map help HTML page. 
	 */
	private void handleHelp() {
		// show the trajectory loader help window
		trajHelpBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				showHtmlPage("trajectory-loader-help-index.html");
			}
		});
		
		// show the map loader help window
		mapHelpBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				showHtmlPage("map-loader-help-index.html");
			}
		});
	}

	/**
	 * Handle the action of the main button in the trajectory parsing tab.
	 * <p>
	 * Run trajectory data loading and parsing on the user-provided
	 * configurations. Run this process in a different thread, to avoid
	 * the user interface to freeze while parsing the data.
	 * 
	 * @return A runnable thread process.
	 */
	private void handleLoadAndParseTrajectoryData() {
		loadParseTrajBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				// check if mandatory fields were provided
				if (!validateDataFields()) return;

				inputDataPath = inputDataPathTxt.getText();

				addLogInfo("Running TRAJECTORY data loader and parser...");
				addLogInfo("Running with configurations:");
				addLogInfo(">> Input data directory: '" + inputDataPath);
				addLogInfo(">> Output data format: '" + outputFormat.name() + "'");
				addLogInfo(">> Output database: '" + outputDatabase.name() + "'");
	
				// Call the process to parse the trajectory data
				callTrajectoryParser();
			}
		});
	}
	
	/**
	 * Call the process/model to parse the trajectory data,
	 * run process in a separated thread, to avoid the GUI
	 * to freeze.
	 */
	private void callTrajectoryParser(){
		// TODO		
		// add a method to count files in a folder in the IOService.
		// showProgressBar();	
		
		// creates a new runnable thread for this process
		Callable<Boolean> process = null;
		boolean parseResult = false;
		try {	
			// start a new parser
			TrajectoryParser parser = new TrajectoryParser(
					inputDataPath, dataFormatContent);
			
			if (outputDatabase.equals(OutputDatabase.LOCAL)) {
				String outputDataPath = outputDataPathTxt.getText();
				addLogInfo(">> Output data directory: '" + outputDataPath + "'");
				
				// parse data and save to local directory
				// run process in a separated thread
				process = new Callable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						// return the parse result
						return parser.parseToLocal(outputFormat, 
								new LocalFSParameters(outputDataPath));
					}};
			} else
			if (outputDatabase.equals(OutputDatabase.MONGODB)) {
				String mongoHost = mongodbHostTxt.getText();
				String mongoPort = mongodbPortTxt.getText();
				String mongoDbName = mongodbDatabaseTxt.getText();
				
				addLogInfo(">> MongoDB host name: '" + mongoHost + "'");
				addLogInfo(">> MongoDB port number: '" + mongoPort + "'");
				addLogInfo(">> MongoDB database name: '" + mongoDbName + "'");
				
				// parse data and save to MongoDB
				// run process in a separated thread
				process = new Callable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						// return the parse result
						return parser.parseToMongoDB(outputFormat, new MongoDBParameters(mongoHost, 
										Integer.parseInt(mongoPort), mongoDbName));
										
					}};
			} else
			if (outputDatabase.equals(OutputDatabase.HBASE)) {
				addLogError("Sorry, 'HBase' storage is not provided yet!");
				//TODO
				
				// parse data and save to HBase
				parseResult = parser.parseToHBase(outputFormat, 
						new HDFSParameters());
			} else
			if (outputDatabase.equals(OutputDatabase.VOLTDB)) {
				addLogError("Sorry, 'VoltDB' storage is not provided yet!");
				//TODO
	
				// parse data and save to HBase
				parseResult = parser.parseToVoltDB(outputFormat, 
						new VoltDBParameters());
			}
			
			// start a new thread to run the process
			FutureTask<Boolean> futureTask = new FutureTask<>(process);
			new Thread(futureTask).start();
			// await async process finish
			parseResult = futureTask.get();
		} catch (InterruptedException e) {
			addLogError("Parsing Error!\n");
			showErrorPopup("Parsing Error!");
			e.printStackTrace();
		} catch (ExecutionException e) {
			addLogError("Parsing Error!\n");
			showErrorPopup("Parsing Error!");
			e.printStackTrace();
		} finally {
			// print parser messages (if any) to log window
			addLogMsg(LogWriter.getLogWriterAsList());
			LogWriter.clearLog();
			
			// show parser result
			if (parseResult) {
				addLogInfo("Trajectory Data Parsing Successful!");
				// show confirmation popup
				showInfoPopup(null,"Trajectory Data Parsing Successful!");
				// Open a Windows with the output results
				showResultWindow();
			} else {
				addLogError("Parsing Error!\n");
				// show error popup
				showErrorPopup("Parsing Error!");
			}
		}
	}
	
	/**
	 * Handle the action of the main button in the Map parsing tab.
	 * 
	 * Run Map data loading and parsing (Jython).
	 */
	private void handleLoadAndParseMapData(){
		loadParseMapBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				// check if mandatory fields were provided
				if (!validateMapFields()) return;
				
				inputMapPath = inputMapPathTxt.getText();
				
				addLogInfo("Running MAP data loader and parser...");
				addLogInfo("Running with configurations:");
				addLogInfo(">> Input map data file: '" + inputMapPath);
				addLogInfo(">> Output database: '" + outputDatabase.name() + "'");
			
				boolean parseResult = false;
				String resultsString = "";
				try {
					// start a new map parser (Python parser)
					OsmHandlerFactory osmFactory = new OsmHandlerFactory();
		
					if (outputDatabase.equals(OutputDatabase.LOCAL)) {
						addLogError("Sorry, 'Local' storage is not provided yet!");
						//TODO
					} else
					if (outputDatabase.equals(OutputDatabase.MONGODB)) {
						String mongoHost = mongodbHostTxt.getText();
						String mongoPort = mongodbPortTxt.getText();
						String mongoDbName = mongodbDatabaseTxt.getText();
						
						addLogInfo(">> MongoDB host name: '" + mongoHost + "'");
						addLogInfo(">> MongoDB port number: '" + mongoPort + "'");
						addLogInfo(">> MongoDB database name: '" + mongoDbName + "'");
						
						// parse data and save to MongoDB
						OsmHandler osmHandler = osmFactory.create(
								mongoHost, Integer.parseInt(mongoPort), mongoDbName);
						parseResult   = osmHandler.parse(inputMapPath);
						resultsString = osmHandler.getParserResults();
					} else
					if (outputDatabase.equals(OutputDatabase.HBASE)) {
						addLogError("Sorry, 'HBase' storage is not provided yet!");
						// TODO
					} else
					if (outputDatabase.equals(OutputDatabase.VOLTDB)) {
						addLogError("Sorry, 'VoltDB' storage is not provided yet!");
						// TODO
					}
				} finally {
					// print parser messages (if any) to log window
					addLogMsg(LogWriter.getLogWriterAsList());
					LogWriter.clearLog();
					
					// show parser result
					if (parseResult) {
						addLogInfo("Map Data Parsing Successful!");
						addLogInfo("Parser Results: " + resultsString);
						// show confirmation popup
						showInfoPopup("Map Data Parsing Successful!\n\n",
									  "Parser Results: " + resultsString);
					} else {
						addLogError("Map Data Parsing Error!\n");
						// show confirmation popup
						showErrorPopup("Map Data Parsing Error!");
					}
				}
			}
		});
	}
	
	/**
	 * Check whether all mandatory fields for trajectory data
	 * loading/parsing were provided.
	 * 
	 * @return True if all the mandatory fields were provided,
	 * false otherwise.
	 */
	private boolean validateDataFields() {
		boolean validate = true;
		// show all missing fields before return
		if (inputDataPathTxt.getText().isEmpty()) {
			addLogError("Input TRAJECTORY data path must be provided.");
			validate = false;
		}
		if (dataFormatContent.isEmpty()) {
			addLogError("Data format must be provided (save).");
			validate = false;
		}
		if (outputDatabase == null) {
			addLogError("Output database must be provided.");
			validate = false;
		}
		if (outputDatabase.equals(OutputDatabase.LOCAL)) {
			if(outputDataPathTxt.getText().isEmpty()) {
				addLogError("Output data path must be provided "
						+ "in 'LOCAL' option.");
				validate = false;
			}
		} else
		if (outputDatabase.equals(OutputDatabase.MONGODB)) {
			if (mongodbHostTxt.getText().isEmpty()) {
				addLogError("MongoDB 'Host Name' must be provided in "
						+ "'MONGODB' option.");
				validate = false;
			}
			if (mongodbPortTxt.getText().isEmpty()) {
				addLogError("MongoDB 'Port Number' must be provided in "
						+ "'MONGODB' option.");
				validate = false;
			}
			if (mongodbDatabaseTxt.getText().isEmpty()) {
				addLogError("MongoDB 'Database Name' must be provided in "
						+ "'MONGODB' option.");
				validate = false;
			}
		}
		
		return validate;
	}

	/**
	 * Check whether all mandatory fields for map data
	 * loading/parsing were provided.
	 * 
	 * @return True if all the mandatory fields were provided,
	 * false otherwise.
	 */
	private boolean validateMapFields(){
		boolean validate = true;
		if (inputMapPathTxt.getText().isEmpty()) {
			addLogError("Input MAP data path must be provided.");
			validate = false;
		}
		if (outputDatabase == null) {
			addLogError("Output database must be provided.");
			validate = false;
		}
		if (outputDatabase.equals(OutputDatabase.LOCAL)) {
			addLogError("Sorry, 'LOCAL' storage is not provided yet.");
			validate = false;
		} else
		if (outputDatabase.equals(OutputDatabase.MONGODB)) {
			if(mongodbHostTxt.getText().isEmpty()){
				addLogError("MongoDB 'Host Name' must be provided in "
						+ "'MONGODB' option.");
				validate = false;
			}
			if (mongodbPortTxt.getText().isEmpty()) {
				addLogError("MongoDB 'Port Number' must be provided in "
						+ "'MONGODB' option.");
				validate = false;
			}
			if (mongodbDatabaseTxt.getText().isEmpty()) {
				addLogError("MongoDB 'Database Name' must be provided in "
						+ "'MONGODB' option.");
				validate = false;
			}
		}
		
		return validate;
	}

	/**
	 * Open and show the content of an HTML file
	 * in a browser-like window.
	 * 
	 * @see HelpBrowser
	 * 
	 * @param htmlFileName The name of the HTML file to load.
	 * HTML file must be inside the application's resources folder.
	 */
	private void showHtmlPage(String htmlFileName){
		String helpContent = IOService.
				readResourcesFileContent(htmlFileName);
		Scene scene = new Scene(
				new HelpBrowser(helpContent, 750, 500), 
				Color.web("#666970")); ;
		Stage outputStage = new Stage();
		outputStage.setTitle("TraMiner Help");
		outputStage.setScene(scene);
		outputStage.show();
	}

	/**
	 * Open a Windows with the output metadata results
	 * after parsing success.
	 */
	private void showResultWindow() {
		try {
			Parent output = FXMLLoader.load(getClass().getResource("OutputScene.fxml"));

			Stage outputStage = new Stage();
			outputStage.setTitle("Parser Output Result");
			outputStage.setScene(new Scene(output));
			outputStage.setHeight(575.0);
			outputStage.setWidth(605.0);
			outputStage.setResizable(false);
			outputStage.show();
			
		} catch (IOException e) {
			addLogError("Error opening 'OutputScene' GUI.");
			e.printStackTrace();
		}
	}

	/**
	 * Open a simple INFORMATION alert/dialog with the given message.
	 * 
	 * @param msg 		The message header text.
	 * @param content	The message content.
	 */
	private void showInfoPopup(String msg, String content) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Parser Message");
		alert.setHeaderText(msg);
		alert.setContentText(content);

		alert.showAndWait();
	}
	
	/**
	 * Open a simple ERROR alert/dialog with the given message.
	 * 
	 * @param content The message content.
	 */
	private void showErrorPopup(String content){
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Parser Message");
		alert.setHeaderText(null);
		alert.setContentText(content);

		alert.showAndWait();
	}
	
	/**
	 * Open a Windows showing the data parsing progress.
	 * Progress is based on the number of files parsed
	 * rather then the number of records.
	 * 
	 * OPen the progress bar with a background process.
	 */
	// TODO
	private void showProgressBar(/*int numFiles*/) {
		Group g = new Group();
	    Scene scene = new Scene(g, 240, 80);
		Stage progressStage = new Stage();
		progressStage.setTitle("Loading and Parsing Progress");
		progressStage.setScene(scene);
		
		// counts reading
		final long total = 100;//numFiles;
		final ProgressBar progressBar = new ProgressBar(0.1);
		
		final Observer progressObserver = new Observer() {			
			@Override
			public void update(Observable o, Object arg) {
				double currentProg = progressBar.getProgress();
				double progress = currentProg + (double)(1 / total);
				progressBar.setProgress(progress);
				System.out.println(progress);
			}
		};
		IOService.getWritingObservable().addObserver(progressObserver);
	
		// create progress window
		HBox hb = new HBox();
	    hb.setSpacing(5);
	    hb.setAlignment(Pos.CENTER);
	    hb.getChildren().addAll(progressBar);
	    g.getChildren().add(hb);
	    		
		// show progress window
		progressStage.show();
	}

	/**
	 * Add a INFORMATION message to main GUI log.
	 * 
	 * @param msg The information message to add.
	 */
	private void addLogInfo(String msg){
		TextField text = new TextField("\n> [INFO] " + msg);
		text.setStyle("-fx-text-box-border: transparent;" +
					  "-fx-focus-color: transparent;" + 
				      "-fx-background-color: -fx-control-inner-background;");
		text.setFont(Font.font("Courier New"));
		text.setEditable(false);
		// add to log panel
		logTxt.getChildren().add(text);
		
		System.out.println( "> [INFO] " + msg);
	}
	
	/**
	 * Add a ERROR message to main GUI log.
	 * 
	 * @param msg The error message to add.
	 */
	private void addLogError(String msg){
		TextField text = new TextField("\n> [ERROR] " + msg);
		text.setStyle("-fx-text-fill: red;" +
				      "-fx-text-box-border: transparent;" +
					  "-fx-focus-color: transparent;" + 
				      "-fx-background-color: -fx-control-inner-background;");
		text.setFont(Font.font("Courier New"));
		text.setEditable(false);
		// add to log panel
		logTxt.getChildren().add(text);
		
		System.err.println( "> [ERROR] " + msg);
	}
	
	/**
	 * Add a list of messages to main GUI log.
	 * 
	 * @param msgList The list of messages to add.
	 */
	private void addLogMsg(List<String> msgList){
		for (String msg : msgList) {
			if(!msg.equals("")){
				TextField text = new TextField("> " + msg);
				if (msg.startsWith("[INFO]")) {
					text.setStyle("-fx-text-box-border: transparent;" +
							  	  "-fx-focus-color: transparent;" + 
						      	  "-fx-background-color: -fx-control-inner-background;");
					System.out.println( "> " + msg);
				} else {
					text.setStyle("-fx-text-fill: red;" +
						      	  "-fx-text-box-border: transparent;" +
						      	  "-fx-focus-color: transparent;" + 
						      	  "-fx-background-color: -fx-control-inner-background;");
					System.err.println( "> " + msg);
				}
				text.setFont(Font.font("Courier New"));
				text.setEditable(false);
				// add to log panel
				logTxt.getChildren().add(text);
			}
		}
	}
	
	public class ProgressObserver implements Observer {

		@Override
		public void update(Observable o, Object arg) {
			// TODO Auto-generated method stub
			
		}
	}
}
