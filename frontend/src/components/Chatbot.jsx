import { useState, useEffect, useRef, useMemo } from 'react'
import { Bot, Send, X, Minus, Trash2, Sparkles, AlertCircle, RefreshCw } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { sendChatMessage } from '../services/assistantService'

const STORAGE_KEY = 'omnicart_chat_history'

function formatTime(date = new Date()) {
  return new Intl.DateTimeFormat('en-US', {
    hour: 'numeric',
    minute: 'numeric',
    hour12: true,
  }).format(date)
}

function FormattedMessage({ text }) {
  if (!text) return null
  const lines = text.split('\n')

  return (
    <div className="chat-formatted-body">
      {lines.map((line, idx) => {
        const trimmed = line.trim()
        if (!trimmed) {
          return <div key={idx} className="chat-spacer" />
        }

        // Parse **bold** markers safely
        const parts = line.split(/(\*\*[^*]+\*\*)/g)
        const renderedLine = parts.map((part, pIdx) => {
          if (part.startsWith('**') && part.endsWith('**')) {
            return <strong key={pIdx}>{part.slice(2, -2)}</strong>
          }
          return part
        })

        const isBullet = trimmed.startsWith('•') || trimmed.startsWith('-') || trimmed.startsWith('*')

        return (
          <div
            key={idx}
            className={`chat-line ${isBullet ? 'chat-bullet' : ''}`}
          >
            {renderedLine}
          </div>
        )
      })}
    </div>
  )
}

export default function Chatbot() {
  const { user } = useAuth()
  const isOwner = user?.role === 'ADMIN'

  const defaultSuggestions = useMemo(() => {
    if (isOwner) {
      return [
        "Today's total sales?",
        'Which products are low in stock?',
        'How many bills were generated today?',
        'Which cashier generated the most bills?',
        "Give me a summary of my shop's current status.",
      ]
    }
    return [
      'What are my sales today?',
      'How many bills have I generated today?',
      'Show my recent bills.',
      'Which products are out of stock?',
      'How do I create a bill?',
    ]
  }, [isOwner])

  const initialWelcomeMessage = useMemo(() => ({
    id: 'welcome',
    sender: 'assistant',
    text: "Hello! I'm your OmniCart Assistant. How can I help you today?",
    timestamp: formatTime(new Date()),
    suggestions: defaultSuggestions,
  }), [defaultSuggestions])

  const [isOpen, setIsOpen] = useState(false)
  const [messages, setMessages] = useState(() => {
    try {
      const saved = sessionStorage.getItem(STORAGE_KEY)
      if (saved) {
        const parsed = JSON.parse(saved)
        if (Array.isArray(parsed) && parsed.length > 0) return parsed
      }
    } catch {
      // Ignore parse errors and use initial
    }
    return [initialWelcomeMessage]
  })
  const [inputValue, setInputValue] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState(null)
  const [showClearConfirm, setShowClearConfirm] = useState(false)

  const messagesEndRef = useRef(null)
  const inputRef = useRef(null)

  // Sync messages to sessionStorage
  useEffect(() => {
    try {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(messages))
    } catch {
      // Storage quota or disabled
    }
  }, [messages])

  // Scroll to bottom whenever messages or loading state changes
  useEffect(() => {
    if (isOpen) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
    }
  }, [messages, isLoading, isOpen])

  // Focus input when opened
  useEffect(() => {
    if (isOpen) {
      setTimeout(() => {
        inputRef.current?.focus()
      }, 150)
    }
  }, [isOpen])

  const handleSendMessage = async (rawText) => {
    const text = (rawText || inputValue).trim()
    if (!text || isLoading) return

    setInputValue('')
    setError(null)
    setShowClearConfirm(false)

    const userMessage = {
      id: `usr_${Date.now()}`,
      sender: 'user',
      text,
      timestamp: formatTime(),
    }

    setMessages((prev) => [...prev, userMessage])
    setIsLoading(true)

    try {
      const data = await sendChatMessage(text)
      const assistantMessage = {
        id: `asst_${Date.now()}`,
        sender: 'assistant',
        text: data.answer || "I've processed your request.",
        intent: data.intent,
        suggestions: Array.isArray(data.suggestions) && data.suggestions.length > 0
          ? data.suggestions
          : defaultSuggestions,
        timestamp: formatTime(),
      }
      setMessages((prev) => [...prev, assistantMessage])
    } catch (err) {
      const errorMsg = err.response?.data?.message || 'Unable to connect to the assistant. Please try again.'
      setError(errorMsg)
      const errorAssistantMessage = {
        id: `err_${Date.now()}`,
        sender: 'assistant',
        isError: true,
        text: `⚠️ **Connection Error**: ${errorMsg}`,
        timestamp: formatTime(),
        suggestions: defaultSuggestions,
      }
      setMessages((prev) => [...prev, errorAssistantMessage])
    } finally {
      setIsLoading(false)
      setTimeout(() => inputRef.current?.focus(), 100)
    }
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSendMessage()
    }
  }

  const handleClearChat = () => {
    setMessages([
      {
        ...initialWelcomeMessage,
        id: `welcome_${Date.now()}`,
        timestamp: formatTime(),
      },
    ])
    setShowClearConfirm(false)
    setError(null)
  }

  return (
    <>
      {/* Floating Chatbot Toggle Button */}
      {!isOpen && (
        <button
          className="chatbot-fab"
          onClick={() => setIsOpen(true)}
          aria-label="Open AI Chatbot"
          title="AI Chatbot"
        >
          <div className="chatbot-fab-inner">
            <Bot size={24} />
            <span className="chatbot-fab-pulse" />
          </div>
          <span className="chatbot-tooltip">AI Chatbot</span>
        </button>
      )}

      {/* Chatbot Window */}
      {isOpen && (
        <div className="chatbot-window" role="dialog" aria-label="AI Chatbot">
          {/* Header */}
          <div className="chatbot-header">
            <div className="chatbot-header-info">
              <div className="chatbot-avatar-badge">
                <Bot size={20} />
                <span className="chatbot-status-dot" title="Online & connected" />
              </div>
              <div className="chatbot-title-box">
                <div className="chatbot-title-row">
                  <span className="chatbot-title">AI Chatbot</span>
                  <span className="chatbot-chip">{isOwner ? 'Owner' : 'Cashier'}</span>
                </div>
                <span className="chatbot-subtitle">OmniCart Assistant</span>
              </div>
            </div>

            <div className="chatbot-header-actions">
              {showClearConfirm ? (
                <div className="clear-confirm-pop">
                  <span>Clear?</span>
                  <button className="confirm-btn yes" onClick={handleClearChat}>Yes</button>
                  <button className="confirm-btn no" onClick={() => setShowClearConfirm(false)}>No</button>
                </div>
              ) : (
                <button
                  className="chatbot-icon-btn"
                  onClick={() => setShowClearConfirm(true)}
                  title="Clear conversation"
                  aria-label="Clear conversation"
                >
                  <Trash2 size={16} />
                </button>
              )}

              <button
                className="chatbot-icon-btn"
                onClick={() => setIsOpen(false)}
                title="Minimize chatbot"
                aria-label="Minimize chatbot"
              >
                <Minus size={17} />
              </button>

              <button
                className="chatbot-icon-btn"
                onClick={() => setIsOpen(false)}
                title="Close chatbot"
                aria-label="Close chatbot"
              >
                <X size={17} />
              </button>
            </div>
          </div>

          {/* Messages Body */}
          <div className="chatbot-messages">
            {messages.map((msg, index) => (
              <div
                key={msg.id || index}
                className={`chat-bubble-row ${msg.sender === 'user' ? 'user' : 'assistant'}`}
              >
                {msg.sender === 'assistant' && (
                  <div className={`chat-msg-avatar ${msg.isError ? 'error' : ''}`}>
                    <Bot size={16} />
                  </div>
                )}

                <div className="chat-bubble-container">
                  <div className={`chat-bubble ${msg.sender === 'user' ? 'user' : 'assistant'} ${msg.isError ? 'error' : ''}`}>
                    <FormattedMessage text={msg.text} />
                  </div>
                  <span className="chat-timestamp">{msg.timestamp}</span>

                  {/* Suggestions Chips below the latest assistant message */}
                  {msg.sender === 'assistant' && index === messages.length - 1 && !isLoading && msg.suggestions?.length > 0 && (
                    <div className="chat-suggestions-wrap">
                      <span className="suggestions-label">
                        <Sparkles size={12} /> Suggested questions:
                      </span>
                      <div className="chat-suggestions-list">
                        {msg.suggestions.map((suggestion, sIdx) => (
                          <button
                            key={sIdx}
                            className="suggestion-chip"
                            onClick={() => handleSendMessage(suggestion)}
                            disabled={isLoading}
                          >
                            {suggestion}
                          </button>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              </div>
            ))}

            {/* Typing Indicator */}
            {isLoading && (
              <div className="chat-bubble-row assistant">
                <div className="chat-msg-avatar">
                  <Bot size={16} />
                </div>
                <div className="chat-bubble-container">
                  <div className="chat-bubble assistant typing-bubble">
                    <span className="typing-dot" />
                    <span className="typing-dot" />
                    <span className="typing-dot" />
                  </div>
                </div>
              </div>
            )}

            {/* Error banner with retry */}
            {error && (
              <div className="chat-error-banner">
                <AlertCircle size={15} />
                <span>Failed to fetch response.</span>
                <button
                  className="chat-retry-btn"
                  onClick={() => {
                    const lastUserMsg = [...messages].reverse().find((m) => m.sender === 'user')
                    if (lastUserMsg) handleSendMessage(lastUserMsg.text)
                  }}
                >
                  <RefreshCw size={12} /> Retry
                </button>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          {/* Input Bar */}
          <div className="chatbot-input-bar">
            <input
              ref={inputRef}
              type="text"
              className="chatbot-input"
              placeholder="Ask me anything about OmniCart..."
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              onKeyDown={handleKeyDown}
              disabled={isLoading}
              maxLength={300}
            />
            <button
              className="chatbot-send-btn"
              onClick={() => handleSendMessage()}
              disabled={!inputValue.trim() || isLoading}
              aria-label="Send message"
              title="Send message"
            >
              <Send size={16} />
            </button>
          </div>
        </div>
      )}
    </>
  )
}
