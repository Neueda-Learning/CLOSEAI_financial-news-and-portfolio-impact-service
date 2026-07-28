package com.fnpis.repository;

import com.fnpis.domain.PricePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PricePointRepository
        extends JpaRepository<PricePoint, PricePoint.Key> {
}
