import { config } from "config";
import { useCookie } from "hooks";
import { useEffect, useState } from "react";
import { Navigate, useNavigate, useParams } from "react-router-dom";

export function Callback() {
    const { token } = useParams();
    const { setCookie } = useCookie();
    const navigate = useNavigate();
    useEffect(() => {
        if (token) {
            setCookie!(config.tokenCookieNome, token, 860000);
            navigate("/");
        }
    }, [token]);

    return <></>;
}