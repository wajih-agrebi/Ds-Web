package com.driveeasy.rental;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.driveeasy.rental.controller.RentalController;
import com.driveeasy.rental.exception.ApiExceptionHandler;
import com.driveeasy.rental.remote.EligibilityRemote;
import com.driveeasy.rental.remote.RentalRemoteClients;
import com.driveeasy.rental.remote.VehicleRemote;
import com.driveeasy.rental.repository.InMemoryRentalRepository;
import com.driveeasy.rental.service.RentalService;
import com.driveeasy.rental.model.RentalStatus;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RentalApiTest {
    private MockMvc mvc;
    private FakeRemoteClients remotes;
    private RentalService service;

    @BeforeEach void setUp() {
        remotes = new FakeRemoteClients();
        service = new RentalService(new InMemoryRentalRepository(), remotes);
        mvc = MockMvcBuilders.standaloneSetup(new RentalController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test void vipPriceSimulationAppliesTenPercentDiscount() throws Exception {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(3);
        remotes.eligibility = new EligibilityRemote(1, true, 35, true, "VIP", "ok");
        remotes.vehicle = new VehicleRemote(2, "ABC", "Toyota", "Yaris", "COMPACTE", new BigDecimal("100.00"), "DISPONIBLE", 1000);
        remotes.available = List.of(remotes.vehicle);

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/locations/simulation")
                        .contentType("application/json").content("""
                                {"clientId":1,"vehiculeId":2,"dateDebut":"%s","dateFinPrevue":"%s"}
                                """.formatted(start, end)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreJours").value(3))
                .andExpect(jsonPath("$.remiseFidelite").value(30.00))
                .andExpect(jsonPath("$.montantEstime").value(270.00));
        org.junit.jupiter.api.Assertions.assertEquals(start, remotes.lastStart);
    }

    @Test void unavailableClientIsRejectedBeforeVehicleHold() {
        remotes.eligibility = new EligibilityRemote(1, false, 19, true, "STANDARD", "Too young");
        var request = new com.driveeasy.rental.dto.CreateRentalRequest(1, 2, LocalDate.now(), LocalDate.now().plusDays(2));
        org.junit.jupiter.api.Assertions.assertThrows(com.driveeasy.rental.exception.ApiException.class, () -> service.create(request));
        org.junit.jupiter.api.Assertions.assertEquals(0, remotes.holdCalls);
    }

    @Test void unknownRentalIsReturnedAs404() throws Exception {
        mvc.perform(get("/locations/999")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test void reservationCanStartAndReturnWithFinalKilometres() {
        LocalDate today = LocalDate.now();
        remotes.eligibility = new EligibilityRemote(1, true, 30, true, "STANDARD", "ok");
        remotes.vehicle = new VehicleRemote(2, "ABC", "Toyota", "Yaris", "COMPACTE", new BigDecimal("100.00"), "DISPONIBLE", 1000);
        remotes.available = List.of(remotes.vehicle);
        var created = service.create(new com.driveeasy.rental.dto.CreateRentalRequest(1, 2, today, today));
        var started = service.start(created.id(), 1020);
        var returned = service.returnVehicle(started.id(), 1050);
        org.junit.jupiter.api.Assertions.assertEquals(RentalStatus.EN_COURS, started.statut());
        org.junit.jupiter.api.Assertions.assertEquals(RentalStatus.TERMINEE, returned.statut());
        org.junit.jupiter.api.Assertions.assertEquals(1050L, returned.kmRetour());
        org.junit.jupiter.api.Assertions.assertEquals(BigDecimal.ZERO.setScale(2), returned.penalite());
    }

    private static class FakeRemoteClients extends RentalRemoteClients {
        private EligibilityRemote eligibility = new EligibilityRemote(1, true, 30, true, "STANDARD", "ok");
        private VehicleRemote vehicle = new VehicleRemote(2, "ABC", "Toyota", "Yaris", "COMPACTE", new BigDecimal("100.00"), "DISPONIBLE", 1000);
        private List<VehicleRemote> available = List.of(vehicle);
        private LocalDate lastStart;
        private int holdCalls;

        FakeRemoteClients() { super(org.springframework.web.client.RestClient.builder()); }
        @Override public EligibilityRemote eligibility(long id) { return eligibility; }
        @Override public VehicleRemote vehicle(long id) { return vehicle; }
        @Override public List<VehicleRemote> availableVehicles(LocalDate start, LocalDate end) { lastStart = start; return available; }
        @Override public String holdVehicle(long id, LocalDate start, LocalDate end) { holdCalls++; return "hold-1"; }
        @Override public void vehicleStatus(long id, String status) { }
        @Override public void vehicleMileage(long id, long mileage) { }
        @Override public void returnVehicle(long id, long mileage) { }
        @Override public void releaseVehicle(long id, String holdId) { }
        @Override public void clientRentalActive(long clientId, long rentalId, boolean active) { }
    }
}
