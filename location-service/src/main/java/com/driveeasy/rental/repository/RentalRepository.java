package com.driveeasy.rental.repository;

import java.util.List;
import java.util.Optional;

import com.driveeasy.rental.model.Rental;

public interface RentalRepository {
    List<Rental> findAll();
    Optional<Rental> findById(long id);
    Rental save(Rental rental);
}
