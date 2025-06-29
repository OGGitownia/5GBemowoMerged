import availableIcon from "../../assets/available.png";
import treeIcon from "../../assets/tree.png";
import starIcon from "../../assets/favourite.png";
import "../../styles/smallComponents/ViewSelector.css";

interface ViewSelectorProps {
    activeButton: "all" | "tree" | "best";
    handleButtonClick: (buttonType: "all" | "tree" | "best") => void;
}

const ViewSelector: React.FC<ViewSelectorProps> = ({ activeButton, handleButtonClick }) => {
    return (
        <div className="view-selector">
            <button
                type="button"
                className={`icon-btn ${activeButton === "all" ? "active" : ""}`}
                onClick={() => handleButtonClick("all")}
            >
                <img src={availableIcon} alt="Available Norms" />
                <span>Available</span>
            </button>

            <button
                type="button"
                className={`icon-btn ${activeButton === "tree" ? "active" : ""}`}
                onClick={() => handleButtonClick("tree")}
            >
                <img src={treeIcon} alt="Tree View" />
                <span>Tree</span>
            </button>

            <button
                type="button"
                className={`icon-btn ${activeButton === "best" ? "active" : ""}`}
                onClick={() => handleButtonClick("best")}
            >
                <img src={starIcon} alt="Favorites" />
                <span>Best</span>
            </button>
        </div>
    );
};

export default ViewSelector;
