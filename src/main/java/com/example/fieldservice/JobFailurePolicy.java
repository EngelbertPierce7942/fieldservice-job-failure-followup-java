package com.example.fieldservice;

public final class JobFailurePolicy {
    private JobFailurePolicy() { }

    public static boolean needsFollowUp(WorkOrder order, boolean scheduledRunFailed) {
        return scheduledRunFailed
                && "DISPATCHED".equals(order.dispatchStatus())
                && !order.technicianFollowUp();
    }

    public static String capturePayload(WorkOrder order, String failureMessage) {
        String safeMessage = failureMessage.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"exception\":{\"type\":\"scheduled_field_service_failure\","
                + "\"message\":\"" + safeMessage + "\","
                + "\"context\":{\"work_order_id\":\"" + order.id() + "\","
                + "\"dispatch_status\":\"" + order.dispatchStatus() + "\"}}}";
    }
}
