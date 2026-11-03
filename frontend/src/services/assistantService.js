import api from './api'

/**
 * Sends a natural-language message to the OmniCart Assistant backend.
 * @param {string} message User query
 * @returns {Promise<{ answer: string, intent: string, suggestions: string[] }>}
 */
export async function sendChatMessage(message) {
  const response = await api.post('/api/assistant/chat', { message })
  return response.data
}
