/**
 * 
 */
package ijt.cellangles;

import static org.junit.Assert.assertTrue;

import java.awt.Point;
import java.util.Collection;

import org.junit.Test;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

/**
 * @author dlegland
 *
 */
public class CellsBoundaryTest
{

	/**
	 * Test method for {@link ijt.cellangles.CellsBoundary#findBoundaryPixels(ij.process.ImageProcessor)}.
	 */
	@Test
	public void testFindBoundaryPixels()
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
		
		CellsBoundary boundary = new CellsBoundary(4, 7, true);
		Collection<Point> pixelList = boundary.findBoundaryPixels(image);
		
		assertTrue(pixelList.contains(new Point(3, 1)));
		assertTrue(pixelList.contains(new Point(3, 2)));
		assertTrue(pixelList.contains(new Point(3, 3)));
		assertTrue(pixelList.contains(new Point(3, 4)));
	}
	
	/**
	 * Test method for {@link ijt.cellangles.CellsBoundary#findBoundaryPixels(ij.process.ImageProcessor)}.
	 */
	@Test
	public final void findBoundaryExtremities()
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
		
		CellsBoundary boundary = new CellsBoundary(4, 7, true);
		boundary.findBoundaryPixels(image);
		Collection<Point> pixelList = boundary.findBoundaryExtremities();
		
		assertTrue(pixelList.contains(new Point(3, 1)));
		assertTrue(pixelList.contains(new Point(3, 4)));
	}
}
