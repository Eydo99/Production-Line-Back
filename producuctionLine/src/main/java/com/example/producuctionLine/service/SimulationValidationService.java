package com.example.producuctionLine.service;

import com.example.producuctionLine.model.Connection;
import com.example.producuctionLine.model.Machine;
import com.example.producuctionLine.model.Queue;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for validating simulation configuration before start
 * Ensures all nodes are properly connected and can function correctly
 */
@Service
public class SimulationValidationService {

    /**
     * Validation result container
     */
    public static class ValidationResult {
        private final boolean isValid;
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(boolean isValid, List<String> errors, List<String> warnings) {
            this.isValid = isValid;
            this.errors = errors;
            this.warnings = warnings;
        }

        public boolean isValid() {
            return isValid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            if (isValid) {
                sb.append("✅ Configuration is valid and ready to run!");
                if (!warnings.isEmpty()) {
                    sb.append("\n\nWarnings:\n");
                    warnings.forEach(w -> sb.append(w).append("\n"));
                }
            } else {
                sb.append("❌ Configuration has errors:\n\n");
                errors.forEach(e -> sb.append(e).append("\n"));
                if (!warnings.isEmpty()) {
                    sb.append("\nWarnings:\n");
                    warnings.forEach(w -> sb.append(w).append("\n"));
                }
            }
            return sb.toString();
        }
    }

    /**
     * Node validation info
     */
    private static class NodeValidation {
        String nodeId;
        String nodeType; // "queue" or "machine"
        boolean hasInput;
        boolean hasOutput;
        boolean isIsolated;

        NodeValidation(String nodeId, String nodeType, boolean hasInput, boolean hasOutput) {
            this.nodeId = nodeId;
            this.nodeType = nodeType;
            this.hasInput = hasInput;
            this.hasOutput = hasOutput;
            this.isIsolated = !hasInput && !hasOutput;
        }
    }

    /**
     * Main validation method - checks all requirements before starting simulation
     */
    public ValidationResult validateSimulation(
            Map<String, Queue> queues,
            Map<String, Machine> machines,
            List<Connection> connections) {

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 1. Check if there are any nodes at all
        if (queues.isEmpty() && machines.isEmpty()) {
            errors.add("❌ No nodes exist. Add at least one queue and one machine.");
            return new ValidationResult(false, errors, warnings);
        }

        // 2. Check minimum requirements
        if (queues.isEmpty()) {
            errors.add("❌ No queues exist. Add at least one queue to hold products.");
        }

        if (machines.isEmpty()) {
            errors.add("❌ No machines exist. Add at least one machine to process products.");
        }

        if (connections.isEmpty()) {
            errors.add("❌ No connections exist. Connect queues to machines (Q→M→Q pattern).");
        }

        // Return early if basic requirements aren't met
        if (!errors.isEmpty()) {
            return new ValidationResult(false, errors, warnings);
        }

        // 3. Validate all nodes have proper connections
        List<NodeValidation> nodeValidations = validateNodeConnections(queues, machines, connections);

        // Find isolated nodes
        nodeValidations.stream()
                .filter(n -> n.isIsolated)
                .forEach(node -> errors.add(
                        String.format("❌ %s \"%s\" is isolated (no connections)",
                                node.nodeType.toUpperCase(), node.nodeId)));

        // Find machines with only input (dead-end)
        nodeValidations.stream()
                .filter(n -> n.nodeType.equals("machine") && n.hasInput && !n.hasOutput)
                .forEach(node -> errors.add(
                        String.format("❌ Machine \"%s\" has input but no output queue (products will be stuck)",
                                node.nodeId)));

        // Find machines with only output (no source)
        nodeValidations.stream()
                .filter(n -> n.nodeType.equals("machine") && !n.hasInput && n.hasOutput)
                .forEach(node -> errors.add(
                        String.format("❌ Machine \"%s\" has output but no input queue (will never receive products)",
                                node.nodeId)));

        // 4. Check for at least one entry point (source queue)
        long sourceQueues = nodeValidations.stream()
                .filter(n -> n.nodeType.equals("queue") && !n.hasInput && n.hasOutput)
                .count();

        if (sourceQueues == 0) {
            errors.add("❌ No source queue found. At least one queue must have no input (entry point for products).");
        }

        // 5. Validate at least one complete Q→M→Q path exists
        if (!hasCompleteProductionPath(queues, machines, connections)) {
            errors.add("❌ No complete production path (Q→M→Q) exists. Products need a full journey from source to destination.");
        }

        // 6. Check for circular dependencies (warning)
        List<List<String>> cycles = detectCycles(connections);
        if (!cycles.isEmpty()) {
            warnings.add(String.format("⚠️ Detected %d circular path(s). Products may loop indefinitely.", cycles.size()));
        }

        // 7. Validate connection patterns
        List<Connection> invalidConnections = validateConnectionPatterns(connections);
        invalidConnections.forEach(conn -> errors.add(
                String.format("❌ Invalid connection: %s → %s. Must follow Q→M or M→Q pattern.",
                        conn.getFromId(), conn.getToId())));

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    /**
     * Validate connections for each node
     */
    private List<NodeValidation> validateNodeConnections(
            Map<String, Queue> queues,
            Map<String, Machine> machines,
            List<Connection> connections) {

        List<NodeValidation> validations = new ArrayList<>();

        // Validate all queues
        queues.values().forEach(queue -> {
            boolean hasInput = connections.stream()
                    .anyMatch(c -> c.getToId().equals(queue.getId()));
            boolean hasOutput = connections.stream()
                    .anyMatch(c -> c.getFromId().equals(queue.getId()));
            validations.add(new NodeValidation(queue.getId(), "queue", hasInput, hasOutput));
        });

        // Validate all machines
        machines.values().forEach(machine -> {
            boolean hasInput = connections.stream()
                    .anyMatch(c -> c.getToId().equals(machine.getName()));
            boolean hasOutput = connections.stream()
                    .anyMatch(c -> c.getFromId().equals(machine.getName()));
            validations.add(new NodeValidation(machine.getName(), "machine", hasInput, hasOutput));
        });

        return validations;
    }

    /**
     * Check if at least one complete Q→M→Q path exists
     */
    private boolean hasCompleteProductionPath(
            Map<String, Queue> queues,
            Map<String, Machine> machines,
            List<Connection> connections) {

        // Build adjacency list
        Map<String, List<String>> graph = new HashMap<>();
        connections.forEach(conn -> {
            graph.computeIfAbsent(conn.getFromId(), k -> new ArrayList<>())
                    .add(conn.getToId());
        });

        // Check if any queue can reach a machine and then another queue
        for (Queue queue : queues.values()) {
            List<String> reachableMachines = graph.getOrDefault(queue.getId(), Collections.emptyList());

            for (String machineId : reachableMachines) {
                List<String> outputQueues = graph.getOrDefault(machineId, Collections.emptyList());
                if (!outputQueues.isEmpty()) {
                    System.out.println("✅ Valid path found: " + queue.getId() +
                            " → " + machineId + " → " + outputQueues.get(0));
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Detect circular paths in the connection graph
     */
    private List<List<String>> detectCycles(List<Connection> connections) {
        Map<String, List<String>> graph = new HashMap<>();
        connections.forEach(conn -> {
            graph.computeIfAbsent(conn.getFromId(), k -> new ArrayList<>())
                    .add(conn.getToId());
        });

        List<List<String>> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String node : graph.keySet()) {
            if (!visited.contains(node)) {
                dfsCycleDetection(node, graph, visited, recursionStack, new ArrayList<>(), cycles);
            }
        }

        return cycles;
    }

    /**
     * DFS helper for cycle detection
     */
    private void dfsCycleDetection(
            String node,
            Map<String, List<String>> graph,
            Set<String> visited,
            Set<String> recursionStack,
            List<String> path,
            List<List<String>> cycles) {

        visited.add(node);
        recursionStack.add(node);
        path.add(node);

        List<String> neighbors = graph.getOrDefault(node, Collections.emptyList());
        for (String neighbor : neighbors) {
            if (!visited.contains(neighbor)) {
                dfsCycleDetection(neighbor, graph, visited, recursionStack,
                        new ArrayList<>(path), cycles);
            } else if (recursionStack.contains(neighbor)) {
                // Found a cycle
                int cycleStart = path.indexOf(neighbor);
                List<String> cycle = new ArrayList<>(path.subList(cycleStart, path.size()));
                cycle.add(neighbor);
                cycles.add(cycle);
            }
        }

        recursionStack.remove(node);
    }

    /**
     * Validate connection patterns (must be Q→M or M→Q)
     */
    private List<Connection> validateConnectionPatterns(List<Connection> connections) {
        return connections.stream()
                .filter(conn -> {
                    char fromType = conn.getFromId().charAt(0);
                    char toType = conn.getToId().charAt(0);
                    // Invalid if same type (Q→Q or M→M)
                    return fromType == toType;
                })
                .collect(Collectors.toList());
    }
}