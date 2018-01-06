package traminer.loader.map;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import traminer.io.params.MongoDBParameters;

/**
 * Object factory to create a Java 'OsmHandler' object 
 * from a Pyhton 'OsmHandler' object using Jython.
 * 
 * @author uqdalves
 */
public class OsmHandlerFactory {
    private PyObject osmHandlerClass;
    private PythonInterpreter interpreter;
	// system log
	private static Logger log = 
			Logger.getLogger(OsmHandlerFactory.class);  
    /**
     * Create a new PythonInterpreter object, then use it to
     * execute some python code. In this case, we want to
     * import the python module that we will coerce.
     * <p>
     * Once the module is imported than we obtain a reference to
     * it and assign the reference to a Java variable
     */
	public OsmHandlerFactory() {
		try {
			Properties preprops = System.getProperties();
			Properties props = new Properties();
			// Used to prevent: console: Failed to install '': java.nio.charset.UnsupportedCharsetException: cp0.
			props.put("python.console.encoding", "UTF-8"); 
			//don't respect java accessibility, so that we can access protected members on subclasses
			props.put("python.security.respectJavaAccessibility", "false"); 
			props.put("python.import.site","false");

			PythonInterpreter.initialize(preprops, props, new String[0]);
			interpreter = new PythonInterpreter();

			PySystemState sys = interpreter.getSystemState();
			sys.path.append(new PyString(
					"./lib/python_modules/pymongo-3.4.0-cp27-none-win_amd64.whl"));
   
			interpreter.exec("from OsmHandlerInterface import OsmHandler");
			osmHandlerClass = interpreter.get("OsmHandler");
		} catch (Exception e) {
			log.error("Error creating OsmHadlerFactory."
					+"\n"+e.getMessage());
		}
    }
	
    /**
     * The create method is responsible for performing the actual
     * coercion of the referenced Python module into Java bytecode.
     * <p>
     * Creates a new OsmHandler to load and parse OSM file to MongoB.
     * 
     * @param mongoParams access parameter to MongoDB
     */
    public OsmHandler create(MongoDBParameters mongoParams) {
    	return create(mongoParams.getHostName(), 
    				  mongoParams.getPortNumber(), 
    				  mongoParams.getDatabaseName());
    }
    
    /**
     * The create method is responsible for performing the actual
     * coercion of the referenced Python module into Java bytecode.
     * <p>
     * Creates a new OsmHandler to load and parse OSM file to MongoB.
     * 
     * @param hostName MongoDB host name.
     * @param portNum MongoDB port number.
     * @param dbName MongoDB database name to output the data.
     */
    public OsmHandler create(String hostName, int portNum, String dbName) {
    	OsmHandler osmHandler = null;
    	try {
			// call the Python class constructor _init_
			PyObject osmHandlerObject = osmHandlerClass.
					__call__(new PyString(hostName),
							 new PyString(""+portNum),
			                 new PyString(dbName));
			// create the java object for this Python class
			osmHandler = (OsmHandler)osmHandlerObject.__tojava__(OsmHandler.class);
		} catch (Exception e) {
			log.error("Error creating OsmHadlerType Java object."
						+"\n"+e.getMessage());
		}
    	return osmHandler;
    }
    
    /**
     * Close Jython interpreter
     */
    public void close() {
        interpreter.close();
    }
}
