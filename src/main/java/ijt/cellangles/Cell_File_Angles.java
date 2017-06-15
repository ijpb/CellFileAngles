/**
 * 
 */
package ijt.cellangles;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import inra.ijpb.measure.GeometricMeasures2D;

/**
 * @author dlegland
 *
 */
public class Cell_File_Angles implements PlugIn
{
	enum RootSides 
	{
		LEFT, 
		RIGHT
	};
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see ij.plugin.PlugIn#run(java.lang.String)
	 */
	@Override
	public void run(String arg0) 
	{
		// Get current open image
		ImagePlus imagePlus = WindowManager.getCurrentImage();
		if (imagePlus == null) 
		{
			IJ.error("No image", "Need at least one image to work");
			return;
		}

		// create the list of image names
		int[] indices = WindowManager.getIDList();
		String[] imageNames = new String[indices.length];
		for (int i=0; i<indices.length; i++)
		{
			imageNames[i] = WindowManager.getImage(indices[i]).getTitle();
		}
		
		// name of selected image
		String selectedImageName = IJ.getImage().getTitle();

		// extract current ROI, and check it has a valid type
		Roi roi = imagePlus.getRoi();
		if (roi == null)
		{
			IJ.error("No ROI", "Requires a LineRoi to work");
			return;			
		}
		if (roi.getType() != Roi.POLYLINE)
		{
			IJ.error("No ROI", "Requires a PolylineRoi to work");
			return;			
		}
		 

		// create the dialog
		GenericDialog gd = new GenericDialog("Cell File Analyzer");
		gd.addChoice("Root Side", new String[] { "Left Side", "Right Side" }, "Left Side");
		gd.addCheckbox("Show Overlay Result", true);
		gd.addChoice("Image to overlay:", imageNames, selectedImageName);
		
		// Display dialog and wait for user input
		gd.showDialog();
		if (gd.wasCanceled())
		{
			return;
		}
		
		boolean isLeftSide = gd.getNextChoiceIndex() == 0;
		@SuppressWarnings("unused")
		boolean showOverlay = gd.getNextBoolean();
		int overlayImageIndex = gd.getNextChoiceIndex();
		
		// find image for displaying geometric overlays
		ImagePlus imageToOverlay = WindowManager.getImage(overlayImageIndex + 1);
		
		// Extract the necessary data
		ImageProcessor image = imagePlus.getProcessor();
		Polygon polyline = ((PolygonRoi) roi).getPolygon();
		
		ResultsTable table = computeCellFileAngles(image, polyline, isLeftSide, imageToOverlay);
		table.show("Cell File Angles");
	}

	public ResultsTable computeCellFileAngles(ImageProcessor labelImage, Polygon polyline, boolean isLeftSide, ImagePlus imageToOverlay)
	{
		List<Integer> labelList = findLabelsAlongRoi(labelImage, polyline);
		int nLabels = labelList.size();
		
		Overlay overlay = imageToOverlay.getOverlay();
		if (overlay == null)
		{
			overlay = new Overlay();
		}
		
		// Create a ROI for the cell file path
		FloatPolygon cellFilePath = getCellFilePath(labelImage, labelList);
		addCellFilePathOverlay(overlay, cellFilePath);
		
		ResultsTable table = new ResultsTable();
		
		List<Point> allExtremities = new ArrayList<Point>((nLabels - 1) * 2);
		for (int i = 0; i < nLabels - 1; i++)
		{
			// get the current labels
			int label1 = labelList.get(i);
			int label2 = labelList.get(i + 1);
			
			table.incrementCounter();
			table.addValue("Label1", label1);
			table.addValue("Label2", label2);
			
			// find points located between the two labels
			List<Point> boundaryPoints = findBoundaryPixels(labelImage, label1, label2);
			
			// isolate boundary extremities
			List<Point> extremities = findBoundaryExtremities(boundaryPoints);
			allExtremities.addAll(extremities);
			
			// filter extremities to isolate inner and outer points
			Point2D centroid1 = getVertex(cellFilePath, i); 
			Point2D centroid2 = getVertex(cellFilePath, i + 1);
			PointPair pair = getInnerAndOuterPoints(extremities, centroid1, centroid2, isLeftSide); 
			
			// add current line segment to overlay
			Point innerPoint = pair.p1;
			Point outerPoint = pair.p2;
			addBoundaryEdgeOverlay(overlay, innerPoint, outerPoint);

			table.addValue("innerX", innerPoint.getX());
			table.addValue("innerY", innerPoint.getY());
			table.addValue("outerX", outerPoint.getX());
			table.addValue("outerY", outerPoint.getY());

			// Compute local angle of cell file
			double cellFileAngle = lineAngle(centroid1, centroid2);
			
			// Compute angle of boundary line segment
			double boundaryAngle = lineAngle(innerPoint, outerPoint);

			// computes the angle between the two lines
			double angle;
			if (isLeftSide)
			{
				angle = betweenLinesAngle(cellFileAngle, boundaryAngle);
			}
			else
			{
				angle = betweenLinesAngle(boundaryAngle, cellFileAngle);
			}
			
			table.addValue("angle", Math.toDegrees(angle));
		}
		
		addExtremitiesOverlay(overlay, allExtremities);
		
		imageToOverlay.setOverlay(overlay);
		
		return table;
	}
	
	// ====================================================
	// Graphical functions 
	
	private void addCellFilePathOverlay(Overlay overlay, FloatPolygon cellFilePath)
	{
		PolygonRoi pathRoi = new PolygonRoi(cellFilePath, Roi.POLYLINE);
		pathRoi.setStrokeColor(Color.RED);
		pathRoi.setStrokeWidth(1);
		overlay.add(pathRoi);
	}
	
	/**
	 * Add each boundary extremity as a red circle ROI.
	 * 
	 * @param imagePlus
	 *            the instance of imagePlus used to display result
	 */
	private void addExtremitiesOverlay(Overlay overlay, List<Point> extremities) 
	{
		for (Point point : extremities) 
		{
			int x = point.x;
			int y = point.y;
			int r = 1;
			
			// draw inscribed circle
			int width = 2 * r + 1;
			Roi roi = new OvalRoi((int) (x - r - 1), (int) (y - r - 1), width, width);
			roi.setStrokeColor(Color.RED);
			roi.setFillColor(Color.RED);
			overlay.add(roi);
		}
	}

	private void addBoundaryEdgeOverlay(Overlay overlay, Point innerPoint, Point outerPoint)
	{
		Line lineRoi = new Line(
				innerPoint.getX(), innerPoint.getY(),
				outerPoint.getX(), outerPoint.getY());
		
		lineRoi.setStrokeColor(Color.GREEN);
		lineRoi.setStrokeWidth(1);
		
		overlay.add(lineRoi);
	}
	
	// ====================================================
	// Computing functions 
	
	public static final List<Integer> findLabelsAlongRoi(ImageProcessor image, Polygon poly)
	{
		if (poly.npoints == 0)
		{
			throw new IllegalArgumentException("Requires at least one point in the polygon");
		}
		
		// init result array
		List<Integer> labels = new ArrayList<Integer>();
		
		// add initial point
		Point point = new Point(poly.xpoints[0], poly.ypoints[0]);
		
		// iterate over edges of polyline
		for (int i = 1; i < poly.npoints; i++)
		{
			Point prev = point;
			point = new Point(poly.xpoints[i], poly.ypoints[i]);
			
			// get integer-coord positions along edge
			List<Point> intPoints = getIntegerPoint(prev, point);
			
			for (Point p : intPoints)
			{
				int label = (int) image.getf(p.x, p.y);
				if (label != 0 && !labels.contains(label))
				{
					labels.add(label);
				}
			}
		}
		
		return labels;
	}
	
	private static final List<Point> getIntegerPoint(Point p1, Point p2)
	{
		int x1 = p1.x;
		int y1 = p1.y;
		int x2 = p2.x;
		int y2 = p2.y;

		int dx = x2 - x1;
		int dy = y2 - y1;

		// create output list
		int nPoints = Math.max(Math.abs(dx),  Math.abs(dy)) + 1;
		List<Point> points = new ArrayList<Point>(nPoints);

		// init with first point
		points.add(new Point(x1, y1));

		// sample points in the main direction of the line segment 
		if (dx >= dy)
		{
			// compute line slope
			int incX = dx / Math.abs(dx);
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
			// compute line slope
			int incY = dy / Math.abs(dy);
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
	

	public static final FloatPolygon getCellFilePath(ImageProcessor labelImage, List<Integer> labelList)
	{
		int nLabels = labelList.size();
		
		int[] labels = new int[nLabels];
		for (int i = 0; i < nLabels; i++)
		{
			labels[i] = labelList.get(i);
		}
		double[][] centroids = GeometricMeasures2D.centroids(labelImage, labels);
		
		// Convert to two arrays of int for each direction
		float[] xCoords = new float[nLabels];
		float[] yCoords = new float[nLabels];
		for (int i = 0; i < nLabels; i++)
		{
			xCoords[i] = (float) centroids[i][0];
			yCoords[i] = (float) centroids[i][1];
		}

		return new FloatPolygon(xCoords, yCoords, nLabels);
	}
	
	public static final List<Point> findBoundaryPixels(ImageProcessor labelImage, int label1, int label2)
	{
		int sizeX = labelImage.getWidth();
		int sizeY = labelImage.getHeight();

		// list of offsets defining the 4-connectivity
		int[] dx = new int[]{0, -1, +1, 0};
		int[] dy = new int[]{-1, 0, 0, +1};

		List<Point> pixelList = new ArrayList<Point>();
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
				
				// check if neihgborhood contains each label
				if (neighborList.contains(label1) && neighborList.contains(label2))
				{
					pixelList.add(new Point(x, y));
				}
			}
		}
		
		return pixelList;
	}
	
	/**
	 * Finds the coordinates of the pixels located at each extremity of a
	 * boundary defined by a list of pixels.
	 * 
	 * Assumes 8-connectivity of the pixels. The input pixels are not necessarily ordered. 
	 * Expect two extremities, but some cases may produce different number of extremities.
	 * 
	 * @param pointList coordinates of the boundary pixels, unordered
	 * @return the coordinates of the two pixels located at the extremity.
	 */
	public static final List<Point> findBoundaryExtremities(List<Point> pointList)
	{
		List<Point> extremityList = new ArrayList<Point>(2);
		
		List<Point> neighborList = new ArrayList<Point>(4); 
		for (Point point : pointList)
		{
			neighborList.clear();
			
			for (Point neighbor : pointList)
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
	
	public static final PointPair getInnerAndOuterPoints(
			List<Point> extremityPoints, Point2D centroid1, Point2D centroid2,
			boolean leftSide)
	{
		int nPoints = extremityPoints.size();
		
		Point innerPoint = null;
		Point outerPoint = null;
		double maxInnerDist = Double.NEGATIVE_INFINITY;
		double maxOuterDist = Double.NEGATIVE_INFINITY;
		
		for (int i = 0; i < nPoints; i++)
		{
			Point point = extremityPoints.get(i);
			double dist = distancePointLine(point, centroid1, centroid2);
			
			boolean isInnerPoint = isLeftSide(point, centroid1, centroid2) ^ leftSide;
			
			if (isInnerPoint)
			{
				// process inner point 
				if (dist > maxInnerDist)
				{
					innerPoint = point;
					maxInnerDist = dist;
				}
			}
			else
			{
				// process outer point 
				if (dist > maxOuterDist)
				{
					outerPoint = point;
					maxOuterDist = dist;
				}
			}
		}
		
		return new PointPair(innerPoint, outerPoint);
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
	private static final double lineAngle(Point2D p1, Point2D p2)
	{
		double dx = p2.getX() - p1.getX();
		double dy = p2.getY() - p1.getY();
		return (Math.atan2(dy, dx) + 2 * Math.PI) % (2 * Math.PI);
	}
	
	private static final double betweenLinesAngle(double angle1, double angle2)
	{
		return (angle2 - angle1 + 2 * Math.PI) % (2 * Math.PI);
	}
	
	private static final Point2D getVertex(FloatPolygon poly, int index)
	{
		 return new Point2D.Double(poly.xpoints[index], poly.ypoints[index]);
	}

	
	/**
	 * A class containing two points, used for storing inner and outer points of the boundary.
	 * 
	 * @author dlegland
	 *
	 */
	static class PointPair
	{
		Point p1;
		Point p2;
		
		public PointPair(Point p1, Point p2)
		{
			this.p1 = p1;
			this.p2 = p2;
		}
	}
}
