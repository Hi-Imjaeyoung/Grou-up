import React from 'react';
import '../../styles/FileUpload.css';

const UploadButton = ({ children, ...props }) => {
    return (
        <button {...props} className="upload-action-button">
            {children}
        </button>
    );
};

export default UploadButton;
