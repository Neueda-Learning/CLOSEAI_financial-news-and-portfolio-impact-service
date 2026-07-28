import type { Holding, PortfolioSummary } from '../types/domain'

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
  { id: 1, ticker: 'AAPL', companyName: 'Apple Inc.', shares: 20, averageCost: 180, currentPrice: 189.12, dayChangePct: -0.52 },
  { id: 2, ticker: 'NVDA', companyName: 'NVIDIA Corporation', shares: 20, averageCost: 100, currentPrice: 125.6, dayChangePct: 3.46 },
  { id: 3, ticker: 'MSFT', companyName: 'Microsoft Corporation', shares: 12, averageCost: 410, currentPrice: 438.18, dayChangePct: 0.84 },
  { id: 4, ticker: 'AMD', companyName: 'Advanced Micro Devices', shares: 90, averageCost: 141, currentPrice: 159.9, dayChangePct: 2.8 },
]

export const portfolioValueTrend = [
  { date: '07/22', totalValue: 124300 },
  { date: '07/23', totalValue: 125880 },
  { date: '07/24', totalValue: 127160 },
  { date: '07/25', totalValue: 126400 },
  { date: '07/26', totalValue: 129820 },
  { date: '07/27', totalValue: 128450.75 },
]
