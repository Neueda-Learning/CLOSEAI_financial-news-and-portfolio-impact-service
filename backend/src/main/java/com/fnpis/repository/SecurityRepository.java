package com.fnpis.repository;

import com.fnpis.domain.Security;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Read the watchlist (V5 seed).
 *
 * <p>Module A (portfolio CRUD) and Module B (quote refresh) both need this.
 * The first module to land it creates it; the second reuses it. Do not create
 * a second SecurityRepository in another package.
 */
@Repository
public interface SecurityRepository extends JpaRepository<Security, String> {
}
