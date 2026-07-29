export type Sentiment = 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL'
export type SentimentState = Sentiment | null

export type Holding = {
  id: number
  ticker: string
  companyName: string
  shares: number
  averageCost: number
  currentPrice: number
  dayChangePct: number
  quoteUpdatedAt: string
  quoteSource: 'LIVE' | 'CACHE'
}

export type Portfolio = {
  id: number
  name: string
  holdings: Holding[]
  createdAt: string
}

export type PortfolioSummary = {
  totalValue: number
  todayChange: number
  todayChangePct: number
  allocation: Array<{ ticker: string; weight: number; value: number }>
  sentimentTrend: Array<{ date: string; positive: number; negative: number; neutral: number }>
}

export type PortfolioValuePoint = {
  date: string
  totalValue: number
}

export type ImpactEvent = {
  id: number
  externalId: string
  ticker: string
  affectedTickers: string[]
  headline: string
  source: string
  url: string
  publishedAt: string
  sentiment: Sentiment
  sentimentScore: number
  confidence: number
  impactDirection: 'BULLISH' | 'BEARISH' | 'NEUTRAL'
  priceChange: number
  portfolioImpact: number
  strength: 'Strong' | 'Moderate' | 'Watch'
  alignment: 'CONFIRMED' | 'DIVERGENT' | 'INCONCLUSIVE'
  content: string
  summary?: string | null
  image?: string | null
  priceSeries: Array<{ time: string; price: number }>
}
