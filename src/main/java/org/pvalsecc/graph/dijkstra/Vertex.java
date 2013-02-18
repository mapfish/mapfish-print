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
import java.util.List;

class Vertex<EDGE extends Edge> implements Comparable<Vertex<EDGE>> {
    private long id;
    private double cost;
    private EDGE previousEdge;
    private List<EDGE> edges = new ArrayList<EDGE>(1);

    public Vertex(long id) {
        this.id = id;
        cost = Double.MAX_VALUE;
        previousEdge = null;
    }

    public List<EDGE> getEdges() {
        return edges;
    }

    public long getId() {
        return id;
    }

    public double getCost() {
        return cost;
    }

    public EDGE getPreviousEdge() {
        return previousEdge;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public int compareTo(Vertex<EDGE> other) {
        int result = Double.compare(cost, other.getCost());
        if (result == 0) {
            long id2 = other.getId();
            result = (id < id2) ? -1 : ((id == id2) ? 0 : 1);
        }
        return result;
    }

    public void setPreviousEdge(EDGE previousEdge) {
        this.previousEdge = previousEdge;
    }

    public String toString() {
        return "Vertex{id=" + id + " cost=" + cost + "}";
    }

    public void add(EDGE edge) {
        edges.add(edge);
    }
}
