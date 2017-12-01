# Trajectory Data Loader
----------
A system for trajectory data loading, representation, and integration, with support for trajectory data compression (i.e. lossless Delta compression). Provides templates for representation of different input trajectory datasets, for data integration and storage.  Currently application provides four different storage options, i.e. Local directory, MongoDB, HBase (Distributed storage), and VoltDB (in-memory storage).

This application also generates statistical information (Metadata) about the input dataset. This application  was built using Java 8, and also provides a platform independent GUI (based on JavaFX) for easy trajectory data loading and parsing.

#### Trajectory Data:

**Brief:** The main functionalities of this application are: 

* **Load** trajectory data in any textual format.
* **Parse** the raw data based on a **Trajectory Data Description Format (TDDF)** to one of the system-provided **Output Formats** (i.e. SPATIAL, SPATIAL-TEMPORAL, ALL).
* **Store** the parsed data to a database of choice (e.g. MongoDB, Local directory, HBase, VoltDB). 


# User and Installation Guide
----------
The trajectory data loader is built in Java, and provides a platform-independent GUI for easy data loading packed in a executable **JAR**, the application `.jar` file can be downloaded from the project repository [here](https://drive.google.com/open?id=0B7xkq-yBgoHvV1RHbXhEcjRyd28). You will need [Java 8][java8] installed in your machine to run this `.jar` file.

The following image shows the data loader GUI window. 

![img](https://github.com/douglasapeixoto/trajectory-data-loader/blob/master/gui-window.png)

**Input Parameters**: 

-	**Trajectory:** One must simply provide (1) the path to the directory containing the trajectory data files; (2) the script (or file) containing the input *Trajectory Data Description Format (TDDF)*; (3) the *Coordinates System* of data coordinate points; and (4) the *Output Format* of choice.

**Output Database**: Finally, one must provide  the *Output Database* information to store the parsed data. Currently the system provides two options for storing the parsed data: 

-   (1) **MONGODB**: which stores the parser data into MongoDB as mongo documents.
    * To use MongoDB, [donwload][mongodb-download] the latest version of MongoDB for your OS, then [install][mongodb-install] and start the MongoDB service. The GUI is already set to use the default MongoDB server parameters, however, the user is free to provide their own server access parameters (i.e. host address, and database name).
	    * **Optional:** You can start the MongoDB server from within the data loader GUI, you first need to setup the `MONGO_HOME` path in your OS environment variables to the binary folder of MongoDB installation, ex: `/path-to-mongodb/Server/3.x/bin`, then click in **Start MongoDB** in the database configuration tab in the application GUI.
    * **Trajectory Collections:** The application will store the parsed data, the *Ouput TDDF*, and the *Metadata* into the default MongoDB collections: `"trajectorydata"` and `"metadata"`.
    
-   (2) **LOCAL**: which stores the parsed data into a user-specified directory as `.csv` files, as well as the the *Ouput Data Format* as `output_format.conf`, and the *Metadata* as `metadata.meta`. Local storage currently is only available for trajectory data.

-	(3) **HBASE** distributed data storage:

-	(4) **VOLTDB** in-memory storage:


# Trajectory Data Loading and Parsing
----------
This application reads raw trajectory files in any textual format. However, different datasets provide different data formats, thus one must specify the fields/attributes in the source data format, this configuration is done through the input **Trajectory Data Description Format (TDDF)**, provided by the user.

### Trajectory Data Description Format: TDDF
***
A user specified script containing the format of the input data. The  script contains both **attribute** declarations and **commands** to be executed. Following we describe the *TDDF* grammar ans syntax, and give some examples.  

***
### TDDF Grammar

**Predefined Keywords:** Predefined keywords aid the parser to identify important parameters and commands in the input data. Following is the list of predefined keywords and their meanings. All keywords are case-sensitive. 

| Keyword          | Type                   | Description       |
| :-------         | :----                  | :---              |
|`_ID`	           | Attribute Name    		| Trajectory Identifier
|`_COORDINATES`	   | Attribute Name 	    | List of Trajectory Coordinates
|`_X`	           | Attribute Name 	    | Coordinate X value
|`_Y`	           | Attribute Name 	    | Coordinate Y value
|`_LON`        	   | Attribute Name 		| Coordinate Longitude value
|`_LAT`            | Attribute Name			| Coordinate Latitude value
|`_TIME`           | Attribute Name 	    | Coordinate Time-Stamp
|`INTEGER`	       | Attribute Type	        | Integer number
|`DECIMAL`	       | Attribute Type	        | Decimal number
|`STRING`          | Attribute Type	        | String character
|`BOOLEAN`	       | Attribute Type	        | Logic type (True/False)
|`CHAR`	           | Attribute Type	        | Single character
|`DATETIME`	       | Attribute Type	        | Date and time (Java DateTimeFormat)
|`DELTAINTEGER`	   | Attribute Type	        | Integer delta compressed number
|`DELTADECIMAL`	   | Attribute Type	        | Decimal delta compressed number
|`ARRAY`	       | Attribute Type         | Array type (List)
|`CARTESIAN`	   | Command Value  		| Cartesian coordinates (x,y) 
|`GEOGRAPHIC`      | Command Value		  	| Geographic coordinates (longitude,latitude) 
|`LN`	           | Command Value  		| Line-break 
|`LS`	           | Command Value  		| Line-space 
|`EOF`             |Command Value   		| End-of-File 
|`SPATIAL_TEMPORAL` |	Output Format 		| Outputs spatial-temporal attributes only 
|`SPATIAL`		   | Output Format 			| Outputs spatial attributes only
|`ALL`			   | Output Format 			| Outputs all attributes
|`#`			   | Comment Marker 		| Line comment symbol

**Default Command Values:** Although necessary for the data interpreter, some commands are provided with a default parameter/value in case they are not provided by the user. 

| Keyword          | Type                   | Description   | Default Value    |
| :-------         | :----                  | :---           | :---    |
|``_RECORDS_DELIM`` |Command Name	  | Data Records Delimiter 		|LN (Line-break) 
|``_IGNORE_ATTR``	 | Command Name	  | Ignore Input Attribute 		| --  
|``_IGNORE_LINES``  | Command Name	  | Ignore Input File Line(s) 	| --  
|``_AUTO_ID``	     | Command Name   | Auto generate ID attribute 	| --  
|``_COORD_SYSTEM``  | Command Name	  | Spatial coordinates system 	| GEOGRAPHIC 
|``_DECIMAL_PREC``  | Command Name   | Precision for decimal numbers | 5
|``_SAMPLE``		 | Command Name	  | Load a sample of the dataset | 1.0 (100%) 
|``_OUTPUT_FORMAT`` | Command Name	  | User-specified output format | ALL 

 
***
### TDDF Syntax and Semantics

For each *attribute* of the data record, one must provide the attributes' ``NAME``, ``TYPE`` and ``DELIMITER``, separated by space or tab. 

    NAME:			Name of the field/attribute
	TYPE*:			Type of the field/attribute to read
	DELIMITER:		Field delimiter (character)

When providing the *TDDF* script, the user must declare one attribute per line in the exact order they appear in the input file. The parser will read the attributes' value until the given field ``DELIMITER`` is reached. Attributes' name must be unique in the TDDF. Commands are declared in the form ``NAME``, and ``VALUE``.

    NAME: 		Name of the field/attribute.  
    VALUE: 		The command's input parameter/value.

**_ID, _AUTO_ID:** The attribute keyword `_ID` describes the identifier field of each trajectory record. Since in our research not all input datasets provide an ID for the trajectory records, the command `_AUTO_ID` to generate the records' IDs automatically. An example of the `_AUTO_ID` command syntax is given as follows:

    _AUTO_ID		prefix
    # Output the ID attribute as STRING: 
    # prefix_1, prefix_2, ...
	
	_AUTO_ID		10
	# Outputs the ID as attribute INTEGER, 
	# starting from the given number: 
	# 10, 11, 12, ...

Either the trajectory `_ID` attribute field, or `_AUTO_ID`, should be provided in the input *TDDF*. If both are omitted, the application will use `_AUTO_ID 	1` by default.	

**_COORDINATES, _X, _Y, _TIME:**	The attribute keyword `_COORDINATES` is a mandatory field, and describes the list of coordinate points of the trajectory records. The  `_COORDINATES` must be declared as an `ARRAY` type, followed by the description of the spatial-temporal attributes -- i.e. `_X, _Y,  _TIME` in  `CARTESIAN` system, or `_LON, _LON, _TIME`  in `GEOGRAPHIC` system -- and any semantic attributes of the coordinate points, in the same order they appear in the input data files. The spatial-temporal fields `_X, _Y, _TIME`, or  `_LON, _LAT, _TIME`, in a `_COORDINATES` attribute declaration are **mandatory**. 

**_RECORDS_DELIM:** The command `_RECORDS_DELIM` tells the parser the final of a data record. In most GPS trajectory datasets in our research, data records are organised by either one trajectory record per file line, that is `_RECORDS_DELIM LN`, one trajectory record per file, that is `_RECORDS_DELIM EOF`, or many records per file separated by a delimiter character or word `c` , that is `_RECORDS_DELIM c`. The parser will read a data record until the given delimiter is found.

**_IGNORE_LINES:** 	The command `_IGNORE_LINES` tells the parser to ignore the given lines in all input data files. For instance, the following command will ignore the lines `1` to `5` and `7` in the input data files.

    _IGNORE_LINES  		[1-5,7] 

**_IGNORE_ATTR:**	The command `_IGNORE_ATTR`, on the other hand, ignores the attribute in the position of its declaration in all data records, and it is followed by the attributer's delimiter. Both `_IGNORE_LINES` and `_IGNORE_ATTR` commands are useful, for instance, when not all data records, file lines, or attributes from the input dataset are necessary for the user application.
	
**_DECIMAL_PREC:**  The command `_DECIMAL_PREC` tells the parser the number of decimal points $d$ to consider in decimal values, the default value is $d=5$. Attributes declared as `DECIMAL` will be converted to a integer number in the format $value * 10 ^ d$, and compressed using a lossless delta-compression to reduce storage space.

**_SAMPLE:**  The command `_SAMPLE` tells the data loader to randomly select a sample the input dataset for reading and parsing. The value for sampling must be in the range $]0.0, 1.0]$ which specifies the percentage of data records to read. The `_SAMPLE` command is particularly useful for large datasets and debugging purposes. 


####Array Type Syntax

Arrays (or lists) types are declared by specifying the attributes in the array, i.e. attributes' `NAME`, `TYPE` and `DELIMITER`, the general syntax Array declaration is:

    ARRAY ( NAME   TYPE   DELIMITER  ... )

Arrays can be single-valued or multi-valued (e.g. objects) of any of the pre-defined data types, the parser will read the parameters until the given field delimiter is reached. Attributes in the array are specified in the exact order they appear in the source file, similar to any other attribute declaration. Following are some examples of  array type declaration for  `_COORDINATES` field.

**Example 1:**  A simple Array of String objects, separated by line-space `LS`
    
```
    ARRAY ( varName  STRING  LS )
```

**Example 2:** Trajectory coordinates as an array/list of spatial-temporal points, comma separated.

```
	ARRAY ( _X     DECIMAL  , 
	        _Y     DECIMAL  , 
	        _TIME  INTEGER  , )
``` 

**Example 3:** Trajectory coordinates as an array/list of spatial-temporal points, with `weight` and `ptType` attributes, one coordinate per file line, separated by semicolon.

```
	ARRAY ( _X      DECIMAL	 ;
		    _Y	    DECIMAL	 ;
	        _TIME   INTEGER	 ;
	        weight  DECIMAL	 ;
	        ptType  STRING	 LN )
```

**Example 4:**   Array of spatial-temporal points, comma separated, `_X` and `_Y` attributes delta-compressed.

```
	ARRAY ( _X     DELTADECIMAL  ,
		    _Y     DELTADECIMAL  ,
		    _TIME  INTEGER       ,  )
```


## Output Formats
***

After the input data is parsed, the data in the new format is stored into any of our primary storage platforms, in the output format of choice, along with the output *TDDF*  and a *Metadata* file, containing information and statistics about the input trajectory dataset, such as *number of records*, statics about the *speed, length, duration, sampling rate*, and *coverage* of the trajectory records. The system generated output *TDDF* file, on the other hand, contains the specifications of the output data, that is, the  `NAME` and `TYPE` of all attributes in the output data.

 e provide three different output formats in our application, namely  `ALL, SPATIAL, SPATIAL-TEMPORAL`. All output data formats follow a *CSV* (comma separated values) style. Attribute values are separated by semicolon, and array items are separated by comma. The output files contain one trajectory record per file line. Furthermore, to reduce storage consumption, the spatial-temporal attributes in the list of coordinates are delta-compressed. The records’ attributes are always in the order:

    _ID;_COORDINATES;_OTHER_ATTRIBUTES

Following we describe the system provided output data formats.
	
**SPATIAL:**  In this output format, records contain the trajectory `_ID` and the list of spatial attributes of the `_COORDINATES` only. This format is useful for applications that does not demand processing over the temporal attributes of the trajectories.  

*Output Data Example:*

    T1_ID;x1,y1,x2,y2,...,xN,yN
    T2_ID;x1,y1,x2,y2,...,xM,yM
        …
    TK_ID;x1,y1,x2,y2,...,xQ,yQ

*Output Data Format:*
```
    _OUTPUT_FORMAT	SPATIAL
    _COORD_SYSTEM	CARTESIAN
    _DECIMAL_PREC   5
    _ID	            STRING
    _COORDINATES	ARRAY(_X     DECIMAL
						  _Y     DECIMAL)
```

**SPATIAL-TEMPORAL:** In this output format, records contain the trajectory `_ID` and the list of spatial-temporal attributes of the `_COORDINATES` only. This output format contains the most basic information of trajectories, commonly used in spatial-temporal queries and mining applications.

*Output Data Example:*

    T1_ID;x1,y1,t1,x2,y2,t2,...,xN,yN,tN
    T2_ID;x1,y1,t1,x2,y2,t2,...,xM,yM,tM
        ...
    TK_ID;x1,y1,t1,x2,y2,t2,...,xQ,yQ,tQ

*Output Data Format:*
```
    _OUTPUT_FORMAT	SPATIAL_TEMPORAL
    _COORD_SYSTEM	CARTESIAN
    _DECIMAL_PREC   5
    _ID	            STRING
    _COORDINATES	ARRAY(_X     DECIMAL
						  _Y     DECIMAL
						  _TIME  INTEGER)
```

**ALL:** In this output format, records contain the complete set of attributes specified in the input *TDDF*, that is, the trajectory `_ID` , the list of trajectory `_COORDINATES`points (with all provides coordinate attributes), and the list of semantic attributes of the trajectory. This is the default output format.

*Output Data Example:*

    T1_ID;x1,y1,t1,x2,y2,t2,...,xN,yN,tN;s1;s2;...;sK
    T2_ID;x1,y1,t1,x2,y2,t2,...,xM,yM,tM;s1;s2;...;sK
        ...
    TK_ID;x1,y1,t1,x2,y2,t2,...,xQ,yQ,tQ;s1;s2;...;sK

*Output Data Format:*
```
    _OUTPUT_FORMAT	ALL
    _COORD_SYSTEM	CARTESIAN
    _DECIMAL_PREC   5
    _ID	            STRING
    _COORDINATES	ARRAY(_X     DECIMAL
						  _Y     DECIMAL
						  _TIME  INTEGER)
    s1	            STRING
    s2	            INTEGER
        ...	
    sK	            DECIMAL
```

ertetr


# Programming Guide (For Developers)
----------
In this documentation we opt to use the [Eclipse][eclipse] IDE. However, for other Java IDEs, such as [Intellij][intellij] and [NetBeans][netbeans], the process is rather similar.


### Importing the Application into Eclipse

Pre-requisites:

**Java:**

* [Java 8][java8] JDK Installed. 
	* *Note that it may not work for previous Java versions.*
* [Eclipse][eclipse] IDE.
* Apache [Maven][maven] Installed.
*	MongoDB [download][mongodb-download] and [install][mongodb-install]
* [JavaFX][javafx] API for the GUI business logic.
* [E(fx)clipse][efxclipse] JavaFX plugin for Eclipse.
	* The system GUI was built using the [JavaFX][javafx] API. To be able to run the application GUI on Eclipse (or any other IDE) you must install the JavaFX plugin (If you are not using Eclipse, you must install the JavaFX plugin for the Java IDE you are using). In Eclipse install the [E(fx)clipse][efxclipse] (*you just need to follow steps 1 and 2 of the e(fx)clipse tutorial*).
	* [SceneBuilder][scene-builder] framework for the GUI building (optional).
* Source code of the `traminer-data-loader-app` application.
* Source code of the `traminer-util-lib` library.
* Source code of the `traminer-io-lib` library.

The project dependencies are managed by [Maven][maven], which is already supported in Eclipse. This application also uses the [Traminer Util Library][traminer-util] and the [Traminer I/O Library][traminer-io], which must also be imported into your development environment.  To import the `traminer-data-loader-app` project, and the `traminer-util-lib` and  `traminer-io-lib` libraries  into Eclipse IDE, do: 

*OPTION 1: Open Project From Local Folder*: 

    File -> Import -> Maven -> Existing Maven Projects 

Browse the Java project folder `traminer-data-loader-app`, click in **Finish** and wait until Maven build the workspace and import the libraries. Do the same for the `traminer-util-lib` and `traminer-io-lib`.

*OPTION 2: Open Project From Git Repository*: 

    File -> Import -> Git -> Projects from Git 

Select **Clone URI** and enter with the Traminer Git repository [URI][traminergit], and select the Java project `traminer-data-loader-app`, click in **Finish**. Do the same for the `traminer-util-lib` and `traminer-io-lib`.

**Configuring the Building Path**: In case this is the first time you import the application to your Eclipse environment, you must add the *Traminer Util Library* and *Traminer I/O Library* to your application building path. 

* In the Eclipse *Project Explorer* tab, right button on the `traminer-data-loader-app`, go to:

        Properties -> Java Build Path -> Projects
        
	* *Add* the `traminer-util-lib` to your build path.
    * *Add* the `traminer-io-lib` to your build path.

### Running the Application: 

* In the *Project Explorer* tab you can run the application GUI at `traminer.loader.gui.DataLoaderGUI`. 
* In the project source folder [`examples`](/traminer-trajectory-loader-app/examples/) you will find some *Input/Output* data examples, and their respectives *Data Formats*.
* In the project source folder [`doc`](/traminer-trajectory-loader-app/doc/) you will find the system documentation (JavaDoc) in HTML format `index.html`.


### Calling the Data Loader and Parser

Following are some short examples of how to use this code to load, parse and output trajectory data using the Eclipse console.

**Ex 1:** Load, parse and output data to a **Local** directory.

```
import traminer.io.IOService;
import traminer.io.params.LocalFSParameters;
import traminer.parser.TrajectoryParser;
import traminer.parser.analyzer.Keywords.OutputFormat;
. . .
	/** Load and parse trajectory data to Local directory */
    public void loadParseTrajectoryData() {
	    String inputDataPath   = "/path/to/trajectory-data-folder";
		String dataFormatPath  = "/path/to/input-data-format.txt";
		
		/* setup output directory */
		String outputDataPath  = "/path/to/output-data-folder";
		LocalDBParameters params =
				new LocalDBParameters(outputDataPath)

		/* output data formats: ALL, SPATIAL, SPATIAL-TEMPORAL */
		OutputFormat outputFormat = OutputFormat.ALL;
		 
		/* read input data format file */
		List<String> inputDataFormat = 
				new IOService().readFile(dataFormatPath);
			
		/* run parser and save data to local directory */
		TrajectoryParser parser = 
				new TrajectoryParser(inputDataPath, inputDataFormat);
		parser.parseToLocal(outputFormat, params);
	}
```

In **Example 1** the application will output the parsed data as `.CSV` files, along with the `output-format.conf` and `metadata.meta` in the given `outputDataPath` directory.

**Ex 2:** Load, parse and output Trajectory and Map data to **MongoDB** 

*	You must start the Mongo database service i n your machine first.
*	Start your mongodb.
	`mongod --dbpath /your/db/path`

``` 
import traminer.io.IOService;
import traminer.io.params.MongoDBParameters;
import traminer.loader.map.OsmHandlerFactory;
import traminer.loader.map.OsmHandler;
import traminer.parser.TrajectoryParser;
import traminer.parser.analyzer.Keywords.OutputFormat;
. . .
	/** Load and parse trajectory data to MongoDB */
    public void loadParseTrajectoryData() {
	    String inputDataPath   = "/path/to/trajectory-data-folder";
		String dataFormatPath  = "/path/to/input-data-format.txt";
		
		/* setup MongoDB parameters */
		String mongoHost 	= "localhost";
		int mongoPort  		= 27017;
		String mongoDbName	= "traminerdb";
		MongoDBParameters params = 
				new MongoDBParameters(mongoHost, mongoPort, mongoDbName);
		
		/* output data formats: ALL, SPATIAL, SPATIAL-TEMPORAL */
		OutputFormat outputFormat = OutputFormat.ALL;
		
		/* read input data format file */
		List<String> inputDataFormat = 
				new IOService().readFile(dataFormatPath);
			
		/* run parser and save data to MongoDB */
		TrajectoryParser parser = 
				new TrajectoryParser(inputDataPath, inputDataFormat);
		parser.parseToMongoDB(outputFormat, params);
	}
```

In **Example 2** the application will output the parsed data, the *Ouput Data Format*, and the metadata into the default MongoDB collections: `"trajectorydata"` and `"metadata"`. 



[de9im]: <http://en.wikipedia.org/wiki/DE-9IM/>
[weka]: <http://www.cs.waikato.ac.nz/ml/weka/index.html>
[elki]: <http://elki.dbs.ifi.lmu.de/>
[gstream]: <http://graphstream-project.org/>
[jgrapht]: <http://jgrapht.org/>
[geotools]: <http://www.geotools.org/>
[unfolding]: <http://unfoldingmaps.org/>
[java8]: <http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html>
[maven]: <https://maven.apache.org/>
[eclipse]: <https://eclipse.org/downloads/>
[anaconda]: <https://www.continuum.io/downloads/>
[pydev]: <http://www.pydev.org/>
[python]: <https://www.python.org/downloads/>
[traminergit]: <https://github.com/Hellisk/TraMiner.git>
[cdt]: <https://eclipse.org/cdt/downloads.php>
[mingw]: <http://www.mingw.org/>
[cygwin]: <https://cygwin.com/install.html>
[mongodb-download]: <https://www.mongodb.com/download-center>
[mongodb-install]: <https://docs.mongodb.com/manual/installation/>
[traminer-util]: <https://github.com/Hellisk/TraMiner/tree/dev/traminer-util-lib>
[traminer-io]: <https://github.com/Hellisk/TraMiner/tree/dev/traminer-io-lib>
[javafx]: <http://docs.oracle.com/javase/8/javafx/get-started-tutorial/jfx-overview.htm>
[efxclipse]: <http://wiki.eclipse.org/Efxclipse/Tutorials/AddingE(fx)clipse_to_eclipse>
[scene-builder]: <http://gluonhq.com/labs/scene-builder/>
[intellij]: <https://www.jetbrains.com/idea/>
[netbeans]: <https://netbeans.org/>
[jython]: <http://www.jython.org/>
[jython-tutorial]: <http://www.jython.org/jythonbook/en/1.0/index.html>
[pymongo]: <https://api.mongodb.com/python/current/>
