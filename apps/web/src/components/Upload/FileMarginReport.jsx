import React, { useState, useEffect } from "react";
import FileUploadComponent from "../FileUploadComponent";
import UploadButton from "../Upload/UploadButton";
import useFileUpload from "../../hooks/useFileUpload";
import UploadLoadingOverlay from "./UploadLoadingOverlay";
import { CircleHelp } from "lucide-react";
import { uploadFile3 } from "../../services/pythonapi";
import "../../styles/FileUpload.css";

const FileMarginReport = () => {
    // 1) 날짜 상태 (초기값: 오늘)
    const [selectedDate, setSelectedDate] = useState(
        () => new Date().toISOString().slice(0, 10)
    );

    // 2) 마운트 시 한 번만 localStorage에서 nextDate 읽어오기
    useEffect(() => {
        const saved = localStorage.getItem("nextDate");
        if (saved) {
            setSelectedDate(saved);
            // 이제 한 번 적용했으니 지워주기
            localStorage.removeItem("nextDate");
        }
    }, []);

    const [showHelp, setShowHelp] = useState(false);
    const [uploadingGlobal, setUploadingGlobal] = useState(false);

    // 3) 날짜를 함께 넘기는 래퍼 함수
    const uploadWithDate = (file, onUploadProgress) =>
        uploadFile3(file, selectedDate, onUploadProgress);

    // 4) useFileUpload 훅에 래퍼 전달
    const {
        file: file3,
        setFile: setFile3,
        handleUploadFile: handleUploadFile3,
    } = useFileUpload(
        uploadWithDate,
        "마진 보고서 업로드 성공!",
        false,
        null,
        setUploadingGlobal
    );

    // 5) 업로드 → 성공 시 다음 날짜 계산 후 localStorage에 저장하고 새로고침
    const handleUploadAndAdvance = async () => {
        if (file3.length === 0) {
            alert("파일을 업로드 해주세요.");
            return;
        }
        const ok = await handleUploadFile3();
        if (ok) {
            const next = new Date(selectedDate);
            next.setDate(next.getDate() + 1);
            const nextStr = next.toISOString().slice(0, 10);
            localStorage.setItem("nextDate", nextStr);
            window.location.reload();
        }
    };

    // 6) 파일 레이블
    const fileLabel =
        file3.length === 0
            ? "판매 분석 보고서를 업로드 해주세요."
            : `선택된 파일: ${selectedDate}_${file3[0].name}`;

    return (
        <>
            <div className="file-card file-margin-report">
                <div
                    className="upload-title-row flex items-center justify-between"
                    style={{ gap: 8 }}
                >
                    <div className="flex items-center">
                        <h2 className="upload-title">마진 보고서</h2>
                        <CircleHelp
                            className="help-icon ml-2"
                            onClick={() => setShowHelp(true)}
                            style={{ cursor: "pointer" }}
                        />
                    </div>
                    {/* 달력은 오른쪽 끝에 */}
                    <input
                        type="date"
                        value={selectedDate}
                        onChange={(e) => setSelectedDate(e.target.value)}
                        className="date-input"
                        disabled={uploadingGlobal} // 업로드 중에는 날짜 변경 불가
                        style={{
                            padding: "4px 8px",
                            fontSize: 14,
                            borderRadius: 4,
                            border: "1px solid #ccc",
                            backgroundColor: uploadingGlobal ? "#f5f5f5" : "white", // 비활성화 시 배경색 변경
                            cursor: uploadingGlobal ? "not-allowed" : "auto",
                        }}
                    />
                </div>

                <FileUploadComponent
                    label={fileLabel}
                    files={file3}
                    setFiles={setFile3}
                    multiple={false}
                />

                <div className="file-upload-wrapper">
                    <UploadButton
                        onClick={handleUploadAndAdvance}
                        className="upload-action-button"
                        disabled={file3.length === 0 || uploadingGlobal}
                    >
                        {uploadingGlobal ? "업로드 중..." : "파일 업로드"}
                    </UploadButton>
                </div>

                {showHelp && (
                    <div className="help-overlay" onClick={() => setShowHelp(false)}>
                        <div className="help-modal">
                            <img
                                src={require("../../images/MarginReport.png")}
                                alt="도움말"
                            />
                        </div>
                    </div>
                )}
            </div>

            {/* 업로드 로딩 오버레이 추가 */}
            <UploadLoadingOverlay
                isUploading={uploadingGlobal}
                message="마진 보고서 업로드 중입니다..."
            />
        </>
    );
};

export default FileMarginReport;