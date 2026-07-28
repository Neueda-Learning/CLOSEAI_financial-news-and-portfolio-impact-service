package com.fnpis.service;

import com.fnpis.api.internal.dto.AddHoldingRequest;
import com.fnpis.api.internal.dto.HoldingRow;
import com.fnpis.api.internal.dto.UpdateHoldingRequest;
import com.fnpis.common.Freshness;
import com.fnpis.common.PagedResponse;
import com.fnpis.common.error.ApiException;
import com.fnpis.common.error.ErrorCode;
import com.fnpis.domain.Holding;
import com.fnpis.repository.HoldingRepository;
import com.fnpis.repository.SecurityRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Position CRUD and the holdings list (A4-A7).
 *
 * @see #add for the EC-09 merge, the one piece of real arithmetic here
 */
@Service
public class HoldingService {

    /** Cost basis column is DECIMAL(18,4); a merged average rounds to the same scale. */
    private static final int COST_SCALE = 4;

    private final HoldingRepository holdings;
    private final SecurityRepository securities;
    private final PortfolioService portfolios;
    private final HoldingValuationLoader loader;

    HoldingService(
            HoldingRepository holdings,
            SecurityRepository securities,
            PortfolioService portfolios,
            HoldingValuationLoader loader) {
        this.holdings = holdings;
        this.securities = securities;
        this.portfolios = portfolios;
        this.loader = loader;
    }

    /**
     * A4, and EC-09 when the symbol is already held.
     *
     * <p>A repeat add is not rejected: the two lots merge into one position at a
     * weighted average cost,
     * {@code (q1*c1 + q2*c2) / (q1+q2)}. Averaging the two cost bases alone
     * would be wrong whenever the lot sizes differ - 1 share at 10 plus 99 at 20
     * averages to 19.90, not 15.
     */
    @Transactional
    public HoldingRow add(Long portfolioId, AddHoldingRequest request, Instant now) {
        portfolios.require(portfolioId);
        String symbol = request.normalisedSymbol();
        // EC-05: reject unknown symbols before writing anything.
        if (!securities.existsById(symbol)) {
            throw ApiException.securityNotFound(symbol);
        }

        Holding merged = holdings.findByPortfolioIdAndSymbol(portfolioId, symbol)
                .map(existing -> mergeInto(existing, request.quantity(), request.costBasis()))
                .orElseGet(() -> new Holding(
                        portfolioId, symbol, request.quantity(), request.costBasis()));

        return rowFor(holdings.save(merged), now);
    }

    /** A7. Absent fields stay as they are. */
    @Transactional
    public HoldingRow update(Long holdingId, UpdateHoldingRequest request, Instant now) {
        if (request.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "至少要修改一个字段");
        }
        Holding h = require(holdingId);
        if (request.quantity() != null) {
            h.setQuantity(request.quantity());
        }
        if (request.costBasis() != null) {
            h.setCostBasis(request.costBasis());
        }
        return rowFor(holdings.save(h), now);
    }

    /** A6. */
    @Transactional
    public void delete(Long holdingId) {
        holdings.delete(require(holdingId));
    }

    /**
     * A5. Weights stay relative to the whole portfolio, not the page.
     *
     * <p>A page-relative weight would sum to 100% on every page and tell the user
     * nothing, so the full position set is valued and the requested page is cut
     * out of the result.
     */
    @Transactional(readOnly = true)
    public PagedResponse<HoldingRow> list(Long portfolioId, Integer page, Integer size, Instant now) {
        portfolios.require(portfolioId);
        int pageSize = PagedResponse.clampSize(size);
        // Contract 1.4: client pages are 1-based, Spring's are 0-based.
        int pageIndex = page == null || page < 1 ? 0 : page - 1;
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by("symbol"));

        Valuation v = loader.value(portfolioId, now);
        List<HoldingRow> all = v.rows();
        Page<HoldingRow> slice = new PageImpl<>(pageOf(all, pageIndex, pageSize), pageable, all.size());

        return PagedResponse.from(slice, new Freshness(v.asOf(), v.stale()));
    }

    private Holding mergeInto(Holding existing, BigDecimal addQty, BigDecimal addCost) {
        BigDecimal totalQty = existing.getQuantity().add(addQty);
        BigDecimal weightedCost = existing.totalCost()
                .add(addQty.multiply(addCost))
                .divide(totalQty, COST_SCALE, RoundingMode.HALF_UP);
        existing.setQuantity(totalQty);
        existing.setCostBasis(weightedCost);
        return existing;
    }

    private HoldingRow rowFor(Holding saved, Instant now) {
        // Valued against the whole portfolio so the returned weight matches what
        // the list endpoint will show for the same row.
        return loader.value(saved.getPortfolioId(), now).rows().stream()
                .filter(r -> r.id().equals(saved.getId()))
                .findFirst()
                .orElseThrow(() -> ApiException.holdingNotFound(saved.getId()));
    }

    private Holding require(Long holdingId) {
        return holdings.findById(holdingId)
                .orElseThrow(() -> ApiException.holdingNotFound(holdingId));
    }

    private static List<HoldingRow> pageOf(List<HoldingRow> all, int pageIndex, int pageSize) {
        int from = Math.min(pageIndex * pageSize, all.size());
        return all.subList(from, Math.min(from + pageSize, all.size()));
    }
}
