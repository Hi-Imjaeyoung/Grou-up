import React, { useState, useEffect } from "react";
import FileUploadComponent from "../components/FileUploadComponent";
import Button from "./Button";
import useFileUpload from "../hooks/useFileUpload";
import { CircleHelp } from "lucide-react";
import { uploadFile1, uploadFile3 } from "../services/pythonapi";
import "../styles/Mainform.css";
import "../styles/FileUpload.css";

const FileUploadForm = () => {
  const [activeDescription, setActiveDescription] = useState("");
  const [uploadingGlobal, setUploadingGlobal] = useState(false);
  const [uploadText, setUploadText] = useState("업로드 중");

  useEffect(() => {
    let interval;
    if (uploadingGlobal) {
      interval = setInterval(() => {
        setUploadText((prev) => (prev.length < 10 ? prev + "." : "업로드 중"));
      }, 500);
    }
    return () => clearInterval(interval);
  }, [uploadingGlobal]);

  const handleDescriptionChange = (section) => {
    setActiveDescription(descriptions[section]);
  };

  const descriptions = {
    광고보고서: {
      imageSrc: require("../images/SalesReport.png"),
      description: "광고 보고서를 업로드 해주세요.",
    },
    상품정보: {
      imageSrc: require("../images/MarginReport.png"),
      description: "하루 마진 데이터를 업로드 해주세요.",
    },
  };

  // ✅ 광고 보고서 업로드 훅 사용 (단일 파일)
  const {
    file: file1,
    setFile: setFile1,
    handleUploadFile: handleUploadFile1,
  } = useFileUpload(uploadFile1, "광고 보고서 업로드 성공!", false, null, setUploadingGlobal);

  // ✅ 마진 보고서 업로드 (다중 파일)
  const {
    file: file3s,
    setFile: setFile3s,
    handleUploadFile: handleUploadFiles3,
  } = useFileUpload(uploadFile3, "마진 보고서 업로드 성공!", false, null, setUploadingGlobal);

  return (
    <div className="upload-content">
      <h1 className="text-3xl font-bold text-left mb-8">파일 업로드</h1>
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* 광고 보고서 업로드 */}
        <div className="upload-card">
          <h2 className="upload-title">광고 보고서</h2>
          <button className="upload-button" onClick={() => handleDescriptionChange("광고보고서")}>
            <div className="icon-next-to">
              <CircleHelp />
              도움말
            </div>
          </button>
          <FileUploadComponent
            label="광고 보고서를 업로드 해주세요."
            files={file1}
            setFiles={setFile1}
            multiple={false}
          />
          <div className="mt-4 text-center">
            <Button onClick={handleUploadFile1}>업로드</Button>
          </div>
        </div>

        {/* 상품 정보 업로드 (여러 파일) */}
        <div className="upload-card">
          <h2 className="upload-title">일일 마진 보고서</h2>
          <button className="upload-button" onClick={() => handleDescriptionChange("상품정보")}>
            <div className="icon-next-to">
              <CircleHelp />
              도움말
            </div>
          </button>
          <FileUploadComponent
            label="일일 마진 데이터를 업로드 해주세요."
            files={file3s}
            setFiles={setFile3s}
            multiple={true}
          />
          <div className="mt-4 text-center">
            <Button onClick={handleUploadFiles3}>업로드</Button>
          </div>
        </div>
      </div>

      {/* 도움말 설명 */}
      {activeDescription && (
        <div className="help-box">
          {activeDescription.imageSrc && <img src={activeDescription.imageSrc} alt="도움말 이미지" />}
          <p>{activeDescription.description}</p>
        </div>
      )}

      {/* 업로드 진행 중일 때 블러 & 스피너 */}
      {uploadingGlobal && (
        <div className="overlay">
          <div className="spinner"></div>
          <p className="overlay-text">{uploadText}</p>
        </div>
      )}
    </div>
  );
};

export default FileUploadForm;
