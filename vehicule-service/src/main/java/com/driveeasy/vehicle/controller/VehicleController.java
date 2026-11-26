package com.driveeasy.vehicle.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.driveeasy.vehicle.dto.StatusRequest;
import com.driveeasy.vehicle.dto.VehicleRequest;
import com.driveeasy.vehicle.dto.VehicleReservationRequest;
import com.driveeasy.vehicle.dto.VehicleReturnRequest;
import com.driveeasy.vehicle.dto.MileageRequest;
import com.driveeasy.vehicle.service.VehicleService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/vehicules")
public class VehicleController {
    private final VehicleService service;
    public VehicleController(VehicleService service) { this.service = service; }

    @GetMapping public Object list(@RequestParam(name = "categorie", required = false) String categorie,
                                   @RequestParam(name = "carburant", required = false) String carburant,
                                   @RequestParam(name = "statut", required = false) String statut,
                                   @RequestParam(name = "prixMax", required = false) BigDecimal prixMax,
                                   @RequestParam(name = "dateDebut", required = false) LocalDate dateDebut,
                                   @RequestParam(name = "dateFin", required = false) LocalDate dateFin) {
        return service.find(categorie, carburant, statut, prixMax, dateDebut, dateFin);
    }

    @GetMapping("/disponibles") public Object available(@RequestParam(name = "dateDebut") LocalDate dateDebut, @RequestParam(name = "dateFin") LocalDate dateFin) {
        return service.find(null, null, "DISPONIBLE", null, dateDebut, dateFin);
    }

    @GetMapping("/{id}") public Object get(@PathVariable("id") long id) { return service.get(id); }

    @PostMapping public ResponseEntity<?> create(@Valid @RequestBody VehicleRequest request) {
        var saved = service.create(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(saved.id()).toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}") public Object update(@PathVariable("id") long id, @Valid @RequestBody VehicleRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable("id") long id) { service.delete(id); return ResponseEntity.noContent().build(); }
    @PatchMapping("/{id}/statut") public Object status(@PathVariable("id") long id, @Valid @RequestBody StatusRequest request) { return service.updateStatus(id, request.statut()); }
    @PatchMapping("/{id}/retour") public Object returnVehicle(@PathVariable("id") long id, @Valid @RequestBody VehicleReturnRequest request) { return service.returnVehicle(id, request.kilometrage()); }
    @PatchMapping("/{id}/kilometrage") public Object mileage(@PathVariable("id") long id, @Valid @RequestBody MileageRequest request) { return service.updateMileage(id, request.kilometrage()); }

    @PostMapping("/{id}/reservations") public Object hold(@PathVariable("id") long id, @Valid @RequestBody VehicleReservationRequest request) {
        return Map.of("reservationId", service.hold(id, request.dateDebut(), request.dateFin()));
    }

    @DeleteMapping("/{id}/reservations/{reservationId}") public ResponseEntity<Void> release(@PathVariable("id") long id, @PathVariable("reservationId") String reservationId) {
        service.release(id, reservationId);
        return ResponseEntity.noContent().build();
    }
}
