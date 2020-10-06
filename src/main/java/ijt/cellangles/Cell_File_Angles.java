/**
 * 
 */
package ijt.cellangles;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GUI;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.frame.PlugInFrame;
import ij.process.ImageProcessor;
import ijt.geom.Polyline2D;

/**
 * @author dlegland
 *
 */
public class Cell_File_Angles extends PlugInFrame implements ActionListener
{	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	enum RootSides 
	{
		LEFT, 
		RIGHT
	};
	
	String[] tissueNames = new String[] {"Epiderm", "Cortex", "Endoderm", "Pericycle"};
	
	
	JTextField tissueTypeTextField;
	JCheckBox leftSideCheckBox;
	JCheckBox smoothPolylineCheckBox;

	JCheckBox showOverlayCheckBox;
	JLabel imageToOverlayLabel;
	JComboBox<String> imageToOverlayCombo;
	
	JButton processCurrentRoiButton;
	
	public Cell_File_Angles()
	{
		super("Cell File Angles");
		
		setupWidgets();
		setupLayout();
		
		this.pack();
//		this.setSize(300, 200);
		
		GUI.center(this);
		setVisible(true);

	}
	
	private void setupWidgets()
	{
		this.tissueTypeTextField = new JTextField(15);

		this.leftSideCheckBox = new JCheckBox("Left Side of Root", true);
		this.leftSideCheckBox.setAlignmentX(LEFT_ALIGNMENT);

		this.smoothPolylineCheckBox = new JCheckBox("Smooth Polyline");
		this.smoothPolylineCheckBox.setAlignmentX(LEFT_ALIGNMENT);
		
		this.showOverlayCheckBox = new JCheckBox("Show Result Overlay");
		this.showOverlayCheckBox.setAlignmentX(0.0f);
		this.showOverlayCheckBox.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent evt)
			{
				updateImageToOverlayComboState();
			}
		});
		this.imageToOverlayLabel = new JLabel("Image to Overlay: ");
		this.imageToOverlayCombo = new JComboBox<String>();
		this.imageToOverlayCombo.addItem("Image 1 sfs sf sf sdf d sdf s fsd fsd sd");
		this.imageToOverlayCombo.addItem("Image 2");
		this.imageToOverlayCombo.addItem("Image 3");

		this.processCurrentRoiButton = new JButton("Process Current Roi");
		this.processCurrentRoiButton.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent evt)
			{
				runAnalysis();
			}
		});
	}
	
	private void setupLayout()
	{
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
		
		JPanel metaDataPanel = createOptionsPanel("Meta-Data");
		addInLine(metaDataPanel, new JLabel("Tissue Type: "), this.tissueTypeTextField);
		addInLine(metaDataPanel, this.leftSideCheckBox);
		
		JPanel processingPanel = createOptionsPanel("Processing Options");
		addInLine(processingPanel, this.smoothPolylineCheckBox);

		JPanel displayOptionsPanel = createOptionsPanel("Display Options");
		addInLine(displayOptionsPanel, this.showOverlayCheckBox);
		addInLine(displayOptionsPanel, this.imageToOverlayLabel, this.imageToOverlayCombo);
		
		mainPanel.add(metaDataPanel);
		mainPanel.add(processingPanel);
		mainPanel.add(displayOptionsPanel);
		
		addInLine(mainPanel, FlowLayout.CENTER, processCurrentRoiButton);
		
		this.setLayout(new BorderLayout());
		this.add(mainPanel, BorderLayout.CENTER);
	}
	
	private JPanel createOptionsPanel(String title)
	{
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createTitledBorder(title));
		panel.setAlignmentX(0.0f);
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		return panel;
	}
	
	private void addInLine(JPanel panel, Component... comps)
	{
		addInLine(panel, FlowLayout.LEFT, comps);
	}
	
	private void addInLine(JPanel panel, int alignment, Component... comps)
	{
		JPanel rowPanel = new JPanel(new FlowLayout(alignment));
		rowPanel.setAlignmentX(0.0f);
		for (Component c : comps)
		{
			rowPanel.add(c);
		}
		panel.add(rowPanel);
	}
	
	public void actionPerformed(ActionEvent evt)
	{
	}
	
	public void runAnalysis()
	{
		// Get current open image
		ImagePlus imagePlus = WindowManager.getCurrentImage();
		if (imagePlus == null) 
		{
			IJ.error("No image", "Need at least one image to work");
			return;
		}
		
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
		
		// Extract Computation options
		String tissueTypeName = this.tissueTypeTextField.getText();
		boolean isLeftSide = this.leftSideCheckBox.isSelected();
		boolean smoothPolyline = this.smoothPolylineCheckBox.isSelected();
		
		
		// find image for displaying geometric overlays
		int overlayImageIndex = this.imageToOverlayCombo.getSelectedIndex();
		ImagePlus imageToOverlay = WindowManager.getImage(overlayImageIndex + 1);
		
		ResultsTable table = computeCellFileAngles(imagePlus, tissueTypeName, smoothPolyline, isLeftSide, imageToOverlay);
		table.show("Cell File Angles");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ij.plugin.PlugIn#run(java.lang.String)
	 */
	@Override
	public void run(String args)
	{
		IJ.log("run plugin");
		
		boolean state = this.imageToOverlayCombo.isEnabled();
		this.imageToOverlayCombo.setEnabled(false);
		this.imageToOverlayCombo.removeAllItems();

		// create the list of image names
		int[] indices = WindowManager.getIDList();
		String[] imageNames = new String[indices.length];
		for (int i=0; i<indices.length; i++)
		{
			imageNames[i] = WindowManager.getImage(indices[i]).getTitle();
			this.imageToOverlayCombo.addItem(imageNames[i]);
		}
		
		this.imageToOverlayCombo.setEnabled(state);
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
		ResultsTable table = cellFile.createTable();
		
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
		updateOverlay(overlay, cellFile);
		imageToOverlay.setOverlay(overlay);
		
		return table;
	}
	
	
	// ====================================================
	// Widget call backs
	
	private void updateImageToOverlayComboState()
	{
		boolean state = this.showOverlayCheckBox.isSelected(); 
		this.imageToOverlayCombo.setEnabled(state);
		this.imageToOverlayLabel.setEnabled(state);
	}
	
	// ====================================================
	// Graphical functions 
	
	public void updateOverlay(Overlay overlay, CellFile cellFile)
	{
		for (CellsBoundary bnd : cellFile.getBoundaries())
		{
			addBoundaryEdgeOverlay(overlay, bnd.innerPoint, bnd.outerPoint);
		}
		addCellFilePathOverlay(overlay, cellFile.pathCurve);
		addExtremitiesOverlay(overlay, cellFile.getAllExtremities());
	}
	
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

}
