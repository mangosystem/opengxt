/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2016, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.process.spatialstatistics.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.util.logging.Logging;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.AbstractNode;
import com.vividsolutions.jts.index.strtree.Boundable;
import com.vividsolutions.jts.index.strtree.ItemBoundable;
import com.vividsolutions.jts.index.strtree.ItemDistance;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.util.PriorityQueue;

/**
 * K-Nearest Neighbor Search.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source https://github.com/jiayuasu/JTSplus/blob/master/src/main/java/com/vividsolutions/jts/index/strtree/STRtree.java
 */
public class KnnSearch {
    protected static final Logger LOGGER = Logging.getLogger(KnnSearch.class);

    private STRtree spatialIndex;

    public KnnSearch(STRtree spatialIndex) {
        this.spatialIndex = spatialIndex;
    }

    public Object[] kNearestNeighbour(Envelope env, Object item, ItemDistance itemDistance, int k) {
        Boundable bnd = new ItemBoundable(env, item);
        BoundablePair bp = new BoundablePair(spatialIndex.getRoot(), bnd, itemDistance);
        return nearestNeighbour(bp, Double.POSITIVE_INFINITY, k);
    }

    private Object[] nearestNeighbour(BoundablePair initBndPair, double maxDistance, int k) {
        double distanceLowerBound = maxDistance;

        // initialize internal structures
        PriorityQueue priQ = new PriorityQueue();

        // initialize queue
        priQ.add(initBndPair);

        List<Object> kNearestNeighbors = new ArrayList<Object>();
        List<Double> kNearestDistances = new ArrayList<Double>();
        while (!priQ.isEmpty() && distanceLowerBound >= 0.0) {
            // pop head of queue and expand one side of pair
            BoundablePair bndPair = (BoundablePair) priQ.poll();
            double currentDistance = bndPair.getDistance();

            /**
             * If the distance for the first node in the queue is >= the current maximum distance in the k queue , all other nodes in the queue must
             * also have a greater distance. So the current minDistance must be the true minimum, and we are done.
             */

            if (currentDistance >= distanceLowerBound && kNearestDistances.size() >= k) {
                break;
            }
            /**
             * If the pair members are leaves then their distance is the exact lower bound. Update the distanceLowerBound to reflect this (which must
             * be smaller, due to the test immediately prior to this).
             */
            if (bndPair.isLeaves()) {
                if (kNearestDistances.size() > 0 && kNearestDistances.size() < k) {

                    int position = Collections.binarySearch(kNearestDistances, currentDistance);
                    if (position < 0) {
                        position = -position - 1;
                    }
                    kNearestNeighbors.add(position,
                            ((ItemBoundable) bndPair.getBoundable(0)).getItem());
                    kNearestDistances.add(position, currentDistance);
                } else if (kNearestDistances.size() >= k) {

                    if (currentDistance < kNearestDistances.get(kNearestDistances.size() - 1)) {
                        int position = Collections.binarySearch(kNearestDistances, currentDistance);
                        if (position < 0) {
                            position = -position - 1;
                        }
                        kNearestNeighbors.add(position,
                                ((ItemBoundable) bndPair.getBoundable(0)).getItem());
                        kNearestDistances.add(position, currentDistance);
                        // assert kNearestNeighbors.size() > k;
                        kNearestNeighbors.remove(kNearestNeighbors.size() - 1);
                        kNearestDistances.remove(kNearestDistances.size() - 1);
                    }
                } else if (kNearestDistances.size() == 0) {
                    kNearestNeighbors.add(((ItemBoundable) bndPair.getBoundable(0)).getItem());
                    kNearestDistances.add(currentDistance);
                } else {
                    try {
                        throw new Exception("Should never reach here");
                    } catch (Exception e1) {
                        LOGGER.log(Level.WARNING, e1.getMessage());
                    }
                }

                distanceLowerBound = kNearestDistances.get(kNearestDistances.size() - 1);
            } else {
                /**
                 * Otherwise, expand one side of the pair, (the choice of which side to expand is heuristically determined) and insert the new
                 * expanded pairs into the queue
                 */
                bndPair.expandToQueue(priQ, distanceLowerBound);
            }
        }
        // done - return items with min distance

        return kNearestNeighbors.toArray(new Object[kNearestNeighbors.size()]);
    }

    /**
     * A pair of {@link Boundable}s, whose leaf items support a distance metric between them. Used to compute the distance between the members, and to
     * expand a member relative to the other in order to produce new branches of the Branch-and-Bound evaluation tree. Provides an ordering based on
     * the distance between the members, which allows building a priority queue by minimum distance.
     * 
     * @author Martin Davis
     * 
     * @source https://github.com/locationtech/jts/blob/master/modules/core/src/main/java/org/locationtech/jts/index/strtree/BoundablePair.java
     * 
     */
    @SuppressWarnings("rawtypes")
    static final class BoundablePair implements Comparable {
        private Boundable boundable1;

        private Boundable boundable2;

        private double distance;

        private ItemDistance itemDistance;

        public BoundablePair(Boundable boundable1, Boundable boundable2, ItemDistance itemDistance) {
            this.boundable1 = boundable1;
            this.boundable2 = boundable2;
            this.itemDistance = itemDistance;
            distance = distance();
        }

        /**
         * Gets one of the member {@link Boundable}s in the pair (indexed by [0, 1]).
         * 
         * @param i the index of the member to return (0 or 1)
         * @return the chosen member
         */
        public Boundable getBoundable(int i) {
            if (i == 0)
                return boundable1;
            return boundable2;
        }

        /**
         * Computes the distance between the {@link Boundable}s in this pair. The boundables are either composites or leaves. If either is composite,
         * the distance is computed as the minimum distance between the bounds. If both are leaves, the distance is computed by
         * {@link #itemDistance(ItemBoundable, ItemBoundable)}.
         * 
         * @return
         */
        private double distance() {
            // if items, compute exact distance
            if (isLeaves()) {
                return itemDistance
                        .distance((ItemBoundable) boundable1, (ItemBoundable) boundable2);
            }
            // otherwise compute distance between bounds of boundables
            return ((Envelope) boundable1.getBounds())
                    .distance(((Envelope) boundable2.getBounds()));
        }

        /**
         * Gets the minimum possible distance between the Boundables in this pair. If the members are both items, this will be the exact distance
         * between them. Otherwise, this distance will be a lower bound on the distances between the items in the members.
         * 
         * @return the exact or lower bound distance for this pair
         */
        public double getDistance() {
            return distance;
        }

        /**
         * Compares two pairs based on their minimum distances
         */
        public int compareTo(Object o) {
            BoundablePair nd = (BoundablePair) o;
            if (distance < nd.distance)
                return -1;
            if (distance > nd.distance)
                return 1;
            return 0;
        }

        /**
         * Tests if both elements of the pair are leaf nodes
         * 
         * @return true if both pair elements are leaf nodes
         */
        public boolean isLeaves() {
            return !(isComposite(boundable1) || isComposite(boundable2));
        }

        public static boolean isComposite(Object item) {
            return (item instanceof AbstractNode);
        }

        private static double area(Boundable b) {
            return ((Envelope) b.getBounds()).getArea();
        }

        /**
         * For a pair which is not a leaf (i.e. has at least one composite boundable) computes a list of new pairs from the expansion of the larger
         * boundable.
         * 
         */
        public void expandToQueue(PriorityQueue priQ, double minDistance) {
            boolean isComp1 = isComposite(boundable1);
            boolean isComp2 = isComposite(boundable2);

            /**
             * HEURISTIC: If both boundable are composite, choose the one with largest area to expand. Otherwise, simply expand whichever is
             * composite.
             */
            if (isComp1 && isComp2) {
                if (area(boundable1) > area(boundable2)) {
                    expand(boundable1, boundable2, priQ, minDistance);
                    return;
                } else {
                    expand(boundable2, boundable1, priQ, minDistance);
                    return;
                }
            } else if (isComp1) {
                expand(boundable1, boundable2, priQ, minDistance);
                return;
            } else if (isComp2) {
                expand(boundable2, boundable1, priQ, minDistance);
                return;
            }

            throw new IllegalArgumentException("neither boundable is composite");
        }

        private void expand(Boundable bndComposite, Boundable bndOther, PriorityQueue priQ,
                double minDistance) {
            List children = ((AbstractNode) bndComposite).getChildBoundables();
            for (Iterator i = children.iterator(); i.hasNext();) {
                Boundable child = (Boundable) i.next();
                BoundablePair bp = new BoundablePair(child, bndOther, itemDistance);
                // only add to queue if this pair might contain the closest points
                // MD - it's actually faster to construct the object rather than called distance(child, bndOther)!
                if (bp.getDistance() < minDistance) {
                    priQ.add(bp);
                }
            }
        }
    }

}
