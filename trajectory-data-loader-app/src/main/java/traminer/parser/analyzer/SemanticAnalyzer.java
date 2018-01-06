package traminer.parser.analyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import traminer.parser.ParserException;
import traminer.parser.ParserInterface;
import traminer.parser.analyzer.LexicalAnalyzer.Token;
import traminer.parser.format.ArrayFormat;

/**
 * Validates the Semantic of the Input Data Format.
 * Check whether the commands and attributes in the 
 * source file form a valid input format.
 * 
 * @author douglasapeixoto
 */
@SuppressWarnings("serial")
public class SemanticAnalyzer implements ParserInterface {
	// Attributes to validate 
    private List<String> attrNameList = new ArrayList<String>();
    // Command values to validate
    private String coordTypeDescr 	= "";
    private String ignoreLinesValue = "";
    private String autoIdValue 		= null;
    
    // System Log
 	private static Logger log = 
 			Logger.getLogger(SemanticAnalyzer.class);

 	/**
 	 * Runs the semantic analysis of the Input Data Format.
 	 * 
 	 * @param sourceTokens Tokens from the LexicalAnalyzer.
 	 * @return Returns the validation result.
 	 */
	public boolean analyze(List<Token> sourceTokens) {
		if (sourceTokens == null) {
			log.error("Source tokens list for semantic analysis "
					+ "must not be null.", new ParserException());
			return false;
		}
		if (sourceTokens.isEmpty()) {
			log.error("Source tokens list for semantic analysis "
					+ "must not be empty.", new ParserException());
			return false;
		}
		// process file tokens
		String attrName, attrType;
		String cmdName, cmdValue;
		for (int i=0; i<sourceTokens.size();) {
			Token token = sourceTokens.get(i);
			//name = token.name;
			// is attribute
			if (token.lexSymbol.equals(Symbols.AttributeSymbol)) {
				attrName = token.value;
				attrType = sourceTokens.get(i+1).value;
				attrNameList.add(attrName);
				// coordinates array
				if (attrName.equals(Keywords._COORDINATES.name())) {
					coordTypeDescr = attrType;
				}
				i += (NUM_PARAMS_ATTR+1);
			} 
			// is command
			else {
				cmdName  = token.value;
				cmdValue = sourceTokens.get(i+1).value;
				if (cmdName.equals(Keywords._IGNORE_LINES.name())) {
					ignoreLinesValue = cmdValue;
				} else 
				if (cmdName.equals(Keywords._AUTO_ID.name())) {
					autoIdValue = cmdValue;
				}
				i += (NUM_PARAMS_CMD+1);
			}
		}	
		
		// run semantic analysis
		boolean result = true;
		if (!validateMandatoryFields()) result = false;
		if (!validateAttrUniqueness())  result = false;
		if (!validateIgnoreLinesCmd())  result = false;
		// include more semantic validations here if needed 
// TODO: if Geographic -> LAT/LON, if Cartesian -> X/Y	
// TODO: types of attributes validation should be done here?		
		return result;
	}

	/**
	 * Check whether the mandatory fields are provided.
	 * 
	 * @return True if the validation passed.
	 */
	private boolean validateMandatoryFields() {
		boolean result = true;
		// check if ID and COORDINATES are provided
		boolean hasID 	 = false;
		boolean isAutoID = false;
		boolean hasCoord = false;
		for (String name : attrNameList) {
			if (autoIdValue != null) isAutoID = true;
			if (name.equals(Keywords._ID.name())) hasID = true;
			if (name.equals(Keywords._COORDINATES.name())) hasCoord = true;
		}
		if (!hasID && !isAutoID) {
			log.error("Data format must contain the '_ID' attribute field.", 
					new ParserException());
			result = false;
		}
		if (hasID && isAutoID) {
			log.error("Attribute '_ID' declaration and command '_AUTO_ID' cannot be "
					+ "provided together in the Data Format.", new ParserException());
			result = false;
		}
		if (!hasCoord) {
			log.error("Data format must contain the '_COORDINATES' attribute field.",
					new ParserException());
			return false;
		}
	
		// check if the coordinate attributes X, Y and TIME were provided
		ArrayFormat coordArrayFormat = new ArrayFormat(coordTypeDescr);
		if (coordArrayFormat.getXAttrIndex() == -1) {
			log.error("Coordinates array type desclaration must contain either "
					+ "the '_X' or '_LON' attribute field.", new ParserException());
			result = false;
		}
		if (coordArrayFormat.getYAttrIndex() == -1) {
			log.error("Coordinates array type desclaration must contain either "
					+ "the '_Y' or '_LAT' attribute field.", new ParserException());
			result = false;
		}
		if (coordArrayFormat.getTimeAttrIndex() == -1) {
			log.error("Coordinates array type desclaration must contain "
					+ "the '_TIME' attribute field.", new ParserException());
			result = false;
		}
		
		return result;
	}
	
	/**
	 * Check whether every attribute provided is unique.
	 * 
	 * @return True if the validation passed.
	 */
	private boolean validateAttrUniqueness() {
		boolean result = true;
		String name_i, name_j;
		int attrCount = attrNameList.size();
		for (int i=0; i<attrCount; i++) {
			name_i = attrNameList.get(i);
			for (int j=i+1; j<attrCount; j++) {
				name_j = attrNameList.get(j);
				if (name_i.equalsIgnoreCase(name_j)) {
					String errMsg = "Attribute '"+ name_i +"' declaration must be unique.";
					log.error(errMsg, new ParserException(errMsg));
					result = false;
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Check the intervals of the _IGNORE_LINES command.
	 * 
	 * @return True if the validation passed.
	 */
	private boolean validateIgnoreLinesCmd() {
		boolean result = true;

		StringTokenizer tokenizer = new StringTokenizer(ignoreLinesValue, "[],");
		final String intervalRegex = "[0-9]+\\-[0-9]+";
		
		while (tokenizer.hasMoreTokens()) {
			String item = tokenizer.nextToken();
			// interval
			if (item.matches(intervalRegex)) {
				String [] interval = item.split("-");
				int begin = Integer.parseInt(interval[0]) - 1;
				int end   = Integer.parseInt(interval[1]) - 1;
				
				if (begin > end) {
					String errMsg = "Ignore lines command interval '" + ignoreLinesValue
							+ "' is in a wrong format: [" +begin+ "-" +end+"]";
					log.error(errMsg, new ParserException(errMsg));
					result = false;
				}
			}
		}
				
		return result;
	}
}
