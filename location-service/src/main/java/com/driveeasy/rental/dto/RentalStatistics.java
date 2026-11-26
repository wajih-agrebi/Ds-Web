package com.driveeasy.rental.dto;

import java.math.BigDecimal;
import java.util.List;

public record RentalStatistics(BigDecimal chiffreAffairesTotal, BigDecimal chiffreAffairesMois,
                               double tauxOccupation, List<PopularVehicle> vehiculesLesPlusLoues) {
    public record PopularVehicle(long vehiculeId, long nombreLocations) { }
}
