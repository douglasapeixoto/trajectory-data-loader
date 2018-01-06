package traminer.parser;

import java.io.Serializable;

import traminer.parser.analyzer.Keywords;
import traminer.parser.analyzer.Keywords.OutputDatabase;
import traminer.parser.analyzer.Keywords.OutputFormat;

/**
 * Base interface for the parser. Contains some 
 * constants used throughout the application.
 * 
 * @author uqdalves
 */
public interface ParserInterface extends Serializable {

	/** Default number of parameters for attributes  
	 *  declaration, (3) i.e. (name | type | delimiter). */
	public int NUM_PARAMS_ATTR  = 3;
	/** Default number of parameters for commands  
	 *  declaration, (2) i.e. (command | value). */
	public int NUM_PARAMS_CMD   = 2;

	/** Default output database. */
	public OutputDatabase 	DEFAULT_OUT_DB 		  = OutputDatabase.LOCAL;
	/** Default output data format. */
	public OutputFormat 	DEFAULT_OUT_FORMAT 	  = OutputFormat.ALL;
	/** Default coordinates system. */
	public Keywords 		DEFAULT_COORD_SYSTEM  = Keywords.CARTESIAN;
	/** Default records delimiter. */
	public String			DEFAULT_RECORDS_DELIM = Keywords.LN.name();
	/** Default Precision for decimal numbers. */
	public int 				DEFAULT_DECIMAL_PREC  = 5;
	/** Default output Database name. */
	public String 			DEFAULT_DB_NAME  	  = "traminerdb";
	/** Default Trajectory collections name. */
	public String 			DATA_COLL_NAME 		  = "trajectorydata";
	/** Default Metadata collections name. */
	public String 			META_COLL_NAME 		  = "metadata";
	
	/** Line break character. */
	public String LINE_BREAK = System.getProperty("line.separator");
	/** Line space character. */
	public String LINE_SPACE = " ";//"U+0020";
	/** Token to bond lines from the same record during pre-processing. */
	public String LINE_BOND  = "@";
	/** Token to bond DATETIME pattern strings during pre-processing. */
	public String DATE_BOND  = "&";
	
	/** List of predefined keywords from the parser. */
	public Keywords[] KEYWORDS_LIST = Keywords.values();
	
}
