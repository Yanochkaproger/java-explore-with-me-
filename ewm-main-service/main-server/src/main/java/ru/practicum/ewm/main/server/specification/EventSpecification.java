package ru.practicum.ewm.main.server.specification;

import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.main.server.entity.Event;
import ru.practicum.ewm.main.server.enums.EventStatus;

import java.time.LocalDateTime;
import java.util.List;

public class EventSpecification {

    public static Specification<Event> hasText(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return null;
            }
            String lowerText = "%" + text.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("annotation")), lowerText),
                    cb.like(cb.lower(root.get("description")), lowerText)
            );
        };
    }

    public static Specification<Event> hasCategories(List<Long> categories) {
        return (root, query, cb) -> {
            if (categories == null || categories.isEmpty()) {
                return null;
            }
            return root.get("category").get("id").in(categories);
        };
    }

    public static Specification<Event> isPaid(Boolean paid) {
        return (root, query, cb) -> {
            if (paid == null) {
                return null;
            }
            return cb.equal(root.get("paid"), paid);
        };
    }

    public static Specification<Event> hasRangeStart(LocalDateTime rangeStart) {
        return (root, query, cb) -> {
            if (rangeStart == null) {
                return null;
            }
            return cb.greaterThanOrEqualTo(root.get("eventDate"), rangeStart);
        };
    }

    public static Specification<Event> hasRangeEnd(LocalDateTime rangeEnd) {
        return (root, query, cb) -> {
            if (rangeEnd == null) {
                return null;
            }
            return cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd);
        };
    }

    public static Specification<Event> isPublished() {
        return (root, query, cb) -> cb.equal(root.get("state"), EventStatus.PUBLISHED);
    }

    public static Specification<Event> hasUsers(List<Long> users) {
        return (root, query, cb) -> {
            if (users == null || users.isEmpty()) {
                return null;
            }
            return root.get("initiator").get("id").in(users);
        };
    }

    public static Specification<Event> hasStates(List<EventStatus> states) {
        return (root, query, cb) -> {
            if (states == null || states.isEmpty()) {
                return null;
            }
            return root.get("state").in(states);
        };
    }
}
