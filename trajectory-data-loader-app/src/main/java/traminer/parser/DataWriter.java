package traminer.parser;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.NullArgumentException;
import org.apache.log4j.Logger;
import org.bson.Document;

import traminer.io.HDFSService;
import traminer.io.IOService;
import traminer.io.db.MongoDBService;
import traminer.io.params.HDFSParameters;
import traminer.io.params.LocalFSParameters;
import traminer.io.params.MongoDBParameters;
import traminer.parser.analyzer.Keywords;
import traminer.parser.analyzer.Keywords.OutputDatabase;
import traminer.parser.analyzer.Keywords.OutputFormat;
import traminer.parser.format.ArrayFormat;
import traminer.parser.format.DataFormat;
import traminer.parser.format.Format.AttributeEntry;

/**
 * Client to manage data output. Generate output files after data
 * parsing (i.e. parsed trajectories, metadata, output format) and 
 * write the files to the database systems supported in the application.
 * 
 * @see TrajectoryParser
 * 
 * @author douglasapeixoto
 */
@SuppressWarnings("serial")
public class DataWriter implements ParserInterface {
	// output database
	private static OutputDatabase outputDb;
	
	// Local data service and parameters
	private static LocalFSParameters localParams = null;
	
	// MongoDB service and parameters
	private static MongoDBService mongodb = null;
	
	// HHFS service and parameters
	private static HDFSService hdfs;
	private static HDFSParameters hdfsParams;
	
	// System log
	private static Logger log = Logger.getLogger(DataWriter.class);
		
	/**
	 * Initialize this data writer service with Local data storage.
	 * 
	 * @param params Local data storage location and parameters.
	 */
	public static void init(LocalFSParameters params) {
		if (params == null) {
			throw new NullArgumentException(
					"Local file system parameters must not be null.");
		}
		localParams = params;
		outputDb = OutputDatabase.LOCAL;
	}
	
	/**
	 * Initialize data writer service with MongoDB.
	 * 
	 * @param params MongoDB access parameters and configurations.
	 */
	public static void init(MongoDBParameters params) {
		if (params == null) {
			throw new NullArgumentException(
					"MongoDB parameters must not be null.");
		}
		params.addCollectionName("data", DATA_COLL_NAME);
		params.addCollectionName("meta", META_COLL_NAME);
		try {
			mongodb = new MongoDBService(params);
		} catch (IOException e) {
			log.error("Unable to initialize MongoDB service.", e);
		}
		outputDb = OutputDatabase.MONGODB;
	}
	
	/**
	 * Initialize this data writer service with Local data storage.
	 * 
	 * @param params Local data storage location and parameters.
	 */
	public static void init(HDFSParameters params) {
		if (params == null) {
			throw new NullArgumentException(
					"HDFS parameters must not be null.");
		}		
		try {
			hdfs = new HDFSService(params);
		} catch (Exception e) {
			log.error("Unable to initialize HDFS service.", e);
		}
		outputDb = OutputDatabase.HDFS;
	}
	
	/**
	 * Save the parsed data file to the output database of choice.
	 * Save data in CSV file format by default.
	 * 
	 * @param parsedFile A stream with the lines of the file to save.
	 */
	public static void saveDataFile(Stream<String> parsedFile) {
		// save output file
		final String fileName = "data_file_" + System.currentTimeMillis() + ".csv";

		// save the parsed file to local folder	
		if (outputDb.equals(OutputDatabase.LOCAL)) {
			final String outDir = localParams.getLocalDataPath().toString();
			try {
				IOService.writeFile(parsedFile, outDir, fileName);
			} catch (IOException e) {
				log.error("Error saving data file '" +fileName+ "'.", e);
			} 
		}		
		// save the parsed file to MongoDB
		else if (outputDb.equals(OutputDatabase.MONGODB)) {
			parsedFile.forEach(line -> {
				if (line.length() > 0) { // if not an empty document
					final String[] values = line.split(";");
					final String _id = values[0];
					final String[] _coords = values[1].split(",");
					Document mongoDoc = new Document()
							.append("_id",  _id)
							.append("_coordinates", _coords)
							.append("record", line);
					// TODO add semantic attrs separately
					mongodb.insertDocument(mongoDoc, DATA_COLL_NAME);
				}
			});
		}
		// save the parsed file to HDFS folder	
		else if (outputDb.equals(OutputDatabase.HDFS)) {
			final String outDir = hdfsParams.getRootDir();
			try {
				hdfs.writeFile(parsedFile.sequential().collect(Collectors.toList()), 
						outDir, fileName);
			} catch (Exception e) {
				log.error("Error saving data file '" +fileName+ "' to HDFS.", e);
			}
		}
	}

	/**
	 * Generate and save the OutputFormatFile. File containing the 
	 * specifications of the intermediate data format. 
	 * 
	 * <br> Save file as 'output-format.tddf'.
	 * 
	 * @param dataFormat User-defined Input data format specifications.
	 * @param outFormat  User-defined Output data format.
	 * 
	 * @return A string containing the script/content of the Output
	 * Data Format file. Return NULL 
	 * 
	 * @throws ParserException If the file could not be successfully
	 * created or saved.
	 */
	public static String saveOutputFormatFile(
			DataFormat dataFormat, OutputFormat outFormat) throws ParserException {
		try {
			// get the auxiliary object containing the coordinates array format
			ArrayFormat coordArrayFormat = dataFormat.getCoordinatesArrayFormat();

			// format commands configuration
			String commandFormat = "";
			commandFormat += Keywords._OUTPUT_FORMAT + "\t" + outFormat.name() + "\n";
			commandFormat += Keywords._COORD_SYSTEM  + "\t" + dataFormat.getCoordinateSystem().name() + "\n";
			commandFormat += Keywords._DECIMAL_PREC  + "\t" + dataFormat.getDecimalPrecision() + "\n";
			commandFormat += Keywords._SPATIAL_DIM   + "\t" + coordArrayFormat.getSpatialDimensions();
			
			// format attributes configuration
			String idFormat="", coordFormat="", otherAttrFormat="";
			// for auto-generated IDs
			if (dataFormat.isAutoId()) {
				if (dataFormat.getIdPrefix().matches("\\d+")){
					idFormat = "\n" + Keywords._ID + "\t" + Keywords.INTEGER;
				} else {
					idFormat = "\n" + Keywords._ID + "\t" + Keywords.STRING;
				}
			}
			// print attributes specifications
			for (AttributeEntry attr : dataFormat.getAttributesList()) {
				if (attr.name.equals(Keywords._ID.name())) {
					idFormat = "\n" + Keywords._ID + "\t" + dataFormat.getAttribute(
							dataFormat.getIdAttrIndex()).type;
				} else 
				if (attr.name.equals(Keywords._COORDINATES.name())) {
					// format the coordinates array type
					String arrayType = Keywords.ARRAY + "(";
					
					int xPos = coordArrayFormat.getXAttrIndex();
					int yPos = coordArrayFormat.getYAttrIndex();
					int tPos = coordArrayFormat.getTimeAttrIndex();
					
					String xType = coordArrayFormat.getAttribute(xPos).type;
					String yType = coordArrayFormat.getAttribute(yPos).type;
					String tType = coordArrayFormat.getAttribute(tPos).type;
					
					// add spatial attributes
					arrayType += Keywords._X + " " + Keywords.getBasicType(xType) + " " + 
								 Keywords._Y + " " + Keywords.getBasicType(yType);
								 
					// add temporal attribute
					if (outFormat.equals(OutputFormat.SPATIAL_TEMPORAL)) {
						arrayType += " " + 
								 Keywords._TIME + " " + Keywords.getBasicType(tType);
					} 
					// add semantic attributes
					else if (outFormat.equals(OutputFormat.ALL)) {
						arrayType += " " + 
								 Keywords._TIME + " " + Keywords.getBasicType(tType);
						for (AttributeEntry arrayAttr: coordArrayFormat.getAttributesList()) {
								 if (arrayAttr.name.equalsIgnoreCase(Keywords._X.name()));
							else if (arrayAttr.name.equalsIgnoreCase(Keywords._Y.name()));
							else if (arrayAttr.name.equalsIgnoreCase(Keywords._TIME.name()));
							else {arrayType += " " + arrayAttr.name + " " + arrayAttr.type;}
						}
					}				
					coordFormat = "\n" + Keywords._COORDINATES + "\t" + arrayType + ")";
				} else {
					if (outFormat.equals(OutputFormat.ALL)) {
						otherAttrFormat += "\n" + attr.name + "\t" + attr.type;
					}
				}
			}

			// create the script of the output format file
			final String script = commandFormat + idFormat + coordFormat + otherAttrFormat;
			
			// create the file in the database
			saveOutputFormatFile(script);
			
			return script;
		} catch (Exception e) {			
			throw new ParserException("Unable to generate and save 'Output Data Format' file.", e);
		}
	}

	/**
	 * Save the OutputFormatFile. File containing the 
	 * specifications of the intermediate data format. 
	 * 
	 * <br> Save file as 'output-format.tddf'.
	 * 
	 * @param outputDataFormat The script containing the 
	 * Output Data format.
	 * 
	 * @throws ParserException If the file could not be successfully
	 * created or saved.
	 */
	public static void saveOutputFormatFile(
			String outputDataFormat) throws ParserException {
		final String fileName = "output-format.tddf";
		try {
			// save file to local folder	
			if (outputDb.equals(OutputDatabase.LOCAL)) {
				final String outDir = localParams.getLocalDataPath().toString();
				IOService.writeFile(outputDataFormat, outDir, fileName);
			}		
			// save file to MongoDB
			else if (outputDb.equals(OutputDatabase.MONGODB)) {
				mongodb.insertDocument(new Document("_id", "output-format")
					   .append("value", outputDataFormat), META_COLL_NAME);
			}
			// save file to HDFS
			else if (outputDb.equals(OutputDatabase.HDFS)) {
				final String outDir = hdfsParams.getRootDir();
				hdfs.writeFile(outputDataFormat, outDir, fileName);
			}
		} catch (Exception e) {			
			throw new ParserException("Unable to generate and save 'Output Data Format' file.", e);
		}
	}
	
	/**
	 * Generate and save the metadata file, containing informations and
	 * statistics about the output dataset. 
	 * 
	 * <br> Save file as 'metadata.meta'.
	 * 
	 * @param dataFormat User-defined Input data format specifications.
	 * @param outFormat  User-defined Output data format.
	 * 
	 * @return The metadata script.
	 * 
	 * @throws ParserException If the file could not be successfully
	 * created or saved.
	 */
	public static String saveMetadataFile(DataFormat dataFormat, 
			OutputFormat outFormat) throws ParserException {
		try {
			// number of trajectory attributes in the output data
			int attrCount;
			// number of coordinate attributes in the output data
			int coordAttrCount;
			if (outFormat.equals(OutputFormat.ALL)) {
				attrCount = dataFormat.numValidAttributes();
				coordAttrCount = dataFormat.getCoordinatesArrayFormat().numValidAttributes();
			} else 
			if (outFormat.equals(OutputFormat.SPATIAL)) {
				attrCount = 2; // id and coordinates only
				coordAttrCount = dataFormat.getCoordinatesArrayFormat().getSpatialDimensions();
			} else {
				attrCount = 2; // id and coordinates only
				coordAttrCount = dataFormat.getCoordinatesArrayFormat().getSpatialDimensions() + 1;
			}
			
			String script = "";
			script += "NUM_FILES\t" + MetadataService.getFilesCount() + "\n";		
			script += "NUM_ATTRIBUTES\t" + attrCount + "\n";
			script += "NUM_COORD_ATTRIBUTES\t" + coordAttrCount + "\n";
			script += MetadataService.getMetadata();
			
			// create the file in the database
			saveMetadataFile(script);
			
			return script;
		} catch (Exception e) {
			throw new ParserException("Unable to generate and save 'Metadata' file.", e);
		}
	}
	
	/**
	 * Save the metadata file, containing informations and
	 * statistics about the output dataset. 
	 * 
	 * <br> Save file as 'metadata.meta'.
	 * 
	 * @param metadata Content of the metadata file.
	 *  
	 * @throws ParserException If the file could not be successfully
	 * created or saved.
	 */
	public static void saveMetadataFile(String metadata) throws ParserException {
		final String fileName = "metadata.meta";
		try {
			// save file to local folder	
			if (outputDb.equals(OutputDatabase.LOCAL)) {
				String outDir = localParams.getLocalDataPath().toString();
				IOService.writeFile(metadata, outDir, fileName);
			}
			// save file to MongoDB
			else if (outputDb.equals(OutputDatabase.MONGODB)) {
				mongodb.insertDocument(new Document("_id", "metadata")
						.append("value", metadata), META_COLL_NAME);
			}
			// save file to HDFS
			else if (outputDb.equals(OutputDatabase.HDFS)) {
				final String outDir = hdfsParams.getRootDir();
				hdfs.writeFile(metadata, outDir, fileName);
			}
		} catch (Exception e) {
			throw new ParserException("Unable to generate and save 'Metadata' file.", e);
		}
	}
}
