package com.wiseguys.repository.spec;

import com.wiseguys.entity.CafeOrder;
import com.wiseguys.entity.CafeTable;
import com.wiseguys.entity.Customer;
import com.wiseguys.entity.TableSession;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class OrderSpecifications {

    public static Specification<CafeOrder> hasStatus(CafeOrder.OrderStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<CafeOrder> customerNameContains(String search) {
        return (root, query, cb) -> {
            Join<CafeOrder, Customer> customer = root.join("customer");
            return cb.like(cb.lower(customer.get("name")), "%" + search.toLowerCase() + "%");
        };
    }

    public static Specification<CafeOrder> tableNumberContains(String tableNumber) {
        return (root, query, cb) -> {
            Join<CafeOrder, TableSession> session = root.join("tableSession");
            Join<TableSession, CafeTable> table = session.join("table");
            return cb.like(cb.lower(table.get("tableNumber")), "%" + tableNumber.toLowerCase() + "%");
        };
    }

    public static Specification<CafeOrder> placedOnDate(LocalDate date) {
        return (root, query, cb) -> {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            return cb.between(root.get("placedAt"), start, end);
        };
    }
}