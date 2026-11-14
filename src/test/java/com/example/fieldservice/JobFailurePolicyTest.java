package com.example.fieldservice;

import java.util.List;

public final class JobFailurePolicyTest {
    public static void main(String[] args) {
        WorkOrder dispatched = new WorkOrder("WO-1042", "DISPATCHED", List.of("photo-7"), false);
        WorkOrder followedUp = new WorkOrder("WO-1042", "DISPATCHED", List.of("photo-7"), true);
        if (!JobFailurePolicy.needsFollowUp(dispatched, true)) {
            throw new AssertionError("a failed run for a dispatched order needs follow-up");
        }
        if (JobFailurePolicy.needsFollowUp(followedUp, true)) {
            throw new AssertionError("an order with technician follow-up needs no duplicate action");
        }
        if (JobFailurePolicy.needsFollowUp(dispatched, false)) {
            throw new AssertionError("a successful run needs no follow-up");
        }
        System.out.println("JobFailurePolicyTest passed");
    }
}
