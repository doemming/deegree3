//$HeadURL$
/*----------------    FILE HEADER  ------------------------------------------

 This file is part of deegree.
 Copyright (C) 2001-2009 by:
 EXSE, Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 Contact:

 Andreas Poth  
 lat/lon GmbH 
 Aennchenstr. 19
 53115 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Prof. Dr. Klaus Greve
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: greve@giub.uni-bonn.de


 ---------------------------------------------------------------------------*/
package org.deegree.geometry.standard.primitive;

import java.util.Collections;
import java.util.List;

import org.deegree.crs.CRS;
import org.deegree.geometry.primitive.LineString;
import org.deegree.geometry.primitive.Point;
import org.deegree.geometry.primitive.curvesegments.CurveSegment;
import org.deegree.geometry.primitive.curvesegments.LineStringSegment;
import org.deegree.geometry.standard.curvesegments.DefaultLineStringSegment;

/**
 * Default implementation of {@link LineString}.
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider </a>
 * @author last edited by: $Author:$
 * 
 * @version $Revision:$, $Date:$
 */
public class DefaultLineString extends DefaultCurve implements LineString {

    private LineStringSegment singleSegment;

    /**
     * 
     * @param id
     * @param crs
     * @param controlPoints
     */
    public DefaultLineString( String id, CRS crs, List<Point> controlPoints ) {
        super( id, crs, Collections.singletonList( (CurveSegment) new DefaultLineStringSegment( controlPoints ) ) );
        singleSegment = (LineStringSegment) getCurveSegments().get( 0 );
    }

    @Override
    public CurveType getCurveType() {
        return CurveType.LineString;
    }

    @Override
    public List<Point> getControlPoints() {
        return singleSegment.getControlPoints();
    }

    @Override
    public int getCoordinateDimension() {
        return singleSegment.getCoordinateDimension();
    }

    @Override
    public double[] getAsArray() {
        List<Point> points = getControlPoints();
        double[] result = null;
        if ( points != null && !points.isEmpty() ) {
            int dim = getCoordinateDimension();
            result = new double[points.size() * dim];
            int i = 0;
            for ( Point p : points ) {
                if ( p != null ) {
                    for ( int d = 0; d < dim; ++d ) {
                        result[i++] = p.get( d );
                    }
                }
            }
        }
        return result;
    }

    @Override
    public LineString getAsLineString() {
        return this;
    }
}
