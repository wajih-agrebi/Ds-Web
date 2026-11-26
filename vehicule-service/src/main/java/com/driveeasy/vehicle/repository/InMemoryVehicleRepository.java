package com.driveeasy.vehicle.repository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import com.driveeasy.vehicle.model.FuelType;
import com.driveeasy.vehicle.model.Gearbox;
import com.driveeasy.vehicle.model.Vehicle;
import com.driveeasy.vehicle.model.VehicleStatus;
import com.driveeasy.vehicle.model.VehicleCategory;

@Repository
public class InMemoryVehicleRepository implements VehicleRepository {
    private final ConcurrentHashMap<Long, Vehicle> vehicles = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();

    public InMemoryVehicleRepository() {
        save(new Vehicle(null, "123TU4567", "Toyota", "Yaris", VehicleCategory.ECONOMIQUE, 2023, "White", FuelType.HYBRIDE, Gearbox.AUTOMATIQUE, 5, 18500, new BigDecimal("120.00"), VehicleStatus.DISPONIBLE,
                "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/90/2020-2024_Toyota_Yaris.jpg/960px-2020-2024_Toyota_Yaris.jpg"));
        save(new Vehicle(null, "234TU5678", "Renault", "Clio", VehicleCategory.COMPACTE, 2022, "Blue", FuelType.ESSENCE, Gearbox.MANUELLE, 5, 26700, new BigDecimal("95.00"), VehicleStatus.DISPONIBLE,
                "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/fb/2024_Renault_Clio_1.6_E-Tech_Hybrid_espirit_Alpine_in_Iron_Blue_at_Renault_Manchester_03.jpg/960px-2024_Renault_Clio_1.6_E-Tech_Hybrid_espirit_Alpine_in_Iron_Blue_at_Renault_Manchester_03.jpg"));
        save(new Vehicle(null, "345TU6789", "Peugeot", "3008", VehicleCategory.SUV, 2024, "Black", FuelType.DIESEL, Gearbox.AUTOMATIQUE, 5, 9200, new BigDecimal("180.00"), VehicleStatus.MAINTENANCE,
                "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/12/2024_Peugeot_3008.jpg/960px-2024_Peugeot_3008.jpg"));
    }

    @Override public List<Vehicle> findAll() { return vehicles.values().stream().sorted(Comparator.comparing(Vehicle::id)).toList(); }
    @Override public Optional<Vehicle> findById(long id) { return Optional.ofNullable(vehicles.get(id)); }
    @Override public boolean existsByImmatriculation(String value) { return vehicles.values().stream().anyMatch(v -> v.immatriculation().equalsIgnoreCase(value)); }
    @Override public Vehicle save(Vehicle vehicle) {
        long id = vehicle.id() == null ? ids.incrementAndGet() : vehicle.id();
        ids.accumulateAndGet(id, Math::max);
        Vehicle stored = new Vehicle(id, vehicle.immatriculation(), vehicle.marque(), vehicle.modele(), vehicle.categorie(), vehicle.annee(), vehicle.couleur(), vehicle.carburant(), vehicle.boite(), vehicle.nbPlaces(), vehicle.kilometrage(), vehicle.prixJournalier(), vehicle.statut(), vehicle.imageUrl());
        vehicles.put(id, stored);
        return stored;
    }
    @Override public void deleteById(long id) { vehicles.remove(id); }
}
