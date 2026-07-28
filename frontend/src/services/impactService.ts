import { impactEventsMock } from '../mock/impactMock'

export const impactService = {
  async getImpactEvents() {
    return impactEventsMock
  },

  async getImpactEvent(id: number) {
    return impactEventsMock.find((event) => event.id === id) ?? impactEventsMock[0]
  },
}
