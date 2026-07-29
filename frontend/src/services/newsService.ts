import { impactService } from './impactService'

export const newsService = {
  async getLatestNews(tickers?: string[]) {
    const events = tickers && tickers.length > 0
      ? await impactService.getImpactEventsForTickers(tickers)
      : await impactService.getImpactEvents()

    return events.map((event) => ({
      id: event.id,
      externalId: event.externalId,
      ticker: event.ticker,
      affectedTickers: event.affectedTickers,
      headline: event.headline,
      source: event.source,
      publishedAt: event.publishedAt,
      url: event.url,
    }))
  },
}
