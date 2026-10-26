// src/components/FileUploadComponent.jsx
import React, { useCallback } from 'react';
import { Upload } from 'lucide-react';
import '../styles/FileUpload.css';

const FileUploadComponent = ({ label, files, setFiles, multiple = false }) => {
  // 기존 파일 선택 처리
  const handleFileUpload = useCallback((event) => {
    const selected = Array.from(event.target.files);
    setFiles(multiple ? prev => [...prev, ...selected] : selected);
  }, [multiple, setFiles]);

  // 드롭된 파일 처리 + 확인창
  const handleDrop = useCallback((e) => {
    e.preventDefault();
    const dropped = Array.from(e.dataTransfer.files);

    if (dropped.length === 0) return;
    // 확인 대화상자
    const ok = window.confirm(
      `정말 다음 파일${multiple ? '들' : ''}을 업로드하시겠습니까?\n\n` +
      dropped.map(f => f.name).join('\n')
    );
    if (!ok) return;

    setFiles(multiple ? prev => [...prev, ...dropped] : dropped);
  }, [multiple, setFiles]);

  return (
    <div
      className="file-upload-wrapper"
      onDragOver={e => e.preventDefault()}
      onDrop={handleDrop}
    >
      <label className="file-upload-label">
        <Upload className="file-upload-icon" />
        <span className="file-upload-text">{label}</span>
        <input
          type="file"
          className="file-upload-input"
          accept=".xlsx, .xls"
          onChange={handleFileUpload}
          multiple={multiple}
        />
      </label>
    </div>
  );
};

export default FileUploadComponent;