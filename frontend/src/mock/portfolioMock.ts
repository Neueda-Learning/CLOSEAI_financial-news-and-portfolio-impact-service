import type { Holding, Portfolio, PortfolioSummary } from '../types/domain'

export const portfolioSummaryMock: PortfolioSummary = {
  totalValue: 128450.75,
  todayChange: -1820.4,
  todayChangePct: -1.4,
  allocation: [
    { ticker: 'AAPL', weight: 32.8, value: 42150 },
    { ticker: 'NVDA', weight: 30.3, value: 38900.25 },
    { ticker: 'MSFT', weight: 17.1, value: 22010 },
    { ticker: 'AMD', weight: 11.2, value: 14390.5 },
    { ticker: 'CASH', weight: 8.6, value: 11000 },
  ],
  sentimentTrend: [
    { date: '07/22', positive: 8, negative: 3, neutral: 5 },
    { date: '07/23', positive: 10, negative: 4, neutral: 6 },
    { date: '07/24', positive: 7, negative: 6, neutral: 8 },
    { date: '07/25', positive: 12, negative: 3, neutral: 4 },
    { date: '07/26', positive: 11, negative: 5, neutral: 5 },
    { date: '07/27', positive: 9, negative: 8, neutral: 7 },
  ],
}

export const holdingsMock: Holding[] = [
  { id: 1, ticker: 'AAPL', companyName: 'Apple Inc.', shares: 20, averageCost: 180, currentPrice: 189.12, dayChangePct: -0.52, quoteUpdatedAt: '2026-07-29T15:38:00Z', quoteSource: 'LIVE' },
  { id: 2, ticker: 'NVDA', companyName: 'NVIDIA Corporation', shares: 20, averageCost: 100, currentPrice: 125.6, dayChangePct: 3.46, quoteUpdatedAt: '2026-07-29T15:39:00Z', quoteSource: 'LIVE' },
  { id: 3, ticker: 'MSFT', companyName: 'Microsoft Corporation', shares: 12, averageCost: 410, currentPrice: 438.18, dayChangePct: 0.84, quoteUpdatedAt: '2026-07-29T15:35:00Z', quoteSource: 'CACHE' },
  { id: 4, ticker: 'AMD', companyName: 'Advanced Micro Devices', shares: 90, averageCost: 141, currentPrice: 159.9, dayChangePct: 2.8, quoteUpdatedAt: '2026-07-29T15:37:00Z', quoteSource: 'LIVE' },
]

export const portfoliosMock: Portfolio[] = [
  {
    id: 1,
    name: 'Core Growth Portfolio',
    createdAt: '2026-07-22T09:00:00Z',
    holdings: holdingsMock,
  },
  {
    id: 2,
    name: 'AI Infrastructure Watchlist',
    createdAt: '2026-07-25T10:30:00Z',
    holdings: [
      { id: 21, ticker: 'NVDA', companyName: 'NVIDIA Corporation', shares: 16, averageCost: 104, currentPrice: 125.6, dayChangePct: 3.46, quoteUpdatedAt: '2026-07-29T15:39:00Z', quoteSource: 'LIVE' },
      { id: 22, ticker: 'AMD', companyName: 'Advanced Micro Devices', shares: 55, averageCost: 148, currentPrice: 159.9, dayChangePct: 2.8, quoteUpdatedAt: '2026-07-29T15:37:00Z', quoteSource: 'LIVE' },
      { id: 23, ticker: 'MSFT', companyName: 'Microsoft Corporation', shares: 8, averageCost: 421, currentPrice: 438.18, dayChangePct: 0.84, quoteUpdatedAt: '2026-07-29T15:35:00Z', quoteSource: 'CACHE' },
    ],
  },
]

export const portfolioValueTrend = [
  { date: '07/22', totalValue: 124300 },
  { date: '07/23', totalValue: 125880 },
  { date: '07/24', totalValue: 127160 },
  { date: '07/25', totalValue: 126400 },
  { date: '07/26', totalValue: 129820 },
  { date: '07/27', totalValue: 128450.75 },
]

export const stockValueTrends = {
  AAPL: [
    { date: '07/01', price: 183.2 },
    { date: '07/08', price: 186.4 },
    { date: '07/12', price: 188.1 },
    { date: '07/16', price: 191.3 },
    { date: '07/20', price: 189.8 },
    { date: '07/24', price: 193.4 },
    { date: '07/27', price: 189.12 },
  ],
  NVDA: [
    { date: '07/01', price: 117.2 },
    { date: '07/08', price: 121.4 },
    { date: '07/12', price: 123.8 },
    { date: '07/16', price: 122.6 },
    { date: '07/20', price: 124.7 },
    { date: '07/24', price: 126.9 },
    { date: '07/27', price: 125.6 },
  ],
  MSFT: [
    { date: '07/01', price: 430.1 },
    { date: '07/08', price: 434.6 },
    { date: '07/12', price: 438.8 },
    { date: '07/16', price: 441.2 },
    { date: '07/20', price: 437.9 },
    { date: '07/24', price: 439.5 },
    { date: '07/27', price: 438.18 },
  ],
  AMD: [
    { date: '07/01', price: 153.4 },
    { date: '07/08', price: 155.6 },
    { date: '07/12', price: 157.2 },
    { date: '07/16', price: 158.8 },
    { date: '07/20', price: 160.7 },
    { date: '07/24', price: 161.5 },
    { date: '07/27', price: 159.9 },
  ],
} as const
