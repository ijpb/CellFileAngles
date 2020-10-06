/**
 * 
 */
package ijt.cellangles;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ij.process.ImageProcessor;
import ijt.geom.Geometry;

/**
 * The boundary between two cells.
 * 
 * @author dlegland
 */
public class CellsBoundary
{
	/**
	 * The label of the first cell region;
	 */
	int label1;

	/**
	 * The label of the second cell region;
	 */
	int label2;
	
	/**
	 * Identification of the side, to be able to identify inner and outer
	 * points.
	 */
	boolean leftSide;
	
	/**
	 * The list of boundary pixels.
	 */
	ArrayList<Point> pixelList = new ArrayList<Point>();
	
	Point innerPoint = null;
	Point outerPoint = null;
	
	/**
	 * The relative angle formed by the extremity points of the boundary and the
	 * line joining the centroids of the two surrounding cells.
	 */
	double angle;
	
	
	public CellsBoundary(int label1, int label2, boolean isLeftSide)
	{
		this.label1 = label1;
		this.label2 = label2;
		this.leftSide = isLeftSide;
	}
	
	/**
	 * Identifies the position of boundary pixels from the specified label
	 * image.
	 * 
	 * @param labelImage
	 *            the image containing labels of cell regions.
	 */
	public Collection<Point> findBoundaryPixels(ImageProcessor labelImage)
	{
		int sizeX = labelImage.getWidth();
		int sizeY = labelImage.getHeight();

		// list of offsets defining the 4-connectivity
		int[] dx = new int[]{0, -1, +1, 0};
		int[] dy = new int[]{-1, 0, 0, +1};

		this.pixelList.clear();
		for (int y = 0; y < sizeY; y++)
		{
			for (int x = 0; x < sizeX; x++)
			{
				// check that current label is background
				int label = (int) labelImage.getf(x, y);
				if (label != 0)
				{
					continue;
				}
				
				// extract the list of neighbors
				List<Integer> neighborList = new ArrayList<Integer>(4);
				for (int i = 0; i < 4; i++)
				{
					// check neighbor position in within image bounds 
					int x2 = x + dx[i];
					if (x2 < 0 || x2 > sizeX - 1) continue;
					int y2 = y + dy[i];
					if (y2 < 0 || y2 > sizeY - 1) continue;
					
					// get label of neighbor
					label = (int) labelImage.getf(x2, y2);
					if (label != 0)
					{
						neighborList.add(label);
					}
				}
				
				// check if neighborhood contains each label
				if (neighborList.contains(label1) && neighborList.contains(label2))
				{
					this.pixelList.add(new Point(x, y));
				}
			}
		}
		
		return Collections.unmodifiableCollection(this.pixelList);
	}
	
	public List<Point> findBoundaryExtremities()
	{
		List<Point> extremityList = new ArrayList<Point>(2);
		
		List<Point> neighborList = new ArrayList<Point>(4); 
		for (Point point : this.pixelList)
		{
			neighborList.clear();
			
			for (Point neighbor : this.pixelList)
			{
				if (neighbor.equals(point)) continue;
				
				int dx = Math.abs(point.x - neighbor.x);
				int dy = Math.abs(point.y - neighbor.y);
				
				if (Math.max(dx,  dy) <= 1)
				{
					neighborList.add(neighbor);
				}
			}
			
			if (neighborList.size() == 1)
			{
				extremityList.add(point);
			}
		}
		
		return extremityList;
	}
	
	public void identifyInnerAndOuterPoints(Point2D centroid1, Point2D centroid2)
	{
		int nPoints = this.pixelList.size();
		
		double maxInnerDist = Double.NEGATIVE_INFINITY;
		double maxOuterDist = Double.NEGATIVE_INFINITY;
		
		for (int i = 0; i < nPoints; i++)
		{
			Point point = this.pixelList.get(i);
			double dist = Geometry.distancePointLine(point, centroid1, centroid2);
			
			boolean isInnerPoint = Geometry.isLeftSide(point, centroid1, centroid2) ^ this.leftSide;
			
			if (isInnerPoint)
			{
				// process inner point 
				if (dist > maxInnerDist)
				{
					this.innerPoint = point;
					maxInnerDist = dist;
				}
			}
			else
			{
				// process outer point 
				if (dist > maxOuterDist)
				{
					this.outerPoint = point;
					maxOuterDist = dist;
				}
			}
		}
	}

	public double computeBoundaryAngle(Point2D centroid1, Point2D centroid2)
	{
		// Compute local angle of cell file
		double cellFileAngle = Geometry.lineAngle(centroid1, centroid2);
		
		// Compute angle of boundary line segment
		double boundaryAngle = Geometry.lineAngle(innerPoint, outerPoint);

		// computes the angle between the two lines
		if (leftSide)
		{
			angle = Geometry.betweenLinesAngle(cellFileAngle, boundaryAngle);
		}
		else
		{
			angle = Geometry.betweenLinesAngle(boundaryAngle, cellFileAngle);
		}
		
		return angle;
	}
}
