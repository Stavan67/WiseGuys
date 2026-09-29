package com.wiseguys.repository;

import com.wiseguys.entity.CafeOrder;
import com.wiseguys.entity.TableSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CafeOrderRepository extends JpaRepository<CafeOrder, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<CafeOrder> {
    @Query("SELECT DISTINCT o FROM CafeOrder o " +
            "JOIN FETCH o.tableSession ts " +
            "JOIN FETCH ts.table " +
            "JOIN FETCH o.customer " +
            "LEFT JOIN FETCH o.items " +
            "WHERE o.status IN :statuses " +
            "ORDER BY o.placedAt DESC")
    List<CafeOrder> findByStatusInWithDetails(@Param("statuses") List<CafeOrder.OrderStatus> statuses);

    @Query("SELECT DISTINCT o FROM CafeOrder o " +
            "JOIN FETCH o.tableSession ts " +
            "JOIN FETCH ts.table " +
            "JOIN FETCH o.customer " +
            "LEFT JOIN FETCH o.items " +
            "ORDER BY o.placedAt DESC")
    List<CafeOrder> findAllWithDetails();

    @Query("SELECT o FROM CafeOrder o " +
            "JOIN FETCH o.tableSession ts " +
            "JOIN FETCH ts.table " +
            "JOIN FETCH o.customer " +
            "LEFT JOIN FETCH o.items " +
            "WHERE o.id = :id")
    Optional<CafeOrder> findByIdWithDetails(@Param("id") Long id);

    boolean existsByTableSessionAndStatusIn(TableSession tableSession, List<CafeOrder.OrderStatus> statuses);

    boolean existsByTableSessionAndStatusNotIn(TableSession tableSession, List<CafeOrder.OrderStatus> statuses);

    List<CafeOrder> findByTableSession_Table_TableNumberAndStatusIn(
            String tableNumber, List<CafeOrder.OrderStatus> statuses);

    List<CafeOrder> findByTableSession_SessionTokenOrderByPlacedAtAsc(String sessionToken);

    List<CafeOrder> findByStatus(CafeOrder.OrderStatus status);
}