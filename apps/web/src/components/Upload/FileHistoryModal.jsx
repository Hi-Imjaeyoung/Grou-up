// FileHistoryModal.jsx
import React from 'react';
import { X, File, Calendar, Database, Plus, Copy, Trash2 } from 'lucide-react';
import '../../styles/Upload/FileHistoryModal.css';
import { deleteNetSalesFile } from "../../services/file";
export default function FileHistoryModal({
    isOpen,
    onClose,
    date,
    files,
    onDeleted,
}) {
    if (!isOpen) return null;

    const handleDelete = async (id) => {
        if (!window.confirm('정말 이 파일을 삭제하시겠습니까?')) return;
        try {
            await deleteNetSalesFile({ id });
            alert('삭제가 완료되었습니다.');
            // 부모 컴포넌트에게 변경 알림
            if (onDeleted) onDeleted(id);
        } catch (error) {
            console.error(error);
            alert('삭제에 실패했습니다.');
        }
    };

    const getFileTypeIcon = (type) => {
        const iconProps = { size: 20, className: "file-type-icon" };
        switch (type?.toLowerCase()) {
            case 'excel':
            case 'xlsx':
            case 'xls':
                return <Database {...iconProps} />;
            case 'csv':
                return <File {...iconProps} />;
            default:
                return <File {...iconProps} />;
        }
    };

    const getFileTypeColor = (type) => {
        switch (type?.toLowerCase()) {
            case 'excel':
            case 'xlsx':
            case 'xls':
                return 'excel';
            case 'csv':
                return 'csv';
            default:
                return 'default';
        }
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="file-history-modal" onClick={(e) => e.stopPropagation()}>
                {files && files.length > 0 ? (
                    <div className="files-list">
                        {files.map((file, idx) => (
                            <div key={idx} className="file-card">
                                <div className="file-header">
                                    <div className="file-info">
                                        <div className={`file-type-badge ${getFileTypeColor(file.type)}`}>
                                            {getFileTypeIcon(file.type)}
                                            <span>{file.type?.toUpperCase() || 'FILE'}</span>
                                        </div>
                                        <h3 className="file-name">{file.fileName}</h3>
                                    </div>
                                    <button
                                        className="delete-button"
                                        onClick={() => handleDelete(file.id)}
                                        aria-label="파일 삭제"
                                    >
                                        <Trash2 size={18} />
                                    </button>
                                </div>

                                <div className="file-stats">
                                    <div className="stat-item">
                                        <Database className="stat-icon" size={16} />
                                        <span className="stat-label">전체 건수</span>
                                        <span className="stat-value total">{file.fileAllCount?.toLocaleString() || 0}</span>
                                    </div>

                                    <div className="stat-item">
                                        <Plus className="stat-icon new" size={16} />
                                        <span className="stat-label">신규 건수</span>
                                        <span className="stat-value new">{file.fileNewCount?.toLocaleString() || 0}</span>
                                    </div>

                                    <div className="stat-item">
                                        <Copy className="stat-icon duplicate" size={16} />
                                        <span className="stat-label">중복 건수</span>
                                        <span className="stat-value duplicate">{file.fileDuplicateCount?.toLocaleString() || 0}</span>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                ) : (
                    <div className="empty-state">
                        <File size={48} className="empty-icon" />
                        <p>업로드된 파일이 없습니다.</p>
                    </div>
                )}
            </div>
        </div >
    );
}