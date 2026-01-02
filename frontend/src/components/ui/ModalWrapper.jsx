function ModalWrapper({ onClose, children, maxWidth = '800px' }) {
  const modalOverlayStyle = {
    position: 'fixed',
    top: 0, left: 0, right: 0, bottom: 0,
    backgroundColor: 'rgba(0,0,0,0.5)',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 1000
  };

  const modalContentStyle = {
    backgroundColor: 'white',
    padding: '20px',
    borderRadius: '8px',
    maxWidth,
    width: '90%',
    maxHeight: '80vh',
    overflowY: 'auto',
    boxShadow: '0 4px 12px rgba(0,0,0,0.2)'
  };

  const closeButtonStyle = {
    float: 'right',
    cursor: 'pointer',
    border: 'none',
    background: 'none',
    fontSize: '20px',
    fontWeight: 'bold'
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
