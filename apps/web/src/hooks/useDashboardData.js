// src/hooks/useDashboardData.js

import { useState, useEffect, useMemo, useCallback } from "react";
import { getMyCampaigns } from "../services/campaign";
import { getMarginByCampaignId, getNetProfitAndReturnCost, findLatestMarginDateByEmail } from "../services/margin";

export const useDashboardData = () => {
    const [campaigns, setCampaigns] = useState([]);
    const [tableData, setTableData] = useState([]);
    const [dailyNetProfitData, setDailyNetProfitData] = useState([]);
    const [dateRange, setDateRange] = useState({ startDate: null, endDate: null });
    const [isLoading, setIsLoading] = useState(true);

    const { startDate } = dateRange;
    const formatDate = (d) => d.toISOString().split("T")[0];

    const fetchAll = useCallback(async (selectedDate, campaignList) => {
        const end = new Date(selectedDate);
        const start = new Date(selectedDate);
        start.setDate(end.getDate() - 6);
        const sd = formatDate(start);
        const ed = formatDate(end);

        try {
            const marginPromises = campaignList.map(c => getMarginByCampaignId({ startDate: sd, endDate: ed, campaignId: c.campaignId }).then(res => ({ campaignId: c.campaignId, data: res.data?.[0]?.data || [] })));
            const profitPromise = getNetProfitAndReturnCost({ startDate: sd, endDate: ed });

            const [marginData, profitResp] = await Promise.all([Promise.all(marginPromises), profitPromise]);

            setTableData(marginData);
            setDailyNetProfitData(Array.isArray(profitResp.data) ? profitResp.data : []);
        } catch (err) {
            console.error("데이터 불러오기 오류:", err);
        }
    }, []);

    useEffect(() => {
        const initialize = async () => {
            setIsLoading(true);
            try {
                const dateRes = await findLatestMarginDateByEmail();
                const latest = dateRes?.data ? new Date(dateRes.data) : new Date();
                const campaignsRes = await getMyCampaigns();
                const loadedCampaigns = campaignsRes.data || [];

                setDateRange({ startDate: latest, endDate: latest });
                setCampaigns(loadedCampaigns);
                await fetchAll(latest, loadedCampaigns);
            } catch (err) {
                console.error("초기화 실패:", err);
            } finally {
                setIsLoading(false);
            }
        };
        initialize();
    }, [fetchAll]);

    const handleDateChange = ({ startDate, endDate }) => {
        const selected = startDate;
        setDateRange({ startDate: selected, endDate: endDate ?? selected });
        fetchAll(selected, campaigns);
    };

    const latestDate = startDate ? formatDate(startDate) : null;
    const flatData = useMemo(() => tableData.flatMap(group => group.data), [tableData]);
    const latestData = useMemo(() => flatData.filter(x => formatDate(new Date(x.marDate)) === latestDate), [flatData, latestDate]);

    const totalSales = latestData.reduce((sum, x) => sum + (x.marSales || 0), 0);
    const totalActualSales = latestData.reduce((sum, x) => sum + (x.marActualSales || 0), 0);
    const totalReturnCount = latestData.reduce((sum, x) => sum + (x.marReturnCount || 0), 0);
    const totalAdCost = latestData.reduce((sum, x) => sum + (x.marAdCost || 0), 0);
    const roas = totalAdCost ? (totalSales / totalAdCost) * 100 : 0;
    const margin = dailyNetProfitData?.find(d => d.marDate === latestDate)?.margin ?? 0;
    const sun = 2;

    return {
        isLoading,
        startDate,
        campaigns,
        tableData,
        latestDate,
        totalSales,
        totalActualSales,
        totalReturnCount,
        margin,
        totalAdCost,
        roas,
        sun,
        handleDateChange
    };
};