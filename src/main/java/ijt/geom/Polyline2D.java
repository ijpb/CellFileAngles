/**
 * 
 */
package ijt.geom;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import ij.gui.PolygonRoi;
import ij.gui.Roi;

/**
 * A polyline shape in the plane.
 * 
 * Polyline is defined as a list of connected vertices.
 * 
 * @author dlegland
 *
 */
public class Polyline2D implements Iterable <Point2D>
{
    // ====================================================
    // Class variables
    
	ArrayList<Point2D> vertices;

	
	// ====================================================
    // Constructors
    
	/**
	 * Creates a new empty Polyline.
	 * 
	 */
	public Polyline2D()
	{
		this.vertices = new ArrayList<Point2D>(10);
	}
	
	/**
	 * Creates a new empty Polyline by allocating enough space for storing vertex
	 * coordinates.
	 * 
	 * @param n
	 *            then number of vertices
	 */
	public Polyline2D(int n)
	{
		this.vertices = new ArrayList<Point2D>(n);
	}
	
	/**
	 * Creates a new Polyline from the coordinates of its vertices.
	 * 
	 * @param xCoords 
	 *            the x-coordinates of the vertices
	 * @param yCoords 
	 *            the y-coordinates of the vertices
	 */
	public Polyline2D(double[] xCoords, double[] yCoords)
	{
		int n = xCoords.length;
		if (yCoords.length != n)
		{
			throw new IllegalArgumentException("Coordinate arrays must have same length");
		}
		this.vertices = new ArrayList<Point2D>(n);
		for (int i = 0; i < n; i++)
		{
			this.vertices.add(new Point2D.Double(xCoords[i], yCoords[i]));
		}
	}
	
	/**
	 * Creates a new polygon from a collection of vertices. A new collection is
	 * created.
	 * 
	 * @param vertices
	 *            the polygon vertices
	 */
	public Polyline2D(Collection<Point2D> vertices)
	{
		this.vertices = new ArrayList<Point2D>(vertices.size());
		this.vertices.addAll(vertices);
	}
	
	
    // ====================================================
    // General methods
    
//	/**
//	 * Returns the bounding box of this polygon.
//	 * 
//	 * @return the bounding box of this polygon.
//	 */
//	public Box2D boundingBox()
//	{
//		double xmin = Double.POSITIVE_INFINITY;
//		double xmax = Double.NEGATIVE_INFINITY;
//		double ymin = Double.POSITIVE_INFINITY;
//		double ymax = Double.NEGATIVE_INFINITY;
//		for (Point2D vertex : this.vertices)
//		{
//			double x = vertex.getX();
//			double y = vertex.getY();
//			xmin = Math.min(xmin, x);
//			xmax = Math.max(xmax, x);
//			ymin = Math.min(ymin, y);
//			ymax = Math.max(ymax, y);
//		}
//		return new Box2D(xmin, xmax, ymin, ymax);
//	}
	
    // ====================================================
    // GUI Tools
    
	/**
	 * Converts this polygon into an ImageJ Polygon ROI.
	 * 
	 * @return the corresponding PolygonRoi
	 */
	public PolygonRoi createRoi()
	{
		// allocate memory for data arrays
		int n = this.vertices.size();
		float[] px = new float[n];
		float[] py = new float[n];
		
		// extract coordinates
		for (int i = 0; i < n; i++)
		{
			Point2D p = this.vertices.get(i);
			px[i] = (float) p.getX();
			py[i] = (float) p.getY();
		}

		// create ROI data structure
		return new PolygonRoi(px, py, n, Roi.POLYGON);
	}

	
    // ====================================================
    // Geometric operations
    
	public Polyline2D smooth()
	{
		// create resulting polyline
		int nv = this.vertexNumber();
		Polyline2D smoothed = new Polyline2D(nv);
		
		// first vertex kept identical
		smoothed.addVertex(this.vertices.get(0));
		
		// iterate over inner vertices
		for (int i = 1; i < nv - 1; i++)
		{
			// init
			float x = 0;
			float y = 0;
			
			// iterate over neighbors
			for (int j = i - 1; j <= i + 1; j++)
			{
				Point2D v = this.vertices.get(i);
				x += v.getX();
				y += v.getY();
			}
			
			// compute average
			smoothed.addVertex(new Point2D.Double(x / 3.0, y / 3.0));
		}
		
		// last vertex kept identical
		smoothed.addVertex(this.vertices.get(nv - 1));

		// return smoothed polyline
		return smoothed;
	}
	
	
	// ====================================================
    // Management of vertices
    
	/**
	 * Returns the number of vertices within this polygon
	 * 
	 * @return the number of vertices within this polygon
	 */
	public int vertexNumber()
	{
		return this.vertices.size();
	}

	/**
	 * Adds a new vertex in this polygon
	 * 
	 * @param position
	 *            the position of the new vertex
	 * @return the index of the newly created vertex
	 */
	public int addVertex(Point2D position)
	{
		int n = this.vertices.size();
		this.vertices.add(position);
		return n;
	}
	
	/**
	 * Returns index at the specific index
	 * 
	 * @param index
	 *            vertex index
	 * @return the vertex at the specified index
	 */
	public Point2D getVertex(int index)
	{
		return this.vertices.get(index);
	}
	
	/**
	 * Changes vertex coordinate at the specified index
	 * 
	 * @param i
	 *            vertex index
	 * @param pos
	 *            the position of the new vertex
	 */
	public void setVertex(int i, Point2D pos)
	{
		this.setVertex(i, pos);
	}

	/**
	 * @return a reference to the inner array of vertices
	 */
	public ArrayList<Point2D> vertices()
	{
		return this.vertices;
	}
	
	/**
	 * @return an iterator over the vertices of this polygon
	 */
	@Override
	public Iterator<Point2D> iterator()
	{
		return this.vertices.iterator();
	}
}
