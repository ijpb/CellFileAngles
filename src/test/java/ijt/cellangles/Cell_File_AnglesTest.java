package ijt.cellangles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Point;
import java.awt.Polygon;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class Cell_File_AnglesTest
{

	@Test
	public final void testFindLabelsAlongRoi_Mock()
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
		
		// create polyline ROI
		int[] roiX = new int[]{2, 7, 11};
		int[] roiY = new int[]{2, 4, 2};
		PolygonRoi roi = new PolygonRoi(roiX, roiY, 3, Roi.POLYLINE);
		Polygon poly = roi.getPolygon();
		
		List<Integer> labels = Cell_File_Angles.findLabelsAlongRoi(image, poly);
		
		assertTrue(labels != null);
		assertEquals(4, labels.size());
		assertEquals( 4, (int) labels.get(0));
		assertEquals( 7, (int) labels.get(1));
		assertEquals(10, (int) labels.get(2));
		assertEquals(15, (int) labels.get(3));
	}

	@Test
	public final void testFindLabelsAlongRoi_Col0()
	{
		// Read input label image
		URL inputFile = getClass().getResource("/images/Coupe-Col0-Calco-02-01-basins.tif");
		ImagePlus imagePlus = IJ.openImage(inputFile.getFile());
		ImageProcessor image = imagePlus.getProcessor();
		
		// create polyline ROI
		int[] roiX = new int[]{187, 285, 414, 485, 609};
		int[] roiY = new int[]{391, 401, 408, 409, 411};
		PolygonRoi roi = new PolygonRoi(roiX, roiY, 5, Roi.POLYLINE); 
		Polygon poly = roi.getPolygon();
		
		List<Integer> labelList = Cell_File_Angles.findLabelsAlongRoi(image, poly);
		
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

	@Test
	public final void testFindBoundaryPixels_FourRect()
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
		
		List<Point> pixelList = Cell_File_Angles.findBoundaryPixels(image, 4, 7);
		assertTrue(pixelList.contains(new Point(3, 1)));
		assertTrue(pixelList.contains(new Point(3, 2)));
		assertTrue(pixelList.contains(new Point(3, 3)));
		assertTrue(pixelList.contains(new Point(3, 4)));
	}
	
	@Test
	public final void testFindBoundaryExtremities()
	{
		List<Point> pixelList = new ArrayList<Point>(4);
		pixelList.add(new Point(3, 1));
		pixelList.add(new Point(3, 2));
		pixelList.add(new Point(3, 3));
		pixelList.add(new Point(3, 4));
		
		List<Point> extremityList = Cell_File_Angles.findBoundaryExtremities(pixelList);
		
		assertTrue(extremityList.contains(new Point(3, 1)));
		assertTrue(extremityList.contains(new Point(3, 4)));
	}
	
	@Test
	public final void testDistancePointLine_Horiz()
	{
		Point lineP1 = new Point(10, 10);
		Point lineP2 = new Point(30, 10);
		
		Point p1 = new Point(5, 5);		double exp1 = 5;
		Point p2 = new Point(20, 0);	double exp2 = 10;
		Point p3 = new Point(5, 25);	double exp3 = 15;
		
		double dist1 = Cell_File_Angles.distancePointLine(p1, lineP1, lineP2);
		assertEquals(exp1, dist1, .001);

		double dist2 = Cell_File_Angles.distancePointLine(p2, lineP1, lineP2);
		assertEquals(exp2, dist2, .001);
		
		double dist3 = Cell_File_Angles.distancePointLine(p3, lineP1, lineP2);
		assertEquals(exp3, dist3, .001);
	}

	@Test
	public final void testDistancePointLine_Vert()
	{
		Point lineP1 = new Point(10, 10);
		Point lineP2 = new Point(10, 30);
		
		Point p1 = new Point(5, 5);		double exp1 = 5;
		Point p2 = new Point(0, 20);	double exp2 = 10;
		Point p3 = new Point(25, 5);	double exp3 = 15;
		
		double dist1 = Cell_File_Angles.distancePointLine(p1, lineP1, lineP2);
		assertEquals(exp1, dist1, .001);

		double dist2 = Cell_File_Angles.distancePointLine(p2, lineP1, lineP2);
		assertEquals(exp2, dist2, .001);
		
		double dist3 = Cell_File_Angles.distancePointLine(p3, lineP1, lineP2);
		assertEquals(exp3, dist3, .001);
	}

	@Test
	public final void testDistancePointLine_Diag()
	{
		Point lineP1 = new Point(10, 10);
		Point lineP2 = new Point(10+40, 10+30);
		
		Point p1 = new Point(10+70, 10-10);		double exp1 = 50;
		Point p2 = new Point(10+50, 10+100);	double exp2 = 50;
		Point p3 = new Point(10+10, 10+70);		double exp3 = 50;
		
		double dist1 = Cell_File_Angles.distancePointLine(p1, lineP1, lineP2);
		assertEquals(exp1, dist1, .001);

		double dist2 = Cell_File_Angles.distancePointLine(p2, lineP1, lineP2);
		assertEquals(exp2, dist2, .001);
		
		double dist3 = Cell_File_Angles.distancePointLine(p3, lineP1, lineP2);
		assertEquals(exp3, dist3, .001);
	}

	@Test
	public final void testIsLeftSide_Diag()
	{
		Point lineP1 = new Point(10, 10);
		Point lineP2 = new Point(10+40, 10+30);
		
		Point p1 = new Point(10+70, 10-10);
		Point p2 = new Point(10+50, 10+100);
		Point p3 = new Point(10+10, 10+70);
		
		assertTrue(!Cell_File_Angles.isLeftSide(p1, lineP1, lineP2));
		assertTrue(Cell_File_Angles.isLeftSide(p2, lineP1, lineP2));
		assertTrue(Cell_File_Angles.isLeftSide(p3, lineP1, lineP2));
	}
}
