package org.vnsemkin;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class CrptApiTest {
    /* For running test change in CrptApi class field from
    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    to
    private static final String API_URL = "http://localhost:8080/api/v3/lk/documents/create";
     */
    @Test
    void createDocumentIntegrationTest() throws JsonProcessingException {
        //GIVEN
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        // Set up WireMock server
        WireMockServer wireMockServer = new WireMockServer();
        wireMockServer.start();
        configureFor("localhost", 8080);

        // Set up expected response from the API
        stubFor(post(urlEqualTo("/api/v3/lk/documents/create"))
                .willReturn(aResponse().withStatus(200)));
        //WHEN
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 1);

        // Set up your document
        CrptApi.Document document = crptApi.new Document();
        CrptApi.Description description = crptApi.new Description();
        description.setParticipantInn("string");
        document.setDescription(description);
        // Set other fields
        document.setDocId("string");
        document.setDocStatus("string");
        document.setDocType("LP_INTRODUCE_GOODS");
        document.setImportRequest(true);
        document.setOwnerInn("string");
        document.setParticipantInn("string");
        document.setProducerInn("string");
        document.setProductionDate(LocalDate.parse("2020-01-23"));
        document.setProductionType("string");

        // Set products
        List<CrptApi.Product> products = new ArrayList<>();
        CrptApi.Product product = crptApi.new Product();
        product.setCertificateDocument("string");
        product.setCertificateDocumentDate(LocalDate.parse("2020-01-23"));
        product.setCertificateDocumentNumber("string");
        product.setOwnerInn("string");
        product.setProducerInn("string");
        product.setProductionDate(LocalDate.parse("2020-01-23"));
        product.setTnvedCode("string");
        product.setUitCode("string");
        product.setUituCode("string");
        products.add(product);

        document.setProducts(products);

        document.setRegDate(LocalDate.parse("2020-01-23"));
        document.setRegNumber("string");
        //THEN
        for (int i = 0; i < 5; i++) {
            crptApi.createDocument(document, "sampleSignature");
        }
        // Optionally, assert that the API was called with the expected payload
        verify(postRequestedFor(urlEqualTo("/api/v3/lk/documents/create")));

        // Retrieve and print captured request bodies
        List<String> list = wireMockServer.getAllServeEvents().stream()
                .map(request -> request.getRequest().getBodyAsString()).toList();
        Assertions.assertEquals(5, list.size());
        Assertions.assertEquals(objectMapper.writeValueAsString(document), list.get(0));
        // Stop the WireMock server
        wireMockServer.stop();
    }
}
