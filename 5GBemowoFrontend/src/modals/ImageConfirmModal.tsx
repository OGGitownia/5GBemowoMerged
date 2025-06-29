import React from "react";
import "../styles/modals/ImageConfirmModal.css";

interface Props {
    file: File;
    onConfirm: () => void;
    onCancel: () => void;
}

const ImageConfirmModal: React.FC<Props> = ({ file, onConfirm, onCancel }) => {
    const imageUrl = URL.createObjectURL(file);

    return (
        <div className="modal-backdrop">
            <div className="modal">
                <h3 className="photo-confirmation-title">Confirm new profile photo</h3>
                <img src={imageUrl} alt="Selected" className="modal-preview" />
                <div className="modal-buttons">
                    <button onClick={onConfirm}>Confirm</button>
                    <button className="photo-confirmation-cancel" onClick={onCancel}>Cancel</button>
                </div>
            </div>
        </div>
    );
};

export default ImageConfirmModal;
