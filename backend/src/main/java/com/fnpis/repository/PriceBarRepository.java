package com.fnpis.repository;

import com.fnpis.domain.PriceBar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PriceBarRepository extends JpaRepository<PriceBar, PriceBar.Key> {
}
