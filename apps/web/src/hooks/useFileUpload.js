import { useState } from "react";

const useFileUpload = (uploadFunction, successMessage, shouldNavigate = false, setFileData = null, setUploadingGlobal) => {
  const [file, setFile] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);

  const handleUploadFile = async () => {
    if (file.length === 0) {
      alert("파일을 업로드 해주세요.");
      return false;
    }

    setUploading(true);
    setUploadingGlobal(true);
    setUploadProgress(0);

    try {
      for (let i = 0; i < file.length; i++) {
        const progressCallback = (progressEvent) => {
          // S3 업로드 진행률 (0-50%)
          const uploadProgress = Math.round((progressEvent.loaded * 50) / progressEvent.total);
          setUploadProgress(uploadProgress);
        };

        // S3 업로드 및 데이터 처리
        const response = await uploadFunction(file[i], progressCallback);

        // 데이터 처리 완료 (100%)
        setUploadProgress(100);

        console.log("처리 완료:", response.data);
      }

      alert(successMessage);
      window.location.reload();
      if (setFileData) setFileData(file);
      return true;

    } catch (error) {
      console.error("업로드 및 처리 오류:", error);
      alert(`업로드 실패: ${error.message || error}`);
      return false;
    } finally {
      setUploading(false);
      setUploadingGlobal(false);
      setUploadProgress(0);
    }
  };

  return {
    file,
    setFile,
    handleUploadFile,
    uploading,
    uploadProgress,
  };
};

export default useFileUpload;