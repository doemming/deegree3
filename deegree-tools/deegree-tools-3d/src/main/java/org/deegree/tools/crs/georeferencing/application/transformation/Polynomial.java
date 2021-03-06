//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2009 by:
 - Department of Geography, University of Bonn -
 and
 - lat/lon GmbH -

 This library is free software; you can redistribute it and/or modify it under
 the terms of the GNU Lesser General Public License as published by the Free
 Software Foundation; either version 2.1 of the License, or (at your option)
 any later version.
 This library is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 details.
 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation, Inc.,
 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Contact information:

 lat/lon GmbH
 Aennchenstr. 19, 53177 Bonn
 Germany
 http://lat-lon.de/

 Department of Geography, University of Bonn
 Prof. Dr. Klaus Greve
 Postfach 1147, 53001 Bonn
 Germany
 http://www.geographie.uni-bonn.de/deegree/

 e-mail: info@deegree.org
 ----------------------------------------------------------------------------*/
package org.deegree.tools.crs.georeferencing.application.transformation;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.jai.WarpPolynomial;
import javax.vecmath.Point3d;

import org.deegree.commons.utils.Triple;
import org.deegree.cs.coordinatesystems.ICRS;
import org.deegree.cs.exceptions.TransformationException;
import org.deegree.geometry.GeometryFactory;
import org.deegree.geometry.points.Points;
import org.deegree.geometry.primitive.Point;
import org.deegree.geometry.primitive.Ring;
import org.deegree.geometry.standard.points.PointsList;
import org.deegree.tools.crs.georeferencing.application.Scene2DValues;
import org.deegree.tools.crs.georeferencing.application.TransformationPoints;
import org.deegree.tools.crs.georeferencing.model.Footprint;
import org.deegree.tools.crs.georeferencing.model.points.Point4Values;
import org.deegree.tools.crs.georeferencing.model.points.PointResidual;

/**
 * Implementation of the Polynomial transformation.
 * 
 * @author <a href="mailto:thomas@lat-lon.de">Steffen Thomas</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class Polynomial extends AbstractTransformation {

    private float[] passPointsSrc;

    private float[] passPointsDst;

    private WarpPolynomial warp;

    float[] xC;

    float[] yC;

    private double rxLocal;

    private double ryLocal;

    private static final Map<Integer, Integer> requiredPoints = new HashMap<Integer, Integer>();

    static {
        requiredPoints.put( 1, 3 );
        requiredPoints.put( 2, 6 );
        requiredPoints.put( 3, 10 );
        requiredPoints.put( 4, 15 );
    }

    public Polynomial( TransformationPoints points, Footprint footPrint, Scene2DValues sceneValues, ICRS sourceCRS,
                       ICRS targetCRS, int order ) {
        super( points, footPrint, sceneValues, sourceCRS, targetCRS, order );

        if ( !points.isEmpty() ) {
            passPointsSrc = new float[points.getNumPoints()];
            passPointsDst = new float[points.getNumPoints()];
            int counterSrc = 0;
            int counterDst = 0;

            for ( Triple<Point4Values, Point4Values, PointResidual> p : points.getMappedPoints() ) {
                if ( p.first == null || p.second == null ) {
                    continue;
                }
                double x = p.first.getWorldCoords().x;
                double y = p.first.getWorldCoords().y;

                passPointsDst[counterDst] = (float) x;
                passPointsDst[++counterDst] = (float) y;
                counterDst++;
                Point4Values pValue = p.second;
                x = pValue.getWorldCoords().x;
                y = pValue.getWorldCoords().y;
                passPointsSrc[counterSrc] = (float) x;
                passPointsSrc[++counterSrc] = (float) y;
                counterSrc++;

            }

            if ( points.getNumPoints() >= requiredPoints.get( order ) ) {
                warp = WarpPolynomial.createWarp( passPointsSrc, 0, passPointsDst, 0, passPointsSrc.length, 1f, 1f, 1f,
                                                  1f, order );
            }
        }
    }

    @Override
    public List<Ring> computeRingList() {
        if ( warp != null ) {
            calculateResiduals();
            // mean square residual
            rxLocal /= ( passPointsSrc.length / 2 );
            ryLocal /= ( passPointsSrc.length / 2 );

            List<Ring> transformedRingList = new ArrayList<Ring>();
            List<Point> pointList;
            GeometryFactory geom = new GeometryFactory();

            for ( Ring ring : footPrint.getWorldCoordinateRingList() ) {
                pointList = new ArrayList<Point>();
                Point first = null;
                for ( int i = 0; i < ring.getControlPoints().size(); i++ ) {
                    double x = ring.getControlPoints().getX( i );
                    double y = ring.getControlPoints().getY( i );

                    Point2D p = warp.mapDestPoint( new Point2D.Double( x, y ) );
                    double rx = ( p.getX() - rxLocal );
                    double ry = ( p.getY() - ryLocal );

                    pointList.add( geom.createPoint( "point", rx, ry, null ) );
                    if ( first == null ) {
                        first = pointList.get( pointList.size() - 1 );
                    }
                }
                pointList.add( first );
                Points points = new PointsList( pointList );
                transformedRingList.add( geom.createLinearRing( "ring", null, points ) );

            }

            return transformedRingList;
        }
        return null;
    }

    @Override
    public AbstractTransformation.TransformationType getType() {

        return AbstractTransformation.TransformationType.Polynomial;
    }

    @Override
    public PointResidual[] calculateResiduals() {
        PointResidual[] residuals = new PointResidual[points.getNumPoints()];
        if ( warp != null ) {
            rxLocal = 0;
            ryLocal = 0;
            int counter = 0;
            for ( int i = 0; i < passPointsDst.length; i += 2 ) {
                Point2D p = warp.mapDestPoint( new Point2D.Float( passPointsDst[i], passPointsDst[i + 1] ) );
                rxLocal += ( p.getX() - passPointsSrc[i] );
                ryLocal += ( p.getY() - passPointsSrc[i + 1] );
                // System.out.println( "[Polynomial] " + rxLocal + " " + ryLocal );
                residuals[counter++] = new PointResidual( rxLocal, ryLocal );
            }
        }
        return residuals;
    }

    @Override
    public List<Point3d> doTransform( List<Point3d> srcPts )
                            throws TransformationException {
        List<Point3d> list = new ArrayList<Point3d>( srcPts.size() );
        for ( Point3d src : srcPts ) {
            Point3d dest = new Point3d();
            Point2D tmp = new Point2D.Double( src.x, src.y );
            tmp = warp.mapDestPoint( tmp );
            dest.x = tmp.getX();
            dest.y = tmp.getY();
            list.add( dest );
        }
        return list;
    }

    @Override
    public String getImplementationName() {
        return "st1polynomial";
    }

    @Override
    public boolean isIdentity() {
        return false;
    }

}
