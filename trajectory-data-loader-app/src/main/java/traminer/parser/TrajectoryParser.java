package traminer.parser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.apache.log4j.Logger;

import traminer.io.IOService;
import traminer.io.params.HDFSParameters;
import traminer.io.params.LocalFSParameters;
import traminer.io.params.MongoDBParameters;
import traminer.io.params.VoltDBParameters;
import traminer.parser.analyzer.Keywords;
import traminer.parser.analyzer.LexicalAnalyzer;
import traminer.parser.analyzer.Symbols;
import traminer.parser.analyzer.SyntaxAnalyzer;
import traminer.parser.analyzer.Keywords.OutputFormat;
import traminer.parser.analyzer.LexicalAnalyzer.Token;
import traminer.parser.analyzer.SemanticAnalyzer;
import traminer.parser.format.ArrayFormat;
import traminer.parser.format.DataFormat;
import traminer.parser.format.Format.AttributeEntry;
import traminer.util.DateUtils;
import traminer.util.DeltaEncoder;
import traminer.util.math.Decimal;
import traminer.util.spatial.distance.EuclideanDistanceFunction;
import traminer.util.spatial.distance.HaversineDistanceFunction;

/**
 * Read and parse the input trajectory dataset, based on 
 * the specifications given in the Input Data Format.
 * <p>
 * Store the data into the user-specified storage system.
 * 
 * @see DataFormat
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class TrajectoryParser implements ParserInterface {
	// contains the configuration of the input data format
	private DataFormat dataFormat = null;
	// the content/lines of the input data format
	private List<String> dataFormatContent = null;
	// path to the input data
	private Path inputDataPath = null;
	// output data format
	private OutputFormat outputFormat = null;
	// tokens from the Lexical analyzer
	private List<Token> lexTokens = null;
	// metadata service and parameters
	private MetadataService metadataService = null;
	// total number of files read
	private long filesCount = 0;
	// number of files that could not be read
	private long errorFileCount = 0;
	// total number of trajectories read
    private AtomicInteger trajCount = new AtomicInteger(0);
	// metadata script generated during the data loading
	private static String outputFormatScript = "";
	// output format script generated during the data loading
	private static String metadataScript = "";

	// System log
	private static Logger log = Logger.getLogger(TrajectoryParser.class);

	/**
	 * Creates a new trajectory data parser.
	 * 
	 * @param inputDataPath   Path to input data directory.
	 * @param inputDataFormat Input data format specifications/content.
	 */
	public TrajectoryParser(
			String inputDataPath, 
			String inputDataFormat) {
		this.inputDataPath = Paths.get(inputDataPath);
		this.dataFormatContent = 
				Arrays.asList(inputDataFormat.split("\n"));
	}
	
	/**
	 * Creates a new trajectory data parser.
	 * 
	 * @param inputDataPath   Path to input data directory.
	 * @param inputDataFormat Input data format file content.
	 * 		Data format given as a list of file lines, 
	 * 		one line per list item.
	 */
	public TrajectoryParser(
			String inputDataPath, 
			List<String> inputDataFormat) {
		this.inputDataPath  = Paths.get(inputDataPath);
		this.dataFormatContent = inputDataFormat;
	}

	/**
	 * Parse the input trajectory data based on the 
	 * user-specified parameters. 
	 * <p> 
	 * Save parsed data and output files to a local directory.
	 * 
	 * @param outputFormat The {@link OutputFormat} of choice 
	 * 		(e.g. SPATIAL, ALL, SPATIAL_TEMPORAL).
	 * @param localParams Local storage parameters 
	 * 		(e.g. output directory);
	 * 
	 * @return Whether or not the parsing was successful.
	 */
	public boolean parseToLocal(
			final OutputFormat outputFormat, 
			final LocalFSParameters localParams){
		// initialize local database service
		DataWriter.init(localParams);
		this.outputFormat = outputFormat;
		
		// parse and store to local directory
		return parse();
	}

	/**
	 * Parse the input trajectory data based on the 
	 * user-specified parameters. 
	 * <p> 
	 * Save parsed data and output files to MongoDB system.
	 * 
	 * @param outputFormat The {@link OutputFormat} of choice 
	 * 		(e.g. SPATIAL, ALL, SPATIAL_TEMPORAL).
	 * @param mongodbParams MongoDB access parameters.
	 * 
	 * @return Whether or not the parsing was successful.
	 */
	public boolean parseToMongoDB(
			final OutputFormat outputFormat, 
			final MongoDBParameters mongodbParams){
		// initialize MongoDB storage service
		DataWriter.init(mongodbParams);
		this.outputFormat = outputFormat;
		
		// parse and store to MongoDB
		return parse();
	}
	
	/**
	 * Parse the input trajectory data based on the 
	 * user-specified parameters. 
	 * <p> 
	 * Save parsed data and output files to HBase distributed 
	 * database system.
	 * 
	 * @param outputFormat The {@link OutputFormat} of choice 
	 * 		(e.g. SPATIAL, ALL, SPATIAL_TEMPORAL).
	 * @param hbaseParams HBase access parameters.
	 * 
	 * @return Whether or not the parsing was successful.
	 */
	public boolean parseToHBase(
			final OutputFormat outputFormat,
			final HDFSParameters hbaseParams){
		// TODO:
		this.outputFormat = outputFormat;
		return false;
	}

	/**
	 * Parse the input trajectory data based on the 
	 * user-specified parameters. 
	 * <p> 
	 * Save parsed data and output files to VoltDB in-memory 
	 * database system.
	 * 
	 * @param outputFormat The {@link OutputFormat} of choice 
	 * 		(e.g. SPATIAL, ALL, SPATIAL_TEMPORAL).
	 * @param voltdbParams VoltDB access parameters.
	 * 
	 * @return Whether or not the parsing was successful.
	 */
	public boolean parseToVoltDB(
			final OutputFormat outputFormat,
			final VoltDBParameters voltdbParams){
		// TODO:
		this.outputFormat = outputFormat;
		return false;
	}
	
	/**
	 * Parse input trajectory data based on the 
	 * user-specified parameters.
	 * <p>
	 * Validates the Input Data Format syntax
	 * and semantic before parsing.
	 * 
	 * @return Whether or not the parsing was successful.
	 */
	private boolean parse() {
		// validates the input data format
		if (!validateDataFormat()) {
			return false;
		}
		
		// creates an object for the user-provided data format
		dataFormat = new DataFormat(lexTokens); 
		
		// init metadata management service			
		if (dataFormat.getCoordinateSystem().equals(Keywords.GEOGRAPHIC)) {
			metadataService = new MetadataService(
					new HaversineDistanceFunction());
		} else 
		if (dataFormat.getCoordinateSystem().equals(Keywords.CARTESIAN)) {
			metadataService = new MetadataService(
					new EuclideanDistanceFunction());
		} else {
			throw new IllegalArgumentException("Coordinates systems '" + 
						dataFormat.getCoordinateSystem()+ "' is not supported.");
		}
		
		// check whether the dataset was read and processed
		if (!readAndParseData()) {
			return false;
		}
		
		// create and write output files, and get the files script
		try {
			outputFormatScript = DataWriter
					.saveOutputFormatFile(dataFormat, outputFormat);
			metadataScript     = DataWriter
					.saveMetadataFile(dataFormat, outputFormat, metadataService);
			return true;
		} catch (ParserException e) {
			log.error(e.getMessage(), e.getCause());
			return false;
		}
	} 

	/**
	 * Read the folder containing the input trajectory data files 
	 * (recursively), and parse each file according with the 
	 * user-specified data format and output parameters.
	 * <p>
	 * Saves the parsed files to the output database of choice.
	 * 
	 * @return Whether or not ALL files were successfully read and parsed.
	 */
	private boolean readAndParseData() {
		String[] delimList = new String[dataFormat.getDelimiters().size()];
		delimList = dataFormat.getDelimiters().toArray(delimList);

		// read file paths recursively (path names only)
		List<String> pathList = IOService.getFilesPathList(inputDataPath);
		
		// number of files read (metadata)
		filesCount = pathList.size();
		metadataService.setFilesCount(filesCount);
		
		// process every file
		for (String path : pathList) {
			// check whether the file was successfully 
			// read, processed and saved.
			try {
				// read the current file lines as a stream
				Stream<String> fileLines = 
						IOService.readFileAsStream(path);
				// pre-process the file according with the records 
				// delimiter, put one data record per file line
				fileLines = preProcessDataFile(fileLines);
				// parse file (one record per line)
				Stream<String> parsedFile = parseFile(fileLines, delimList);
				// write file to database of choice
				DataWriter.saveDataFile(parsedFile);
			} catch(Exception e) {
				errorFileCount++;
				String errMsg = "Unable to parse file: '" + path + "'.\nFile Ignored!";
				log.error(errMsg, new ParserException(errMsg, e));
			}
		}
		
		return (errorFileCount != filesCount);
	}

	/**
	 * Parse the given file stream to the Intermediate format
	 * of choice, each line in the file is split according to 
	 * the given attributes' delimiter list.
	 * <p>
	 * Note: Assume the data files have already been pre-processed,
	 * {@link preProcessDataFile}, hence contains one trajectory  
	 * record per file line. 
	 * 
	 * @param fileLineStream Stream with the input file lines to parse.
	 * @param delimList List of attributes' delimiter in the data records.
	 * 
	 * @return A Stream with the parsed file lines. 
	 */
	private Stream<String> parseFile(
			Stream<String> fileLineStream,
			final String[] delimList){
		final int idPos    = dataFormat.getIdAttrIndex();
		final int coordPos = dataFormat.getCoordinatesAttrIndex();
				
		// Map each line of the file, according to the 
		// Input Data Format, to the chosen Intermediate Format.
		Stream<String> parsedLinesStream = fileLineStream.parallel().map(
				new Function<String, String>() {
			public String apply(String line) {
				// check whether a record of the input data was 
				// successfully read and processed
				try {
					// attributes to read
					String id = "";
					String coordinates  = "";
					String semanticAttr = "";
					// get trajectory attributes
					String[] attrValues = getAttributes(line, delimList);
					String attrValue;
					for (int i=0; i<attrValues.length; i++) {
						// process only not-ignored attributes
						if(dataFormat.isIgnoredAttr(i))	continue;						
	
						// check if this is the ID or Coordinates attribute,
						// or an array type attribute (format the array)
						attrValue = attrValues[i];
						AttributeEntry attr = dataFormat.getAttribute(i);
						if (i == idPos) {
							id = attrValue;
						} 
						// coordinates
						else if (i == coordPos) {
							coordinates = parseCoordinatesArray(attrValue);
						}  
						// semantic attribute, array type 
						else if (attr.isArrayType()) {
							attrValue = parseArray(attr.type, attrValue);
							semanticAttr += ";" + attrValue;
						} 
						// semantic attribute, single valued
						else {
							semanticAttr += ";" + attrValue;
						}
					}
	
					// auto-generated IDs
					if (dataFormat.isAutoId()) {
						int num = trajCount.incrementAndGet();
						// if the prefix is a integer number
						String idPrefix = dataFormat.getIdPrefix();
						if (idPrefix.matches("\\d+")) {
							// TODO no need to do parseInt() every time
							id = ""+(num + Integer.parseInt(idPrefix) -1);
						} else {
							id = dataFormat.getIdPrefix() + "_" + num;
						}
					}
					
					// parse attributes according to the output format
					String parsedLine = "";
					if (outputFormat.equals(OutputFormat.ALL)) {
						parsedLine = id + ";" + coordinates + semanticAttr;
					} else{
						parsedLine = id + ";" + coordinates;
					}
					
					return parsedLine;
					
				// Error processing file line	
				} catch (Exception e) {
					log.warn("Unable to parse data record: '" + line 
							+ "'.\nRecord Ignored!", new ParserException(e));
					return "";
				}
			}
		});
					
		return parsedLinesStream;
	}
	
	/**
	 * Perform the Lexical, Syntactical and Semantical
	 * analysis of the input data format.
	 * 
	 * @return Result of the validation.
	 */
	private boolean validateDataFormat() {
		// lexical analysis
		LexicalAnalyzer lex = new LexicalAnalyzer();
		lexTokens = lex.analyzeFile(dataFormatContent);
				
		// syntax analysis
		SyntaxAnalyzer syntax = new SyntaxAnalyzer();
		boolean syntaxResult = syntax.analyze(lexTokens);

		// semantic analysis
		SemanticAnalyzer semantic = new SemanticAnalyzer();
		boolean semanticResult = semantic.analyze(lexTokens);
		
		if (!syntaxResult || !semanticResult) {
			String errMsg = "Error parsing the data. "
					+ "Please check the input data format.";
			log.error(errMsg, new ParserException(errMsg));
		}
		
		return (syntaxResult && semanticResult);
	}
	
	/**
	 * Put the data items one record per file line, and remove
	 * ignored lines (if any).
	 * 
	 * @param fileLines The input file to preprocess.
	 * @return The file lines after preprocessing.
	 */
	private synchronized Stream<String> preProcessDataFile(Stream<String> fileLines) {
		// remove ignored lines (if any)
		if (dataFormat.hasLinesToIgnore()) {
			fileLines = fileLines.sequential().filter(new Predicate<String>() {
				int lineNumber = 0;
				public boolean test (String line) {
					if (dataFormat.getIgnoredLinesList().contains(lineNumber++)) {
						return false;
					} return true;
				}
			});
		}

		// put one data record per file line
		final String recordsDelim = dataFormat.getRecordsDelim();
		
		// one record/item per file line
		if (recordsDelim.equals(LINE_BREAK)){
			return fileLines; // nothing to do
		} else 
		// one record/item per file
		if (recordsDelim.equals(Keywords.EOF.name())) {
			String record = fileLines
					.sequential()
					.reduce((a,b) -> a + LINE_BOND + b) // LINE_BOND changed from LINE_SPACE
					.get(); 
			return Stream.of(record);
		}
		
		// many records per file, split by delimiter
		Builder<String> streamBuilder = Stream.builder();
		Iterator<String> linesItr = fileLines.iterator();
		String record = "";
		while (linesItr.hasNext()) {
			String line = linesItr.next();
			if (line.startsWith(recordsDelim)) {
				// add previous record, if any
				if (!record.isEmpty()) {
					streamBuilder.accept(record);
				}
				// start new record
				record = line.replace(recordsDelim, "");
			} else 
			if (record.isEmpty()) {
				record = line;
			} else {
				record += LINE_BOND + line;
			}
		}
		// last record
		if (!record.isEmpty()) {
			streamBuilder.accept(record);
		}
				
		return streamBuilder.build();		
	}

	/**
	 * A Thread-safe method to split a given file line (trajectory record, attributes) 
	 * based on the list of attributes delimiters. This method assumes that the file 
	 * has already been pre-processed, thus has one record per line.
	 * 
	 * @param line A line (trajectory record) in the input file.
	 * @param delimList List of attributes' delimiters
	 * @return The list of attributes' values in this file line.
	 */
	private synchronized String[] getAttributes(String line, String[] delimList) {
		// process the line, read each attribute in the line
		int numDelim = delimList.length;
		String[] attrValues = new String[numDelim]; 
		String attrValue = "";
		int toIndex = 0, fromIndex = 0;  // attribute index in the line
		for (int i=0; i<numDelim; i++) {
			toIndex = line.indexOf(delimList[i], fromIndex);
			// read until the end of the line
			if (toIndex == -1) {
				attrValue = line.substring(fromIndex);
			} 
			// stop at the given delimiter
			else {
				attrValue = line.substring(fromIndex, toIndex);
			}
			attrValues[i] = attrValue;
			
			fromIndex = toIndex + 1;
		}

		return attrValues;
	}
	
	/**
	 * A Thread-safe method to format an Array type attribute. 
	 * Put all array items comma separated.
	 * 
	 * @param arrayFormat Array type description/specification, 
	 * as in the file format
	 * @param arrayString The array string (items)
	 * 
	 * @return The new formated array
	 */
	private synchronized String parseArray(
			String arrayFormat, String arrayString){
		// get the auxiliary object containing the array format
		ArrayFormat array = new ArrayFormat(arrayFormat);
		// put all array items comma separated
		for (String delim : array.getDelimiters()) {
			arrayString = arrayString.replace(delim, ",");
		}
// TODO: colocar os attributos de um general array delta-compressed?
		return arrayString;
	}

	/**
	 * A Thread-safe method to parse the Coordinates ARRAY, i.e.
	 * put all array items comma separated, and organize the attributes 
	 * in (x,y,time) order, or (x,y,time,other_attributes) order 
	 * if the coordinates have attributes other than spatial-temporal.
	 * <p>
	 * Also compress the spatial-temporal attributes in the array
	 * (x,y,time) using delta encoding (if they are not already compressed.
	 *
	 * @param arrayString The coordinates array (items)
	 * 
	 * @return The formated coordinates array.
	 */
	private synchronized String parseCoordinatesArray(String arrayString) {
		// get the auxiliary object containing the coordinates array format
		ArrayFormat coordArrayFormat = dataFormat.getCoordinatesArrayFormat();

		// read the values in the array, split by the delimiters
		List<String> itemValues = new ArrayList<>();
		int index = 0;
		// while there are values to read 
		while (index != -1) {
			for (String delim : coordArrayFormat.getDelimiters()) {
				index = arrayString.indexOf(delim);
				if (index == -1){
					// last item 
					itemValues.add(arrayString);
				} else {
					// move to next item
					itemValues.add(arrayString.substring(0, index));
					arrayString = arrayString.substring(index+1, arrayString.length());
				}
			}
		}
/*
TODO Usar o getAttributes ao inves do codigo acima, mas ta dando erro
String[] delimList = new String[coordArrayFormat.getDelimiters().size()];
delimList = coordArrayFormat.getDelimiters().toArray(delimList);
String itemValues[] = getAttributes(arrayString, delimList);
*/
		// read array item values
		int numItems = itemValues.size();
		int numAttr  = coordArrayFormat.numAttributes();
		int xPos = coordArrayFormat.getXAttrIndex();
		int yPos = coordArrayFormat.getYAttrIndex();
		int tPos = coordArrayFormat.getTimeAttrIndex();
		
		// get spatial-temporal attributes only
		final int numPts = numItems / numAttr;
		String[] xValues = new String[numPts];
		String[] yValues = new String[numPts];
		String[] tValues = new String[numPts];

		for (int i=0,j=0; i<numItems; i+=numAttr,j++) {
			xValues[j] = itemValues.get(i + xPos);
			yValues[j] = itemValues.get(i + yPos);
			tValues[j] = itemValues.get(i + tPos);
		}

		AttributeEntry xAttr = coordArrayFormat.getAttribute(xPos);
		AttributeEntry yAttr = coordArrayFormat.getAttribute(yPos);
		AttributeEntry tAttr = coordArrayFormat.getAttribute(tPos);

		// check if any coordinate attribute is compressed (delta type)
		boolean isDeltaX, isDeltaY, isDeltaT;
		Token token;
		token = new LexicalAnalyzer().analyzeToken(xAttr.type, false);
		isDeltaX = token.lexSymbol.equals(Symbols.DeltaTypeSymbol); 
		token = new LexicalAnalyzer().analyzeToken(yAttr.type, false);
		isDeltaY = token.lexSymbol.equals(Symbols.DeltaTypeSymbol);
		token = new LexicalAnalyzer().analyzeToken(tAttr.type, false);
		isDeltaT = token.lexSymbol.equals(Symbols.DeltaTypeSymbol);

		// convert DATETIME attributes from the given pattern to INTEGER, 
		// i.e. number of milliseconds passed since 01/Jan/1970.
		if (Keywords.isDateTimeType(tAttr.type)) {
			formatDate(tValues, tAttr.type);
			// attribute type now is INTEGER
			tAttr = coordArrayFormat.new AttributeEntry(
					tAttr.name, Keywords.INTEGER.name(), tAttr.delim);
		}
		// fix backwards time-stamp values (if any)
		fixTimeStamps(tValues, tAttr.type);

		// update metadata (if values are numeric)
		metadataService.addValues(
				xValues, isDeltaX, xAttr.type,
				yValues, isDeltaY, yAttr.type,
				tValues, isDeltaT, tAttr.type);

		// delta compress (x,y,t) values (if not)
		xValues = compressValues(xValues, xAttr.type, isDeltaX);
		yValues = compressValues(yValues, yAttr.type, isDeltaY);
		tValues = compressValues(tValues, tAttr.type, isDeltaT);

		// format the coordinates array according with the 
		// user-specified output format (x,y,time,semantic)		
		String parsedArray = "";
		// spatial attributes only
		if (outputFormat.equals(OutputFormat.SPATIAL)) {
			for (int i=0; i<numPts; i++) {
				parsedArray += "," + xValues[i] + "," + yValues[i]; 
			}
		}  
		// spatial-temporal attributes only	
		else if (outputFormat.equals(OutputFormat.SPATIAL_TEMPORAL)) {
			for(int i=0; i<numPts; i++){
				parsedArray += "," + xValues[i] + "," + yValues[i];
				parsedArray += "," + tValues[i];
			}
		} 
		// all attributes	
		else if (outputFormat.equals(OutputFormat.ALL)) {
			for (int i=0,k=0; i < numItems; i+=numAttr,k++) {
				String xVal="", yVal="", tVal="", semantic="";
				for (int j=i; j < (i+numAttr); j++) {
						 if (j == (i + xPos)){xVal = xValues[k];} // xVal
					else if (j == (i + yPos)){yVal = yValues[k];} // yVal
					else if (j == (i + tPos)){tVal = tValues[k];} // tVal
					else {semantic += "," + itemValues.get(j);}   // semanticVals
				}
				parsedArray += "," + xVal + "," + yVal; 
				parsedArray += "," + tVal + semantic;
			}
		}

		return parsedArray.substring(1);
	}
	
	/**
	 * Compress the array of values using Delta compression,
	 * only if the array is not already compressed. 
	 * <p>
	 * Return the compressed values as Integer  (value * 10^DECIMAL_PRECISION)
	 * 
	 * @param values The array of values to compress.
	 * @param type The type of the values to compress.
	 * @param isCompressed If the values are already compressed.
	 * 
	 * @return The array of values in delta-compression.
	 */
	private synchronized String[] compressValues(String[] values, String type, boolean isCompressed) {
		// if the values are not numbers, then do nothing
		if (!Keywords.isNumberType(type)) return values;
		// compress values, if not
		if (!isCompressed) { // $etype
			values = DeltaEncoder.deltaEncode(values);
		} 
		// if attribute is a Decimal, then convert and save values 
		// to integer with given precision ( value * 10^DECIMAL_PRECISION )
		if (type.equals(Keywords.DECIMAL.name()) ||
			type.equals(Keywords.DELTADECIMAL.name())) {
			Decimal round = Decimal.valueOf(
					Math.pow(10, dataFormat.getDecimalPrecision()));
			Decimal intVal;
			for (int i=0; i<values.length; i++) {
				intVal = Decimal.valueOf(values[i]).multiply(round);
				values[i] = ""+intVal.longValue();
			}	
		}		
		// if attribute is an Integer, get only the integer part
		if (type.equals(Keywords.INTEGER.name()) ||
			type.equals(Keywords.DELTAINTEGER.name())) {
			Decimal intVal;
			for (int i=0; i<values.length; i++) {
				intVal = Decimal.valueOf(values[i]);
				values[i] = ""+intVal.longValue();
			}	
		}
		
		return values;
	}
	
	/**
	 * Format DATETIME time attributes, convert the date attribute
	 * from the given pattern to a Integer format, i.e. number of 
	 * milliseconds passed since 01/Jan/1970.
	 * 
	 * @param tValues The list of attribute values to format.
	 * @param typeDescr the DATETIME("...") type description.
	 */
	private synchronized void formatDate(String[] tValues, final String typeDescr) {
		// get the current DATETIME pattern
		int s = typeDescr.indexOf("\"") + 1;
		int e = typeDescr.lastIndexOf("\"");
		final String datePattern = typeDescr
				.substring(s, e)
				.replaceAll("\\"+DATE_BOND, " ");
		Date newDate;
		for (int i=0; i<tValues.length; i++) {
			String oldDate = tValues[i];
			newDate = DateUtils.parseDate(oldDate, datePattern);
			tValues[i] = ""+newDate.getTime();
		}	
	}

	/**
	 * Fix backwards time-stamps (negative delta time-stamps).
	 * 
	 * @param deltaTimeValues List of time-stamp values.
	 */
	private synchronized void fixTimeStamps(String[] deltaTimeValues, String type) {
		// do only if time-stamp values are numbers
		if (Keywords.isNumberType(type)) {
			String prev, next;
			for (int i=0; i<deltaTimeValues.length-1; i++) {
				prev = deltaTimeValues[i];
				next = deltaTimeValues[i+1];
				// if delta is negative
				if (Double.parseDouble(next) < Double.parseDouble(prev)) {
					deltaTimeValues[i+1] = prev;
				}
			}			
		}
	}
	
	/**
	 * @return The metadata script generated during the
	 * data loading and parsing.
	 */
	public static String getMetadata(){
		return metadataScript;
	}

	/**
	 * @return The output format script generated during the
	 * data loading and parsing.
	 */
	public static String getOutputFormat(){
		return outputFormatScript;
	}
}
