package com.driveeasy.vehicle;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.driveeasy.vehicle.controller.VehicleController;
import com.driveeasy.vehicle.exception.ApiExceptionHandler;
import com.driveeasy.vehicle.repository.InMemoryVehicleRepository;
import com.driveeasy.vehicle.service.VehicleService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VehicleApiTest {
    private MockMvc mvc;

    @BeforeEach void setUp() {
        var service = new VehicleService(new InMemoryVehicleRepository());
        mvc = MockMvcBuilders.standaloneSetup(new VehicleController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test void createsVehicleAndReturns201WithLocation() throws Exception {
        mvc.perform(post("/vehicules").contentType("application/json").content("""
                {"immatriculation":"456TU7890","marque":"Kia","modele":"Rio","annee":2022,
                 "couleur":"Red","carburant":"ESSENCE","boite":"MANUELLE","nbPlaces":5,
                 "kilometrage":30000,"prixJournalier":88.5,"statut":"DISPONIBLE","imageUrl":null}
                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test void duplicateRegistrationIsAConflict() throws Exception {
        String body = """
                {"immatriculation":"123TU4567","marque":"Toyota","modele":"Yaris","annee":2023,
                 "couleur":"White","carburant":"HYBRIDE","boite":"AUTOMATIQUE","nbPlaces":5,
                 "kilometrage":1000,"prixJournalier":120,"statut":"DISPONIBLE"}
                """;
        mvc.perform(post("/vehicules").contentType("application/json").content(body))
                .andExpect(status().isConflict());
    }

    @Test void categoryFilterReturnsOnlyMatchingVehicles() throws Exception {
        mvc.perform(get("/vehicules").param("categorie", "SUV"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categorie").value("SUV"))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test void availabilityRejectsAnInvertedDateRange() throws Exception {
        mvc.perform(get("/vehicules/disponibles")
                        .param("dateDebut", LocalDate.now().plusDays(3).toString())
                        .param("dateFin", LocalDate.now().plusDays(2).toString()))
                .andExpect(status().isBadRequest());
    }
}
