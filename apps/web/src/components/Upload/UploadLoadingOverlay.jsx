import React from 'react';
import '../../styles/Upload/UploadLoadingOverlay.css';

const UploadLoadingOverlay = ({ isUploading, message = "업로드 중입니다...", progress = 0 }) => {
    if (!isUploading) return null;

    return (
        <div className="upload-loading-overlay">
            <div className="upload-loading-modal">
                <div className="spinner"></div>
                <p className="upload-message">{message}</p>

                {/* 진행률 바 (선택사항) */}
                {progress > 0 && (
                    <div className="progress-container">
                        <div className="progress-bar">
                            <div
                                className="progress-fill"
                                style={{ width: `${progress}%` }}
                            ></div>
                        </div>
                        <p className="progress-text">{progress}%</p>
                    </div>
                )}

                <p className="upload-submessage">
                    큰 파일의 경우 시간이 오래 걸릴 수 있습니다.<br />
                    잠시만 기다려주세요...
                </p>
            </div>
        </div>
    );
};

export default UploadLoadingOverlay;