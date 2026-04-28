import { config } from "config";
import { useCookie } from "hooks";
import { useEffect } from "react";
import { useParams } from "react-router-dom";

export function Callback() {
    const { token } = useParams();
    const { setCookie } = useCookie();

    useEffect(() => {
        if (token) {
            setCookie(config.tokenCookieNome, token, 860000);
            window.location.replace("/");
        }
    }, [token]);

    return <></>;
}