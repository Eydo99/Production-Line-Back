package com.example.producuctionLine.model.dto;

import java.util.ArrayList;
import java.util.List;

import com.example.producuctionLine.Obserevers.MachineObserver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Machine {
    
    private  String name ;
    private  int id ;
    private  List<MachineQueue> next = new ArrayList<>() ;
    private  List<MachineObserver> observers  =new ArrayList<>();
    private int serviceTime ;
    private String color ;
    private String defaoultColor ;
    private Product currentProduct ;
    private boolean isReady=true ;
   
    // Register an observer (input queue)
    public void addObserver(MachineObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }
    
    // Remove an observer
    public void removeObserver(MachineObserver observer) {
        observers.remove(observer);
    }
    
    // Notify all observers that machine is ready
    public void notifyReady() {
        for (MachineObserver observer : observers) {
            observer.onMachineReady(this);
        }
    }

}
