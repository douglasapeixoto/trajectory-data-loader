package traminer.parser;

/**
 * Exception for Parser runtime errors.
 * 
 * @author douglasapeixoto
 */
@SuppressWarnings("serial")
public class ParserException extends RuntimeException {
	/**
	 * Constructs a new Parser exception without detail message. 
	 */
	public ParserException() {}
	/**
	 * Constructs a new Parser exception with the specified 
	 * detail message.
	 * 
	 * @param message The exception detail message.
	 */
	public ParserException(String message) {
		super(message);
	}
	/**
	 * Constructs a new Parser exception with the specified 
	 * cause.
	 * 
	 * @param cause The cause for the exception.
	 */
	public ParserException(Throwable cause) {
		super(cause);
	}
	/**
	 * Constructs a new Parser exception with the specified 
	 * cause and a detail message.
	 * 
	 * @param message The exception detail message.
	 * @param cause The cause for the exception.
	 */
	public ParserException(String message, Throwable cause) {
		super(message, cause);
	}
}
