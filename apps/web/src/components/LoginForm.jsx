import React, { useEffect, useRef } from "react";
import "../styles/Global/Global.css"; // CSS 파일 import
import LoginNavbar from "./Login/LoginNavbar";
import WelcomeSection from "./Login/WelcomeSection";
import LoginSection from "./Login/LoginSection"; // 로그인 섹션
import ContactSection from "./Login/ContactSection";

const LoginForm = () => {
  const sectionsRef = useRef([]);

  useEffect(() => {
    // 로그인 페이지일 때 body에 클래스 추가
    document.body.classList.add("login-page");

    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add("visible"); // 섹션이 보일 때 클래스 추가
        }
      });
    }, {
      threshold: 0.1 // 10%가 보일 때 트리거
    });

    sectionsRef.current.forEach(section => {
      observer.observe(section); // 각 섹션을 관찰
    });

    return () => {
      // 컴포넌트 언마운트 시 body에서 클래스 제거
      document.body.classList.remove("login-page");

      sectionsRef.current.forEach(section => {
        observer.unobserve(section); // 관찰 해제
      });
    };
  }, []);

  return (
    <div className="login-page">
      <LoginNavbar /> {/* 네비게이션 바 추가 */}
      {/* <div ref={el => sectionsRef.current[0] = el} className="section">
          <FeaturesSection />
        </div>
        <div ref={el => sectionsRef.current[1] = el} className="section">
          <FeaturesSectionEx />
        </div>
        <div ref={el => sectionsRef.current[2] = el} className="section">
          <FeatureSectionMeetSellerOrDev />
        </div>
        <div ref={el => sectionsRef.current[3] = el} className="section">
          <FeautresSectionGrouup />
          <FeaturesSectionGrouupEx />
        </div>
        <div ref={el => sectionsRef.current[4] = el} className="section">
          <FeatureLastSection />
        </div>
        <div ref={el => sectionsRef.current[5] = el} className="section">
          <PricingSection />
        </div> */}

      <div className="min-h-screen bg-gradient-to-br from-purple-600 to-blue-500 flex items-center justify-center">
        <div className="w-full max-w-screen-lg grid grid-cols-1 lg:grid-cols-2 bg-white rounded-lg shadow-xl overflow-hidden">
          <WelcomeSection /> {/* 환영 메시지 섹션 */}
          <LoginSection /> {/* 로그인 섹션 */}
        </div>
      </div>
      <div ref={el => sectionsRef.current[6] = el} className="section">
        <ContactSection /> {/* 문의 섹션 */}
      </div>
    </div>
  );
};

export default LoginForm;
