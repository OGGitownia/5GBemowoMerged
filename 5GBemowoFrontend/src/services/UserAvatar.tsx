import React, { useEffect, useState } from "react";
import {User} from "../types/User.tsx";
import deafultAvatar from "../assets/goethe.png"


interface UserAvatarProps {
    user: User;
    size?: number;
}

const UserAvatar: React.FC<UserAvatarProps> = ({ user, size = 64 }) => {
    const [imageSrc, setImageSrc] = useState<string>(deafultAvatar);


    useEffect(() => {
        let active = true;
        let objectUrl: string | null = null;

        const loadImage = async () => {
            if (!user.avatarPath || user.avatarPath.trim() === "") {
                setImageSrc(deafultAvatar);
                return;
            }

            try {
                const response = await fetch(user.avatarPath);
                if (!response.ok) throw new Error("Failed to fetch image");

                const blob = await response.blob();
                objectUrl = URL.createObjectURL(blob);
                if (active) setImageSrc(objectUrl);
            } catch (error) {
                console.error("Error loading user avatar:", error);
                setImageSrc(deafultAvatar);
            }
        };

        loadImage();

        return () => {
            active = false;
            if (objectUrl) URL.revokeObjectURL(objectUrl);
        };
    }, [user.avatarPath]);

    return (
        <img
            src={imageSrc}
            alt="User avatar"
            width={size}
            height={size}
            style={{ borderRadius: "50%", objectFit: "cover" }}
        />
    );
};

export default UserAvatar;