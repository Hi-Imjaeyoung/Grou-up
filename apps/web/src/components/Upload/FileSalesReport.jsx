import React, { useState } from "react";
import FileUploadComponent from '../FileUploadComponent';
import UploadButton from "../Upload/UploadButton";
import UploadLoadingOverlay from "./UploadLoadingOverlay";
import useFileUpload from '../../hooks/useFileUpload';
import { CircleHelp } from "lucide-react";
import { uploadFile1 } from "../../services/pythonapi";
import { uploadFileWithPresignedUrl } from "../../services/pythonapi";

import "../../styles/FileUpload.css";

const FileSalesReport = () => {
    const [showHelp, setShowHelp] = useState(false);
    const [uploadingGlobal, setUploadingGlobal] = useState(false);
    const {
        file: file1,
        setFile: setFile1,
        handleUploadFile: handleUploadFile1,
    } = useFileUpload(uploadFileWithPresignedUrl, "광고 보고서 업로드 성공!", false, null, setUploadingGlobal);

    return (
        <>
            <div className="file-card file-sales-report">
                <div className="upload-title-row">
                    <h2 className="upload-title">광고 보고서</h2>
                    <CircleHelp
                        className="help-icon"
                        onClick={() => setShowHelp(true)}
                        style={{ cursor: "pointer" }}
                    />
                </div>

                <FileUploadComponent
                    label={
                        file1.length === 0
                            ? "광고 보고서를 업로드 해주세요."
                            : file1.length === 1
                                ? `선택된 파일: ${file1[0].name}`
                                : `선택된 파일 ${file1.length}개`
                    }
                    files={file1}
                    setFiles={setFile1}
                    multiple={false}
                />

                <div className="file-upload-wrapper">
                    <UploadButton
                        onClick={handleUploadFile1}
                        className="upload-action-button"
                        disabled={file1.length === 0 || uploadingGlobal}
                    >
                        {uploadingGlobal ? "업로드 중..." : "파일 업로드"}
                    </UploadButton>
                </div>

                {/* 도움말 이미지 팝업 */}
                {showHelp && (
                    <div className="help-overlay" onClick={() => setShowHelp(false)}>
                        <div className="help-modal">
                            <img src={require("../../images/SalesReport.png")} alt="도움말" />
                        </div>
                    </div>
                )}
            </div>

            {/* 업로드 로딩 오버레이 추가 */}
            <UploadLoadingOverlay
                isUploading={uploadingGlobal}
                message="광고 보고서 업로드 중입니다..."
            />
        </>
    );
};

export default FileSalesReport;