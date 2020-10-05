/**
 * 
 */
package ijt.cellangles;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import ij.process.FloatPolygon;

/**
 * Collection of static methods for geometric computations.
 * 
 * @author dlegland
 *
 */
public class Geometry
{
	
	public static final List<Point> digitalLineSegment(Point p1, Point p2)
	{
		int x1 = p1.x;
		int y1 = p1.y;
		int x2 = p2.x;
		int y2 = p2.y;

		// get direction vector 
		int dx = x2 - x1;
		int dy = y2 - y1;
		
		// get absolute values of direction vector components
		int adx = Math.abs(dx);
		int ady = Math.abs(dy);

		// create output list
		int nPoints = Math.max(adx, ady) + 1;
		List<Point> points = new ArrayList<Point>(nPoints);

		// init with first point
		points.add(new Point(x1, y1));

		// small check to avoid problems in case of multiple vertex 
		if (dx == 0 && dy == 0)
		{
			return points;
		}
		
		// sample points in the main direction of the line segment 
		if (adx >= ady)
		{
			// compute line slope for horizontal lines
			int incX = dx / adx;
			double m = ((double) dy) / ((double) dx);

			// iterate over x, and compute corresponding y
			int x = x1;
			for (int i = 1; i < nPoints; i++)
			{
				x += incX;
				int y = (int) Math.round(y1 + m * (x - x1));
				points.add(new Point(x, y));
			}
			
		}	    
		else
		{
			// compute line slope for vertical lines
			int incY = dy / ady;
			double m = ((double) dx) / ((double) dy);

			// iterate over y, and compute corresponding x
			int y = y1;
			for (int i = 1; i < nPoints; i++)
			{
				y += incY;
				int x = (int) Math.round(x1 + m * (y - y1));
				points.add(new Point(x, y));
			}
		}
		
		return points;
	}
	
	public static final FloatPolygon smoothPolyline(FloatPolygon poly)
	{
		// create resulting polyline
		FloatPolygon smoothed = poly.duplicate();
		
		// iterate over inner vertices
		for (int i = 1; i < poly.npoints - 1; i++)
		{
			float x = 0;
			float y = 0;
			// iterate over neighbors
			for (int j = i - 1; j <= i + 1; j++)
			{
				x += poly.xpoints[j];
				y += poly.ypoints[j];
			}
			
			// compute average
			smoothed.xpoints[i] = (float) (x / 3.0);
			smoothed.ypoints[i] = (float) (y / 3.0);
		}
		
		// return smoothed polyline
		return smoothed;
	}
	
	public static final double betweenLinesAngle(double angle1, double angle2)
	{
		return (angle2 - angle1 + 2 * Math.PI) % (2 * Math.PI);
	}
	
	public static final Point2D getVertex(FloatPolygon poly, int index)
	{
		 return new Point2D.Double(poly.xpoints[index], poly.ypoints[index]);
	}
	
	/**
	 * Returns true if the point given as first argument is on the left side of
	 * the line defined by the second and third arguments.
	 * 
	 * @param p
	 *            the point to test
	 * @param p1
	 *            the first point of the line
	 * @param p2
	 *            the second point of the line
	 * @return true if the test point is on the left side of the line
	 */
	public static final boolean isLeftSide(Point2D p, Point2D p1, Point2D p2)
	{
		// direction vector of the line
		double dx = p2.getX() - p1.getX();
		double dy = p2.getY() - p1.getY();
		
		return ((p.getX() - p1.getX()) * dy - (p.getY() - p1.getY()) * dx < 0);
	}
	
	public static final double distancePointLine(Point2D p, Point2D p1, Point2D p2)
	{
		// direction vector of the line
		double vx = p2.getX() - p1.getX();
		double vy = p2.getY() - p1.getY();

		double delta = vx * vx + vy * vy;
		if (delta < 1e-12)
		{
			throw new IllegalArgumentException("The distance between the points defining the lines is not large enough");
		}
		
		// Difference of coordinates between point and line origins
		double dx = p.getX() - p1.getX();
		double dy = p.getY() - p1.getY();

		// compute position of points projected on the line, using normalized dot product 
		double pos = ( (dx * vx) + (dy * vy) ) / delta; 

		// compute distance between point and its projection on the line
		return Math.hypot(pos * vx - dx, pos * vy - dy);
	}
	
	/**
	 * Computes the angle with the horizontal of the straight line passing
	 * through the input points.
	 * 
	 * @param p1
	 *            the origin of the line
	 * @param p2
	 *            another point on the line defining the line direction
	 * @return the angle of the line with the horizontal direction, in radians
	 */
	public static final double lineAngle(Point2D p1, Point2D p2)
	{
		double dx = p2.getX() - p1.getX();
		double dy = p2.getY() - p1.getY();
		return (Math.atan2(dy, dx) + 2 * Math.PI) % (2 * Math.PI);
	}
	
	/**
	 * Private constructor to prevent instantiation.
	 */
	private Geometry()
	{
	}
}
