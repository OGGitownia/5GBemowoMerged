import { useState } from "react";
import "../styles/views/ProfileView.css";
import BackButton from "../components/smallComponents/BackButton.tsx";
import UserAvatar from "../services/UserAvatar.tsx";
import {useApp} from "../services/AppContext.tsx";
import ImageConfirmModal from "../modals/ImageConfirmModal.tsx";
import {fileUpdate} from "../services/fileUpdate.tsx";

function handleDeleteAccount() {

}

const ProfileView = () => {
    const [username, setUsername] = useState("exampleUser");
    const [email, setEmail] = useState("user@example.com");
    const [password, setPassword] = useState("password123");
    const [selectedFile, setSelectedFile] = useState<File | null>(null);


    const [editingField, setEditingField] = useState<"username" | "email" | "password" | null>(null);
    const [tempValue, setTempValue] = useState("");

    const handleEditClick = (field: "username" | "email" | "password") => {
        setEditingField(field);
        if (field === "username") setTempValue(username);
        if (field === "email") setTempValue(email);
        if (field === "password") setTempValue(password);
    };
    const {user} = useApp();

    const handleSave = () => {
        if (editingField === "username") setUsername(tempValue);
        if (editingField === "email") setEmail(tempValue);
        if (editingField === "password") setPassword(tempValue);
        setEditingField(null);
    };

    return (
        <>
        <div className="profile-cointainer">
            <BackButton />
            <div className="profile-wrapper">

                <button className="delete-button-fixed" onClick={handleDeleteAccount}>
                    Delete Account
                </button>
                <div className="profile-view">
                    <div className="avatar-block">
                        <UserAvatar user={user!} size={80} />

                        <input
                            type="file"
                            accept="image/*"
                            id="avatar-upload"
                            style={{ display: "none" }}
                            onChange={(e) => {
                                const file = e.target.files?.[0];
                                if (file) {
                                    setSelectedFile(file);
                                    console.log("Wybrano plik:", file);
                                }
                            }}
                        />

                        <button
                            className="photo-editor"
                            onClick={() => document.getElementById("avatar-upload")?.click()}
                        >
                            Edit Photo
                        </button>
                    </div>
                    <h2 className="profile-title">Profile</h2>
                    <div className="profile-field">
                        <label>Username:</label>
                        {editingField === "username" ? (
                            <>
                                <input
                                    type="text"
                                    value={tempValue}
                                    onChange={(e) => setTempValue(e.target.value)}
                                />
                                <button className="back-button-2" onClick={handleSave}>Save</button>
                            </>
                        ) : (
                            <>
                                <span>{username}</span>
                                <button className="back-button-2" onClick={() => handleEditClick("username")}>
                                    Change Username
                                </button>
                            </>
                        )}
                    </div>

                    <div className="profile-field">
                        <label>Email:</label>
                        {editingField === "email" ? (
                            <>
                                <input
                                    type="email"
                                    value={tempValue}
                                    onChange={(e) => setTempValue(e.target.value)}
                                />
                                <button className="back-button-2" onClick={handleSave}>Save</button>
                            </>
                        ) : (
                            <>
                                <span>{email}</span>
                                <button className="back-button-2" onClick={() => handleEditClick("email")}>
                                    Change Email
                                </button>
                            </>
                        )}
                    </div>

                    <div className="profile-field">
                        <label>Password:</label>
                        {editingField === "password" ? (
                            <>
                                <input
                                    type="password"
                                    value={tempValue}
                                    onChange={(e) => setTempValue(e.target.value)}
                                />
                                <button className="back-button-2" onClick={handleSave}>Save</button>
                            </>
                        ) : (
                            <>
                                <span>{"*".repeat(password.length)}</span>
                                <button className="back-button-2" onClick={() => handleEditClick("password")}>
                                    Change Password
                                </button>
                            </>
                        )}
                    </div>
                </div>
            </div>
        </div>
            {selectedFile && (
                <ImageConfirmModal
                    file={selectedFile as File}
                    onConfirm={() => {
                        if (selectedFile) {
                            fileUpdate(selectedFile,setSelectedFile);
                        }
                    }}
                    onCancel={() => setSelectedFile(null)}
                />
            )}
        </>
    );
};

export default ProfileView;
