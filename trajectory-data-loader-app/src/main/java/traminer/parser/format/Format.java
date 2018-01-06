package traminer.parser.format;

import java.util.ArrayList;
import java.util.List;

import traminer.parser.ParserInterface;
import traminer.parser.analyzer.Keywords;

/**
 * Base superclass to manage the input data format.
 * This object manages the informations about the data format provided
 * in the Trajectory Data Description Format (TDDF) file.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public abstract class Format implements ParserInterface {
	// list of attributes in the format
	private List<AttributeEntry> attributesList = 
			new ArrayList<AttributeEntry>();
	// list of attributes delimiters
	private List<String> delimitersList = 
			new ArrayList<String>();

	/**
	 * Each attribute of the Input Format. Composed by the 
	 * attributes' NAME, TYPE, and DELIMITER char.
	 */
	public class AttributeEntry {
		/** Attributes's name */
		public final String name; 
		/** Attributes's type */
		public final String type; 
		/** Attributes's delimiter */
		public final String delim;
		
		/**
		 * An attribute from the input data format file.
		 * 
		 * @param name Attributes's name.
		 * @param type Attributes's type.
		 * @param delim Attributes's delimiter.
		 */
		public AttributeEntry(String name, String type, String delim) {
			if (name == null || type == null || delim == null) {
				throw new NullPointerException(
						"Attribute properties must not be null.");
			}
			this.name  = name;
			this.type  = type;
			this.delim = delim;
		}
		
		/**
		 * @return Check whether this attribute is of the Array type.
		 */
		public boolean isArrayType() {
			return this.type.startsWith(Keywords.ARRAY.name());
		}
		
		/**
		 * @return Check whether this attribute is of the DateTime type.
		 */
		public boolean isDateTimeType() {
			return this.type.startsWith(Keywords.DATETIME.name());
		}
		
		/**
		 * @return Check whether this attribute has been marked as 'ignored'.
		 */
		public boolean isIgnoredAttr() {
			return type.equals(Keywords._IGNORE_ATTR.name());
		}
	}

	/**
	 * @return List of attributes in the format file.
	 */
	public List<AttributeEntry> getAttributesList() {
		return attributesList;
	}
	
	/**
	 * Get the i-th attribute in this data format.
	 * 
	 * @param i
	 * @return A attribute entry from the data format.
	 */
	public AttributeEntry getAttribute(int i) {
		if (i < 0 || i >= attributesList.size()) {
			throw new IndexOutOfBoundsException(
					"Attribute index out of bound.");
		}
		return attributesList.get(i);		
	}

	/**
	 * Add a new attribute to the format file entity.
	 * 
	 * @param attrName Attributes's name.
	 * @param attrType Attributes's type.
	 * @param attrDelim Attributes's delimiter.
	 */
	public void addAttribute(String attrName, String attrType, String attrDelim) {
		if (attrName == null || attrType == null || attrDelim == null) {
			throw new NullPointerException(
					"Attribute's properties must not be null.");
		}
		if (attrDelim.equals(Keywords.LS.name())) {
			attrDelim = LINE_SPACE;
		} else 
		if (attrDelim.equals(Keywords.LN.name())) {
			// the pre-processing will remove the line breaks,
			// will put all lines of the same record in the same line
			attrDelim = LINE_BOND; // LINE_BOND changed from LINE_SPACE
		}
		attributesList.add(new AttributeEntry(attrName, attrType, attrDelim));
		delimitersList.add(attrDelim);
	}
	
	/**
	 * @return The total number of attributes specified in the input data format,
	 * including the ignores attributes.
	 */
	public int numAttributes() {
		return attributesList.size();
	}
		
	/**
	 * @return The total of valid attributes from the input data, that is,
	 * the number of attributes specified in the input data format
	 * less the ignores attributes.
	 */
	public int numValidAttributes() {
		int count = 0;
		for (AttributeEntry attr : attributesList) {
			if (!attr.isIgnoredAttr()) count++;
		}
		return count;
	}
	
	/**
	 * Check whether the attribute at the i-th position
	 * is ignored for reading.
	 * 
	 * @param i
	 * @return True if the i-th attribute is to be ignored.
	 */
	public boolean isIgnoredAttr(int i) {
		if (i < 0 || i >= attributesList.size()) {
			throw new IndexOutOfBoundsException(
					"Attribute index out of bound.");
		}
		return attributesList.get(i).isIgnoredAttr();
	}

	/** 
	 * Get the list of attributes' delimiters in the data format,
	 * <p>
	 * For TRAJECTORY records, this is the list of the attribute's
	 * delimiters, in the same order as given in the format file.
	 * <p>
	 * For ARRAY records, this is the delimiter of each array item
	 * declared. In the same order as given in the format file.
	 * 
	 * @return List of attributes' delimiters.
	 */
	public List<String> getDelimiters() {
		return delimitersList;
	}
}