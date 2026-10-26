import React from "react";
import "../../styles/margin/OptionsTable.css";
import { PlusCircle, AlertTriangle } from 'lucide-react';

const OptionsTable = ({
    options,
    handleInputChange,
    handleCheckboxChange,
    selectedOptionIds,
    handleDeleteOption,
    handleSelectAll,
    allSelected,
    campaigns,
    addEmptyRowForCampaign
}) => {
    const getMfcTypeDisplayName = (mfcType) => {
        switch (mfcType) {
            case 'ROCKET_GROWTH': return '로켓그로스';
            case 'SELLER_DELIVERY': return '판매자배송';
            default: return mfcType;
        }
    };

    const calculateShippingCost = (salePrice) => {
        const price = Number(salePrice);
        if (isNaN(price) || price < 0) return 0;
        if (price < 5000) return 300;
        if (price < 10000) return 400;
        if (price < 15000) return 600;
        if (price < 20000) return 800;
        return 1000;
    };

    // 필수 필드 체크 함수
    const isRequiredFieldEmpty = (option, field) => {
        if (!selectedOptionIds.has(option.id)) return false; // 선택되지 않은 항목은 체크 안함

        const requiredFields = ['campaignId', 'mfcProductName', 'mfcType', 'mfcSalePrice', 'mfcCostPrice', 'mfcTotalPrice'];
        if (!requiredFields.includes(field)) return false;

        return !option[field] || option[field] === "";
    };

    // 입력 필드에 필수 표시 추가하는 함수
    const renderInputWithValidation = (option, field, inputElement) => {
        const isEmpty = isRequiredFieldEmpty(option, field);
        return (
            <div className="input-with-validation">
                {inputElement}
                {isEmpty && (
                    <AlertTriangle
                        size={16}
                        className="required-field-icon"
                        title="필수 입력 항목입니다"
                    />
                )}
            </div>
        );
    };

    return (
        <div className="options-table-container">
            <table className="options-table-component">
                <thead>
                    <tr>
                        <th><input type="checkbox" checked={allSelected} onChange={handleSelectAll} /></th>
                        <th>캠페인 명 <span className="required-asterisk">*</span></th>
                        <th>옵션명 <span className="required-asterisk">*</span></th>
                        <th>판매 유형 <span className="required-asterisk">*</span></th>
                        <th>판매가 <span className="required-asterisk">*</span></th>
                        <th>원가 <span className="required-asterisk">*</span></th>
                        <th>총 비용(쿠팡) <span className="required-asterisk">*</span></th>
                        <th>반품비</th>
                        <th>재입고비</th>
                        <th>개당 마진</th>
                        <th>제로 ROAS</th>
                    </tr>
                </thead>
                <tbody>
                    {(() => {
                        let lastCampaignId = null;
                        return (options || []).map((option, index) => {
                            const isFirstInCampaign = option.campaignId !== lastCampaignId;
                            if (option.campaignId) {
                                lastCampaignId = option.campaignId;
                            }

                            const rowClass = isFirstInCampaign ? 'group-start' : 'group-middle';

                            // 🔥 옵션이 없는 캠페인의 헤더 행
                            if (option.isCampaignHeader) {
                                return (
                                    <tr key={option.id} className="campaign-header-row">
                                        <td></td>
                                        <td className="campaign-name-cell">
                                            <div className="campaign-header">
                                                {/* ✅ 수정된 부분: title 속성 추가 */}
                                                <span className="campaign-name" title={option.campaignName}>
                                                    {option.campaignName}
                                                </span>
                                                <button
                                                    className="add-option-button-inline"
                                                    onClick={() => addEmptyRowForCampaign(option.campaignId, option.campaignName)}
                                                    title={`${option.campaignName}에 옵션 추가`}
                                                >
                                                    <PlusCircle size={14} /> 옵션추가
                                                </button>
                                            </div>
                                        </td>
                                        <td className="empty-cell"></td>
                                        <td className="empty-cell"></td>
                                        <td className="empty-cell"></td>
                                        <td className="empty-cell"></td>
                                        <td className="empty-cell"></td>
                                        <td className="empty-cell"></td>
                                        <td className="empty-cell"></td>
                                        <td className="empty-cell"></td>
                                        <td className="empty-cell"></td>
                                    </tr>
                                );
                            }

                            // 🔥 일반 옵션 행
                            return (
                                <tr key={option.id} className={rowClass}>
                                    <td>
                                        <input
                                            type="checkbox"
                                            checked={selectedOptionIds.has(option.id)}
                                            onChange={() => handleCheckboxChange(option.id)}
                                        />
                                    </td>
                                    <td className="campaign-name-cell">
                                        {!option.campaignId && String(option.id).startsWith('new-') ? (
                                            // 미지정 캠페인인 경우 드롭다운
                                            renderInputWithValidation(option, 'campaignId',
                                                <select
                                                    value={option.campaignId || ""}
                                                    onChange={(e) => {
                                                        const selectedCampaign = campaigns.find(c => String(c.campaignId) === e.target.value);
                                                        if (selectedCampaign) {
                                                            handleInputChange(option.id, 'campaignId', selectedCampaign.campaignId);
                                                            handleInputChange(option.id, 'campaignName', selectedCampaign.title);
                                                        }
                                                    }}
                                                    className={`options-table-dropdown ${isRequiredFieldEmpty(option, 'campaignId') ? 'required-empty' : ''}`}
                                                >
                                                    <option value="">캠페인 선택</option>
                                                    {(campaigns || []).map(c => (
                                                        <option key={c.campaignId} value={c.campaignId}>{c.title}</option>
                                                    ))}
                                                </select>
                                            )
                                        ) : (
                                            // ✅ 캠페인이 있는 경우 - 첫 번째 행에만 캠페인명과 버튼 표시
                                            <div className="campaign-header">
                                                {isFirstInCampaign && (
                                                    <>
                                                        {/* ✅ 수정된 부분: title 속성 추가 */}
                                                        <span className="campaign-name" title={option.campaignName}>
                                                            {option.campaignName}
                                                        </span>
                                                        <button
                                                            className="add-option-button-inline"
                                                            onClick={() => addEmptyRowForCampaign(option.campaignId, option.campaignName)}
                                                            title={`${option.campaignName}에 옵션 추가`}
                                                        >
                                                            <PlusCircle size={14} /> 옵션추가
                                                        </button>
                                                    </>
                                                )}
                                            </div>
                                        )}
                                    </td>
                                    <td title={option.mfcProductName || ""}>
                                        {renderInputWithValidation(option, 'mfcProductName',
                                            <input
                                                type="text"
                                                value={option.mfcProductName || ""}
                                                onChange={(e) => handleInputChange(option.id, 'mfcProductName', e.target.value)}
                                                className={`options-table-input-name ${isRequiredFieldEmpty(option, 'mfcProductName') ? 'required-empty' : ''}`}
                                                placeholder="상품명 입력"
                                            />
                                        )}
                                    </td>
                                    <td>
                                        {renderInputWithValidation(option, 'mfcType',
                                            <select
                                                className={`options-table-dropdown ${isRequiredFieldEmpty(option, 'mfcType') ? 'required-empty' : ''}`}
                                                value={option.mfcType || ""}
                                                onChange={(e) => handleInputChange(option.id, 'mfcType', e.target.value)}
                                            >
                                                <option value="">선택</option>
                                                <option value="ROCKET_GROWTH">로켓그로스</option>
                                                <option value="SELLER_DELIVERY">판매자배송</option>
                                            </select>
                                        )}
                                    </td>
                                    <td>
                                        {renderInputWithValidation(option, 'mfcSalePrice',
                                            <input
                                                type="number"
                                                value={option.mfcSalePrice || ""}
                                                onChange={(e) => handleInputChange(option.id, 'mfcSalePrice', e.target.value)}
                                                className={`options-table-input ${isRequiredFieldEmpty(option, 'mfcSalePrice') ? 'required-empty' : ''}`}
                                                placeholder="0"
                                            />
                                        )}
                                    </td>
                                    <td>
                                        {renderInputWithValidation(option, 'mfcCostPrice',
                                            <input
                                                type="number"
                                                value={option.mfcCostPrice || ""}
                                                onChange={(e) => handleInputChange(option.id, 'mfcCostPrice', e.target.value)}
                                                className={`options-table-input ${isRequiredFieldEmpty(option, 'mfcCostPrice') ? 'required-empty' : ''}`}
                                                placeholder="0"
                                            />
                                        )}
                                    </td>
                                    <td>
                                        {renderInputWithValidation(option, 'mfcTotalPrice',
                                            <input
                                                type="number"
                                                value={option.mfcTotalPrice || ""}
                                                onChange={(e) => handleInputChange(option.id, 'mfcTotalPrice', e.target.value)}
                                                className={`options-table-input ${isRequiredFieldEmpty(option, 'mfcTotalPrice') ? 'required-empty' : ''}`}
                                                placeholder="0"
                                            />
                                        )}
                                    </td>
                                    <td>
                                        <input
                                            type="number"
                                            value={option.mfcReturnPrice || ""}
                                            onChange={(e) => handleInputChange(option.id, 'mfcReturnPrice', e.target.value)}
                                            className="options-table-input"
                                            placeholder="0"
                                        />
                                    </td>
                                    <td>
                                        <span className="options-table-text">
                                            {calculateShippingCost(option.mfcSalePrice)}
                                        </span>
                                    </td>
                                    <td>
                                        <span className="options-table-text">
                                            {option.mfcPerPiece || 0}
                                        </span>
                                    </td>
                                    <td>
                                        <span className="options-table-text">
                                            {option.mfcZeroRoas ? Number(option.mfcZeroRoas).toFixed(2) : "0.00"}
                                        </span>
                                    </td>
                                </tr>
                            );
                        });
                    })()}
                </tbody>
            </table>
        </div>
    );
};

export default OptionsTable;