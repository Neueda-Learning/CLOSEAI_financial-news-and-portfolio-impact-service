import type { ImpactEvent, Sentiment } from '../types/domain'

const tickers = ['AAPL', 'NVDA', 'MSFT', 'AMD'] as const
const sources = ['Reuters', 'Bloomberg', 'CNBC', 'MarketWatch', 'Financial Times'] as const
const sentimentCycle: Array<Sentiment | null> = ['POSITIVE', 'NEGATIVE', 'NEUTRAL', 'POSITIVE', null]

const headlines: Record<(typeof tickers)[number], string[]> = {
  AAPL: [
    'Apple lowers production forecast after supplier checks',
    'Apple expands services bundle for enterprise customers',
    'Apple supplier commentary points to softer iPhone demand',
    'Apple updates App Store review policy',
    'Apple announces new Mac security features for businesses',
    'Apple shares drift as hardware margins remain under watch',
  ],
  NVDA: [
    'Nvidia raises guidance as enterprise AI demand accelerates',
    'Nvidia cloud partners increase GPU capacity orders',
    'Nvidia faces export-control questions after analyst note',
    'Nvidia data center backlog remains above expectations',
    'Nvidia supplier lead times improve into August',
    'Nvidia shares hold gains after AI infrastructure update',
  ],
  MSFT: [
    'Microsoft cloud margin commentary keeps analysts neutral',
    'Microsoft announces Azure region in Malaysia',
    'Microsoft security revenue grows as enterprise renewals improve',
    'Microsoft AI tooling adoption rises across Office customers',
    'Microsoft capex guidance keeps Wall Street cautious',
    'Microsoft expands Copilot integrations for finance teams',
  ],
  AMD: [
    'AMD announces expanded data center GPU availability',
    'AMD wins new server processor design with cloud customer',
    'AMD gaming revenue outlook remains mixed',
    'AMD launches enterprise AI accelerator software update',
    'AMD shares rise after data center channel checks',
    'AMD supply chain note flags cautious inventory planning',
  ],
}

function scoreFor(sentiment: Sentiment | null, index: number) {
  if (sentiment === null) return null
  if (sentiment === 'POSITIVE') return Number((0.48 + (index % 4) * 0.11).toFixed(2))
  if (sentiment === 'NEGATIVE') return Number((-0.42 - (index % 4) * 0.1).toFixed(2))
  return Number(((index % 2) * 0.06).toFixed(2))
}

function impactFor(sentiment: Sentiment | null, index: number) {
  if (sentiment === null) return 0
  const base = 120 + index * 37
  if (sentiment === 'NEGATIVE') return -base
  if (sentiment === 'NEUTRAL') return Math.round(base * 0.16)
  return base
}

export const impactEventsMock: ImpactEvent[] = Array.from({ length: 25 }, (_, index) => {
  const ticker = tickers[index % tickers.length]
  const sentiment = sentimentCycle[index % sentimentCycle.length]
  const sentimentScore = scoreFor(sentiment, index)
  const portfolioImpact = impactFor(sentiment, index)
  const priceChange = sentiment === null ? 0 : sentiment === 'NEGATIVE' ? -Number((0.72 + (index % 5) * 0.44).toFixed(2)) : Number((0.38 + (index % 5) * 0.72).toFixed(2))
  const alignment = sentiment === null ? null : sentiment === 'NEUTRAL' ? 'INCONCLUSIVE' : index % 7 === 0 ? 'DIVERGENT' : 'CONFIRMED'
  const publishedHour = 9 + (index % 7)
  const publishedMinute = String((index * 7) % 60).padStart(2, '0')
  const firstPrice = ticker === 'NVDA' ? 121.4 : ticker === 'MSFT' ? 436.3 : ticker === 'AMD' ? 158.4 : 190.1

  return {
    id: index + 1,
    ticker,
    headline: headlines[ticker][index % headlines[ticker].length],
    source: sources[index % sources.length],
    url: `https://example.com/article/${index + 1}`,
    publishedAt: `2026-07-${String(27 - (index % 3)).padStart(2, '0')}T${String(publishedHour).padStart(2, '0')}:${publishedMinute}:00Z`,
    sentiment,
    sentimentScore,
    confidence: sentiment === null ? null : Number((0.7 + (index % 4) * 0.07).toFixed(2)),
    priceChange,
    portfolioImpact,
    strength: Math.abs(portfolioImpact) > 600 ? 'Strong' : Math.abs(portfolioImpact) > 260 ? 'Moderate' : 'Watch',
    alignment,
    content:
      sentiment === null
        ? `${ticker} news has been stored and matched to the portfolio, but the LLM agent has not finished sentiment analysis yet. This is a normal pipeline state, so the interface keeps it as analyzing rather than reporting an error.`
        : `${ticker} was mentioned in a market-moving headline from ${sources[index % sources.length]}. The assessment links the headline direction to the portfolio holding, then compares it with the same-day price move to estimate dollar impact.`,
    priceSeries: [
      { time: `${String(publishedHour - 1).padStart(2, '0')}:45`, price: Number((firstPrice * (1 - 0.006)).toFixed(2)) },
      { time: `${String(publishedHour).padStart(2, '0')}:${publishedMinute}`, price: Number(firstPrice.toFixed(2)) },
      { time: `${String(publishedHour).padStart(2, '0')}:45`, price: Number((firstPrice * (1 + priceChange / 180)).toFixed(2)) },
      { time: `${String(publishedHour + 1).padStart(2, '0')}:20`, price: Number((firstPrice * (1 + priceChange / 100)).toFixed(2)) },
    ],
  }
})
