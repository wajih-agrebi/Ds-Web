package com.driveeasy.vehicle.repository;

import java.util.List;
import java.util.Optional;

import com.driveeasy.vehicle.model.Vehicle;

public interface VehicleRepository {
    List<Vehicle> findAll();
    Optional<Vehicle> findById(long id);
    boolean existsByImmatriculation(String immatriculation);
    Vehicle save(Vehicle vehicle);
    void deleteById(long id);
}
