package com.driveeasy.client.repository;

import java.util.List;
import java.util.Optional;

import com.driveeasy.client.model.Client;

public interface ClientRepository {
    List<Client> findAll();
    Optional<Client> findById(long id);
    Client save(Client client);
    void deleteById(long id);
}
