
import { useState, useEffect, useCallback } from "react";
import { STATUS, ACTIONS, EVENTS } from 'react-joyride';

// ✅ 이제 steps, isLoading, tourKey를 인자로 받습니다.
export const useProductTour = ({ steps, isLoading = false, tourKey }) => {
    const [runTour, setRunTour] = useState(false);
    const [stepIndex, setStepIndex] = useState(0);

    // ❌ 더 이상 tourSteps를 여기서 직접 정의하지 않습니다.

    useEffect(() => {
        // ✅ 밖에서 전달받은 tourKey를 사용합니다.
        const hasCompleted = localStorage.getItem(tourKey);
        // 데이터 로딩이 끝났고, 해당 투어를 본 적이 없을 때만 투어를 실행합니다.
        if (!isLoading && !hasCompleted) {
            setRunTour(true);
        }
    }, [isLoading, tourKey]);

    const handleJoyrideCallback = useCallback((data) => {
        const { action, index, status, type } = data;
        if ([STATUS.FINISHED, STATUS.SKIPPED].includes(status)) {
            // ✅ 밖에서 전달받은 tourKey를 사용합니다.
            localStorage.setItem(tourKey, 'true');
            setStepIndex(0);
            setRunTour(false);
        } else if ([EVENTS.STEP_AFTER].includes(type)) {
            const nextStepIndex = index + (action === ACTIONS.PREV ? -1 : 1);
            setStepIndex(nextStepIndex);
        }
    }, [tourKey]);

    // ✅ 밖에서 받은 steps를 그대로 다시 반환해줍니다.
    return { runTour, steps, stepIndex, handleJoyrideCallback };
};