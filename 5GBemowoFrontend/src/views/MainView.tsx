import "../styles/views/MainView.css";
import { useApp } from "../services/AppContext.tsx";
import {useEffect, useState} from "react";
import TreeComponent from "../components/bigComponents/TreeComponent.tsx";
import BestBasesComponent from "../components/bigComponents/BestBasesComponent.tsx";
import AllBasesComponent from "../components/bigComponents/AllBasesComponent.tsx";
import ViewSelector from "../components/smallComponents/ViewSelector.tsx";
import ChatHistoryPanel from "../components/bigComponents/ChatHistoryPanel.tsx";
import RightIcons from "../components/smallComponents/RightIcons.tsx";

export default function MainView() {

    const [activeButton, setActiveButton] = useState<"all" | "tree" | "best">("all");
    const {setSelectedChatInfo, user } = useApp();

    useEffect(() => {
        setSelectedChatInfo(prev => ({
            ...prev,
            chatRel: null,
            chatSeries: null,
            chatNorm: null
        }));
    }, []);

    const handleButtonClick = (buttonType: "all" | "tree" | "best") => {
        setActiveButton(buttonType);
        console.log(`Active button is now: ${buttonType}`);
        console.log(user)
        setSelectedChatInfo(prev => ({
            ...prev,
            chatRel: null,
            chatSeries: null,
            chatNorm: null
        }));
    };

    const renderContent = () => {
        switch (activeButton) {
            case "all":
                return <AllBasesComponent/>;
            case "best":
                return <BestBasesComponent/>;
            case "tree":
                return <TreeComponent/>;
            default:
                return null;
        }
    };

    return (
        <div className="main-container">
                <RightIcons/>
            <div className="main-content">
                <div className="header">
                    <div className="text">Chat 3 GPP</div>
                    <div className="underline"/>
                </div>
                <ViewSelector
                    activeButton={activeButton}
                    handleButtonClick={handleButtonClick}
                />
                <div className="content-columns">

                    <div className="norm-list-scroll-expanded">
                        {renderContent()}
                    </div>
                </div>

            </div>
            <div className="chat-history-wrapper">
                <ChatHistoryPanel/>
            </div>
        </div>
    );
}
