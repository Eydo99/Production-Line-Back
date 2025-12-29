package com.example.producuctionLine.runner;

import com.example.producuctionLine.dto.MachineUpdateDTO;
import com.example.producuctionLine.dto.QueueUpdateDTO;
import com.example.producuctionLine.model.Machine;
import com.example.producuctionLine.model.Product;
import com.example.producuctionLine.model.Queue;
import com.example.producuctionLine.service.StatisticsService;
import com.example.producuctionLine.service.WebSocketBroadcaster;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;

/**
 * Runnable class to handle machine processing thread.
 * Extracted from SimulationManager for better modularity.
 * 
 * @author Refactored
 */
public class MachineRunner implements Runnable {

    private final Machine machine;
    private final BooleanSupplier isRunningSupplier;
    private final BooleanSupplier isPausedSupplier;
    private final Random random;
    private final WebSocketBroadcaster broadcaster;
    private final StatisticsService statisticsService;
    private final Runnable broadcastStatisticsCallback;
    private final Object pauseLock;

    public MachineRunner(
            Machine machine,
            BooleanSupplier isRunningSupplier,
            BooleanSupplier isPausedSupplier,
            Random random,
            WebSocketBroadcaster broadcaster,
            StatisticsService statisticsService,
            Runnable broadcastStatisticsCallback,
            Object pauseLock) {
        this.machine = machine;
        this.isRunningSupplier = isRunningSupplier;
        this.isPausedSupplier = isPausedSupplier;
        this.random = random;
        this.broadcaster = broadcaster;
        this.statisticsService = statisticsService;
        this.broadcastStatisticsCallback = broadcastStatisticsCallback;
        this.pauseLock = pauseLock;
    }

    private boolean isRunning() {
        return isRunningSupplier.getAsBoolean();
    }

    private boolean isPaused() {
        return isPausedSupplier.getAsBoolean();
    }

    @Override
    public void run() {
        System.out.println("🏁 " + machine.getName() + " thread started");

        while (isRunning() && !Thread.currentThread().isInterrupted()) {
            try {
                // Wait while paused
                while (isPaused()) {
                    if (!isRunning())
                        break;
                    Thread.sleep(50);
                }

                // Check if stopped while paused
                if (!isRunning())
                    break;

                // If machine is ready and has input queues, check for products
                if (machine.isReady() && !machine.getInputQueues().isEmpty()) {
                    List<Queue> inputQueues = machine.getInputQueues();

                    // Filter non-empty queues
                    List<Queue> nonEmptyQueues = new ArrayList<>();
                    for (Queue q : inputQueues) {
                        if (!q.isEmpty()) {
                            nonEmptyQueues.add(q);
                        }
                    }

                    if (!nonEmptyQueues.isEmpty()) {
                        // 🚫 DO NOT take product if paused or stopped - with synchronized check
                        synchronized (pauseLock) {
                            if (isPaused() || !isRunning()) {
                                Thread.sleep(50);
                                continue;
                            }
                        }

                        // Double-check to prevent race condition
                        if (isPaused() || !isRunning()) {
                            Thread.sleep(50);
                            continue;
                        }

                        // Randomly select one non-empty queue
                        Queue selectedQueue = nonEmptyQueues.get(random.nextInt(nonEmptyQueues.size()));
                        Product product = selectedQueue.dequeue();

                        if (product != null) {
                            processProduct(product);

                            // ⛔ stop immediately if simulation ended mid-processing
                            if (!isRunning() || Thread.currentThread().isInterrupted()) {
                                break;
                            }

                            // BROADCAST QUEUE UPDATE (Dequeued)
                            if (broadcaster != null) {
                                broadcaster.broadcastQueueUpdate(new QueueUpdateDTO(
                                        selectedQueue.getId(),
                                        selectedQueue.getProducts().size()));
                            }
                        }
                    } else {
                        // Register observer to all queues
                        for (Queue q : inputQueues) {
                            q.registerObserver(machine);
                        }
                    }
                }

                Thread.sleep(50);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("⏹️  " + machine.getName() + " thread interrupted");
                break;
            } catch (Exception e) {
                System.err.println("❌ Error in " + machine.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("🛑 " + machine.getName() + " thread stopped");
    }

    private void processProduct(Product product) {
        // ⏸️ IMMEDIATE PAUSE CHECK
        if (isPaused()) {
            if (!machine.getInputQueues().isEmpty() && product != null) {
                machine.getInputQueues().get(0).enqueue(product); // Put back
                System.out.println("⏸️  " + machine.getName() + " returned product to queue (paused)");
            }
            resetMachine();
            return;
        }

        if (!isRunning()) {
            if (!machine.getInputQueues().isEmpty() && product != null) {
                machine.getInputQueues().get(0).enqueue(product);
            }
            resetMachine();
            return;
        }

        try {
            machine.setReady(false);
            machine.setStatus("processing");
            machine.setCurrentProduct(product);
            machine.setCurrentTask(product.getId());
            machine.setColor(product.getColor());

            System.out.println("⚙️  " + machine.getName() + " started processing " +
                    product.getId() + " (color: " + product.getColor() + ")");

            // 🆕 BROADCAST - Processing Started
            if (broadcaster != null) {
                MachineUpdateDTO dto = new MachineUpdateDTO(
                        machine.getName(),
                        "processing",
                        product.getColor());
                broadcaster.broadcastMachineUpdate(dto);
            }

            // Sleep in small chunks
            int serviceTime = machine.getServiceTime();
            int elapsed = 0;
            int sleepChunk = 100;

            while (elapsed < serviceTime) {
                if (!isRunning()) {
                    resetMachine();
                    if (broadcaster != null) {
                        broadcaster.broadcastMachineUpdate(new MachineUpdateDTO(
                                machine.getName(),
                                "idle",
                                machine.getDefaultColor()));
                    }
                    return;
                }

                while (isPaused()) {
                    Thread.sleep(10);
                    if (!isRunning()) {
                        if (!machine.getInputQueues().isEmpty() && product != null) {
                            machine.getInputQueues().get(0).enqueue(product);
                        }
                        resetMachine();
                        if (broadcaster != null) {
                            broadcaster.broadcastMachineUpdate(new MachineUpdateDTO(
                                    machine.getName(),
                                    "idle",
                                    machine.getDefaultColor()));
                        }
                        return;
                    }
                }

                int remainingTime = serviceTime - elapsed;
                int timeToSleep = Math.min(sleepChunk, remainingTime);
                Thread.sleep(timeToSleep);
                elapsed += timeToSleep;
            }

            if (!isRunning()) {
                if (!machine.getInputQueues().isEmpty() && product != null) {
                    machine.getInputQueues().get(0).enqueue(product);
                }
                resetMachine();
                if (broadcaster != null) {
                    broadcaster.broadcastMachineUpdate(new MachineUpdateDTO(
                            machine.getName(),
                            "idle",
                            machine.getDefaultColor()));
                }
                return;
            }

            // Flash effect
            System.out.println("✨ " + machine.getName() + " finished processing " + product.getId());

            if (broadcaster != null) {
                broadcaster.broadcastMachineUpdate(new MachineUpdateDTO(
                        machine.getName(),
                        "FLASHING",
                        product.getColor()));
            }

            Thread.sleep(200); // Flash duration

            synchronized (pauseLock) {
                if (!isRunning() || isPaused()) {
                    if (!machine.getInputQueues().isEmpty()) {
                        machine.getInputQueues().get(0).enqueue(product);
                    }
                    resetMachine();
                    if (broadcaster != null) {
                        broadcaster.broadcastMachineUpdate(new MachineUpdateDTO(
                                machine.getName(),
                                "idle",
                                machine.getDefaultColor()));
                    }
                    return;
                }
            }

            // Move to output queue
            List<Queue> outputQueues = machine.getOutputQueues();
            if (!outputQueues.isEmpty()) {
                int index = Math.abs(product.getId().hashCode()) % outputQueues.size();
                Queue selectedOutput = outputQueues.get(index);
                selectedOutput.enqueue(product);

                statisticsService.incrementProductsProcessed();
                System.out.println("📤 " + machine.getName() + " sent product to " + selectedOutput.getId());

                if (broadcaster != null) {
                    broadcaster.broadcastQueueUpdate(new QueueUpdateDTO(
                            selectedOutput.getId(),
                            selectedOutput.getProducts().size()));
                }
            } else {
                System.out.println("⚠️  " + machine.getName() + " has no output queue - product completed");
                statisticsService.incrementProductsProcessed();
            }

            resetMachine();

            if (broadcaster != null) {
                broadcaster.broadcastMachineUpdate(new MachineUpdateDTO(
                        machine.getName(),
                        "idle",
                        machine.getDefaultColor()));
            }

            broadcastStatisticsCallback.run();

        } catch (InterruptedException e) {
            if (!machine.getInputQueues().isEmpty() && product != null) {
                machine.getInputQueues().get(0).enqueue(product);
            }
            resetMachine();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Helper method to reset machine to idle state
     */
    private void resetMachine() {
        machine.setCurrentProduct(null);
        machine.setCurrentTask(null);
        machine.setColor(machine.getDefaultColor());
        machine.setStatus("idle");
        machine.setReady(true);
    }
}
