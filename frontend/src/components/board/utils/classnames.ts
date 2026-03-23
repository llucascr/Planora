type ClassValue = string | number | boolean | null | undefined | ClassValue[] | Record<string, unknown>;

function toVal(val: ClassValue): string {
    if (typeof val === "string") return val;
    if (typeof val !== "object" || val === null) return "";
    if (Array.isArray(val)) return classnames(...val);
    let str = "";
    for (const key in val as Record<string, unknown>) {
        if ((val as Record<string, unknown>)[key]) str = str ? `${str} ${key}` : key;
    }
    return str;
}

export function classnames(...args: ClassValue[]): string {
    let str = "";
    for (const arg of args) {
        const val = toVal(arg);
        if (val) str = str ? `${str} ${val}` : val;
    }
    return str;
}