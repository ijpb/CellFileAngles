/**
 * 
 */
package ijt.cellangles;

import static org.junit.Assert.*;

import java.awt.geom.Point2D;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

/**
 * @author dlegland
 *
 */
public class CellFileTest
{

	/**
	 * Test method for {@link ijt.cellangles.CellFile#computeLabelList(ij.process.ImageProcessor)}.
	 */
	@Test
	public void testComputeLabelList_Mock()
	{
		// Create simple demo image
		ImageProcessor image = new ByteProcessor(13, 6);
		for (int y = 0; y < 4; y++)
		{
			for (int x = 0; x < 2; x++)
			{
				image.set(x + 1, y + 1,  4);
				image.set(x + 4, y + 1,  7);
				image.set(x + 7, y + 1, 10);
				image.set(x + 10, y + 1, 15);
			}
		}

		// create default cell file
		CellFile cellFile = new CellFile("dummy", true);
		
		// Add waypoints
		List<Point2D> wayPoints = new ArrayList<Point2D>(3);
		wayPoints.add(new Point2D.Double(2, 2));
		wayPoints.add(new Point2D.Double(7, 4));
		wayPoints.add(new Point2D.Double(11, 2));
		cellFile.setWayPoint(wayPoints);
		
		List<Integer> labels = cellFile.computeLabelList(image);
		
		assertTrue(labels != null);
		assertEquals(4, labels.size());
		assertEquals( 4, (int) labels.get(0));
		assertEquals( 7, (int) labels.get(1));
		assertEquals(10, (int) labels.get(2));
		assertEquals(15, (int) labels.get(3));
	}

	/**
	 * Test method for {@link ijt.cellangles.CellFile#computeLabelList(ij.process.ImageProcessor)}.
	 */
	@Test
	public void testComputeLabelList_Col0()
	{
		// Read input label image
		URL inputFile = getClass().getResource("/images/Coupe-Col0-Calco-02-01-basins.tif");
		ImagePlus imagePlus = IJ.openImage(inputFile.getFile());
		ImageProcessor image = imagePlus.getProcessor();
		
		// create default cell file
		CellFile cellFile = new CellFile("dummy", true);
		
		// Add waypoints
		int[] roiX = new int[]{187, 285, 414, 485, 609};
		int[] roiY = new int[]{391, 401, 408, 409, 411};
		List<Point2D> wayPoints = new ArrayList<Point2D>(roiX.length);
		for (int i = 0; i < roiX.length; i++)
		{
			wayPoints.add(new Point2D.Double(roiX[i], roiY[i]));
		}
		cellFile.setWayPoint(wayPoints);
		
		// Compute
		List<Integer> labelList = cellFile.computeLabelList(image);
		
		// Compare with a priori set of labels
		int[] labels = new int[]{
				392, 395, 401, 405, 413, 415, 423, 424, 427, 429, 432, 437, 438, 440, 441, 444, 445, 448};
		List<Integer> expectedLabelList = new ArrayList<Integer>(labels.length);
		for (int label : labels)
		{
			expectedLabelList.add(label);
		}
		
		assertEquals(labels.length, labelList.size());
		for (int i = 0; i < labels.length; i++)
		{
			assertEquals(labels[i], (int) labelList.get(i));
		}
	}


	/**
	 * Test method for {@link ijt.cellangles.CellFile#computeCellsBoundaries(ij.process.ImageProcessor)}.
	 */
	@Test
	public void testComputeCellsBoundaries()
	{
		fail("Not yet implemented");
	}

}
