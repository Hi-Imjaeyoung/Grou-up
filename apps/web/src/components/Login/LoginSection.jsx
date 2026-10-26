import React from "react";
import KakaoLoginButton from "../KakaoLoginButton";
import GoogleLoginButton from "../GoogleLoginButton";

const LoginSection = () => {
    return (
        <section id="login" >
            <div className="flex flex-col items-center justify-center p-8 w-full">
                <div className="w-full max-w-md">
                    <div className="text-center mb-6">
                        <h1 className="text-3xl font-bold text-gray-900">로그인</h1>
                        <p className="text-gray-500">계정에 로그인해주세요</p>
                    </div>
                    <div className="text-center mt-4 space-y-3">
                        <KakaoLoginButton />
                        <GoogleLoginButton />
                    </div>
                </div>
            </div>
        </section>
    );
};

export default LoginSection;
