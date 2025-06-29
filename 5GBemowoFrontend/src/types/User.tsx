import {UserRole} from "./UserRole.tsx";


export interface User {
    id: number;
    email: string | null;
    phoneNumber: string | null;
    username: string;
    password: string;
    avatarPath: string | null;
    createdAt: string;
    lastActiveAt: string;
    roles: UserRole[];
    isActive: boolean;
    emailVerified: boolean;
    phoneNumberVerified: boolean;
}