/**
 * 
 */
package ijt.cellangles;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.util.Collection;

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
import ij.process.ImageProcessor;
import ijt.geom.Polyline2D;

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

		
		// create a new CellFile object
		CellFile cellFile = new CellFile(tissueTypeName, isLeftSide);
		
		// Compute label succession from polyline ROI
		cellFile.setWayPoints(polyline);
		cellFile.computeLabelList(labelImage);

		// compute the polyline between cell centroids, optionally smoothed
		cellFile.computePathCurve(labelImage);
		if (smoothPath)
		{
			cellFile.pathCurve = cellFile.pathCurve.smooth();
		}
		
		cellFile.computeCellsBoundaries(labelImage);
		
		// Concatenates some values into ResultsTable
		ResultsTable table = new ResultsTable();
		for (CellsBoundary boundary : cellFile.getBoundaries())
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
		
		// Write current results into the "log" window
		String imageName = labelImagePlus.getShortTitle();
		for (CellsBoundary bnd : cellFile.getBoundaries())
		{
			String str = imageName + "; " + tissueTypeName + "; " 
					+ bnd.label1 + "; " + bnd.label2 + "; "
					+ bnd.innerPoint.getX() + "; " + bnd.innerPoint.getY() + "; " 
					+ bnd.outerPoint.getX() + "; " + bnd.outerPoint.getY() + "; "
					+ Math.toDegrees(bnd.angle);
			IJ.log(str);
		}
		
		
		// Create overlay
		Overlay overlay = imageToOverlay.getOverlay();
		if (overlay == null)
		{
			overlay = new Overlay();
		}
		for (CellsBoundary bnd : cellFile.getBoundaries())
		{
			addBoundaryEdgeOverlay(overlay, bnd.innerPoint, bnd.outerPoint);
		}
		addCellFilePathOverlay(overlay, cellFile.pathCurve);
		addExtremitiesOverlay(overlay, cellFile.getAllExtremities());
		imageToOverlay.setOverlay(overlay);
		
		return table;
	}
	
	
	// ====================================================
	// Graphical functions 
	
	private void addCellFilePathOverlay(Overlay overlay, Polyline2D poly)
	{
		int nv = poly.vertexNumber();
		float[] x = new float[nv];
		float[] y = new float[nv];
		for (int i = 0; i < nv; i++)
		{
			Point2D v = poly.getVertex(i);
			x[i] = (float) v.getX();
			y[i] = (float) v.getY();
		}
		PolygonRoi pathRoi = new PolygonRoi(x, y, Roi.POLYLINE);
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
	private void addExtremitiesOverlay(Overlay overlay, Collection<Point2D> extremities) 
	{
		for (Point2D point : extremities) 
		{
			double x = point.getX();
			double y = point.getY();
			double r = 1;
			
			// draw inscribed circle
			int width = (int) (2 * r);
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

}
