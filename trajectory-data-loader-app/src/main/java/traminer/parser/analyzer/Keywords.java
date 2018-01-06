package traminer.parser.analyzer;

import traminer.parser.analyzer.Symbols.LexicalSymbol;

/**
 * Predefined parser/interpreter keywords. Reserved keywords 
 * used in the input data format file.
 * 
 * @author uqdalves
 */
public enum Keywords {

	/**
	 * Attribute name keywords
	 */
	@LexicalSymbol(symbol = Symbols.AttributeSymbol)
	_ID,			// trajectory ID
	@LexicalSymbol(symbol = Symbols.AttributeSymbol)
	_COORDINATES,	// list of trajectory coordinates
	@LexicalSymbol(symbol = Symbols.AttributeSymbol)
	_X,				// point X (or Longitude) coordinate
	@LexicalSymbol(symbol = Symbols.AttributeSymbol)
	_Y,				// point Y (or Latitude)  coordinate
	@LexicalSymbol(symbol = Symbols.AttributeSymbol)
	_LON,			// point Longitude coordinate
	@LexicalSymbol(symbol = Symbols.AttributeSymbol)
	_LAT,			// point Latitude  coordinate	
	@LexicalSymbol(symbol = Symbols.AttributeSymbol)
	_TIME,			// point time-stamp

	/**
	 * Command keywords
	 */
	@LexicalSymbol(symbol = Symbols.CommandSymbol)
	_OUTPUT_FORMAT,	// intermediate file format
	@LexicalSymbol(symbol = Symbols.CommandSymbol)
	_IGNORE_ATTR,	// ignore input file attribute
	@LexicalSymbol(symbol = Symbols.CommandSymbol)
	_IGNORE_LINES,	// lines in the files to ignore
	@LexicalSymbol(symbol = Symbols.CommandSymbol)
	_COORD_SYSTEM,	// coordinate system (e.g. Cartesian, Geographic)
	@LexicalSymbol(symbol = Symbols.CommandSymbol)
	_SPATIAL_DIM,   // number of spatial dimension of the data points
	@LexicalSymbol(symbol = Symbols.CommandSymbol)
	_AUTO_ID,   	// auto generate trajectory ID
	@LexicalSymbol(symbol = Symbols.CommandSymbol)
	_RECORDS_DELIM,	// data records delimiter
	@LexicalSymbol(symbol = Symbols.CommandSymbol)
	_DECIMAL_PREC,	// precision for decimal numbers

	/**
	 * Attribute type keywords
	 */
	// basic data types
	@LexicalSymbol(symbol = Symbols.TypeSymbol)
	DECIMAL,
	@LexicalSymbol(symbol = Symbols.TypeSymbol)
	INTEGER,
	@LexicalSymbol(symbol = Symbols.TypeSymbol)
	STRING,
	@LexicalSymbol(symbol = Symbols.TypeSymbol)
	BOOLEAN,
	@LexicalSymbol(symbol = Symbols.TypeSymbol)
	CHAR,
	// complex data types
	@LexicalSymbol(symbol = Symbols.DateTypeSymbol)
	DATETIME,
	@LexicalSymbol(symbol = Symbols.ArrayTypeSymbol)
	ARRAY,
	@LexicalSymbol(symbol = Symbols.DeltaTypeSymbol)
	DELTAINTEGER,
	@LexicalSymbol(symbol = Symbols.DeltaTypeSymbol)
	DELTADECIMAL,
	
	
	/**
	 * Attribute delimiter keywords
	 */
	@LexicalSymbol(symbol = Symbols.DelimiterSymbol)
 	LN,		// end line delimiter (e.g. \n)
 	@LexicalSymbol(symbol = Symbols.DelimiterSymbol)
	LS,		// line space delimiter
	@LexicalSymbol(symbol = Symbols.DelimiterSymbol)
	EOF,	// end of file delimiter

	
	/**
	 * Coordinates system keywords
	 */
	@LexicalSymbol(symbol = Symbols.CoordinateSysSymbol)
	CARTESIAN,	// (x, y)
	@LexicalSymbol(symbol = Symbols.CoordinateSysSymbol)
	GEOGRAPHIC;	// (longitude, latitude)

	
	/** 
	 * Line comment char 
	 */
	@LexicalSymbol(symbol = Symbols.CommentSymbol)
	public static final String COMMENT_CHAR = "#";
	
	
	/**
	 * Output format type keywords
	 */
	public enum OutputFormat{
		@LexicalSymbol(symbol = Symbols.FormatSymbol)
		SPATIAL_TEMPORAL, // output format style - ALL attributes
		@LexicalSymbol(symbol = Symbols.FormatSymbol)
		ALL, 			  // output format style - spatial-temporal
		@LexicalSymbol(symbol = Symbols.FormatSymbol)
		SPATIAL;		  // output format style - spatial attributes
	}

	/**
	 * Available databases to output the data.
	 */
	public enum OutputDatabase {
		@LexicalSymbol(symbol = Symbols.DBNameSymbol)
		LOCAL, 
		@LexicalSymbol(symbol = Symbols.DBNameSymbol)
		MONGODB, 
		@LexicalSymbol(symbol = Symbols.DBNameSymbol)
		HBASE,
		@LexicalSymbol(symbol = Symbols.DBNameSymbol)
		VOLTDB
	}

	/**
	 * The Enumeration of the given keyword.
	 * 
	 * @param keyword
	 * @return The enumeration of the given keyword. Or the keyword 
	 * "String" if the specified value does not belong to this enumeration
	 */
	public static Keywords lookup(String keyword) {
		try {
			return valueOf(keyword);
		} catch (IllegalArgumentException e1) {
			return Keywords.STRING;
        }
	}
	
	/**
	 * @param type
	 * @return Return the basic type keyword of the given type 
	 * (if the given type is a complex type).
	 */
	public static Keywords getBasicType(String type) {
		if (type.equals(DELTADECIMAL.name())) return DECIMAL;
		if (type.equals(DELTAINTEGER.name())) return INTEGER;
		if (type.startsWith(DATETIME.name())) return DATETIME;
		if (type.startsWith(ARRAY.name())) 	  return ARRAY;
		return lookup(type);
	}
	
	/**
	 * Check whether the given attribute type is a number type,
	 * e.g. Integer, Decimal, DeltaInteger, DeltaDecimal.
	 * 
	 * @param type The type to check.
	 * @return True if the given type is a number type.
	 */
	public static boolean isNumberType(String type) {
		if (type.equals(DECIMAL.name())) return true;
		if (type.equals(INTEGER.name())) return true;
		if (type.equals(DELTADECIMAL.name())) return true;
		if (type.equals(DELTAINTEGER.name())) return true;
		return false;
	}
	
	/**
	 * Check whether the given attribute type is a DATETIME type.
	 * 
	 * @param type The type to check.
	 * @return True if the given type is a DATETIME.
	 */
	public static boolean isDateTimeType(String type) {
		if (type.startsWith(DATETIME.name())) return true;
		return false;
	}
}