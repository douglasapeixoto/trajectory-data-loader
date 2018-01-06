package traminer.parser.format;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import traminer.parser.analyzer.Keywords;
import traminer.parser.analyzer.Keywords.OutputFormat;
import traminer.parser.analyzer.Symbols;
import traminer.parser.analyzer.LexicalAnalyzer.Token;

/**
 * Object to manage the input Trajectory Data Description Format (TDDF) file.
 * </br>
 * Store the properties and parameter described in the TDDF.
 *  
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class DataFormat extends Format {
	// position of the trajectory ID attribute
	private int _idPos 	  = -1;
	// position of the trajectory coordinates attribute
	private int _coordPos = -1;
	// list of lines to ignore in the file
	private List<Integer> ignoredLinesList = null;
	// Auto ID prefix (if any)
	private String autoIdPrefix = null;
	// precision for decimal numbers
	private int decimalPrecision = DEFAULT_DECIMAL_PREC;
	// coordinates space
	private Keywords coordinateSystem = DEFAULT_COORD_SYSTEM;
	// output data format
	private OutputFormat outputFormat = DEFAULT_OUT_FORMAT;
	// delimiter of each object record
	private String recordsDelim = DEFAULT_RECORDS_DELIM;
	// The format of the _coordinates attribute (array)
	private ArrayFormat coordArrayFormat = null;

	/**
	 * Creates a new format entity for the given input data format
	 * tokens.
	 * <p>
	 * Note: This constructor assumes that the Input Data Format
	 * has already passed the syntax analysis.
	 * 
	 * @param tokensList List of tokens from the lexical analyzer,
	 * representing the input data format.
	 * 
	 * @see LexicalAnalyzer
	 * @see SyntaxAnalyzer
	 */
	public DataFormat(List<Token> tokensList) {
		if (tokensList == null) {
			throw new NullPointerException("Tokens list for data format "
					+ "construction must not be null.");
		}
		if (tokensList.isEmpty()) {
			throw new NullPointerException("Tokens list for data format "
					+ "construction must not be empty.");
		}
		// create object
		create(tokensList);
	}

	/**
	 * Creates this object for the given input data format tokens.
	 * 
	 * @param tokensList List of tokens from the lexical analyzer,
	 * representing the input data format.
	 */
	private void create(List<Token> tokensList) {
		// process file tokens
		String name, type, value, delim;
		for (int i=0; i<tokensList.size();) {
			Token token = tokensList.get(i);
			name = token.value;
			// is attribute
			if (token.lexSymbol.equals(Symbols.AttributeSymbol)) {
				type  = tokensList.get(i+1).value;
				delim = tokensList.get(i+2).value;
				this.addAttribute(name, type, delim);
				// format the coordinates array
				if (token.value.equals(Keywords._COORDINATES.name())) {
					coordArrayFormat = new ArrayFormat(type);
				}
				i += (NUM_PARAMS_ATTR+1);
			} 
			// is command
			else {
				value = tokensList.get(i+1).value;
				this.addCommand(name, value);
				i += (NUM_PARAMS_CMD + 1);
			}
		}
	}
	
	/**
	 * Add a command to the format file entity.
	 * A command is defined by the command's name, and value.
	 *
	 * @param cmdName  Command's name.
	 * @param cmdValue Command's value.
	 */
	private void addCommand(String cmdName, String cmdValue) {
		// process command declaration
		Keywords cmdKeyword = Keywords.lookup(cmdName);
		
		switch (cmdKeyword) {
			case _IGNORE_ATTR:
				addAttribute(cmdName, Keywords._IGNORE_ATTR.name(), cmdValue);
				break;
				
			case _IGNORE_LINES:
				addIgnoredLines(cmdValue);
				break;
				
			case _COORD_SYSTEM:
				coordinateSystem = Keywords.lookup(cmdValue);
				break;
				
			case _AUTO_ID:
				autoIdPrefix = cmdValue;
				break;
				
			case _RECORDS_DELIM:
				setRecordsDelim(cmdValue);
				break;
				
			case _OUTPUT_FORMAT:
				outputFormat = OutputFormat.valueOf(cmdValue);
				break;
				
			case _DECIMAL_PREC:
				decimalPrecision = Integer.parseInt(cmdValue);
				break;
				
			default: break;
		}
	}

	/**
	 * Add the the lines in the input file to ignore.
	 *  
	 * @param ignoreLinesValue The value of the _IGNORE_LINES command,
	 * containing the lines of the input file to ignore. 
	 */
	private void addIgnoredLines(String ignoreLinesValue) {
		ignoredLinesList = new ArrayList<Integer>();
		StringTokenizer tokenizer = new StringTokenizer(
				ignoreLinesValue, "[],");
		final String intervalRegex = "[0-9]+\\-[0-9]+";
		while (tokenizer.hasMoreTokens()) {
			String item = tokenizer.nextToken();
			// interval
			if (item.matches(intervalRegex)) {
				String[] interval = item.split("-");
				int begin = Integer.parseInt(interval[0]) - 1;
				int end   = Integer.parseInt(interval[1]) - 1;
				for (int i=begin; i<=end; i++) {
					ignoredLinesList.add(i);
				}
			} else {
				ignoredLinesList.add(Integer.parseInt(item)-1);
			}
		}
	}
	
	@Override
	public int numValidAttributes() {
		int num = super.numValidAttributes() + (isAutoId() ? 1 : 0);
		return num;
	}
	
	/**
	 * @return The object containing the configurations 
	 * of the coordinates array.
	 */
	public ArrayFormat getCoordinatesArrayFormat() {
		return this.coordArrayFormat;
	}

	/**
	 * @return The input data space (e.g. CARTESIAN, POLAR).
	 */
	public Keywords getCoordinateSystem() {
		return coordinateSystem;
	}

	/**
	 * @return The prefix of the ID attribute. In case 
	 * the _AUTO_ID command has been provided.
	 */
	public String getIdPrefix() {
		return autoIdPrefix;
	}

	/**
	 * @return Precision for decimal numbers. 
	 * Number of decimal points to round up.
	 */
	public int getDecimalPrecision(){
		return decimalPrecision;
	}
	
	/**
	 * @return The output data format.
	 */
	public OutputFormat getOutputFormat() {
		return outputFormat;
	}

	/**
	 * @return True if the _AUTO_ID command has been provided.
	 */
	public boolean isAutoId() {
		return (autoIdPrefix != null);
	}

	/**
	 * @return List of the lines in the input file to ignore.
	 * Lines index start from zero = 0.
	 */
	public List<Integer> getIgnoredLinesList() {
		return ignoredLinesList;
	}
	
	/**
	 * @return True if there is any lines to ignore in the input data.
	 */
	public boolean hasLinesToIgnore() {
		return (ignoredLinesList == null ? false : !ignoredLinesList.isEmpty());
	}

	/**
	 * The delimiter of the data records in the input dataset.
	 * <p>
	 * This is the string/char use as delimiter for every record
	 * in the input file (ex. line breaks, line space, chars, etc.).
	 * 
	 * @return The delimiter of the data records in the input dataset.
	 */
	public String getRecordsDelim() {
		return recordsDelim;
	}

	/**
	 * Set the delimiter of the data records in the input dataset.
	 * <p>
	 * This is the string/char use as delimiter for every record
	 * in the input file (ex. line breaks, line space, chars, etc.)
	 * <p>
	 * For TRAJECTORY records, this is the delimiter of each 
	 * trajectory object. If there is one trajectory per file, 
	 * then records delimiter = 'EOF'.
	 * 
	 * @param delim The delimiter of the data records in the input dataset.
	 */
	public void setRecordsDelim(String delim) {
		if (delim == null) {
			throw new NullPointerException(
					"Records delimiter must not be null.");
		}
		if (delim.equals(Keywords.LS.name())) {
			delim = LINE_SPACE;
		} else 
		if (delim.equals(Keywords.LN.name())) {
			delim = LINE_BREAK;
		}
		recordsDelim = delim;
	}
	
	/**
	 * The position of the trajectory '_ID' attribute
	 * on each line record of the input file. Attribute
	 * position starts from zero = 0.
	 * 
	 * @return the position of the '_ID' attribute, or
	 * -1 if no '_ID' attribute has been specified.
	 */
	public int getIdAttrIndex() {
		if (_idPos != -1) return _idPos; 
		for (int i=0; i<numAttributes(); i++) {
			AttributeEntry line = getAttribute(i);
			if (line.name.equals(Keywords._ID.name())) {
				_idPos = i;
				return _idPos;
			}
		}
		// no '_ID' attribute found in the format file
		return _idPos; //-1
	}
	
	/**
	 * The position of the trajectory '_COORDINATES' attribute
	 * on each line record of the input file. Attribute
	 * position starts from zero = 0.
	 * 
	 * @return the position of the '_COORDINATES' attribute, or
	 * -1 if no '_COORDINATES' attribute has been specified.
	 */
	public int getCoordinatesAttrIndex() {
		if (_coordPos != -1) return _coordPos; 
		for (int i=0; i<numAttributes(); i++) {
			AttributeEntry line = getAttribute(i);
			if (line.name.equals(Keywords._COORDINATES.name())) {
				_coordPos = i;
				return _coordPos;
			}
		}
		// no '_COORDINATES' attribute found in the format file
		return _coordPos; //-1
	}
}
