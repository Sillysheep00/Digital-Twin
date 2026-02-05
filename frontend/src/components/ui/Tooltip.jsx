import { useState } from 'react';
import { HelpCircle } from 'lucide-react';

function Tooltip({ text, children, position = 'top' }) {
  const [show, setShow] = useState(false);

  const getPositionStyles = () => {
    const base = {
      position: 'absolute',
      padding: '10px 14px',
      background: 'rgba(0, 0, 0, 0.95)',
      color: 'white',
      borderRadius: '6px',
      fontSize: '12px',
      lineHeight: '1.5',
      minWidth: '180px',
      maxWidth: '600px',
      zIndex: 10000,
      border: '1px solid rgba(59, 130, 246, 0.3)',
      boxShadow: '0 4px 12px rgba(0, 0, 0, 0.4)',
      whiteSpace: 'normal',
      pointerEvents: 'none'
    };

    switch (position) {
      case 'top':
        return {
          ...base,
          bottom: '100%',
          left: '50%',
          transform: 'translateX(-50%)',
          marginBottom: '8px'
        };
      case 'bottom':
        return {
          ...base,
          top: '100%',
          left: '50%',
          transform: 'translateX(-50%)',
          marginTop: '8px'
        };
      case 'left':
        return {
          ...base,
          right: '100%',
          top: '50%',
          transform: 'translateY(-50%)',
          marginRight: '8px'
        };
      case 'right':
        return {
          ...base,
          left: '100%',
          top: '50%',
          transform: 'translateY(-50%)',
          marginLeft: '8px'
        };
      default:
        return base;
    }
  };

  const getArrowStyles = () => {
    const arrow = {
      position: 'absolute',
      width: 0,
      height: 0
    };

    switch (position) {
      case 'top':
        return {
          ...arrow,
          top: '100%',
          left: '50%',
          transform: 'translateX(-50%)',
          borderLeft: '6px solid transparent',
          borderRight: '6px solid transparent',
          borderTop: '6px solid rgba(0, 0, 0, 0.95)'
        };
      case 'bottom':
        return {
          ...arrow,
          bottom: '100%',
          left: '50%',
          transform: 'translateX(-50%)',
          borderLeft: '6px solid transparent',
          borderRight: '6px solid transparent',
          borderBottom: '6px solid rgba(0, 0, 0, 0.95)'
        };
      case 'left':
        return {
          ...arrow,
          left: '100%',
          top: '50%',
          transform: 'translateY(-50%)',
          borderTop: '6px solid transparent',
          borderBottom: '6px solid transparent',
          borderLeft: '6px solid rgba(0, 0, 0, 0.95)'
        };
      case 'right':
        return {
          ...arrow,
          right: '100%',
          top: '50%',
          transform: 'translateY(-50%)',
          borderTop: '6px solid transparent',
          borderBottom: '6px solid transparent',
          borderRight: '6px solid rgba(0, 0, 0, 0.95)'
        };
      default:
        return arrow;
    }
  };

  return (
    <span style={{ position: 'relative', display: 'inline-flex', alignItems: 'center' }}>
      <span
        onMouseEnter={() => setShow(true)}
        onMouseLeave={() => setShow(false)}
        style={{ 
          cursor: 'help', 
          display: 'inline-flex', 
          alignItems: 'center', 
          gap: 4,
          userSelect: 'none'
        }}
      >
        {children}
        <HelpCircle size={14} style={{ opacity: 0.5, flexShrink: 0 }} />
      </span>
      {show && (
        <div style={getPositionStyles()}>
          {text}
          <div style={getArrowStyles()} />
        </div>
      )}
    </span>
  );
}

export default Tooltip;
