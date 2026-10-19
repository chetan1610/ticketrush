from dataclasses import dataclass

import jwt
from fastapi import Depends, Header, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.config import settings

bearer = HTTPBearer(auto_error=False)


@dataclass
class CurrentUser:
    id: int
    email: str


def current_user(creds: HTTPAuthorizationCredentials | None = Depends(bearer)):
    if creds is None:
        raise HTTPException(status_code=401, detail="missing bearer token")
    try:
        claims = jwt.decode(creds.credentials, settings.jwt_secret, algorithms=["HS256"])
    except jwt.PyJWTError:
        raise HTTPException(status_code=401, detail="invalid or expired token")
    if claims.get("token_type") != "access":
        raise HTTPException(status_code=401, detail="access token required")
    return CurrentUser(id=int(claims["user_id"]), email=claims["email"])


def internal_only(x_internal_token: str | None = Header(default=None)):
    if x_internal_token != settings.internal_token:
        raise HTTPException(status_code=403, detail="internal endpoint")
