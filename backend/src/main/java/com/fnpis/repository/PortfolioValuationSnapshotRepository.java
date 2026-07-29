package com.fnpis.repository;

import com.fnpis.domain.PortfolioValuationSnapshot;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Portfolio value snapshots (B5). Read by the valuation-history chart (F5).
 */
@Repository
public interface PortfolioValuationSnapshotRepository
        extends JpaRepository<PortfolioValuationSnapshot, PortfolioValuationSnapshot.Key> {

    List<PortfolioValuationSnapshot> findByPortfolioIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            Long portfolioId, LocalDate from, LocalDate to);
}
