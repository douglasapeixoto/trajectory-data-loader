package traminer.loader.generator;

import java.util.stream.Stream;

import traminer.io.params.HDFSParameters;
import traminer.io.params.LocalFSParameters;
import traminer.io.params.MongoDBParameters;
import traminer.parser.DataWriter;
import traminer.parser.MetadataService;
import traminer.parser.ParserInterface;
import traminer.parser.analyzer.Keywords;
import traminer.util.DeltaEncoder;
import traminer.util.math.Decimal;
import traminer.util.spatial.distance.EuclideanDistanceFunction;
import traminer.util.trajectory.SyntheticTrajectoryGenerator;
import traminer.util.trajectory.Trajectory;

// generator model
@SuppressWarnings("serial")
public class TrajectoryGenerator implements ParserInterface {
	private int quantity; 
	private int minPts, maxPts; 
	private double minX, minY, maxX, maxY;
	private long minStartTime, maxStartTime, timeRate;
	
	/** Default output data format for synthetic data */
	private final static String OUTPUT_FORMAT =
			"_OUTPUT_FORMAT	SPATIAL_TEMPORAL\n"+
			"_COORD_SYSTEM	CARTESIAN\n"+
			"_DECIMAL_PREC	5\n"+
			"_SPATIAL_DIM	2\n"+
			"_ID				STRING\n"+
			"_COORDINATES	ARRAY(_X DECIMAL _Y DECIMAL _TIME INTEGER)";
	
	/**
	 * @param quantity The number of synthetic trajectories to generate.
	 * @param minPts The minimum number of sample points in each trajectory.
	 * @param maxPts The maximum number of sample points in each trajectory.
	 * @param minX The lowest possible value for the X coordinate of each sample point.
	 * @param minY The lowest possible value for the Y coordinate of each sample point.
	 * @param maxX The highest possible value for the X coordinate of each sample point.
	 * @param maxY The highest possible value for the Y coordinate of each sample point.
	 * @param minStartTime The lowest possible start time of each trajectory. 
	 * @param maxStartTime The highest possible start time of each trajectory. 
	 * @param timeRate The time/sampling rate of each trajectory point.
	 */
	public TrajectoryGenerator(int quantity, int minPts, int maxPts, double minX, double minY, double maxX, double maxY,
			long minStartTime, long maxStartTime, long timeRate) {
		this.quantity = quantity;
		this.minPts = minPts;
		this.maxPts = maxPts;
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
		this.minStartTime = minStartTime;
		this.minStartTime = maxStartTime;
		this.timeRate = timeRate;
		// generate data in the Euclidean space only
		MetadataService.init(new EuclideanDistanceFunction());
	}

	public boolean generateToLocal(LocalFSParameters params) {
		try {
			Stream<String> trajectoryStream = generate();
			DataWriter.init(params);
			DataWriter.saveDataFile(trajectoryStream);
			DataWriter.saveOutputFormatFile(OUTPUT_FORMAT);
			DataWriter.saveMetadataFile(MetadataService.getMetadata());
			
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public boolean generateToMongoDB(MongoDBParameters mongoParams) {
		try {
			Stream<String> trajectoryStream = generate();
			DataWriter.init(mongoParams);
			DataWriter.saveDataFile(trajectoryStream);
			DataWriter.saveOutputFormatFile(OUTPUT_FORMAT);
			DataWriter.saveMetadataFile(MetadataService.getMetadata());
			
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public boolean generateToHDFS(HDFSParameters hdfsParams) {
		try {
			DataWriter.init(hdfsParams);
			Stream<String> trajectoryStream = generate();
			DataWriter.saveDataFile(trajectoryStream);
			DataWriter.saveOutputFormatFile(OUTPUT_FORMAT);
			DataWriter.saveMetadataFile(MetadataService.getMetadata());
			
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	private synchronized Stream<String> generate(){
		Stream<Trajectory> syntheticData = SyntheticTrajectoryGenerator
				.generateStream(
					quantity, minPts, maxPts, 
					minX, minY, maxX, maxY, 
					minStartTime, maxStartTime, timeRate);
			
		// map every trajectory to the standard format
		Stream<String> trajectoryStream = 
				syntheticData.map(t -> {
				String[] xList = compressDoubleValues(t.getXValues());
				String[] yList = compressDoubleValues(t.getYValues());
				String[] tList = compressLongValues(t.getTimeValues());
	
				// update metadata
				MetadataService.addValues(
						xList, true, Keywords.DECIMAL.name(),
						yList, true, Keywords.DECIMAL.name(),
						tList, true, Keywords.INTEGER.name());
				
				String s = "";
				for (int i=0; i<xList.length; i++) {
					s += ","+ xList[i] +","+ yList[i] +","+ tList[i];
				}
				s = t.getId() + ";" + s.substring(1);
				
				return s;
			});

		return trajectoryStream;
	}
	
	/**
	 * Compress the array of values using Delta compression
	 * <p>
	 * Return the compressed values as Integer  (value * 10^DECIMAL_PRECISION)
	 * 
	 * @param values The array of values to compress.
	 * 
	 * @return The array of values in delta-compression.
	 */
	private synchronized String[] compressDoubleValues(double[] values) {
		// delta compression
		values = DeltaEncoder.deltaEncode(values);
		// convert and save values to integer with given 
		// precision ( value * 10^DECIMAL_PRECISION )
		String[] compressedVals = new String[values.length];
		Decimal round = Decimal.valueOf(Math.pow(10, DEFAULT_DECIMAL_PREC));
		Decimal intVal;
		for (int i=0; i<values.length; i++) {
			intVal = Decimal.valueOf(values[i]).multiply(round);
			compressedVals[i] = ""+intVal.longValue();
		}
		
		return compressedVals;
	}
	
	/**
	 * Compress the array of values using Delta compression
	 * 
	 * @param values The array of values to compress.
	 * 
	 * @return The array of values in delta-compression.
	 */
	private synchronized String[] compressLongValues(long[] values) {
		// delta compression
		values = DeltaEncoder.deltaEncode(values);
		String[] compressedVals = new String[values.length];
		Decimal intVal;
		for (int i=0; i<values.length; i++) {
			intVal = Decimal.valueOf(""+values[i]);
			compressedVals[i] = ""+intVal.longValue();
		}
		
		return compressedVals;
	}
}
