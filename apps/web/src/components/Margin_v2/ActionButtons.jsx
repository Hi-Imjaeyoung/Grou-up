import React, { useState, useEffect, useRef } from "react";
import { downloadExcel, uploadExcel } from "../../services/marginforcampaign";
import "../../styles/MarginCalculatorForm.css"; // CSS 연결

const ActionButtons = ({ selectedOptions, options, handleSave, handleDelete }) => {
    const [errorMessage, setErrorMessage] = useState("");
    // const [selectedFile, setSelectedFile] = useState(null);
    const fileInputRef = useRef(null);

    const onExcelDownloadClick = async () => {
        try {
            // ✨ [수정] downloadExcel() now returns the Blob directly!
            const blobData = await downloadExcel();

            // Check if blobData is actually a Blob (for debugging)
            if (!(blobData instanceof Blob)) {
                console.error("Expected a Blob, but received:", blobData);
                throw new Error("Invalid data received from server.");
            }

            // Use blobData directly
            const url = window.URL.createObjectURL(blobData);
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', 'grouUpExcel.xlsx');
            document.body.appendChild(link);
            link.click();
            link.parentNode.removeChild(link);
            window.URL.revokeObjectURL(url);

        } catch (error) {
            // [2] '실패'는 종류가 뭐든 여기서 모두 처리한다! (404, 500, 네트워크 에러 등)
            console.error("엑셀 다운로드 처리 중 오류 발생:", error);
            if (error.message) {
                // ✅ 서버가 404 등으로 '실패'라고 응답해준 경우
                // 서버가 보낸 진짜 에러 메시지는 error.response.data에 있어.
                alert(error.message);
                // setErrorMessage(serverErrorMessage); // alert 대신 이걸 쓰는 게 더 좋지!
            } else {
                // ❌ 네트워크 문제 등 서버에서 응답조차 받지 못한 경우
                const networkErrorMessage = "다운로드에 실패했습니다. 네트워크 상태를 확인해주세요.";
                alert(networkErrorMessage);
                // setErrorMessage(networkErrorMessage);
            }
        }
    };
    // 저장 가능한 상태인지 확인하는 함수
    const isSaveEnabled = () => {
        if (selectedOptions.length === 0) return false;

        // ✅ selectedOptions는 이제 실제 옵션 객체들의 배열임
        return selectedOptions.every(option => {
            return option &&
                option.campaignId &&
                option.mfcProductName &&
                option.mfcType &&
                option.mfcSalePrice &&
                option.mfcCostPrice &&
                option.mfcTotalPrice;
        });
    };

    const onSaveClick = () => {
        if (!isSaveEnabled()) {
            setErrorMessage("필수 항목(캠페인, 상품명, 유형, 판매가, 원가, 총비용)을 모두 채워주세요.");
            return;
        }
        setErrorMessage(""); // 에러 메시지 초기화
        handleSave(); // 부모의 저장 함수 호출
    };

    const onDeleteClick = () => {
        if (selectedOptions.length === 0) {
            alert("삭제할 항목을 선택해주세요.");
            return;
        }
        handleDelete(); // 부모의 삭제 함수 호출
    };

    // [3] 버튼 클릭 시 숨겨진 input을 클릭하는 역할은 그대로
    const onUploadButtonClick = () => {
        fileInputRef.current.click();
    };


    // [2] 파일 선택과 업로드를 한 번에 처리하는 새로운 함수
    const handleFileSelectAndUpload = async (event) => {
        const file = event.target.files[0];
        if (!file) {
            return;
        }

        const formData = new FormData();
        formData.append('file', file);

        try {
            setErrorMessage("업로드 중...");

            // [1] 서버의 응답을 변수에 저장한다. (axios 응답 객체라고 가정)
            const response = await uploadExcel(formData);

            // [2] 응답 본문(response.data)에서 필요한 데이터를 추출하고 가공한다.
            // 예: "data: "{input=0, total=6, update=6, error=0}""
            const resultString = response.data.replace(/[{}"\s]/g, "").replace("data:", "");
            // -> "input=0,total=6,update=6,error=0"

            const results = {};
            resultString.split(',').forEach(part => {
                const [key, value] = part.split('=');
                results[key] = value;
            });
            // -> {input: '0', total: '6', update: '6', error: '0'}

            // [3] 추출한 데이터로 사용자에게 보여줄 메시지를 만든다.
            const alertMessage = `업로드 완료!\n\n총 ${results.total}건 중 신규 등록${results.input}건, 업데이트 ${results.update}건이 완료되었습니다. (오류: ${results.error}건)`;

            // [4] 만든 메시지를 alert으로 보여준다!
            alert(alertMessage);
            window.location.reload();
        } catch (error) {
            console.error("엑셀 업로드 중 오류 발생:", error);

            // 에러 상황에서도 alert을 띄워주자.
            const errorMessage = error.response ? error.response.data : `❌ 업로드 실패: ${error.message}`;
            alert(errorMessage);
            // setErrorMessage(errorMessage);

        } finally {
            event.target.value = null;
        }
    };


    // 3초 후 에러 메시지 자동 삭제
    useEffect(() => {
        if (errorMessage) {
            const timer = setTimeout(() => setErrorMessage(""), 3000);
            return () => clearTimeout(timer);
        }
    }, [errorMessage]);

    return (
        <div className="action-button-group">
            <input
                type="file"
                ref={fileInputRef}
                onChange={handleFileSelectAndUpload} // 👈 새로운 통합 함수를 연결
                style={{ display: 'none' }}
                accept=".xlsx, .xls"
            />
            <button className="excel-upload-button" onClick={onUploadButtonClick}>
                엑셀 업로드
            </button>
            <button
                className="excel-download-button" // CSS 클래스는 원하는 대로 지정해줘
                onClick={onExcelDownloadClick}
            >
                엑셀 다운로드
            </button>
            <button
                className="save-button"
                onClick={onSaveClick}
                disabled={!isSaveEnabled()}
                style={{
                    opacity: isSaveEnabled() ? 1 : 0.6,
                    cursor: isSaveEnabled() ? 'pointer' : 'not-allowed'
                }}
            >
                저장 ({selectedOptions.length})
            </button>
            <button
                className="delete-button"
                onClick={onDeleteClick}
                disabled={selectedOptions.length === 0}
                style={{
                    opacity: selectedOptions.length > 0 ? 1 : 0.6,
                    cursor: selectedOptions.length > 0 ? 'pointer' : 'not-allowed'
                }}
            >
                삭제 ({selectedOptions.length})
            </button>
            {errorMessage && (
                <div className="error-message-box">
                    {errorMessage}
                </div>
            )}
        </div>
    );
};

export default ActionButtons;