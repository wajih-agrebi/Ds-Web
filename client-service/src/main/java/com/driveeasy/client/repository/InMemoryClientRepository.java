package com.driveeasy.client.repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import com.driveeasy.client.model.Client;
import com.driveeasy.client.model.CustomerCategory;

@Repository
public class InMemoryClientRepository implements ClientRepository {
    private final ConcurrentHashMap<Long, Client> clients = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();

    public InMemoryClientRepository() {
        save(new Client(null, "09876543", "Ben Salem", "Amira", LocalDate.of(1998, 3, 12), "amira@example.test", "+216 20 100 100", "Tunis", "P-123456", LocalDate.now().plusYears(3), CustomerCategory.STANDARD, LocalDate.now()));
        save(new Client(null, "11223344", "Trabelsi", "Youssef", LocalDate.of(1991, 8, 25), "youssef@example.test", "+216 21 200 200", "Sousse", "P-234567", LocalDate.now().plusYears(1), CustomerCategory.FIDELE, LocalDate.now().minusYears(2)));
        save(new Client(null, "22334455", "Khalfallah", "Ines", LocalDate.of(1987, 1, 5), "ines@example.test", "+216 22 300 300", "Sfax", "P-345678", LocalDate.now().minusDays(10), CustomerCategory.VIP, LocalDate.now().minusYears(4)));
    }

    @Override public List<Client> findAll() { return clients.values().stream().sorted(Comparator.comparing(Client::id)).toList(); }
    @Override public Optional<Client> findById(long id) { return Optional.ofNullable(clients.get(id)); }
    @Override public Client save(Client client) {
        long id = client.id() == null ? ids.incrementAndGet() : client.id();
        ids.accumulateAndGet(id, Math::max);
        Client stored = new Client(id, client.cin(), client.nom(), client.prenom(), client.dateNaissance(), client.email(), client.telephone(), client.adresse(), client.numeroPermis(), client.dateExpirationPermis(), client.categorieClient(), client.dateInscription());
        clients.put(id, stored);
        return stored;
    }
    @Override public void deleteById(long id) { clients.remove(id); }
}
