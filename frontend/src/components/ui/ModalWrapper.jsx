function ModalWrapper({ onClose, children, maxWidth = '800px' }) {
  const modalOverlayStyle = {
    position: 'fixed',
    top: 0, left: 0, right: 0, bottom: 0,
    backgroundColor: 'rgba(11, 14, 20, 0.85)',
    backdropFilter: 'blur(5px)',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 1000
  };

  const modalContentStyle = {
    backgroundColor: '#1E1E1E',
    color: '#FFFFFF',
    padding: '20px',
    borderRadius: '8px',
    border: '1px solid rgba(59, 130, 246, 0.3)',
    maxWidth,
    width: '90%',
    maxHeight: '80vh',
    overflowY: 'auto',
    boxShadow: '0 20px 60px rgba(0,0,0,0.7)'
  };

  const closeButtonStyle = {
    float: 'right',
    cursor: 'pointer',
    border: 'none',
    background: 'none',
    fontSize: '20px',
    fontWeight: 'bold',
    color: '#FFFFFF'
  };

  return (
    <div style={modalOverlayStyle}>
      <div style={modalContentStyle}>
        <button style={closeButtonStyle} onClick={onClose}>×</button>
        {children}
      </div>
    </div>
  );
}

export default ModalWrapper;
