/**
 * 
 */
package ijt.cellangles;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ij.IJ;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;
import ijt.geom.Geometry;
import ijt.geom.Polyline2D;
import inra.ijpb.measure.region2d.Centroid;

/**
 * A file of cells in a root.
 * 
 * @author dlegland
 *
 */
public class CellFile
{
	// ====================================================
	// Class members

	List<Point2D> wayPoints = new ArrayList<Point2D>();

	List<Integer> labelList = new ArrayList<Integer>();

	Polyline2D pathCurve = new Polyline2D();

	ArrayList<CellsBoundary> boundaries = new ArrayList<CellsBoundary>();

	// Meta-data on the cell file
	
	String tissueTypeName;
	
	boolean isLeftSide = true;
	
	
	// ====================================================
	// Constructor

	public CellFile(String tissueType, boolean isLeft)
	{
		this.tissueTypeName = tissueType;
		this.isLeftSide = isLeft;
	}
	
	
	// ====================================================
	// Access methods

	/**
	 * Creates a Results table containing most of numerical results.
	 * 
	 * @return a ResultsTable with most numerical results.
	 */
	public ResultsTable createTable()
	{
		// Concatenates some values into ResultsTable
		ResultsTable table = new ResultsTable();
		for (CellsBoundary boundary : this.boundaries)
		{
			table.incrementCounter();
			table.addValue("Label1", boundary.label1);
			table.addValue("Label2", boundary.label2);

			table.addValue("innerX", boundary.innerPoint.getX());
			table.addValue("innerY", boundary.innerPoint.getY());
			table.addValue("outerX", boundary.outerPoint.getX());
			table.addValue("outerY", boundary.outerPoint.getY());
			table.addValue("angle", Math.toDegrees(boundary.angle));
		}

		return table;
	}
	
	public Collection<CellsBoundary> getBoundaries()
	{
		return this.boundaries;
	}
	
	public Collection<Point2D> getAllExtremities()
	{
		ArrayList<Point2D> points = new ArrayList<Point2D>(this.boundaries.size() * 2);
		for (CellsBoundary bnd : this.boundaries)
		{
			points.add(bnd.innerPoint);
			points.add(bnd.outerPoint);
		}
		return points;
	}
	
	
	// ====================================================
	// Setup inputs

	public void setWayPoints(Polygon poly)
	{
		this.wayPoints.clear();
		for (int i = 0; i < poly.npoints; i++)
		{
			this.wayPoints.add(new Point2D.Double(poly.xpoints[i], poly.ypoints[i]));
		}
	}

	public void setWayPoint(List<Point2D> points)
	{
		this.wayPoints.clear();
		this.wayPoints.addAll(points);
	}
	

	// ====================================================
	// Computation methods

	/**
	 * Computes the list of labels along the cell file, when traveling along the
	 * way points within the label image.
	 * 
	 * @param labelImage
	 *            the image containing cell labels
	 */
	public List<Integer> computeLabelList(ImageProcessor labelImage)
	{
		int nPoints = this.wayPoints.size();
		if (nPoints < 2)
		{
			throw new IllegalArgumentException("Requires at least two way points");
		}
		
		// init result array
		this.labelList.clear();
		
		// add initial point
		Point point = intPoint(this.wayPoints.get(0));
		
		// iterate over the segments between the way points
		for (int i = 1; i < nPoints; i++)
		{
			Point prev = point;
			point = intPoint(this.wayPoints.get(i));
			
			// iterate over integer-coord positions between the two way points
			for (Point p : Geometry.digitalLineSegment(prev, point))
			{
				int label = (int) labelImage.getf(p.x, p.y);
				if (label != 0 && !labelList.contains(label))
				{
					labelList.add(label);
				}
			}
		}
		
		return this.labelList;
	}

	/**
	 * Converts a point with floating point coordinates into a point with
	 * integer coordinates.
	 * 
	 * @param point
	 *            the point with floating point coordinates
	 * @return the point with rounded integer coordinates
	 */
	private Point intPoint(Point2D point)
	{
		return new Point((int) point.getX(), (int) point.getY());
	}

	public void computePathCurve(ImageProcessor labelImage)
	{
		int nLabels = labelList.size();
		
		// convert label list to int array
		int[] labels = new int[nLabels];
		for (int i = 0; i < nLabels; i++)
		{
			labels[i] = labelList.get(i);
		}
		
		// Compute centroid of each cell regions
		double[][] centroids = Centroid.centroids(labelImage, labels);
		
		// init list of points
		this.pathCurve = new Polyline2D(nLabels);
		
		// convert 
		for (double[] centroid : centroids)
		{
			this.pathCurve.addVertex(new Point2D.Double(centroid[0], centroid[1]));
		}
	}
	
	public void computeCellsBoundaries(ImageProcessor labelImage)
	{
		int nLabels = labelList.size();
		
		boundaries.clear();
		boundaries.ensureCapacity(nLabels - 1);
		
		List<Point> allExtremities = new ArrayList<Point>((nLabels - 1) * 2);
		for (int i = 0; i < nLabels - 1; i++)
		{
			// get the current labels
			int label1 = labelList.get(i);
			int label2 = labelList.get(i + 1);
			
			// find points located between the two labels
			CellsBoundary boundary = new CellsBoundary(label1, label2, this.isLeftSide);
			boundary.findBoundaryPixels(labelImage);
			
			// isolate boundary extremities
			boundary.findBoundaryExtremities();
			allExtremities.addAll(boundary.pixelList);
			
			// filter extremities to isolate inner and outer points
			Point2D centroid1 = this.pathCurve.getVertex(i);
			Point2D centroid2 = this.pathCurve.getVertex(i + 1);
			boundary.identifyInnerAndOuterPoints(centroid1, centroid2); 
			
			// Check validity of inner and outer points
			if (boundary.innerPoint == null)
			{
				IJ.error("Inner point is null, i=" + i);
				break;
			}
			if (boundary.outerPoint == null)
			{
				IJ.error("Outer point is null, i=" + i);
				break;
			}

			// computes the angle between the two lines
			boundary.computeBoundaryAngle(centroid1, centroid2);
			
			boundaries.add(boundary);
		}
	}
	
}
