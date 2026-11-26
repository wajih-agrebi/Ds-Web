package com.driveeasy.client.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.driveeasy.client.dto.CategoryRequest;
import com.driveeasy.client.dto.ClientRequest;
import com.driveeasy.client.service.ClientService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/clients")
public class ClientController {
    private final ClientService service;
    public ClientController(ClientService service) { this.service = service; }

    @GetMapping public Object search(@RequestParam(name = "nom", required = false) String nom,
                                     @RequestParam(name = "cin", required = false) String cin,
                                     @RequestParam(name = "email", required = false) String email) {
        return service.search(nom, cin, email);
    }
    @GetMapping("/{id}") public Object get(@PathVariable("id") long id) { return service.get(id); }
    @PostMapping public ResponseEntity<?> create(@Valid @RequestBody ClientRequest request) {
        var saved = service.create(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(saved.id()).toUri();
        return ResponseEntity.created(location).body(saved);
    }
    @PutMapping("/{id}") public Object update(@PathVariable("id") long id, @Valid @RequestBody ClientRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable("id") long id) { service.delete(id); return ResponseEntity.noContent().build(); }
    @GetMapping("/{id}/eligibilite") public Object eligibility(@PathVariable("id") long id) { return service.eligibility(id); }
    @PatchMapping("/{id}/categorie") public Object category(@PathVariable("id") long id, @Valid @RequestBody CategoryRequest request) {
        int completedRentals = switch (request.categorieClient()) { case VIP -> 15; case FIDELE -> 5; case STANDARD -> 0; };
        return service.updateCategory(id, completedRentals);
    }

    /** Rental Service uses this endpoint to track clients with active rentals. */
    @PutMapping("/{id}/locations/{locationId}") public ResponseEntity<Void> rentalActivity(@PathVariable("id") long id,
            @PathVariable("locationId") String locationId, @RequestBody Map<String, Boolean> body) {
        service.setRentalActive(id, locationId, Boolean.TRUE.equals(body.get("active")));
        return ResponseEntity.noContent().build();
    }
}
