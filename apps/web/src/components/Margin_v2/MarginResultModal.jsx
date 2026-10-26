import React, { useState, useEffect } from "react";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css"; // DatePicker 스타일
import "../../styles/margin/MarginResultModal.css"; // 스타일 파일
import { getMyAllExecution } from "../../services/marginforcampaign";
import { marginUpdatesByPeriod } from "../../services/margin";

const MarginResultModal = ({ isOpen, onClose, campaignId }) => {
    const [startDate, setStartDate] = useState(new Date());
    const [endDate, setEndDate] = useState(new Date());
    const [tableData, setTableData] = useState([]); // 테이블 데이터를 저장할 상태
    const [selectedOptions, setSelectedOptions] = useState([]); // 선택된 옵션 상태
    const [allSelected, setAllSelected] = useState(false); // 전체 선택 상태

    useEffect(() => {
        if (isOpen) {
            fetchData(); // 모달이 열릴 때 데이터 가져오기
        }
    }, [isOpen]); // isOpen과 campaignId가 변경될 때마다 실행

    const fetchData = async () => {
        try {
            const response = await getMyAllExecution({});
            const optionsWithDefaults = (response.data || []).map(option => ({
                campaignId: option.campaignId,
                campaignName: option.campaignName,
                id: option.id,
                mfcProductName: option.mfcProductName,
                mfcType: option.mfcType,
                mfcSalePrice: option.mfcSalePrice || 0,
                mfcTotalPrice: option.mfcTotalPrice || 0,
                mfcCostPrice: option.mfcCostPrice || 0,
                mfcReturnPrice: option.mfcReturnPrice || 0,
                mfcPerPiece: option.mfcPerPiece || 0,
                mfcZeroRoas: option.mfcZeroRoas || 0,
                // 다른 속성도 필요에 따라 초기화
            }));
            optionsWithDefaults.sort((a, b) => {
                if (a.campaignId < b.campaignId) return -1;
                if (a.campaignId > b.campaignId) return 1;
                return 0;
            });
            setTableData(optionsWithDefaults);
        } catch (error) {
            console.error("옵션 데이터를 가져오는 중 오류 발생:", error);
        }
    };

    // 자동 계산 함수 - 단일 아이템에 대해서만 계산
    const calculateMarginForItem = (item) => {
        if (item.mfcSalePrice > 0 && item.mfcTotalPrice > 0 && item.mfcCostPrice > 0) {
            const margin = Math.round(item.mfcSalePrice - (1.1 * item.mfcTotalPrice) - item.mfcCostPrice) || 0;
            const zeroROAS = margin !== 0 ? ((item.mfcSalePrice / margin) * 1.1 * 100).toFixed(2) : "0.00";

            return {
                ...item,
                mfcPerPiece: margin,
                mfcZeroRoas: parseFloat(zeroROAS),
            };
        }
        return item;
    };

    // 입력값 변경 핸들러 - 자동 계산 포함
    const handleInputChange = (index, field, value) => {
        const updatedTableData = [...tableData];
        updatedTableData[index][field] = parseFloat(value) || 0;

        // 자동 계산 실행
        updatedTableData[index] = calculateMarginForItem(updatedTableData[index]);

        setTableData(updatedTableData);
    };

    // mfcType을 한국어로 변환하는 함수
    const getMfcTypeDisplayName = (mfcType) => {
        switch (mfcType) {
            case 'ROCKET_GROWTH':
                return '로켓그로스';
            case 'SELLER_DELIVERY':
                return '판매자배송';
            default:
                return mfcType; // 기본값: 원래의 값 반환
        }
    };

    const handleSave = async () => {
        if (selectedOptions.length === 0) {
            alert("옵션을 선택해주세요.");
            return;
        }

        // ✅ 1. 선택된 각 항목에 대해 API 요청 Promise 배열을 만듭니다.
        const savePromises = selectedOptions.map(index => {
            const item = tableData[index]; // 선택된 행의 데이터

            // ✅ 2. 백엔드의 SaveReqDto 형식에 정확히 맞춰 payload를 만듭니다.
            const payload = {
                mfcId: item.id, // 또는 item.mfcId (데이터 구조에 맞게)
                startDate: startDate.toISOString().split("T")[0],
                endDate: endDate.toISOString().split("T")[0],
                salePrice: item.mfcSalePrice, // 사용자가 입력한 새 값
                totalPrice: item.mfcTotalPrice, // 사용자가 입력한 새 값
                costPrice: item.mfcCostPrice,   // 사용자가 입력한 새 값
                returnPrice: item.mfcReturnPrice, // 사용자가 입력한 새 값
            };

            // ✅ 3. 이 payload로 API 서비스 함수를 호출합니다.
            // (함수 이름은 marginUpdateByPeriod (단수형) 또는 적절한 것으로 가정)
            return marginUpdatesByPeriod(payload);
        });

        try {
            // ✅ 4. 모든 API 요청을 동시에 보냅니다. (선택한 항목이 3개면 3번의 요청이 나감)
            await Promise.all(savePromises);

            alert("선택된 데이터가 저장되었습니다!");
            // window.location.reload();
        } catch (error) {
            console.error("저장 중 오류 발생:", error);
            alert("저장 중 오류가 발생했습니다.");
        }
    };

    const handleCheckboxChange = (index) => {
        setSelectedOptions((prev) => {
            if (prev.includes(index)) {
                return prev.filter((i) => i !== index); // 체크 해제
            } else {
                return [...prev, index]; // 체크
            }
        });
    };

    const handleSelectAll = () => {
        if (allSelected) {
            setSelectedOptions([]); // 전체 선택 해제
        } else {
            setSelectedOptions(tableData.map((_, index) => index)); // 전체 선택
        }
        setAllSelected(!allSelected); // 전체 선택 상태 반전
    };

    // 모달이 열리지 않을 때는 null을 반환
    if (!isOpen) return null;

    return (
        <div className="modal-overlay">
            <div className="modal-content">
                <button className="close-button" onClick={onClose}>X</button>

                <div className="modal-header">
                    <h2>옵션 마진 설정</h2>
                </div>

                <div className="date-picker-container">
                    <div className="date-picker-group">
                        <DatePicker
                            selected={startDate}
                            onChange={(date) => setStartDate(date)}
                            dateFormat="yyyy-MM-dd"
                            maxDate={new Date()}
                            className="date-picker"
                        />
                        <span className="date-separator">~</span>
                        <DatePicker
                            selected={endDate}
                            onChange={(date) => setEndDate(date)}
                            dateFormat="yyyy-MM-dd"
                            minDate={startDate}
                            maxDate={new Date()}
                            className="date-picker"
                        />
                    </div>
                    <div className="marginupdate-action-buttons">
                        {/* 계산하기 버튼 제거 - 이제 자동으로 계산됩니다 */}
                        <button className="marginupdate-save" onClick={handleSave}>
                            수정하기
                        </button>
                    </div>
                </div>

                <div className="modal-body">
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th>
                                    <input
                                        type="checkbox"
                                        checked={allSelected}
                                        onChange={handleSelectAll}
                                    />
                                </th>
                                <th>캠페인 명</th>
                                <th>옵션명</th>
                                <th>상품타입</th>
                                <th>판매가</th>
                                <th>총비용</th>
                                <th>원가</th>
                                <th>반품비용</th>
                                <th>마진</th>
                                <th>제로ROAS</th>
                            </tr>
                        </thead>
                        <tbody>
                            {(() => {
                                let lastCampaignId = null; // ✅ 이전 행의 캠페인 ID를 추적
                                return tableData.map((item, index) => {
                                    // ✅ 현재 행이 새로운 그룹의 시작인지, 혹은 그룹의 중간인지 확인
                                    const isFirstInGroup = item.campaignId !== lastCampaignId;
                                    const isMiddleOfGroup = item.campaignId === lastCampaignId;

                                    // ✅ 다음 비교를 위해 현재 캠페인 ID 저장
                                    lastCampaignId = item.campaignId;

                                    // ✅ 조건에 따라 CSS 클래스 부여
                                    const rowClass = isFirstInGroup ? 'group-start' : (isMiddleOfGroup ? 'group-middle' : '');

                                    return (
                                        <tr key={item.id} className={rowClass}>
                                            <td>
                                                <input
                                                    type="checkbox"
                                                    checked={selectedOptions.includes(index)}
                                                    onChange={() => handleCheckboxChange(index)}
                                                />
                                            </td>
                                            <td>
                                                {/* ✅ 그룹의 첫 번째 행일 때만 캠페인 이름 표시 */}
                                                {isFirstInGroup ? item.campaignName : ''}
                                            </td>
                                            <td>{item.mfcProductName}</td>
                                            <td>{getMfcTypeDisplayName(item.mfcType)}</td>
                                            <td>
                                                <input
                                                    type="number"
                                                    value={item.mfcSalePrice}
                                                    onChange={(e) => handleInputChange(index, 'mfcSalePrice', e.target.value)}
                                                />
                                            </td>
                                            <td>
                                                <input
                                                    type="number"
                                                    value={item.mfcTotalPrice}
                                                    onChange={(e) => handleInputChange(index, 'mfcTotalPrice', e.target.value)}
                                                />
                                            </td>
                                            <td>
                                                <input
                                                    type="number"
                                                    value={item.mfcCostPrice}
                                                    onChange={(e) => handleInputChange(index, 'mfcCostPrice', e.target.value)}
                                                />
                                            </td>
                                            <td>
                                                <input
                                                    type="number"
                                                    value={item.mfcReturnPrice}
                                                    onChange={(e) => handleInputChange(index, 'mfcReturnPrice', e.target.value)}
                                                />
                                            </td>
                                            <td><span className="option-text">{item.mfcPerPiece}</span></td>
                                            <td><span className="option-text">{item.mfcZeroRoas.toFixed(2)}</span></td>
                                        </tr>
                                    );
                                });
                            })()}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};

export default MarginResultModal;