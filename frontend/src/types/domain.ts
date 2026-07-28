export type Sentiment = 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL'

export type Holding = {
  id: number
  ticker: string
  companyName: string
  shares: number
  averageCost: number
  currentPrice: number
  dayChangePct: number
}

export type PortfolioSummary = {
  totalValue: number
  todayChange: number
  todayChangePct: number
  allocation: Array<{ ticker: string; weight: number; value: number }>
  sentimentTrend: Array<{ date: string; positive: number; negative: number; neutral: number }>
}

export type ImpactEvent = {
  id: number
  ticker: string
  headline: string
  source: string
  publishedAt: string
  sentiment: Sentiment
  confidence: number
  priceChange: number
  portfolioImpact: number
  strength: 'Strong' | 'Moderate' | 'Watch'
  content: string
  priceSeries: Array<{ time: string; price: number }>
}
