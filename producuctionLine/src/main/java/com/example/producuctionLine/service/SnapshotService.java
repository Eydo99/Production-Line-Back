package com.example.producuctionLine.service;

import com.example.producuctionLine.model.*;
import com.example.producuctionLine.model.snapshot.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SnapshotService {

    private final SimulationCaretaker caretaker = new SimulationCaretaker();

    
    public static class RestoreResult {
        public final int queueCounter;
        public final int machineCounter;
        public final int totalProductsGenerated;
        public final int totalProductsProcessed;
        public final long randomSeed;

        public RestoreResult(int queueCounter, int machineCounter,
                int totalProductsGenerated, int totalProductsProcessed,
                long randomSeed) {
            this.queueCounter = queueCounter;
            this.machineCounter = machineCounter;
            this.totalProductsGenerated = totalProductsGenerated;
            this.totalProductsProcessed = totalProductsProcessed;
            this.randomSeed = randomSeed;
        }
    }

    public SimulationSnapshot createSnapshot(
            Map<String, Queue> queues,
            Map<String, Machine> machines,
            List<Connection> connections,
            int queueCounter,
            int machineCounter,
            int totalProductsGenerated,
            int totalProductsProcessed,
            long simulationDuration,
            List<ProductSnapshot> recordedProducts,
            long randomSeed) {
        SimulationSnapshot snapshot = new SimulationSnapshot();
        snapshot.setTimestamp(System.currentTimeMillis());

        // Capture queue states with their products
        List<QueueSnapshot> queueSnapshots = new ArrayList<>();
        for (Queue queue : queues.values()) {
            QueueSnapshot qs = new QueueSnapshot();
            qs.setId(queue.getId());
            qs.setX(queue.getX());
            qs.setY(queue.getY());

            // Capture products in queue
            List<ProductSnapshot> productSnapshots = new ArrayList<>();
            for (Product product : queue.getProducts()) {
                ProductSnapshot ps = new ProductSnapshot();
                ps.setId(product.getId());
                ps.setColor(product.getColor());
                ps.setCreatedAt(product.getCreatedAt());
                productSnapshots.add(ps);
            }
            qs.setProductSnapshots(productSnapshots);
            queueSnapshots.add(qs);
        }
        snapshot.setQueueSnapshots(queueSnapshots);

        // Capture machine states
        List<MachineSnapshot> machineSnapshots = new ArrayList<>();
        for (Machine machine : machines.values()) {
            MachineSnapshot ms = new MachineSnapshot();
            ms.setName(machine.getName());
            ms.setMachineNumber(machine.getMachineNumber());
            ms.setX(machine.getX());
            ms.setY(machine.getY());
            ms.setStatus(machine.getStatus());
            ms.setColor(machine.getColor());
            ms.setDefaultColor(machine.getDefaultColor());
            ms.setServiceTime(machine.getServiceTime());
            ms.setReady(machine.isReady());

            // Capture queue references
            java.util.List<String> inIds = new ArrayList<>();
            for (Queue q : machine.getInputQueues()) {
                inIds.add(q.getId());
            }
            ms.setInputQueueIds(inIds);

            java.util.List<String> outIds = new ArrayList<>();
            for (Queue q : machine.getOutputQueues()) {
                outIds.add(q.getId());
            }
            ms.setOutputQueueIds(outIds);

            // Capture current product if processing
            if (machine.getCurrentProduct() != null) {
                Product p = machine.getCurrentProduct();
                ProductSnapshot ps = new ProductSnapshot(p.getId(), p.getColor(), p.getCreatedAt());
                ms.setCurrentProductSnapshot(ps);
            }

            machineSnapshots.add(ms);
        }
        snapshot.setMachineSnapshots(machineSnapshots);

        // Capture connections
        List<ConnectionSnapshot> connectionSnapshots = new ArrayList<>();
        for (Connection conn : connections) {
            ConnectionSnapshot cs = new ConnectionSnapshot(conn.getId(), conn.getFromId(), conn.getToId());
            connectionSnapshots.add(cs);
        }
        snapshot.setConnectionSnapshots(connectionSnapshots);

        // Capture counters and statistics
        snapshot.setQueueCounter(queueCounter);
        snapshot.setMachineCounter(machineCounter);
        snapshot.setTotalProductsGenerated(totalProductsGenerated);
        snapshot.setTotalProductsProcessed(totalProductsProcessed);

        // Capture simulation duration for replay
        snapshot.setSimulationDuration(simulationDuration);

        // Capture recorded products for deterministic replay
        snapshot.setGeneratedProductsRecord(new ArrayList<>(recordedProducts));
        snapshot.setRandomSeed(randomSeed);

        // Store snapshot
        caretaker.saveSnapshot(snapshot);

        return snapshot;
    }

    
    public RestoreResult restoreFromSnapshot(
            SimulationSnapshot snapshot,
            Map<String, Queue> queues,
            Map<String, Machine> machines,
            ConnectionService connectionService) {

        if (snapshot == null || !snapshot.isValid()) {
            throw new IllegalArgumentException("Invalid or null snapshot");
        }

        System.out.println("🔄 Restoring from snapshot taken at " + snapshot.getTimestamp());

        // Clear current state
        queues.clear();
        machines.clear();
        connectionService.clearConnections();

        // Restore queues with their products
        for (QueueSnapshot qs : snapshot.getQueueSnapshots()) {
            Queue queue = new Queue(qs.getId(), qs.getX(), qs.getY());

            // Restore products in queue
            for (ProductSnapshot ps : qs.getProductSnapshots()) {
                Product product = new Product();
                product.setId(ps.getId());
                product.setColor(ps.getColor());
                product.setCreatedAt(ps.getCreatedAt());
                queue.getProducts().offer(product);
            }

            queues.put(qs.getId(), queue);
        }

        // Restore machines (without queue references first)
        for (MachineSnapshot ms : snapshot.getMachineSnapshots()) {
            Machine machine = new Machine(ms.getName(), ms.getMachineNumber(), ms.getX(), ms.getY());
            machine.setStatus(ms.getStatus());
            machine.setColor(ms.getColor());
            machine.setDefaultColor(ms.getDefaultColor());
            machine.setServiceTime(ms.getServiceTime());
            machine.setReady(ms.isReady());

            // Restore current product if was processing
            if (ms.getCurrentProductSnapshot() != null) {
                ProductSnapshot ps = ms.getCurrentProductSnapshot();
                Product product = new Product();
                product.setId(ps.getId());
                product.setColor(ps.getColor());
                product.setCreatedAt(ps.getCreatedAt());
                machine.setCurrentProduct(product);
                machine.setCurrentTask(ps.getId());
            }

            machines.put(ms.getName(), machine);
        }

        // Restore connections and wire up queue-machine references
        for (ConnectionSnapshot cs : snapshot.getConnectionSnapshots()) {
            // Recreate connection
            Connection conn = new Connection(cs.getFromId(), cs.getToId());
            connectionService.addConnection(conn);

            // Wire up the objects
            char fromType = cs.getFromId().charAt(0);
            char toType = cs.getToId().charAt(0);

            if (fromType == 'Q' && toType == 'M') {
                Queue queue = queues.get(cs.getFromId());
                Machine machine = machines.get(cs.getToId());
                if (queue != null && machine != null) {
                    machine.addInputQueue(queue);
                    queue.registerObserver(machine);
                }
            } else if (fromType == 'M' && toType == 'Q') {
                Machine machine = machines.get(cs.getFromId());
                Queue queue = queues.get(cs.getToId());
                if (machine != null && queue != null) {
                    machine.addOutputQueue(queue);
                }
            }
        }

        System.out.println("✅ Snapshot restored successfully");
        System.out.println("   Queues: " + queues.size());
        System.out.println("   Machines: " + machines.size());
        System.out.println("   Connections: " + connectionService.getConnectionCount());

        return new RestoreResult(
                snapshot.getQueueCounter(),
                snapshot.getMachineCounter(),
                snapshot.getTotalProductsGenerated(),
                snapshot.getTotalProductsProcessed(),
                snapshot.getRandomSeed());
    }

    public boolean hasSnapshot() {
        return caretaker.hasSnapshots();
    }

    public SimulationSnapshot getLastSnapshot() {
        return caretaker.getLastSnapshot();
    }

    public SimulationCaretaker getCaretaker() {
        return caretaker;
    }
}
