package com.hyeonpyo.wallpadcontroller.domain.packethistory;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public final class PacketLogSpecification {

    private PacketLogSpecification() {
    }

    public static Specification<PacketLog> fromCriteria(PacketLogSearchCriteria c) {
        return (root, query, cb) -> {
            applyNewestFirstOrder(root, query, cb);
            List<Predicate> predicates = new ArrayList<>();
            c.header().ifPresent(h -> predicates.add(headerEquals(root, cb, h)));
            c.receivedFromInclusive()
                    .ifPresent(t -> predicates.add(cb.greaterThanOrEqualTo(root.get("receivedAt"), t)));
            c.receivedToInclusive()
                    .ifPresent(t -> predicates.add(cb.lessThanOrEqualTo(root.get("receivedAt"), t)));
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** 목록 조회에서만 정렬. 카운트 쿼리({@code Long})에는 orderBy를 넣지 않음. */
    private static void applyNewestFirstOrder(Root<PacketLog> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        if (!PacketLog.class.equals(query.getResultType())) {
            return;
        }
        query.orderBy(cb.desc(root.get("receivedAt")), cb.desc(root.get("id")));
    }

    private static Predicate headerEquals(Root<PacketLog> root, CriteriaBuilder cb, String headerUpper) {
        Expression<String> noSpace = cb.function(
                "replace", String.class, root.get("rawData"), cb.literal(" "), cb.literal(""));
        Expression<String> upperCompact = cb.upper(noSpace);
        Expression<String> head = cb.substring(upperCompact, 1, 2);
        return cb.equal(head, headerUpper);
    }
}
