package com.driveeasy.client.dto;

import com.driveeasy.client.model.CustomerCategory;
import jakarta.validation.constraints.NotNull;

public record CategoryRequest(@NotNull CustomerCategory categorieClient) { }
