package org.vnsemkin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

@Slf4j
public class CrptApi {
    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json");
    private final Semaphore semaphore;
    private final OkHttpClient httpClient;
    private final ScheduledExecutorService timerUpdater;
    private final TimeUnit timeUnit;
    private final int requestLimit;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.semaphore = new Semaphore(requestLimit, true);
        this.httpClient = new OkHttpClient();
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.timerUpdater = Executors.newSingleThreadScheduledExecutor();
        semaphoreLimitUpdater();
    }

    public void createDocument(Document document, String signature) {
        try {
            semaphore.acquire();
            sendPostRequest(document, signature);
        } catch (InterruptedException e) {
            semaphore.release();
            e.printStackTrace();
        }
    }

    private void sendPostRequest(Document document, String signature) {
        String body = null;
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        if (Objects.nonNull(document)) {
            try {
                body = objectMapper.writeValueAsString(document);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            body = "{" + "description " + ":" + "Something wrong with document" + "}";
        }
        RequestBody requestBody = RequestBody.create(body, JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .addHeader("Signature", signature)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                log.info("Document created");
            } else {
                log.info("Error " + response.code()
                        + " - "
                        + response.message());
            }
        } catch (IOException e) {
            semaphore.release();
            e.printStackTrace();
        }
    }

    private void semaphoreLimitUpdater() {
        timerUpdater.scheduleWithFixedDelay(this::releaseSemaphorePermission
                , 0, 1, timeUnit);
    }

    private void releaseSemaphorePermission() {
        int release = requestLimit - semaphore.availablePermits();
        semaphore.release(release);
    }

    public void shutdownSemaphoreLimitUpdater() {
        timerUpdater.shutdown();
    }

    @Getter
    @Setter
    public class Document {
        private Description description;
        @JsonProperty("doc_id")
        private String docId;
        @JsonProperty("doc_status")
        private String docStatus;
        @JsonProperty("doc_type")
        private String docType;
        private boolean importRequest;
        @JsonProperty("owner_inn")
        private String ownerInn;
        @JsonProperty("participant_inn")
        private String participantInn;
        @JsonProperty("producer_inn")
        private String producerInn;
        @JsonProperty("production_date")
        private LocalDate productionDate;
        @JsonProperty("production_type")
        private String productionType;
        private List<Product> products;
        @JsonProperty("reg_date")
        private LocalDate regDate;
        @JsonProperty("reg_number")
        private String regNumber;
    }

    @Getter
    @Setter
    public class Description {
        private String participantInn;

    }

    @Getter
    @Setter
    public class Product {
        @JsonProperty("certificate_document")
        private String certificateDocument;
        @JsonProperty("certificate_document_date")
        private LocalDate certificateDocumentDate;
        @JsonProperty("certificate_document_number")
        private String certificateDocumentNumber;
        @JsonProperty("owner_inn")
        private String ownerInn;
        @JsonProperty("producer_inn")
        private String producerInn;
        @JsonProperty("production_date")
        private LocalDate productionDate;
        @JsonProperty("tnved_code")
        private String tnvedCode;
        @JsonProperty("uit_code")
        private String uitCode;
        @JsonProperty("uitu_code")
        private String uituCode;
    }
}
