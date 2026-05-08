package com.neuroforged.leadsystem.repository.spec;

import com.neuroforged.leadsystem.entity.Lead;
import com.neuroforged.leadsystem.entity.LeadStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class LeadFilterSpec {

    private LeadFilterSpec() {}

    public static Specification<Lead> withClientId(String clientId) {
        return (root, query, cb) ->
                (clientId == null || clientId.isBlank())
                        ? cb.conjunction()
                        : cb.equal(root.get("clientIdStr"), clientId);
    }

    public static Specification<Lead> withStatus(LeadStatus status) {
        return (root, query, cb) ->
                status == null
                        ? cb.conjunction()
                        : cb.equal(root.get("status"), status);
    }

    public static Specification<Lead> withSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) return cb.conjunction();
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("businessName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)
            );
        };
    }

    public static Specification<Lead> withDateRange(String from, String to) {
        return (root, query, cb) -> {
            if ((from == null || from.isBlank()) && (to == null || to.isBlank())) {
                return cb.conjunction();
            }
            jakarta.persistence.criteria.Predicate predicate = cb.conjunction();
            if (from != null && !from.isBlank()) {
                LocalDateTime fromDt = LocalDate.parse(from).atStartOfDay();
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("createdAt"), fromDt));
            }
            if (to != null && !to.isBlank()) {
                LocalDateTime toDt = LocalDate.parse(to).plusDays(1).atStartOfDay();
                predicate = cb.and(predicate, cb.lessThan(root.get("createdAt"), toDt));
            }
            return predicate;
        };
    }
}
