package com.example.producuctionLine.service;

import com.example.producuctionLine.model.Queue;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StatisticsService {

    // ========== STATISTICS ==========
    @Getter
    private int totalProductsGenerated = 0;
    @Getter
    private int totalProductsProcessed = 0;

    // ========== TIMING ==========
    private long simulationStartTime = 0;
    private long totalPausedTime = 0;
    private long pauseStartTime = 0;

    public void startSimulation() {
        totalProductsGenerated = 0;
        totalProductsProcessed = 0;
        simulationStartTime = System.currentTimeMillis();
        totalPausedTime = 0;
        pauseStartTime = 0;
    }

    public void pauseSimulation() {
        pauseStartTime = System.currentTimeMillis();
    }

    public void resumeSimulation() {
        if (pauseStartTime > 0) {
            totalPausedTime += (System.currentTimeMillis() - pauseStartTime);
            pauseStartTime = 0;
        }
    }

    public void clearStatistics() {
        totalProductsGenerated = 0;
        totalProductsProcessed = 0;
        simulationStartTime = 0;
        totalPausedTime = 0;
        pauseStartTime = 0;
    }

    public synchronized void incrementProductsGenerated() {
        totalProductsGenerated++;
    }

    public synchronized void incrementProductsProcessed() {
        totalProductsProcessed++;
    }

    // Setters for restoration
    public void setStates(int totalGenerated, int totalProcessed) {
        this.totalProductsGenerated = totalGenerated;
        this.totalProductsProcessed = totalProcessed;
    }

    public long getSimulationDuration(boolean isPaused) {
        if (simulationStartTime == 0)
            return 0;

        long currentTime = System.currentTimeMillis();
        long totalElapsed = currentTime - simulationStartTime;

        long currentPauseDuration = 0;
        if (isPaused && pauseStartTime > 0) {
            currentPauseDuration = currentTime - pauseStartTime;
        }

        return totalElapsed - totalPausedTime - currentPauseDuration;
    }

    public double getAverageQueueLength(Map<String, Queue> queues) {
        if (queues.isEmpty())
            return 0;
        return queues.values().stream()
                .mapToInt(Queue::size)
                .average()
                .orElse(0.0);
    }
}
