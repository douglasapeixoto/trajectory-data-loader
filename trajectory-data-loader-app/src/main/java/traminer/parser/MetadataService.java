package traminer.parser;

import java.math.RoundingMode;
import java.text.DecimalFormat;

import traminer.parser.analyzer.Keywords;
import traminer.util.DeltaEncoder;
import traminer.util.math.Decimal;
import traminer.util.spatial.SpatialInterface;
import traminer.util.spatial.distance.EuclideanDistanceFunction;
import traminer.util.spatial.distance.PointDistanceFunction;

/**
 * Metadata management and dataset statistics service.
 * This service computes the metadata by processing
 * one input trajectory at time and updating the metadata
 * statistics after each addition.
 * 
 * @author douglasapeixoto
 */
@SuppressWarnings("serial")
public final class MetadataService implements SpatialInterface {
	// Point distance measure to use
	private static PointDistanceFunction distFunc;
	// Total Number of trajectories processed
	private static long trajectoriesCount = 0;
	// Total number of trajectory points processed
	private static long pointsCount = 0;
	// Total number of data files processed
	private static long filesCount = 0;
	
	// Arrays containing the (x,y,time) values of the 
	// current trajectory
	private static Decimal[] xValues, yValues, tValues;
	// Number of points in the current trajectory
	private static int numPts = 0;
	
	// Statics about the total number of points
	private static Decimal numPtsSum = new Decimal(0);
	private static Decimal numPtsSqr = new Decimal(0);
	private static Decimal numPtsMin  = new Decimal(INFINITY);
	private static Decimal numPtsMax  = new Decimal(-INFINITY);
	// Statics about the length of the trajectories
	private static Decimal lengthSum = new Decimal(0);
	private static Decimal lengthSqr = new Decimal(0);
	private static Decimal lengthMin = new Decimal(INFINITY);
	private static Decimal lengthMax = new Decimal(-INFINITY);
	// Statics about the time duration of the trajectories
	private static Decimal durationSum = new Decimal(0);
	private static Decimal durationSqr = new Decimal(0);
	private static Decimal durationMin = new Decimal(INFINITY);
	private static Decimal durationMax = new Decimal(-INFINITY);
	// Statics about the speed of the trajectories
	private static Decimal speedSum = new Decimal(0);
	private static Decimal speedSqr = new Decimal(0);
	private static Decimal speedMin = new Decimal(INFINITY);
	private static Decimal speedMax = new Decimal(-INFINITY);
	// Statics about the sampling rate of the trajectories
	private static Decimal samplingSum = new Decimal(0);
	private static Decimal samplingSqr = new Decimal(0);
	private static Decimal samplingMin = new Decimal(INFINITY);
	private static Decimal samplingMax = new Decimal(-INFINITY);
	// Statics about the spatial-temporal coverage of the trajectories
	private static Decimal minX = new Decimal(INFINITY);
	private static Decimal minY = new Decimal(INFINITY);
	private static Decimal minT = new Decimal(INFINITY);
	private static Decimal maxX = new Decimal(-INFINITY);
	private static Decimal maxY = new Decimal(-INFINITY);
	private static Decimal maxT = new Decimal(-INFINITY);

	/**
	 * Initialize this service using the default Euclidean 
	 * distance function.
	 */
	public static void init() {
		distFunc = new EuclideanDistanceFunction();
	}
	
	/**
	 * Initialize this service using the given distance function.
	 * 
	 * @param distFunc Point distance measure to use to calculate
	 * the statics about the trajectory data.
	 */
	public static void init(PointDistanceFunction distFunc) {
		MetadataService.distFunc = distFunc;
	}

	/**
	 * Add trajectory spatial-temporal data to this metadata.
	 * Re-calculate the statistics and update the metadada.
	 * 
	 * @param xValues Array containing the X coordinates of 
	 * 		the trajectory points as a String.
	 * @param isDeltaX If the array X is delta-encoded.
	 * @param xType The type of the X values.
	 * @param yValues Array containing the Y coordinates of 
	 * 		the trajectory points as a String.
	 * @param isDeltaY If the array Y is delta-encoded.
	 * @param yType The type of the Y values.
	 * @param tValues Array containing the Time values of 
	 * 		the trajectory points as a String.
	 * @param isDeltaT If the array T is delta-encoded.
	 * @param tType The type of the T values.
	 * 
	 * @throws IllegalArgumentException
	 */
	public static synchronized void addValues(
			String[] xValues, boolean isDeltaX, String xType,
			String[] yValues, boolean isDeltaY, String yType,
			String[] tValues, boolean isDeltaT, String tType) 
			throws IllegalArgumentException {		
		if (xValues == null || yValues == null || tValues == null) {
			throw new NullPointerException("Values for metadata "
					+ "computation must not be null.");
		}
		if (xValues.length == 0 || yValues.length == 0 || tValues.length == 0) {
			throw new IllegalArgumentException("Values for metadata "
					+ "computation must not be empty.");
		}
		if (xValues.length != yValues.length || xValues.length != tValues.length) {
			throw new IllegalArgumentException("Arrays with values for "
					+ "metadata computation must be of same size.");
		}
		
		numPts = xValues.length;
		// decompress (if not) and parse if possible
		String[] newValues;
		if (isDeltaX) {
			newValues = DeltaEncoder.deltaDecode(xValues);
			MetadataService.xValues = parseToDecimal(newValues);
		} else if (Keywords.isNumberType(xType)) {
			MetadataService.xValues = parseToDecimal(xValues);
		} else {
			MetadataService.xValues = new Decimal[numPts];
			for (int i=0; i<numPts; i++) {
				MetadataService.xValues[i] = new Decimal(0);
			}
		}
		if (isDeltaY) {
			newValues = DeltaEncoder.deltaDecode(yValues);
			MetadataService.yValues = parseToDecimal(newValues);
		} else if (Keywords.isNumberType(yType)){
			MetadataService.yValues = parseToDecimal(yValues);
		} else {
			MetadataService.yValues = new Decimal[numPts];
			for (int i=0; i<numPts; i++) {
				MetadataService.yValues[i] = new Decimal(0);
			}
		}
		if (isDeltaT) {
			newValues = DeltaEncoder.deltaDecode(tValues);
			MetadataService.tValues = parseToDecimal(newValues);
		} else if (Keywords.isNumberType(tType)){
			MetadataService.tValues = parseToDecimal(tValues);
		} else {
			MetadataService.tValues = new Decimal[numPts];
			for (int i=0; i<numPts; i++) {
				MetadataService.tValues[i] = new Decimal(0);
			}
		}
		
		// update dataset statistics for this new trajectory
		updateStatistics();
	}

	/**
	 * Add trajectory spatial-temporal data to this metadata.
	 * Re-calculate the statistics and update the metadada.
	 * 
	 * @param xValues Array containing the X coordinates of 
	 * 		the trajectory points.
	 * @param isDeltaX If the array X is delta-encoded.
	 * @param yValues Array containing the Y coordinates of 
	 * 		the trajectory points.
	 * @param isDeltaY If the array Y is delta-encoded.
	 * @param tValues Array containing the Time values of 
	 * 		the trajectory points.
	 * @param isDeltaT If the array T is delta-encoded.
	 * 
	 * @throws IllegalArgumentException
	 */
	public static synchronized void addValues(
			double[] xValues, boolean isDeltaX,
			double[] yValues, boolean isDeltaY,
			double[] tValues, boolean isDeltaT) 
			throws IllegalArgumentException {
		if (xValues == null || yValues == null || tValues == null) {
			throw new NullPointerException("Values for metadata "
					+ "computation must not be null.");
		}
		if (xValues.length == 0 || yValues.length == 0 || tValues.length == 0) {
			throw new IllegalArgumentException("Values for metadata "
					+ "computation must not be empty.");
		}
		if (xValues.length != yValues.length || xValues.length != tValues.length) {
			throw new IllegalArgumentException("Arrays with values for "
					+ "metadata computation must be of same size.");
		}
		
		numPts = xValues.length;
		// decompress (if not)
		double[] newValues;
		if (isDeltaX) {
			newValues = DeltaEncoder.deltaDecode(xValues);
			MetadataService.xValues = parseToDecimal(newValues);
		} else {
			MetadataService.xValues = parseToDecimal(xValues);
		}
		if (isDeltaY) {
			newValues = DeltaEncoder.deltaDecode(yValues);
			MetadataService.yValues = parseToDecimal(newValues);
		} else {
			MetadataService.yValues = parseToDecimal(yValues);
		}		
		if (isDeltaT) {
			newValues = DeltaEncoder.deltaDecode(tValues);
			MetadataService.tValues = parseToDecimal(newValues);
		} else {
			MetadataService.tValues = parseToDecimal(tValues);
		}

		// update dataset statistics for this new trajectory
		updateStatistics();
	}

	/**
	 * Parse this array of String to Decimal values.
	 * 
	 * @param values Values to parse.
	 * @return Array containing the parsed String numbers to Decimal numbers.
	 * 
	 * @throws NumberFormatException If the values cannot be parsed to Decimal.
	 */
	private static Decimal[] parseToDecimal(String[] values) throws NumberFormatException {
		int size = values.length;
		Decimal[] result = new Decimal[size];
		for (int i=0; i<size; i++) {
			result[i] = new Decimal(values[i]);
		}
		return result;
	}

	/**
	 * Parse this array of doubles to Decimal values.
	 * 
	 * @param values Values to parse.
	 * @return Array containing the parsed values to Decimal numbers.
	 * 
	 * @throws NumberFormatException If the values cannot be parsed to Decimal.
	 */
	private static Decimal[] parseToDecimal(double[] values) throws NumberFormatException {
		int size = values.length;
		Decimal[] result = new Decimal[size];
		for (int i=0; i<size; i++) {
			result[i] = new Decimal(values[i]);
		}
		return result;
	}
	/**
	 * Update the metadata statistics for every new input trajectory.
	 * <br> [0]: mean [1]: min [2]: max [3]: std
	 */
	private static void updateStatistics() {
		// get statistics of this trajectory points
		// using the provided distance measure
		double distance;
		Decimal length   = new Decimal(0);
		Decimal duration = new Decimal(0);
		for (int i=0; i<numPts-1; i++) {
			distance = distFunc.pointToPointDistance(
					xValues[i].value(), yValues[i].value(), 
					xValues[i+1].value(), yValues[i+1].value());
			// trajectory's length and duration
			length   = length.sum(distance);
			duration = duration.sum(tValues[i+1].sub(tValues[i]));
			
			minX = minX.min(xValues[i]);
			minY = minY.min(yValues[i]);
			minT = minT.min(tValues[i]);
			maxX = maxX.max(xValues[i]);
			maxY = maxY.max(yValues[i]);
			maxT = maxT.max(tValues[i]);
		}
		
		numPtsSum = numPtsSum.sum(numPts); 
		numPtsSqr = numPtsSqr.sum(numPts*numPts);
		numPtsMin = numPtsMin.min(numPts); 
		numPtsMax = numPtsMax.max(numPts);
			
		lengthSum = lengthSum.sum(length);
		lengthSqr = lengthSqr.sum(length.multiply(length));
		lengthMin = lengthMin.min(length); 
		lengthMax = lengthMax.max(length); 
	
		durationSum = durationSum.sum(duration); 
		durationSqr = durationSqr.sum(duration.multiply(duration));
		durationMin = durationMin.min(duration); 
		durationMax = durationMax.max(duration);
		
		Decimal speed = duration.equals(0) ? 
				new Decimal(0) : length.divide(duration);
		speedSum = speedSum.sum(speed);
		speedSqr = speedSqr.sum(speed.multiply(speed));
		speedMin = speedMin.min(speed); 
		speedMax = speedMax.max(speed);
	 		
		Decimal sampling = duration.divide(numPts-1);
		samplingSum = samplingSum.sum(sampling); 
		samplingSqr = samplingSqr.sum(sampling.multiply(sampling));
		samplingMin = samplingMin.min(sampling); 
		samplingMax = samplingMax.max(sampling);
		
		minX = minX.min(xValues[numPts-1]);
		minY = minY.min(yValues[numPts-1]);
		minT = minT.min(tValues[numPts-1]);
		maxX = maxX.max(xValues[numPts-1]);
		maxY = maxY.max(yValues[numPts-1]);
		maxT = maxT.max(tValues[numPts-1]);
		
		trajectoriesCount++;
		pointsCount += numPts;
	}
	
	/**
	 * @return Total number of data files processed.
	 */
	public static long getFilesCount() {
		return filesCount;
	}

	/**
	 * @param count Total number of data files
	 *  in the dataset to process.
	 */
	public static void setFilesCount(long count) {
		filesCount = count;
	}

	/**
	 * @return Number of trajectories processed so far 
	 * from the input dataset.
	 */
	public static long getTrajectoriesCount(){
		return trajectoriesCount;
	}
	
	/**
	 * @return Number of trajectory sample points processed
	 * so far from the input dataset.
	 */
	public static long getPointsCount(){
		return pointsCount;
	}

	/**
	 * Statistics about the average number of the points per 
	 * trajectory in this dataset.
	 * 
	 * @return An array containing the average points per trajectory 
	 * in this dataset. 
	 * <p> [0]: min [1]: max [2]: mean [3]: std
	 */
	public static double[] getNumPointsStats() {
		if (trajectoriesCount == 0) {
			return new double[4];
		}
		Decimal mean = numPtsSum.divide(trajectoriesCount);
		Decimal stdSqr = numPtsSqr.sub(numPtsSum.pow2().divide(trajectoriesCount)).abs();
		Decimal std = stdSqr.divide(trajectoriesCount).sqrt();
		
		// min, max, mean, std
		double[] result = new double[] {
				numPtsMin.value(), numPtsMax.value(), 
				mean.value(), std.value()
		};
				
		return result;
	}
	
	/**
	 * Statistics about the average length of the trajectories 
	 * in the dataset.
	 * 
	 * @return Array containing statistics on the trajectories length.
	 * <p> [0]: min [1]: max [2]: mean [3]: std
	 */
	public static double[] getLengthStats() {
		if (trajectoriesCount == 0) {
			return new double[4];
		}
		Decimal mean   = lengthSum.divide(trajectoriesCount);
		Decimal stdSqr = lengthSqr.sub(lengthSum.pow2().divide(trajectoriesCount)).abs();
		Decimal std    = stdSqr.divide(trajectoriesCount).sqrt();
		
		// min, max, mean, std
		double[] result = new double[] {
				lengthMin.value(), lengthMax.value(), 
				mean.value(), std.value()
		};
		
		return result;
	}	
	
	/**
	 * Statistics about the average time duration of the  
	 * trajectories in the dataset.
	 * 
	 * @return Array containing statistics on the trajectories duration.
	 * <p> [0]: min [1]: max [2]: mean [3]: std
	 */
	public static double[] getDurationStats() {
		if (trajectoriesCount == 0) {
			return new double[4];
		}
		Decimal mean   = durationSum.divide(trajectoriesCount);
		Decimal stdSqr = durationSqr.sub(durationSum.pow2().divide(trajectoriesCount)).abs();
		Decimal std    = stdSqr.divide(trajectoriesCount).sqrt();
		
		// min, max, mean, std
		double[] result = new double[] {
				durationMin.value(), durationMax.value(), 
				mean.value(), std.value()
		};
		
		return result;
	}

	/**
	 * Statistics about the average speed of the trajectories 
	 * in the dataset.
	 * 
	 * @return Array containing statistics on the trajectories speed.
	 * <p> [0]: min [1]: max [2]: mean [3]: std
	 */
	public static double[] getSpeedStats() {
		if (trajectoriesCount == 0) {
			return new double[4];
		}
		Decimal mean   = speedSum.divide(trajectoriesCount);
		Decimal stdSqr = speedSqr.sub(speedSum.pow2().divide(trajectoriesCount)).abs();
		Decimal std    = stdSqr.divide(trajectoriesCount).sqrt();
		
		// min, max, mean, std
		double[] result = new double[] {
				speedMin.value(), speedMax.value(), 
				mean.value(), std.value()
		};
		
		return result;
	}

	/**
	 * Statistics about the average sampling rate of the  
	 * trajectories in the dataset.
	 * 
	 * @return Array containing statistics on the trajectories 
	 * sampling rate.
	 * <p> [0]: min [1]: max [2]: mean [3]: std
	 */
	public static double[] getSamplingRateStats() {
		if (trajectoriesCount == 0) {
			return new double[4];
		}
		Decimal mean   = samplingSum.divide(trajectoriesCount);
		Decimal stdSqr = samplingSqr.sub(samplingSum.pow2().divide(trajectoriesCount)).abs();
		Decimal std    = stdSqr.divide(trajectoriesCount).sqrt();
		
		// min, max, mean, std
		double[] result = new double[] {
				samplingMin.value(), samplingMax.value(), 
				mean.value(), std.value()
		};
		
		return result;
	}

	/**
	 * Statistics about the spatial-temporal coverage of the  
	 * trajectories in the dataset.
	 * 
	 * @return Array containing statistics on the trajectories 
	 * spatial-temporal coverage.
	 * <p>	[0]: min X [1]: min Y [2]: min Time 
	 * 	    [3]: max X [4]: max Y [5]: max Time
	 */
	public static double[] getCoverageStats() {
		if (trajectoriesCount == 0) {
			return new double[6];
		}
		
		double[] result = new double[] {
				minX.value(), minY.value(), minT.value(),
				maxX.value(), maxY.value(), maxT.value()
		};
		
		return result;
	}
	
	/**
	 * @return A String text with the statistics about 
	 * data collected so far in this dataset.
	 */
	public static String getMetadata() {
     	String script = "";
		if (trajectoriesCount == 0) {
			return script;
		}
		
     	long numTraj = getTrajectoriesCount();
     	long numPts  = getPointsCount();
     	double[] avgPts = getNumPointsStats();
     	double[] avgDur = getDurationStats();
     	double[] avgLen = getLengthStats();
     	double[] avgSpeed = getSpeedStats();
     	double[] avgRate  = getSamplingRateStats();
     	double[] cover  = getCoverageStats();

     	// Format of the numbers in the output statistics
     	final DecimalFormat df = new DecimalFormat("#.#####");
     	df.setRoundingMode(RoundingMode.HALF_EVEN);
     	
     	// create metadata script
     	script += "NUM_TRAJECTORIES\t" + numTraj + "\n";
     	script += "NUM_POINTS\t" 	   + numPts  + "\n";
     	
     	script += "MIN_PTS_PER_TRAJECTORY\t" + avgPts[0] + "\n";
     	script += "MAX_PTS_PER_TRAJECTORY\t" + avgPts[1] + "\n";
     	script += "AVG_PTS_PER_TRAJECTORY\t" + df.format(avgPts[2]) + "\n";
     	script += "STD_PTS_PER_TRAJECTORY\t" + df.format(avgPts[3]) + "\n";
     	
     	script += "MIN_TRAJECTORY_LENGTH\t" + df.format(avgLen[0]) + "\n";
     	script += "MAX_TRAJECTORY_LENGTH\t" + df.format(avgLen[1]) + "\n";
     	script += "AVG_TRAJECTORY_LENGTH\t" + df.format(avgLen[2]) + "\n";
     	script += "STD_TRAJECTORY_LENGTH\t" + df.format(avgLen[3]) + "\n";
     	
     	script += "MIN_TRAJECTORY_DURATION\t" + df.format(avgDur[0]) + "\n"; 
     	script += "MAX_TRAJECTORY_DURATION\t" + df.format(avgDur[1]) + "\n"; 
     	script += "AVG_TRAJECTORY_DURATION\t" + df.format(avgDur[2]) + "\n";
     	script += "STD_TRAJECTORY_DURATION\t" + df.format(avgDur[3]) + "\n";
     	
     	script += "MIN_TRAJECTORY_SPEED\t" + df.format(avgSpeed[0]) + "\n";
     	script += "MAX_TRAJECTORY_SPEED\t" + df.format(avgSpeed[1]) + "\n";
     	script += "AVG_TRAJECTORY_SPEED\t" + df.format(avgSpeed[2]) + "\n";
     	script += "STD_TRAJECTORY_SPEED\t" + df.format(avgSpeed[3]) + "\n";
     	
     	script += "MIN_SAMPLING_RATE\t" + df.format(avgRate[0]) + "\n";
     	script += "MAX_SAMPLING_RATE\t" + df.format(avgRate[1]) + "\n";
     	script += "AVG_SAMPLING_RATE\t" + df.format(avgRate[2]) + "\n";
     	script += "STD_SAMPLING_RATE\t" + df.format(avgRate[3]) + "\n";
     	
     	script += "MIN_X\t" + cover[0] + "\n";
     	script += "MIN_Y\t" + cover[1] + "\n";
     	script += "MIN_T\t" + cover[2] + "\n";
     	script += "MAX_X\t" + cover[3] + "\n";
     	script += "MAX_Y\t" + cover[4] + "\n";
     	script += "MAX_T\t" + cover[5];
    	
     	return script;
	}
}
