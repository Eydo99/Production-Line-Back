package com.example.producuctionLine.runner;

import com.example.producuctionLine.model.Product;
import com.example.producuctionLine.model.Queue;
import com.example.producuctionLine.model.snapshot.ProductSnapshot;
import com.example.producuctionLine.service.StatisticsService;
import com.example.producuctionLine.service.WebSocketBroadcaster;

import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Runnable class to handle product generation thread.
 * Supports both random generation and replay mode.
 * Extracted from SimulationManager for better modularity.
 * 
 * @author Refactored
 */
public class ProductGenerator implements Runnable {

    private static final int MIN_PRODUCT_DELAY = 1000;
    private static final int MAX_PRODUCT_DELAY = 3000;

    private final BooleanSupplier isRunningSupplier;
    private final BooleanSupplier isPausedSupplier;
    private final BooleanSupplier isReplayModeSupplier;
    private final Supplier<Queue> firstQueueSupplier;
    private final Supplier<List<ProductSnapshot>> productsToReplaySupplier;
    private final Supplier<Integer> replayIndexSupplier;
    private final Runnable incrementReplayIndex;
    private final List<ProductSnapshot> recordedProducts;
    private final StatisticsService statisticsService;
    private final WebSocketBroadcaster broadcaster;
    private final Runnable broadcastStatisticsCallback;

    public ProductGenerator(
            BooleanSupplier isRunningSupplier,
            BooleanSupplier isPausedSupplier,
            BooleanSupplier isReplayModeSupplier,
            Supplier<Queue> firstQueueSupplier,
            Supplier<List<ProductSnapshot>> productsToReplaySupplier,
            Supplier<Integer> replayIndexSupplier,
            Runnable incrementReplayIndex,
            List<ProductSnapshot> recordedProducts,
            StatisticsService statisticsService,
            WebSocketBroadcaster broadcaster,
            Runnable broadcastStatisticsCallback) {
        this.isRunningSupplier = isRunningSupplier;
        this.isPausedSupplier = isPausedSupplier;
        this.isReplayModeSupplier = isReplayModeSupplier;
        this.firstQueueSupplier = firstQueueSupplier;
        this.productsToReplaySupplier = productsToReplaySupplier;
        this.replayIndexSupplier = replayIndexSupplier;
        this.incrementReplayIndex = incrementReplayIndex;
        this.recordedProducts = recordedProducts;
        this.statisticsService = statisticsService;
        this.broadcaster = broadcaster;
        this.broadcastStatisticsCallback = broadcastStatisticsCallback;
    }

    private boolean isRunning() {
        return isRunningSupplier.getAsBoolean();
    }

    private boolean isPaused() {
        return isPausedSupplier.getAsBoolean();
    }

    private boolean isReplayMode() {
        return isReplayModeSupplier.getAsBoolean();
    }

    @Override
    public void run() {
        Random random = new Random();
        System.out.println("🏭 Product generator started (replay mode: " + isReplayMode() + ")");

        while (isRunning() && !Thread.currentThread().isInterrupted()) {
            try {
                // Wait while paused
                while (isPaused() && isRunning()) {
                    Thread.sleep(100);
                }

                // Check if stopped while paused
                if (!isRunning())
                    break;

                Queue firstQueue = firstQueueSupplier.get();
                if (firstQueue == null) {
                    Thread.sleep(100);
                    continue;
                }

                if (isReplayMode()) {
                    // REPLAY MODE: Use recorded products
                    List<ProductSnapshot> productsToReplay = productsToReplaySupplier.get();
                    int replayIndex = replayIndexSupplier.get();

                    if (replayIndex < productsToReplay.size()) {
                        ProductSnapshot ps = productsToReplay.get(replayIndex);

                        // Calculate delay based on createdAt timestamp (relative timing)
                        long delay;
                        if (replayIndex == 0) {
                            delay = ps.getCreatedAt(); // First product's delay is its createdAt value
                        } else {
                            // Delay is the difference from previous product
                            delay = ps.getCreatedAt() - productsToReplay.get(replayIndex - 1).getCreatedAt();
                        }

                        if (delay > 0) {
                            Thread.sleep(delay);
                        }

                        if (!isRunning())
                            break;

                        // Create product with recorded values
                        Product product = new Product();
                        product.setId(ps.getId());
                        product.setColor(ps.getColor());

                        statisticsService.incrementProductsGenerated();
                        incrementReplayIndex.run();

                        firstQueue.enqueue(product);

                        // ✅ FIXED: BROADCAST QUEUE UPDATE - Pass Queue object with products
                        if (broadcaster != null) {
                            broadcaster.broadcastQueueUpdate(firstQueue);
                        }

                        System.out.println("🔁 Replayed product #" + statisticsService.getTotalProductsGenerated() +
                                ": " + product.getId() +
                                " (color: " + product.getColor() + ") → " + firstQueue.getId());

                        broadcastStatisticsCallback.run();
                    } else {
                        // All products replayed, wait for simulation to end
                        Thread.sleep(100);
                    }
                } else {
                    // NORMAL MODE: Generate new random products and record them
                    int delay = MIN_PRODUCT_DELAY + random.nextInt(MAX_PRODUCT_DELAY - MIN_PRODUCT_DELAY);
                    Thread.sleep(delay);

                    if (!isRunning())
                        break;

                    Product product = new Product();
                    statisticsService.incrementProductsGenerated();

                    // Record product for future replay (store relative time from start)
                    long relativeTime = statisticsService.getSimulationDuration(false);
                    recordedProducts.add(new ProductSnapshot(product.getId(), product.getColor(), relativeTime));

                    firstQueue.enqueue(product);

                    System.out.println("🆕 Generated product #" + statisticsService.getTotalProductsGenerated() +
                            ": " + product.getId() +
                            " (color: " + product.getColor() + ") → " + firstQueue.getId());

                    // ✅ FIXED: BROADCAST QUEUE UPDATE - Pass Queue object with products
                    if (broadcaster != null) {
                        broadcaster.broadcastQueueUpdate(firstQueue);
                    }

                    broadcastStatisticsCallback.run();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("⏹️  Product generator stopped");
                break;
            } catch (Exception e) {
                System.err.println("❌ Error in product generator: " + e.getMessage());
            }
        }

        System.out.println("🛑 Product generator stopped");
    }
}