import { impactEventsMock } from '../mock/impactMock'
import type { ImpactEvent } from '../types/domain'

function normalizeEvents(events: ImpactEvent[]) {
  const uniqueEvents = new Map<string, ImpactEvent>()

  for (const event of events) {
    if (!uniqueEvents.has(event.externalId)) {
      uniqueEvents.set(event.externalId, event)
    }
  }

  return Array.from(uniqueEvents.values()).sort(
    (left, right) => new Date(right.publishedAt).getTime() - new Date(left.publishedAt).getTime(),
  )
}

export const impactService = {
  async getImpactEvents() {
    return normalizeEvents(impactEventsMock)
  },

  async getImpactEventsForTickers(tickers: string[]) {
    const tickerSet = new Set(tickers.map((ticker) => ticker.toUpperCase()))
    return normalizeEvents(impactEventsMock).filter((event) =>
      event.affectedTickers.some((ticker) => tickerSet.has(ticker)),
    )
  },

  async getImpactEvent(id: number) {
    return normalizeEvents(impactEventsMock).find((event) => event.id === id) ?? normalizeEvents(impactEventsMock)[0]
  },
}
