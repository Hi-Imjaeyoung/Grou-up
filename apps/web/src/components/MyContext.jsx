import React, { createContext, useState, useContext, useCallback } from 'react';
import { addMemo as addMemoApi, deleteMemo as memoDelete } from "../services/memo";
import { getMemoAboutCampaign as fetchMemoData } from "../services/keyword"
const MyContext = createContext();

const MyContextProvider = ({ children }) => {
    const [campaignData, setCampaignData] = useState(null);
    const [memoData, setMemoData] = useState({}); // 초기값을 빈 객체로 설정
    const [scrollToDate, setScrollToDate] = useState(null); // 스크롤할 날짜 상태 추가

    const addMemo = useCallback(async ({ campaignId, contents, date }) => {
        try {
            const newMemo = await addMemoApi({ campaignId, contents, date });
            // memoData 상태 업데이트
            setMemoData(prevMemoData => {
                const newMemoData = { ...prevMemoData }; // 이전 상태 복사
                const memoDate = newMemo.data.date; // newMemo에서 date 값을 가져옴
                if (newMemoData[memoDate]) {
                    // 해당 날짜에 이미 메모가 있는 경우, 새로운 메모 추가
                    newMemoData[memoDate] = [...newMemoData[memoDate], newMemo.data.contents]; // newMemo에서 contents 값을 가져옴
                } else {
                    // 해당 날짜에 메모가 없는 경우, 새로운 배열 생성
                    newMemoData[memoDate] = [newMemo.data.contents]; // newMemo에서 contents 값을 가져옴
                }
                return newMemoData;
            });
            return newMemo;
        } catch (error) {
            console.error("Error add memo:", error);
            throw error; // 오류를 caller에게 re-throw
        }
    }, []);

    const deleteMemo = useCallback(async ({ id, date, contents }) => {
        try {
            await memoDelete({ id });

            // memoData 상태 업데이트 (삭제된 메모 내용 제거)
            setMemoData(prevMemoData => {
                const newMemoData = { ...prevMemoData }; // 이전 상태 복사

                if (newMemoData[date]) {
                    // 해당 날짜에 메모가 있는 경우
                    const indexToRemove = newMemoData[date].indexOf(contents); // 삭제할 요소의 인덱스 찾기

                    if (indexToRemove > -1) {
                        // 삭제할 요소가 있는 경우
                        newMemoData[date] = [
                            ...newMemoData[date].slice(0, indexToRemove), // 삭제할 요소 이전의 요소들
                            ...newMemoData[date].slice(indexToRemove + 1)  // 삭제할 요소 이후의 요소들
                        ];

                        // 만약 해당 날짜에 메모가 더 이상 없다면, 해당 날짜 키 자체를 삭제
                        if (newMemoData[date].length === 0) {
                            delete newMemoData[date];
                        }
                    }
                }

                return newMemoData;
            });
            return true; // 성공적으로 삭제되었음을 알림
        } catch (error) {
            console.error("Error deleting memo:", error);
            return false; // 삭제 실패를 알림
        }
    }, []);


    const getMemoAboutCampaign = useCallback(async ({ campaignId, start, end }) => {
        try {
            const response = await fetchMemoData({ campaignId, start, end });
            // console.log(response.data);
            setMemoData(response.data); // API 응답으로 memoData 업데이트
            setScrollToDate(start); // start 값을 scrollToDate 상태에 저장
            return response;
        } catch (error) {
            console.error("Error fetching memo data:", error);
            // 에러 처리 로직 추가
        }
    }, []);


    const value = {
        // campaignData,
        // memoData,
        // fetchCampaignData,
        // fetchMemoData,
        memoData, // memoData를 value에 포함
        deleteMemo,
        getMemoAboutCampaign,
        addMemo,
        scrollToDate, // scrollToDate를 value에 포함
    };

    return (
        <MyContext.Provider value={value}>
            {children}
        </MyContext.Provider>
    );
}
const useMyContext = () => {
    return useContext(MyContext);
};

export { MyContext, MyContextProvider, useMyContext };
