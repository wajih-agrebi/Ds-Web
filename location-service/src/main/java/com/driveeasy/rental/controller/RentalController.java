package com.driveeasy.rental.controller;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.driveeasy.rental.dto.CreateRentalRequest;
import com.driveeasy.rental.dto.ReturnRentalRequest;
import com.driveeasy.rental.dto.StartRentalRequest;
import com.driveeasy.rental.dto.SimulationRequest;
import com.driveeasy.rental.model.RentalStatus;
import com.driveeasy.rental.service.RentalService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/locations")
public class RentalController {
    private final RentalService service;
    public RentalController(RentalService service) { this.service = service; }

    @GetMapping public Object list(@RequestParam(name = "page", defaultValue = "0") int page,
                                   @RequestParam(name = "size", defaultValue = "10") int size,
                                   @RequestParam(name = "statut", required = false) RentalStatus status,
                                   @RequestParam(name = "clientId", required = false) Long clientId,
                                   @RequestParam(name = "vehiculeId", required = false) Long vehicleId,
                                   @RequestParam(name = "dateDebut", required = false) LocalDate start,
                                   @RequestParam(name = "dateFin", required = false) LocalDate end) {
        return service.search(page, size, status, clientId, vehicleId, start, end);
    }
    @GetMapping("/statistiques") public Object statistics() { return service.statistics(); }
    @PostMapping("/simulation") public Object simulate(@Valid @RequestBody SimulationRequest request) {
        return service.simulate(new CreateRentalRequest(request.clientId(), request.vehiculeId(), request.dateDebut(), request.dateFinPrevue()));
    }
    @PostMapping public ResponseEntity<?> create(@Valid @RequestBody CreateRentalRequest request) {
        var saved = service.create(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(saved.id()).toUri();
        return ResponseEntity.created(location).body(saved);
    }
    @GetMapping("/{id}") public Object get(@PathVariable("id") long id) { return service.get(id); }
    @PatchMapping("/{id}/depart") public Object start(@PathVariable("id") long id, @Valid @RequestBody StartRentalRequest request) {
        return service.start(id, request.kmDepart());
    }
    @PatchMapping("/{id}/retour") public Object returnVehicle(@PathVariable("id") long id, @Valid @RequestBody ReturnRentalRequest request) {
        return service.returnVehicle(id, request.kmRetour());
    }
    @PatchMapping("/{id}/annulation") public Object cancel(@PathVariable("id") long id) { return service.cancel(id); }
}
