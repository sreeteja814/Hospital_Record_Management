import { useEffect } from 'react'

export function PageHead({ title, subtitle, children }) {
  return (
    <div className="page-head">
      <div>
        <h1>{title}</h1>
        {subtitle && <p>{subtitle}</p>}
      </div>
      {children && <div className="page-actions">{children}</div>}
    </div>
  )
}

export function Modal({ title, onClose, children, wide }) {
  useEffect(() => {
    const onKey = (e) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKey)
    const previous = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = previous
    }
  }, [onClose])

  return (
    <div
      className="overlay"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <div className={`dialog${wide ? ' wide' : ''}`} role="dialog" aria-modal="true" aria-label={title}>
        <div className="dialog-head">
          <h2>{title}</h2>
          <button type="button" className="btn small" onClick={onClose}>
            Close
          </button>
        </div>
        <div className="dialog-body">{children}</div>
      </div>
    </div>
  )
}

export function Alert({ kind = 'error', children }) {
  return (
    <div className={`alert ${kind}`} role={kind === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  )
}

export function ErrorNote({ error }) {
  if (!error) return null
  return <Alert kind="error">{error.message}</Alert>
}

export function Field({ label, error, hint, children, className = '' }) {
  return (
    <label className={`field${error ? ' invalid' : ''} ${className}`}>
      <span className="field-label">{label}</span>
      {children}
      {hint && !error && <span className="hint">{hint}</span>}
      {error && <span className="field-error">{error}</span>}
    </label>
  )
}

export function Pager({ page, totalPages, total, onPage }) {
  if (!total) return null
  return (
    <div className="pager">
      <span>
        {total} {total === 1 ? 'result' : 'results'}
      </span>
      {totalPages > 1 && (
        <div className="pager-btns">
          <button type="button" className="btn small" disabled={page === 0} onClick={() => onPage(page - 1)}>
            Previous
          </button>
          <span>
            Page {page + 1} of {totalPages}
          </span>
          <button
            type="button"
            className="btn small"
            disabled={page + 1 >= totalPages}
            onClick={() => onPage(page + 1)}
          >
            Next
          </button>
        </div>
      )}
    </div>
  )
}

export function Tag({ tone = '', children }) {
  return <span className={`tag ${tone}`}>{children}</span>
}

const STATUS = {
  SCHEDULED: { label: 'Scheduled', tone: 'amber' },
  COMPLETED: { label: 'Completed', tone: 'green' },
  CANCELLED: { label: 'Cancelled', tone: '' },
}

export function StatusTag({ status }) {
  const s = STATUS[status] || { label: status, tone: '' }
  return <Tag tone={s.tone}>{s.label}</Tag>
}

export function Empty({ title, children }) {
  return (
    <div className="empty">
      <strong>{title}</strong>
      {children}
    </div>
  )
}

export function Loading({ children = 'Loading' }) {
  return (
    <div className="empty" role="status">
      {children}
    </div>
  )
}
