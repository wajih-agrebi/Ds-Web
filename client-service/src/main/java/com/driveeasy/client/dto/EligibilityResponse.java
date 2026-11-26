package com.driveeasy.client.dto;

import com.driveeasy.client.model.CustomerCategory;

public record EligibilityResponse(long clientId, boolean eligible, int age, boolean permisValide,
                                  CustomerCategory categorieClient, String message) { }
