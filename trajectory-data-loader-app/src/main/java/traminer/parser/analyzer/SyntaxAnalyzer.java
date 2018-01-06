package traminer.parser.analyzer;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import traminer.parser.ParserException;
import traminer.parser.ParserInterface;
import traminer.parser.analyzer.LexicalAnalyzer.Token;

/**
 * Validates the Syntax of the Input Data Format. Check whether 
 * the symbols in the source file form a valid input format. 
 * <p>
 * Left-Right (LR) syntax analysis.
 * 
 * @author douglasapeixoto
 */
@SuppressWarnings("serial")
public class SyntaxAnalyzer implements ParserInterface {
	// System log 
	private static Logger log = Logger.getLogger(SyntaxAnalyzer.class);
	// Regular expression for type general format
	private final String typeRegex = 
			Keywords.DATETIME.name() + "\\[\".+\"\\]|" + 
			Keywords.ARRAY.name() + "\\(.+\\)|" + 
			"\\w+"; // add other types here?
	// Regular expression for delimiters
	private final String delimRegex = "\\W{1}|LN|LS|EOF";
	
	/**
	 * Check whether the tokens (from the LexicalAnalyzer) 
	 * form a valid input format. 
	 * <p>
	 * Check whether the attributes and commands declaration 
	 * are correct.
	 * 
	 * @param sourceTokens Tokens list form the LexicalAnalizer.
	 * 
	 * @return True if the format syntax is correct.
	 */
	public boolean analyze(List<Token> sourceTokens) {
		if (sourceTokens == null) {
			log.error("Source tokens list for syntax analysis "
					+ "must not be null.", new ParserException());
			return false;
		}
		if (sourceTokens.isEmpty()) {
			log.error("Source tokens list for syntax analysis "
					+ "must not be empty.", new ParserException());
			return false;
		}
		List<Token> lineCommand = new ArrayList<Token>();
		boolean result = true;
		for (Token token : sourceTokens) {
			// check the syntax of the tokens on each line
			if (token.lexSymbol.equals(Symbols.EndCmdSymbol)) {
				// check if this line is a command or attribute declaration
				Token firstTkn = lineCommand.get(0);
				if (firstTkn.lexSymbol.equals(Symbols.CommandSymbol)) {
					result = commandAnalyzerLR(lineCommand);
				}
				// otherwise it is an attribute declaration
				else {
					result = attributeAnalyzerLR(lineCommand);
				}
				if (!result) return false;
				lineCommand = new ArrayList<Token>();
			} else {
				lineCommand.add(token);
			}
		}
		
		// no syntax error
		return true;
	}

	/**
	 * Analyze the tokens in a line containing an Attribute declaration.
	 * 
	 * @param lineLexTokens The tokens in an attribute declaration line.
	 * 
	 * @return True if there is no syntax error.
	 */
	private boolean attributeAnalyzerLR(List<Token> lineLexTokens) {
		Token attrTkn  = lineLexTokens.get(0);
		// attributes declaration must have NUM_PARAM_ATTR parameters
		if (lineLexTokens.size() != NUM_PARAMS_ATTR) {
			log.error("Attribute declaration '" + attrTkn.value + 
					"' must contain " + NUM_PARAMS_ATTR + " parameters.", 
					new ParserException());
			return false;
		}
		// attributes name can only contain characters, numbers, 
		// and underline. And cannot start with numbers.
		if (!attrTkn.value.matches("[^0-9][a-zA-Z_0-9]+")) {
			log.error("Attribute declaration '" + attrTkn.value + 
					"' must only contain characters, numbers and underline."
					+ " And must not start with numbers.", 
					new ParserException());
			return false;
		}
		
		Token typeTkn  = lineLexTokens.get(1);
		Token delimTkn = lineLexTokens.get(2);
		String lineLex = attrTkn.lexSymbol + typeTkn.lexSymbol + delimTkn.lexSymbol;
		String regex = ""; // regular expression

		// Check the syntax of this attribute declaration 
		Keywords attrKeyword = Keywords.lookup(attrTkn.value);
		
		switch (attrKeyword) {
			case _ID: 
				// regular expression of ID attribute 
				// "\\$attr\\$type\\$delim"
				regex = "\\"+Symbols.AttributeSymbol + 
				        "\\"+Symbols.TypeSymbol +
					    "\\"+Symbols.DelimiterSymbol;
				// ID attribute must be of a basic type
				if (!lineLex.matches(regex)) {
					log.error("Attribute '" + attrTkn.value + "' must be "
							+ "of a basic type.", new ParserException());
					return false;
				};
				break;
			
			case _COORDINATES:
				// regular expression of Coordinate attribute
				// "\\$attr\\$array\\$delim"
				regex = "\\"+Symbols.AttributeSymbol + 
				        "\\"+Symbols.ArrayTypeSymbol +
					    "\\"+Symbols.DelimiterSymbol;
				// Coordinate attribute must be an Array type
				if (!lineLex.matches(regex)) {
					log.error("Attribute '" + attrTkn.value + "' must be of an '" 
							+ Keywords.ARRAY + "' type.", new ParserException());
					return false;
				};
				break;

			default:
				// default format for attributes
				// "\\$attr(\\$type|\\$array|\\$dtype)\\$delim"
				regex = "\\"+Symbols.AttributeSymbol + 
					   "(\\"+Symbols.TypeSymbol + "|" + 
				        "\\"+Symbols.ArrayTypeSymbol + "|" + 
				        "\\"+Symbols.DateTypeSymbol +")" +
					    "\\"+Symbols.DelimiterSymbol;
				if (!lineLex.matches(regex)) {
					log.error("Attribute declaration '" + attrTkn.value + 
							"' is in a wrong format.", new ParserException());
					return false;
				} 
		}
		
		/* Analyze attribute's type */
		
		// If this attribute is an ARRAY type, then analyze array syntax
		if (typeTkn.lexSymbol.equals(Symbols.ArrayTypeSymbol)) {
			if (!arrayAnalyzerLR(typeTkn.value)) {
				return false;
			}
		}
		
		// If this attribute is an DATETIME type, then analyze DateTime syntax
		if (typeTkn.lexSymbol.equals(Symbols.DateTypeSymbol)) {
			if (!dateTypeAnalyzerLR(typeTkn.value)) {
				return false;
			}
		}
		
		// no syntax error in this attribute declaration
		return true;
	}

	/**
	 * Analyze the tokens in a line containing a Command declaration.
	 * 
	 * @param lineLexTokens The tokens in a command declaration line.
	 * 
	 * @return True if there is no syntax error. 
	 */
	private boolean commandAnalyzerLR(List<Token> lineLexTokens) {
		Token cmdToken = lineLexTokens.get(0);
		// command declaration must have NUM_PARAM_CMD parameters
		if (lineLexTokens.size()!= NUM_PARAMS_CMD) {
			log.error("Command '" + cmdToken.value + "' must contain " 
					+ NUM_PARAMS_CMD + " parameters.", new ParserException());
			return false;
		}
		Token valToken = lineLexTokens.get(1);
		String lineLex = cmdToken.lexSymbol + 
				         valToken.lexSymbol;
		String regex = ""; // regular expression

		// Check the syntax of this command declaration 
		Keywords cmdKeyword = Keywords.lookup(cmdToken.value);
		
		// default error message
		final String errMsg = "Command declaration '" + cmdToken.value + 
		   			  	"' is in a wrong format.";
		
		switch(cmdKeyword){
			case _COORD_SYSTEM:
				// "\\$cmd$space"
				regex = "\\"+Symbols.CommandSymbol +
					    "\\"+Symbols.CoordinateSysSymbol;
				if (!lineLex.matches(regex)) {
					log.error(errMsg + " Check coordinates system types "
							+ "definition.", new ParserException());
					return false;
				}
				break;
				
			case _SPATIAL_DIM:
				// "\\$cmd[1-3]"
				regex = "\\"+Symbols.CommandSymbol + "[1-3]";
				String dimCmd = cmdToken.lexSymbol + valToken.value;
				if (!dimCmd.matches(regex)) {
					log.error(errMsg + " Dimensions must be 1, 2 or 3.", 
							new ParserException());
					return false;
				}
				break;

			case _IGNORE_LINES:
				// "\\$cmd[1-9,15,18]"
				regex = "\\"+Symbols.CommandSymbol + 
				        "\\[[^,]{0}(,{0,1}[0-9]|,{0,1}[0-9]\\-[0-9])+\\]";
				String ignoreCmd = cmdToken.lexSymbol + valToken.value;
				if (!ignoreCmd.matches(regex)) {
					log.error(errMsg, new ParserException());
					return false;
				}
				break;
					
			case _AUTO_ID:
				// "\\$cmd\\$str"
				regex = "\\"+Symbols.CommandSymbol +
					    "\\"+Symbols.StringSymbol;
				if (!lineLex.matches(regex)) {
					log.error(errMsg + " Check ID prefix declaration.", 
							new ParserException());
					return false;
				}
				break;
				
			case _IGNORE_ATTR:
				// "\\$cmd\\$delim"
				regex = "\\"+Symbols.CommandSymbol +
					    "\\"+Symbols.DelimiterSymbol;
				if (!lineLex.matches(regex)) {
					log.error(errMsg + "  Check attribute delimiter.", 
							new ParserException());
					return false;
				}
				break;
				
			case _RECORDS_DELIM:	
				// "\\$cmd\\$delim"
				regex = "\\"+Symbols.CommandSymbol +
					    "\\"+Symbols.DelimiterSymbol;
				if (!lineLex.matches(regex)) {
					log.error(errMsg + " Check records delimiter.", 
							new ParserException());
					return false;
				}
				break;
			
			case _DECIMAL_PREC:
				// "\\$cmd\\d+"
				regex = "\\"+Symbols.CommandSymbol + "\\d+";
				String precisionCmd = cmdToken.lexSymbol + valToken.value;
				if (!precisionCmd.matches(regex)) {
					log.error(errMsg + " Decimal precision must be an "
							+ "integer number.", new ParserException());
					return false;
				}
				break;
				
			default:
				// default format for commands
				// "\\$cmd(\\$str|\\$delim)"
				regex = "\\"+Symbols.CommandSymbol + 
					   "(\\"+Symbols.StringSymbol + "|" +
					    "\\"+Symbols.DelimiterSymbol + ")";
				if (!lineLex.matches(regex)) {
					log.error(errMsg, new ParserException());
					return false;
				}
		}
		
		// no syntax error in this command declaration
		return true;
	}

	/**
	 * Analyze the tokens in an ARRAY type declaration.
	 * 
	 * @param arrayDescr The description of an ARRAY type, 
	 * as specified in the input format file.
	 * 
	 * @return True if there is no syntax error.
	 */
	private boolean arrayAnalyzerLR(String arrayDescr) {
		// general format for array declaration
		final String arrayRegex = Keywords.ARRAY.name() + 
				"\\((?>\\s*\\w+\\s+("+typeRegex+")\\s+("+delimRegex+")\\s*)+\\)";
		if (!arrayDescr.matches(arrayRegex)) {
			log.error("Array declaration '" + arrayDescr + "' is in a wrong format.",
					new ParserException());
			return false;
		}

		// gets array attribute tokens, and validate the array format
		List<Token> tokensList = new LexicalAnalyzer()
				.analyzeArray(arrayDescr);

		// attributes must have NUM_PARAMS_ATTR parameters
		if (tokensList.size() % NUM_PARAMS_ATTR != 0) {
			log.error("Array attributes declaration '" + arrayDescr + 
					"' is in a wrong format. Number of parameters does not match.", 
					new ParserException());
			return false;
		}

		// general format for array spatial attributes (x,y,lat,lon)
		// "\\$attr(\\$type|\\$etype)\\$delim"
		String regex = "\\"+Symbols.AttributeSymbol +
			     	  "(\\"+Symbols.TypeSymbol + "|" +
			     	   "\\"+Symbols.DeltaTypeSymbol + ")" +
			     	   "\\"+Symbols.DelimiterSymbol;

		Token tknName, tknType, tknDelim;
		for (int i=0; i<tokensList.size(); i+=NUM_PARAMS_ATTR) {
			tknName  = tokensList.get(i);
			tknType  = tokensList.get(i+1);
			tknDelim = tokensList.get(i+2);
			String attrFormat = tknName.lexSymbol +
								tknType.lexSymbol + 
								tknDelim.lexSymbol;					     		
			
			// Check the syntax of this array attribute declaration 
			Keywords attrKeyword = Keywords.lookup(tknName.value);

			switch (attrKeyword) {
				case _X:   break;
				case _Y:   break;
				case _LON: break;
				case _LAT: break;
				
				case _TIME: 
					// general format for array temporal attributes
					// "\\$attr(\\$type|\\$etype|\\$dtype)\\$delim"
					regex = "\\"+Symbols.AttributeSymbol +
				     	   "(\\"+Symbols.TypeSymbol + "|" +
				     	    "\\"+Symbols.DeltaTypeSymbol + "|" +
				     	    "\\"+Symbols.DateTypeSymbol + ")" +
				     	    "\\"+Symbols.DelimiterSymbol;	
				break; 
				
				default: 
					// general format for array semantic attributes
					// "\\$attr(\\$type|\\$dtype)\\$delim"
					regex = "\\"+Symbols.AttributeSymbol +
			     	   	   "(\\"+Symbols.TypeSymbol + "|" +
			     	   		"\\"+Symbols.DateTypeSymbol + ")" +
			     	   		"\\"+Symbols.DelimiterSymbol;	
			}
			
			if (!attrFormat.matches(regex)) {
				log.error("ARRAY type declaration '" + tknName.value 
						+ "' is in a wrong format.", new ParserException());
				return false;
			}
			
			// If this attribute is an DATETIME type, then analyze DateTime syntax
			if (tknType.lexSymbol.equals(Symbols.DateTypeSymbol)) {
				if (!dateTypeAnalyzerLR(tknType.value)) {
					return false;
				}
			}
		}

		// no syntax error in this array declaration
		return true;
	}
		
	/**
	 * Analyze the syntax of a DATETIME type declaration.
	 * 
	 * @param dateTimeDescr The description of the DATETIME type,
	 * as specified in the input format file.
	 * 
	 * @return True if there is no syntax error.
	 */
	private boolean dateTypeAnalyzerLR(String dateTimeDescr) {
		// general format for DATETIME declaration
		final String dateTimeRegex = Keywords.DATETIME.name() + "\\[\\\".*\\\"\\]";
		if (!dateTimeDescr.matches(dateTimeRegex)) {
			log.error("DATETIME type declaration '" + dateTimeDescr + 
					"' is in a wrong format.", new ParserException());
			return false;
		}

		// get and validate the DATETIME pattern
		try {	
			int s = dateTimeDescr.indexOf("[\"") + 1;
			int e = dateTimeDescr.lastIndexOf("\"]");
			final String pattern = dateTimeDescr.substring(s, e);
			DateTimeFormatter.ofPattern(pattern);		
		} catch (Exception e) {
			log.error("Pattern of DATETIME declaration '" + dateTimeDescr + 
					"' is invalid.", new ParserException());
			return false;
		}
		
		// no syntax error in this type declaration
		return true;
	}
}
