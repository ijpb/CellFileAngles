/**
 * 
 */
package ijt.cellangles.geom;

import static org.junit.Assert.*;

import java.awt.Point;

import org.junit.Test;

import ijt.geom.Geometry;

/**
 * @author dlegland
 *
 */
public class GeometryTest
{

	@Test
	public final void testDistancePointLine_Horiz()
	{
		Point lineP1 = new Point(10, 10);
		Point lineP2 = new Point(30, 10);
		
		Point p1 = new Point(5, 5);		double exp1 = 5;
		Point p2 = new Point(20, 0);	double exp2 = 10;
		Point p3 = new Point(5, 25);	double exp3 = 15;
		
		double dist1 = Geometry.distancePointLine(p1, lineP1, lineP2);
		assertEquals(exp1, dist1, .001);

		double dist2 = Geometry.distancePointLine(p2, lineP1, lineP2);
		assertEquals(exp2, dist2, .001);
		
		double dist3 = Geometry.distancePointLine(p3, lineP1, lineP2);
		assertEquals(exp3, dist3, .001);
	}

	@Test
	public final void testDistancePointLine_Vert()
	{
		Point lineP1 = new Point(10, 10);
		Point lineP2 = new Point(10, 30);
		
		Point p1 = new Point(5, 5);		double exp1 = 5;
		Point p2 = new Point(0, 20);	double exp2 = 10;
		Point p3 = new Point(25, 5);	double exp3 = 15;
		
		double dist1 = Geometry.distancePointLine(p1, lineP1, lineP2);
		assertEquals(exp1, dist1, .001);

		double dist2 = Geometry.distancePointLine(p2, lineP1, lineP2);
		assertEquals(exp2, dist2, .001);
		
		double dist3 = Geometry.distancePointLine(p3, lineP1, lineP2);
		assertEquals(exp3, dist3, .001);
	}

	@Test
	public final void testDistancePointLine_Diag()
	{
		Point lineP1 = new Point(10, 10);
		Point lineP2 = new Point(10+40, 10+30);
		
		Point p1 = new Point(10+70, 10-10);		double exp1 = 50;
		Point p2 = new Point(10+50, 10+100);	double exp2 = 50;
		Point p3 = new Point(10+10, 10+70);		double exp3 = 50;
		
		double dist1 = Geometry.distancePointLine(p1, lineP1, lineP2);
		assertEquals(exp1, dist1, .001);

		double dist2 = Geometry.distancePointLine(p2, lineP1, lineP2);
		assertEquals(exp2, dist2, .001);
		
		double dist3 = Geometry.distancePointLine(p3, lineP1, lineP2);
		assertEquals(exp3, dist3, .001);
	}

	@Test
	public final void testIsLeftSide_Diag()
	{
		Point lineP1 = new Point(10, 10);
		Point lineP2 = new Point(10+40, 10+30);
		
		Point p1 = new Point(10+70, 10-10);
		Point p2 = new Point(10+50, 10+100);
		Point p3 = new Point(10+10, 10+70);
		
		assertTrue(!Geometry.isLeftSide(p1, lineP1, lineP2));
		assertTrue(Geometry.isLeftSide(p2, lineP1, lineP2));
		assertTrue(Geometry.isLeftSide(p3, lineP1, lineP2));
	}
}
