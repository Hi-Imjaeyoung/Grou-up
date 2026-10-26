import FileSalesReport from "./FileSalesReport";
import FileMarginReport from "./FileMarginReport";
import FileSalesHistory from "./FileSalesHistory";

import "../../styles/Upload/FileUploadGrid.css";


const FileUploadGrid = () => {
    return (
        <div className="form-main-content">

            <div className="upload-container">
                <div className="upload-mainhead">파일 업로드</div>
                <FileSalesReport />
                <FileMarginReport />
                <FileSalesHistory />
                {/* <FileMarginHistory /> */}
            </div>
        </div>
    );
};
export default FileUploadGrid;