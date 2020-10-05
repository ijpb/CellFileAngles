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
import inra.ijpb.measure.region2d.Centroid;

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
	
	String[] tissueNames = new String[] {"Epiderm", "Cortex", "Endoderm", "Pericycle"};
	
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
			IJ.error("No ROI", "Requires a Polyline Roi to work");
			return;			
		}
		if (roi.getType() != Roi.POLYLINE)
		{
			IJ.error("No ROI", "Requires a Polyline Roi to work");
			return;			
		}
		 

		// create the dialog
		GenericDialog gd = new GenericDialog("Cell File Analyzer");
		gd.addChoice("Tissue Type", tissueNames, tissueNames[0]);
		gd.addStringField("Other Type", "");
		gd.addChoice("Root Side", new String[] { "Left Side", "Right Side" }, "Left Side");
		gd.addCheckbox("Smooth Cell File Path", true);
		gd.addCheckbox("Show Overlay Result", true);
		gd.addChoice("Image to overlay:", imageNames, selectedImageName);
		
		// Display dialog and wait for user input
		gd.showDialog();
		if (gd.wasCanceled())
		{
			return;
		}
		
		int tissueTypeIndex = gd.getNextChoiceIndex();
		String tissueTypeName;
		String otherType = gd.getNextString();
		if (otherType.isEmpty())
		{
			tissueTypeName = tissueNames[tissueTypeIndex];
		}
		else
		{
			tissueTypeName = otherType;
			tissueTypeIndex = -1;
		}
		boolean isLeftSide = gd.getNextChoiceIndex() == 0;
		boolean smoothCellFilePath = gd.getNextBoolean();
		@SuppressWarnings("unused")
		boolean showOverlay = gd.getNextBoolean();
		int overlayImageIndex = gd.getNextChoiceIndex();
		
		// find image for displaying geometric overlays
		ImagePlus imageToOverlay = WindowManager.getImage(overlayImageIndex + 1);
		
		ResultsTable table = computeCellFileAngles(imagePlus, tissueTypeName, smoothCellFilePath, isLeftSide, imageToOverlay);
		table.show("Cell File Angles");
	}

	public ResultsTable computeCellFileAngles(ImagePlus labelImagePlus, String tissueTypeName, 
			boolean smoothPath, boolean isLeftSide, ImagePlus imageToOverlay)
	{
		ImageProcessor labelImage = labelImagePlus.getProcessor();
		Roi roi = labelImagePlus.getRoi();
		Polygon polyline = ((PolygonRoi) roi).getPolygon();

		List<Integer> labelList = findLabelsAlongRoi(labelImage, polyline);
		int nLabels = labelList.size();
		
		Overlay overlay = imageToOverlay.getOverlay();
		if (overlay == null)
		{
			overlay = new Overlay();
		}
		
		// Create a ROI for the cell file path
		FloatPolygon cellFilePath = getCellFilePath(labelImage, labelList);
		if (smoothPath)
		{
			cellFilePath = Geometry.smoothPolyline(cellFilePath);
		}
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
			Point2D centroid1 = Geometry.getVertex(cellFilePath, i); 
			Point2D centroid2 = Geometry.getVertex(cellFilePath, i + 1);
			PointPair pair = getInnerAndOuterPoints(extremities, centroid1, centroid2, isLeftSide); 
			
			// add current line segment to overlay
			Point innerPoint = pair.p1;
			Point outerPoint = pair.p2;
			if (innerPoint == null)
			{
				IJ.error("Inner point is null, i=" + i);
				break;
			}
			if (outerPoint == null)
			{
				IJ.error("Outer point is null, i=" + i);
				break;
			}
			addBoundaryEdgeOverlay(overlay, innerPoint, outerPoint);

			table.addValue("innerX", innerPoint.getX());
			table.addValue("innerY", innerPoint.getY());
			table.addValue("outerX", outerPoint.getX());
			table.addValue("outerY", outerPoint.getY());

			// Compute local angle of cell file
			double cellFileAngle = Geometry.lineAngle(centroid1, centroid2);
			
			// Compute angle of boundary line segment
			double boundaryAngle = Geometry.lineAngle(innerPoint, outerPoint);

			// computes the angle between the two lines
			double angle;
			if (isLeftSide)
			{
				angle = Geometry.betweenLinesAngle(cellFileAngle, boundaryAngle);
			}
			else
			{
				angle = Geometry.betweenLinesAngle(boundaryAngle, cellFileAngle);
			}
			
			table.addValue("angle", Math.toDegrees(angle));
			
			// Write current results into the "log" window
			String imageName = labelImagePlus.getShortTitle();
			String str = imageName + "; " + tissueTypeName + "; " + label1 + "; " + label2 + "; " + innerPoint.getX() + "; " + innerPoint.getY() + "; " + outerPoint.getX() + "; " + outerPoint.getY() + "; " + Math.toDegrees(angle); 
			IJ.log(str);
		}
		
		// update display of overlay
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
			int width = 2 * r;
			Roi roi = new OvalRoi((int) (x - r), (int) (y - r), width, width);
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
			List<Point> intPoints = Geometry.digitalLineSegment(prev, point);
			
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


	public static final FloatPolygon getCellFilePath(ImageProcessor labelImage, List<Integer> labelList)
	{
		int nLabels = labelList.size();
		
		int[] labels = new int[nLabels];
		for (int i = 0; i < nLabels; i++)
		{
			labels[i] = labelList.get(i);
		}
		double[][] centroids = Centroid.centroids(labelImage, labels);
		
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
				
				// check if neighborhood contains each label
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
			double dist = Geometry.distancePointLine(point, centroid1, centroid2);
			
			boolean isInnerPoint = Geometry.isLeftSide(point, centroid1, centroid2) ^ leftSide;
			
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
