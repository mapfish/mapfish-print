package org.mapfish.print.map.geotools.grid;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;
import org.mapfish.print.ExceptionUtils;
import org.opengis.referencing.cs.AxisDirection;

/**
 * The LinearCoordinateSequence class.
 */
public final class LinearCoordinateSequence implements CoordinateSequence, Cloneable {
    private int dimension;
    private double axis0Origin;
    private double axis1Origin;
    private int variableAxis;
    private int numPoints;
    private double spacing;
    private AxisDirection ordinate0AxisDirection;

    /**
     * Set the origin point of the sequence.
     *
     * @param axis1Origin origin of axis 1
     * @param axis2Origin origin of axis 2
     */
    public LinearCoordinateSequence setOrigin(final double axis1Origin, final double axis2Origin) {
        this.axis0Origin = axis1Origin;
        this.axis1Origin = axis2Origin;
        return this;
    }

    /**
     * Set the axis which will be variable.
     *
     * @param variableAxis the axis which will be variable.
     */
    public LinearCoordinateSequence setVariableAxis(final int variableAxis) {
        this.variableAxis = variableAxis;
        return this;
    }

    /**
     * Set the number of points in the sequence.
     *
     * @param numPoints the number of points in the sequence.
     */
    public LinearCoordinateSequence setNumPoints(final int numPoints) {
        this.numPoints = numPoints;
        return this;
    }

    /**
     * Set the space between points.
     *
     * @param spacing space between points.
     */
    public LinearCoordinateSequence setSpacing(final double spacing) {
        this.spacing = spacing;
        return this;
    }

    /**
     * Set the axisDirection of the first ordinate.
     *
     * @param ordinate0AxisDirection the axisDirection of the first ordinate.
     */
    public LinearCoordinateSequence setOrdinate0AxisDirection(final AxisDirection ordinate0AxisDirection) {
        this.ordinate0AxisDirection = ordinate0AxisDirection;
        return this;
    }

    @Override
    public int getDimension() {
        return this.dimension;
    }

    /**
     * Set the number of dimensions.
     *
     * @param dimension the number of dimensions for each point.
     */
    public LinearCoordinateSequence setDimension(final int dimension) {
        this.dimension = dimension;
        return this;
    }

    @Override
    public Coordinate getCoordinate(final int i) {
        final Coordinate coord = new Coordinate();
        getCoordinate(i, coord);
        return coord;
    }

    @Override
    public Coordinate getCoordinateCopy(final int i) {
        final Coordinate coord = new Coordinate();
        getCoordinate(i, coord);
        return coord;
    }

    @Override
    public void getCoordinate(final int index, final Coordinate coord) {
        Coordinate finalCoord = coord;
        if (coord == null) {
            finalCoord = new Coordinate();
        }

        for (int i = 0; i < this.dimension; i++) {
            finalCoord.setOrdinate(i, getOrdinate(index, i));
        }
    }

    @Override
    public double getX(final int index) {
        if (ordinate0IsY()) {
            return getOrdinate(0, 1);
        } else {
            return getOrdinate(0, 0);
        }
    }

    @Override
    public double getY(final int index) {
        if (ordinate0IsY()) {
            return getOrdinate(0, 0);
        } else {
            return getOrdinate(0, 1);
        }
    }

    @Override
    public double getOrdinate(final int index, final int ordinateIndex) {
        double ordinate;
        if (ordinateIndex == 0) {
            ordinate = this.axis0Origin;
        } else if (ordinateIndex == 1) {
            ordinate = this.axis1Origin;
        } else {
            return 0;
        }

        if (this.variableAxis == ordinateIndex) {
            ordinate += (this.spacing * index);
        }

        return ordinate;
    }

    @Override
    public int size() {
        return this.numPoints;
    }

    @Override
    public void setOrdinate(final int index, final int ordinateIndex, final double value) {
        throw new UnsupportedOperationException("This coordinate sequence implementation is read only");
    }

    @Override
    public Coordinate[] toCoordinateArray() {
        final Coordinate[] coordinates = new Coordinate[this.numPoints];
        for (int i = 0; i < coordinates.length; i++) {
            coordinates[i] = getCoordinate(i);

        }
        return coordinates;
    }

    @Override
    public Envelope expandEnvelope(final Envelope env) {
        Envelope envelope = env;
        if (envelope == null) {
            envelope = new Envelope();
        }
        for (int i = 0; i < this.numPoints; i++) {
            envelope.expandToInclude(getOrdinate(i, 0), getOrdinate(i, 1));
        }

        return envelope;
    }

    private boolean ordinate0IsY() {
        return this.ordinate0AxisDirection == AxisDirection.DOWN ||
                this.ordinate0AxisDirection == AxisDirection.UP ||
                this.ordinate0AxisDirection == AxisDirection.NORTH ||
                this.ordinate0AxisDirection == AxisDirection.SOUTH ||
                this.ordinate0AxisDirection == AxisDirection.DISPLAY_DOWN ||
                this.ordinate0AxisDirection == AxisDirection.DISPLAY_UP;
    }

    @Override
    public Object clone() {
        return copy();
    }

    @Override
    public CoordinateSequence copy() {
        try {
            final LinearCoordinateSequence clone = (LinearCoordinateSequence) super.clone();
            clone.numPoints = this.numPoints;
            clone.axis0Origin = this.axis0Origin;
            clone.axis1Origin = this.axis1Origin;
            clone.variableAxis = this.variableAxis;
            clone.dimension = this.dimension;
            clone.ordinate0AxisDirection = this.ordinate0AxisDirection;
            clone.spacing = this.spacing;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw ExceptionUtils.getRuntimeException(e);
        }
    }
}
