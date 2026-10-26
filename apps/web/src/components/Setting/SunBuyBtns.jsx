import React from "react";
import "./SunBuyBtns.css";
const SunBuyBtns = () => {

    return (
        <div className="ItemComponent" >
            <div className="Items">
                <div className="ItemButton">
                    <div className="ItemHeader">햇살충전하기</div>
                    <div className="ItemPriceContainer">
                        <div className="ItemPrice">
                            100<span className="unit">개</span>
                        </div>

                    </div>
                    <hr class="thick-hr"></hr>
                    <div className="ItemButtonDetailContainer">
                        <div className="ItemButtonDetail">캠패인 1개당 햇살 10개 사용(1개월)</div>
                        <div className="ItemButtonDetail">광고 캠패인 1개 10달 조회</div>
                        <div className="ItemButtonDetail">광고 캠패인 10개 1달 조회</div>
                    </div>
                    <button className="payButton">
                        100,000원
                    </button>
                </div>
                <div className="ItemButton">
                    <div className="ItemHeader">햇살충전하기</div>
                    {/* <div className="ItemBadge">새싹 샐러</div> */}
                    <div className="ItemPriceContainer">
                        {/* <div className="ItemPrePrice">10000</div> */}
                        <div className="ItemPrice">
                            200<span className="unit">개</span>
                        </div>
                    </div>
                    <hr class="thick-hr"></hr>
                    <div className="ItemButtonDetailContainer">
                        <div className="ItemButtonDetail">캠패인 1개당 햇살 10개 사용(1개월)</div>
                        <div className="ItemButtonDetail">광고 캠패인 1개 10달 조회</div>
                        <div className="ItemButtonDetail">광고 캠패인 10개 1달 조회</div>
                    </div>
                    <button className="payButton">
                        100,000원
                    </button>
                </div>
                <div className="ItemButton">
                    <div className="ItemHeader">햇살충전하기</div>
                    {/* <div className="ItemBadge">새싹 샐러</div> */}
                    <div className="ItemPriceContainer">
                        {/* <div className="ItemPrePrice">10000</div> */}
                        <div className="ItemPrice">
                            300<span className="unit">개</span>
                        </div>

                    </div>
                    <hr class="thick-hr"></hr>
                    <div className="ItemButtonDetailContainer">
                        <div className="ItemButtonDetail">캠패인 1개당 햇살 10개 사용(1개월)</div>
                        <div className="ItemButtonDetail">광고 캠패인 1개 10달 조회</div>
                        <div className="ItemButtonDetail">광고 캠패인 10개 1달 조회</div>
                    </div>
                    <button className="payButton">
                        100,000원
                    </button>
                </div>
                <div className="ItemButton">
                    <div className="ItemHeader">햇살충전하기</div>
                    {/* <div className="ItemBadge">새싹 샐러</div> */}
                    <div className="ItemPriceContainer">
                        {/* <div className="ItemPrePrice">10000</div> */}
                        <div className="ItemPrice">
                            400<span className="unit">개</span>
                        </div>

                    </div>
                    <hr class="thick-hr"></hr>
                    <div className="ItemButtonDetailContainer">
                        <div className="ItemButtonDetail">캠패인 1개당 햇살 10개 사용(1개월)</div>
                        <div className="ItemButtonDetail">광고 캠패인 1개 10달 조회</div>
                        <div className="ItemButtonDetail">광고 캠패인 10개 1달 조회</div>
                    </div>
                    <button className="payButton">
                        100,000원
                    </button>
                </div>
            </div>
        </div>
    );
};
export default SunBuyBtns;