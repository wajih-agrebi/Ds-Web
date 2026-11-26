package com.driveeasy.rental.remote;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.driveeasy.rental.exception.ApiException;

@Component
public class RentalRemoteClients {
    private final RestClient client;

    public RentalRemoteClients(RestClient.Builder builder) { this.client = builder.build(); }

    public EligibilityRemote eligibility(long clientId) {
        return invoke(() -> client.get().uri("http://client-service/clients/{id}/eligibilite", clientId).retrieve().body(EligibilityRemote.class));
    }
    public Object clientDetails(long clientId) {
        return invoke(() -> client.get().uri("http://client-service/clients/{id}", clientId).retrieve().body(Object.class));
    }
    public VehicleRemote vehicle(long vehicleId) {
        return invoke(() -> client.get().uri("http://vehicule-service/vehicules/{id}", vehicleId).retrieve().body(VehicleRemote.class));
    }
    public List<VehicleRemote> fleet() {
        return invoke(() -> client.get().uri("http://vehicule-service/vehicules").retrieve().body(new ParameterizedTypeReference<>() { }));
    }
    public List<VehicleRemote> availableVehicles(LocalDate start, LocalDate end) {
        return invoke(() -> client.get().uri(uri -> uri.scheme("http").host("vehicule-service")
                .path("/vehicules/disponibles").queryParam("dateDebut", start).queryParam("dateFin", end).build())
                .retrieve().body(new ParameterizedTypeReference<>() { }));
    }
    public String holdVehicle(long id, LocalDate start, LocalDate end) {
        Map<?, ?> response = invoke(() -> client.post().uri("http://vehicule-service/vehicules/{id}/reservations", id)
                .body(Map.of("dateDebut", start, "dateFin", end)).retrieve().body(Map.class));
        return String.valueOf(response.get("reservationId"));
    }
    public void releaseVehicle(long id, String reservationId) {
        invoke(() -> client.delete().uri("http://vehicule-service/vehicules/{id}/reservations/{reservationId}", id, reservationId).retrieve().toBodilessEntity());
    }
    public void vehicleStatus(long id, String status) {
        invoke(() -> client.patch().uri("http://vehicule-service/vehicules/{id}/statut", id)
                .body(Map.of("statut", status)).retrieve().toBodilessEntity());
    }
    public void returnVehicle(long id, long mileage) {
        invoke(() -> client.patch().uri("http://vehicule-service/vehicules/{id}/retour", id)
                .body(Map.of("kilometrage", mileage)).retrieve().toBodilessEntity());
    }
    public void clientRentalActive(long clientId, long rentalId, boolean active) {
        invoke(() -> client.put().uri("http://client-service/clients/{id}/locations/{rentalId}", clientId, rentalId)
                .body(Map.of("active", active)).retrieve().toBodilessEntity());
    }
    public void updateCategory(long clientId, String category) {
        invoke(() -> client.patch().uri("http://client-service/clients/{id}/categorie", clientId)
                .body(Map.of("categorieClient", category)).retrieve().toBodilessEntity());
    }
    public void vehicleMileage(long id, long mileage) {
        invoke(() -> client.patch().uri("http://vehicule-service/vehicules/{id}/kilometrage", id)
                .body(Map.of("kilometrage", mileage)).retrieve().toBodilessEntity());
    }

    private <T> T invoke(java.util.function.Supplier<T> request) {
        try { return request.get(); }
        catch (ApiException ex) { throw ex; }
        catch (RestClientException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "A dependent service request failed: " + ex.getMessage());
        }
    }
}
