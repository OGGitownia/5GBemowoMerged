import ReactDOM from "react-dom/client";
import "./index.css";


const App = () => {
    localStorage.setItem("token", "1")
    return (
        <div className="app-container">

        </div>
    );
};

ReactDOM.createRoot(document.getElementById("root")!).render(<App />);
