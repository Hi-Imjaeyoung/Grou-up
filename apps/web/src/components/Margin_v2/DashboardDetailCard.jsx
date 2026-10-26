import React from 'react';

const DashboardDetailCard = ({ data, hoveredProduct, formatCurrency }) => {
    return (
        <div className="lg:col-span-4 bg-white rounded-2xl shadow-lg p-4 flex flex-col justify-center">
            {data ? (
                <div className="space-y-3">
                    <h4 className="text-md font-bold text-gray-800 truncate" title={data.campaignName}>
                        {hoveredProduct || '첫 번째 캠페인'} 상세
                    </h4>
                    <div className="grid grid-cols-2 gap-3">
                        <div className="bg-[#57B9AA]/20 p-3 rounded-lg"><p className="text-xs text-gray-600">매출</p><p className="font-bold text-lg text-gray-800">{formatCurrency(data.marSales)}</p></div>
                        <div className="bg-[#F5A244]/20 p-3 rounded-lg"><p className="text-xs text-gray-600">순이익</p><p className="font-bold text-lg text-gray-800">{formatCurrency(data.marNetProfit)}</p></div>
                        <div className="bg-[#5E87E3]/20 p-3 rounded-lg"><p className="text-xs text-gray-600">마진율</p><p className="font-bold text-lg text-gray-800">{(data.marMarginRate ?? 0).toFixed(1)}%</p></div>
                        <div className="bg-[#E0A2C8]/20 p-3 rounded-lg"><p className="text-xs text-gray-600">ROI</p><p className="font-bold text-lg text-gray-800">{(data.marRoi ?? 0).toFixed(1)}%</p></div>
                    </div>
                    <div className="pt-3 border-t border-gray-200">
                        <div className="space-y-2">
                            <div className="flex justify-between text-sm"><span className="text-gray-600">광고비</span><span className="font-semibold text-gray-800">{formatCurrency(data.marAdCost)}</span></div>
                            <div className="flex justify-between text-sm"><span className="text-gray-600">반품비용</span><span className="font-semibold text-gray-800">{formatCurrency(data.marReturnCost)}</span></div>
                            <div className="flex justify-between text-sm"><span className="text-gray-600">반품률</span><span className="font-semibold text-gray-800">{(data.marReturnRate ?? 0).toFixed(1)}%</span></div>
                        </div>
                    </div>
                </div>
            ) : <div className="text-center text-gray-500">데이터가 없습니다.</div>}
        </div>
    );
};

export default DashboardDetailCard;