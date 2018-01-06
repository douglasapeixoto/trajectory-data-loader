package traminer.parser.analyzer;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Lexical symbols used in the format analyzer.
 * The symbols for each type (class) of token in  
 * the Input Data Format.
 * 
 * @author douglasapeixoto
 */
public interface Symbols extends Serializable {
	/** Annotation to identify the lexical symbol of each keywords. */
	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface LexicalSymbol{String symbol() default StringSymbol;}
	
	/** Lexical symbols must be unique. */
	public static final String
		CommentSymbol		= "$cmnt",
		AttributeSymbol 	= "$attr",
		CommandSymbol   	= "$cmd",
		DelimiterSymbol		= "$dlim",
		TypeSymbol			= "$type",
		ArrayTypeSymbol 	= "$array",
		DateTypeSymbol		= "$dtype",
		DeltaTypeSymbol 	= "$etype",
		FormatSymbol		= "$frmt",
		CoordinateSysSymbol	= "$csys",
		DBNameSymbol		= "$db",
		EndCmdSymbol		= "$end",
		StringSymbol		= "$str";
}

