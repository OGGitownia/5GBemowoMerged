

export class ValidateData {
    static isValidName(name: string): boolean {
        const trimmed = name.trim();
        const hasMinLength = trimmed.length >= 4;
        const hasLettersOrDigits = /[a-zA-Z0-9]/.test(trimmed);
        return hasMinLength && hasLettersOrDigits;
    }

    static isValidEmail(email: string): boolean {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    static isValidPassword(password: string): boolean {
        const hasMinLength = password.length >= 8;
        const hasUppercase = /[A-Z]/.test(password);
        const hasNumber = /\d/.test(password);
        const hasSpecialChar = /[!@#$%^&*(),.?":{}|<>]/.test(password);
        return hasMinLength && hasUppercase && hasNumber && hasSpecialChar;
    }
}
