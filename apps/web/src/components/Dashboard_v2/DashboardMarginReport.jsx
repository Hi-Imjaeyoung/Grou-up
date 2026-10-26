import React, { useEffect, useState } from "react";
import { getDailyMarginSummary } from '../../services/margin';
// CSS 파일 import 경로는 .module.css가 아니므로 그대로 둔다.
import "../../styles/Dashboard/DashboardMarginReport.css";
import { formatInteger } from "../../utils/formatUtils";

const DashboardMarginReport = ({ selectedDate }) => {
    const [data, setData] = useState([]);
    useEffect(() => {
        const fetchData = async () => {
            try {
                const response = await getDailyMarginSummary({ date: selectedDate });
                setData(response.data || []);
            } catch (error) {
                console.error("Error fetching margin summary:", error);
            }
        };
        if (selectedDate) {
            fetchData();
        }
    }, [selectedDate]);


    return (
        <div className="dashboard-card dashboard-margin-report">
            <div className="card-label">마진 보고서</div>

            {/* --- 테이블 구조를 하나로 합치고, tbody에 스크롤 클래스 적용 --- */}
            <div className="margin-report-table-container"> {/* 테이블을 감싸는 컨테이너 추가 */}
                <table className="margin-table">
                    <thead>
                        <tr>
                            <th>캠페인 이름</th>
                            <th>광고 마진</th>
                            <th>순 이익</th>
                        </tr>
                    </thead>
                    <tbody className="margin-table-body-scroll"> {/* tbody에 스크롤 클래스 적용! */}
                        {data.length === 0 ? (
                            <tr>
                                <td colSpan="3" style={{ textAlign: 'center', color: '#999' }}>
                                    일일 보고서를 넣어주세요.
                                </td>
                            </tr>
                        ) : (
                            data.map((item, i) => (
                                // key 값은 고유한 ID가 있으면 그걸 쓰는 게 제일 좋아.
                                <tr key={`${item.marProductName}-${i}`}>
                                    <td>{item.marProductName}</td>
                                    <td>{formatInteger(item.marAdMargin)}</td>
                                    <td className={item.marNetProfit < 0 ? 'text-red' : ''}>
                                        {formatInteger(item.marNetProfit)}
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default DashboardMarginReport;