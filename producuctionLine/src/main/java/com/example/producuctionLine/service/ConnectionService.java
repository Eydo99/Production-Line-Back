package com.example.producuctionLine.service;

import com.example.producuctionLine.model.Connection;
import com.example.producuctionLine.model.Machine;
import com.example.producuctionLine.model.Queue;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service to manage connections between queues and machines.
 * Handles connection creation, deletion, and validation.
 * Implements Observer Pattern wiring for Q→M connections.
 * 
 * @author Refactored from SimulationManager
 */
@Service
public class ConnectionService {

    private final List<Connection> connections = new CopyOnWriteArrayList<>();

    /**
     * Create connection between nodes.
     * Validates Q→M→Q pattern.
     * Implements Observer Pattern wiring.
     *
     * @param fromId   Source node ID (Queue or Machine)
     * @param toId     Target node ID (Queue or Machine)
     * @param queues   Map of all queues
     * @param machines Map of all machines
     * @return Created connection
     * @throws IllegalArgumentException if connection is invalid
     */
    public Connection createConnection(String fromId, String toId,
            Map<String, Queue> queues,
            Map<String, Machine> machines) {
        boolean fromExists = queues.containsKey(fromId) || machines.containsKey(fromId);
        boolean toExists = queues.containsKey(toId) || machines.containsKey(toId);

        if (!fromExists) {
            throw new IllegalArgumentException("Source node '" + fromId + "' does not exist");
        }
        if (!toExists) {
            throw new IllegalArgumentException("Target node '" + toId + "' does not exist");
        }

        char fromType = fromId.charAt(0);
        char toType = toId.charAt(0);

        if (fromType == toType) {
            throw new IllegalArgumentException(
                    "Invalid connection: Cannot connect " + fromType + " to " + toType +
                            ". Must alternate between Queue and Machine (Q→M or M→Q)");
        }

        Connection connection = new Connection(fromId, toId);

        boolean exists = connections.stream()
                .anyMatch(c -> c.getFromId().equals(fromId) && c.getToId().equals(toId));

        if (exists) {
            throw new IllegalArgumentException("Connection already exists");
        }

        connections.add(connection);

        if (fromType == 'Q' && toType == 'M') {
            Queue queue = queues.get(fromId);
            Machine machine = machines.get(toId);
            machine.addInputQueue(queue);
            queue.registerObserver(machine);
            System.out.println("🔗 Connected: Queue " + fromId + " → Machine " + toId);
            System.out.println("   Observer Pattern: " + toId + " now observes " + fromId);
        } else if (fromType == 'M' && toType == 'Q') {
            Machine machine = machines.get(fromId);
            Queue queue = queues.get(toId);
            machine.addOutputQueue(queue);
            System.out.println("🔗 Connected: Machine " + fromId + " → Queue " + toId);
        }

        return connection;
    }

    /**
     * Delete a connection between nodes.
     * Cleans up observer registrations and queue references.
     *
     * @param fromId   Source node ID
     * @param toId     Target node ID
     * @param queues   Map of all queues
     * @param machines Map of all machines
     */
    public void deleteConnection(String fromId, String toId,
            Map<String, Queue> queues,
            Map<String, Machine> machines) {
        connections.removeIf(conn -> conn.getFromId().equals(fromId) && conn.getToId().equals(toId));

        char fromType = fromId.charAt(0);
        char toType = toId.charAt(0);

        if (fromType == 'Q' && toType == 'M') {
            Queue queue = queues.get(fromId);
            Machine machine = machines.get(toId);
            if (queue != null && machine != null) {
                queue.unregisterObserver(machine);
                machine.removeInputQueue(queue);
            }
        } else if (fromType == 'M' && toType == 'Q') {
            Machine machine = machines.get(fromId);
            Queue queue = queues.get(toId);
            if (machine != null && queue != null) {
                machine.removeOutputQueue(queue);
            }
        }

        System.out.println("🔌 Connection deleted: " + fromId + " → " + toId);
    }

    /**
     * Get all connections (returns a defensive copy).
     *
     * @return List of all connections
     */
    public List<Connection> getConnections() {
        return new ArrayList<>(connections);
    }

    /**
     * Get the internal connection list (for snapshot operations).
     *
     * @return The internal connection list
     */
    public List<Connection> getConnectionsInternal() {
        return connections;
    }

    /**
     * Clear all connections.
     */
    public void clearConnections() {
        connections.clear();
    }

    /**
     * Add a connection directly (used during snapshot restoration).
     *
     * @param connection The connection to add
     */
    public void addConnection(Connection connection) {
        connections.add(connection);
    }

    /**
     * Remove connections involving a specific node.
     *
     * @param nodeId The node ID to remove connections for
     */
    public void removeConnectionsForNode(String nodeId) {
        connections.removeIf(conn -> conn.getFromId().equals(nodeId) || conn.getToId().equals(nodeId));
    }

    /**
     * Get connections involving a specific node.
     *
     * @param nodeId The node ID
     * @return List of connections involving this node
     */
    public List<Connection> getConnectionsForNode(String nodeId) {
        List<Connection> result = new ArrayList<>();
        for (Connection conn : connections) {
            if (conn.getFromId().equals(nodeId) || conn.getToId().equals(nodeId)) {
                result.add(conn);
            }
        }
        return result;
    }

    /**
     * Check if a valid Q→M path exists.
     *
     * @return true if at least one Q→M connection exists
     */
    public boolean hasValidPath() {
        return connections.stream()
                .anyMatch(c -> c.getFromId().startsWith("Q") && c.getToId().startsWith("M"));
    }

    /**
     * Get the connection count.
     *
     * @return Number of connections
     */
    public int getConnectionCount() {
        return connections.size();
    }
}
