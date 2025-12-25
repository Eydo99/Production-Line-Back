package com.example.producuctionLine.service;

import java.util.Random;

import org.springframework.stereotype.Service;

import com.example.producuctionLine.model.dto.Machine;
import com.example.producuctionLine.model.dto.Product;

@Service
class MachineService {
    private final Random random =new Random() ;
    public void processProduct(Machine machine, Product product) throws InterruptedException {
        machine.setReady(false);;
        machine.setColor(product.getColor());
        machine.setCurrentProduct(product);
        // Thread.sleep(machine.getServiceTime());
    }

    public void finishProcessing(Machine machine,Product product) {
        machine.getNext().get(selectQueue(machine)).add(product); 
        machine.setColor(machine.getDefaoultColor());
        machine.setCurrentProduct(null);
        machine.setReady(true);
        machine.notifyReady();
    }

    public int serviceTime() {
        return random.nextInt(5000) + 1000;
    }

    public int selectQueue(Machine machine) {
        if (machine.getNext() == null || machine.getNext().isEmpty()) {
            throw new IllegalStateException("Machine " + machine.getName() + " has no output queues");
        }
        return random.nextInt(machine.getNext().size());
    }

}
