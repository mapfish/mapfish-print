/*
 * Copyright (C) 2008 Patrick Valsecchi
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  U
 */
package org.pvalsecc.graph.dijkstra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class Dijkstra<EDGE extends Edge> {

    private TreeSet<Vertex<EDGE>> vecticesToVisit = new TreeSet<Vertex<EDGE>>();

    private Map<Long, Vertex<EDGE>> vecticesMap = new HashMap<Long, Vertex<EDGE>>();

    public void addEdge(EDGE edge) {
        Vertex<EDGE> a = vecticesMap.get(edge.getVertexIdA());
        if (a == null) {
            a = new Vertex<EDGE>(edge.getVertexIdA());
            vecticesMap.put(a.getId(), a);
            vecticesToVisit.add(a);
        }

        Vertex<EDGE> b = vecticesMap.get(edge.getVertexIdB());
        if (b == null) {
            b = new Vertex<EDGE>(edge.getVertexIdB());
            vecticesMap.put(b.getId(), b);
            vecticesToVisit.add(b);
        }

        if (!Double.isNaN(edge.getCost(Edge.Direction.A_TO_B))) {
            a.add(edge);
        }

        if (!Double.isNaN(edge.getCost(Edge.Direction.B_TO_A))) {
            b.add(edge);
        }
    }

    public List<EDGE> getShortestPath(long startId, long endId, Double maxCost) {
        compute(startId, endId, maxCost);

        Vertex<EDGE> cur = vecticesMap.get(endId);
        if (cur == null) throw new RuntimeException("Cannot find vertex with id=" + endId);

        if (cur.getPreviousEdge() == null) {
            //path not found
            return null;
        }

        List<EDGE> result = new ArrayList<EDGE>();
        while (cur != null && cur.getId() != startId) {
            EDGE edge = cur.getPreviousEdge();
            if (edge == null) throw new RuntimeException("Broken path around " + cur);
            result.add(edge);
            cur = getOtherVertex(edge, cur);
        }
        return result;
    }

    public void getReachableVectices(long startId, double maxCost, ReachableVecticesVisitor<EDGE> visitor) {
        compute(startId, null, maxCost);
        for (Vertex<EDGE> vertex : vecticesMap.values()) {
            final long id = vertex.getId();
            if (vertex.getPreviousEdge() != null || id == startId) {
                final double cost = vertex.getCost();
                visitor.vertex(id, vertex.getPreviousEdge(), cost);
                List<EDGE> edges = vertex.getEdges();

                for (int i = 0; i < edges.size(); i++) {
                    EDGE edge = edges.get(i);
                    if (getEdgeCost(edge, id) + cost > maxCost) {
                        visitor.exitEdge(id, edge);
                    }
                }
            }
        }
    }

    private void compute(long startId, Long endId, Double maxCost) {
        Vertex<EDGE> start = vecticesMap.get(startId);
        if (start == null) throw new RuntimeException("Cannot find the start vertex with id=" + startId);
        vecticesToVisit.remove(start);
        start.setCost(0);
        vecticesToVisit.add(start);

        while (!vecticesToVisit.isEmpty()) {
            Vertex<EDGE> cur = vecticesToVisit.first();
            vecticesToVisit.remove(cur);
            double curCost = cur.getCost();

            if (curCost == Double.MAX_VALUE) {
                //the next best node has never been reached => the end
                break;
            }
            if (endId != null && cur.getId() == endId) {
                //found the shortest path!
                break;
            }

            List<EDGE> edges = cur.getEdges();
            for (int i = 0; i < edges.size(); i++) {
                EDGE edge = edges.get(i);

                Vertex<EDGE> linkedVertex = getOtherVertex(edge, cur);
                double newCost = curCost + getEdgeCost(edge, cur.getId());
                if (maxCost != null && newCost > maxCost) {
                    //reached the max distance
                } else if (newCost < linkedVertex.getCost()) {
                    boolean removed = vecticesToVisit.remove(linkedVertex);

                    linkedVertex.setCost(newCost);
                    linkedVertex.setPreviousEdge(edge);

                    if (removed) {
                        //re-sort this vertex
                        vecticesToVisit.add(linkedVertex);
                    }
                }
            }
        }
    }

    private Vertex<EDGE> getOtherVertex(EDGE edge, Vertex<EDGE> cur) {
        long linkedId;
        if (cur.getId() == edge.getVertexIdA()) {
            linkedId = edge.getVertexIdB();
        } else {
            linkedId = edge.getVertexIdA();
        }
        Vertex<EDGE> result = vecticesMap.get(linkedId);
        if (result == null) throw new RuntimeException("Cannot find linked vertex with id=" + linkedId);
        return result;
    }

    private double getEdgeCost(EDGE edge, long fromId) {
        if (fromId == edge.getVertexIdA()) return edge.getCost(Edge.Direction.A_TO_B);
        if (fromId == edge.getVertexIdB()) return edge.getCost(Edge.Direction.B_TO_A);
        throw new RuntimeException("Inconsistency, edge " + edge + " is not linking vertex " + fromId);
    }

    public int getNbVectices() {
        return vecticesMap.size();
    }
}
