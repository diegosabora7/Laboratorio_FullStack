package com.taskmanager.taskservice.domain.exception;

public class InvalidStatusTransitionException extends RuntimeException {

    private final String currentStatus;
    private final String targetStatus;

    public InvalidStatusTransitionException(String currentStatus, String targetStatus) {
        super(String.format("Transición de estado inválida: no se puede pasar de '%s' a '%s'",
                currentStatus, targetStatus));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public String getTargetStatus() {
        return targetStatus;
    }
}
