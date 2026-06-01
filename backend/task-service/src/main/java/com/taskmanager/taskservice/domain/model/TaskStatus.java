package com.taskmanager.taskservice.domain.model;

import java.util.EnumSet;
import java.util.Set;

// CÓDIGO ANTERIOR (antes de Feature 10):
// public enum TaskStatus {
//     TODO,
//     IN_PROGRESS,
//     IN_REVIEW,
//     DONE
// }

// FEATURE 10: Se agregó getAllowedTransitions() y canTransitionTo() para validar
// que las transiciones de estado sigan el flujo definido del Kanban.
public enum TaskStatus {
    TODO {
        @Override
        public Set<TaskStatus> getAllowedTransitions() {
            return EnumSet.of(IN_PROGRESS);
        }
    },
    IN_PROGRESS {
        @Override
        public Set<TaskStatus> getAllowedTransitions() {
            return EnumSet.of(IN_REVIEW, TODO);
        }
    },
    IN_REVIEW {
        @Override
        public Set<TaskStatus> getAllowedTransitions() {
            return EnumSet.of(DONE, IN_PROGRESS);
        }
    },
    DONE {
        @Override
        public Set<TaskStatus> getAllowedTransitions() {
            return EnumSet.noneOf(TaskStatus.class);
        }
    };

    public abstract Set<TaskStatus> getAllowedTransitions();

    public boolean canTransitionTo(TaskStatus target) {
        return getAllowedTransitions().contains(target);
    }
}
