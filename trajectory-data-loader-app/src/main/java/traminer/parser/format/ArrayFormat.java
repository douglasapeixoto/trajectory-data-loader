package traminer.parser.format;
 
import java.util.StringTokenizer;

import traminer.parser.analyzer.Keywords;

/**
 * Object to manage array formats (properties and parameters)
 * from the input data format.
 * 
 * @author douglasapeixoto
 */
@SuppressWarnings("serial")
public class ArrayFormat extends Format {
	// position of the X or LON coordinate attribute
	private int _xPos = -1;
	// position of the Y or LAT coordinate attribute
	private int _yPos = -1;
	// position of the Time coordinate attribute
	private int _tPos = -1;
	
	/**
	 * Creates a new format entity for a given Array type declaration.
	 * <p>
	 * Note: This constructor assumes that the Input Data Format
	 * has already passed the syntax analysis.
	 * 
	 * @param arrayFormat The description of the Array format, 
	 * e.g. Array(_X Double , _Y Double , _TIME Integer ,) 
	 * 
	 * @see LexicalAnalyzer
	 * @see SyntaxAnalyzer
	 */
	public ArrayFormat(String arrayFormat) {
		addArrayFormat(arrayFormat);
	}
	
	/**
	 * Add the description of the array format declaration,
	 * e.g. Array(_X DECIMAL , _Y DECIMAL , _TIME DECIMAL ,)
	 * 
	 * @param arrayFormat Array format description.
	 */
	private void addArrayFormat(String arrayFormat) {
		StringTokenizer arrayAttrs = new StringTokenizer(arrayFormat);
		arrayAttrs.nextToken("("); // skip "ARRAY("
		// attribute's name, type, and delimiter
		String attrName, attrType, attrDelim;
		while (arrayAttrs.hasMoreTokens()) {
			attrName  = arrayAttrs.nextToken("( ");		
			attrType  = arrayAttrs.nextToken(" ");
			attrDelim = arrayAttrs.nextToken(" )");		
			this.addAttribute(attrName, attrType, attrDelim);
		}
	}
		
	/**
	 * @return The number of spatial dimensions in the input
	 * data format (spatial dimensions of the coordinates).
	 */
	public int getSpatialDimensions() {
		int dim = 0;
		if (getXAttrIndex() != -1) dim++;
		if (getYAttrIndex() != -1) dim++;
		return dim;
	}
	
	/**
	 * The position of the '_X' or '_LON' attribute in the coordinates array. 
	 * <br> Attribute's  position starts from zero = 0.
	 * 
	 * @return the position of the '_X' or '_LON' attribute, or -1 if no 
	 * attribute '_X' or '_LON' has been specified.
	 */
	public final int getXAttrIndex() {
		if (_xPos != -1) return _xPos; 
		for (int i=0; i<numAttributes(); i++) {
			AttributeEntry attr = getAttribute(i);
			if (attr.name.equals(Keywords._X.name()) ||
				attr.name.equals(Keywords._LON.name())) {
				_xPos = i;
				return _xPos;
			}
		}
		// no '_X' attribute found in the format file
		return _xPos; // -1;
	}
	
	/**
	 * The position of the '_Y' or '_LAT' attribute in the coordinates array.
	 * <br> Attribute's position starts from zero = 0.
	 * 
	 * @return the position of the '_Y' or '_LAT' attribute, or -1 if no  
	 * attribute '_Y' or '_LAT' has been specified.
	 */
	public final int getYAttrIndex() {
		if (_yPos != -1) return _yPos; 
		for (int i=0; i<numAttributes(); i++) {
			AttributeEntry attr = getAttribute(i);
			if (attr.name.equals(Keywords._Y.name()) ||
				attr.name.equals(Keywords._LAT.name())) {
				_yPos = i;
				return _yPos;
			}
		}
		// no '_Y' or '_LAT' attribute found in the format file
		return _yPos; // -1;
	}
		
	/**
	 * The position of the '_TIME' attribute in the coordinates array. 
	 * <br> Attribute's position starts from zero = 0.
	 * 
	 * @return the position of the '_TIME' attribute, or -1 if no 
	 * attribute '_TIME' has been specified.
	 */
	public final int getTimeAttrIndex(){
		if (_tPos != -1) return _tPos; 
		for (int i=0; i<numAttributes(); i++) {
			AttributeEntry attr = getAttribute(i);
			if (attr.name.equals(Keywords._TIME.name())) {
				_tPos = i;
				return _tPos;
			}
		}
		// no '_TIME' attribute found in the format file
		return _tPos; // -1;
	}
	
}