import uvicorn
from fastapi import FastAPI, Request

app = FastAPI()


@app.post("/callback")
async def callback(request: Request):
    body = await request.json()
    print(body)
    return {"status": "ok"}


if __name__ == "__main__":
    uvicorn.run("teste:app", host="0.0.0.0", port=9000)
