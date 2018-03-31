package traminer.loader.gui;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import traminer.parser.MetadataService;
import traminer.parser.TrajectoryParser;

/**
 * Controller for the metadata results window. Configure the output 
 * Window {@link OutputScene.fxml} containing the result metadata.
 *  
 * @author uqdalves
 */
public class OutputGUIController implements Initializable {
	// root panel in the output window
	@FXML
	private Pane rootPane;

	@FXML
	private TableView<MetadataEntry> metadataTable;
	@FXML
	private TableColumn<MetadataEntry,String> attributeCol;
	@FXML
	private TableColumn<MetadataEntry,String> valueCol;
	
	@FXML
	private Button closeBtn;
	
	@Override
	public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
		attributeCol.setCellValueFactory(
                new PropertyValueFactory<MetadataEntry,String>("metadata"));
		valueCol.setCellValueFactory(
                new PropertyValueFactory<MetadataEntry,String>("value"));
		
		String metadataScript = MetadataService.getMetadata();
		List<MetadataEntry> itemList = new ArrayList<MetadataEntry>();
		for (String item : metadataScript.split("\n")) {
			String items[] = item.split("\\s+");
			itemList.add(new MetadataEntry(items[0], items[1]));
		}
		
		// add metadata items to output table
		ObservableList<MetadataEntry> metadataItems = 
				FXCollections.observableArrayList(itemList);
		metadataTable.setItems(metadataItems);
		
		// close window event
		closeBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				rootPane.getScene().getWindow().hide();
			}
		});
	}
	
	/**
	 * Auxiliary object to be used in the TableView. 
	 * An input in the metadata's table, containing  
	 * the metadata's 'name' and 'value'.
	 */
	@SuppressWarnings("serial")
	public class MetadataEntry implements Serializable {
		// metadata attribute name
		private final SimpleStringProperty metadata;
		// metadata value
        private final SimpleStringProperty value;
        
        /**
         * Creates a new metadata entry.
         * 
         * @param metadata Metadata entry's name.
         * @param value    Metadata value.
         */
		public MetadataEntry(String metadata, String value) {
			this.metadata = new SimpleStringProperty(metadata);
			this.value = new SimpleStringProperty(value);
		}
		
		/**
		 * @return The metadata entry's name.
		 */
		public String getMetadata() {
			return metadata.get();
		}
		/**
		 * @param metadata The metadata entry's name.
		 */
		public void setMetadata(String metadata) {
			this.metadata.set(metadata);
		}
		/**
		 * @return The metadata entry's value.
		 */
		public String getValue() {
			return value.get();
		}
		/**
		 * @param value The metadata entry's value.
		 */
		public void setValue(String value) {
			this.value.set(value);
		}
	}
}
