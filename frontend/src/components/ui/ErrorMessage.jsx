import React, { useState } from 'react';
import { AlertTriangle, AlertCircle, Info, RefreshCw, X, ChevronDown, ChevronUp } from 'lucide-react';

const ERROR_STYLES = {
  critical: {
    background: 'rgba(231, 76, 60, 0.15)',
    border: '1px solid rgba(231, 76, 60, 0.4)',
    iconColor: '#e74c3c',
    textColor: '#ff6b6b',
    headerColor: '#FFFFFF'
  },
  warning: {
    background: 'rgba(243, 156, 18, 0.15)',
    border: '1px solid rgba(243, 156, 18, 0.4)',
    iconColor: '#f39c12',
    textColor: '#ffa726',
    headerColor: '#FFFFFF'
  },
  info: {
    background: 'rgba(59, 130, 246, 0.15)',
    border: '1px solid rgba(59, 130, 246, 0.4)',
    iconColor: '#3B82F6',
    textColor: '#64B5F6',
    headerColor: '#FFFFFF'
  }
};

function ErrorMessage({ 
  type = 'critical', 
  title,
  message, 
  details, 
  recovery = [], 
  onRetry, 
  onDismiss,
  isRetrying = false 
}) {
  const [showDetails, setShowDetails] = useState(false);
  const style = ERROR_STYLES[type];

  const getIcon = () => {
    switch (type) {
      case 'critical':
        return <AlertTriangle size={18} color={style.iconColor} />;
      case 'warning':
        return <AlertCircle size={18} color={style.iconColor} />;
      case 'info':
        return <Info size={18} color={style.iconColor} />;
      default:
        return <AlertTriangle size={18} color={style.iconColor} />;
    }
  };

  return (
    <div
      style={{
        padding: '15px',
        background: style.background,
        border: style.border,
        borderRadius: '8px',
        marginTop: '15px'
      }}
    >
      {/* Header with Icon and Title */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          {getIcon()}
          <strong style={{ color: style.headerColor, fontSize: '15px' }}>
            {title || (type === 'critical' ? 'ERROR' : type === 'warning' ? 'WARNING' : 'INFO')}
          </strong>
        </div>
        {onDismiss && (
          <button
            onClick={onDismiss}
            style={{
              background: 'transparent',
              border: 'none',
              cursor: 'pointer',
              padding: '4px',
              display: 'flex',
              alignItems: 'center',
              opacity: 0.6,
              transition: 'opacity 0.2s'
            }}
            onMouseEnter={(e) => e.currentTarget.style.opacity = '1'}
            onMouseLeave={(e) => e.currentTarget.style.opacity = '0.6'}
          >
            <X size={16} color={style.textColor} />
          </button>
        )}
      </div>

      {/* Main Message */}
      <p style={{ margin: '0 0 10px 0', fontSize: '14px', color: style.headerColor, lineHeight: 1.5 }}>
        {message}
      </p>

      {/* Details */}
      {details && (
        <div style={{ marginBottom: '10px' }}>
          <button
            onClick={() => setShowDetails(!showDetails)}
            style={{
              background: 'rgba(255, 255, 255, 0.05)',
              border: `1px solid ${style.textColor}40`,
              borderRadius: '4px',
              padding: '6px 10px',
              color: style.textColor,
              fontSize: '12px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              transition: 'all 0.2s'
            }}
          >
            {showDetails ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
            {showDetails ? 'Hide Details' : 'Show Details'}
          </button>
          {showDetails && (
            <div
              style={{
                marginTop: '8px',
                padding: '10px',
                background: 'rgba(0, 0, 0, 0.2)',
                borderRadius: '4px',
                fontSize: '13px',
                color: '#B0B0B0',
                fontFamily: 'monospace',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word'
              }}
            >
              {details}
            </div>
          )}
        </div>
      )}

      {/* Recovery Suggestions */}
      {recovery && recovery.length > 0 && (
        <div
          style={{
            marginTop: '12px',
            padding: '12px',
            background: 'rgba(255, 255, 255, 0.03)',
            borderRadius: '6px',
            border: '1px solid rgba(255, 255, 255, 0.1)'
          }}
        >
          <div style={{ color: style.headerColor, fontWeight: 'bold', fontSize: '13px', marginBottom: '8px' }}>
            How to fix:
          </div>
          <ul style={{ margin: 0, paddingLeft: '20px', color: '#B0B0B0', fontSize: '13px', lineHeight: 1.8 }}>
            {recovery.map((suggestion, index) => (
              <li key={index} style={{ marginBottom: '4px' }}>
                {suggestion}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Action Buttons */}
      {(onRetry || onDismiss) && (
        <div style={{ display: 'flex', gap: '10px', marginTop: '15px' }}>
          {onRetry && (
            <button
              onClick={onRetry}
              disabled={isRetrying}
              style={{
                flex: 1,
                padding: '10px 16px',
                background: style.iconColor,
                color: 'white',
                border: 'none',
                borderRadius: '6px',
                fontWeight: 'bold',
                fontSize: '13px',
                cursor: isRetrying ? 'not-allowed' : 'pointer',
                opacity: isRetrying ? 0.6 : 1,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '6px',
                transition: 'all 0.2s'
              }}
            >
              <RefreshCw size={14} className={isRetrying ? 'animate-spin' : ''} />
              {isRetrying ? 'Retrying...' : 'Retry'}
            </button>
          )}
          {onDismiss && !onRetry && (
            <button
              onClick={onDismiss}
              style={{
                flex: 1,
                padding: '10px 16px',
                background: 'rgba(255, 255, 255, 0.1)',
                color: style.headerColor,
                border: `1px solid ${style.textColor}40`,
                borderRadius: '6px',
                fontWeight: 'bold',
                fontSize: '13px',
                cursor: 'pointer',
                transition: 'all 0.2s'
              }}
            >
              Dismiss
            </button>
          )}
        </div>
      )}
    </div>
  );
}

export default ErrorMessage;
