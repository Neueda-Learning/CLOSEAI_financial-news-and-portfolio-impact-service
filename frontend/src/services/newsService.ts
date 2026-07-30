import { apiFetch, type PagedResponse } from './apiClient'

export const newsService = {
  async getLatestNews(ticker?: string) {
    const query = new URLSearchParams({ page: '1', size: '20' })
    if (ticker) query.set('symbol', ticker)
    return apiFetch<PagedResponse<unknown>>(`/news?${query}`)
  },
}
