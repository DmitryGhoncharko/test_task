package org.example;

import java.util.concurrent.*;

public class CrptApi {
    private final int requestLimit;
    private final TimeUnit timeUnit;
    private final ScheduledExecutorService scheduler;
    private final BlockingQueue<Runnable> taskQueue;
    private final Semaphore tokenBucket;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestLimit = requestLimit;
        this.timeUnit = timeUnit;
        this.tokenBucket = new Semaphore(requestLimit);
        this.taskQueue = new LinkedBlockingQueue<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::refillTokens, 0, 1, timeUnit);
    }

    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 10);
        String documentJson = "{\"description\": {\"participantInn\": \"1234567890\"}, \"doc_id\": \"123\", \"doc_status\": \"new\", \"doc_type\": \"LP_INTRODUCE_GOODS\", \"importRequest\": true, \"owner_inn\": \"9876543210\", \"participant_inn\": \"1234567890\", \"producer_inn\": \"1234567890\", \"production_date\": \"2020-01-23\", \"production_type\": \"raw\", \"products\": [{\"certificate_document\": \"123\", \"certificate_document_date\": \"2020-01-23\", \"certificate_document_number\": \"456\", \"owner_inn\": \"9876543210\", \"producer_inn\": \"1234567890\", \"production_date\": \"2020-01-23\", \"tnved_code\": \"789\", \"uit_code\": \"987\", \"uitu_code\": \"654\"}], \"reg_date\": \"2020-01-23\", \"reg_number\": \"789\"}";
        String signature = "dummy_signature";
        crptApi.createDocument(documentJson, signature);
    }

    private void refillTokens() {
        tokenBucket.release(requestLimit - tokenBucket.availablePermits());
    }

    public void createDocument(String documentJson, String signature) {
        try {
            tokenBucket.acquire();
            submitTask(() -> {
                System.out.println("Making POST request to API with document:");
                System.out.println(documentJson);
                System.out.println("Signature: " + signature);
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void submitTask(Runnable task) {
        try {
            taskQueue.put(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
