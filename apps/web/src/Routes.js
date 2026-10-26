import React from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Login from "./pages/Login";
import SignupForm from "./components/SignupForm";
import MainForm from "./components/MainForm";
import LoginCallback from "./pages/LoginCallback";
import PrivateRoute from "./components/PrivateRoute"; // PrivateRoute 추가
import Sidebar from "./components/Sidebar";

import DeleteScreen from "./components/DataSetting/DataDeletionComponent";
// import FileUploadForm from "./components/FileUploadForm";
import FileUploadGrid from "./components/Upload/FileUploadGrid";
import CampaignDetail from "./components/CampaignDetail";
import OtherComponent from "./components/memo/MemoComponent";
import MemoButton from "./components/memo/MemoButton";
import MarginCalculatorForm from "./components/Margin_v2/MarginCalculatorForm";
import MarginCalculatorResult from "./components/Margin_v2/MarginCalculatorResult";
import { MyContextProvider } from "../src/components/MyContext"
import SettingPage from "./components/Setting/SettingPage";
import CampaignAmalysis from "./components/Campaign/CampaignAnalysis";
import MarginOverview from "./components/Margin_v2/MarginOverview";
import { getMyCampaigns } from "./services/campaign";
class AppRoutes extends React.Component {
    state = {
        campaigns: [],
        showOtherComponent: false,
    };

    componentDidMount() {
        this.fetchCampaigns();
    }

    fetchCampaigns = async () => {
        try {
            const { data } = await getMyCampaigns();
            this.setState({ campaigns: data || [] });
        } catch (err) {
            console.error(err);
        }
    };

    // ✅ 캠페인 순서 변경을 처리할 함수를 여기에 추가합니다.
    handleCampaignOrderChange = (newCampaigns) => {
        this.setState({ campaigns: newCampaigns });
    };

    toggleOtherComponent = () => {
        this.setState((prevState) => ({
            showOtherComponent: !prevState.showOtherComponent,
        }));
    };
    render() {
        // ✅ state에서 campaigns를 가져와서 props로 전달할 준비를 합니다.
        const { campaigns, showOtherComponent } = this.state;
        return (
            <Router>
                <Routes>
                    {/* 공개된 경로 */}
                    <Route path="/" element={<Login />} />
                    <Route path="/signup" element={<SignupForm />} />
                    <Route path="/oauth/kakao/callback" element={<LoginCallback />} />
                    <Route path="/oauth/google/callback" element={<LoginCallback />} />

                    {/* 보호된 경로 - ✅ 필요한 모든 곳에 campaigns를 props로 전달합니다. */}
                    <Route
                        path="/main"
                        element={
                            <PrivateRoute>
                                <Sidebar campaigns={campaigns} />
                                <MainForm />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/upload"
                        element={
                            <PrivateRoute>
                                <Sidebar campaigns={campaigns} />
                                <FileUploadGrid />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/delete-data"
                        element={
                            <PrivateRoute>
                                <Sidebar campaigns={campaigns} />
                                <DeleteScreen />
                            </PrivateRoute>
                        }
                    >


                    </Route>
                    <Route
                        path="/margin-overview"
                        element={
                            <PrivateRoute>
                                <Sidebar campaigns={campaigns} />
                                <MarginOverview />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/margin-calculator/formula"
                        element={
                            <PrivateRoute>
                                <Sidebar campaigns={campaigns} />
                                {/* ✅ onCampaignOrderChange 함수를 props로 전달합니다. */}
                                <MarginCalculatorForm
                                    campaigns={campaigns}
                                    onCampaignOrderChange={this.handleCampaignOrderChange}
                                />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/margin-calculator/result"
                        element={
                            <PrivateRoute>
                                <Sidebar campaigns={campaigns} />
                                <MarginCalculatorResult campaigns={campaigns} />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/settings"
                        element={
                            <PrivateRoute>
                                <Sidebar campaigns={campaigns} />
                                < SettingPage />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/campaigns/:campaignId"
                        element={
                            <PrivateRoute>
                                <MyContextProvider>
                                    {showOtherComponent ? (
                                        <>
                                            <OtherComponent />
                                            <CampaignDetail />
                                        </>
                                    ) : (
                                        <>
                                            <Sidebar campaigns={campaigns} />
                                            <CampaignDetail />
                                        </>
                                    )}
                                    <MemoButton onClick={this.toggleOtherComponent} />
                                </MyContextProvider>
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/campaigns/analysis"
                        element={
                            <PrivateRoute>
                                <Sidebar campaigns={campaigns} />
                                < CampaignAmalysis />
                            </PrivateRoute>
                        }
                    />


                </Routes>
            </Router>
        );
    }
}


export default AppRoutes;
