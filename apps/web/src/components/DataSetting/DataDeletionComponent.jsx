import React, { useState, useEffect } from "react";
import { getMyCampaigns, deleteCampaignAll, deleteCampaignData } from "../../services/campaign";
import DateRangeCalendar from "../Date/DateRangeCalendar";
import styles from "./DataDeletionComponent.module.css";
import "react-datepicker/dist/react-datepicker.css";
import UploadLoadingOverlay from "../Upload/UploadLoadingOverlay"; // 로딩 오버레이 컴포넌트 import


// 순수 함수는 컴포넌트 밖으로
const formatDate = (date) => date.toISOString().slice(0, 10);

const DataDeletionComponent = () => {
    const [startDate, setStartDate] = useState(() => {
        const today = new Date();
        return formatDate(new Date(today.getFullYear(), today.getMonth(), 1));
    });
    const [endDate, setEndDate] = useState(formatDate(new Date()));
    const [showCalendar, setShowCalendar] = useState(false);
    const [campaigns, setCampaigns] = useState([]);
    const [isAllChecked, setIsAllChecked] = useState(false);
    // / --- 1. 삭제 작업 중인지 여부를 알려주는 state 추가 ---
    const [isDeleting, setIsDeleting] = useState(false);

    useEffect(() => {
        const fetchCampaigns = async () => {
            try {
                const response = await getMyCampaigns();
                // --- 데이터에 checked 속성 추가해서 초기화 ---
                const initialCampaigns = response.data.map(c => ({ ...c, checked: false })) || [];
                setCampaigns(initialCampaigns);
            } catch (error) {
                console.error('Error fetching campaigns:', error);
            }
        };
        fetchCampaigns();
    }, []);

    // --- useEffect로 isAllChecked 상태 동기화 (이전 코드에서 빠져있었네) ---
    useEffect(() => {
        const allChecked = campaigns.length > 0 && campaigns.every(c => c.checked);
        setIsAllChecked(allChecked);
    }, [campaigns]);


    const toggleCalendar = () => setShowCalendar((prev) => !prev);

    const handleDateRangeChange = ({ startDate, endDate }) => {
        setStartDate(startDate);
        setEndDate(endDate);
    };

    const handleCheckboxChange = (id) => {
        setCampaigns(
            campaigns.map((campaign) =>
                campaign.campaignId === id ? { ...campaign, checked: !campaign.checked } : campaign
            )
        );
    };

    const handleSelectAllChange = (e) => {
        const checked = e.target.checked;
        setIsAllChecked(checked);
        setCampaigns(campaigns.map(c => ({ ...c, checked })));
    };

    // handleDeleteAll 함수 수정
    const handleDeleteAll = async () => {
        const selectedCampaigns = campaigns.filter((campaign) => campaign.checked);
        if (selectedCampaigns.length > 0) {
            const confirmation = window.confirm(`정말 ${selectedCampaigns.length}개의 캠페인을 삭제하시겠습니까?`);
            if (confirmation) {
                try {
                    setIsDeleting(true); // --- 2. 로딩 시작 ---
                    const campaignIdsToDelete = selectedCampaigns.map(campaign => Number(campaign.campaignId));
                    await deleteCampaignAll(campaignIdsToDelete);
                    const response = await getMyCampaigns();
                    setCampaigns(response.data || []);
                    alert(`${selectedCampaigns.length}개의 캠패인이 삭제되었습니다.`);
                } catch (error) {
                    console.error("캠페인 삭제 실패:", error);
                    alert("캠페인 삭제에 실패했습니다.");
                } finally {
                    setIsDeleting(false); // --- 3. 로딩 끝 (성공/실패 상관없이) ---
                }
            }
        } else {
            alert("선택된 캠페인이 없습니다.");
        }
    };
    // handleDeleteByDate 함수 수정
    const handleDeleteByDate = async () => {
        const selectedCampaigns = campaigns.filter((campaign) => campaign.checked);
        if (selectedCampaigns.length > 0) {
            const confirmation = window.confirm(`정말 ${selectedCampaigns.length}개의 캠페인의 ${startDate} 부터 ${endDate} 까지의 데이터를 삭제할까요?`);
            if (confirmation) {
                try {
                    setIsDeleting(true); // --- 2. 로딩 시작 ---
                    const campaignIdsToDelete = selectedCampaigns.map(campaign => Number(campaign.campaignId));
                    const response = await deleteCampaignData({
                        campaignIds: campaignIdsToDelete,
                        start: startDate,
                        end: endDate
                    });
                    const getresponse = await getMyCampaigns();
                    const refreshedCampaigns = getresponse.data.map(c => ({ ...c, checked: false })) || [];
                    setCampaigns(refreshedCampaigns);
                    const { campaignOptionDetail, keyword, margin, memo } = response.data;
                    alert(`삭제된 데이터:\n캠페인 옵션 상세: ${campaignOptionDetail}개\n키워드: ${keyword}개\n마진: ${margin}개\n메모: ${memo}개`);
                } catch (error) {
                    console.error("캠페인 삭제 실패:", error);
                    alert("캠페인 삭제에 실패했습니다.");
                } finally {
                    setIsDeleting(false); // --- 3. 로딩 끝 (성공/실패 상관없이) ---
                }
            }
        } else {
            alert("선택된 캠페인이 없습니다.");
        }
    };

    return (
        // --- 5. JSX 구조를 CSS 모듈과 시맨틱 태그에 맞게 재구성 ---
        <div className={styles.container}>
            <header className={styles.header}>
                <h2>캠패인 삭제</h2>

                <div className={styles.dateSelectionContainer}>
                    <button className={styles.dateSelectionButton} onClick={toggleCalendar}>
                        {startDate} ~ {endDate}
                        <span className={styles.dropdownArrow}>▼</span>
                    </button>
                    {showCalendar && (
                        <>
                            <div className={styles.datePickerOverlay} onClick={toggleCalendar}></div>
                            <div className={styles.datePickerModal}>
                                <DateRangeCalendar
                                    initialStartDate={startDate}
                                    initialEndDate={endDate}
                                    onDateRangeChange={handleDateRangeChange}
                                    onClose={toggleCalendar}
                                />
                            </div>
                        </>
                    )}
                </div>
            </header>


            <div className={styles.tableComponent}>
                <div className={styles.controls}>
                    <div className={styles.actions}>
                        <button className={styles.deleteButton} onClick={handleDeleteAll}>캠페인 전체 삭제</button>
                        <button className={styles.deleteButton} onClick={handleDeleteByDate}>캠페인 기간 삭제</button>
                    </div>
                </div>
                <table className={styles.campaignTable}>
                    <thead>
                        <tr>
                            <th>
                                <input
                                    type="checkbox"
                                    checked={isAllChecked}
                                    onChange={handleSelectAllChange}
                                />
                            </th>
                            <th>캠페인명</th>
                        </tr>
                    </thead>
                    <tbody>
                        {campaigns.map((campaign) => (
                            // --- 6. key를 index 대신 고유한 campaignId로 변경 (아주 중요!) ---
                            <tr key={campaign.campaignId}>
                                <td>
                                    <input
                                        type="checkbox"
                                        checked={campaign.checked || false}
                                        onChange={() => handleCheckboxChange(campaign.campaignId)}
                                    />
                                </td>
                                <td>{campaign.title}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
            {isDeleting && (
                <UploadLoadingOverlay
                    isUploading={isDeleting}
                    message="선택한 데이터를 삭제하는 중입니다..."
                />
            )}
        </div>
    );
};

export default DataDeletionComponent;