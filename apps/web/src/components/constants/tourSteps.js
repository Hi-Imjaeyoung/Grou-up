
export const dashboardTourSteps = [
    {
        target: '.dashboard-container', // ✅ 대시보드 전체를 감싸는 컨테이너를 타겟으로 지정
        content: (
            <>
                그로우업에 오신 것을 환영합니다!
                <br />
                지금부터 주요 기능들을 간단히 소개해 드릴게요.
            </>
        ), placement: 'center', // ✅ 설명창을 화면 정가운데에 위치시킴
        disableBeacon: true,
    },
    {
        target: '.tour-step-calendar > *',
        content: '여기서 원하시는 날짜를 선택하여 데이터를 조회할 수 있습니다.',
        placement: 'bottom',
        disableBeacon: true,
    },
    {
        target: '.tour-step-sales > *',
        content: '선택한 날짜의 총 매출을 보여줍니다.',
        placement: 'bottom',
        disableBeacon: true,
    },
    {
        target: '.tour-step-margin > *',
        content: '총 매출에서 광고비, 원가 등을 제외한 마진 금액입니다.',
        placement: 'bottom',
        disableBeacon: true,
    },
    {
        target: '.tour-step-roas > *',
        content: '광고비 대비 수익률(ROAS)을 나타냅니다. 높을수록 효율이 좋습니다.',
        placement: 'bottom',
        disableBeacon: true,
    },
    {
        target: '.tour-step-sales-report > *',
        content: '일자별 총매출, 총광고비, ROAS 추이를 확인할 수 있습니다.',
        placement: 'top',
        disableBeacon: true,
    },
    {
        target: '.tour-step-sales-report .chart-toggle-switch',
        content: '이 버튼들을 눌러 그래프 보기와 표 보기 모드를 서로 전환할 수 있습니다.',
        placement: 'top',
        disableBeacon: true,
        spotlightPadding: 5,
    },
    {
        target: '.tour-step-margin-report > *',
        content: '캠페인별 마진과 순이익을 빠르게 확인할 수 있는 요약 보고서입니다.',
        placement: 'top',
        disableBeacon: true,

    }
];