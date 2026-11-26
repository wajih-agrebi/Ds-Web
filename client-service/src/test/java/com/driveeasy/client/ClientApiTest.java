package com.driveeasy.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.driveeasy.client.controller.ClientController;
import com.driveeasy.client.exception.ApiExceptionHandler;
import com.driveeasy.client.repository.InMemoryClientRepository;
import com.driveeasy.client.service.ClientService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClientApiTest {
    private MockMvc mvc;

    @BeforeEach void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ClientController(new ClientService(new InMemoryClientRepository())))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test void expiredLicenceMakesClientIneligible() throws Exception {
        mvc.perform(get("/clients/3/eligibilite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.permisValide").value(false));
    }

    @Test void duplicateCinIsAConflict() throws Exception {
        mvc.perform(post("/clients").contentType("application/json").content("""
                {"cin":"09876543","nom":"Test","prenom":"User","dateNaissance":"1990-01-01",
                 "email":"different@example.test","telephone":"+216 55 000 000","adresse":"Tunis",
                 "numeroPermis":"NEW-123","dateExpirationPermis":"2030-01-01"}
                """))
                .andExpect(status().isConflict());
    }

    @Test void searchFindsClientByEmailFragment() throws Exception {
        mvc.perform(get("/clients").param("email", "amira"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("amira@example.test"));
    }
}
