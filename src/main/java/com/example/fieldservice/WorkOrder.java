package com.example.fieldservice;

import java.util.List;

public record WorkOrder(String id, String dispatchStatus, List<String> photoIds, boolean technicianFollowUp) {
    public WorkOrder {
        photoIds = List.copyOf(photoIds);
    }
}
