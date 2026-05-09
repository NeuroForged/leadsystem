package com.neuroforged.leadsystem.repository.spec;

import com.neuroforged.leadsystem.entity.Lead;
import com.neuroforged.leadsystem.entity.LeadStatus;
import org.springframework.data.jpa.domain.Specification;

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
}
