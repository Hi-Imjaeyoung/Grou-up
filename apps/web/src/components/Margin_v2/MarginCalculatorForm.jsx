import React, { useState, useEffect, useCallback, useMemo } from "react";
import OptionsTable from "./OptionsTable";
import ActionButtons from "./ActionButtons";
import { getMyAllExecution, deleteExecutionAboutCampaign, updateExecutionAboutCampaign } from "../../services/marginforcampaign";
import "../../styles/MarginCalculatorForm.css"

const MarginCalculatorForm = ({ campaigns: initialCampaigns }) => {
    const [allOptions, setAllOptions] = useState([]);
    const [campaigns, setCampaigns] = useState(initialCampaigns || []);
    const [selectedOptionIds, setSelectedOptionIds] = useState(new Set());
    const [isLoading, setIsLoading] = useState(true);

    const allSelected = useMemo(() => {
        const displayIds = allOptions.filter(opt => !opt.isPlaceholder).map(opt => opt.id);
        return displayIds.length > 0 && displayIds.every(id => selectedOptionIds.has(id));
    }, [allOptions, selectedOptionIds]);

    // campaigns prop이 변경될 때마다 로컬 state 업데이트
    useEffect(() => {
        if (initialCampaigns && initialCampaigns.length > 0) {
            setCampaigns(initialCampaigns);
        }
    }, [initialCampaigns]);

    // 옵션 데이터는 항상 가져오도록 수정
    useEffect(() => {
        const fetchAllOptions = async () => {
            try {
                setIsLoading(true);
                const response = await getMyAllExecution({});
                setAllOptions(response.data || []);
            } catch (error) {
                console.error("전체 옵션 데이터를 가져오는 중 오류 발생:", error);
                setAllOptions([]);
            } finally {
                setIsLoading(false);
            }
        };
        fetchAllOptions();
    }, []);

    // ✅ 수정된 displayRows - 모든 캠페인 표시 + 각 캠페인별 옵션들
    const displayRows = useMemo(() => {
        if (isLoading) return [];

        const rows = [];
        const optionsMap = allOptions.reduce((map, option) => {
            if (!map.get(option.campaignId)) {
                map.set(option.campaignId, []);
            }
            map.get(option.campaignId).push(option);
            return map;
        }, new Map());

        // 미지정 옵션들 먼저 추가 (맨 위에)
        const unassigned = optionsMap.get(null);
        if (unassigned && unassigned.length > 0) {
            rows.push(...unassigned);
        }

        if (campaigns && campaigns.length > 0) {
            campaigns.forEach(campaign => {
                const optionsForCampaign = optionsMap.get(campaign.campaignId) || [];

                if (optionsForCampaign.length > 0) {
                    // 🔥 옵션이 있는 캠페인: 실제 옵션들 추가
                    rows.push(...optionsForCampaign);
                } else {
                    // 🔥 옵션이 없는 캠페인: 헤더 행만 추가
                    rows.push({
                        id: `campaign-header-${campaign.campaignId}`,
                        campaignId: campaign.campaignId,
                        campaignName: campaign.title,
                        isCampaignHeader: true,
                    });
                }
            });
        }

        return rows;
    }, [campaigns, allOptions, isLoading]);

    const handleSelectAll = () => {
        const allOptionIds = allOptions.filter(opt => !opt.isPlaceholder).map(opt => opt.id);
        if (allSelected) {
            setSelectedOptionIds(new Set());
        } else {
            setSelectedOptionIds(new Set(allOptionIds));
        }
    };

    // ✅ 수정된 addEmptyRowForCampaign
    const addEmptyRowForCampaign = (campaignId, campaignName) => {
        const newRow = {
            id: `new-${Date.now()}-${Math.random()}`, // 더 고유한 ID
            campaignId: campaignId,
            campaignName: campaignName,
            mfcProductName: "",
            mfcSalePrice: "",
            mfcType: "",
            mfcTotalPrice: "",
            mfcCostPrice: "",
            mfcReturnPrice: "",
            mfcPerPiece: "",
            mfcZeroRoas: ""
        };
        setAllOptions(prevOptions => [...prevOptions, newRow]);
    };

    const handleInputChange = (id, field, value) => {
        setAllOptions(prevOptions => {
            const updatedOptions = [...prevOptions];
            const optionIndex = updatedOptions.findIndex(opt => opt.id === id);
            if (optionIndex === -1) return prevOptions;

            const isNumericField = ['mfcSalePrice', 'mfcTotalPrice', 'mfcCostPrice', 'mfcReturnPrice'].includes(field);
            const processedValue = isNumericField ? Number(value) : value;
            updatedOptions[optionIndex] = { ...updatedOptions[optionIndex], [field]: processedValue };

            const currentOption = updatedOptions[optionIndex];
            if (currentOption.mfcSalePrice > 0 && currentOption.mfcTotalPrice > 0 && currentOption.mfcCostPrice > 0) {
                const margin = Math.round(currentOption.mfcSalePrice - (1.1 * currentOption.mfcTotalPrice) - currentOption.mfcCostPrice) || 0;
                const zeroROAS = margin !== 0 ? ((currentOption.mfcSalePrice / margin) * 1.1 * 100).toFixed(2) : "0.00";
                currentOption.mfcPerPiece = margin;
                currentOption.mfcZeroRoas = parseFloat(zeroROAS);
            }
            return updatedOptions;
        });
    };

    const handleCheckboxChange = (id) => {
        setSelectedOptionIds(prev => {
            const newSelected = new Set(prev);
            if (newSelected.has(id)) newSelected.delete(id);
            else newSelected.add(id);
            return newSelected;
        });
    };

    // ✅ 수정된 handleDeleteOption - ID로 삭제하도록 변경
    const handleDeleteOption = async () => {
        if (selectedOptionIds.size === 0) {
            alert("삭제할 항목을 선택해주세요.");
            return;
        }

        const confirmDelete = window.confirm("선택된 항목을 삭제하시겠습니까?");
        if (!confirmDelete) return;

        const deletePromises = [];
        const remainingOptions = allOptions.filter((option) => {
            if (selectedOptionIds.has(option.id)) {
                // 실제 저장된 옵션인 경우에만 API 호출
                if (option.id && !String(option.id).startsWith('new-')) {
                    deletePromises.push(deleteExecutionAboutCampaign({ id: option.id }));
                }
                return false; // 제거
            }
            return true; // 유지
        });

        try {
            await Promise.all(deletePromises);
            setAllOptions(remainingOptions);
            setSelectedOptionIds(new Set());
            alert("삭제되었습니다.");
        } catch (error) {
            console.error("삭제 중 오류 발생:", error);
            alert("삭제 중 오류가 발생했습니다.");
        }
    };

    // ✅ 수정된 handleSave - 캠페인별 저장 로직
    const handleSave = useCallback(async () => {
        if (selectedOptionIds.size === 0) {
            alert("저장할 항목을 선택해주세요.");
            return;
        }

        const selectedItems = allOptions.filter(opt => selectedOptionIds.has(opt.id));

        // ... (필수 값 검증 로직) ...
        const hasInvalidItems = selectedItems.some(item =>
            !item.campaignId || !item.mfcProductName || !item.mfcType ||
            !item.mfcSalePrice || !item.mfcCostPrice || !item.mfcTotalPrice
        );
        if (hasInvalidItems) {
            alert("선택된 항목의 필수 값(캠페인, 상품명, 유형, 판매가, 원가, 총비용)을 모두 채워주세요.");
            return;
        }

        const groupedByCampaign = selectedItems.reduce((acc, item) => {
            if (!acc[item.campaignId]) {
                acc[item.campaignId] = [];
            }
            acc[item.campaignId].push(item);
            return acc;
        }, {});

        try {
            const savePromises = Object.entries(groupedByCampaign).map(([campaignId, items]) => {
                const payload = {
                    campaignId: Number(campaignId),
                    data: items.map(item => ({
                        mfcId: String(item.id).startsWith('new-') ? null : item.id,
                        mfcProductName: item.mfcProductName, mfcType: item.mfcType,
                        mfcSalePrice: item.mfcSalePrice, mfcTotalPrice: item.mfcTotalPrice,
                        mfcCostPrice: item.mfcCostPrice, mfcPerPiece: item.mfcPerPiece,
                        mfcReturnPrice: item.mfcReturnPrice || 0, // 반품비가 비어있으면 0으로 설정
                        mfcZeroRoas: item.mfcZeroRoas,
                    })),
                };
                console.log(payload)
                return updateExecutionAboutCampaign(payload);
            });


            // ✅ 1. Promise.all의 결과를 `responses` 배열로 받습니다.
            const responses = await Promise.all(savePromises);

            // ✅ 2. `reduce`를 사용해 모든 `responses` 배열을 순회하며,
            //      `failedProductNames`가 있는 경우 하나의 큰 실패 목록으로 합칩니다.
            const allFailedProductNames = responses.reduce((acc, response) => {
                if (response.data && response.data.failedProductNames && response.data.failedProductNames.length > 0) {
                    return [...acc, ...response.data.failedProductNames];
                }
                return acc;
            }, []);

            // ✅ 3. 최종적으로 만들어진 실패 목록의 길이를 확인하여 알림을 분기 처리합니다.
            if (allFailedProductNames.length > 0) {
                alert(`저장에 실패한 상품이 있습니다:\n${allFailedProductNames.join(", ")}`);
            } else {
                alert("저장이 성공적으로 완료되었습니다.");
                window.location.reload(); // 성공 시에만 페이지 새로고침
            }

        } catch (error) {
            console.error("저장하는 중 오류가 발생했습니다:", error);
            alert("저장하는 중 오류가 발생했습니다.");
        }
    }, [selectedOptionIds, allOptions]);

    if (isLoading) {
        return (
            <div className="form-main-content">
                <div className="main-table-container">
                    <div>로딩 중...</div>
                </div>
            </div>
        );
    }

    // ✅ selectedOptions 계산 - ActionButtons에서 사용하기 위해
    const selectedOptionsArray = allOptions.filter(option => selectedOptionIds.has(option.id));

    return (
        <div className="form-main-content">
            <div className="main-table-container">
                <div className="main-table-header">
                    <h3>전체 캠페인 마진 계산식</h3>
                    <div className="header-button-group">
                        <ActionButtons
                            selectedOptions={selectedOptionsArray}
                            options={allOptions}
                            handleSave={handleSave}
                            handleDelete={handleDeleteOption}
                        />
                    </div>
                </div>
                <OptionsTable
                    options={displayRows}
                    handleInputChange={handleInputChange}
                    handleCheckboxChange={handleCheckboxChange}
                    selectedOptionIds={selectedOptionIds}
                    handleSelectAll={handleSelectAll}
                    allSelected={allSelected}
                    campaigns={campaigns}
                    handleDeleteOption={handleDeleteOption}
                    addEmptyRowForCampaign={addEmptyRowForCampaign}
                />
            </div>
        </div>
    );
};

export default MarginCalculatorForm;