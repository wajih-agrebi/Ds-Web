package com.driveeasy.client.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.driveeasy.client.dto.ClientRequest;
import com.driveeasy.client.dto.EligibilityResponse;
import com.driveeasy.client.exception.ApiException;
import com.driveeasy.client.model.Client;
import com.driveeasy.client.model.CustomerCategory;
import com.driveeasy.client.repository.ClientRepository;

@Service
public class ClientService {
    private final ClientRepository repository;
    private final Map<Long, Set<String>> activeLocations = new ConcurrentHashMap<>();

    public ClientService(ClientRepository repository) { this.repository = repository; }

    public List<Client> search(String nom, String cin, String email) {
        return repository.findAll().stream()
                .filter(c -> contains(c.nom() + " " + c.prenom(), nom))
                .filter(c -> contains(c.cin(), cin))
                .filter(c -> contains(c.email(), email))
                .toList();
    }

    public Client get(long id) { return repository.findById(id).orElseThrow(() -> notFound(id)); }

    public synchronized Client create(ClientRequest request) {
        ensureUnique(request, null);
        return repository.save(toClient(null, request, CustomerCategory.STANDARD, LocalDate.now()));
    }

    public synchronized Client update(long id, ClientRequest request) {
        Client old = get(id);
        ensureUnique(request, id);
        return repository.save(toClient(id, request, old.categorieClient(), old.dateInscription()));
    }

    public synchronized void delete(long id) {
        get(id);
        if (!activeLocations.getOrDefault(id, Set.of()).isEmpty())
            throw new ApiException(HttpStatus.CONFLICT, "A client with an active rental cannot be deleted");
        repository.deleteById(id);
    }

    public EligibilityResponse eligibility(long id) {
        Client client = get(id);
        int age = Period.between(client.dateNaissance(), LocalDate.now()).getYears();
        boolean licenceValid = !client.dateExpirationPermis().isBefore(LocalDate.now());
        boolean eligible = age >= 21 && licenceValid;
        String message = eligible ? "Client may rent a vehicle" : age < 21 ? "Client must be at least 21 years old" : "Driving licence has expired";
        return new EligibilityResponse(id, eligible, age, licenceValid, client.categorieClient(), message);
    }

    public synchronized Client updateCategory(long id, int completedRentals) {
        if (completedRentals < 0) throw new ApiException(HttpStatus.BAD_REQUEST, "Completed rental count cannot be negative");
        Client client = get(id);
        CustomerCategory earned = completedRentals >= 15 ? CustomerCategory.VIP
                : completedRentals >= 5 ? CustomerCategory.FIDELE : CustomerCategory.STANDARD;
        // Keep an existing tier if the supplied completed-rental count is stale.
        CustomerCategory category = rank(earned) > rank(client.categorieClient()) ? earned : client.categorieClient();
        return repository.save(new Client(client.id(), client.cin(), client.nom(), client.prenom(), client.dateNaissance(), client.email(), client.telephone(), client.adresse(), client.numeroPermis(), client.dateExpirationPermis(), category, client.dateInscription()));
    }

    public synchronized void setRentalActive(long id, String rentalId, boolean active) {
        get(id);
        if (active) activeLocations.computeIfAbsent(id, ignored -> ConcurrentHashMap.newKeySet()).add(rentalId);
        else {
            Set<String> rentals = activeLocations.get(id);
            if (rentals != null) {
                rentals.remove(rentalId);
                if (rentals.isEmpty()) activeLocations.remove(id);
            }
        }
    }

    private int rank(CustomerCategory category) { return switch (category) { case STANDARD -> 0; case FIDELE -> 1; case VIP -> 2; }; }
    private boolean contains(String value, String query) { return query == null || value.toLowerCase().contains(query.trim().toLowerCase()); }

    private void ensureUnique(ClientRequest request, Long thisId) {
        boolean duplicate = repository.findAll().stream().anyMatch(c -> !c.id().equals(thisId)
                && (c.cin().equalsIgnoreCase(request.cin().trim())
                || c.email().equalsIgnoreCase(request.email().trim())
                || c.numeroPermis().equalsIgnoreCase(request.numeroPermis().trim())));
        if (duplicate) throw new ApiException(HttpStatus.CONFLICT, "CIN, email, and driving licence number must be unique");
    }
    private Client toClient(Long id, ClientRequest r, CustomerCategory category, LocalDate registered) {
        return new Client(id, r.cin().trim(), r.nom().trim(), r.prenom().trim(), r.dateNaissance(), r.email().trim().toLowerCase(), r.telephone().trim(), r.adresse().trim(), r.numeroPermis().trim(), r.dateExpirationPermis(), category, registered);
    }
    private ApiException notFound(long id) { return new ApiException(HttpStatus.NOT_FOUND, "Client " + id + " was not found"); }
}
