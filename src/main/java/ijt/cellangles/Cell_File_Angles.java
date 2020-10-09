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
import ij.Prefs;
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
public class Cell_File_Angles extends PlugInFrame
{	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	String[] tissueNames = new String[] {"Epiderm", "Cortex", "Endoderm", "Pericycle"};
	
	enum RootSides 
	{
		LEFT, 
		RIGHT
	};
	
	// processing options
	JComboBox<String> tissueTypeCombo;
	JTextField tissueTypeTextField;
	JComboBox<String> rootSideCombo;
	JCheckBox smoothPolylineCheckBox;

	// result display management
	JCheckBox showTableCheckBox;
	JCheckBox showLogCheckBox;
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
		
		GUI.center(this);
		setVisible(true);
	}
	
	private void setupWidgets()
	{
		this.tissueTypeCombo = new JComboBox<String>();
		for (String tissueType : tissueNames)
		{
			this.tissueTypeCombo.addItem(tissueType);
		}
		this.tissueTypeTextField = new JTextField(15);

		this.rootSideCombo = new JComboBox<String>();
		this.rootSideCombo.addItem("Left Side of Root");
		this.rootSideCombo.addItem("Right Side of Root");

		this.smoothPolylineCheckBox = new JCheckBox("Smooth Polyline", true);
		
		this.showTableCheckBox = new JCheckBox("Show Results in Table", true);
		this.showLogCheckBox = new JCheckBox("Show Result in Log", true);
		this.showOverlayCheckBox = new JCheckBox("Overlay Results on Image", true);
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
		addInLine(metaDataPanel, new JLabel("Tissue Type: "), this.tissueTypeCombo);
		addInLine(metaDataPanel, new JLabel("Other Tissue Type: "), this.tissueTypeTextField);
		addInLine(metaDataPanel, this.rootSideCombo);
		
		JPanel processingPanel = createOptionsPanel("Processing Options");
		addInLine(processingPanel, this.smoothPolylineCheckBox);

		JPanel displayOptionsPanel = createOptionsPanel("Display Options");
		addInLine(displayOptionsPanel, this.showTableCheckBox);
		addInLine(displayOptionsPanel, this.showLogCheckBox);
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
	
	
	public void runAnalysis()
	{
		// retrieve data necessary for computation

		// Get current open image
		ImagePlus labelImagePlus = WindowManager.getCurrentImage();
		if (labelImagePlus == null) 
		{
			IJ.error("No image", "Need at least one image to work");
			return;
		}
		ImageProcessor labelImage = labelImagePlus.getProcessor();
		
		
		// extract current ROI, and check it has a valid type
		Roi roi = labelImagePlus.getRoi();
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
		
		
		// Meta-data associated to analysis 
		String tissueTypeName = this.tissueTypeTextField.getText();
		if (tissueTypeName.isEmpty())
		{
			tissueTypeName = (String) this.tissueTypeCombo.getSelectedItem();
		}
		boolean isLeftSide = this.rootSideCombo.getSelectedIndex() == 0;
		
		// Extract Computation options
		boolean smoothPolyline = this.smoothPolylineCheckBox.isSelected();
		
		
		// Compute Cell File for current settings
		CellFile cellFile = analyzeCellFile(labelImage, roi, tissueTypeName, isLeftSide, smoothPolyline);
		
		
		// Display results in Table if appropriate
		if (this.showTableCheckBox.isSelected())
		{
			// Concatenates some values into ResultsTable
			ResultsTable table = cellFile.createTable();
			table.show("Cell File Angles");
		}

		// Display results in log window if appropriate
		if (this.showLogCheckBox.isSelected())
		{
			 printToLog(cellFile, labelImagePlus.getShortTitle());
		}
		
		// Overlay results on image if appropriate
		if (showOverlayCheckBox.isSelected())
		{
			// find image for displaying geometric overlays
			int overlayImageIndex = this.imageToOverlayCombo.getSelectedIndex();
			ImagePlus imageToOverlay = WindowManager.getImage(overlayImageIndex + 1);

			// Create overlay
			addCellFileToOverlay(imageToOverlay, cellFile);
		}
		
	}
	
	public static final CellFile analyzeCellFile(ImageProcessor labelImage, Roi roi, String tissueTypeName, boolean isLeftSide, boolean smoothPolyline)
	{
		// create a new CellFile object
		CellFile cellFile = new CellFile(tissueTypeName, isLeftSide);
		
		// Compute label succession from polyline ROI
		Polygon polyline = ((PolygonRoi) roi).getPolygon();
		cellFile.setWayPoints(polyline);
		cellFile.computeLabelList(labelImage);

		// compute the polyline between cell centroids, optionally smoothed
		cellFile.computePathCurve(labelImage);
		if (smoothPolyline)
		{
			cellFile.pathCurve = cellFile.pathCurve.smooth();
		}
		
		// perform computation of boundaries, and of angles
		cellFile.computeCellsBoundaries(labelImage);
		
		return cellFile;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ij.plugin.PlugIn#run(java.lang.String)
	 */
	@Override
	public void run(String args)
	{
		// prepare combo box for modification
		boolean state = this.imageToOverlayCombo.isEnabled();
		this.imageToOverlayCombo.setEnabled(false);
		this.imageToOverlayCombo.removeAllItems();

		// update the list of images in combo
		for (int index : WindowManager.getIDList())
		{
			String imageName = WindowManager.getImage(index).getTitle();
			this.imageToOverlayCombo.addItem(imageName);
		}
		
		this.imageToOverlayCombo.setEnabled(state);
	}

	private void printToLog(CellFile cellFile, String imageName)
	{
		// Write current results into the "log" window
		IJ.log("imageName;tissueType;label1;label2;innerPointX;innerPointY;outerPointX;outerPointY;angleInDegrees");
		for (CellsBoundary bnd : cellFile.getBoundaries())
		{
			String str = imageName + "; " + cellFile.tissueTypeName + "; " 
					+ bnd.label1 + "; " + bnd.label2 + "; "
					+ bnd.innerPoint.getX() + "; " + bnd.innerPoint.getY() + "; " 
					+ bnd.outerPoint.getX() + "; " + bnd.outerPoint.getY() + "; "
					+ Math.toDegrees(bnd.angle);
			IJ.log(str);
		}

	}

	// ====================================================
	// Graphical functions 
	
	public void addCellFileToOverlay(ImagePlus image, CellFile cellFile)
	{
		Overlay overlay = image.getOverlay();
		if (overlay == null)
		{
			overlay = new Overlay();
		}

		updateOverlay(overlay, cellFile);
		
		image.setOverlay(overlay);
	}
	
	private void updateOverlay(Overlay overlay, CellFile cellFile)
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

	
	// ====================================================
	// Widget call backs
	
	private void updateImageToOverlayComboState()
	{
		boolean state = this.showOverlayCheckBox.isSelected(); 
		this.imageToOverlayCombo.setEnabled(state);
		this.imageToOverlayLabel.setEnabled(state);
	}
	
	// ====================================================
	// Widget call backs
	
	/** Overrides close() in PlugInFrame. */
	public void close()
	{
		super.close();
	}

}
