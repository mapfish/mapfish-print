/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import static org.junit.Assert.assertTrue;


/**
 * Class for comparing an image to another image.
 *
 *
 * @author Jesse on 3/27/14.
 */
public class ImageSimilarity {

    // The reference image "signature" (25 representative pixels, each in R,G,B).
    // We use instances of Color to make things simpler.
    private Color[][] signature;
    // The size of the sampling area.
    private int sampleSize = 15;
    float[] prop = new float[]
            {1f / 10f, 3f / 10f, 5f / 10f, 7f / 10f, 9f / 10f};

    /*
     * The constructor, which creates the GUI and start the image processing task.
     */
    public ImageSimilarity(BufferedImage referenceImage, int sampleSize) throws IOException {
        if ( referenceImage.getWidth() * prop[0] - sampleSize < 0 ||
            referenceImage.getWidth() * prop[4] + sampleSize > referenceImage.getWidth()) {
            throw new IllegalArgumentException("sample size is too big for the image.");
        }
        if ( referenceImage.getHeight() * prop[0] - sampleSize < 0 ||
            referenceImage.getHeight() * prop[4] + sampleSize > referenceImage.getHeight()) {
            throw new IllegalArgumentException("sample size is too big for the image.");
        }
        this.sampleSize = sampleSize;

        signature = calcSignature(referenceImage);
    }

    /*
     * This method calculates and returns signature vectors for the input image.
     */
    private Color[][] calcSignature(BufferedImage i) {
        // Get memory for the signature.
        Color[][] sig = new Color[5][5];
        // For each of the 25 signature values average the pixels around it.
        // Note that the coordinate of the central pixel is in proportions.
        for (int x = 0; x < 5; x++)
            for (int y = 0; y < 5; y++)
                sig[x][y] = averageAround(i, prop[x], prop[y]);
        return sig;
    }

    /*
     * This method averages the pixel values around a central point and return the
     * average as an instance of Color. The point coordinates are proportional to
     * the image.
     */
    private Color averageAround(BufferedImage i, double px, double py) {
        // Get an iterator for the image.
        RandomIter iterator = RandomIterFactory.create(i, null);
        // Get memory for a pixel and for the accumulator.
        double[] pixel = new double[i.getSampleModel().getNumBands()];
        double[] accum = new double[3];
        int numPixels = 0;
        // Sample the pixels.

        for (double x = px * i.getWidth() - sampleSize; x < px * i.getWidth() + sampleSize; x++) {
            for (double y = py * i.getHeight() - sampleSize; y < py * i.getHeight() + sampleSize; y++) {
                iterator.getPixel((int) x, (int) y, pixel);
                accum[0] += pixel[0];
                accum[1] += pixel[1];
                accum[2] += pixel[2];
                numPixels++;
            }
        }
        // Average the accumulated values.
        accum[0] /= numPixels;
        accum[1] /= numPixels;
        accum[2] /= numPixels;
        return new Color((int) accum[0], (int) accum[1], (int) accum[2]);
    }

    /*
     * This method calculates the distance between the signatures of an image and
     * the reference one. The signatures for the image passed as the parameter are
     * calculated inside the method.
     */
    private double calcDistance(BufferedImage other) {
        // Calculate the signature for that image.
        Color[][] sigOther = calcSignature(other);
        // There are several ways to calculate distances between two vectors,
        // we will calculate the sum of the distances between the RGB values of
        // pixels in the same positions.
        double dist = 0;
        for (int x = 0; x < 5; x++)
            for (int y = 0; y < 5; y++) {
                int r1 = signature[x][y].getRed();
                int g1 = signature[x][y].getGreen();
                int b1 = signature[x][y].getBlue();
                int r2 = sigOther[x][y].getRed();
                int g2 = sigOther[x][y].getGreen();
                int b2 = sigOther[x][y].getBlue();
                double tempDist = Math.sqrt((r1 - r2) * (r1 - r2) + (g1 - g2)  * (g1 - g2) + (b1 - b2) * (b1 - b2));
                dist += tempDist;
            }
        return dist;
    }

    /**
     * Check that the other image and the image calculated by this object are within the given distance.
     *
     * @param other the image to compare to "this" image.
     * @param maxDistance the maximum distance between the two images.
     */
    public void assertSimilarity(File other, double maxDistance) throws IOException {
        final double distance = calcDistance(ImageIO.read(other));
        assertTrue("similarity difference between images is: " + distance + " which is greater than the max distance of " + maxDistance,
                distance <= maxDistance);
    }
}
