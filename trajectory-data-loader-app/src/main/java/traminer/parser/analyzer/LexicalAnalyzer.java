package traminer.parser.analyzer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.Logger;

import traminer.parser.ParserException;
import traminer.parser.ParserInterface;
import traminer.parser.analyzer.Symbols.LexicalSymbol;

/**
 * Process the source file (InputDataFormat), token by token, 
 * and classify each token by its lexical symbol. 
 * <p>
 * Returns a symbolic representation (lexical symbols) of the source file.
 * 
 * @author douglasapeixoto
 */
@SuppressWarnings("serial")
public class LexicalAnalyzer implements ParserInterface {
	// System log 
 	private static Logger log = Logger.getLogger(LexicalAnalyzer.class);
 	
	/**
	 * A token in the InputFormat source file.
	 */	
	public class Token {
		/** Token's name/value */
		public final String value;
		/** Token lexical symbol */
		public final String lexSymbol;

		/**
		 * A token in the InputFormat source file.
		 * 
		 * @param value  Token's name/value.
		 * @param symbol Token lexical symbol.
		 */
		public Token(String value, String symbol) {
			this.value = value;
			this.lexSymbol = symbol;
		}
		
		@Override
		public String toString(){return (value + lexSymbol);}
	}
	
	/**
	 * Read and perform the lexical analysis of the input data
	 * format file (file lines). Identify file tokens and classify
	 * them by lexical symbols.
	 * 
	 * @param fileLines List with the data format file lines.
	 * One file line per list item.
	 * 
	 * @return Return the list of tokens in the file. Each
	 * token is composed by the token's [name] and [lexical symbol].
	 * 
	 * @throws NullPointerException If the input data format is null.
	 * @throws IllegalArgumentException If the input data format is empty.
	 */
	public List<Token> analyzeFile(List<String> fileLines) 
			throws NullPointerException, IllegalArgumentException {
		if (fileLines == null) {
			log.error("Input data format must not be null.");
			throw new NullPointerException(
					"Input data format must not be null.");
		}
		if (fileLines.isEmpty()) {
			log.error("Input data format must not be empty.");
			throw new IllegalArgumentException(
					"Input data format must not be empty.");
		}
		
		// pre-process the input file
		fileLines = preProcessFile(fileLines);

		// lexical analysis of every file line
		List<Token> tokensList = new ArrayList<Token>();
		Scanner scanner = null;		
		for (String line : fileLines) {		
			scanner = new Scanner(line);
			// first token in this line (attribute or command)
			String token = scanner.next();	
			tokensList.add(analyzeToken(token, true));
			// remainder tokens in this line (type, value, delim)
			while (scanner.hasNext()) {
				token = scanner.next();				
				// read the whole ARRAY type declaration as one token
				if (token.startsWith(Keywords.ARRAY.name()+"(")) {
					String a = scanner.findWithinHorizon(".*\\)", line.length());
					token += " " + (a != null ? a : scanner.nextLine());				
				}
				// read the whole DATETIME type declaration as one token
				if (token.startsWith(Keywords.DATETIME.name()+"[")) {
					String a = scanner.findWithinHorizon(".*\\\"\\]", line.length());
					token += (a != null ? a : scanner.next());
				}
				tokensList.add(analyzeToken(token, false));
			}
			// mark end line
			tokensList.add(new Token("end", Symbols.EndCmdSymbol));
		}
		scanner.close();
		
		return tokensList;
	}

	/**
	 * Perform the lexical analysis of an Array attributes. 
	 * Identify tokens and classify them by lexical symbols.
	 * 
	 * @param arrayDescr The description of an array type 
	 * as specified in the input format file.
	 * 
	 * @return Return the list of tokens in the Array. Each
	 * token is composed by the token's name and lexical symbol.
	 * 
	 * @throws NullPointerException If the array format is null.
	 * @throws IllegalArgumentException If the array format is empty.
	 */
	public List<Token> analyzeArray(String arrayDescr) 
			throws NullPointerException, IllegalArgumentException {
		if (arrayDescr == null) {
			log.error("Array format must not be null.");
			throw new NullPointerException(
					"Array format must not be null.");
		}
		if (arrayDescr.isEmpty()) {
			log.error("Array format must not be empty.");
			throw new IllegalArgumentException(
					"Array format must not be empty.");
		}
	
		// lexical analysis of every array attribute
		List<Token> tokensList = new ArrayList<Token>();
		arrayDescr = arrayDescr.replaceAll(Keywords.ARRAY.name()+"\\(", "");
		Scanner scanner = new Scanner(arrayDescr);
		String token;
		while (scanner.hasNext()) {
			// first token is the attribute's name
			token = scanner.next();			
			tokensList.add(analyzeToken(token, true));
			// attribute's type
			token = scanner.next();
			tokensList.add(analyzeToken(token, false));
			// attribute's delim
			token = scanner.next().replace(")", "");			
			tokensList.add(analyzeToken(token, false));		
		}
		scanner.close();

		return tokensList;
	}

	/**
	 * Analyze and classify the given line token. Check 
	 * the lexical symbol of the given token.
	 * 
	 * @param tokenValue The token value as in the declaration.
	 * @param firstToken True if this is the first token in 
	 * the line. First tokens are either a command or an
	 * attribute declaration.
	 * 
	 * @return A Token object containing the token's
	 * name and lexical symbol.
	 */
	public Token analyzeToken(String tokenValue, boolean firstToken) 
			throws NullPointerException {
		if (tokenValue == null) {
			log.error("Token value for analysis must not be null.");
			throw new NullPointerException(
					"Token value for analysis must not be null.");
		}
		
		// general string by default
		String lexSymbol = Symbols.StringSymbol;
		// first token in the line is set as attribute by default, 
		// if it is not predefined keyword, then it must be an 
		// attribute declaration
		if (firstToken) { 
			lexSymbol = Symbols.AttributeSymbol; 
		}
				
		// check if the token is a predefined keyword,
		// then classify it accordingly
		for (Keywords keyword : KEYWORDS_LIST) {
			if (tokenValue.startsWith(Keywords.ARRAY.name()+"(")) {
				lexSymbol = Symbols.ArrayTypeSymbol;
				return new Token(tokenValue, lexSymbol);
			}
			if (tokenValue.startsWith(Keywords.DATETIME.name()+"[")) {
				lexSymbol = Symbols.DateTypeSymbol;			
				return new Token(tokenValue, lexSymbol);
			}
			// delimiter (non-word char)
			if (tokenValue.matches("\\W{1}")) {
				lexSymbol = Symbols.DelimiterSymbol; 
				return new Token(tokenValue, lexSymbol);
			}
			try {
				if (tokenValue.equalsIgnoreCase(keyword.name())) {
					Field field = Keywords.class.getDeclaredField(keyword.name());
					lexSymbol = field.getAnnotation(LexicalSymbol.class).symbol();
					return new Token(tokenValue, lexSymbol);
				}
			} catch (NoSuchFieldException e) {
				log.error("Error identifying token '"+tokenValue+"'.", 
						new ParserException(e));
			} catch (SecurityException e) {
				log.error("Error identifying token '"+tokenValue+"'.", 
						new ParserException(e));
			}
		}
		
		return new Token(tokenValue, lexSymbol);
	}

	/**
	 * Pre-process file lines. Remove comments, empty lines,
	 * and organize ARRAY and DATETIME declaration.
	 * 
	 * @param fileLines The lines of the input file to pre-process.
	 * @return Return the file lines pre-processed.
	 */
	private List<String> preProcessFile(List<String> fileLines) {
		List<String> preProcessedLines = new ArrayList<String>();
		
		// pre-process all lines
		for (int i=0; i<fileLines.size(); i++) {
			String line = fileLines.get(i);
			
			/** Ignore comments and empty lines **/
			if (line.startsWith(Keywords.COMMENT_CHAR)) continue;
			if (line.trim().length() == 0) continue;	
						
			// if this line contains an ARRAY declaration
			if (line.contains(Keywords.ARRAY.name()+"(")) {
				String arrayLine = line;
				int endIndex = line.lastIndexOf(")");
				while (endIndex == -1 && i < fileLines.size()-1) {
					line = fileLines.get(++i);
					if (line.startsWith(Keywords.COMMENT_CHAR)) continue;
					arrayLine += " " + line;
					endIndex = line.lastIndexOf(")");
				}
				line = arrayLine;
			}
			
			// if this line contains a DATETIME declaration
			if (line.contains(Keywords.DATETIME.name()+"[")) {
				// replace spaces in the DATETIME pattern declaration (if any)
				int sIndex = line.indexOf("[\"") + 1;
				int eIndex = line.lastIndexOf("\"]");
				String newDate = line.substring(sIndex, eIndex).replaceAll("\\s", DATE_BOND);
				line = line.substring(0, sIndex) + newDate +
					   line.substring(eIndex, line.length());			
			}
			
			// replace multiple blank spaces/tab by a single space
			line = line.replaceAll("\\s+", " ");
			preProcessedLines.add(line);
		}
		
		return preProcessedLines;
	}
}
