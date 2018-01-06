package traminer.test.parser;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import junit.framework.TestCase;
import traminer.parser.MetadataService;
import traminer.util.Printable;
import traminer.util.spatial.SpatialUtils;
import traminer.util.spatial.distance.EuclideanDistanceFunction;
import traminer.util.trajectory.Trajectory;

/**
 * Unit test for the {@link MetadataService}.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class __MetadataTest__ extends TestCase implements Printable {
	// Arrays containing the (x,y,time) values of the trajectories
	private double[] x1Values, y1Values, t1Values;
	private double[] x2Values, y2Values, t2Values;
	private double[] x3Values, y3Values, t3Values;
	private double[] xDelta, yDelta, tDelta;
	private Trajectory t1 = new Trajectory("t1");
	private Trajectory t2 = new Trajectory("t2");
	private Trajectory t3 = new Trajectory("t3");
	private Trajectory td = new Trajectory("td");
	// Service to test
	private MetadataService metadataService;
	private MetadataService metadataServiceDelta;
	
    /**
     * Run by JUnit before each test to initialize variables.
     */
    @Before
    public void setUp() {
    	// trajectory 1
    	x1Values = new double[]{0,2,4,6,8};
    	y1Values = new double[]{1,3,5,7,9};
    	t1Values = new double[]{10,20,30,40,50};
    	t1.add(0, 1, 10);
		t1.add(2, 3, 20);
		t1.add(4, 5, 30);
		t1.add(6, 7, 40);
		t1.add(8, 9, 50); 
		
    	// trajectory 2
    	x2Values = new double[]{10,12,14,16,18,20};
    	y2Values = new double[]{9,7,5,3,1,0};
    	t2Values = new double[]{10,20,30,40,50,60};
    	t2.add(10, 9, 10);
		t2.add(12, 7, 20);
		t2.add(14, 5, 30);
		t2.add(16, 3, 40);
		t2.add(18, 1, 50);
		t2.add(20, 0, 60);
		
    	// trajectory 3
    	x3Values = new double[]{10,20,30,40};
    	y3Values = new double[]{1,1,1,1};
    	t3Values = new double[]{10,20,30,40};
		t3.add(10, 1, 10);
		t3.add(20, 1, 20);
		t3.add(30, 1, 30);
		t3.add(40, 1, 40);
		
    	// init service
    	metadataService = new MetadataService();
    	metadataService.addValues(x1Values, false, y1Values, false, t1Values, false);
    	metadataService.addValues(x2Values, false, y2Values, false, t2Values, false);
    	metadataService.addValues(x3Values, false, y3Values, false, t3Values, false);
    
    	// trajectory in Delta compression
    	xDelta = new double[]{10,0.1,0.4,0.5,1.0}; // 10.0 10.1 10.5 11.0 12.0
    	yDelta = new double[]{20,2.1,2.4,2.5,3.0}; // 20.0 22.1 24.5 27.0 30.0
    	tDelta = new double[]{10,10,10,10,10};     // 10.0 20.0 30.0 40.0 50.0
    	td.add(10,  20,  10);
		td.add(0.1, 2.1, 10);
		td.add(0.4, 2.4, 10);
		td.add(0.5, 2.5, 10);
		td.add(1.0, 1.0, 10); 
		
    	metadataServiceDelta = new MetadataService();
    	metadataServiceDelta.addValues(xDelta, true, yDelta, true, tDelta, true);
    }

	@Test
    public void testCount() throws Exception {
		long trajCount = metadataService.getTrajectoriesCount();
		long ptsCount  = metadataService.getPointsCount();
		
		Assert.assertEquals(3, trajCount);
		Assert.assertEquals(15, ptsCount);
	}
	
	@Test
    public void testNumPoints() throws Exception {
		double[] numPts = metadataService.getNumPointsStats();
		double[] expected = new double[] {4,6,5,1};
		
		Assert.assertArrayEquals(expected, numPts, 0.0001);
	}
	
	@Test
    public void testLength() throws Exception {
		double t1Len = t1.length(new EuclideanDistanceFunction());	
		double t2Len = t2.length(new EuclideanDistanceFunction());	
		double t3Len = t3.length(new EuclideanDistanceFunction());	
    	double mean = SpatialUtils.getMean(t1Len, t2Len, t3Len);
    	double std  = SpatialUtils.getStd (t1Len, t2Len, t3Len);

		double[] stats = metadataService.getLengthStats();
		double[] expected = new double[] {t1Len,t3Len,mean,std};
		
		Assert.assertArrayEquals(expected, stats, 0.0001);
	}
	
	@Test
    public void testDuration() throws Exception {
		double t1Dur = t1.duration();	
		double t2Dur = t2.duration();	
		double t3Dur = t3.duration();	
    	double mean = SpatialUtils.getMean(t1Dur, t2Dur, t3Dur);
    	double std  = SpatialUtils.getStd (t1Dur, t2Dur, t3Dur);

		double[] stats = metadataService.getDurationStats();
		double[] expected = new double[] {t3Dur,t2Dur,mean,std};
		
		Assert.assertArrayEquals(expected, stats, 0.0001);		
	}
	
	@Test
    public void testSpeed() throws Exception {
		double t1Speed = t1.speed(new EuclideanDistanceFunction());	
		double t2Speed = t2.speed(new EuclideanDistanceFunction());
		double t3Speed = t3.speed(new EuclideanDistanceFunction());
    	double mean = SpatialUtils.getMean(t1Speed, t2Speed, t3Speed);
    	double std  = SpatialUtils.getStd (t1Speed, t2Speed, t3Speed);

		double[] stats = metadataService.getSpeedStats();
		double[] expected = new double[] {t2Speed,t3Speed,mean,std};
		
		Assert.assertArrayEquals(expected, stats, 0.0001);	
	}

	@Test
    public void testSamplingRate() throws Exception {
		double t1Sampling = t1.samplingRate();
		double t2Sampling = t2.samplingRate();
		double t3Sampling = t3.samplingRate();
    	double mean = SpatialUtils.getMean(t1Sampling, t2Sampling, t3Sampling);
    	double std  = SpatialUtils.getStd (t1Sampling, t2Sampling, t3Sampling);

		double[] stats = metadataService.getSamplingRateStats();
		double[] expected = new double[] {t2Sampling,t3Sampling,mean,std};

		Assert.assertArrayEquals(expected, stats, 0.0001);		
	}

	@Test
    public void testCoverage() throws Exception {
		double[] stats = metadataService.getCoverageStats();
		double[] expected = new double[] {0,0,10,40,9,60};
		
		Assert.assertArrayEquals(expected, stats, 0.0001);		
	}
	
	@Test
    public void testScript() throws Exception {
		String stats = metadataService.getMetadata();
		
		print(stats);	
	}
	
	@Test
    public void testCoverageDelta() throws Exception {
		double[] stats = metadataServiceDelta.getCoverageStats();
		double[] expected = new double[] {10,20,10,12,30,50};

		Assert.assertArrayEquals(expected, stats, 0.0001);		
	}
}
