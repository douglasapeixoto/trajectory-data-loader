package traminer.loader.map;

/**
 * Java interface for a OsmHandler object.
 * Simply implements each of the Python class methods definitions. 
 * If we want to change the python code a bit and add some code to 
 * one of the methods we can do so without touching the Java interface.
 * 
 * @author uqdalves
 */
public interface OsmHandler{
	/**
	 * Parse the OSM file in the given path name,
	 * and save the parsed files to MongoDB.
	 * 
	 * @return True if the parsing was successful.
	 */
	public boolean parse(String filePathName);
	public String getHostName();
	public String getPortNumber();
	public String getDatabaseName();
	public String getMongoUri();
	public String getParserResults();
}
