import { useState, useRef, useEffect } from 'react'
import { ChevronDown, Check, X } from 'lucide-react'

/**
 * Reusable Multi-Select Category Filter Dropdown
 *
 * Supports:
 * - "All categories" toggle and individual category selection
 * - AND search combination logic
 * - Outside-click and Escape key to close
 * - Keyboard accessible and styled according to OmniCart design system
 */
export default function MultiSelectCategoryFilter({
  categories = [],
  selectedIds = [],
  onChange,
  placeholder = 'All categories'
}) {
  const [isOpen, setIsOpen] = useState(false)
  const containerRef = useRef(null)

  const allCategoryIds = categories.map((c) => c.id)
  const isAllSelected = selectedIds.length === 0 || (categories.length > 0 && selectedIds.length === categories.length)
  const isIndeterminate = selectedIds.length > 0 && selectedIds.length < categories.length

  // Close dropdown on outside click
  useEffect(() => {
    function handleClickOutside(event) {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setIsOpen(false)
      }
    }
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside)
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [isOpen])

  // Close dropdown on Escape key
  useEffect(() => {
    function handleKeyDown(event) {
      if (event.key === 'Escape') {
        setIsOpen(false)
      }
    }
    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown)
    }
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [isOpen])

  // Toggle "All categories"
  const handleToggleAll = () => {
    if (isAllSelected) {
      // If all are currently selected, uncheck all (empty selection)
      onChange([])
    } else {
      // If some or none are selected, select all (represented by empty array meaning no filter)
      onChange([])
    }
  }

  // Toggle an individual category
  const handleToggleCategory = (id) => {
    if (isAllSelected) {
      // If currently in "All categories" mode, selecting one category narrows to just that one
      onChange([id])
    } else if (selectedIds.includes(id)) {
      const next = selectedIds.filter((item) => item !== id)
      // If deselecting leaves 0 selected, it naturally reverts to All categories
      onChange(next)
    } else {
      const next = [...selectedIds, id]
      if (next.length === categories.length) {
        // If all are now selected, normalize to empty array (meaning All)
        onChange([])
      } else {
        onChange(next)
      }
    }
  }

  const handleClear = (e) => {
    e.stopPropagation()
    onChange([])
  }

  // Determine button summary label
  const getSummaryLabel = () => {
    if (isAllSelected) return placeholder
    if (selectedIds.length === 1) {
      const found = categories.find((c) => c.id === selectedIds[0])
      return found ? found.name : placeholder
    }
    return `${selectedIds.length} categories selected`
  }

  return (
    <div className="multi-select-wrap" ref={containerRef}>
      <button
        type="button"
        className={`multi-select-trigger input ${isOpen ? 'active' : ''}`}
        onClick={() => setIsOpen(!isOpen)}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
      >
        <span className="multi-select-label">
          {getSummaryLabel()}
          {!isAllSelected && selectedIds.length > 1 && (
            <span className="multi-select-badge">{selectedIds.length}</span>
          )}
        </span>
        <div className="multi-select-icons">
          {!isAllSelected && (
            <span
              role="button"
              tabIndex={0}
              className="multi-select-clear"
              onClick={handleClear}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') handleClear(e)
              }}
              title="Clear selection"
              aria-label="Clear category selection"
            >
              <X size={14} />
            </span>
          )}
          <ChevronDown
            size={16}
            className={`multi-select-chevron ${isOpen ? 'open' : ''}`}
          />
        </div>
      </button>

      {isOpen && (
        <div className="multi-select-panel" role="listbox">
          <div className="multi-select-header">
            <label className="multi-select-option all-option">
              <input
                type="checkbox"
                checked={isAllSelected}
                ref={(el) => {
                  if (el) el.indeterminate = isIndeterminate
                }}
                onChange={handleToggleAll}
              />
              <span className="multi-select-name">
                <strong>All categories</strong>
              </span>
              <span className="multi-select-count muted">{categories.length}</span>
            </label>
          </div>

          <div className="multi-select-divider" />

          <div className="multi-select-list">
            {categories.map((c) => {
              const isChecked = !isAllSelected && selectedIds.includes(c.id)
              return (
                <label key={c.id} className="multi-select-option">
                  <input
                    type="checkbox"
                    checked={isChecked}
                    onChange={() => handleToggleCategory(c.id)}
                  />
                  <span className="multi-select-name">{c.name}</span>
                </label>
              )
            })}
          </div>

          <div className="multi-select-footer">
            <button
              type="button"
              className="multi-select-action-btn"
              onClick={() => onChange([])}
            >
              Reset to all
            </button>
            <button
              type="button"
              className="multi-select-action-btn primary"
              onClick={() => setIsOpen(false)}
            >
              Done
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
