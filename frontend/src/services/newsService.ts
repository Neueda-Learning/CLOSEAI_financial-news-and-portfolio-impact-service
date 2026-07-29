import { newsMock } from '../mock/newsMock'

export const newsService = {
  async getLatestNews() {
    return newsMock
  },
}
