package com.example.fieldservice;

import java.net.http.HttpClient;
import java.util.List;

public final class FieldServiceFailureDemo {
    private FieldServiceFailureDemo() { }

    public static void main(String[] args) throws Exception {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Set INFRAI_API_KEY before running the demo");
        }
        WorkOrder order = new WorkOrder("WO-1042", "DISPATCHED", List.of("photo-7"), false);
        boolean scheduledRunFailed = true;
        boolean followUpRequired = JobFailurePolicy.needsFollowUp(order, scheduledRunFailed);
        if (followUpRequired) {
            InfraiErrorsClient client = new InfraiErrorsClient(HttpClient.newHttpClient(), key);
            String envelope = client.capture(JobFailurePolicy.capturePayload(order, "technician status sync failed"));
            System.out.println("failure captured for " + order.id() + ": " + envelope);
        }
    }
}
