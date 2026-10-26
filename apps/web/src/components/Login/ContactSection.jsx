import React from "react";

const ContactSection = () => {
    return (
        <section id="contact" className="p-8 bg-gray-100">
            <h2 className="text-4xl font-bold mb-4">문의</h2>
            <p className="text-lg">
                궁금한 점이 있으신가요? 아래의 방법으로 문의해 주세요:
            </p>
            <ul className="list-disc ml-6 mt-2 font-bold">
                <li>이메일: grouup7271@gmail.com</li>
            </ul>
        </section>
    );
};

export default ContactSection;
